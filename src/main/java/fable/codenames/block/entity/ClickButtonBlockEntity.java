package fable.codenames.block.entity;

import fable.codenames.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ClickButtonBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation CLICK = RawAnimation.begin().thenPlay("click_button");
    private static final RawAnimation START_CLICK = RawAnimation.begin().thenPlay("animation");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Mode mode = Mode.NORMAL;

    public ClickButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CLICK_BUTTON_BLOCK_ENTITY.getBlockEntityType(), pos, state);
    }

    public void triggerClick() {
        triggerAnim("button", isStartButtonVisual() ? "start_click" : "click");
    }

    public boolean isStartButtonVisual() {
        return getCachedState().isOf(ModBlocks.START_BUTTON.getBlock());
    }

    public Mode getMode() {
        return this.mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Mode", this.mode.getId());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.mode = Mode.byId(nbt.getString("Mode"));
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "button", state -> PlayState.STOP)
                .triggerableAnim("click", CLICK)
                .triggerableAnim("start_click", START_CLICK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public enum Mode {
        NORMAL("normal"),
        RESET("reset"),
        PASS_TURN("pass_turn");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public static Mode byId(String id) {
            for (Mode mode : values()) {
                if (mode.id.equalsIgnoreCase(id)) {
                    return mode;
                }
            }
            return NORMAL;
        }
    }
}
