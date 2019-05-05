package streams

import farseek.FarseekBaseMod
import farseek.world._
import net.minecraft.block.material._
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event._
import net.minecraftforge.fml.common._
import net.minecraftforge.fml.relauncher.Side
import streams.block._
import streams.world.gen.structure.RiverGenerator

/** @author delvr */
@Mod(modid = "streams", modLanguage = "scala", useMetadata = true)
object StreamsMod extends FarseekBaseMod {

    val configuration = None

    override val existingWorldWarning = Some("Can cause unexpected behavior such as incomplete rivers.")

    @EventHandler def handle(event: FMLPreInitializationEvent) {
        FixedFlowBlock.getClass // Register river blocks
        RiverGenerator.surfaceWaterGenerator = new RiverGenerator(Material.WATER.asInstanceOf[MaterialLiquid], SurfaceDimensionId)
    }

    @EventHandler def handle(event: FMLInitializationEvent) =
        if(FMLCommonHandler.instance.getSide == Side.CLIENT) streams.client.setupRiverBlockColors()
}
