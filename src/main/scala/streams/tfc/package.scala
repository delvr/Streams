package streams

import com.bioxx.tfc.Core.TFC_Climate._
import com.bioxx.tfc.Core.TFC_Core._
import com.bioxx.tfc.Core._
import com.bioxx.tfc.Entities.EntityFallingBlockTFC
import com.bioxx.tfc.WorldGen.Generators.WorldGenFissure
import com.bioxx.tfc.WorldGen.TFCBiome._
import farseek.block.BlockAndData
import farseek.util._
import farseek.world._
import java.util.Random
import net.minecraft.block.Block
import net.minecraft.world._
import streams.block.BlockRiver
import streams.world.gen.structure.RiverGenerator._

package object tfc {

    def generate(generator: WorldGenFissure, world: World, random: Random, x: Int, y: Int, z: Int) {
        if(between(y, y+10).forall(!blockAt(x, _, z)(world).getMaterial.isLiquid))
            generator.generate(world, random, x, y, z)
    }

    def generateStreams(generator: com.bioxx.tfc.WorldGen.TFCChunkProviderGenerate, world: World, xChunk: Int, zChunk: Int, blocks: Array[Block], datas: Array[Byte]) {
        surfaceWaterGenerator.onChunkGeneration(world.asInstanceOf[WorldServer], generator, xChunk, zChunk, blocks, datas)
    }

    def tfcSurfaceBlockAt(wxz: XZ, world: World, sedimentOnly: Boolean = false): BlockAndData = {
        val biome = world.getBiomeGenForCoords(wxz.x, wxz.z)
        val rain = getRainfall(world, wxz.x, 0, wxz.z)
        val soilData = getCacheManager(world).getRockLayerAt(wxz.x, wxz.z, 0).data1
        val soilBlock =
            if(biome == BEACH || biome == OCEAN || biome == DEEP_OCEAN) getTypeForSand(soilData)
            else if(biome == GRAVEL_BEACH || sedimentOnly) getTypeForGravel(soilData)
            else getTypeForGrassWithRain(soilData, rain)
        (soilBlock, getSoilMeta(soilData))
    }

    def isFreshWater(block: Block): Boolean = block.isInstanceOf[BlockRiver] || TFC_Core.isFreshWater(block)

    def canReplace(fallingBlock: EntityFallingBlockTFC, world: World, x: Int, y: Int, z: Int): Boolean = {
        if(!fallingBlock.isDead)
            false
        else {
            if(!blockAt(x, y, z)(world).isInstanceOf[BlockRiver])
                fallingBlock.canReplace(world, x, y, z)
            else {
                fallingBlock.shouldDropItem = true
                false
            }
        }
    }
}
