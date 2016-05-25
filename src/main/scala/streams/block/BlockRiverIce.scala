package streams.block

import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
import farseek.world._
import java.util.Random
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks._
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world._
import net.minecraftforge.fml.common.registry.GameRegistry._
import net.minecraftforge.fml.common.registry.RegistryDelegate

/** @author delvr */
class BlockRiverIce(val dx: Int, val dz: Int) extends BlockIce with FixedFlowBlock {

    cloneObject(classOf[BlockIce], ICE, this, _.getType == classOf[RegistryDelegate[Block]])
    blockState = createBlockState
    setDefaultState(blockState.getBaseState)
    registerBlock(this, null, s"river/ice/$dx/$dz")

    override def updateTick(w: World, pos: BlockPos, state: IBlockState, random: Random) {
        implicit val world = w
        if(!w.isRemote && !isFreezing(pos)) changeToWater(pos)
    }

    override def removedByPlayer(state: IBlockState, world: World, pos: BlockPos, player: EntityPlayer, willHarvest: Boolean) = changeToWater(pos)(world)

    override def harvestBlock(w: World, player: EntityPlayer, pos: BlockPos, state: IBlockState, tileEntity: TileEntity, stack: ItemStack) {}

    private def changeToWater(pos: BlockPos)(implicit w: World) = setBlockAt(pos, FixedFlowBlock(Material.WATER, dx, dz), dataAt(pos))
}
