package streams.world.gen.structure

import farseek.block._
import farseek.util._
import farseek.world.Direction._
import farseek.world.gen._
import farseek.world.gen.structure.Structure
import farseek.world.{Direction, _}
import java.util.Random
import net.minecraft.block.material.Material
import net.minecraft.world._
import net.minecraft.world.gen.structure.StructureBoundingBox
import scala.util.Random._
import streams.world.gen.structure.RiverComponent._

/** @author delvr */
class RiverStructure(generator: RiverGenerator, boundingBox: StructureBoundingBox, worldProvider: WorldProvider)
        extends Structure[RiverComponent](generator, boundingBox, worldProvider) {

    import streams.world.gen.structure.RiverStructure._

    val liquid = generator.liquid

    val seaLevel = liquid match {
        case Material.water => worldProvider.seaLevel.get
        case Material.lava => worldProvider.lavaLevel.get
    }
    val seaLevelUnits = surfaceLevelUnits(seaLevel)

    def generate()(implicit worldAccess: IBlockAccess, random: Random) {
        random.shuffle(CardinalDirections).foreach {
            case North => for(x <- slide(xMin, xMax, XPlanSize)) if(createMouth(South, XPlanSize, ZPlanSize, x, zMin + ShorePadding)) return
            case South => for(x <- slide(xMin, xMax, XPlanSize)) if(createMouth(North, XPlanSize, ZPlanSize, x, zMax - ZPlanSize - ShorePadding + 1)) return
            case West  => for(z <- slide(zMin, zMax, ZPlanSize)) if(createMouth(East , ZPlanSize, XPlanSize, xMin + ShorePadding, z)) return
            case East  => for(z <- slide(zMin, zMax, ZPlanSize)) if(createMouth(West , ZPlanSize, XPlanSize, xMax - XPlanSize - ShorePadding + 1, z)) return
        }
    }

    private def createMouth(downstreamOrientation: Direction, xSize: Int, zSize: Int, xMin: Int, zMin: Int)(implicit worldAccess: IBlockAccess, random: Random) = {
        val mouth = new RiverMouthComponent(this, sizedBox(xMin, zMin, xSize, zSize), downstreamOrientation, halfChance)
        mouth.isValid(worldAccess) && {
            mouth.addUpstream(worldAccess, random)
            val valid = this.isValid
            if(isValid) {
                mouth.adjustUpstream()
                mouth.debugPos(worldAccess)
            }
            else components.clear()
            valid
        }
    }

    def carveValleys(implicit blockArray: ChunkBlockArrayAccess, random: Random) {
        for(x <- blockArray.boundingBox.xs; z <- blockArray.boundingBox.zs) {
            val comps = intersectingComponentsAt((x, z), _.paddedBox)
            if(comps.nonEmpty) {
                yUpFrom(seaLevel + 1).find(!blockAt(x, _, z).isSolid).foreach { yAboveGround =>
                    comps.foreach(_.carveValleyAt(x, yAboveGround - 1, z))
                }
            }
        }
    }

    def isValid = components.size >= 10 && components.last.widthStretch >= MaxStretch - 2
}

object RiverStructure {
    private def slide(start: Int, end: Int, width: Int): Range = start + ShorePadding to end - ShorePadding - width + 1 by width / 2
}
