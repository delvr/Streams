package streams.entity.item

import farseek.core.ReplacedMethod
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.item.EntityBoat.Status._
import net.minecraft.util.math.BlockPos._
import net.minecraft.util.math.MathHelper._
import streams.block.BlockRiver

/** @author delvr */
object EntityBoatExtensions {

  def getUnderwaterStatus(super_getUnderwaterStatus: ReplacedMethod[EntityBoat])
                         (implicit boat: EntityBoat): EntityBoat.Status = {
    val box = boat.getEntityBoundingBox
    val yTop = box.maxY + 0.001D
    val xMin =   floor_double    (box.minX)
    val xMax = ceiling_double_int(box.maxX)
    val yMin =   floor_double    (box.maxY)
    val yMax = ceiling_double_int(yTop)
    val zMin =   floor_double    (box.minZ)
    val zMax = ceiling_double_int(box.maxZ)
    var underWater = false
    val pos = PooledMutableBlockPos.retain
    try for(x <- xMin until xMax; y <- yMin until yMax; z <- zMin until zMax) {
      pos.set(x, y, z)
      val state = boat.worldObj.getBlockState(pos)
      if(state.getMaterial == Material.WATER && yTop < EntityBoat.getLiquidHeight(state, boat.worldObj, pos).toDouble) {
        if(state.getBlock.isInstanceOf[BlockRiver])
          return IN_WATER
        if(state.getValue(BlockLiquid.LEVEL).intValue != 0)
          return UNDER_FLOWING_WATER
        underWater = true
      }
    } finally pos.release()
    if(underWater) UNDER_WATER else null
  }
}
