package fable.codenames.item;

import fable.codenames.game.CodenamesGameService;
import fable.codenames.pedestal.PedestalState;
import fable.codenames.role.PlayerRole;
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
import java.util.Comparator;
import java.util.List;

public class PedestalConfiguratorItem extends Item {
    public PedestalConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        PedestalState state = PedestalState.get(player.getServer());
        if (player.isSneaking()) {
            state.remove(context.getBlockPos());
            player.sendMessage(Text.literal("Пьедестал снят: " + formatPos(context.getBlockPos())).formatted(Formatting.YELLOW), false);
            return ActionResult.CONSUME;
        }

        List<Choice> choices = choices(player);
        if (choices.isEmpty()) {
            player.sendMessage(Text.literal("Сначала создай реальные scoreboard-команды для красных и синих.").formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }

        Choice choice = nextChoice(choices, state.get(context.getBlockPos()));
        state.set(context.getBlockPos(), choice.teamName(), choice.role());
        player.sendMessage(Text.literal("Пьедестал " + formatPos(context.getBlockPos()) + ": ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(choice.teamName()).formatted(teamFormatting(choice.teamName())))
                .append(Text.literal(" / " + roleLabel(choice.role())).formatted(Formatting.WHITE)), false);
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("ПКМ по блоку: назначить/переключить пьедестал").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Shift + ПКМ по блоку: снять пьедестал").formatted(Formatting.GRAY));
    }

    private static List<Choice> choices(ServerPlayerEntity player) {
        List<Team> teams = new ArrayList<>(player.getServer().getScoreboard().getTeams());
        teams.removeIf(team -> CodenamesGameService.expectedTypeForTeam(team.getName()).name().equals("UNASSIGNED"));
        teams.sort(Comparator.comparingInt(team -> CodenamesGameService.expectedTypeForTeam(team.getName()).ordinal()));

        List<Choice> choices = new ArrayList<>();
        for (Team team : teams) {
            choices.add(new Choice(team.getName(), PlayerRole.LIDER));
            choices.add(new Choice(team.getName(), PlayerRole.GUESSING));
        }
        return choices;
    }

    private static Choice nextChoice(List<Choice> choices, PedestalState.Assignment current) {
        if (current == null) {
            return choices.get(0);
        }

        for (int i = 0; i < choices.size(); i++) {
            Choice choice = choices.get(i);
            if (choice.teamName().equals(current.teamName()) && choice.role() == current.role()) {
                return choices.get((i + 1) % choices.size());
            }
        }
        return choices.get(0);
    }

    private static Formatting teamFormatting(String teamName) {
        return switch (CodenamesGameService.expectedTypeForTeam(teamName)) {
            case RED -> Formatting.RED;
            case BLUE -> Formatting.AQUA;
            default -> Formatting.WHITE;
        };
    }

    private static String roleLabel(PlayerRole role) {
        return role == PlayerRole.LIDER ? "лидер" : "угадывающий";
    }

    private static String formatPos(net.minecraft.util.math.BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private record Choice(String teamName, PlayerRole role) {
    }
}
