package fable.codenames.client.command;

import com.mojang.brigadier.Command;
import fable.codenames.client.chat.VanillaChatState;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class VanillaChatToggleClientCommand {
    private VanillaChatToggleClientCommand() {
    }

    public static int set(FabricClientCommandSource source, boolean disabled) {
        VanillaChatState.setDisabled(disabled);
        source.sendFeedback(Text.literal("Vanilla chat: " + (disabled ? "disabled" : "enabled")));
        return Command.SINGLE_SUCCESS;
    }

    public static int toggle(FabricClientCommandSource source) {
        boolean disabled = VanillaChatState.toggle();
        source.sendFeedback(Text.literal("Vanilla chat: " + (disabled ? "disabled" : "enabled")));
        return Command.SINGLE_SUCCESS;
    }
}
