package streams.block

import farseek.core.ReplacedMethod
import java.lang.Math._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world._
import net.minecraftforge.client.model.ModelLoader

/** @author delvr */
object FixedFlowBlockExtensions {

    def getSlopeAngle(w: IBlockAccess, pos: BlockPos, material: Material, state: IBlockState,
                      super_getSlopeAngle: ReplacedMethod[BlockLiquid]): Float = {
        state.getBlock match {
            case block: BlockRiver => // Fixed flow
                val flowVector = block.getFlow(w, pos, state)
                if(flowVector.xCoord == 0D && flowVector.zCoord == 0D) -1000F
                else (atan2(flowVector.zCoord, flowVector.xCoord) - PI / 2D).toFloat
            case _ => super_getSlopeAngle(w, pos, material, state)
        }
    }

    def onRegisterAllBlocks(shapes: BlockModelShapes, super_onRegisterAllBlocks: ReplacedMethod[ModelLoader]): Unit = {
      super_onRegisterAllBlocks(shapes)
      shapes.registerBuiltInBlocks(FixedFlowBlock.FixedFlowBlocks.values.filter(_.isInstanceOf[BlockRiver]).map(
        _.asInstanceOf[Block]).toSeq:_*) // The cast to Block is _necessary_
    }

    def getModelResourceLocation(state: IBlockState, super_getModelResourceLocation: ReplacedMethod[DefaultStateMapper])
                                (implicit dsm: DefaultStateMapper): ModelResourceLocation = {
      if(state.getBlock.isInstanceOf[BlockRiverIce])
        new ModelResourceLocation(Block.REGISTRY.getNameForObject(Blocks.ICE), null)
      else
        super_getModelResourceLocation(state)
    }
}
