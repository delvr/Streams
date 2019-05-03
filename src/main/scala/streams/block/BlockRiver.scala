package streams.block

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
import farseek.util._
import farseek.world.Direction._
import farseek.world._
import farseek.world.biome._
import java.util.Random
import net.minecraft.block.BlockLiquid._
import net.minecraft.block._
import net.minecraft.block.material._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item._
import net.minecraft.init.SoundEvents
import net.minecraft.util._
import net.minecraft.util.math._
import net.minecraft.world._
import net.minecraftforge.fml.common.registry._
import net.minecraftforge.fml.relauncher.Side._
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.registries.IRegistryDelegate
import scala.math._

/** @author delvr */
class BlockRiver(liquid: MaterialLiquid, val dxFlow: Int, val dzFlow: Int) extends BlockLiquid(liquid) with FixedFlowBlock {

    import streams.block.FixedFlowBlock._

    private val baseFlowVector = new Vec3d(dxFlow, 0, dzFlow)

    cloneObject(classOf[BlockLiquid], getStaticBlock(liquid), this, f => f.getType == classOf[IRegistryDelegate[Block]] || f.getType == classOf[ResourceLocation])
    blockState = createBlockState
    setDefaultState(blockState.getBaseState)
    ForgeRegistries.BLOCKS.register(setRegistryName("streams", s"river/${getStaticBlock(liquid).getTranslationKey}/$dxFlow/$dzFlow"))

    override def onBlockAdded(w: World, pos: BlockPos, state: IBlockState) {
        if(populating)
            shoreUp(pos)(w)
        else
            getFlowingBlock(liquid).onBlockAdded(w, pos, state) // Checks for hardening & schedules update
    }

    override def neighborChanged(state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block, neighborPos: BlockPos) {
        if(formerNeighbor.isSolidOrLiquid) {
            implicit val world = w
            if(populating)
                shoreUp(pos)
            else {
                getFlowingBlock(liquid).neighborChanged(state, w, pos, formerNeighbor, neighborPos) // Checks for hardening
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

    override def getFlow(w: IBlockAccess, pos: BlockPos, state: IBlockState): Vec3d = { // Normalized in World.handleMaterialAcceleration()
        if(blockStateMetadata(state) == 0)
            baseFlowVector
        else {
            val fallingNeighborDirections = CompassDirections.filter(d => w.getBlockState(pos.add(d.x, 1, d.z)).getBlock.material == liquid)
            if(fallingNeighborDirections.isEmpty)
                baseFlowVector + super.getFlow(w, pos, state)
            else { // Avoid water rising up counter-flow to meet waterfall
            val combinedDirection = fallingNeighborDirections.reduce(_ + _)
                val dMax = MathHelper.absMax(combinedDirection.x.toDouble, combinedDirection.z.toDouble)
                new Vec3d(-round(combinedDirection.x / dMax * 2).toInt, 0, -round(combinedDirection.z / dMax * 2).toInt)
            }
        }
    }

    override def updateTick(w: World, pos: BlockPos, state: IBlockState, random: Random) {
        if(!w.isRemote) {
            implicit val world = w
            val data = dataAt(pos)
            tryToFlowInto(pos.below, data, isBelow = true)
            pos.neighbors.foreach(tryToFlowInto(_, data))
            getStaticBlock(liquid).updateTick(w, pos, state, random) // Set stuff on fire
            //tryFreezing(x, y, z)
        }
    }

    private def tryToFlowInto(xyz: XYZ, decay: Int, isBelow: Boolean = false)(implicit w: World) {
        if(!blockAt(xyz).isSolidOrLiquid) {
            val riverNeighbors = xyz.neighbors.flatMap{case(nx, ny, nz) => blockAt(nx, xyz.y, nz) match {
                case riverBlock: FixedFlowBlock => Some((riverBlock.dxFlow, riverBlock.dzFlow, dataAt(nx, ny, nz)))
                case _ => None
            }}
            if(riverNeighbors.size >= 2) {
                val (ndx, ndz, nDecay) = interpolate(riverNeighbors:_*)
                displace(xyz, FixedFlowBlock(liquid, ndx, ndz), nDecay)
            } else if(isBelow)
                displace(xyz, getFlowingBlock(liquid), 8)
            else if(decay < 7)
                displace(xyz, getFlowingBlock(liquid), decay + 1)
        }
    }

    @SideOnly(CLIENT)
    override def randomDisplayTick(state: IBlockState, w: World, pos: BlockPos, random: Random) {
        super.randomDisplayTick(state, w, pos, random)
        if(liquid == Material.WATER && dataAt(pos)(w) == 0 && oneChanceOutOf(64)(random))
            w.playSound(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, random.nextFloat * 0.25F + 0.75F, random.nextFloat + 0.5F, false)
    }
}
