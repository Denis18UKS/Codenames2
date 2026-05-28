package fable.codenames.client.mixin;

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

    // ❌ отключаем стандартные HP/голод/броню
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void codenames$hideStatusBars(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }

    // ❌ отключаем стандартный XP bar
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void codenames$renderCustomXpBar(DrawContext context, int x, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        ci.cancel();

        PlayerEntity player = client.player;

        int barWidth = 182;
        int barHeight = 5;

        int barX = (context.getScaledWindowWidth() - barWidth) / 2;
        int barY = context.getScaledWindowHeight() - 32 + 3;

        // 🔥 FIX: правильный масштаб (НЕ 183)
        int progress = (int) (player.experienceProgress * barWidth);

        // 🔥 FIX: защита от выхода за границы (убирает "штык")
        progress = Math.max(0, Math.min(progress, barWidth - 1));

        Identifier fillTexture = isBlueTeam()
                ? XP_FILL_BLUE
                : XP_FILL_RED;

        // 🧱 background
        context.drawTexture(
                XP_EMPTY,
                barX,
                barY,
                0f,
                0f,
                barWidth,
                barHeight,
                barWidth,
                barHeight
        );

        // 🟦/🟥 fill
        if (progress > 0) {
            context.drawTexture(
                    fillTexture,
                    barX,
                    barY,
                    0f,
                    0f,
                    progress,
                    barHeight,
                    barWidth,
                    barHeight
            );
        }

        // 🔢 level text
        if (player.experienceLevel > 0) {
            String levelText = String.valueOf(player.experienceLevel);

            int textX = (context.getScaledWindowWidth()
                    - client.textRenderer.getWidth(levelText)) / 2;

            int textY = barY - 10;

            context.drawText(
                    client.textRenderer,
                    levelText,
                    textX,
                    textY,
                    0x80FF20,
                    true
            );
        }
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