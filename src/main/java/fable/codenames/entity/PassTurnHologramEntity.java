package fable.codenames.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PassTurnHologramEntity extends Entity {

    private static final TrackedData<Integer> DIRECTION = DataTracker.registerData(PassTurnHologramEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    private BlockPos attachedButtonPos;
    private boolean visible = true;

    @SuppressWarnings("unchecked")
    public PassTurnHologramEntity(EntityType<?> type, World world) {
        super((EntityType<? extends Entity>) type, world);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    // Основной конструктор с игроком
    public PassTurnHologramEntity(World world, BlockPos buttonPos, PlayerEntity player) {
        this(ModMiscEntityTypes.PASS_TURN_HOLOGRAM.getEntityType(), world);
        this.attachedButtonPos = buttonPos;
        this.setPosition(
                buttonPos.getX() + 0.5,
                buttonPos.getY() + 0.8,
                buttonPos.getZ() + 0.5
        );

        if (player != null) {
            Direction dir = Direction.fromRotation(player.getYaw());
            setFixedDirection(dir);
            this.setYaw(player.getYaw());
        }
    }

    // Конструктор без игрока
    public PassTurnHologramEntity(World world, BlockPos buttonPos) {
        this(ModMiscEntityTypes.PASS_TURN_HOLOGRAM.getEntityType(), world);
        this.attachedButtonPos = buttonPos;
        this.setPosition(
                buttonPos.getX() + 0.5,
                buttonPos.getY() + 0.8,
                buttonPos.getZ() + 0.5
        );
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(DIRECTION, Direction.NORTH.getId());
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

    public Direction getFixedDirection() {
        return Direction.byId(this.dataTracker.get(DIRECTION));
    }

    public void setFixedDirection(Direction direction) {
        this.dataTracker.set(DIRECTION, direction.getId());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("ButtonX")) {
            this.attachedButtonPos = new BlockPos(
                    nbt.getInt("ButtonX"),
                    nbt.getInt("ButtonY"),
                    nbt.getInt("ButtonZ")
            );
        }

        // Читаем направление из NBT
        if (nbt.contains("FixedDirection")) {
            setFixedDirection(Direction.byId(nbt.getInt("FixedDirection")));
        }
        
        if (nbt.contains("face")) {
            String faceStr = nbt.getString("face").toUpperCase();
            try {
                setFixedDirection(Direction.valueOf(faceStr));
            } catch (IllegalArgumentException e) {
                // Оставляем текущее направление
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        if (attachedButtonPos != null) {
            nbt.putInt("ButtonX", attachedButtonPos.getX());
            nbt.putInt("ButtonY", attachedButtonPos.getY());
            nbt.putInt("ButtonZ", attachedButtonPos.getZ());
        }
        nbt.putInt("FixedDirection", getFixedDirection().getId());
    }
}