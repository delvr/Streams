package streams.world.gen.structure

import farseek.block._
import farseek.util._
import farseek.world._
import farseek.world.gen._
import java.util.Random
import net.minecraft.block.material.Material
import net.minecraft.world.IBlockAccess
import net.minecraft.world.gen.structure.StructureBoundingBox

/** @author delvr */
class RiverMouthComponent(river: RiverStructure, boundingBox: StructureBoundingBox, upstreamOrientation: Direction, isMirrored: Boolean)
        extends RiverComponent(river, boundingBox, upstreamOrientation, isMirrored) {

    import streams.world.gen.structure.RiverComponent._
    import streams.world.gen.structure.RiverMouthComponent._

    assert(river.boundingBox.contains(paddedBox))

    val downstreamComponent = None

    for(z <- ZLine) {
        maxSurfaceLevels(z) = river.seaLevel
        surfaceLevelsUnits(z) = river.seaLevelUnits
    }

    def isValid(implicit bac: IBlockAccess): Boolean = {
        for(x <- XLine) {
            if(x >= 2 && x <= XPlanMax - 2)
                if(blockAt(x, river.seaLevel, 0).material != liquid || blockAt(x, river.seaLevel - MinDepth + 1, 0).material != liquid ||
                        !blockAt(x, river.seaLevel, ZPlanMax).isSolid)
                    return false
        }
        true
    }

    def addUpstream(implicit bac: IBlockAccess, random: Random) {
        val modelPlan = randomElement(ModelPlans)
        newUpstreamComponent(straightOffset(modelPlan).get, upstreamOrientation, Nil, halfChance).foreach { upstream =>
            if(upstream.setMaxSurfaceLevels(river.seaLevel)) {
                upstream.addUpstream(river.seaLevelUnits, Seq(this)).foreach { branch =>
                    straightUpstream = Some(branch)
                    overlay(stretchNorthSouth(modelPlan), flowPlan)
                    widthStretch = branch.widthStretch
                    widen(flowPlan, widthStretch)
                    commit(bac, random)
                }
            }
        }
    }

    override protected def commit(implicit bac: IBlockAccess, random: Random) {
        super.commit(bac, random)
        for(x <- XLine; z <- ZLine) {
            val material = blockStateAt(x, river.seaLevel, z).block.material
            if(material.isLiquid || material == Material.ICE) flowPlan(x)(z) = None
        }
    }

    def debugPos(implicit bac: IBlockAccess) {
        val xyzDebug = upFrom(XPlanMax, river.seaLevel, ZPlanMax).find(xyz => !blockAt(xyz).isSolid).getOrElse((XPlanMax, river.seaLevel, ZPlanMax))
        info(s"River at ${cs.xyzWorld(xyzDebug).debug}")
    }
}

object RiverMouthComponent {

    import streams.world.gen.structure.RiverComponent._

    val MinDepth = 2

    private val ModelPlans = Array(
        modelPlan(
            ___,___,N_W,NNN,NNN,NNE,N_E,___,
            ___,___,N_W,NNN,NNN,N_E,___,___,
            ___,___,___,NNW,NNN,NNE,___,___,
            ___,___,N_E,NNE,NNN,NNE,___,___,
            ___,___,NNN,NNN,NNN,N_E,___,___,
            ___,___,N_W,NNW,NNN,___,___,___,
            ___,___,___,NNW,NNN,___,___,___,
            ___,___,___,NNN,NNN,___,___,___
        ),
        modelPlan(
            ___,___,NNN,NNN,NNN,NNN,N_E,___,
            ___,___,NNN,NNN,NNN,N_E,NNE,___,
            ___,___,NNW,NNN,N_W,NNE,N_E,___,
            ___,___,NNW,NNN,NNN,NNN,___,___,
            ___,___,N_W,NNW,NNN,NNE,___,___,
            ___,___,___,NNW,N_E,N_E,___,___,
            ___,___,___,NNN,NNE,___,___,___,
            ___,___,___,NNN,NNN,___,___,___
        )
    )
}
