package fable.codenames.chat;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class TeamChatSync {
    private TeamChatSync() {
    }

    public static void openForPlayer(ServerPlayerEntity player) {
        openForPlayer(player, null);
    }

    public static void openForPlayer(ServerPlayerEntity player, BlockPos chatBlockPos) {
        String teamName = TeamChatService.getPlayerTeamName(player);
        if (teamName == null) {
            player.sendMessage(Text.literal("Сначала вступите в команду."), false);
            return;
        }
        String chatTeamName = chatBlockPos == null ? teamName : getChatTeamName(player, chatBlockPos, teamName);
        boolean isLeader = Roles.getState(player.getServer()).getRole(player.getUuid()) == PlayerRole.LIDER;

        if (chatBlockPos != null && !isLeader) {
            player.sendMessage(Text.literal("\u0414\u043e\u0441\u0442\u0443\u043f \u0442\u043e\u043b\u044c\u043a\u043e \u0443 \u043b\u0438\u0434\u0435\u0440\u043e\u0432").formatted(Formatting.YELLOW), true);
            return;
        }

        if (chatBlockPos != null && !chatTeamName.equals(teamName)) {
            player.sendMessage(Text.literal("\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u044d\u0442\u043e\u043c\u0443 \u0447\u0430\u0442\u0443.").formatted(net.minecraft.util.Formatting.YELLOW), true);
            return;
        }
        if (chatBlockPos != null && !TeamChatRightsMode.isEnabled() && !CodenamesGameService.isLeaderTeamTurn(player)) {
            player.sendMessage(Text.literal("\u041d\u0435 \u0445\u043e\u0434 \u0432\u0430\u0448\u0435\u0439 \u043a\u043e\u043c\u0430\u043d\u0434\u044b").formatted(Formatting.YELLOW), true);
            return;
        }
        boolean canSend = TeamChatService.canWrite(player, chatTeamName);
        if (chatBlockPos != null && !canSend) {
            player.sendMessage(Text.literal("\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u044d\u0442\u043e\u043c\u0443 \u0447\u0430\u0442\u0443.").formatted(net.minecraft.util.Formatting.YELLOW), true);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(chatBlockPos != null);
        if (chatBlockPos != null) {
            buf.writeBlockPos(chatBlockPos);
        }
        writeChatPayload(buf, chatTeamName, canSend, isLeader, TeamChats.getState(player.getServer()).getMessages());
        ServerPlayNetworking.send(player, TeamChatPackets.OPEN, buf);
    }

    public static void syncAll(MinecraftServer server) {
        List<TeamChatMessage> messages = TeamChats.getState(server).getMessages();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String playerTeam = TeamChatService.getPlayerTeamName(player);
            if (playerTeam == null) {
                continue;
            }

            boolean canSend = TeamChatService.canWrite(player, playerTeam);
            boolean isLeader = Roles.getState(server).getRole(player.getUuid()) == PlayerRole.LIDER;
            PacketByteBuf buf = PacketByteBufs.create();
            writeChatPayload(buf, playerTeam, canSend, isLeader, messages);
            ServerPlayNetworking.send(player, TeamChatPackets.SYNC, buf);
        }
    }

    private static String getChatTeamName(ServerPlayerEntity player, BlockPos chatBlockPos, String fallbackTeamName) {
        if (player.getWorld().getBlockEntity(chatBlockPos) instanceof fable.codenames.block.entity.TeamChatBlockEntity entity
                && !entity.getTeamName().isBlank()) {
            return entity.getTeamName();
        }
        return fallbackTeamName;
    }

    private static void writeChatPayload(PacketByteBuf buf, String teamName, boolean canSend, boolean isLeader, List<TeamChatMessage> messages) {
        buf.writeString(teamName);
        buf.writeBoolean(canSend);
        buf.writeBoolean(isLeader);
        buf.writeVarInt(messages.size());
        for (TeamChatMessage message : messages) {
            message.write(buf);
        }
    }
}
