package betterwithmods.common.blocks.mechanical.tile;

import net.minecraft.tileentity.TileEntity;

public abstract class TileAxleGenerator extends TileEntity {
    public float waterMod = 0;
    public abstract void calculatePower();
    public abstract void setPower(byte power);
}
