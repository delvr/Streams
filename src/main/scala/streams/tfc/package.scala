package streams

import com.bioxx.tfc.Core.TFC_Climate._
import com.bioxx.tfc.Core.TFC_Core._
import com.bioxx.tfc.Core._
import com.bioxx.tfc.WorldGen.TFCBiome._
import com.bioxx.tfc.WorldGen._
import farseek.block.BlockAndData
import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
import farseek.util.{XZ, _}
import farseek.world.gen.chunkGeneratorWorldClassFields
import java.lang.reflect.Field
import java.util.Random
import net.minecraft.block.Block
import net.minecraft.world._
import net.minecraft.world.biome.BiomeGenBase
import streams.block.BlockRiver
import streams.world.gen.structure.RiverGenerator._

package object tfc {

    private val replaceHighBlocksMethod = classOf[TFCChunkProviderGenerate].getDeclaredMethods.find(
    _.getName == "replaceBlocksForBiomeHigh").get.accessible
    private val getBiome = classOf[TFCChunkProviderGenerate].getDeclaredMethods.find(_.getName == "getBiome").get.accessible

    private val layerFields = classFields[Array[DataLayer], TFCChunkProviderGenerate]
    private val rockLayer1 = layerFields.find(_.getName == "rockLayer1").get
    private val rainfallLayer = layerFields.find(_.getName == "rainfallLayer").get

    def replaceBlocksForBiomeHigh(generator: TFCChunkProviderGenerate, xChunk: Int, zChunk: Int, topBlocks: Array[Block],
                                  random: Random, allBlocks: Array[Block], allDatas: Array[Byte]) {
        replaceHighBlocksMethod(generator, xChunk, zChunk, topBlocks, random, allBlocks, allDatas)
        val world = chunkGeneratorWorldClassFields(generator.getClass).value[World](generator).asInstanceOf[WorldServer]
        surfaceWaterGenerator.onChunkGeneration(world, generator, xChunk, zChunk, allBlocks, allDatas)
    }

    def tfcGeneratingSurfaceBlockAt(wxz: XZ, world: World, sedimentOnly: Boolean = false): BlockAndData = {
        val generator = world.asInstanceOf[WorldServer].theChunkProviderServer.currentChunkProvider.asInstanceOf[TFCChunkProviderGenerate]
        val biome = getBiome(generator, wxz.x & 15, wxz.z & 15).asInstanceOf[TFCBiome]
        val soilData = getSoilMeta(tfcLayerAt(wxz, rockLayer1, generator).data1)
        val rain = tfcLayerAt(wxz, rainfallLayer, generator).floatdata1
        tfcSurfaceBlockAt(wxz, world, biome, soilData, rain, sedimentOnly)
    }

    def tfcPopulatingSurfaceBlockAt(wxz: XZ, world: World, sedimentOnly: Boolean = false): BlockAndData = {
        val biome = world.getBiomeGenForCoords(wxz.x, wxz.z)
        val soilData = getSoilMeta(getCacheManager(world).getRockLayerAt(wxz.x, wxz.z, 0).data1)
        val rain = getRainfall(world, wxz.x, 0, wxz.z)
        tfcSurfaceBlockAt(wxz, world, biome, soilData, rain, sedimentOnly)
    }

    private def tfcSurfaceBlockAt(wxz: XZ, world: World, biome: BiomeGenBase, soilData: Int, rain: Float, sedimentOnly: Boolean): BlockAndData = {
        val temp = TFC_Climate.getBioTemperature(world, wxz.x, wxz.z)
        if((rain < 125f && (temp < 1.5f || (temp > 20f && biome.heightVariation < 0.5f))) ||
            biome == BEACH || biome == OCEAN || biome == DEEP_OCEAN) getTypeForSand(soilData)
        else if(biome == GRAVEL_BEACH || sedimentOnly) getTypeForGravel(soilData)
        else getTypeForGrassWithRain(soilData, rain)
    }

    def tfcGeneratingRockAt(wxz: XZ, world: World): BlockAndData = {
        val generator = world.asInstanceOf[WorldServer].theChunkProviderServer.currentChunkProvider.asInstanceOf[TFCChunkProviderGenerate]
        val layer = tfcLayerAt(wxz, rockLayer1, generator)
        (layer.block, layer.data2)
    }

    private def tfcLayerAt(wxz: XZ, layerField: Field, generator: TFCChunkProviderGenerate): DataLayer = {
        layerField(generator).asInstanceOf[Array[DataLayer]]((wxz.z & 15) + (wxz.x & 15)*16)
    }

    def isFreshWater(block: Block): Boolean = block.isInstanceOf[BlockRiver] || TFC_Core.isFreshWater(block)

}
