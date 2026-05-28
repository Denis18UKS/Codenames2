package fable.codenames.client.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void codenames$blockVanillaChatSending(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (chatText.startsWith("/")) {
            return;
        }
        cir.setReturnValue(true);
    }
}
