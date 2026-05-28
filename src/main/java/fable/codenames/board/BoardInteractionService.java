package fable.codenames.board;

import fable.codenames.block.CodenamesHeadBlock;
import fable.codenames.dev.SoloModeService;
import fable.codenames.game.CodenamesGames;
import fable.codenames.game.CodenamesPhase;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.item.ModItems;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public final class BoardInteractionService {
    private BoardInteractionService() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register(BoardInteractionService::handleUse);
    }

    private static ActionResult handleUse(PlayerEntity playerEntity, net.minecraft.world.World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient() || !(playerEntity instanceof ServerPlayerEntity player) || player.isSpectator()) {
            return ActionResult.PASS;
        }

        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }

        ItemStack heldStack = player.getStackInHand(hand);
        if (!heldStack.isEmpty() && heldStack.isOf(ModItems.BOARD_CONFIGURATOR.getItem())) {
            return ActionResult.PASS;
        }

        return selectCell(player, hitResult.getBlockPos());
    }

    static ActionResult selectCell(ServerPlayerEntity player, BlockPos pos) {
        BoardState boardState = Boards.getState(player.getServer());
        ClickedCell clickedCell = resolveClickedCell(player, boardState, pos);
        if (clickedCell == null) {
            return ActionResult.PASS;
        }
        BlockPos canonicalPos = clickedCell.canonicalPos();

        if (!canSelect(player)) {
            return ActionResult.FAIL;
        }

        String teamName = TeamService.getTeamName(player);
        if (teamName == null) {
            player.sendMessage(Text.literal("Сначала вступите в команду.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (!CodenamesGameService.canPlayerSelect(player)) {
            player.sendMessage(Text.literal("Сейчас не ваш ход или ещё не дана подсказка.").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }

        if (CodenamesGames.getState(player.getServer()).getPhase() != CodenamesPhase.STOPPED
                && boardState.getType(canonicalPos) == BoardCellType.UNASSIGNED) {
            player.sendMessage(Text.literal("\u042d\u0442\u043e\u0442 \u043e\u0431\u044a\u0435\u043a\u0442 \u0443\u0436\u0435 \u043e\u0442\u043a\u0440\u044b\u0442.").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }

        BoardSelectionState.Vote current = BoardSelectionState.getVote(player.getUuid());
        if (current != null && canonicalPos.equals(current.pos())) {
            BoardSelectionState.clearVote(player.getUuid());
            BoardVoteService.updateConfirmation(player.getServer(), teamName);
            BoardSelectionSync.syncToAll(player.getServer());
            player.sendMessage(Text.literal("Ваш голос снят.").formatted(Formatting.YELLOW), true);
            return ActionResult.SUCCESS;
        }

        BoardSelectionState.setVote(player.getUuid(), teamName, canonicalPos, clickedCell.displayPos());
        BoardVoteService.updateConfirmation(player.getServer(), teamName);
        BoardSelectionSync.syncToAll(player.getServer());
        return ActionResult.SUCCESS;
    }

    private static ClickedCell resolveClickedCell(ServerPlayerEntity player, BoardState boardState, BlockPos pos) {
        BlockPos canonical = boardState.resolvePosition(pos);
        if (canonical != null) {
            return new ClickedCell(canonical, pos.toImmutable());
        }

        BlockState state = player.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof CodenamesHeadBlock && state.contains(CodenamesHeadBlock.FACING)) {
            BlockPos displayPos = pos.offset(state.get(CodenamesHeadBlock.FACING).getOpposite());
            canonical = boardState.resolvePosition(displayPos);
            return canonical == null ? null : new ClickedCell(canonical, displayPos.toImmutable());
        }
        return null;
    }

    private static boolean canSelect(ServerPlayerEntity player) {
        if (SoloModeService.isEnabled(player.getServer(), player.getUuid())) {
            return true;
        }
        return Roles.getState(player.getServer()).getRole(player.getUuid()) == PlayerRole.GUESSING;
    }

    private static Formatting teamFormatting(String teamName) {
        return switch (CodenamesGameService.expectedTypeForTeam(teamName)) {
            case RED -> Formatting.RED;
            case BLUE -> Formatting.AQUA;
            default -> Formatting.WHITE;
        };
    }

    private record ClickedCell(BlockPos canonicalPos, BlockPos displayPos) {
    }
}
