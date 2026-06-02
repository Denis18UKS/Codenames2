package fable.codenames.registry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import fable.codenames.board.BoardService;
import fable.codenames.board.BoardState;
import fable.codenames.board.BoardSync;
import fable.codenames.board.Boards;
import fable.codenames.block.ClickButtonBlock;
import fable.codenames.block.TeamChatBlock;
import fable.codenames.block.entity.ClickButtonBlockEntity;
import fable.codenames.chat.TeamChatPackets;
import fable.codenames.chat.TeamChatRightsMode;
import fable.codenames.chat.TeamChatSync;
import fable.codenames.chat.TeamChats;
import fable.codenames.dev.DevBotService;
import fable.codenames.dev.SoloModeService;
import fable.codenames.entity.HologramProjectorEntity;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.head.HeadRandomizeManager;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.RoleDisplay;
import fable.codenames.role.RoleState;
import fable.codenames.role.RoleValidation;
import fable.codenames.role.Roles;
import fable.codenames.score.TeamScoreState;
import fable.codenames.score.TeamScoreSync;
import fable.codenames.score.TeamScores;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TeamArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public final class RegisterCommands {
    private static final DynamicCommandExceptionType UNKNOWN_TEAM =
            new DynamicCommandExceptionType(name -> Text.literal("Неизвестная команда: " + name));

    private RegisterCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("codenames")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("reset")
                            .executes(context -> {
                                CodenamesGameService.reset(context.getSource().getServer());
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(registerScoreNode())
                    .then(registerBoardNode())
                    .then(registerGameNode())
                    .then(registerButtonNode())
                    .then(registerHologramNode())
                    .then(registerChatNode())
                    .then(registerSoloNode())
                    .then(registerTestBotNode()));

            dispatcher.register(CommandManager.literal("cn_chat")
                    .then(CommandManager.literal("clear")
                            .executes(context -> clearTeamChat(context.getSource()))));

            dispatcher.register(registerRoleNode());
            dispatcher.register(registerLeaderNode());
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> registerRoleNode() {
        return CommandManager.literal("role")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    PlayerRole role = Roles.getState(context.getSource().getServer()).getRole(player.getUuid());

                    if (role == null) {
                        context.getSource().sendFeedback(() -> Text.literal("Роль не выбрана."), false);
                    } else {
                        context.getSource().sendFeedback(() -> Text.literal("Текущая роль: " + role.getId()), false);
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(CommandManager.literal(PlayerRole.LIDER.getId())
                        .executes(context -> setRole(context.getSource(), PlayerRole.LIDER)))
                .then(CommandManager.literal(PlayerRole.GUESSING.getId())
                        .executes(context -> setRole(context.getSource(), PlayerRole.GUESSING)))
                .then(CommandManager.literal("clear")
                        .executes(context -> clearRole(context.getSource())))
                .then(CommandManager.literal("check")
                        .executes(context -> {
                            boolean valid = RoleValidation.validateAndBroadcast(context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.literal(valid
                                    ? "Роли команд настроены корректно."
                                    : "Игра не может начаться, пока не выбран ровно один лидер на команду."), false);
                            return valid ? Command.SINGLE_SUCCESS : 0;
                        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> registerLeaderNode() {
        return CommandManager.literal("lider")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("for")
                        .then(CommandManager.literal("team")
                                .then(CommandManager.argument("team", TeamArgumentType.team())
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> setLeaderForTeam(
                                                        context.getSource(),
                                                        TeamArgumentType.getTeam(context, "team"),
                                                        EntityArgumentType.getPlayer(context, "player"))))
                                        .then(CommandManager.literal("bot")
                                                .then(CommandManager.argument("name", StringArgumentType.word())
                                                        .executes(context -> setLeaderBotForTeam(
                                                                context.getSource(),
                                                                TeamArgumentType.getTeam(context, "team"),
                                                                StringArgumentType.getString(context, "name"))))))));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerScoreNode() {
        return CommandManager.literal("score")
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            Team team = getExistingTeam(context.getSource(), StringArgumentType.getString(context, "team"));
                                            int value = IntegerArgumentType.getInteger(context, "value");

                                            TeamScoreState state = TeamScores.getState(context.getSource().getServer());
                                            state.setScore(team.getName(), value);
                                            TeamScoreSync.syncToAll(context.getSource().getServer());

                                            context.getSource().sendFeedback(() -> Text.literal("Счёт команды " + team.getName() + " установлен на " + value), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            Team team = getExistingTeam(context.getSource(), StringArgumentType.getString(context, "team"));
                                            int value = IntegerArgumentType.getInteger(context, "value");

                                            TeamScoreState state = TeamScores.getState(context.getSource().getServer());
                                            int updated = state.addScore(team.getName(), value);
                                            TeamScoreSync.syncToAll(context.getSource().getServer());

                                            context.getSource().sendFeedback(() -> Text.literal("Счёт команды " + team.getName() + " обновлён до " + updated), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(context -> {
                                    Team team = getExistingTeam(context.getSource(), StringArgumentType.getString(context, "team"));
                                    int value = TeamScores.getState(context.getSource().getServer()).getScore(team.getName());
                                    context.getSource().sendFeedback(() -> Text.literal("Счёт команды " + team.getName() + ": " + value), false);
                                    return value;
                                })))
                .then(CommandManager.literal("reset")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(context -> {
                                    Team team = getExistingTeam(context.getSource(), StringArgumentType.getString(context, "team"));

                                    TeamScoreState state = TeamScores.getState(context.getSource().getServer());
                                    state.resetScore(team.getName());
                                    TeamScoreSync.syncToAll(context.getSource().getServer());

                                    context.getSource().sendFeedback(() -> Text.literal("Счёт команды " + team.getName() + " сброшен"), true);
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerBoardNode() {
        return CommandManager.literal("board")
                .then(CommandManager.literal("clear")
                        .executes(context -> {
                            BoardState state = Boards.getState(context.getSource().getServer());
                            state.clear();
                            BoardSync.syncToAll(context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.literal("Поле очищено."), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("counts")
                        .executes(context -> {
                            context.getSource().sendFeedback(() -> BoardService.progressText(Boards.getState(context.getSource().getServer())), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("randomize")
                        .executes(context -> {
                            BoardState state = Boards.getState(context.getSource().getServer());
                            state.repairCellsFromFields();
                            try {
                                BoardService.randomize(state, context.getSource().getServer());
                            int heads = HeadRandomizeManager.randomize(context.getSource().getServer(), state);
                            BoardSync.syncToAll(context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.literal("Типы объектов распределены автоматически: 8 красных, 8 синих, 4 нейтральных, 1 убийца."), true);
                            context.getSource().sendFeedback(() -> Text.literal("Головы перед полем перемешаны: " + heads + "."), true);
                                context.getSource().sendFeedback(() -> BoardService.progressText(state), false);
                                return Command.SINGLE_SUCCESS;
                            } catch (IllegalStateException exception) {
                                context.getSource().sendError(Text.literal(exception.getMessage()));
                                return 0;
                            }
                        }));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerGameNode() {
        return CommandManager.literal("game")
                .then(CommandManager.literal("start")
                        .executes(context -> CodenamesGameService.start(context.getSource().getServer()) ? Command.SINGLE_SUCCESS : 0))
                .then(CommandManager.literal("stop")
                        .executes(context -> {
                            CodenamesGameService.stop(context.getSource().getServer());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("next")
                        .executes(context -> {
                            Text error = CodenamesGameService.validateNextTurn(context.getSource().getServer());
                            if (error != null) {
                                context.getSource().sendError(error);
                                return 0;
                            }
                            CodenamesGameService.nextTurn(context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.literal("Ход передан следующей команде."), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("reset")
                        .executes(context -> {
                            CodenamesGameService.reset(context.getSource().getServer());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            context.getSource().sendFeedback(() -> CodenamesGameService.status(context.getSource().getServer()), false);
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerButtonNode() {
        return CommandManager.literal("button")
                .then(CommandManager.literal("mode")
                        .then(CommandManager.literal("normal")
                                .executes(context -> setClickButtonMode(context.getSource(), ClickButtonBlockEntity.Mode.NORMAL)))
                        .then(CommandManager.literal("reset")
                                .executes(context -> setClickButtonMode(context.getSource(), ClickButtonBlockEntity.Mode.RESET)))
                        .then(CommandManager.literal("pass_turn")
                                .executes(context -> setClickButtonMode(context.getSource(), ClickButtonBlockEntity.Mode.PASS_TURN))));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerHologramNode() {
        return CommandManager.literal("hologram")
                .then(CommandManager.literal("face")
                        .then(CommandManager.argument("direction", StringArgumentType.word())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    Direction direction = Direction.byName(StringArgumentType.getString(context, "direction"));
                                    if (direction == null || !direction.getAxis().isHorizontal()) {
                                        context.getSource().sendError(Text.literal("Направление: north, south, west или east."));
                                        return 0;
                                    }

                                    HologramProjectorEntity hologram = player.getWorld()
                                            .getEntitiesByClass(HologramProjectorEntity.class, new Box(player.getBlockPos()).expand(8.0D), entity -> true)
                                            .stream()
                                            .min(java.util.Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)))
                                            .orElse(null);
                                    if (hologram == null) {
                                        context.getSource().sendError(Text.literal("Рядом нет голограммы-счётчика."));
                                        return 0;
                                    }

                                    hologram.setYaw(direction.asRotation());
                                    context.getSource().sendFeedback(() -> Text.literal("Направление голограммы обновлено."), true);
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    private static int setClickButtonMode(ServerCommandSource source, ClickButtonBlockEntity.Mode mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        HitResult hitResult = player.raycast(5.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            source.sendError(Text.literal("Посмотри на click_button."));
            return 0;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (!(player.getWorld().getBlockState(pos).getBlock() instanceof ClickButtonBlock)
                || !(player.getWorld().getBlockEntity(pos) instanceof ClickButtonBlockEntity entity)) {
            source.sendError(Text.literal("Посмотри на click_button."));
            return 0;
        }

        entity.setMode(mode);
        source.sendFeedback(() -> Text.literal("Режим кнопки: " + mode.getId()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerChatNode() {
        return CommandManager.literal("chat")
                .then(CommandManager.literal("clear")
                        .executes(context -> {
                            TeamChats.getState(context.getSource().getServer()).clearMessages();
                            TeamChatSync.syncAll(context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.literal("Командный чат очищен."), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("rights")
                        .then(CommandManager.literal("on")
                                .executes(context -> setTeamChatRightsMode(context.getSource(), true)))
                        .then(CommandManager.literal("off")
                                .executes(context -> setTeamChatRightsMode(context.getSource(), false)))
                        .then(CommandManager.literal("status")
                                .executes(context -> {
                                    context.getSource().sendFeedback(() -> Text.literal("Team chat rights mode: "
                                            + (TeamChatRightsMode.isEnabled() ? "on" : "off")), false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                ;
    }

    private static int setTeamChatRightsMode(ServerCommandSource source, boolean enabled) {
        TeamChatRightsMode.setEnabled(enabled);
        TeamChatSync.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("Team chat rights mode: " + (enabled ? "on" : "off")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearTeamChat(ServerCommandSource source) {
        TeamChats.getState(source.getServer()).clearMessages();
        TeamChatSync.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("Командный чат очищен."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openChatMoveEditor(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        HitResult hitResult = player.raycast(5.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            source.sendError(Text.literal("Посмотри на баннер командного чата."));
            return 0;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (!(player.getWorld().getBlockState(pos).getBlock() instanceof TeamChatBlock)) {
            source.sendError(Text.literal("Посмотри на баннер командного чата."));
            return 0;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send(player, TeamChatPackets.OPEN_MOVE_BANNER, buf);
        return Command.SINGLE_SUCCESS;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerTestBotNode() {
        return CommandManager.literal("testbot")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("team", TeamArgumentType.team())
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            Team team = TeamArgumentType.getTeam(context, "team");
                                            String name = StringArgumentType.getString(context, "name");
                                            DevBotService.addBot(context.getSource().getServer(), team, name);
                                            context.getSource().sendFeedback(() -> Text.literal("Добавлен тестовый бот " + name + " в команду " + team.getName() + "."), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    boolean removed = DevBotService.removeBot(context.getSource().getServer(), name);
                                    if (!removed) {
                                        context.getSource().sendError(Text.literal("Тестовый бот " + name + " не найден."));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(() -> Text.literal("Тестовый бот " + name + " удалён."), true);
                                    RoleValidation.validateAndBroadcast(context.getSource().getServer());
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(CommandManager.literal("role")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.literal(PlayerRole.LIDER.getId())
                                        .executes(context -> setTestBotRole(context.getSource(), StringArgumentType.getString(context, "name"), PlayerRole.LIDER)))
                                .then(CommandManager.literal(PlayerRole.GUESSING.getId())
                                        .executes(context -> setTestBotRole(context.getSource(), StringArgumentType.getString(context, "name"), PlayerRole.GUESSING)))
                                .then(CommandManager.literal("clear")
                                        .executes(context -> setTestBotRole(context.getSource(), StringArgumentType.getString(context, "name"), null)))))
                .then(CommandManager.literal("list")
                        .executes(context -> {
                            context.getSource().sendFeedback(() -> DevBotService.describeBots(context.getSource().getServer()), false);
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> registerSoloNode() {
        return CommandManager.literal("solo")
                .then(CommandManager.literal("enable")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            SoloModeService.enable(context.getSource().getServer(), player.getUuid());
                            context.getSource().sendFeedback(() -> Text.literal("Solo-режим включён: этот игрок теперь считается и лидером, и угадывающим для теста."), true);
                            RoleValidation.validateAndBroadcast(context.getSource().getServer());
                            BoardSync.syncToAll(context.getSource().getServer());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("disable")
                        .executes(context -> {
                            SoloModeService.disable(context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.literal("Solo-режим выключен."), true);
                            RoleValidation.validateAndBroadcast(context.getSource().getServer());
                            BoardSync.syncToAll(context.getSource().getServer());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            java.util.UUID activePlayer = SoloModeService.getState(context.getSource().getServer()).getActivePlayer();
                            if (activePlayer == null) {
                                context.getSource().sendFeedback(() -> Text.literal("Solo-режим сейчас выключен."), false);
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("Solo-режим включён для UUID: " + activePlayer), false);
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static Team getExistingTeam(ServerCommandSource source, String teamName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Team team = source.getServer().getScoreboard().getTeam(teamName);
        if (team == null) {
            throw UNKNOWN_TEAM.create(teamName);
        }
        return team;
    }

    private static int setRole(ServerCommandSource source, PlayerRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        AbstractTeam team = player.getScoreboardTeam();
        if (team == null) {
            source.sendError(Text.literal("Сначала вступите в команду через /team join."));
            return 0;
        }

        RoleState state = Roles.getState(source.getServer());
        state.setRole(player.getUuid(), role);
        RoleDisplay.refreshPlayer(player, role);

        source.sendFeedback(() -> Text.literal("Роль " + role.getId() + " выбрана для команды " + team.getName() + "."), false);
        RoleValidation.validateAndBroadcast(source.getServer());
        BoardSync.syncToAll(source.getServer());
        TeamChatSync.syncAll(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int clearRole(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        RoleState state = Roles.getState(source.getServer());
        state.clearRole(player.getUuid());
        RoleDisplay.refreshPlayer(player, null);

        source.sendFeedback(() -> Text.literal("Роль очищена."), false);
        RoleValidation.validateAndBroadcast(source.getServer());
        BoardSync.syncToAll(source.getServer());
        TeamChatSync.syncAll(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int setLeaderForTeam(ServerCommandSource source, Team team, ServerPlayerEntity player) {
        AbstractTeam playerTeam = player.getScoreboardTeam();
        if (playerTeam == null || !playerTeam.getName().equals(team.getName())) {
            source.sendError(Text.literal("Игрок " + player.getName().getString() + " не состоит в команде " + team.getName() + "."));
            return 0;
        }

        clearExistingLeadersForTeam(source, team, null);

        RoleState state = Roles.getState(source.getServer());
        state.setRole(player.getUuid(), PlayerRole.LIDER);
        RoleDisplay.refreshPlayer(player, PlayerRole.LIDER);

        source.sendFeedback(() -> Text.literal("Лидером команды " + team.getName() + " назначен " + player.getName().getString() + "."), true);
        RoleValidation.validateAndBroadcast(source.getServer());
        BoardSync.syncToAll(source.getServer());
        TeamChatSync.syncAll(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int setLeaderBotForTeam(ServerCommandSource source, Team team, String name) {
        String entry = DevBotService.entryName(name);
        Team botTeam = source.getServer().getScoreboard().getPlayerTeam(entry);
        if (botTeam == null || !botTeam.getName().equals(team.getName())) {
            source.sendError(Text.literal("Тестовый бот " + name + " не состоит в команде " + team.getName() + "."));
            return 0;
        }

        clearExistingLeadersForTeam(source, team, entry);
        DevBotService.setRole(source.getServer(), name, PlayerRole.LIDER);

        source.sendFeedback(() -> Text.literal("Лидером команды " + team.getName() + " назначен тестовый бот " + name + "."), true);
        RoleValidation.validateAndBroadcast(source.getServer());
        BoardSync.syncToAll(source.getServer());
        TeamChatSync.syncAll(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static int setTestBotRole(ServerCommandSource source, String name, PlayerRole role) {
        String entry = DevBotService.entryName(name);
        Team botTeam = source.getServer().getScoreboard().getPlayerTeam(entry);
        if (botTeam == null) {
            source.sendError(Text.literal("Тестовый бот " + name + " не найден."));
            return 0;
        }

        if (role == PlayerRole.LIDER) {
            clearExistingLeadersForTeam(source, botTeam, entry);
        }

        DevBotService.setRole(source.getServer(), name, role);
        String roleText = role == null ? "очищена" : role.getId();
        source.sendFeedback(() -> Text.literal("Роль тестового бота " + name + ": " + roleText + "."), true);
        RoleValidation.validateAndBroadcast(source.getServer());
        BoardSync.syncToAll(source.getServer());
        TeamChatSync.syncAll(source.getServer());
        return Command.SINGLE_SUCCESS;
    }

    private static void clearExistingLeadersForTeam(ServerCommandSource source, Team team, String preservedBotEntry) {
        RoleState state = Roles.getState(source.getServer());

        for (ServerPlayerEntity onlinePlayer : source.getServer().getPlayerManager().getPlayerList()) {
            AbstractTeam onlineTeam = onlinePlayer.getScoreboardTeam();
            if (onlineTeam != null
                    && onlineTeam.getName().equals(team.getName())
                    && state.getRole(onlinePlayer.getUuid()) == PlayerRole.LIDER) {
                state.clearRole(onlinePlayer.getUuid());
                RoleDisplay.refreshPlayer(onlinePlayer, null);
            }
        }

        for (String entry : team.getPlayerList()) {
            if (!DevBotService.isBotEntry(entry) || entry.equals(preservedBotEntry)) {
                continue;
            }
            if (DevBotService.getRole(source.getServer(), entry) == PlayerRole.LIDER) {
                state.clearRole(DevBotService.uuid(entry));
            }
        }
    }
}
