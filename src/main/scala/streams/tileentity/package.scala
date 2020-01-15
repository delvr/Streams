package streams

import betterwithmods.common.blocks.mechanical.tile._
import betterwithmods.util.DirUtils._
import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
import farseek.util._
import farseek.world._
import net.minecraft.util.EnumFacing.Axis._
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import scala.math._
import streams.block.BlockRiver

package object tileentity {

    private lazy val oldIsWater = classOf[TileEntityWaterwheel].getDeclaredMethod("isWater", classOf[BlockPos])

    def isWater(e: TileEntityWaterwheel, pos: BlockPos): Boolean =
        (oldIsWater(e, pos): Boolean) || blockAt(pos)(e.getWorld).isInstanceOf[BlockRiver]

    def isWater(w: World, pos: BlockPos): Boolean =
        TileEntityWaterwheel.isWater(w, pos) || blockAt(pos)(w).isInstanceOf[BlockRiver]

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
