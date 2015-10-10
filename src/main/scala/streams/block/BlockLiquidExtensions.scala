package streams.block

import com.bioxx.tfc.Core.TFC_Core
import farseek.core.ReplacedMethod
import farseek.world._
import java.lang.Math._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.world.{ChunkCache, World, IBlockAccess}
import farseek.world.BlockAccess

/** @author delvr */
object BlockLiquidExtensions {

    def getFlowDirection(w: IBlockAccess, x: Int, y: Int, z: Int, material: Material,
                         super_getFlowDirection: ReplacedMethod[BlockLiquid]): Double = {
        if(w.isInstanceOf[World] || w.isInstanceOf[ChunkCache] || w.isInstanceOf[BlockAccess]) {
            blockAt(x, y, z)(w) match {
                case block: BlockRiver => // Fixed flow
                    val flowVector = block.getFlowVector(w, x, y, z)
                    if(flowVector.xCoord == 0D && flowVector.zCoord == 0D) -1000D
                    else atan2(flowVector.zCoord, flowVector.xCoord) - PI / 2D
                case _ => super_getFlowDirection(w, x, y, z, material)
            }
        } else super_getFlowDirection(w, x, y, z, material)
    }

    def isFreshWater(block: Block, super_isFreshWater: ReplacedMethod[TFC_Core]): Boolean =
        (block.isInstanceOf[BlockRiver] && block.getMaterial == Material.water) || super_isFreshWater(block)
}
