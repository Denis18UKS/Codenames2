package fable.codenames.client.chat;

import fable.codenames.chat.TeamChatMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class TeamChatClientState {
    private static String currentTeam;
    private static boolean canSend;
    private static boolean leader;
    private static List<TeamChatMessage> messages = List.of();
    private static BlockPos activeBannerPos;
    private static boolean bannerInputActive;
    private static String draft = "";

    private TeamChatClientState() {
    }

    public static void update(String teamName, boolean canWrite, boolean isLeader, List<TeamChatMessage> messages) {
        currentTeam = teamName;
        canSend = canWrite;
        leader = isLeader;
        TeamChatClientState.messages = List.copyOf(messages);
    }

    public static void clear() {
        currentTeam = null;
        canSend = false;
        leader = false;
        messages = List.of();
        activeBannerPos = null;
        bannerInputActive = false;
        draft = "";
    }

    public static String getCurrentTeam() {
        return currentTeam;
    }

    public static boolean canSend() {
        return canSend;
    }

    public static boolean isLeader() {
        return leader;
    }

    public static List<TeamChatMessage> getMessages() {
        return messages;
    }

    public static void startBannerInput(BlockPos pos) {
        activeBannerPos = pos;
        bannerInputActive = pos != null;
        draft = "";
    }

    public static void stopBannerInput() {
        bannerInputActive = false;
        draft = "";
    }

    public static boolean isBannerInputActive(BlockPos pos) {
        return bannerInputActive && activeBannerPos != null && activeBannerPos.equals(pos);
    }

    public static boolean hasActiveBannerInput() {
        return bannerInputActive;
    }

    public static String getDraft() {
        return draft;
    }

    public static void setDraft(String value) {
        draft = value == null ? "" : value;
    }

    public static boolean isOwnMessage(TeamChatMessage message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        return client.player.getUuid().equals(message.senderUuid());
    }
}
