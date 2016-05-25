package streams.block

import farseek.block._
import farseek.block.material._
import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
import farseek.util._
import farseek.world.Direction._
import farseek.world._
import farseek.world.biome._
import java.util.Random
import net.minecraft.block._
import net.minecraft.block.material._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.MathHelper._
import net.minecraft.util._
import net.minecraft.world._
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fml.common.registry.GameRegistry._
import net.minecraftforge.fml.common.registry.RegistryDelegate
import net.minecraftforge.fml.relauncher.Side._
import net.minecraftforge.fml.relauncher.SideOnly
import scala.math._

/** @author delvr */
// IFluidBlock implementation is mostly for Entity.isInsideOfMaterial calculations.
// We don't use it as a base for various reasons, mostly because height percentage don't line up with normal water.
class BlockRiver(liquid: MaterialLiquid, val dx: Int, val dz: Int) extends BlockLiquid(liquid) with IFluidBlock with FixedFlowBlock {

    import streams.block.FixedFlowBlock._

    private val baseFlowVector = new Vec3(dx, 0, dz)

    cloneObject(classOf[BlockLiquid], liquid.stillBlock, this, _.getType == classOf[RegistryDelegate[Block]])
    blockState = createBlockState
    setDefaultState(blockState.getBaseState)
    //setTickRandomly(liquid == Material.water) // For freezing
    registerBlock(this, null, s"river/$getUnlocalizedName/$dx/$dz")

    override def onBlockAdded(w: World, pos: BlockPos, state: IBlockState) {
        if(populating)
            shoreUp(pos)(w)
        else
            liquid.flowingBlock.onBlockAdded(w, pos, state) // Checks for hardening & schedules update
    }

    override def onNeighborBlockChange(w: World, pos: BlockPos, state: IBlockState, formerNeighbor: Block) {
        if(formerNeighbor.isSolidOrLiquid) {
            implicit val world = w
            if(populating)
                shoreUp(pos)
            else {
                liquid.flowingBlock.onNeighborBlockChange(w, pos, state, formerNeighbor) // Checks for hardening
                if(blockAt(pos) == this && (!blockBelow(pos).isSolidOrLiquid || neighbors(pos).map(blockAt(_)).exists(!_.isSolidOrLiquid)))
                    w.scheduleUpdate(pos, this, tickRate(w))
            }
        }
    }

    private def shoreUp(xyz: XYZ)(implicit w: World) {
        stabilize(xyz.below)
        allNeighbors(xyz.x, xyz.y, xyz.z).foreach(stabilize)
    }

    private def stabilize(xyz: XYZ)(implicit w: World) {
        val block = blockAt(xyz)
        if(!block.isSolidOrLiquid || (block.isGranular && !blockBelow(xyz).isSolid))
            setBlockAndDataAt(xyz, rockBlockFor(xyz.x, xyz.y, xyz.z), notifyNeighbors = false)
    }

    override def breakBlock(w: World, pos: BlockPos, state: IBlockState) {
        implicit val world = w
        if(!w.scheduledUpdatesAreImmediate && blockAt(pos).isSolid) { // Prevent river damming from block generation or fallen sand/gravel etc.
            if(populating || entityPresent(pos, classOf[EntityFallingBlock]) || blocksFallInstantlyAt(pos))
                setBlockAt(pos, state, notifyNeighbors = false)
        }
    }

    override def getFlowVector(w: IBlockAccess, pos: BlockPos): Vec3 = { // Normalized in World.handleMaterialAcceleration()
        implicit val world = w
        if(dataAt(pos) == 0)
            baseFlowVector
        else {
            val fallingNeighborDirections = CompassDirections.filter(d => blockAt(pos.add(d.x, 1, d.z)).getMaterial == liquid)
            if(fallingNeighborDirections.isEmpty)
                baseFlowVector + super.getFlowVector(w, pos)
            else { // Avoid water rising up counter-flow to meet waterfall
            val combinedDirection = fallingNeighborDirections.reduce(_ + _)
                val dMax = abs_max(combinedDirection.x.toDouble, combinedDirection.z.toDouble)
                new Vec3(-round(combinedDirection.x / dMax * 2).toInt, 0, -round(combinedDirection.z / dMax * 2).toInt)
            }
        }
    }

    override def updateTick(w: World, pos: BlockPos, state: IBlockState, random: Random) {
        if(!w.isRemote) {
            implicit val world = w
            val data = dataAt(pos)
            tryToFlowInto(pos.below, data, isBelow = true)
            pos.neighbors.foreach(tryToFlowInto(_, data))
            liquid.stillBlock.updateTick(w, pos, state, random) // Set stuff on fire
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

    @SideOnly(CLIENT)
    override def randomDisplayTick(w: World, pos: BlockPos, state: IBlockState, random: Random) {
        super.randomDisplayTick(w, pos, state, random)
        if(liquid == Material.water && dataAt(pos)(w) == 0 && oneChanceOutOf(64)(random))
            w.playSound(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5, "liquid.water", random.nextFloat*0.25f + 0.75f, random.nextFloat + 0.5f, false)
    }

    val getFluid = liquid.fluid

    def drain(w: World, pos: BlockPos, doDrain: Boolean) = null

    def canDrain(w: World, pos: BlockPos) = false

    def getFilledPercentage(w: World, pos: BlockPos) = {
        implicit val world = w
        val data = dataAt(pos)
        val decay = if(data != 0 && allNeighbors(pos).exists(blockAbove(_).getMaterial == liquid)) 0f else data.toFloat
        (8f - decay) / 9f
    }
}
