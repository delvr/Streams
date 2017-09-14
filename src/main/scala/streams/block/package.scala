package streams

import java.lang.Math._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world._
import net.minecraftforge.fml.relauncher.Side._
import net.minecraftforge.fml.relauncher.SideOnly

package object block {

  @SideOnly(CLIENT)
  def getSlopeAngle(w: IBlockAccess, pos: BlockPos, material: Material, state: IBlockState): Float = {
      state.getBlock match {
          case block: BlockRiver => // Fixed flow
              val flowVector = block.getFlow(w, pos, state)
              if(flowVector.x == 0D && flowVector.z == 0D) -1000F
              else (atan2(flowVector.z, flowVector.x) - PI / 2D).toFloat
          case _ => BlockLiquid.getSlopeAngle(w, pos, material, state)
      }
  }
}
