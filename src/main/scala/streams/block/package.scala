package streams

import java.lang.Math._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world._
import net.minecraftforge.fml.relauncher.Side._
import net.minecraftforge.fml.relauncher.SideOnly
import scala.math.{atan2 => _}

package object block {

  @SideOnly(CLIENT)
  def getSlopeAngle(w: IBlockAccess, pos: BlockPos, material: Material, state: IBlockState): Float = {
      state.getBlock match {
          case block: BlockRiver => // Fixed flow
              val flowVector = block.getFlow(w, pos, state)
              if(flowVector.xCoord == 0D && flowVector.zCoord == 0D) -1000F
              else (atan2(flowVector.zCoord, flowVector.xCoord) - PI / 2D).toFloat
          case _ => BlockLiquid.getSlopeAngle(w, pos, material, state)
      }
  }
}
