package fable.codenames.client.mixin;

import fable.codenames.config.UnicodeConfig;

import net.minecraft.client.option.GameOptions;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Inject(method = "load", at = @At("TAIL"))
    private void codenames$forceUnicodeOff(CallbackInfo ci) {

        GameOptions options = (GameOptions) (Object) this;

        options.getForceUnicodeFont().setValue(
                UnicodeConfig.UNICODE_GLOBAL
        );

        options.write();
    }
}