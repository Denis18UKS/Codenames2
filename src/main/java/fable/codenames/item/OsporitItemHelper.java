package fable.codenames.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;

public final class OsporitItemHelper {
    private OsporitItemHelper() {
    }

    public static boolean isOsporit(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(ModItems.OSPORIT.getItem());
    }

    public static boolean isRestrictedFor(PlayerEntity player, ItemStack stack) {
        return player != null && !player.isCreative() && isOsporit(stack);
    }

    public static int findFirstAllowedHotbarSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
            if (!isOsporit(inventory.getStack(slot))) {
                return slot;
            }
        }
        return inventory.selectedSlot;
    }
}
