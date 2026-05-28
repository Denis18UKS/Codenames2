package fable.codenames.client.command;

import com.mojang.brigadier.Command;
import fable.codenames.client.screen.TeamChatTextMoveScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

public final class TeamChatTextMoveClientCommand {
    private TeamChatTextMoveClientCommand() {
    }

    public static int open(FabricClientCommandSource source) {
        MinecraftClient.getInstance().setScreen(new TeamChatTextMoveScreen());
        return Command.SINGLE_SUCCESS;
    }
}
