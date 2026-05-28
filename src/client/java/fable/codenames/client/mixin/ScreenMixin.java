package fable.codenames.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    private static final String SCHOLAR_BOOK_VIEW_SCREEN = "io.github.mortuusars.scholar.screen.SpreadBookViewScreen";

    @Inject(method = "render", at = @At("TAIL"))
    private void codenames$drawVisibleScholarPageButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object screen = this;
        if (!SCHOLAR_BOOK_VIEW_SCREEN.equals(screen.getClass().getName())) {
            return;
        }

        drawPageButton(context, screen, "prevButton", "<");
        drawPageButton(context, screen, "nextButton", ">");
    }

    private static void drawPageButton(DrawContext context, Object screen, String fieldName, String label) {
        ButtonWidget button = getButton(screen, fieldName);
        if (button == null || !button.visible) {
            return;
        }

        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();

        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xAA176E78);
        context.fill(x, y, x + width, y + height, 0xEE2DAEBF);
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal(label),
                x + width / 2,
                y + (height - 8) / 2,
                0xFFFFFFFF
        );
    }

    private static ButtonWidget getButton(Object screen, String fieldName) {
        try {
            Field field = screen.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(screen);
            return value instanceof ButtonWidget button ? button : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
