package fable.codenames.game;

import fable.codenames.board.BoardCellType;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class GameTitleService {
    private GameTitleService() {
    }

    public static void showCountdown(MinecraftServer server, int value) {
        MutableText title = Text.empty()
                .append(Text.literal("3... ").formatted(Formatting.RED))
                .append(Text.literal("2... ").formatted(Formatting.GOLD))
                .append(Text.literal("1... ").formatted(Formatting.GREEN))
                .append(Text.literal("Старт!").formatted(Formatting.AQUA));
        if (value == 3) {
            title = Text.literal("3...").formatted(Formatting.RED);
        } else if (value == 2) {
            title = Text.literal("2...").formatted(Formatting.GOLD);
        } else if (value == 1) {
            title = Text.literal("1...").formatted(Formatting.GREEN);
        }
        show(server, title, 0, 25, 5);
    }

    public static void showStart(MinecraftServer server) {
        show(server, Text.literal("Старт!").formatted(Formatting.AQUA), 0, 40, 10);
    }

    public static void showWin(MinecraftServer server, String teamName) {
        show(server, teamText(teamName, " победили"), 5, 100, 15);
    }

    public static void showLose(MinecraftServer server, String teamName) {
        show(server, teamText(teamName, " проиграли"), 5, 100, 15);
    }

    public static void showCancelled(MinecraftServer server) {
        show(server, Text.literal("Запуск отменён").formatted(Formatting.YELLOW), 0, 35, 10);
    }

    private static MutableText teamText(String teamName, String suffix) {
        BoardCellType type = CodenamesGameService.expectedTypeForTeam(teamName);
        if (type == BoardCellType.BLUE) {
            return Text.literal("Синие").formatted(Formatting.AQUA).append(Text.literal(suffix).formatted(Formatting.AQUA));
        }
        return Text.literal("Красные").formatted(Formatting.RED).append(Text.literal(suffix).formatted(Formatting.RED));
    }

    private static void show(MinecraftServer server, Text title, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
    }
}
