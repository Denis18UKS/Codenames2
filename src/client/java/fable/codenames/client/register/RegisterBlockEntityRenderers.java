package fable.codenames.client.register;

import fable.codenames.block.entity.ModBlockEntityTypes;
import fable.codenames.block.entity.ClickButtonBlockEntity;
import fable.codenames.block.entity.TeamChatBlockEntity;
import fable.codenames.client.renderer.ClickButtonBlockEntityRenderer;
import fable.codenames.client.renderer.TeamChatBlockEntityRenderer;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class RegisterBlockEntityRenderers {
    private RegisterBlockEntityRenderers() {
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        BlockEntityRendererFactories.register((BlockEntityType<TeamChatBlockEntity>) ModBlockEntityTypes.TEAM_CHAT_BLOCK_ENTITY.getBlockEntityType(), TeamChatBlockEntityRenderer::new);
        BlockEntityRendererFactories.register((BlockEntityType<ClickButtonBlockEntity>) ModBlockEntityTypes.CLICK_BUTTON_BLOCK_ENTITY.getBlockEntityType(), ClickButtonBlockEntityRenderer::new);
    }
}
