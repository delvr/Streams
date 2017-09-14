package streams

import farseek.FarseekBaseMod
import farseek.world._
import net.minecraft.block.material._
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event._
import net.minecraftforge.fml.relauncher.Side._
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

    @EventHandler override def handle(event: FMLLoadCompleteEvent) {
        super.handle(event)
        if(event.getSide == CLIENT) {
          info("Welcome to Farseek/Streams. Please visit http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2346379-streams-real-flowing-rivers for information and updates.")
          info("Shader configuration: if you wish to use shaders with Streams river blocks, put the block.properties file from this mod's jar into your shaderpack's 'shaders' directory.")
          info("(Requires OptiFine version HD_U_B8 or later.)")
        }
    }
}
