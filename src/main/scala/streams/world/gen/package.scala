package streams.world

import com.bioxx.tfc.WorldGen.TFCChunkProviderGenerate
import farseek.util._
import net.minecraft.world.WorldProvider
import net.minecraft.world.chunk.IChunkProvider

package object gen {

    private lazy val customTfc = classOf[TFCChunkProviderGenerate].getDeclaredMethods.exists(_.getName == "generateStreams")

    def createChunkGenerator(provider: WorldProvider): IChunkProvider =
        if(tfcLoaded && provider.isSurfaceWorld && !customTfc)
            new streams.tfc.TFCChunkProviderGenerate(provider.worldObj, provider.worldObj.getSeed, provider.worldObj.getWorldInfo.isMapFeaturesEnabled)
        else provider.createChunkGenerator
}
