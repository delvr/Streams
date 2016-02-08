package streams.world.gen.structure

import java.util.Random

import farseek.block._
import farseek.block.material._
import farseek.util._
import farseek.world.Direction._
import farseek.world._
import farseek.world.biome._
import farseek.world.gen._
import farseek.world.gen.structure.StructureComponent
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks._
import net.minecraft.world._
import net.minecraft.world.gen.structure.StructureBoundingBox
import net.minecraftforge.common.BiomeDictionary.Type._
import net.minecraftforge.common.BiomeDictionary._
import streams.block.FixedFlowBlock._
import streams.block._
import streams.world.gen.TfcChunkGeneratorExtensions
import streams.world.gen.structure.RiverComponent._

import scala.Array._
import scala.collection.mutable
import scala.math._

/** @author delvr */
abstract class RiverComponent(val river: RiverStructure, val boundingBox: StructureBoundingBox,
                              val upstreamOrientation: Direction, isMirrored: Boolean) extends StructureComponent(river) {

    implicit val cs = new DirectedCoordinates(xMin, yMin, zMin, ZPlanMax, upstreamOrientation)

    // Bounding box with extra padding for carving out shores. Unlike flows, shores are allowed to intersect other components.
    override val paddedBox = new StructureBoundingBox(xMin - ShorePadding, yMin, zMin - ShorePadding, xMax + ShorePadding, yMax, zMax + ShorePadding)

    protected val liquid = river.liquid

    protected val flowPlan: FlowPlan = fill(XPlanSize, ZPlanSize)(None)

    // WORLD coordinates, as opposed to the translated/mirrored/rotated local coordinates used elsewhere
    val flows  = mutable.Set[XZ]()
    val shores = mutable.Set[XZ]()

    private val valleys = mutable.Set[XZ]()

    var widthStretch = -1

    protected var straightUpstream: Option[RiverUpstreamComponent] = None
    protected var   curvedUpstream: Option[RiverUpstreamComponent] = None

    // Surface and roof levels are set by "Z-Lines" which are slices of the flow model starting from downstream (z = 0)
    val roofLevels = new Array[Int](ZPlanSize)
    val maxSurfaceLevels = new Array[Int](ZPlanSize)

    val surfaceLevelsUnits = new Array[Int](ZPlanSize) // Units: 7 fractions per block, to account for flow decay

    // Components can be mirrored on the model's X-axis (left curves become right curves, etc.)
    protected def mirrored(orientation: Direction) = if(isMirrored) orientation.opposite else orientation
    private def mirrored(flow: Flow) = flow.map{case (dx, dz) => if(isMirrored) (-dx, dz) else (dx, dz)}
    private def xMirrored(p: FlowPlan, x: Int) = if(isMirrored) xFlowPlanMax(p) - x else x
    private def zMirrored(p: FlowPlan, z: Int) = if(upstreamOrientation == North || upstreamOrientation == West) zFlowPlanMax(p) - z else z

    protected def straightOffset(p: FlowPlan): Option[Int] = (0 to xFlowPlanMax(p)).find(x => p(xMirrored(p, x))(zFlowPlanMax(p)).isDefined)
    protected def curvedOffset(p: FlowPlan, zMirror: Boolean = true): Option[Int] =
        (0 to zFlowPlanMax(p)).find(z => p(0)(if(zMirror) zMirrored(p, z) else z).isDefined || p(xFlowPlanMax(p))(if(zMirror) zMirrored(p, z) else z).isDefined)

    private def flowDefined(x: Int, z: Int) = isWithin(flowPlan, x, z) && flowPlan(x)(z).isDefined

    val downstreamComponent: Option[RiverComponent]
    protected def upstreamComponents: Seq[RiverUpstreamComponent] = Seq(straightUpstream, curvedUpstream).flatten

    def downstreamLevel(z: Int, levelType: RiverComponent => Array[Int], offset: Int = 1): Option[Int] = {
        val dsz = z - offset
        if(dsz >= 0) Some(levelType(this)(dsz))
        else downstreamComponent.flatMap(_.downstreamLevel(dsz + ZPlanSize, levelType, 0))
    }

    def straightUpstreamLevel(z: Int, levelType: RiverComponent => Array[Int], offset: Int = 1): Option[Int] = {
        val usz = z + offset
        if(usz <= ZPlanMax) Some(levelType(this)(usz))
        else straightUpstream.flatMap(_.straightUpstreamLevel(usz - ZPlanSize, levelType, 0))
    }

    def curvedUpstreamLevel(x: Int, levelType: RiverComponent => Array[Int], offset: Int = 1): Option[Int] = {
        if(!isMirrored) {
            val usx = x + offset
            if(usx <= XPlanMax) None
            else curvedUpstream.flatMap(_.straightUpstreamLevel(usx - XPlanSize, levelType, 0))
        } else {
            val usx = x - offset
            if(usx >= 0) None
            else curvedUpstream.flatMap(_.straightUpstreamLevel(usx + XPlanSize, levelType, 0))
        }
    }

    def upstreamLevels(x: Int, z: Int, levelType: RiverComponent => Array[Int], offset: Int = 1): Seq[Int] =
        Seq(straightUpstreamLevel(z, levelType, offset), curvedUpstreamLevel(x, levelType, offset)).flatten

    private def flowDecayAt(z: Int) = flowDecay(clamped(surfaceLevelsUnits, z))

    def hasCurve = curvedUpstream.isDefined
    def isJunction = straightUpstream.isDefined && curvedUpstream.isDefined
    def isSource = straightUpstream.isEmpty && curvedUpstream.isEmpty

    //-----------------------------------------------------------------------------------------------------------------
    // Generating
    //-----------------------------------------------------------------------------------------------------------------
    protected def newUpstreamComponent(offset: Int, direction: Direction, uncommitted: Seq[RiverComponent], mirrored: Boolean)
                                      (implicit bac: IBlockAccess, random: Random): Option[RiverUpstreamComponent] = {
        val (xBoxSize, zBoxSize) = if(direction == South || direction == North) (XPlanSize, ZPlanSize) else (ZPlanSize, XPlanSize)
        val bounds = boundingBox.adjacentBox(direction, xBoxSize, bac.height, zBoxSize, newUpstreamComponentOffset(offset, mirrored))
        val next = new RiverUpstreamComponent(this, bounds, direction, mirrored)
        if(next.isValid(uncommitted)) Some(next) else None
    }

    private def newUpstreamComponentOffset(offset: Int, mirrored: Boolean) =
        offset - (if(mirrored) XModelPlanSize - ModelPlanRiverStartX - ModelPlanRiverWidth else ModelPlanRiverStartX)

    protected def stretchNorthSouth(model: FlowPlan): FlowPlan = {
        val stretched: FlowPlan = fill(xFlowPlanLength(model), zFlowPlanLength(flowPlan))(None)
        val zModelStretchLimit = curvedOffset(model, zMirror = false)
        val stretchRatio: Float = zModelStretchLimit match {
            case Some(limit) => (MaxStretch + limit).toFloat / limit.toFloat
            case None => (MaxStretch * 2 + zFlowPlanLength(model)).toFloat / zFlowPlanLength(model).toFloat
        }
        for(x <- 0 to xFlowPlanMax(stretched);
            z <- 0 to zFlowPlanMax(stretched)) {
            zModelStretchLimit match {
                case Some(zl) if z - MaxStretch >= zl =>
                    if(z - MaxStretch <= zFlowPlanMax(model))
                        stretched(x)(z) = model(x)(z - MaxStretch)
                case _ =>
                    stretched(x)(z) = clamped(model(x), round(z.toFloat / stretchRatio))
            }
        }
        stretched
    }

    protected def stretchEastWest(model: FlowPlan): FlowPlan = {
        val stretched: FlowPlan = fill(xFlowPlanLength(flowPlan), zFlowPlanLength(model))(None)
        val xModelStretchStart = ModelPlanRiverStartX + ModelPlanRiverWidth
        val stretchRatio: Float = (xFlowPlanLength(model) - xModelStretchStart + MaxStretch).toFloat / (xFlowPlanLength(model) - xModelStretchStart).toFloat
        for(x <- 0 to xFlowPlanMax(stretched);
            z <- 0 to zFlowPlanMax(stretched)) {
            if(x - MaxStretch < xModelStretchStart) {
                if(x - MaxStretch >= 0)
                    stretched(x)(z) = model(x - MaxStretch)(z)
            } else
                stretched(x)(z) = model(clampedIndex(model, round(x.toFloat / stretchRatio)))(z)
        }
        for(x <- 0 to xFlowPlanMax(stretched);
            z <- 0 to zFlowPlanMax(stretched)) {
            if(stretched(x)(z).isDefined && x > 0 && x < xFlowPlanMax(stretched) && z > 0 && z < zFlowPlanMax(stretched))
                if(stretched(x-1)(z-1).isEmpty && stretched(x+1)(z+1).isEmpty)
                    stretched(x)(z) = None // Repair ugly 2x2 square corners
        }
        stretched
    }

    protected def overlay(source: FlowPlan, target: FlowPlan) {
        val xOffset = (xFlowPlanLength(target) - xFlowPlanLength(source)) / 2
        for(x <- 0 to xFlowPlanMax(source);
            z <- 0 to zFlowPlanMax(source)) {
            target(x + xOffset)(z) = combine(target(x + xOffset)(z), mirrored(source(xMirrored(source, x))(z)))
        }
    }

    protected def widen(plan: FlowPlan, amount: Int, eastWest: Boolean = true, northSouth: Boolean = true) {
        for(_ <- 1 to amount) widen(plan, eastWest, northSouth)
    }

    protected def widen(plan: FlowPlan, eastWest: Boolean, northSouth: Boolean) { // Stretch a flow plan on a component
        val justAdded = fill(xFlowPlanLength(plan), zFlowPlanLength(plan))(false)
        for(x <- 0 to xFlowPlanMax(plan);
            z <- 0 to zFlowPlanMax(plan)) {
            if(plan(x)(z).isEmpty) {
                val neighborFlows = neighbors(x, z).flatMap { case (nx, nz) =>
                    if(!isWithin(plan, nx, nz) || justAdded(nx)(nz) || (!eastWest && nx != x) || (!northSouth && nz != z)) None
                    else plan(nx)(nz)
                }
                if(neighborFlows.nonEmpty) {
                    val newFlow = interpolate(neighborFlows: _*)
                    plan(x)(z) = Some(normalize(newFlow._1, newFlow._2))
                    justAdded(x)(z) = true
                }
            }
        }
    }

    protected def commit(implicit bac: IBlockAccess, random: Random) {
        val flatLines = ZLine.map(isFlatAt)
        for(z <- ZLine.reverse) {
            maxSurfaceLevels(z) = maxSurfaceLevelAt(z, flatLines)
            for(x <- XLine) {
                if(flowPlan(x)(z).isDefined) flows += cs.xzWorld(x, z)
                else if(allNeighbors(x, z).exists{case(nx, nz) => flowDefined(nx, nz)}) shores += cs.xzWorld(x, z)
            }
        }
        river += this
    }

    protected def isFlatAt(z: Int) = { // Rivers must be flat wherever they curve or flows don't all have a downstream match
        if(isSource) false
        else if(z == 0) true // By design of the flow plans; can itself be a waterfall but seen as flat from upstream
        else curvedOffset(flowPlan) match {
            case Some(offset) if z >= offset => true
            case _ => XLine.exists(x => flowPlan(x)(z).isDefined != flowPlan(x)(z - 1).isDefined ||
                    (z < ZPlanMax && flowPlan(x)(z).isDefined != flowPlan(x)(z + 1).isDefined))
        }
    }

    private def maxSurfaceLevelAt(z: Int, flatLines: IndexedSeq[Boolean]): Int = { // takes flat zones into account
        val upstreamSurfaceLevels =
            if(isSource && z > ZModelPlanMax) Nil
            else upstreamLevels(xMirrored(flowPlan, XPlanMax), z, _.maxSurfaceLevels)
        val flatSurfaceLevels = (z to 0 by -1).takeWhile(flatLines(_)).map(maxSurfaceLevels)
        (upstreamSurfaceLevels ++ flatSurfaceLevels :+ maxSurfaceLevels(z)).min
    }

    def adjustUpstream()(implicit bac: IBlockAccess) {
        for(z <- ZLine) {
            val yMaxTunnelRoof = min(maxSurfaceLevels(z) + MinTunnelHeight + 1, cs.yLocal(bac.yMax))
            surfaceLevelsUnits(z) = downstreamLevel(z, _.surfaceLevelsUnits).getOrElse(surfaceLevelUnits(river.seaLevel))
            roofLevels(z) = downstreamLevel(z, _.roofLevels).getOrElse(yMaxTunnelRoof)
            val tunnel = blockAt(-ShorePadding, roofLevels(z), z).isSolid || blockAt(XPlanMax + ShorePadding, roofLevels(z), z).isSolid
            if(!tunnel && !isSource)
                XLine.foreach(x => valleys += ((x, z)))
            if(!isFlatAt(z)) {
                if(roofLevels(z) < yMaxTunnelRoof) {
                    roofLevels(z) += 1
                    surfaceLevelsUnits(z) += 1 // gentle slope
                }
                if(roofLevels(z) >= yMaxTunnelRoof || isSource) {
                    val maxSurfaceLevelUnitsByRoof = max(surfaceLevelsUnits(z), surfaceLevelUnits(
                                                         roofLevels(z) - MinTunnelHeight - 1 - (if(isSource) MinSourceBackWallHeight else 0)))
                    if(widthStretch >= 2)
                        surfaceLevelsUnits(z) = min(surfaceLevelUnits(surfaceLevel(surfaceLevelsUnits(z)) + 1), maxSurfaceLevelUnitsByRoof) // steep slope
                    else
                        surfaceLevelsUnits(z) = maxSurfaceLevelUnitsByRoof // waterfall
                }
            }
        }
        upstreamComponents.foreach(_.adjustUpstream())
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Building
    //-----------------------------------------------------------------------------------------------------------------
    def carveValleyAt(wx: Int, yGround: Int, wz: Int)(implicit blockSetter: BlockSetter, random: Random) {
        val (x, z) = cs.xzLocal(wx, wz)
        if(valleys.nonEmpty) {
            val valley = valleys.contains(x, z)
            val flow = valley && flowDefined(x, z)
            val dfs = if(valley) distanceFromShore(x, z) else {
              val valleyFlows = valleys.filter(xz => flowDefined(xz.x, xz.z))
              if(valleyFlows.isEmpty) distanceFromShore(x, z) else round(valleyFlows.map(v => distance(x, z, v.x, v.z)).min).toInt
            }
            if((flow || dfs <= ShorePadding) && !isSource) {
                val ySurface = surfaceLevelAt(x, z, dfs) // Surface level adjusted for distance from shore
                val xyzSurface = (x, ySurface, z)
                carveValley(x, ySurface, z, dfs, flow, yGround)
                if(flow && blockAt(xyzSurface).getMaterial != liquid) {
                    if(tfcLoaded) // Placeholders would get removed during block replacement; defer their placement
                        TfcChunkGeneratorExtensions.xyzValleySurfaces += cs.xyzWorld(xyzSurface)
                    else // Don't add water yet or it would block caves from digging nearby
                        setBlockAt(xyzSurface, SurfacePlaceholder) // but do prevent chunk block replacement below here
                }
            }
        }
    }

    private def carveValley(x: Int, ySurface: Int, z: Int, dfs: Int, flow: Boolean, yGround: Int)(implicit blockSetter: BlockSetter) {
        val yFloor = if(flow) ySurface else adjustedFloorLevel(x, valleyFloor(x, ySurface, z, dfs, flow, yGround), z)
        for(y <- yGround until yFloor by -1)
            clearBlockAt(x, y, z)
    }

    private def valleyFloor(x: Int, ySurface: Int, z: Int, dfs: Int, flow: Boolean, yGround: Int)(implicit bac: IBlockAccess) = {
        val groundHeight = yGround - ySurface
        if(groundHeight <= 0)
            ySurface
        else {
            val midHeight = (groundHeight + 1) / 2
            val floorHeight =
                if(dfs < MidPadding) min(dfs, midHeight)
                else if(dfs == MidPadding) max(dfs, midHeight)
                else max(max(dfs, midHeight), groundHeight - (ShorePadding - dfs) - 1)
            ySurface + floorHeight
        }
    }

    protected def build(implicit blockSetter: BlockSetter, random: Random) {
        for((x, z) <- EachLocalPosWithPadding) {
            val wxz = cs.xzWorld(x, z)
            if(blockSetter.validAt(wxz)) {
                val flow = flowDefined(x, z)
                val dfs = distanceFromShore(x, z)
                if(flow || dfs <= ShorePadding) {
                    val ySurface = surfaceLevelAt(x, z, dfs)
                    if(isSource && !flow && dfs == 0) {
                        val rockBlock = rockBlockFor(x, ySurface, z)
                        foreachDownFrom((x, clamped(roofLevels, z) - 1, z), blockAt(_).getMaterial != Material.rock, xyz =>
                            if(blockAt(xyz).isGround) setBlockAndDataAt(xyz, rockBlock))
                    } else if(!valleys.contains(x, z))
                        carveTunnel(x, ySurface, z, dfs, flow)
                    else if(tfcLoaded && dfs == 0) { // Fix TFC-mangled valleys
                        val shoreBlock = blockAt(x, ySurface, z)
                        val belowShoreBlockAndData = blockAndDataBelow(x, ySurface, z)
                        val belowShoreBlock = belowShoreBlockAndData.block
                        if(!shoreBlock.isSolidOrLiquid || (shoreBlock.getMaterial == Material.rock && belowShoreBlock.isSolid && belowShoreBlock.getMaterial != Material.rock))
                            setBlockAndDataAt((x, ySurface, z), if(belowShoreBlock.isSoil) grassBlockFor(x, ySurface, z) else belowShoreBlockAndData, notifyNeighbors = false)
                    }
                    if(flow)
                        fillRiver(x, ySurface, z, dfs)
                }
            }
        }
    }

    private def carveTunnel(x: Int, ySurface: Int, z: Int, dfs: Int, flow: Boolean)(implicit blockSetter: BlockSetter) {
        if(flow || dfs < MidPadding) {
            val yFloor = if(flow) ySurface else adjustedFloorLevel(x, ySurface + dfs, z)
            val yRoof = if(!isSource) clamped(roofLevels, z) else ySurface + max(0, MinTunnelHeight - z * 3 / 4 - 1)
            val yBaseCeiling = yRoof - (yFloor - ySurface) - (if(flow) max(0, BaseTunnelCeilingThickness - dfs) else BaseTunnelCeilingThickness)
            val stalactite = TunnelCeilingRandom.nextBoolean && blockAt(x, yBaseCeiling, z).isSolid
            val yCeiling = if(stalactite) yBaseCeiling - 1 else yBaseCeiling
            if(yCeiling > yFloor + 1 || (isSource && yCeiling > yFloor)) {
                val ceiling = blockAt(x, yCeiling, z)
                if(ceiling.isGranular) {
                  setRockBlockAt(x, yCeiling, z) // Harden ceiling
                  if(stalactite) setRockBlockAt(x, yCeiling + 1, z)
                }
                for(y <- yCeiling - 1 until yFloor by -1)
                    clearBlockAt(x, y, z)
            }
        }
    }

    private def fillRiver(x: Int, ySurface: Int, z: Int, dfs: Int)(implicit blockSetter: BlockSetter) {
        val yDownstreamSurface = surfaceLevel(downstreamLevel(z, _.surfaceLevelsUnits).getOrElse(river.seaLevelUnits))
        val yBottom = yDownstreamSurface - clamped(1, dfs, MaxDepth)
        if(blockSetter.worldProvider.isSurfaceWorld) { // Set riverbed
            val bottomBlock =
                if(tfcLoaded || isBiomeOfType(baseBiomeAt(x, yBottom, z), COLD)) gravelBlockFor(x, yBottom, z)
                else sandBlockFor(x, yBottom, z)
            foreachDownFrom((x, yBottom, z), blockAt(_).isSoil, setBlockAndDataAt(_, bottomBlock, notifyNeighbors = false))
        }
        val liquidBlock = riverBlock(flowPlan(x)(z).get)
        for(y <- yBottom + 1 until yDownstreamSurface)
            setRiverBlockAt(x, y, z, liquidBlock) // Upwards, to simplify shoring-up
        setRiverBlockAt(x, yDownstreamSurface, z, liquidBlock, flowDecayAt(z))
        for(y <- yDownstreamSurface + 1 to ySurface)
            setBlockAt((x, y, z), liquid.flowingBlock, 8, notifyNeighbors = false) // Waterfall
    }

    private def clearBlockAt(xyz: XYZ)(implicit blockSetter: BlockSetter) {
        val block = blockAt(xyz)
        if(block != bedrock && !block.isInstanceOf[BlockRiver])
            deleteBlockAt(xyz) // In build step, will notify neighbors and shore up and crossing river
    }

    private def setRockBlockAt(xyz: XYZ)(implicit blockSetter: BlockSetter) {
        if(blockAt(xyz) != bedrock)
            setBlockAndDataAt(xyz, rockBlockFor(xyz.x, xyz.y, xyz.z), notifyNeighbors = false)
    }

    private def setRiverBlockAt(x: Int, y: Int, z: Int, newBlock: BlockRiver, flowDecay: Int = 0)(implicit blockSetter: BlockSetter) {
        blockAt(x, y, z) match {
            case block: FixedFlowBlock if block.getMaterial == liquid || block.getMaterial == Material.ice =>
                val (dx, dz, decay) = interpolate((block.dx, block.dz, dataAt(x, y, z)), (newBlock.dx, newBlock.dz, flowDecay))
                setBlockAndDataAt((x, y, z), (FixedFlowBlock(liquid, dx, dz), decay), notifyNeighbors = false)
            case block: Block if !(block == bedrock || block.getMaterial == Material.ice || (block.getMaterial == liquid && dataAt(x, y, z) == 0)) =>
                setBlockAndDataAt((x, y, z), (newBlock, flowDecay), notifyNeighbors = false)
            case _ =>
        }
    }

    private def riverBlock(flow: XZ): BlockRiver = {
        val (dx, dz) = effectiveFlow(flow.x, flow.z)
        FixedFlowBlock(liquid, dx, dz).asInstanceOf[BlockRiver]
    }

    private def effectiveFlow(dx: Int, dz: Int) = upstreamOrientation match { // account for component rotation
        case South => ( dx,  dz)
        case West  => (-dz,  dx)
        case North => ( dx, -dz)
        case East  => ( dz,  dx)
    }

    def isFlowOrShoreAt(wxz: XZ) = flows.contains(wxz) || shores.contains(wxz)

    private def adjustedFloorLevel(x: Int, yFloor: Int, z: Int) = intersectingFlowOrShoreSurfaceLevelAt(cs.xzWorld(x, z)) match {
        case Some(minLevel) => max(minLevel, yFloor)
        case None => yFloor
    }

    private def intersectingFlowOrShoreSurfaceLevelAt(wxz: XZ): Option[Int] =
        intersectingFlowOrShoreComponentAt(wxz).map{c =>
            val xz = c.cs.xzLocal(wxz)
            surfaceLevel(c.surfaceLevelsUnits(xz.z))
        }

    private def intersectingFlowOrShoreComponentAt(wxz: XZ): Option[RiverComponent] =
        if(shores.contains(wxz)) None
        else river.intersectingComponentsAt(wxz, _.boundingBox).find(c => c != this && c.isFlowOrShoreAt(wxz))

    private def distanceFromShore(x: Int, z: Int): Int = {
        val wx = cs.xWorld(x, z)
        val wz = cs.zWorld(x, z)
        if(shores.contains(wx, wz)) 0 else round(shores.map(s => distance(wx, wz, s.x, s.z)).min).toInt
    }

    private def surfaceLevelAt(x: Int, z: Int, dfs: Int) = {
        surfaceLevel(if(flowDefined(x, z)) surfaceLevelsUnits(z) else {
            val upstreamSurfaceLevelUnits = upstreamLevels(x, z, _.surfaceLevelsUnits, dfs)
            if(upstreamSurfaceLevelUnits.nonEmpty) upstreamSurfaceLevelUnits.max else clamped(surfaceLevelsUnits, z)
        }) // Forms v-shaped ridge around waterfalls
    }
}

object RiverComponent {

    val XModelPlanSize = 8
    val ZModelPlanSize = 8
    val XModelPlanMax = XModelPlanSize - 1
    val ZModelPlanMax = ZModelPlanSize - 1
    val ModelPlanRiverStartX = 2
    val ModelPlanRiverWidth  = 2
    val MaxStretch = 5
    val ShorePadding = 6
    val MidPadding = (ShorePadding + 1) / 2

    val ZPlanSize = ZModelPlanSize + MaxStretch * 2
    val XPlanSize = XModelPlanSize + MaxStretch * 2
    val XPlanMax = XPlanSize - 1
    val ZPlanMax = ZPlanSize - 1
    val XLine = 0 to XPlanMax
    val ZLine = 0 to ZPlanMax

    val MaxWidth = ModelPlanRiverWidth + MaxStretch * 2
    val MaxDepth = MaxWidth / 2

    val EachLocalPos: Seq[XZ] =
        for(x <- XLine; z <- ZLine) yield (x, z)

    val EachLocalPosWithPadding: Seq[XZ] =
        for(x <- -ShorePadding to XPlanMax + ShorePadding;
            z <- -ShorePadding to ZPlanMax + ShorePadding) yield (x, z)

    val MinTunnelHeight = 8
    val BaseTunnelCeilingThickness = 2
    val TunnelCeilingRandom = new Random
    
    val SurfacePlaceholder = cobblestone // Opaque and non-fertile

    val MinElevationForRatcheting = 6

    val MinSourceBackWallHeight = 2

    def surfaceLevelUnits(level: Int) = level*7 + 6
    def surfaceLevel(units: Int) = units / 7
    def flowDecay(units: Int) = 6 - (units % 7) // Maximum 6, so we don't "starve" waterfalls

    type Flow = Option[XZ]
    type FlowPlan = Array[Array[Flow]]

    def xFlowPlanLength(p: FlowPlan): Int = p.length
    def zFlowPlanLength(p: FlowPlan): Int = p(0).length

    def xFlowPlanMax(p: FlowPlan): Int = xFlowPlanLength(p) - 1
    def zFlowPlanMax(p: FlowPlan): Int = zFlowPlanLength(p) - 1

    def isWithin(p: FlowPlan, x: Int, z: Int) = x >= 0 && z >= 0 && x <= xFlowPlanMax(p) && z <= zFlowPlanMax(p)

    def modelPlan(flows: Flow*): FlowPlan = {
        assert(flows.length == XModelPlanSize * ZModelPlanSize)
        val plan = ofDim[Flow](XModelPlanSize, ZModelPlanSize)
        for(i <- 0 until flows.length) plan(i % XModelPlanSize)(i / XModelPlanSize) = flows(i)
        plan
    }

    val ___ = None
    val OOO = Some(( 0,  0))
    val SSS = Some(( 0,  2))
    val SSW = Some((-1,  2))
    val S_W = Some((-2,  2))
    val WSW = Some((-2,  1))
    val WWW = Some((-2,  0))
    val WNW = Some((-2, -1))
    val N_W = Some((-2, -2))
    val NNW = Some((-1, -2))
    val NNN = Some(( 0, -2))
    val NNE = Some(( 1, -2))
    val N_E = Some(( 2, -2))
    val ENE = Some(( 2, -1))
    val EEE = Some(( 2,  0))
    val ESE = Some(( 2,  1))
    val S_E = Some(( 2,  2))
    val SSE = Some(( 1,  2))

    def combine(flow1: Flow, flow2: Flow): Flow = {
        if(flow1.isEmpty) flow2 else if(flow2.isEmpty) flow1
        else Some((round((flow1.get.x + flow2.get.x).toFloat / 2F),
                   round((flow1.get.z + flow2.get.z).toFloat / 2F)))
    }
}
