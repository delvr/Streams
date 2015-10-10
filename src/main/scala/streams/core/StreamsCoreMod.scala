package streams.core

import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex
import farseek.core._

/** @author delvr */
@SortingIndex(value = FarseekCoreModSortIndex + 1)
class StreamsCoreMod extends FarseekBaseCoreMod {

    protected val transformerClasses = Seq(classOf[StreamsClassTransformer])
}

class StreamsClassTransformer extends MethodReplacementTransformer {

    implicit private val transformer = this

    protected val methodReplacements = Seq(
        MethodReplacement("net/minecraft/block/BlockLiquid", "getFlowDirection", "func_149802_a", "(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraft/block/material/Material;)D",
            "streams/block/BlockLiquidExtensions/getFlowDirection"),
        MethodReplacement("net/minecraft/world/World", "handleMaterialAcceleration", "func_72918_a", "(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z",
            "streams/entity/item/EntityBoatExtensions/handleMaterialAcceleration"),
        MethodReplacement("com/bioxx/tfc/WorldGen/TFCChunkProviderGenerate", "replaceBlocksForBiomeHigh", "(II[Lnet/minecraft/block/Block;Ljava/util/Random;[Lnet/minecraft/block/Block;[B)V",
            "streams/world/gen/TfcChunkGeneratorExtensions/replaceBlocksForBiomeHigh"),
        MethodReplacement("com/bioxx/tfc/Core/TFC_Core", "isFreshWater", "(Lnet/minecraft/block/Block;)Z",
            "streams/block/BlockLiquidExtensions/isFreshWater")
    )
}
