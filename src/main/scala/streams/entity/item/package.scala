package streams.entity

import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.item.EntityBoat.Status._
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos
import net.minecraft.util.math.MathHelper._
import streams.block.BlockRiver

package object item {

  def getUnderwaterStatus(boat: EntityBoat): EntityBoat.Status = {
    val box = boat.getEntityBoundingBox
    val yTop = box.maxY + 0.001D
    val xMin = floor(box.minX)
    val xMax = ceil (box.maxX)
    val yMin = floor(box.maxY)
    val yMax = ceil (yTop)
    val zMin = floor(box.minZ)
    val zMax = ceil (box.maxZ)
    var underWater = false
    val pos = PooledMutableBlockPos.retain
    try for(x <- xMin until xMax; y <- yMin until yMax; z <- zMin until zMax) {
      pos.setPos(x, y, z)
      val state = boat.world.getBlockState(pos)
      if(state.getMaterial == Material.WATER && yTop < BlockLiquid.getLiquidHeight(state, boat.world, pos).toDouble) {
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
