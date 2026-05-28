package fable.codenames.client.register;

import fable.codenames.block.TeamChatBlock;
import fable.codenames.client.renderer.BoardOverlayRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class RegisterWorldRenderers {
    private RegisterWorldRenderers() {
    }

    public static void init() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            if (hitResult.getType() != HitResult.Type.BLOCK || context.world() == null) {
                return true;
            }

            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return !(context.world().getBlockState(blockHit.getBlockPos()).getBlock() instanceof TeamChatBlock);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BoardOverlayRenderer::render);
    }
}
