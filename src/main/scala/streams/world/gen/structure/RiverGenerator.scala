package streams.world.gen.structure

import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import farseek.world.gen._
import farseek.world.gen.structure._
import java.util.Random
import net.minecraft.block.Block
import net.minecraft.block.material._
import net.minecraft.world.WorldType._
import net.minecraft.world._
import net.minecraft.world.chunk.ChunkPrimer
import net.minecraft.world.gen.structure._

/** @author delvr */
class RiverGenerator(val liquid: MaterialLiquid, dimensionId: Int) extends StructureGenerator[RiverStructure](-1, dimensionId) {

    private val riverSizeChunksBinLog = 4
    private val riverSize = 16 << riverSizeChunksBinLog

    override protected val invalidWorldTypes = Set(FLAT)

    private def iRiverChunk(iChunk: Int) = (iChunk >> riverSizeChunksBinLog) << riverSizeChunksBinLog // Partition world into 16x16 chunk zones, 1 river each

    def riverKey(xChunk: Int, zChunk: Int) = (iRiverChunk(xChunk), iRiverChunk(zChunk))

    override protected def generate(xChunk: Int, zChunk: Int, primer: ChunkPrimer)(implicit worldAccess: IBlockAccess) {
        val xRiverChunk = iRiverChunk(xChunk)
        val zRiverChunk = iRiverChunk(zChunk)
        val riverKey = (xRiverChunk, zRiverChunk)
        if(!structures.contains(riverKey)) {
            implicit val random = chunkRandom(xRiverChunk, zRiverChunk)(worldAccess.worldProvider)
            val bounds = sizedBox(xRiverChunk*16, zRiverChunk*16, riverSize, riverSize)
            val riverOption = createStructure(bounds).flatMap { river =>
                river.generate(worldAccess, random)
                if(river.isValid) {
                    river.commit()
                    debug(river.debug)
                    Some(river)
                } else {
                    river.clear()
                    None
                }
            }
            structures(riverKey) = riverOption
        }
        structures.get(riverKey).foreach(_.foreach{ river =>
            val worldProvider = worldAccess.worldProvider
            val blockArray = new ChunkBlockArrayAccess(worldProvider, xChunk, zChunk, primer)
            river.carveValleys(blockArray, chunkRandom(xChunk, zChunk)(worldProvider)) // 1st pass, before chunk terrain replacement
        })
    }

    def createStructure(boundingBox: StructureBoundingBox)(implicit world: IBlockAccess, random: Random): Option[RiverStructure] =
        Some(new RiverStructure(this, boundingBox, world))

    override protected def build(xChunk: Int, zChunk: Int)(implicit world: WorldServer, random: Random) {
        Set(riverKey(xChunk, zChunk), riverKey(xChunk + 1, zChunk), riverKey(xChunk, zChunk + 1), riverKey(xChunk + 1, zChunk + 1)).foreach { key =>
            structures.get(key).foreach(_.foreach(_.build(new PopulatingArea(xChunk, zChunk, world), random)))
        }
    }
}

object RiverGenerator {
    var surfaceWaterGenerator: RiverGenerator = _
}
