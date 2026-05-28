package fable.codenames.item;

import fable.codenames.board.BoardCellType;
import fable.codenames.board.BoardSelections;
import fable.codenames.board.BoardService;
import fable.codenames.board.BoardState;
import fable.codenames.board.BoardSync;
import fable.codenames.board.Boards;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BoardConfiguratorItem extends Item {
    public BoardConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        BoardState state = Boards.getState(player.getServer());
        if (player.isSneaking()) {
            return handleSelection(player, context, state);
        }

        if (!state.contains(context.getBlockPos())) {
            player.sendMessage(Text.literal("Этот блок не входит в игровое поле 7x3. Сначала задай две угловые точки с зажатым Shift.").formatted(Formatting.YELLOW), true);
            return ActionResult.CONSUME;
        }

        BoardCellType nextType = state.getType(context.getBlockPos()).next();
        state.setType(context.getBlockPos(), nextType);
        BoardSync.syncToAll(player.getServer());
        player.sendMessage(Text.literal("Ячейка " + formatPos(context.getBlockPos()) + " -> " + nextType.getLabel().getString()).formatted(Formatting.GOLD), true);
        player.sendMessage(BoardService.progressText(state), false);
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, net.minecraft.util.Hand hand) {
        if (!world.isClient() && user.isSneaking() && user instanceof ServerPlayerEntity player) {
            player.sendMessage(BoardService.progressText(Boards.getState(player.getServer())), false);
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Shift + ПКМ: две угловые точки поля 7x3").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("ПКМ по ячейке: сменить тип объекта").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Shift + ПКМ в воздух: показать прогресс разметки").formatted(Formatting.GRAY));
    }

    private ActionResult handleSelection(ServerPlayerEntity player, ItemUsageContext context, BoardState state) {
        var firstCorner = BoardSelections.getFirstCorner(player.getUuid());
        if (firstCorner == null) {
            BoardSelections.setFirstCorner(player.getUuid(), context.getBlockPos());
            player.sendMessage(Text.literal("Первая точка поля сохранена: " + formatPos(context.getBlockPos())).formatted(Formatting.GREEN), true);
            return ActionResult.CONSUME;
        }

        try {
            state.setBoard(BoardService.createBoardPositions(firstCorner, context.getBlockPos()));
            BoardSelections.clearFirstCorner(player.getUuid());
            BoardSync.syncToAll(player.getServer());
            player.sendMessage(Text.literal("Поле 7x3 зарегистрировано. Теперь можно назначать типы или использовать /codenames board randomize.").formatted(Formatting.GREEN), false);
            player.sendMessage(BoardService.progressText(state), false);
            return ActionResult.CONSUME;
        } catch (IllegalArgumentException exception) {
            BoardSelections.clearFirstCorner(player.getUuid());
            player.sendMessage(Text.literal(exception.getMessage()).formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }
    }

    private static String formatPos(net.minecraft.util.math.BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
