package streams.world.gen

import com.bioxx.tfc.WorldGen.TFCChunkProviderGenerate
import farseek.core.ReplacedMethod
import farseek.util.Reflection._
import farseek.util.XYZ
import farseek.world.BlockWriteAccess.NonWorldBlockWriteAccess
import farseek.world._
import farseek.world.gen._
import java.util.Random
import net.minecraft.block.Block
import net.minecraft.world.World
import scala.collection.mutable

/** @author delvr */
object TfcChunkGeneratorExtensions {

    import streams.world.gen.structure.RiverComponent._
    import streams.world.gen.structure.RiverGenerator._

    val xyzValleySurfaces = mutable.Buffer[XYZ]()

    def replaceBlocksForBiomeHigh(xChunk: Int, zChunk: Int, topBlocks: Array[Block], random: Random, allBlocks: Array[Block], allDatas: Array[Byte],
                                  super_replaceBlocksForBiomeHigh: ReplacedMethod[TFCChunkProviderGenerate])(implicit generator: TFCChunkProviderGenerate) {
        val worldProvider = chunkGeneratorWorldClassFields(generator.getClass).value[World](generator).provider
        xyzValleySurfaces.clear()
        surfaceWaterGenerator.onChunkGeneration(worldProvider, generator, xChunk, zChunk, topBlocks, null)
        super_replaceBlocksForBiomeHigh(xChunk, zChunk, topBlocks, random, allBlocks, allDatas)
        if(xyzValleySurfaces.nonEmpty) {
            val blockArray = new ChunkBlockArrayAccess(worldProvider, xChunk, zChunk, allBlocks, Some(allDatas), 0)
            for(xyz <- xyzValleySurfaces)
                setBlockAt(xyz, SurfacePlaceholder)(blockArray, NonWorldBlockWriteAccess)
        }
    }
}
