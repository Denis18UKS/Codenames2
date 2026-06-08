package fable.codenames.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class XodKomandProjectorEntity extends HologramProjectorEntity {

    public XodKomandProjectorEntity(EntityType<? extends HologramProjectorEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setVelocity(Vec3d.ZERO);
    }

    @Override
    protected void initDataTracker() {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}

    @Override
    public boolean hasNoGravity() {
        return true;
    }
}