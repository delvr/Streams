package streams.world.gen.structure

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world.Direction._
import farseek.world.gen._
import farseek.world.{Direction, _}
import java.util.Random
import net.minecraft.world.IBlockAccess
import net.minecraft.world.gen.structure.StructureBoundingBox
import scala.math._
import streams.world.gen.structure.RiverComponent._

/** @author delvr */
class RiverUpstreamComponent(downstream: RiverComponent, boundingBox: StructureBoundingBox,
                             upstreamOrientation: Direction, isMirrored: Boolean)(implicit random: Random)
        extends RiverComponent(downstream.river, boundingBox, upstreamOrientation, isMirrored) {

    import streams.world.gen.structure.RiverUpstreamComponent._

    val downstreamComponent = Some(downstream)

    def isValid(uncommitted: Seq[RiverComponent])(implicit bac: IBlockAccess): Boolean =
        isValid && !uncommitted.exists(c => !downstream.downstreamComponent.contains(c) && c.intersectsWith(boundingBox))

    override def isValid: Boolean =
        paddedBox.isWithin(river) && !river.intersectingComponents(boundingBox, _.boundingBox).exists(_.shores.exists(boundingBox.contains))

    def addUpstream(minSurfaceLevelUnits: Int, uncommitted: Seq[RiverComponent])(implicit bac: IBlockAccess, random: Random): Option[RiverUpstreamComponent] = {
        val yMinSurfaceLevel = surfaceLevel(minSurfaceLevelUnits)
        val straightModel = randomElement(StraightModelPlans)
        val curvedModel = randomElement(CurvedModelPlans)
        val sortedCandidates = upstreamCandidates(straightModel, curvedModel, uncommitted).filter(
            _.setMaxSurfaceLevels(yMinSurfaceLevel)).sortBy(_.roofLevels(ZPlanMax))
        sortedCandidates.lastOption.foreach { highest =>
            val newUncommitted = this +: uncommitted
            val highBranch = highest.addUpstream(upstreamMinSurfaceLevelUnits(highest, minSurfaceLevelUnits), newUncommitted)
            highBranch.foreach(setBranch)
            sortedCandidates.find(highBranch.isEmpty || _.upstreamOrientation != highBranch.get.upstreamOrientation).foreach { lowest =>
                lowest.addUpstream(upstreamMinSurfaceLevelUnits(lowest, minSurfaceLevelUnits), newUncommitted).foreach(setBranch)
            }
        }
        straightUpstream.foreach { straightBranch =>
            val stretchedPlan = stretchNorthSouth(straightModel)
            overlay(stretchedPlan, flowPlan)
            widen(flowPlan, straightBranch.widthStretch)
            widthStretch = straightBranch.widthStretch
        }
        curvedUpstream.foreach { curvedBranch =>
            val stretchedPlan = stretchEastWest(stretchNorthSouth(curvedModel))
            widen(stretchedPlan, curvedBranch.widthStretch)
            if(widthStretch >= 0 && abs(widthStretch - curvedBranch.widthStretch) <= 2 && max(widthStretch, curvedBranch.widthStretch) < MaxStretch) {
                widen(stretchedPlan, max(0, widthStretch - curvedBranch.widthStretch) + 1, eastWest = true, northSouth = false)
                widthStretch = max(widthStretch, curvedBranch.widthStretch) + 1
            } else
                widthStretch = max(widthStretch, curvedBranch.widthStretch)
            overlay(stretchedPlan, flowPlan)
        }
        if(widthStretch < 0 && maxSurfaceLevels(ZModelPlanMax) >= yMinSurfaceLevel + minSourceBackWallHeight(world.getWorldInfo) && isValid(uncommitted)) {
            overlay(randomElement(SourceModelPlans), flowPlan)
            widthStretch = 0
        }
        if(widthStretch >= 0) {
            commit(bac, random)
            Some(this)
        } else None
    }

    private def upstreamMinSurfaceLevelUnits(upstream: RiverUpstreamComponent, minSurfaceLevelUnits: Int) = {
        val heightDiff = surfaceLevelUnits(upstream.maxSurfaceLevels.sum / upstream.maxSurfaceLevels.length) - minSurfaceLevelUnits
        if(heightDiff > surfaceLevelUnits(MinElevationForRatcheting))
            minSurfaceLevelUnits + heightDiff/6
        else minSurfaceLevelUnits // Make sure MinRatchet >= is BackWallHeight * 2
    }

    private def setBranch(branch: RiverUpstreamComponent) {
        if(branch.upstreamOrientation == this.upstreamOrientation) straightUpstream = Some(branch)
        else curvedUpstream = Some(branch)
    }

    private def upstreamCandidates(straightModel: FlowPlan, curvedModel: FlowPlan, uncommitted: Seq[RiverComponent])(implicit bac: IBlockAccess, random: Random) = {
        val curveDirection = if(upstreamOrientation == North || upstreamOrientation == South) mirrored(East) else mirrored(South)
        Seq(
            newUpstreamComponent(straightOffset(straightModel).get, upstreamOrientation, uncommitted, mirrored = false),
            newUpstreamComponent(straightOffset(straightModel).get, upstreamOrientation, uncommitted, mirrored = true ),
            newUpstreamComponent(  curvedOffset(  curvedModel).get,      curveDirection, uncommitted, mirrored = false),
            newUpstreamComponent(  curvedOffset(  curvedModel).get,      curveDirection, uncommitted, mirrored = true )
        ).flatten
    }

    def setMaxSurfaceLevels(yMinSurfaceLevel: Int)(implicit bac: IBlockAccess): Boolean = { // Note: flow plan has not been overlaid yet
        for(z <- ZLine) {
            maxSurfaceLevels(z) = Int.MaxValue
            for(x <- XLine) {
                if(!blockAt(x, yMinSurfaceLevel, z).isSolid)
                    return false
                val yMaxSurfaceLevel = yUpFrom(yMinSurfaceLevel + 1).find(!blockAt(x, _, z).isSolid).getOrElse(cs.yLocal(bac.yMax)) - 1
                if(yMaxSurfaceLevel < maxSurfaceLevels(z))
                    maxSurfaceLevels(z) = yMaxSurfaceLevel
            }
        }
        true
    }
}

object RiverUpstreamComponent {

    def modelPlan(flows: Flow*): FlowPlan = {
        val plan = RiverComponent.modelPlan(flows:_*)
        for(x <- plan.indices) {
            assert(plan(x)(0).isDefined == plan(x)(1).isDefined) // Always allow waterfalls at z == 0, otherwise components would need to check downstream for flat ranges
            assert(if(x < ModelPlanRiverStartX || x >= ModelPlanRiverStartX + ModelPlanRiverWidth) plan(x)(0).isEmpty else plan(x)(0).isDefined)
        }
        plan
    }

    private val StraightModelPlans = Array(
        modelPlan(
            ___,___,NNN,NNN,___,___,___,___,
            ___,___,NNW,NNW,___,___,___,___,
            ___,___,N_W,NNW,N_W,___,___,___,
            ___,___,___,N_W,NNW,N_W,___,___,
            ___,___,___,___,NNW,N_W,___,___,
            ___,___,___,___,N_W,NNN,N_W,___,
            ___,___,___,___,___,NNW,NNW,___,
            ___,___,___,___,___,NNW,NNN,___
        ),
        modelPlan(
            ___,___,NNN,NNN,___,___,___,___,
            ___,___,N_E,NNE,___,___,___,___,
            ___,N_E,NNN,___,___,___,___,___,
            ___,N_W,N_W,WWW,N_W,___,___,___,
            ___,___,N_W,N_W,WWW,N_W,___,___,
            ___,___,___,___,N_W,N_W,N_W,___,
            ___,___,___,___,___,NNN,N_E,___,
            ___,___,___,___,N_E,N_E,___,___
        )
    )

    private val CurvedModelPlans = Array(
        modelPlan(
            ___,___,NNN,NNN,___,___,___,___,
            ___,___,N_W,NNW,___,___,___,___,
            ___,___,___,N_W,N_W,___,___,___,
            ___,___,___,___,N_W,N_W,___,___,
            ___,___,___,___,___,N_W,N_W,___,
            ___,___,___,___,___,___,N_W,WNW,
            ___,___,___,___,___,___,___,N_W,
            ___,___,___,___,___,___,___,___
        )
    )

    private val SourceModelPlans = Array(
        modelPlan(
            ___,___,NNN,NNN,___,___,___,___,
            ___,___,N_W,NNW,___,___,___,___,
            ___,___,___,N_W,N_W,___,___,___,
            ___,___,___,NNN,WNW,N_W,___,___,
            ___,___,ENE,N_E,___,NNW,___,___,
            ___,___,NNE,___,___,N_W,N_W,___,
            ___,___,NNN,___,___,___,NNW,___,
            ___,___,___,___,___,___,NNN,___
        ),
        modelPlan(
            ___,___,NNN,NNN,___,___,___,___,
            ___,___,NNE,NNW,___,___,___,___,
            ___,N_E,N_E,N_W,N_W,___,___,___,
            ___,NNE,___,___,NNW,N_W,___,___,
            ___,NNN,___,N_E,N_E,NNW,___,___,
            ___,___,___,NNE,___,N_W,N_W,___,
            ___,___,N_E,N_E,___,___,NNW,___,
            ___,___,NNN,___,___,___,NNN,___
        )
    )
}
