package streams.core

import farseek.core._
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex

/** @author delvr */
@SortingIndex(value = FarseekCoreModSortIndex + 1)
class StreamsCoreMod extends FarseekBaseCoreMod {

    protected val transformerClasses = Seq(classOf[StreamsClassTransformer])
}

class StreamsClassTransformer extends MethodReplacementTransformer {

    implicit private val transformer = this

    protected val methodReplacements = Seq(
        MethodReplacement("net/minecraft/block/BlockLiquid", "getFlowDirection", "func_180689_a",
            "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/material/Material;)D",
            "streams/block/FixedFlowBlockExtensions/getFlowDirection"),
        MethodReplacement("net/minecraft/world/World", "handleMaterialAcceleration", "func_72918_a",
            "(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z",
            "streams/entity/item/EntityBoatExtensions/handleMaterialAcceleration"),
        MethodReplacement("net/minecraftforge/client/model/ModelLoader", "onRegisterAllBlocks",
            "(Lnet/minecraft/client/renderer/BlockModelShapes;)V",
            "streams/block/FixedFlowBlockExtensions/onRegisterAllBlocks"),
        MethodReplacement("net/minecraft/client/renderer/block/statemap/DefaultStateMapper", "getModelResourceLocation", "func_178132_a",
            "(Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/client/resources/model/ModelResourceLocation;",
            "streams/block/FixedFlowBlockExtensions/getModelResourceLocation")
    )
}
