package fable.codenames.protection;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.ActionResult;

public final class WorldProtectionService {
    private WorldProtectionService() {
    }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                entity instanceof PaintingEntity && !player.isCreative() && !player.isSpectator()
                        ? ActionResult.FAIL
                        : ActionResult.PASS);
    }
}
