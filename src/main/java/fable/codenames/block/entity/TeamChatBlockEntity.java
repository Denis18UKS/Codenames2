package fable.codenames.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class TeamChatBlockEntity extends BlockEntity {
    private static final String OFFSET_X_KEY = "BannerOffsetXPixels";
    private static final String OFFSET_Y_KEY = "BannerOffsetYPixels";
    private static final String TEAM_NAME_KEY = "TeamName";
    private static final int MAX_OFFSET_PIXELS = 64;
    private int bannerOffsetXPixels;
    private int bannerOffsetYPixels;
    private String teamName = "";

    public TeamChatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.TEAM_CHAT_BLOCK_ENTITY.getBlockEntityType(), pos, state);
    }

    public int getBannerOffsetXPixels() {
        return this.bannerOffsetXPixels;
    }

    public int getBannerOffsetYPixels() {
        return this.bannerOffsetYPixels;
    }

    public String getTeamName() {
        return this.teamName;
    }

    public void setTeamName(String teamName) {
        String next = teamName == null ? "" : teamName;
        if (this.teamName.equals(next)) {
            return;
        }

        this.teamName = next;
        markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void moveBannerOffset(int deltaXPixels, int deltaYPixels) {
        setBannerOffset(this.bannerOffsetXPixels + deltaXPixels, this.bannerOffsetYPixels + deltaYPixels);
    }

    public void resetBannerOffset() {
        setBannerOffset(0, 0);
    }

    public void setBannerOffset(int xPixels, int yPixels) {
        int nextX = clamp(xPixels);
        int nextY = clamp(yPixels);
        if (this.bannerOffsetXPixels == nextX && this.bannerOffsetYPixels == nextY) {
            return;
        }

        this.bannerOffsetXPixels = nextX;
        this.bannerOffsetYPixels = nextY;
        markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt(OFFSET_X_KEY, this.bannerOffsetXPixels);
        nbt.putInt(OFFSET_Y_KEY, this.bannerOffsetYPixels);
        nbt.putString(TEAM_NAME_KEY, this.teamName);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.bannerOffsetXPixels = clamp(nbt.getInt(OFFSET_X_KEY));
        this.bannerOffsetYPixels = clamp(nbt.getInt(OFFSET_Y_KEY));
        this.teamName = nbt.getString(TEAM_NAME_KEY);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    private static int clamp(int value) {
        return Math.max(-MAX_OFFSET_PIXELS, Math.min(MAX_OFFSET_PIXELS, value));
    }
}
