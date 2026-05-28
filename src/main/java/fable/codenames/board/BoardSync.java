package fable.codenames.board;

import fable.codenames.Codenames;
import fable.codenames.dev.SoloModeService;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.LinkedHashMap;

public final class BoardSync {
    public static final Identifier CHANNEL_ID = new Identifier(Codenames.MOD_ID, "board_state");

    private BoardSync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncToPlayer(server, handler.player));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncToPlayer(server, player);
        }
    }

    public static void syncToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        BoardState state = Boards.getState(server);
        Map<BlockPos, BoardCellType> cells = visibleCells(state);
        buf.writeVarInt(cells.size());
        cells.forEach((pos, type) -> {
            buf.writeBlockPos(pos);
            buf.writeEnumConstant(type);
        });
        buf.writeBoolean(canSeeAnswers(server, player));
        ServerPlayNetworking.send(player, CHANNEL_ID, buf);
    }

    private static Map<BlockPos, BoardCellType> visibleCells(BoardState state) {
        Map<BlockPos, BoardCellType> cells = new LinkedHashMap<>();
        java.util.List<BlockPos> canonicalPositions = state.getPrimaryFieldPositions();
        for (java.util.List<BlockPos> field : state.getFields()) {
            for (int i = 0; i < field.size() && i < canonicalPositions.size(); i++) {
                BlockPos canonicalPos = canonicalPositions.get(i);
                if (state.contains(canonicalPos)) {
                    BoardCellType type = state.getType(canonicalPos);
                    cells.put(field.get(i), type);
                }
            }
        }
        return cells;
    }

    private static boolean canSeeAnswers(MinecraftServer server, ServerPlayerEntity player) {
        return SoloModeService.isEnabled(server, player.getUuid())
                || Roles.getState(server).getRole(player.getUuid()) == PlayerRole.LIDER;
    }
}
