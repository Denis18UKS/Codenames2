package fable.codenames.chat;

import fable.codenames.block.entity.TeamChatBlockEntity;
import fable.codenames.item.ModItems;
import fable.codenames.item.TeamChatConfiguratorItem;
import fable.codenames.game.CodenamesGameService;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class TeamChatNetworking {
    private TeamChatNetworking() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.REQUEST_OPEN, (server, player, handler, buf, responseSender) ->
                server.execute(() -> TeamChatSync.openForPlayer(player)));
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.REQUEST_CLEAR, (server, player, handler, buf, responseSender) ->
                server.execute(() -> handleClear(player)));
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.DISPUTE_CLUE, (server, player, handler, buf, responseSender) ->
                server.execute(() -> CodenamesGameService.disputeClue(server, player)));
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.SEND, (server, player, handler, buf, responseSender) -> {
            String message = buf.readString(32767);
            server.execute(() -> handleSend(player, message));
        });
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.MOVE_BANNER, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int deltaX = buf.readVarInt();
            int deltaY = buf.readVarInt();
            boolean reset = buf.readBoolean();
            server.execute(() -> handleMoveBanner(player, pos, deltaX, deltaY, reset));
        });
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.APPLY_CONFIG, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> handleApplyConfig(player, pos));
        });
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.UPDATE_TEXT_LAYOUT, (server, player, handler, buf, responseSender) -> {
            int leftTextX = buf.readVarInt();
            int rightTextX = buf.readVarInt();
            int textY = buf.readVarInt();
            float textScale = buf.readFloat();
            server.execute(() -> handleTextLayoutUpdate(server, player, leftTextX, rightTextX, textY, textScale));
        });
        ServerPlayNetworking.registerGlobalReceiver(TeamChatPackets.TOGGLE_VANILLA_CHAT, (server, player, handler, buf, responseSender) ->
                server.execute(() -> syncVanillaChat(server)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> {
                    TeamChatSync.openForPlayer(handler.player); // <-- фикс истории после перезахода
                    syncVanillaChat(handler.player);
                    syncTextLayout(handler.player);
                }));
    }

    private static void handleSend(ServerPlayerEntity player, String message) {
        if (player.getServer() == null) return;
        TeamChatService.appendLeaderMessage(player.getServer(), player, message);
    }

    private static void syncVanillaChat(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncVanillaChat(player);
        }
    }

    private static void syncVanillaChat(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(false);
        ServerPlayNetworking.send(player, TeamChatPackets.SYNC_VANILLA_CHAT, buf);
    }

    private static void handleClear(ServerPlayerEntity player) {
        if (player.getServer() == null) return;
        if (!player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("Очищать чат может только оператор."), false);
            return;
        }
        TeamChats.getState(player.getServer()).clearMessages();
        TeamChatSync.syncAll(player.getServer());
        player.sendMessage(Text.literal("Командный чат очищен."), false);
    }

    private static void handleMoveBanner(ServerPlayerEntity player, BlockPos pos, int deltaX, int deltaY, boolean reset) {
        if (!player.hasPermissionLevel(2)) return;
        if (player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) return;

        BlockEntity blockEntity = player.getServerWorld().getBlockEntity(pos);
        if (!(blockEntity instanceof TeamChatBlockEntity teamChatBlockEntity)) return;

        if (reset) {
            teamChatBlockEntity.resetBannerOffset();
        } else {
            teamChatBlockEntity.moveBannerOffset(deltaX, deltaY);
        }
    }

    private static void handleApplyConfig(ServerPlayerEntity player, BlockPos pos) {
        if (player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 100.0D) return;

        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ModItems.TEAM_CHAT_CONFIGURATOR.getItem())) {
            stack = player.getOffHandStack();
        }
        if (!stack.isOf(ModItems.TEAM_CHAT_CONFIGURATOR.getItem())) return;

        TeamChatConfiguratorItem.applyToBanner(player, stack, pos);
    }

    private static void handleTextLayoutUpdate(MinecraftServer server, ServerPlayerEntity player, int leftTextX, int rightTextX, int textY, float textScale) {
        if (!player.hasPermissionLevel(2)) return;
        TeamChatTextLayoutState state = TeamChatTextLayouts.getState(server);
        state.set(clampInt(leftTextX, 0, 20), clampInt(rightTextX, 0, 20), clampInt(textY, 0, 10), clampFloat(textScale, 0.35F, 1.0F));
        syncTextLayout(server);
    }

    private static void syncTextLayout(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncTextLayout(player);
        }
    }

    private static void syncTextLayout(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        TeamChatTextLayoutState state = TeamChatTextLayouts.getState(server);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(state.leftTextX());
        buf.writeVarInt(state.rightTextX());
        buf.writeVarInt(state.textY());
        buf.writeFloat(state.textScale());
        ServerPlayNetworking.send(player, TeamChatPackets.SYNC_TEXT_LAYOUT, buf);
    }

    private static int clampInt(int value, int min, int maxValue) {
        return Math.max(min, Math.min(maxValue, value));
    }

    private static float clampFloat(float value, float min, float maxValue) {
        return Math.max(min, Math.min(maxValue, value));
    }
}