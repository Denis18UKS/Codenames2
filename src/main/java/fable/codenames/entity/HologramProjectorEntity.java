package fable.codenames.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HologramProjectorEntity extends Entity {
    public HologramProjectorEntity(EntityType<? extends HologramProjectorEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRender(double distance) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setVelocity(Vec3d.ZERO);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
