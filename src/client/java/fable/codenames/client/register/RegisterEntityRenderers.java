package fable.codenames.client.register;

import fable.codenames.client.renderer.CrownEntityRenderer;
import fable.codenames.client.renderer.HologramProjectorEntityRenderer;
import fable.codenames.entity.CrownEntity;
import fable.codenames.entity.HologramProjectorEntity;
import fable.codenames.entity.ModMiscEntityTypes;
import fable.codenames.entity.ModMobEntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;

public class RegisterEntityRenderers {
    @SuppressWarnings("unchecked")
    public static void init() {
        EntityRendererRegistry.register((EntityType<CrownEntity>) ModMobEntityTypes.CROWN.getEntityType(), CrownEntityRenderer::new);
        EntityRendererRegistry.register((EntityType<HologramProjectorEntity>) ModMiscEntityTypes.HOLOGRAM_PROJECTOR.getEntityType(), HologramProjectorEntityRenderer::new);
    }
}
