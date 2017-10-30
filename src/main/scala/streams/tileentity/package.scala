package streams

import betterwithmods.common.blocks.mechanical.tile._
import betterwithmods.util.DirUtils._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import net.minecraft.util.EnumFacing.Axis._
import net.minecraft.util.math.BlockPos
import scala.math._
import streams.block.BlockRiver

package object tileentity {

    def isWater(e: TileEntityWaterwheel, pos: BlockPos): Boolean = {
        implicit val w = e.getWorld
        e.isWater(pos) || blockAt(pos).isInstanceOf[BlockRiver]
    }

    def calculatePower(e: TileAxleGenerator): Unit = {
        e match {
            case wheel: TileEntityWaterwheel =>
                implicit val w = e.getWorld
                blockAt(e.getPos - 2) match {
                    case br: BlockRiver =>
                        val flowAxis = if(blockStateAt(e.getPos).getValue(AXIS) == X) br.dzFlow else br.dxFlow
                        e.waterMod = if(flowAxis == 2) -1 else if(flowAxis == -2) 1 else 0
                        e.setPower(abs(e.waterMod).toByte)
                    case _ => e.calculatePower()
                }
            case _ => e.calculatePower()
        }
    }
}
