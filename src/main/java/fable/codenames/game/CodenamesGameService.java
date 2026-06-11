package fable.codenames.game;

import fable.codenames.board.BoardCellType;
import fable.codenames.board.BoardSelectionState;
import fable.codenames.board.BoardSelectionSync;
import fable.codenames.board.BoardService;
import fable.codenames.board.BoardState;
import fable.codenames.board.BoardSync;
import fable.codenames.board.Boards;
import fable.codenames.board.TeamService;
import fable.codenames.block.ClickButtonBlock;
import fable.codenames.block.ModBlocks;
import fable.codenames.block.entity.ClickButtonBlockEntity;
import fable.codenames.chat.TeamChatMessage;
import fable.codenames.chat.TeamChatSync;
import fable.codenames.chat.TeamChats;
import fable.codenames.head.HeadRandomizeManager;
import fable.codenames.item.ModItems;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.RoleValidation;
import fable.codenames.role.Roles;
import fable.codenames.score.TeamScoreState;
import fable.codenames.score.TeamScoreSync;
import fable.codenames.score.TeamScores;
import fable.codenames.teleport.TeleportPointService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import fable.codenames.entity.PassTurnHologramEntity;

public final class CodenamesGameService {

    private static final java.util.Map<BlockPos, PassTurnHologramEntity> PASS_HOLOGRAMS = new java.util.HashMap<>();
    private static final long CLUE_TICKS = 20L * 60L;
    private static final long GUESSING_TICKS = 20L * 60L;
    private static final long DISPUTE_TICKS = 20L * 30L;
    private static final long PLAYER_RESUME_DELAY_TICKS = 20L * 5L;
    private static final int UNLIMITED_CLUE = -1;
    private static final List<BlockPos> BLUE_TURN_BUTTONS = List.of(
            new BlockPos(-33, -59, -60),
            new BlockPos(-32, -59, -118),
            new BlockPos(-28, -59, -186));
    private static final List<BlockPos> RED_TURN_BUTTONS = List.of(
            new BlockPos(-23, -59, -49),
            new BlockPos(-22, -59, -109),
            new BlockPos(-18, -59, -179));
    private static final long LOBBY_TELEPORT_DELAY_TICKS = 20L * 20L;
    private static long pendingLobbyTeleportTick = -1L;
    private static boolean pausedForMissingPlayers;
    private static long pausedRemainingTicks;
    private static long resumeAfterPlayersReadyTick = -1L;

    // ========== Управление таймером ==========
    private static boolean timerPaused = false;
    private static long timerPausedRemaining = 0L;
    private static long customClueTicks = CLUE_TICKS;
    private static long customGuessingTicks = GUESSING_TICKS;

    private CodenamesGameService() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CodenamesGameService::tick);
    }

    public static boolean start(MinecraftServer server) {
        if (!RoleValidation.validateAndBroadcast(server)) {
            return false;
        }
        pendingLobbyTeleportTick = -1L;
        clearMissingPlayersPause();
        prepareWorldAndPlayers(server);

        List<String> teams = playableTeams(server);
        if (teams.size() < 2) {
            server.getPlayerManager()
                    .broadcast(Text.literal("Нужно минимум две команды для старта.").formatted(Formatting.RED), false);
            return false;
        }

        BoardState board = Boards.getState(server);
        board.repairCellsFromFields();
        if (board.size() != 21) {
            server.getPlayerManager()
                    .broadcast(Text.literal("Сначала зарегистрируй поле 7x3.").formatted(Formatting.RED), false);
            return false;
        }
        resetBoardCells(server.getOverworld(), board);
        try {
            BoardService.randomize(board, server);
            HeadRandomizeManager.randomize(server, board);
            BoardSync.syncToAll(server);
        } catch (IllegalStateException exception) {
            server.getPlayerManager().broadcast(Text.literal(exception.getMessage()).formatted(Formatting.RED), false);
            return false;
        }
        CodenamesGameSnapshot.capture(server.getOverworld(), board);

        TeamScoreState scores = TeamScores.getState(server);
        for (String team : teams) {
            scores.setScore(team, 8);
        }
        TeamScoreSync.syncToAll(server);

        startCluePhase(server, teams.get(0));
        TeleportPointService.teleportPlayersToRoundRoom(server);
        giveOsporitItems(server);
        giveRuleBooks(server);
        GameTimerSync.syncToAll(server);
        server.getPlayerManager().broadcast(
                Text.literal("Игра началась. Лидеры по очереди дают подсказки в формате: ")
                        .formatted(Formatting.GOLD)
                        .append(
                                Text.literal("слово число")
                                        .formatted(Formatting.WHITE)),
                false);
        return true;
    }

    public static void stop(MinecraftServer server) {
        CodenamesGames.getState(server).stop();
        clearMissingPlayersPause();
        resetTimers();
        BoardSelectionState.clearAll();
        BoardSelectionSync.syncToAll(server);
        clearOsporitItems(server);
        scheduleLobbyTeleport(server);
        giveRuleBooks(server);
        clearTeamChat(server);
        syncTurnButtons(server);
        GameTimerSync.syncToAll(server);
        server.getPlayerManager().broadcast(Text.literal("Игра остановлена.").formatted(Formatting.YELLOW), false);
    }

    public static void reset(MinecraftServer server) {
        reset(server, true);
    }

    public static void resetWithoutLobbyTeleport(MinecraftServer server) {
        reset(server, false);
    }

    private static void reset(MinecraftServer server, boolean teleportToLobby) {
        CodenamesGames.getState(server).stop();
        clearMissingPlayersPause();
        resetTimers();
        BoardSelectionState.clearAll();
        BoardSelectionSync.syncToAll(server);
        clearOsporitItems(server);
        if (teleportToLobby) {
            scheduleLobbyTeleport(server);
        } else {
            pendingLobbyTeleportTick = -1L;
        }
        giveRuleBooks(server);

        BoardState board = Boards.getState(server);
        CodenamesGameSnapshot.restore(server.getOverworld(), board);
        BoardSync.syncToAll(server);

        TeamScores.getState(server).clearScores();
        TeamScoreSync.syncToAll(server);

        TeamChats.getState(server).clearMessages();
        TeamChatSync.syncAll(server);

        HeadRandomizeManager.resetWeights(server);
        CodenamesRoundState.get(server).resetRounds();
        syncTurnButtons(server);
        GameTimerSync.syncToAll(server);

        server.getPlayerManager()
                .broadcast(Text.literal("Игра сброшена до начального состояния.").formatted(Formatting.GOLD), false);
    }

    public static boolean canPlayerSelect(ServerPlayerEntity player) {
        if (pausedForMissingPlayers) {
            return false;
        }
        CodenamesGameState state = CodenamesGames.getState(player.getServer());
        String team = TeamService.getTeamName(player);
        if (state.getPhase() == CodenamesPhase.STOPPED) {
            return true;
        }
        return state.getPhase() == CodenamesPhase.GUESSING && team != null && team.equals(state.getActiveTeam());
    }

    public static boolean tryAcceptClue(MinecraftServer server, ServerPlayerEntity sender, String rawMessage) {
        if (pausedForMissingPlayers) {
            return false;
        }
        CodenamesGameState state = CodenamesGames.getState(server);
        String team = TeamService.getTeamName(sender);
        if (state.getPhase() != CodenamesPhase.WAITING_CLUE || team == null
                || !samePlayableTeam(team, state.getActiveTeam())) {
            return false;
        }
        if (Roles.getState(server).getRole(sender.getUuid()) != PlayerRole.LIDER) {
            return false;
        }

        ParsedClue clue = parseClue(rawMessage);
        if (clue == null) {
            sender.sendMessage(
                    Text.literal("Подсказка должна быть: одно_слово число. Можно использовать 0 или неограниченно.")
                            .formatted(Formatting.YELLOW),
                    false);
            return true;
        }

        state.setClue(clue.word(), clue.count());
        long guessingDuration = customGuessingTicks;
        state.setPhase(CodenamesPhase.GUESSING, team, server.getOverworld().getTime(), guessingDuration);
        state.unlockGuessingTimer();
        GameTimerSync.syncToAll(server);
        broadcastActionBar(server, Text.literal("Ход отгадывающих команды ")
                .append(Text.literal(teamDisplayPluralClean(team)).formatted(teamFormatting(team)))
                .append(Text.literal(".")));
        TeamChats.getState(server).addMessage(new TeamChatMessage(
                sender.getUuid(),
                sender.getName().getString(),
                team,
                clue.word() + " " + clue.countLabel(),
                Instant.now().toEpochMilli()));
        TeamChatSync.syncAll(server);
        return true;
    }

    public static boolean canLeaderOpenTeamChat(ServerPlayerEntity player, String chatTeamName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        if (Roles.getState(server).getRole(player.getUuid()) != PlayerRole.LIDER) {
            return true;
        }
        if (!samePlayableTeam(TeamService.getTeamName(player), chatTeamName)) {
            return false;
        }
        return isLeaderTeamTurn(player);
    }

    public static boolean isLeaderTeamTurn(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        CodenamesGameState state = CodenamesGames.getState(server);
        String playerTeam = TeamService.getTeamName(player);
        return state.getPhase() == CodenamesPhase.WAITING_CLUE
                && samePlayableTeam(playerTeam, state.getActiveTeam());
    }

    public static void disputeClue(MinecraftServer server, ServerPlayerEntity player) {
        if (pausedForMissingPlayers) {
            return;
        }
        CodenamesGameState state = CodenamesGames.getState(server);
        String team = TeamService.getTeamName(player);
        if (team == null) {
            player.sendMessage(Text.literal("Сначала вступите в команду.").formatted(Formatting.RED), false);
            return;
        }
        if (state.getPhase() != CodenamesPhase.GUESSING || state.getClueWord().isEmpty()) {
            player.sendMessage(
                    Text.literal("Сейчас нет активной подсказки, которую можно оспорить.").formatted(Formatting.YELLOW),
                    false);
            return;
        }

        if (state.getDisputeTeam().isEmpty()) {
            state.startDispute(team, server.getOverworld().getTime() + DISPUTE_TICKS);
            String opposite = nextTeam(server, team);
            server.getPlayerManager()
                    .broadcast(Text.literal("! Подсказка была оспорена командой ").formatted(Formatting.RED)
                            .append(Text.literal(teamDisplayPluralClean(team)).formatted(teamFormatting(team))), false);
            server.getPlayerManager().broadcast(Text.literal("! Участник из команды ").formatted(Formatting.YELLOW)
                    .append(Text.literal(teamDisplayPluralClean(opposite)).formatted(teamFormatting(opposite)))
                    .append(Text.literal(" должен подтвердить оспаривание в течение 30 секунд")
                            .formatted(Formatting.YELLOW)),
                    false);
            playGlobalSound(server, net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), 10.0F, 0.0F);
            return;
        }

        if (state.getDisputeTeam().equals(team)) {
            player.sendMessage(Text.literal("Ваша команда уже оспорила подсказку. Ждём подтверждение другой команды.")
                    .formatted(Formatting.YELLOW), false);
            return;
        }

        rejectCurrentClue(server, team);
    }

    public static void confirmSelection(MinecraftServer server, String teamName, BlockPos pos) {
        if (pausedForMissingPlayers) {
            return;
        }
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() != CodenamesPhase.GUESSING || !teamName.equals(state.getActiveTeam())) {
            BoardSelectionState.clearVotesForTeam(teamName);
            BoardSelectionSync.syncToAll(server);
            return;
        }

        BoardState board = Boards.getState(server);
        BlockPos canonicalPos = board.resolvePosition(pos);
        if (canonicalPos == null) {
            return;
        }
        BoardCellType selected = board.getType(canonicalPos);
        BoardCellType own = expectedTypeForTeam(teamName);
        BoardCellType opponent = own == BoardCellType.RED ? BoardCellType.BLUE : BoardCellType.RED;

        BoardSelectionState.clearVotesForTeam(teamName);
        BoardSelectionState.clearVotesForPos(canonicalPos);
        BoardSelectionSync.syncToAll(server);
        revealCell(server.getOverworld(), board, canonicalPos, selected);
        board.removeCell(canonicalPos);
        state.incrementGuessesThisTurn();
        BoardSync.syncToAll(server);
        GameTimerSync.syncToAll(server);
        syncTurnButtons(server);

        if (selected == BoardCellType.ASSASSIN) {
            playGlobalSound(server, net.minecraft.sound.SoundEvents.BLOCK_BEACON_DEACTIVATE, 10.0F, 1.0F);
            broadcastActionBar(server, Text.literal("Команда ").formatted(Formatting.DARK_RED)
                    .append(Text.literal(teamDisplayPluralClean(teamName)).formatted(teamFormatting(teamName)))
                    .append(Text.literal(" открыла запрещённый объект и проиграла").formatted(Formatting.DARK_RED)));
            finishRound(server, teamName, false);
            return;
        }

        if (selected == own) {
            playGlobalSound(server, net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 10.0F, 2.0F);
            int score = Math.max(0, TeamScores.getState(server).addScore(teamName, -1));
            TeamScoreSync.syncToAll(server);
            if (score <= 0) {
                finishRound(server, teamName, true);
            }
            return;
        }

        if (selected == opponent) {
            playGlobalSound(server, net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 10.0F, 0.0F);
            String opponentTeam = nextTeam(server, teamName);
            int opponentScore = Math.max(0, TeamScores.getState(server).addScore(opponentTeam, -1));
            TeamScoreSync.syncToAll(server);
            if (opponentScore <= 0) {
                finishRound(server, opponentTeam, true);
                return;
            }
            broadcastActionBar(server, Text.literal("Это объект соперников. Ход переходит.").formatted(Formatting.RED));
            startCluePhase(server, opponentTeam);
            return;
        }

        playGlobalSound(server, net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 10.0F, 0.0F);
        broadcastActionBar(server, Text.literal("Нейтральный объект. Ход переходит.").formatted(Formatting.GRAY));
        startCluePhase(server, nextTeam(server, teamName));
    }

    public static Text status(MinecraftServer server) {
        CodenamesGameState state = CodenamesGames.getState(server);
        String clue = state.getClueWord().isEmpty() ? "-" : state.getClueWord() + " " + clueLabel(state.getClueCount());
        String dispute = state.getDisputeTeam().isEmpty() ? "" : " | спор: " + state.getDisputeTeam();
        return Text.literal("Игра: " + state.getPhase().getId()
                + " | ход: " + emptyDash(state.getActiveTeam())
                + " | подсказка: " + clue
                + dispute);
    }

    public static Text validateNextTurn(MinecraftServer server) {
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED) {
            return Text.literal("Игра сейчас не идет.");
        }
        if (state.getActiveTeam().isEmpty()) {
            return Text.literal("Сейчас нет активной команды.");
        }
        if (state.getPhase() != CodenamesPhase.GUESSING) {
            return Text.literal("Передать ход можно только во время выбора объектов.");
        }
        if (state.getGuessesThisTurn() <= 0) {
            return Text.literal("Передать ход можно только после хотя бы одного выбранного объекта.");
        }
        if (playableTeams(server).size() < 2) {
            return Text.literal("Нужно минимум две игровые команды, чтобы передавать ход.");
        }
        return null;
    }

    public static boolean nextTurn(MinecraftServer server) {
        if (pausedForMissingPlayers) {
            return false;
        }
        if (validateNextTurn(server) != null) {
            return false;
        }

        CodenamesGameState state = CodenamesGames.getState(server);
        startCluePhase(server, nextTeam(server, state.getActiveTeam()));
        return true;
    }

    public static boolean tryPassTurn(MinecraftServer server, ServerPlayerEntity player) {
        if (pausedForMissingPlayers) {
            return false;
        }
        CodenamesGameState state = CodenamesGames.getState(server);
        Text error = validateNextTurn(server);
        if (error != null) {
            player.sendMessage(error.copy().formatted(Formatting.YELLOW), true);
            return false;
        }

        String team = TeamService.getTeamName(player);
        if (team == null || !team.equals(state.getActiveTeam())) {
            player.sendMessage(Text.literal("Передать ход может только активная команда.").formatted(Formatting.YELLOW),
                    true);
            return false;
        }
        if (Roles.getState(server).getRole(player.getUuid()) != PlayerRole.GUESSING) {
            player.sendMessage(Text.literal("Передать ход могут только отгадывающие.").formatted(Formatting.YELLOW),
                    true);
            return false;
        }

        startCluePhase(server, nextTeam(server, state.getActiveTeam()));
        return true;
    }

    private static void tick(MinecraftServer server) {
        CodenamesGameState state = CodenamesGames.getState(server);
        long now = server.getOverworld().getTime();
        if (now % 40L == 0L) {
            maintainWorldAndPlayers(server);
            giveRuleBooks(server);
        }
        if (pendingLobbyTeleportTick > 0L && now >= pendingLobbyTeleportTick) {
            pendingLobbyTeleportTick = -1L;
            TeleportPointService.teleportAllToLobby(server);
            giveRuleBooks(server);
        }
        if (handleMissingPlayersPause(server, state, now)) {
            if (now % 5L == 0L) {
                GameTimerSync.syncToAll(server);
            }
            return;
        }
        if (now % 5L == 0L) {
            GameTimerSync.syncToAll(server);
        }
        syncVanillaTimerBar(server, state, now);

        if (!state.getDisputeTeam().isEmpty() && state.getDisputeEndTick() > 0 && now >= state.getDisputeEndTick()) {
            broadcastActionBar(server, Text.literal("Оспаривание подсказки истекло. Подсказка считается принятой.")
                    .formatted(Formatting.YELLOW));
            state.clearDispute();
        }

        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED
                || state.getPhaseEndTick() <= 0) {
            return;
        }
        if (now < state.getPhaseEndTick()) {
            return;
        }

        if (state.getPhase() == CodenamesPhase.WAITING_CLUE) {
            String nextTeam = nextTeam(server, state.getActiveTeam());
            broadcastActionBar(server, Text.literal(
                    "Время вышло. Ход переходит команде ")
                    .formatted(Formatting.YELLOW)
                    .append(Text.literal(teamDisplayPluralClean(nextTeam)).formatted(teamFormatting(nextTeam)))
                    .append(Text.literal(".").formatted(Formatting.YELLOW)));
            startCluePhase(server, nextTeam);
        } else if (state.getPhase() == CodenamesPhase.GUESSING) {
            if (BoardSelectionState.getConfirmation(state.getActiveTeam()) != null) {
                return;
            }
            String nextTeam = nextTeam(server, state.getActiveTeam());
            broadcastActionBar(server, Text.literal(
                    "Время вышло. Ход переходит команде ")
                    .formatted(Formatting.YELLOW)
                    .append(Text.literal(teamDisplayPluralClean(nextTeam)).formatted(teamFormatting(nextTeam)))
                    .append(Text.literal(".").formatted(Formatting.YELLOW)));
            startCluePhase(server, nextTeam);
        }
    }

    private static boolean handleMissingPlayersPause(MinecraftServer server, CodenamesGameState state, long now) {
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED
                || state.getPhaseEndTick() <= 0) {
            clearMissingPlayersPause();
            return false;
        }

        boolean enoughPlayers = RoleValidation.canStart(server);
        if (!enoughPlayers) {
            if (!pausedForMissingPlayers) {
                pausedForMissingPlayers = true;
                pausedRemainingTicks = Math.max(1L, state.getPhaseEndTick() - now);
                resumeAfterPlayersReadyTick = -1L;
                GameTimerSync.syncToAll(server);
                broadcastActionBar(server, Text.literal(
                        "Игра поставлена на паузу: не хватает игроков.")
                        .formatted(Formatting.YELLOW));
            }
            state.resetPhaseTimer(now, pausedRemainingTicks);
            return true;
        }

        if (!pausedForMissingPlayers) {
            return false;
        }

        if (resumeAfterPlayersReadyTick < 0L) {
            resumeAfterPlayersReadyTick = now + PLAYER_RESUME_DELAY_TICKS;
            broadcastActionBar(server, Text.literal(
                    "Игроки вернулись. Игра продолжится через 5 секунд.")
                    .formatted(Formatting.GREEN));
        }

        if (now < resumeAfterPlayersReadyTick) {
            state.resetPhaseTimer(now, pausedRemainingTicks);
            return true;
        }

        state.resetPhaseTimer(now, pausedRemainingTicks);
        clearMissingPlayersPause();
        GameTimerSync.syncToAll(server);
        broadcastActionBar(server,
                Text.literal("Игра продолжена.")
                        .formatted(Formatting.GREEN));
        return false;
    }

    public static boolean isPausedForMissingPlayers() {
        return pausedForMissingPlayers;
    }

    public static boolean isLobbyReady(MinecraftServer server) {
        return CodenamesGames.getState(server).getPhase() == CodenamesPhase.STOPPED && pendingLobbyTeleportTick < 0L;
    }

    private static void clearMissingPlayersPause() {
        pausedForMissingPlayers = false;
        pausedRemainingTicks = 0L;
        resumeAfterPlayersReadyTick = -1L;
    }

    private static void rejectCurrentClue(MinecraftServer server, String confirmingTeam) {
        CodenamesGameState state = CodenamesGames.getState(server);
        String activeTeam = state.getActiveTeam();
        String content = state.getClueWord() + " " + clueLabel(state.getClueCount());
        TeamChats.getState(server).removeLatestMessage(activeTeam, content);
        TeamChatSync.syncAll(server);
        broadcastActionBar(server, Text.literal("! Оспаривание подтверждено командой ")
                .append(Text.literal(teamDisplaySingularInstrumental(confirmingTeam))
                        .formatted(teamFormatting(confirmingTeam)))
                .append(Text.literal(". Подсказка удалена, ход переходит.").formatted(Formatting.RED)));
        startCluePhase(server, nextTeam(server, activeTeam));
    }

    private static void startCluePhase(MinecraftServer server, String teamName) {
        CodenamesGameState state = CodenamesGames.getState(server);
        state.clearClue();
        long clueDuration = state.isGuessingTimerUnlocked() ? customClueTicks : 0L;
        state.setPhase(CodenamesPhase.WAITING_CLUE, teamName, server.getOverworld().getTime(), clueDuration);
        state.clearGuessesThisTurn();
        BoardSelectionState.clearAll();
        syncTurnButtons(server);
        GameTimerSync.syncToAll(server);
        BoardSelectionSync.syncToAll(server);
        broadcastActionBar(server, Text.literal("Ход команды: ")
                .append(Text.literal(teamDisplayPluralClean(teamName)).formatted(teamFormatting(teamName))));
    }

    public static void broadcastActionBar(MinecraftServer server, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, true);
        }
    }

    public static void sendTeamActionBar(MinecraftServer server, String teamName, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (samePlayableTeam(TeamService.getTeamName(player), teamName)
                    && Roles.getState(server).getRole(player.getUuid()) == PlayerRole.GUESSING) {
                player.sendMessage(message, true);
            }
        }
    }

    private static ParsedClue parseClue(String rawMessage) {
        String[] parts = rawMessage.trim().split("\\s+");
        if (parts.length != 2) {
            return null;
        }

        String word = parts[0].endsWith(":") ? parts[0].substring(0, parts[0].length() - 1) : parts[0];
        if (word.isBlank() || word.contains(":") || word.contains(",")) {
            return null;
        }

        String rawCount = parts[1].toLowerCase(Locale.ROOT);
        if (rawCount.equals("неограниченно")
                || rawCount.equals("безлимит")
                || rawCount.equals("inf")) {
            return new ParsedClue(word, UNLIMITED_CLUE);
        }

        try {
            int count = Integer.parseInt(rawCount);
            if (count < 0 || count > 8) {
                return null;
            }
            return new ParsedClue(word, count);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void revealCell(ServerWorld world, BoardState board, BlockPos pos, BoardCellType type) {
        for (BlockPos linkedPos : board.getLinkedPositions(pos)) {
            revealSingleCell(world, linkedPos, type);
        }
    }

    private static void revealSingleCell(ServerWorld world, BlockPos pos, BoardCellType type) {
        switch (type) {
            case RED -> world.setBlockState(pos, Blocks.RED_CONCRETE.getDefaultState());
            case BLUE -> world.setBlockState(pos, Blocks.BLUE_CONCRETE.getDefaultState());
            case NEUTRAL -> world.setBlockState(pos, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
            case ASSASSIN -> world.setBlockState(pos, Blocks.BLACK_CONCRETE.getDefaultState());
            default -> {
            }
        }
    }

    private static void finishRound(MinecraftServer server, String teamName, boolean win) {
        if (win) {
            GameTitleService.showWin(server, teamName);
            playGlobalSound(server, net.minecraft.sound.SoundEvent
                    .of(new net.minecraft.util.Identifier("minecraft", "item.goat_horn.sound.0")), 10.0F, 1.0F);
        } else {
            GameTitleService.showLose(server, teamName);
        }

        restoreRoundBlocks(server);
        Boards.getState(server).repairCellsFromFields();
        Roles.clearAll(server);
        CodenamesGames.getState(server).stop();
        clearMissingPlayersPause();
        resetTimers();
        BoardSelectionState.clearAll();
        BoardSelectionSync.syncToAll(server);
        clearOsporitItems(server);
        scheduleLobbyTeleport(server);
        giveRuleBooks(server);
        clearTeamChat(server);
        CodenamesRoundState.get(server).advanceRound();
        syncTurnButtons(server);
        GameTimerSync.syncToAll(server);
    }

    private static void giveOsporitItems(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            clearOsporitItems(player);

            if (Roles.getState(server).getRole(player.getUuid()) != PlayerRole.GUESSING)
                continue;

            ItemStack stack = new ItemStack(ModItems.OSPORIT.getItem());
            player.getInventory().setStack(8, stack);
            syncInventory(player);
        }
    }

    private static void giveRuleBooks(MinecraftServer server) {
        boolean lobbyReady = isLobbyReady(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            if (player.interactionManager.getGameMode() != GameMode.ADVENTURE) {
                clearRuleBook(player, 7);
                clearRuleBook(player, 8);
                continue;
            }

            ensureRuleBook(player, 7, "short", RuleBookStacks.shortRules());

            if (lobbyReady) {
                ensureRuleBook(player, 8, "full", RuleBookStacks.fullRules());
            } else {
                clearRuleBook(player, 8);
            }
        }
    }

    private static void ensureRuleBook(ServerPlayerEntity player, int slot, String type, ItemStack book) {
        ItemStack current = player.getInventory().getStack(slot);
        if (RuleBookStacks.isRuleBook(current, type)) {
            return;
        }
        if (RuleBookStacks.isAnyRuleBook(current) || current.isEmpty()) {
            player.getInventory().setStack(slot, book);
            syncInventory(player);
        }
    }

    private static void clearRuleBook(ServerPlayerEntity player, int slot) {
        ItemStack current = player.getInventory().getStack(slot);
        if (RuleBookStacks.isAnyRuleBook(current)) {
            player.getInventory().setStack(slot, ItemStack.EMPTY);
            syncInventory(player);
        }
    }

    private static void clearOsporitItems(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            clearOsporitItems(player);
        }
    }

    private static void clearOsporitItems(ServerPlayerEntity player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(ModItems.OSPORIT.getItem())) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            syncInventory(player);
        }
    }

    private static void syncInventory(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.sendContentUpdates();
    }

    private static void playGlobalSound(MinecraftServer server, net.minecraft.sound.SoundEvent sound, float volume,
            float pitch) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.playSound(sound, net.minecraft.sound.SoundCategory.PLAYERS, volume, pitch);
        }
    }

    private static void syncVanillaTimerBar(MinecraftServer server, CodenamesGameState state, long now) {
        if (state.getPhase() == CodenamesPhase.STOPPED) {
            return;
        }

        if (now % 5L != 0L) {
            return;
        }

        float totalTicks = switch (state.getPhase()) {
            case WAITING_CLUE -> customClueTicks;
            case GUESSING -> customGuessingTicks;
            default -> 0L;
        };

        float progress = 0.0F;

        if (totalTicks > 0L && state.getPhaseEndTick() > now) {
            progress = Math.max(0.0F, Math.min(1.0F, (float) (state.getPhaseEndTick() - now) / totalTicks));
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            boolean visible = false;
            String teamName = TeamService.getTeamName(player);

            if (teamName != null
                    && samePlayableTeam(teamName, state.getActiveTeam())) {

                PlayerRole role = Roles.getState(server)
                        .getRole(player.getUuid());

                if (state.getPhase() == CodenamesPhase.WAITING_CLUE) {
                    visible = role == PlayerRole.LIDER;
                }

                if (state.getPhase() == CodenamesPhase.GUESSING) {
                    visible = role == PlayerRole.GUESSING;
                }
            }

            if (!visible) {
                player.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(0.0F, 0, 0));
                continue;
            }

            player.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(progress, 0, 0));
        }
    }

    private static void clearTeamChat(MinecraftServer server) {
        TeamChats.getState(server).clearMessages();
        TeamChatSync.syncAll(server);
    }

    private static void prepareWorldAndPlayers(MinecraftServer server) {
        maintainWorldAndPlayers(server);
    }

    private static void maintainWorldAndPlayers(MinecraftServer server) {
        server.setDifficulty(Difficulty.PEACEFUL, true);
        for (Team team : server.getScoreboard().getTeams()) {
            if (expectedTypeForTeam(team.getName()) != BoardCellType.UNASSIGNED) {
                team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
            }
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            LobbySpawnService.ensureAdventureIfSurvival(player);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 5, 10, true, false, false));
        }
    }

    private static void resetBoardCells(ServerWorld world, BoardState board) {
        for (BlockPos pos : board.getCells().keySet()) {
            for (BlockPos linkedPos : board.getLinkedPositions(pos)) {
                BlockState state = ((linkedPos.getX() + linkedPos.getY() + linkedPos.getZ()) & 1) == 0
                        ? Blocks.WHITE_CONCRETE.getDefaultState()
                        : Blocks.WHITE_CONCRETE.getDefaultState();
                world.setBlockState(linkedPos, state);
            }
        }
    }

    private static void scheduleLobbyTeleport(MinecraftServer server) {
        pendingLobbyTeleportTick = server.getOverworld().getTime() + LOBBY_TELEPORT_DELAY_TICKS;
    }

    private static void restoreRoundBlocks(MinecraftServer server) {
        BoardState board = Boards.getState(server);
        if (CodenamesGameSnapshot.restore(server.getOverworld(), board)) {
            BoardSync.syncToAll(server);
        }
    }

    private static String nextTeam(MinecraftServer server, String currentTeam) {
        List<String> teams = playableTeams(server);
        if (teams.isEmpty()) {
            return "";
        }
        int index = teams.indexOf(currentTeam);
        return teams.get((index + 1 + teams.size()) % teams.size());
    }

    private static List<String> playableTeams(MinecraftServer server) {
        List<String> teams = new ArrayList<>();
        for (Team team : server.getScoreboard().getTeams()) {
            String name = team.getName();
            if (expectedTypeForTeam(name) != BoardCellType.UNASSIGNED) {
                teams.add(name);
            }
        }
        teams.sort(Comparator.comparingInt(CodenamesGameService::teamOrder).thenComparing(value -> value));
        return teams;
    }

    private static int teamOrder(String teamName) {
        BoardCellType type = expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return 0;
        }
        if (type == BoardCellType.BLUE) {
            return 1;
        }
        return 2;
    }

    public static BoardCellType expectedTypeForTeam(String teamName) {
        if (teamName == null) {
            return BoardCellType.UNASSIGNED;
        }
        String normalized = teamName.toLowerCase(Locale.ROOT);
        if (normalized.contains("red") || normalized.contains("крас")) {
            return BoardCellType.RED;
        }
        if (normalized.contains("blue") || normalized.contains("син")) {
            return BoardCellType.BLUE;
        }
        return BoardCellType.UNASSIGNED;
    }

    private static boolean samePlayableTeam(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.equals(second)) {
            return true;
        }

        BoardCellType firstType = expectedTypeForTeam(first);
        return firstType != BoardCellType.UNASSIGNED && firstType == expectedTypeForTeam(second);
    }

    private static void syncTurnButtons(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        CodenamesGameState state = CodenamesGames.getState(server);
        boolean canPass = state.getPhase() == CodenamesPhase.GUESSING && state.getGuessesThisTurn() > 0;
        BoardCellType activeType = expectedTypeForTeam(state.getActiveTeam());

        updateTurnButtons(world, BLUE_TURN_BUTTONS, canPass && activeType == BoardCellType.BLUE, Direction.WEST);
        updateTurnButtons(world, RED_TURN_BUTTONS, canPass && activeType == BoardCellType.RED, Direction.EAST);
    }

    private static void updateTurnButtons(ServerWorld world, List<BlockPos> positions, boolean visible, Direction facing) {
        for (BlockPos pos : positions) {
            if (visible) {
                BlockState buttonState = createTurnButtonState(facing);
                world.setBlockState(pos, buttonState);
                if (world.getBlockEntity(pos) instanceof ClickButtonBlockEntity entity) {
                    entity.setMode(ClickButtonBlockEntity.Mode.PASS_TURN);
                }

                if (!PASS_HOLOGRAMS.containsKey(pos)) {
                    net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(pos).expand(0.5);
                    List<PassTurnHologramEntity> existingList = world.getEntitiesByClass(PassTurnHologramEntity.class, box, e -> true);

                    if (!existingList.isEmpty()) {
                        PassTurnHologramEntity existing = existingList.get(0);
                        existing.setFixedDirection(facing.getOpposite());
                        PASS_HOLOGRAMS.put(pos, existing);
                    } else {
                        PassTurnHologramEntity hologram = new PassTurnHologramEntity(world, pos);
                        hologram.setFixedDirection(facing.getOpposite());
                        world.spawnEntity(hologram);
                        PASS_HOLOGRAMS.put(pos, hologram);
                    }
                } else {
                    PassTurnHologramEntity existing = PASS_HOLOGRAMS.get(pos);
                    if (existing.isRemoved()) {
                        PassTurnHologramEntity hologram = new PassTurnHologramEntity(world, pos);
                        hologram.setFixedDirection(facing.getOpposite());
                        world.spawnEntity(hologram);
                        PASS_HOLOGRAMS.put(pos, hologram);
                    }
                }
            } else if (!world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());

                PassTurnHologramEntity hologram = PASS_HOLOGRAMS.remove(pos);
                if (hologram != null && !hologram.isRemoved()) {
                    hologram.setVisible(false);
                    hologram.discard();
                }
            }
        }
    }

    private static BlockState createTurnButtonState(Direction facing) {
        return ModBlocks.CLICK_BUTTON.getBlock().getDefaultState()
                .with(ClickButtonBlock.FACING, facing)
                .with(ClickButtonBlock.POWERED, false);
    }

    private static String clueLabel(int count) {
        return count == UNLIMITED_CLUE
                ? "неограниченно"
                : Integer.toString(count);
    }

    private static String emptyDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private static String teamDisplayName(String teamName) {
        BoardCellType type = expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return "красная команда";
        }
        if (type == BoardCellType.BLUE) {
            return "синяя команда";
        }
        return teamName == null ? "" : teamName;
    }

    private static String teamDisplayPlural(String teamName) {
        BoardCellType type = expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return "Красных";
        }
        if (type == BoardCellType.BLUE) {
            return "Синих";
        }
        return teamName == null ? "" : teamName;
    }

    private static String teamDisplaySingularInstrumental(String teamName) {
        BoardCellType type = expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return "Красной";
        }
        if (type == BoardCellType.BLUE) {
            return "Синей";
        }
        return teamName == null ? "" : teamName;
    }

    private static String teamDisplayPluralClean(String teamName) {
        BoardCellType type = expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return "Красные";
        }
        if (type == BoardCellType.BLUE) {
            return "Синие";
        }
        return teamName == null ? "" : teamName;
    }

    private static Formatting teamFormatting(String teamName) {
        BoardCellType type = expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return Formatting.RED;
        }
        if (type == BoardCellType.BLUE) {
            return Formatting.BLUE;
        }
        return Formatting.WHITE;
    }

    // ========== Методы управления таймером ==========

    public static void setClueTimer(int seconds) {
        customClueTicks = Math.max(20L, seconds * 20L);
    }

    public static void setGuessingTimer(int seconds) {
        customGuessingTicks = Math.max(20L, seconds * 20L);
    }

    public static long getClueTimerTicks() {
        return customClueTicks;
    }

    public static long getGuessingTimerTicks() {
        return customGuessingTicks;
    }

    public static void resetTimers() {
        customClueTicks = CLUE_TICKS;
        customGuessingTicks = GUESSING_TICKS;
        timerPaused = false;
        timerPausedRemaining = 0L;
    }

    public static boolean pauseTimer(MinecraftServer server) {
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED) {
            return false;
        }
        
        if (!timerPaused) {
            long now = server.getOverworld().getTime();
            timerPausedRemaining = Math.max(0L, state.getPhaseEndTick() - now);
            timerPaused = true;
            
            state.setPhase(state.getPhase(), state.getActiveTeam(), now, Long.MAX_VALUE - now);
            GameTimerSync.syncToAll(server);
            
            broadcastActionBar(server, Text.literal("⏸ Таймер поставлен на паузу.").formatted(Formatting.YELLOW));
            return true;
        }
        return false;
    }

    public static boolean resumeTimer(MinecraftServer server) {
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED) {
            return false;
        }
        
        if (timerPaused) {
            long now = server.getOverworld().getTime();
            state.setPhase(state.getPhase(), state.getActiveTeam(), now, timerPausedRemaining);
            timerPaused = false;
            timerPausedRemaining = 0L;
            
            GameTimerSync.syncToAll(server);
            broadcastActionBar(server, Text.literal("▶ Таймер возобновлён.").formatted(Formatting.GREEN));
            return true;
        }
        return false;
    }

    public static boolean addTime(MinecraftServer server, int seconds) {
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED) {
            return false;
        }
        
        long now = server.getOverworld().getTime();
        long remaining = Math.max(0L, state.getPhaseEndTick() - now);
        long newRemaining = remaining + (seconds * 20L);
        
        state.setPhase(state.getPhase(), state.getActiveTeam(), now, newRemaining);
        GameTimerSync.syncToAll(server);
        
        broadcastActionBar(server, Text.literal("⏱ Добавлено " + seconds + " сек. к таймеру.").formatted(Formatting.GREEN));
        return true;
    }

    public static boolean removeTime(MinecraftServer server, int seconds) {
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED) {
            return false;
        }
        
        long now = server.getOverworld().getTime();
        long remaining = Math.max(0L, state.getPhaseEndTick() - now);
        long newRemaining = Math.max(20L, remaining - (seconds * 20L));
        
        state.setPhase(state.getPhase(), state.getActiveTeam(), now, newRemaining);
        GameTimerSync.syncToAll(server);
        
        broadcastActionBar(server, Text.literal("⏱ Убрано " + seconds + " сек. с таймера.").formatted(Formatting.RED));
        return true;
    }

    public static Text getTimerStatus(MinecraftServer server) {
        CodenamesGameState state = CodenamesGames.getState(server);
        if (state.getPhase() == CodenamesPhase.STOPPED || state.getPhase() == CodenamesPhase.FINISHED) {
            return Text.literal("Игра не активна.");
        }
        
        long now = server.getOverworld().getTime();
        long remaining = Math.max(0L, state.getPhaseEndTick() - now);
        long seconds = remaining / 20L;
        
        String pauseStatus = timerPaused ? " (пауза)" : "";
        
        return Text.literal("Фаза: " + state.getPhase().getId() 
            + " | Осталось: " + seconds + " сек." + pauseStatus
            + " | Clue: " + (customClueTicks / 20L) + "с"
            + " | Guess: " + (customGuessingTicks / 20L) + "с");
    }

    public static boolean isTimerPaused() {
        return timerPaused;
    }

    private record ParsedClue(String word, int count) {
        String countLabel() {
            return clueLabel(this.count);
        }
    }
}