package streams.world.gen

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.storage._
import net.minecraftforge.fml.common.FMLCommonHandler
import streams.StreamsMod
import streams.world.gen.structure.RiverComponent._

package object structure {

  private val BackWallHeightTagKey = "backWallHeight"

  def handleWorldDataLoad(fmlHandler: FMLCommonHandler, saveHandler: SaveHandler, worldInfo: WorldInfo, tags: NBTTagCompound): Unit = {
    fmlHandler.handleWorldDataLoad(saveHandler, worldInfo, tags)
    minSourceBackWallHeights(worldInfo) = tags.getCompoundTag(StreamsMod.id).getInteger(BackWallHeightTagKey) // default 0
  }

  def handleWorldDataSave(fmlHandler: FMLCommonHandler, saveHandler: SaveHandler, worldInfo: WorldInfo, tags: NBTTagCompound): Unit = {
    fmlHandler.handleWorldDataSave(saveHandler, worldInfo, tags)
    val streamsTags = tags.getCompoundTag(StreamsMod.id)
    streamsTags.setInteger(BackWallHeightTagKey, minSourceBackWallHeight(worldInfo))
    tags.setTag(StreamsMod.id, streamsTags)
  }
}
