package fable.codenames.client.renderer;

import fable.codenames.block.ClickButtonBlock;
import fable.codenames.block.ModBlocks;
import fable.codenames.block.entity.ClickButtonBlockEntity;
import fable.codenames.client.model.ClickButtonBlockModel;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ClickButtonBlockEntityRenderer extends GeoBlockRenderer<ClickButtonBlockEntity> {
    public ClickButtonBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(new ClickButtonBlockModel());
    }

    @Override
    public void preRender(MatrixStack poseStack, ClickButtonBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!isReRender && animatable.getCachedState().isOf(ModBlocks.START_BUTTON.getBlock())) {
            ClickButtonBlock.PlacementOffset offset = animatable.getCachedState().get(ClickButtonBlock.PLACEMENT_OFFSET);
            poseStack.translate(offset.getX() * 0.5, 0.0, offset.getZ() * 0.5);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
