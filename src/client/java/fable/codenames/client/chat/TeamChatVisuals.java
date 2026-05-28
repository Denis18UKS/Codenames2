package fable.codenames.client.chat;

import fable.codenames.Codenames;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class TeamChatVisuals {
    public static final Identifier GREY_BACKGROUND = new Identifier(Codenames.MOD_ID, "textures/screens/background_chat/fon_chat_interface_grey.png");
    public static final Identifier RED_BACKGROUND = new Identifier(Codenames.MOD_ID, "textures/screens/background_chat/red/fon_chat_interface_red.png");
    public static final Identifier BLUE_BACKGROUND = new Identifier(Codenames.MOD_ID, "textures/screens/background_chat/blue/fon_chat_interface_blue.png");
    public static final Identifier RED_LEFT = new Identifier(Codenames.MOD_ID, "textures/screens/sms_chat/red/red_left_sms.png");
    public static final Identifier RED_RIGHT = new Identifier(Codenames.MOD_ID, "textures/screens/sms_chat/red/red_right_sms.png");
    public static final Identifier BLUE_LEFT = new Identifier(Codenames.MOD_ID, "textures/screens/sms_chat/blue/blue_left_sms.png");
    public static final Identifier BLUE_RIGHT = new Identifier(Codenames.MOD_ID, "textures/screens/sms_chat/blue/blue_right_sms.png");

    private TeamChatVisuals() {
    }

    public static Identifier backgroundTexture(String teamName) {
        if (isRed(teamName)) {
            return RED_BACKGROUND;
        }
        if (isBlue(teamName)) {
            return BLUE_BACKGROUND;
        }
        return BLUE_BACKGROUND;
    }

    public static Identifier bubbleTexture(String senderTeam, boolean own) {
        if (isRed(senderTeam)) {
            return own ? RED_RIGHT : RED_LEFT;
        }
        return own ? BLUE_RIGHT : BLUE_LEFT;
    }

    public static boolean isRed(String teamName) {
        Formatting color = scoreboardColor(teamName);
        if (color == Formatting.RED || color == Formatting.DARK_RED) {
            return true;
        }

        String normalized = normalize(teamName);
        return normalized.contains("red")
                || normalized.contains("\u043a\u0440\u0430\u0441")
                || normalized.contains("РєСЂР°СЃ");
    }

    public static boolean isBlue(String teamName) {
        Formatting color = scoreboardColor(teamName);
        if (color == Formatting.BLUE || color == Formatting.DARK_BLUE || color == Formatting.AQUA) {
            return true;
        }

        String normalized = normalize(teamName);
        return normalized.contains("blue")
                || normalized.contains("\u0441\u0438\u043d")
                || normalized.contains("СЃРёРЅ");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static Formatting scoreboardColor(String teamName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || teamName == null) {
            return Formatting.RESET;
        }

        Team team = client.world.getScoreboard().getTeam(teamName);
        return team == null ? Formatting.RESET : team.getColor();
    }
}
