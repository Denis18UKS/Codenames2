package fable.codenames.item;

import fable.codenames.block.entity.TeamChatBlockEntity;
import fable.codenames.game.CodenamesGameService;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeamChatConfiguratorItem extends Item {
    private static final String TEAM_KEY = "TeamChatTeam";

    public TeamChatConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isSneaking() && !world.isClient() && user instanceof ServerPlayerEntity player) {
            cycleTeam(stack, player);
            player.sendMessage(selectedText(stack), true);
            return TypedActionResult.success(stack, false);
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        if (context.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (context.getPlayer().isSneaking()) {
            cycleTeam(context.getStack(), player);
            player.sendMessage(selectedText(context.getStack()), true);
            return ActionResult.SUCCESS;
        }

        return applyToBanner(player, context.getStack(), context.getBlockPos()) ? ActionResult.CONSUME : ActionResult.PASS;
    }

    public static boolean applyToBanner(ServerPlayerEntity player, ItemStack stack, BlockPos pos) {
        if (!(player.getWorld().getBlockEntity(pos) instanceof TeamChatBlockEntity entity)) {
            return false;
        }

        String teamName = getSelectedTeam(stack, player);
        if (teamName.isBlank()) {
            player.sendMessage(Text.literal("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0432\u044b\u0431\u0435\u0440\u0438 \u043a\u043e\u043c\u0430\u043d\u0434\u0443: Shift + \u041f\u041a\u041c.").formatted(Formatting.YELLOW), true);
            return false;
        }

        entity.setTeamName(teamName);
        player.sendMessage(Text.literal("\u0427\u0430\u0442-\u0431\u0430\u043d\u043d\u0435\u0440 \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d \u043a \u043a\u043e\u043c\u0430\u043d\u0434\u0435: " + displayTeam(teamName)).formatted(teamFormatting(teamName)), true);
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Shift + \u041f\u041a\u041c: \u0432\u044b\u0431\u0440\u0430\u0442\u044c \u043a\u043e\u043c\u0430\u043d\u0434\u0443").formatted(Formatting.GRAY));
        tooltip.add(selectedText(stack).copy().formatted(Formatting.YELLOW));
    }

    private static void cycleTeam(ItemStack stack, ServerPlayerEntity player) {
        List<String> teams = playableTeams(player);
        if (teams.isEmpty()) {
            stack.getOrCreateNbt().putString(TEAM_KEY, "");
            return;
        }
        String current = stack.hasNbt() ? stack.getNbt().getString(TEAM_KEY) : "";
        int index = teams.indexOf(current);
        stack.getOrCreateNbt().putString(TEAM_KEY, teams.get((index + 1 + teams.size()) % teams.size()));
    }

    private static String getSelectedTeam(ItemStack stack, ServerPlayerEntity player) {
        String selected = stack.hasNbt() ? stack.getNbt().getString(TEAM_KEY) : "";
        if (!selected.isBlank() && player.getServer().getScoreboard().getTeam(selected) != null) {
            return selected;
        }
        List<String> teams = playableTeams(player);
        return teams.isEmpty() ? "" : teams.get(0);
    }

    private static List<String> playableTeams(ServerPlayerEntity player) {
        List<String> teams = new ArrayList<>();
        for (Team team : player.getServer().getScoreboard().getTeams()) {
            if (CodenamesGameService.expectedTypeForTeam(team.getName()) != fable.codenames.board.BoardCellType.UNASSIGNED) {
                teams.add(team.getName());
            }
        }
        teams.sort(Comparator.comparingInt(TeamChatConfiguratorItem::teamOrder).thenComparing(value -> value));
        return teams;
    }

    private static int teamOrder(String teamName) {
        return CodenamesGameService.expectedTypeForTeam(teamName) == fable.codenames.board.BoardCellType.RED ? 0 : 1;
    }

    private static Text selectedText(ItemStack stack) {
        String teamName = stack.hasNbt() ? stack.getNbt().getString(TEAM_KEY) : "";
        return Text.literal("\u0412\u044b\u0431\u0440\u0430\u043d\u043e: " + (teamName.isBlank() ? "-" : displayTeam(teamName))).formatted(teamFormatting(teamName));
    }

    private static String displayTeam(String teamName) {
        return switch (CodenamesGameService.expectedTypeForTeam(teamName)) {
            case RED -> "\u043a\u0440\u0430\u0441\u043d\u0430\u044f \u043a\u043e\u043c\u0430\u043d\u0434\u0430";
            case BLUE -> "\u0441\u0438\u043d\u044f\u044f \u043a\u043e\u043c\u0430\u043d\u0434\u0430";
            default -> teamName;
        };
    }

    private static Formatting teamFormatting(String teamName) {
        return switch (CodenamesGameService.expectedTypeForTeam(teamName)) {
            case RED -> Formatting.RED;
            case BLUE -> Formatting.AQUA;
            default -> Formatting.WHITE;
        };
    }
}
