package streams

import com.bioxx.tfc.Core.TFC_Core
import cpw.mods.fml.relauncher.Side._
import cpw.mods.fml.relauncher.SideOnly
import farseek.world.{BlockAccess, _}
import java.lang.Math._
import net.minecraft.block.{BlockLiquid, _}
import net.minecraft.block.material.Material
import net.minecraft.world._

package object block {

  @SideOnly(CLIENT)
  def getFlowDirection(w: IBlockAccess, x: Int, y: Int, z: Int, material: Material): Double = {
    if(w.isInstanceOf[World] || w.isInstanceOf[ChunkCache] || w.isInstanceOf[BlockAccess]) {
      blockAt(x, y, z)(w) match {
        case block: BlockRiver => // Fixed flow
          val flowVector = block.getFlowVector(w, x, y, z)
          if(flowVector.xCoord == 0D && flowVector.zCoord == 0D) -1000D
          else atan2(flowVector.zCoord, flowVector.xCoord) - PI / 2D
        case _ => BlockLiquid.getFlowDirection(w, x, y, z, material)
      }
    } else BlockLiquid.getFlowDirection(w, x, y, z, material)
  }

  def isFreshWater(block: Block): Boolean =
    (block.isInstanceOf[BlockRiver] && block.getMaterial == Material.water) || TFC_Core.isFreshWater(block)
}
