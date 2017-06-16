package streams.world

import farseek.util._
import net.minecraft.world.WorldProvider
import net.minecraft.world.chunk.IChunkProvider
import streams.tfc.TFCChunkProviderGenerate

package object gen {

    def createChunkGenerator(provider: WorldProvider): IChunkProvider =
        if(tfcLoaded) new TFCChunkProviderGenerate(provider.worldObj, provider.worldObj.getSeed, provider.worldObj.getWorldInfo.isMapFeaturesEnabled)
        else provider.createChunkGenerator
}
