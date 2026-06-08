package fable.codenames.client.renderer;

import fable.codenames.entity.PassTurnHologramEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PassTurnHologramRenderer extends EntityRenderer<PassTurnHologramEntity> {

    private final TextRenderer textRenderer;
    private static final Text PASS_ICON = Text.literal("↩");
    private static final int ICON_COLOR = 0xFF00FF00;

    public PassTurnHologramRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.textRenderer = context.getTextRenderer();
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            PassTurnHologramEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        matrices.push();

        matrices.translate(0.0, 0.3, 0.0);

        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        float x = -this.textRenderer.getWidth(PASS_ICON) / 2f;

        var matrix = matrices.peek().getPositionMatrix();

        this.textRenderer.draw(
                PASS_ICON,
                x,
                0,
                ICON_COLOR,
                false,
                matrix,
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0x40000000,
                light
        );

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(PassTurnHologramEntity entity) {
        return null;
    }
}