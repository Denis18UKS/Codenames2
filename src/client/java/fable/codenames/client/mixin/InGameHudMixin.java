package fable.codenames.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    private static final Identifier XP_EMPTY =
            Identifier.of("codenames", "textures/gui/xp_bar_empty.png");

    private static final Identifier XP_FILL_BLUE =
            Identifier.of("codenames", "textures/gui/xp_bar_blue.png");

    private static final Identifier XP_FILL_RED =
            Identifier.of("codenames", "textures/gui/xp_bar_red.png");

    // Отключаем стандартные HP/голод/броню
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void codenames$hideStatusBars(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }

    // Отключаем стандартный XP bar и рисуем свой
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void codenames$renderCustomXpBar(DrawContext context, int x, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.interactionManager == null) {
            return;
        }

        ci.cancel();

        PlayerEntity player = client.player;

        int barWidth = 182;
        int barHeight = 5;

        int barX = (context.getScaledWindowWidth() - barWidth) / 2;
        int barY = context.getScaledWindowHeight() - 32 + 3;

        int progress = (int) (player.experienceProgress * (float) barWidth);
        progress = Math.max(0, Math.min(progress, barWidth));

        Identifier fillTexture = isBlueTeam() ? XP_FILL_BLUE : XP_FILL_RED;

        // Включаем блендинг для правильной прозрачности
        RenderSystem.enableBlend();
        
        // Рисуем пустой фон
        context.drawTexture(
                XP_EMPTY,
                barX,
                barY,
                0,
                0,
                barWidth,
                barHeight,
                barWidth,
                barHeight
        );

        // Рисуем заполнение
        if (progress > 0) {
            context.drawTexture(
                    fillTexture,
                    barX,
                    barY,
                    0,
                    0,
                    progress,
                    barHeight,
                    barWidth,
                    barHeight
            );
        }

        // Рисуем уровень
        if (player.experienceLevel > 0) {
            String levelText = String.valueOf(player.experienceLevel);
            int textX = (context.getScaledWindowWidth() - client.textRenderer.getWidth(levelText)) / 2;
            int textY = barY - 10;

            // Рисуем тень для читаемости
            context.drawText(
                    client.textRenderer,
                    levelText,
                    textX + 1,
                    textY + 1,
                    0x40000000,
                    false
            );
            
            // Рисуем сам текст
            context.drawText(
                    client.textRenderer,
                    levelText,
                    textX,
                    textY,
                    0x80FF20,
                    false
            );
        }

        RenderSystem.disableBlend();
    }

    // Отключаем мигание оверлея (таб и ники)
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void codenames$disableOverlayFlash(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        // Отключаем мигание PumpkinBlur, PowderSnow, Spyglass, Vignette
        if (texture.getPath().contains("pumpkinblur") 
                || texture.getPath().contains("powder_snow") 
                || texture.getPath().contains("spyglass")
                || texture.getPath().contains("vignette")) {
            return; // Оставляем как есть
        }
        // Не отменяем другие оверлеи, чтобы не сломать остальной HUD
    }

    private static boolean isBlueTeam() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return false;
        }

        AbstractTeam abstractTeam = client.player.getScoreboardTeam();

        if (!(abstractTeam instanceof Team team)) {
            return false;
        }

        String teamName = team.getName().toLowerCase(Locale.ROOT);
        Formatting formatting = team.getColor();

        return teamName.contains("blue")
                || teamName.contains("син")
                || formatting == Formatting.BLUE
                || formatting == Formatting.AQUA
                || formatting == Formatting.DARK_AQUA;
    }
}