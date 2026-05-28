package fable.codenames.client.model;

import fable.codenames.Codenames;
import fable.codenames.entity.CrownEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class CrownEntityModel extends GeoModel<CrownEntity> {
    private static final Identifier MODEL = new Identifier(Codenames.MOD_ID, "geo/block/crown.geo.json");
    private static final Identifier TEXTURE = new Identifier(Codenames.MOD_ID, "textures/block/crown.png");
    private static final Identifier ANIMATION = new Identifier(Codenames.MOD_ID, "animations/crown.animation.json");

    @Override
    public Identifier getModelResource(CrownEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(CrownEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CrownEntity animatable) {
        return ANIMATION;
    }
}
