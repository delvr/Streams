package streams

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event._
import farseek.FarseekBaseMod
import farseek.block._
import farseek.world._
import net.minecraft.block.Block._
import net.minecraft.block.material.{MaterialLiquid, Material}
import streams.block._
import streams.world.gen.structure.RiverGenerator

/** @author delvr */
@Mod(modid = "streams", modLanguage = "scala", useMetadata = true, dependencies = "after:terrafirmacraft")
object StreamsMod extends FarseekBaseMod {

  val configuration = None

  override val existingWorldWarning = Some("Can cause unexpected behavior such as incomplete rivers.")

  @EventHandler def handle(event: FMLPreInitializationEvent) {
    FixedFlowBlock.getClass // Register river blocks
    RiverGenerator.surfaceWaterGenerator = new RiverGenerator(Material.water.asInstanceOf[MaterialLiquid], SurfaceDimensionId)
  }

    @EventHandler override def handle(event: FMLLoadCompleteEvent) {
        super.handle(event)
        val customWaterIds = allBlocks.filter(_.getMaterial == Material.water).map(getIdFromBlock).toSeq.sorted.map("mc_Entity.x == " + _).mkString( " || ")
        info("Welcome to Farseek/Streams. Please visit http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2346379-streams-real-flowing-rivers for information and updates.")
        info("Shader configuration: if you wish to use shaders with custom water blocks including Streams river blocks, edit your shader's gbuffers_wqater.wsh file to replace the line:\n" +
                s"if (mc_Entity.x == 8 || mc_Entity.x == 9) {\n" +
                "with the following line:\n" +
                s"if ($customWaterIds) {\n" +
                "Note that these ids will change with every new combination of loaded mods.")
    }
}
