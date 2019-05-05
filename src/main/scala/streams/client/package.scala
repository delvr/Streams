package streams

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockModelShapes
import net.minecraft.client.renderer.color._
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.biome.BiomeColorHelper
import net.minecraftforge.client.model.ModelLoader
import streams.block.FixedFlowBlock._

package object client {

  def onRegisterAllBlocks(shapes: BlockModelShapes): Unit = {
    ModelLoader.onRegisterAllBlocks(shapes)
    shapes.registerBuiltInBlocks(FixedFlowBlocks.values.map(_.asInstanceOf[Block]).toSeq:_*)
  }

  def setupRiverBlockColors(): Unit = {
    val colors = Minecraft.getMinecraft.getBlockColors
    val handler = new IBlockColor {
      override def colorMultiplier(state: IBlockState, worldIn: IBlockAccess, pos: BlockPos, tintIndex: Int): Int =
        if(worldIn != null && pos != null) BiomeColorHelper.getWaterColorAtPos(worldIn, pos) else -1
    }
    FixedFlowBlocks.values.foreach(colors.registerBlockColorHandler(handler, _))
  }
}
