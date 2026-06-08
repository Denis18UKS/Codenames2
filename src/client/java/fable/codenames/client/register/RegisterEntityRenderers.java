package fable.codenames.client.register;

import fable.codenames.client.renderer.CrownEntityRenderer;
import fable.codenames.client.renderer.PassTurnHologramRenderer;
import fable.codenames.client.renderer.XodKomandProjectorEntityRenderer;
import fable.codenames.entity.CrownEntity;
import fable.codenames.entity.PassTurnHologramEntity;
import fable.codenames.entity.XodKomandProjectorEntity;
import fable.codenames.entity.ModMiscEntityTypes;
import fable.codenames.entity.ModMobEntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;

public class RegisterEntityRenderers {

    @SuppressWarnings("unchecked")
    public static void init() {

        EntityRendererRegistry.register(
                (EntityType<CrownEntity>) ModMobEntityTypes.CROWN.getEntityType(),
                CrownEntityRenderer::new
        );

        EntityRendererRegistry.register(
                (EntityType<XodKomandProjectorEntity>) ModMiscEntityTypes.XOD_KOMAND_PROJECTOR.getEntityType(),
                XodKomandProjectorEntityRenderer::new
        );

        EntityRendererRegistry.register(
                (EntityType<PassTurnHologramEntity>) ModMiscEntityTypes.PASS_TURN_HOLOGRAM.getEntityType(),
                PassTurnHologramRenderer::new
        );
    }
}