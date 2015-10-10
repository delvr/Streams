package streams.block

import com.bioxx.tfc.Entities.EntityFallingBlockTFC
import cpw.mods.fml.common.registry.GameRegistry._
import cpw.mods.fml.relauncher.Side._
import cpw.mods.fml.relauncher._
import farseek.block._
import farseek.block.material._
import farseek.util.Reflection._
import farseek.util._
import farseek.world.Direction._
import farseek.world._
import farseek.world.biome._
import java.util.Random
import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialLiquid}
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.MathHelper._
import net.minecraft.util.Vec3._
import net.minecraft.world._
import net.minecraftforge.fluids.IFluidBlock
import scala.math._

/** @author delvr */
// IFluidBlock implementation is mostly for Entity.isInsideOfMaterial calculations.
// We don't use it as a base for various reasons, mostly because height percentage don't line up with normal water.
class BlockRiver(liquid: MaterialLiquid, val dx: Int, val dz: Int) extends BlockLiquid(liquid) with IFluidBlock with FixedFlowBlock {

    import streams.block.FixedFlowBlock._

    private val baseFlowVector = createVectorHelper(dx, 0, dz)

    cloneObject(classOf[BlockLiquid], liquid.stillBlock, this)
    setTickRandomly(liquid == Material.water) // For freezing
    registerBlock(this, s"river/$getUnlocalizedName/$dx/$dz")

    override def onBlockAdded(w: World, x: Int, y: Int, z: Int) {
        if(populating)
            shoreUp(x, y, z)(w)
        else
            liquid.flowingBlock.onBlockAdded(w, x, y, z) // Checks for hardening & schedules update
    }

    override def onNeighborBlockChange(w: World, x: Int, y: Int, z: Int, formerNeighbor: Block) {
        if(formerNeighbor.isSolidOrLiquid) {
            implicit val world = w
            if(populating)
                shoreUp(x, y, z)
            else {
                liquid.flowingBlock.onNeighborBlockChange(w, x, y, z, formerNeighbor) // Checks for hardening
                if(blockAt(x, y, z) == this && (!blockBelow(x, y, z).isSolidOrLiquid || neighbors(x, y, z).map(blockAt).exists(!_.isSolidOrLiquid)))
                    w.scheduleBlockUpdate(x, y, z, this, tickRate(w))
            }
        }
    }

    private def shoreUp(xyz: XYZ)(implicit w: World) {
        stabilize(xyz.below)
        allNeighbors(xyz.x, xyz.y, xyz.z).foreach(stabilize) // All 8 neighbors, to prevent diagonally placed mid-air TFC stone blocks from dropping as items
    }

    private def stabilize(xyz: XYZ)(implicit w: World) {
        val block = blockAt(xyz)
        if(!block.isSolidOrLiquid || (block.isGranular && !blockBelow(xyz).isSolid))
            setBlockAndDataAt(xyz, rockBlockFor(xyz.x, xyz.y, xyz.z), notifyNeighbors = false)
    }

    override def breakBlock(w: World, x: Int, y: Int, z: Int, block: Block, data: Int) {
        implicit val world = w
        val xyz = (x, y, z)
        if(!w.scheduledUpdatesAreImmediate && blockAt(xyz).isSolid) { // Prevent river damming from block generation or fallen sand/gravel etc.
            if(populating || entityPresent(xyz, classOf[EntityFallingBlock]) ||
                    (tfcLoaded && entityPresent(xyz, classOf[EntityFallingBlockTFC])) || blocksFallInstantlyAt(xyz))
                setBlockAt(xyz, block, data, notifyNeighbors = false)
        }
    }

    override def getFlowVector(w: IBlockAccess, x: Int, y: Int, z: Int) = { // Normalized in World.handleMaterialAcceleration()
        implicit val world = w
        if(dataAt(x, y, z) == 0)
            baseFlowVector
        else {
            val fallingNeighborDirections = CompassDirections.filter(d => blockAbove(x + d.x, y, z + d.z).getMaterial == liquid)
            if(fallingNeighborDirections.isEmpty)
                baseFlowVector + super.getFlowVector(w, x, y, z)
            else { // Avoid water rising up counter-flow to meet waterfall
            val combinedDirection = fallingNeighborDirections.reduce(_ + _)
                val dMax = abs_max(combinedDirection.x.toDouble, combinedDirection.z.toDouble)
                createVectorHelper(-round(combinedDirection.x / dMax * 2).toInt, 0, -round(combinedDirection.z / dMax * 2).toInt)
            }
        }
    }

    override def updateTick(w: World, x: Int, y: Int, z: Int, random: Random) {
        if(!w.isRemote) {
            implicit val world = w
            val xyz = (x, y, z)
            val data = dataAt(xyz)
            tryToFlowInto(xyz.below, data, isBelow = true)
            xyz.neighbors.foreach(tryToFlowInto(_, data))
            if(!tfcLoaded)
                liquid.stillBlock.updateTick(w, x, y, z, random) // Set stuff on fire
            //tryFreezing(x, y, z)
        }
    }

    private def tryToFlowInto(xyz: XYZ, decay: Int, isBelow: Boolean = false)(implicit w: World) {
        if(!blockAt(xyz).isSolidOrLiquid) {
            val riverNeighbors = xyz.neighbors.flatMap{case(nx, ny, nz) => blockAt(nx, xyz.y, nz) match {
                case riverBlock: FixedFlowBlock => Some((riverBlock.dx, riverBlock.dz, dataAt(nx, ny, nz)))
                case _ => None
            }}
            if(riverNeighbors.size >= 2) {
                val (ndx, ndz, nDecay) = interpolate(riverNeighbors:_*)
                displace(xyz, FixedFlowBlock(liquid, ndx, ndz), nDecay)
            } else if(isBelow)
                displace(xyz, liquid.flowingBlock, 8)
            else if(decay < 7)
                displace(xyz, liquid.flowingBlock, decay + 1)
        }
    }

    def tryFreezing(xyz: XYZ)(implicit w: World) {
        if(liquid == Material.water && isFreezing(xyz) && !blockAbove(xyz).isLiquid &&
                xyz.neighbors.exists(blockAt(_).isSolid) && takeDownFrom(xyz, blockAt(_).isLiquid).length <= 2)
                setBlockAt(xyz, FixedFlowBlock(Material.ice, dx, dz))
    }

    override def onEntityCollidedWithBlock(w: World, x: Int, y: Int, z: Int, entity: Entity) {
        liquid.stillBlock.onEntityCollidedWithBlock(w, x, y, z, entity) // for TFC
    }

    @SideOnly(CLIENT)
    override def colorMultiplier(w: IBlockAccess, x: Int, y: Int, z: Int) = liquid.stillBlock.colorMultiplier(w, x, y, z) // for TFC

    @SideOnly(CLIENT)
    override def randomDisplayTick(w: World, x: Int, y: Int, z: Int, random: Random) {
        super.randomDisplayTick(w, x, y, z, random)
        if(liquid == Material.water && dataAt(x, y, z)(w) == 0 && oneChanceOutOf(64)(random))
            w.playSound(x + 0.5, y + 0.5, z + 0.5, "liquid.water", random.nextFloat*0.25f + 0.75f, random.nextFloat + 0.5f, false)
    }

    val getFluid = liquid.fluid

    def drain(w: World, x: Int, y: Int, z: Int, doDrain: Boolean) = null

    def canDrain(w: World, x: Int, y: Int, z: Int) = false

    def getFilledPercentage(w: World, x: Int, y: Int, z: Int) = {
        implicit val world = w
        val data = dataAt(x, y, z)
        val decay = if(data != 0 && allNeighbors(x, y, z).exists(blockAbove(_).getMaterial == liquid)) 0f else data.toFloat
        (8f - decay) / 9f
    }
}
