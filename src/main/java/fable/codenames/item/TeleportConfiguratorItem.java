package fable.codenames.item;

import fable.codenames.game.CodenamesGameService;
import fable.codenames.game.CodenamesRoundState;
import fable.codenames.teleport.TeleportPointService;
import fable.codenames.teleport.TeleportPointState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TeleportConfiguratorItem extends Item {
    public TeleportConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        List<Choice> choices = choices(player);
        if (choices.isEmpty()) {
            player.sendMessage(Text.literal("Сначала создай реальные scoreboard-команды для красных и синих.").formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }

        ItemStack stack = player.getStackInHand(context.getHand());
        Choice choice = choices.get(Math.floorMod(stack.getOrCreateNbt().getInt("choice"), choices.size()));
        stack.getOrCreateNbt().putInt("choice", choices.indexOf(choice) + 1);
        TeleportPointState.get(player.getServer()).set(choice.key(), context.getBlockPos());

        player.sendMessage(Text.literal("ТП-точка: ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(choice.label()).formatted(choice.color()))
                .append(Text.literal(" -> " + formatPos(context.getBlockPos())).formatted(Formatting.WHITE)), false);
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("ПКМ по блоку: назначить следующую ТП-точку").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Порядок: лобби, затем комнаты 1-3: лидеры, красные, синие").formatted(Formatting.GRAY));
    }

    private static List<Choice> choices(ServerPlayerEntity player) {
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(TeleportPointService.LOBBY, "Лобби", Formatting.GOLD));

        boolean hasRed = false;
        boolean hasBlue = false;
        for (Team team : player.getServer().getScoreboard().getTeams()) {
            switch (CodenamesGameService.expectedTypeForTeam(team.getName())) {
                case RED -> hasRed = true;
                case BLUE -> hasBlue = true;
                default -> {
                }
            }
        }

        for (int room = 1; room <= CodenamesRoundState.ROOM_COUNT; room++) {
            choices.add(new Choice("room" + room + "|leaders", "Комната " + room + " / лидеры", Formatting.GOLD));
            if (hasRed) {
                choices.add(new Choice("room" + room + "|red_guessing", "Комната " + room + " / красные угадывающие", Formatting.RED));
            }
            if (hasBlue) {
                choices.add(new Choice("room" + room + "|blue_guessing", "Комната " + room + " / синие угадывающие", Formatting.AQUA));
            }
        }
        return choices;
    }

    private static String formatPos(net.minecraft.util.math.BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private record Choice(String key, String label, Formatting color) {
    }
}
