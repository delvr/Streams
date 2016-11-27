package streams

import net.minecraft.block.Block
import net.minecraft.client.renderer.BlockModelShapes
import net.minecraftforge.client.model.ModelLoader
import streams.block.FixedFlowBlock._

package object client {

  def onRegisterAllBlocks(shapes: BlockModelShapes): Unit = {
    ModelLoader.onRegisterAllBlocks(shapes)
    shapes.registerBuiltInBlocks(FixedFlowBlocks.values.map(_.asInstanceOf[Block]).toSeq:_*)
  }
}
