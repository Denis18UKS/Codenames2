package fable.codenames.client.renderer;

import fable.codenames.entity.PassTurnHologramEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

public class PassTurnHologramRenderer extends EntityRenderer<PassTurnHologramEntity> {

    private static final Identifier TEXTURE =
            new Identifier("codenames", "textures/screens/turn.png");

    public PassTurnHologramRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(PassTurnHologramEntity entity,
                       float yaw,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light) {

        matrices.push();

        matrices.translate(0.0, 0.5, 0.0);

        Direction dir = entity.getFixedDirection();
        float rotationY = switch (dir) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case NORTH -> 0f;
            case EAST -> -90f;
            default -> 0f;
        };
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));

        float scale = 0.5f;
        matrices.scale(scale, scale, scale);

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();

        float size = 0.5f;

        vc.vertex(entry.getPositionMatrix(), -size, -size, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 1.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), 0.0f, 0.0f, 1.0f)
                .next();

        vc.vertex(entry.getPositionMatrix(), size, -size, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 1.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), 0.0f, 0.0f, 1.0f)
                .next();

        vc.vertex(entry.getPositionMatrix(), size, size, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 0.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), 0.0f, 0.0f, 1.0f)
                .next();

        vc.vertex(entry.getPositionMatrix(), -size, size, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 0.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), 0.0f, 0.0f, 1.0f)
                .next();

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(PassTurnHologramEntity entity) {
        return TEXTURE;
    }
}