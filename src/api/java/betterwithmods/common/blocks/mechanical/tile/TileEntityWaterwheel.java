package betterwithmods.common.blocks.mechanical.tile;

import net.minecraft.util.math.BlockPos;

public abstract class TileEntityWaterwheel extends TileAxleGenerator {
    public abstract boolean isWater(BlockPos pos);
}
