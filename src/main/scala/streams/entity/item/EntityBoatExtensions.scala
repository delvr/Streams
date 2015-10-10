package streams.entity.item

import farseek.block._
import farseek.core.ReplacedMethod
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world.Direction._
import farseek.world._
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.entity._
import net.minecraft.entity.item.EntityBoat
import net.minecraft.util.MathHelper._
import net.minecraft.util._
import net.minecraft.world._
import scala.math._
import streams.block.BlockRiver

/** @author delvr */
object EntityBoatExtensions {

    final val speedLimit = 0.2d
    final val singleDirectionSpeedLimit = sqrt(speedLimit*speedLimit/2d) - 0.01d

    def handleMaterialAcceleration(entityBox: AxisAlignedBB, material: Material, entity: Entity,
                                   super_handleMaterialAcceleration: ReplacedMethod[World])(implicit world: World): Boolean = {
        entity match {
            case boat: EntityBoat =>
                boat.isBoatEmpty = false // Inspired by jonathan2520 on https://bugs.mojang.com/browse/MC-2931
                val xMin = floor_double(boat.minX) //- 1
                val xMax = floor_double(boat.maxX + 1d) //+ 1
                val yMin = floor_double(boat.minY)
                val yMax = floor_double(boat.maxY + 1d)
                val zMin = floor_double(boat.minZ) //- 1
                val zMax = floor_double(boat.maxZ + 1d) //+ 1
                world.checkChunksExist(xMin, yMin, zMin, xMax, yMax, zMax) && {
                    var submerged = false
                    val yMinForSubmerge = floor_double(entityBox.minY)
                    val yMaxForSubmerge = floor_double(entityBox.maxY + 1d)
                    val vector = Vec3.createVectorHelper(0d, 0d, 0d)
                    for(x <- xMin until xMax; z <- zMin until zMax) {
                        for(y <- yMin until yMax) {
                            val block = blockAt(x, y, z)
                            if(block.getMaterial == material && yMax >= ((y + 1).toFloat - BlockLiquid.getLiquidHeightPercent(dataAt(x, y, z)))) {
                                boat.riddenByEntity match {
                                    case e: EntityLivingBase if e.moveForward != 0f || e.moveStrafing != 0f =>
                                    case _ =>
                                        if(block.isInstanceOf[BlockRiver] || dataAt(x, y, z) != 0) {
                                            block.velocityToAddToEntity(world, x, y, z, boat, vector)
                                            for(d <- CompassDirections) {
                                                if(takeDownFrom((x + d.x, y, z + d.z), blockAt(_).isLiquid).length < takeDownFrom((x, y, z), blockAt(_).isLiquid).length) {
                                                    vector.xCoord -= d.x
                                                    vector.zCoord -= d.z
                                                }
                                            }
                                        }
                                }
                            }
                        }
                        if(!submerged) {
                            for(y <- yMinForSubmerge until yMaxForSubmerge) {
                                if(blockAt(x, y, z).getMaterial == material && yMaxForSubmerge >= ((y + 1).toFloat - BlockLiquid.getLiquidHeightPercent(dataAt(x, y, z))))
                                    submerged = true
                            }
                        }
                    }
                    if(vector.lengthVector > 0d) {
                        val normalizedVector = vector.normalize
                        val limit = 0.014d
                        boat.motionX += normalizedVector.xCoord * limit
                        boat.motionY += normalizedVector.yCoord * limit
                        boat.motionZ += normalizedVector.zCoord * limit
                        if(hypotenuse(boat.motionX, boat.motionZ) > speedLimit) { // Whoa Nelly
                            boat.motionX = clamp_double(boat.motionX, -singleDirectionSpeedLimit, singleDirectionSpeedLimit)
                            boat.motionZ = clamp_double(boat.motionZ, -singleDirectionSpeedLimit, singleDirectionSpeedLimit)
                        }
                    }
                    submerged
                }
            case _ => super_handleMaterialAcceleration(entityBox, material, entity)
        }
    }
}
