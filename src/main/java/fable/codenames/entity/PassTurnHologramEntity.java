package fable.codenames.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PassTurnHologramEntity extends Entity {

    private BlockPos attachedButtonPos;
    private boolean visible = true;

    @SuppressWarnings("unchecked")
    public PassTurnHologramEntity(EntityType<?> type, World world) {
        super((EntityType<? extends Entity>) type, world);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    public PassTurnHologramEntity(World world, BlockPos buttonPos) {
        this(ModMiscEntityTypes.PASS_TURN_HOLOGRAM.getEntityType(), world);
        this.attachedButtonPos = buttonPos;
        this.setPosition(buttonPos.getX() + 0.5, buttonPos.getY() + 0.8, buttonPos.getZ() + 0.5);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setVelocity(0, 0, 0);
        
        if (attachedButtonPos != null && !visible) {
            this.discard();
        }
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            this.discard();
        }
    }

    @Override
    protected void initDataTracker() {}

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("ButtonX") && nbt.contains("ButtonY") && nbt.contains("ButtonZ")) {
            this.attachedButtonPos = new BlockPos(
                nbt.getInt("ButtonX"),
                nbt.getInt("ButtonY"),
                nbt.getInt("ButtonZ")
            );
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        if (attachedButtonPos != null) {
            nbt.putInt("ButtonX", attachedButtonPos.getX());
            nbt.putInt("ButtonY", attachedButtonPos.getY());
            nbt.putInt("ButtonZ", attachedButtonPos.getZ());
        }
    }
}