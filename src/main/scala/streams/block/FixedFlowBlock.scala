package streams.block

import farseek.util._
import net.minecraft.block.Block
import net.minecraft.block.material.{MaterialLiquid, Material}
import net.minecraft.util.math.MathHelper
import scala.math._

/** @author delvr */
trait FixedFlowBlock extends Block {
    def dxFlow: Int
    def dzFlow: Int
}

object FixedFlowBlock {

    val FixedFlowBlocks: Map[(Material, Int, Int), FixedFlowBlock] =
        (for(material <- Seq(Material.WATER, Material.LAVA).map(_.asInstanceOf[MaterialLiquid]);
             dx <- -2 to 2; dz <- -2 to 2; if abs(dx) == 2 || abs(dz) == 2 || (dx == 0 && dz == 0))
        yield (material, dx, dz) -> new BlockRiver(material, dx, dz)).toMap

    def apply(material: Material): Seq[FixedFlowBlock] = FixedFlowBlocks.filter(_._1._1 == material).values.toSeq

    def apply(material: Material, flow: XZ): FixedFlowBlock = apply(material, flow.x, flow.z)

    def apply(material: Material, dx: Int   , dz: Int   ): FixedFlowBlock = FixedFlowBlocks((material, dx, dz))
    def apply(material: Material, dx: Double, dz: Double): FixedFlowBlock = {
        val (ndx, ndz) = normalize(dx, dz)
        apply(material, ndx, ndz)
    }

    def normalize(flow: XZ): XZ = normalize(flow.x, flow.z)
    def normalize(dx: Double, dz: Double): XZ = {
        if(dx == 0 && dz == 0)
            (0, 0)
        else {
            val dMax = MathHelper.abs_max(dx, dz)
            (round(dx / dMax * 2).toInt, round(dz / dMax * 2).toInt)
        }
    }

    def interpolate(flows: XZ*): (Double, Double) = {
        val (dxSum, dzSum) = flows.reduce((acc, v) => (acc.x + v.x, acc.z + v.z))
        val divisor = flows.size.toDouble
        (dxSum/divisor, dzSum/divisor)
    }

    def interpolate(flows: (Int, Int, Int)*): (Double, Double, Int) = {
        val (dxSum, dzSum, decaySum) = flows.reduce((acc, v) => (acc.x + v.x, acc.y + v.y, acc.z + v.z))
        val divisor = flows.size.toDouble
        (dxSum/divisor, dzSum/divisor, round(decaySum.toDouble/divisor).toInt)
    }
}
