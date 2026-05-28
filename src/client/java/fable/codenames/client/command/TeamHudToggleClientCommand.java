package fable.codenames.client.command;

import com.mojang.brigadier.Command;
import fable.codenames.client.hud.TeamHudState;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class TeamHudToggleClientCommand {
    private TeamHudToggleClientCommand() {
    }

    public static int toggle(FabricClientCommandSource source) {
        boolean enabled = TeamHudState.toggle();
        source.sendFeedback(Text.literal("Codenames HUD: " + (enabled ? "enabled" : "disabled")));
        return Command.SINGLE_SUCCESS;
    }
}
