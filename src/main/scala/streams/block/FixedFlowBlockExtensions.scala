package streams.block

import farseek.core.ReplacedMethod
import farseek.util.ImplicitConversions._
import farseek.world.{BlockAccess, _}
import java.lang.Math._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.init.Blocks
import net.minecraft.util._
import net.minecraft.world._
import net.minecraftforge.client.model.ModelLoader

/** @author delvr */
object FixedFlowBlockExtensions {

    def getFlowDirection(w: IBlockAccess, pos: BlockPos, material: Material,
                         super_getFlowDirection: ReplacedMethod[BlockLiquid]): Double = {
        if(w.isInstanceOf[World] || w.isInstanceOf[ChunkCache] || w.isInstanceOf[BlockAccess]) {
            blockAt(pos)(w) match {
                case block: BlockRiver => // Fixed flow
                    val flowVector = block.getFlowVector(w, pos)
                    if(flowVector.xCoord == 0D && flowVector.zCoord == 0D) -1000D
                    else atan2(flowVector.zCoord, flowVector.xCoord) - PI / 2D
                case _ => super_getFlowDirection(w, pos, material)
            }
        } else super_getFlowDirection(w, pos, material)
    }

    def onRegisterAllBlocks(shapes: BlockModelShapes, super_onRegisterAllBlocks: ReplacedMethod[ModelLoader]): Unit = {
      super_onRegisterAllBlocks(shapes)
      FixedFlowBlock.FixedFlowBlocks.values.foreach(block =>
        if(block.isInstanceOf[BlockRiver]) shapes.registerBuiltInBlocks(block))
    }

    def getModelResourceLocation(state: IBlockState, super_getModelResourceLocation: ReplacedMethod[DefaultStateMapper])
                                (implicit dsm: DefaultStateMapper): ModelResourceLocation = {
      if(state.getBlock.isInstanceOf[BlockRiverIce])
        new ModelResourceLocation(Block.blockRegistry.getNameForObject(Blocks.ice), null)
      else
        super_getModelResourceLocation(state)
    }
}
