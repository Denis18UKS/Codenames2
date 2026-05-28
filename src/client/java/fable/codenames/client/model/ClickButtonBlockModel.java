package fable.codenames.client.model;

import fable.codenames.Codenames;
import fable.codenames.block.entity.ClickButtonBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class ClickButtonBlockModel extends GeoModel<ClickButtonBlockEntity> {
    private static final Identifier MODEL = new Identifier(Codenames.MOD_ID, "geo/block/click_button.geo.json");
    private static final Identifier START_MODEL = new Identifier(Codenames.MOD_ID, "geo/block/start_button.geo.json");
    private static final Identifier TEXTURE = new Identifier(Codenames.MOD_ID, "textures/block/reset_button.png");
    private static final Identifier START_TEXTURE = new Identifier(Codenames.MOD_ID, "textures/block/start_button.png");
    private static final Identifier ANIMATION = new Identifier(Codenames.MOD_ID, "animations/click_button.animation.json");
    private static final Identifier START_ANIMATION = new Identifier(Codenames.MOD_ID, "animations/start_button.animation.json");

    @Override
    public Identifier getModelResource(ClickButtonBlockEntity animatable) {
        return animatable.isStartButtonVisual() ? START_MODEL : MODEL;
    }

    @Override
    public Identifier getTextureResource(ClickButtonBlockEntity animatable) {
        return animatable.isStartButtonVisual() ? START_TEXTURE : TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ClickButtonBlockEntity animatable) {
        return animatable.isStartButtonVisual() ? START_ANIMATION : ANIMATION;
    }
}
