package streams

import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
import farseek.world._
import java.lang.Math._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math._
import net.minecraft.world._
import net.minecraftforge.fml.relauncher.Side._
import net.minecraftforge.fml.relauncher.SideOnly

package object block {

    private lazy val immersiveEngineeringUtilsGetFlowVector =
        Class.forName("blusunrize.immersiveengineering.common.util.Utils").getMethod("getFlowVector", classOf[World], classOf[BlockPos])

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

    def getFlowVector(world: World, pos: BlockPos): Vec3d = {
        implicit val w = world
        val state = blockStateAt(pos)
        state.getBlock match {
            case block: BlockRiver => block.getFlow(world, pos, state)
            case _ => immersiveEngineeringUtilsGetFlowVector(null, world, pos)
        }
    }

}
