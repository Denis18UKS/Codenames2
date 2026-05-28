package fable.codenames.client.command;

import com.mojang.brigadier.Command;
import fable.codenames.block.TeamChatBlock;
import fable.codenames.client.screen.TeamChatBannerMoveScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class TeamChatBannerMoveClientCommand {
    private TeamChatBannerMoveClientCommand() {
    }

    public static int open(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            source.sendError(Text.literal("Look at a team chat banner first."));
            return 0;
        }

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        if (!(client.world.getBlockState(pos).getBlock() instanceof TeamChatBlock)) {
            source.sendError(Text.literal("Look at a team chat banner first."));
            return 0;
        }

        client.setScreen(new TeamChatBannerMoveScreen(pos));
        return Command.SINGLE_SUCCESS;
    }
}
