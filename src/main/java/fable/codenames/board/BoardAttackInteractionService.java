package fable.codenames.board;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class BoardAttackInteractionService {
    private BoardAttackInteractionService() {
    }

    public static void init() {
        AttackBlockCallback.EVENT.register(BoardAttackInteractionService::handleAttack);
    }

    private static ActionResult handleAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (world.isClient() || player.isSpectator() || world.getServer() == null) {
            return ActionResult.PASS;
        }

        return Boards.getState(world.getServer()).contains(pos) ? ActionResult.FAIL : ActionResult.PASS;
    }
}
