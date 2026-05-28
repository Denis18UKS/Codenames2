package fable.codenames.protection;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.util.ActionResult;

public final class FlowerPotProtectionService {
    private FlowerPotProtectionService() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!player.isCreative()
                    && !player.isSpectator()
                    && world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof FlowerPotBlock
                    && !world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.FLOWER_POT)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
