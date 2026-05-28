package fable.codenames.board;

import fable.codenames.Codenames;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public final class BoardSelectionSync {
    public static final Identifier CHANNEL_ID = new Identifier(Codenames.MOD_ID, "board_selections");

    private BoardSelectionSync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncToPlayer(handler.player));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncToPlayer(player);
        }
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        List<BoardSelectionState.VoteIndicator> indicators = BoardSelectionState.getIndicators();
        buf.writeVarInt(indicators.size());
        for (BoardSelectionState.VoteIndicator indicator : indicators) {
            buf.writeString(indicator.teamName());
            buf.writeBlockPos(indicator.pos());
            buf.writeBlockPos(indicator.displayPos());
            buf.writeVarInt(indicator.count());
            buf.writeLong(indicator.confirmationStartTick());
        }
        ServerPlayNetworking.send(player, CHANNEL_ID, buf);
    }
}
