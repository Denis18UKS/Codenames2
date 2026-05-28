package fable.codenames.client;

import fable.codenames.block.HeadBlocks;
import fable.codenames.client.chat.ChatHoverHintRenderer;
import fable.codenames.client.chat.TeamChatTextLayout;
import fable.codenames.client.register.RegisterBlockEntityRenderers;
import fable.codenames.client.register.RegisterClientCommands;
import fable.codenames.client.register.RegisterEntityRenderers;
import fable.codenames.client.register.RegisterHud;
import fable.codenames.client.register.RegisterKeyBindings;
import fable.codenames.client.register.RegisterNetworking;
import fable.codenames.client.register.RegisterWorldRenderers;
import fable.codenames.config.UnicodeConfig;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;

public class CodenamesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // =========================
        // FORCE UNICODE SETTING
        // =========================
        MinecraftClient client = MinecraftClient.getInstance();

        if (client != null && client.options != null) {

            client.options
                    .getForceUnicodeFont()
                    .setValue(UnicodeConfig.UNICODE_GLOBAL);

            client.options.write();
        }

        // =========================
        // BLOCK RENDER LAYERS
        // =========================
        BlockRenderLayerMap.INSTANCE.putBlocks(
                RenderLayer.getCutout(),
                HeadBlocks.getBlocks().values().toArray(Block[]::new)
        );

        // =========================
        // CLIENT SYSTEMS
        // =========================
        TeamChatTextLayout.load();

        RegisterClientCommands.init();
        RegisterBlockEntityRenderers.init();
        RegisterEntityRenderers.init();

        RegisterHud.init();
        RegisterKeyBindings.init();
        RegisterNetworking.init();
        RegisterWorldRenderers.init();

        ChatHoverHintRenderer.init();
    }
}