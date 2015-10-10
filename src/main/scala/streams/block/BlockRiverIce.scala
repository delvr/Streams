package streams.block

import cpw.mods.fml.common.registry.GameRegistry._
import farseek.util.Reflection._
import farseek.world._
import java.util.Random
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks._
import net.minecraft.world._

/** @author delvr */
class BlockRiverIce(val dx: Int, val dz: Int) extends BlockIce with FixedFlowBlock {

    cloneObject(classOf[BlockIce], ice, this)
    registerBlock(this, s"river/ice/$dx/$dz")

    override def updateTick(w: World, x: Int, y: Int, z: Int, random: Random) {
        implicit val world = w
        if(!w.isRemote && !isFreezing(x, y, z)) changeToWater(x, y, z)
    }

    override def removedByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean) = changeToWater(x, y, z)(world)

    override def harvestBlock(w: World, player: EntityPlayer, x: Int, y: Int, z: Int, data: Int) {}

    private def changeToWater(x: Int, y: Int, z: Int)(implicit w: World) = setBlockAt((x, y, z), FixedFlowBlock(Material.water, dx, dz), dataAt(x, y, z))
}
