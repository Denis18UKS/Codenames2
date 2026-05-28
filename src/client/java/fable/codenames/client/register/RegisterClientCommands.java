package fable.codenames.client.register;

import com.mojang.brigadier.Command;
import fable.codenames.client.command.TeamChatBannerMoveClientCommand;
import fable.codenames.client.command.TeamChatTextMoveClientCommand;
import fable.codenames.client.hud.TeamHudState;
import fable.codenames.client.screen.TeamHudEditorScreen;
import fable.codenames.client.command.TeamHudToggleClientCommand;
import fable.codenames.client.OsporitClientActions;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

public final class RegisterClientCommands {
    private RegisterClientCommands() {
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("test")
                        .executes(context -> {
                            OsporitClientActions.runTestAction();
                            return Command.SINGLE_SUCCESS;
                        })));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("cdn_move")
                        .executes(context -> TeamChatBannerMoveClientCommand.open(context.getSource()))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("cdn_text_move")
                        .executes(context -> TeamChatTextMoveClientCommand.open(context.getSource()))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("codenameshud")
                        .executes(context -> TeamHudToggleClientCommand.toggle(context.getSource()))
                        .then(ClientCommandManager.literal("edit")
                                .executes(context -> {
                                    net.minecraft.client.MinecraftClient.getInstance().setScreen(new TeamHudEditorScreen());
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(ClientCommandManager.literal("reset")
                                .executes(context -> {
                                    TeamHudState.resetPosition();
                                    context.getSource().sendFeedback(net.minecraft.text.Text.literal("Codenames HUD position reset."));
                                    return Command.SINGLE_SUCCESS;
                                }))));

    }
}
