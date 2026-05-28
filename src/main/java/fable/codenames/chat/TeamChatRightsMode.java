package fable.codenames.chat;

public final class TeamChatRightsMode {
    private static boolean enabled;

    private TeamChatRightsMode() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        TeamChatRightsMode.enabled = enabled;
    }
}
