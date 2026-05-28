package fable.codenames.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class WrittenBookModelMixin {

    private static final ModelIdentifier SHORT_BOOK_MODEL =
            new ModelIdentifier("codenames", "written_book_short", "inventory");

    private static final ModelIdentifier FULL_BOOK_MODEL =
            new ModelIdentifier("codenames", "written_book_full", "inventory");

    @Inject(
            method = "getModel(Lnet/minecraft/item/ItemStack;" +
                    "Lnet/minecraft/world/World;" +
                    "Lnet/minecraft/entity/LivingEntity;" +
                    "I)Lnet/minecraft/client/render/model/BakedModel;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void codenames$overrideWrittenBookModel(
            ItemStack stack,
            World world,
            LivingEntity entity,
            int seed,
            CallbackInfoReturnable<BakedModel> cir
    ) {

        if (stack == null || stack.getItem() != Items.WRITTEN_BOOK) {
            return;
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return;
        }

        String type = nbt.getString("CodenamesRuleBook");

        ModelIdentifier modelId;

        if ("short".equals(type)) {
            modelId = SHORT_BOOK_MODEL;
        } else if ("full".equals(type)) {
            modelId = FULL_BOOK_MODEL;
        } else {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        BakedModelManager manager = client.getBakedModelManager();

        BakedModel model = manager.getModel(modelId);

        if (model != null) {
            cir.setReturnValue(model);
        }
    }
}