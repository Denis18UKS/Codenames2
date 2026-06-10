package fable.codenames.mixin;

import fable.codenames.entity.PassTurnHologramEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    
    @Inject(method = "readNbt", at = @At("RETURN"))
    private void afterReadNbt(NbtCompound nbt, CallbackInfo ci) {
        // Проверяем, что это наша сущность
        if ((Object) this instanceof PassTurnHologramEntity hologram) {
            // После чтения NBT принудительно обновляем позицию если есть координаты кнопки
            if (nbt.contains("ButtonX") && nbt.contains("ButtonY") && nbt.contains("ButtonZ")) {
                hologram.setPosition(
                    nbt.getInt("ButtonX") + 0.5,
                    nbt.getInt("ButtonY") + 0.8,
                    nbt.getInt("ButtonZ") + 0.5
                );
            }
        }
    }
}