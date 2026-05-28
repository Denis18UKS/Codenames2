package fable.codenames.client.renderer;

import fable.codenames.client.model.CrownEntityModel;
import fable.codenames.entity.CrownEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrownEntityRenderer extends GeoEntityRenderer<CrownEntity> {
    public CrownEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new CrownEntityModel());
        this.shadowRadius = 0.0f;
    }

    @Override
    public boolean shouldRender(CrownEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
