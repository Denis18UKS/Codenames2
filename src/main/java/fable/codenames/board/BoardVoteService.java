package fable.codenames.board;

import fable.codenames.block.CodenamesHeadBlock;
import net.minecraft.block.AbstractSkullBlock;
import fable.codenames.dev.SoloModeService;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public final class BoardVoteService {
    private static final int CONFIRM_TICKS = 100;
    private static final int OUTLINE_STEPS = 42;

    private BoardVoteService() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BoardVoteService::tick);
    }

    public static void updateConfirmation(MinecraftServer server, String teamName) {
        BlockPos unanimousPos = findUnanimousVote(server, teamName);
        BoardSelectionState.Confirmation confirmation = BoardSelectionState.getConfirmation(teamName);
        long tick = server.getOverworld().getTime();

        if (unanimousPos == null) {
            if (confirmation != null) {
                BoardSelectionState.clearConfirmation(teamName);
                BoardSelectionSync.syncToAll(server);
            }
            return;
        }

        if (confirmation == null || !confirmation.pos().equals(unanimousPos)) {
            BoardSelectionState.startConfirmation(teamName, unanimousPos, tick);
            BoardSelectionSync.syncToAll(server);
        }
    }

    private static void tick(MinecraftServer server) {
        for (String teamName : BoardSelectionState.getConfirmingTeams()) {
            BoardSelectionState.Confirmation confirmation = BoardSelectionState.getConfirmation(teamName);
            if (confirmation == null) {
                continue;
            }

            BlockPos unanimousPos = findUnanimousVote(server, teamName);
            if (!confirmation.pos().equals(unanimousPos)) {
                BoardSelectionState.clearConfirmation(teamName);
                BoardSelectionSync.syncToAll(server);
                continue;
            }

            long elapsed = server.getOverworld().getTime() - confirmation.startTick();
            for (BlockPos linkedPos : Boards.getState(server).getLinkedPositions(confirmation.pos())) {
                spawnProgressParticles(server.getOverworld(), teamName, linkedPos, elapsed);
            }
            if (elapsed >= CONFIRM_TICKS) {
                BoardSelectionState.clearConfirmation(teamName);
                BoardSelectionSync.syncToAll(server);
                CodenamesGameService.sendTeamActionBar(server, teamName, Text.literal("Выбор подтверждён."));
                CodenamesGameService.confirmSelection(server, teamName, confirmation.pos());
            }
        }
    }

    private static BlockPos findUnanimousVote(MinecraftServer server, String teamName) {
        int required = requiredGuessers(server, teamName);
        if (required <= 0) {
            return null;
        }

        Map<BlockPos, Integer> counts = new HashMap<>();
        for (BoardSelectionState.VoteIndicator indicator : BoardSelectionState.getIndicators()) {
            if (teamName.equals(indicator.teamName())) {
                counts.put(indicator.pos(), indicator.count());
            }
        }

        for (Map.Entry<BlockPos, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= required) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static int requiredGuessers(MinecraftServer server, String teamName) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (SoloModeService.isEnabled(server, player.getUuid()) && teamName.equals(TeamService.getTeamName(player))) {
                return 1;
            }
        }

        Team team = server.getScoreboard().getTeam(teamName);
        if (team == null) {
            return 0;
        }

        int guessers = 0;
        for (String entry : team.getPlayerList()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry);
            if (player != null && Roles.getState(server).getRole(player.getUuid()) == PlayerRole.GUESSING) {
                guessers++;
            }
        }
        return guessers;
    }

    private static void spawnProgressParticles(ServerWorld world, String teamName, BlockPos pos, long elapsed) {
        int currentStep = Math.min(OUTLINE_STEPS - 1, (int) Math.floor((elapsed + 1) / (double) CONFIRM_TICKS * OUTLINE_STEPS));
        DustParticleEffect particle = new DustParticleEffect(teamVector(teamName), 0.7F);

        java.util.List<BlockPos> field = fieldFor(world.getServer(), pos);
        boolean sameZ = field.stream().mapToInt(BlockPos::getZ).distinct().count() == 1;
        boolean sameX = field.stream().mapToInt(BlockPos::getX).distinct().count() == 1;
        Direction frontSide = detectFrontSide(world, field);

        // ВНЕШНИЙ контур блока
        double minA = sameX ? pos.getZ() + 0.08 : pos.getX() + 0.08;
        double maxA = sameX ? pos.getZ() + 0.92 : pos.getX() + 0.92;
        double minY = pos.getY() + 0.08;
        double maxY = pos.getY() + 0.92;

        for (int i = 0; i <= currentStep; i++) {
            double t = i / (double) OUTLINE_STEPS;
            double visualX;
            double y;

            // старт с верхнего центра ("12 часов"), дальше по часовой
            if (t < 0.125) {
                visualX = 0.5 + (t / 0.125) * 0.5;
                y = maxY;
            } else if (t < 0.375) {
                visualX = 1.0;
                y = maxY - (maxY - minY) * ((t - 0.125) / 0.25);
            } else if (t < 0.625) {
                visualX = 1.0 - ((t - 0.375) / 0.25);
                y = minY;
            } else if (t < 0.875) {
                visualX = 0.0;
                y = minY + (maxY - minY) * ((t - 0.625) / 0.25);
            } else {
                visualX = ((t - 0.875) / 0.125) * 0.5;
                y = maxY;
            }

            if (sameX) {
                double z = coordinateFromVisualX(minA, maxA, visualX, frontSide.rotateYCounterclockwise() == Direction.SOUTH);
                double x = frontSide == Direction.EAST ? pos.getX() + 1.04 : pos.getX() - 0.04;
                spawnOldParticle(world, particle, x, y, z, true);
            } else if (sameZ) {
                double x = coordinateFromVisualX(minA, maxA, visualX, frontSide.rotateYCounterclockwise() == Direction.EAST);
                double z = frontSide == Direction.SOUTH ? pos.getZ() + 1.04 : pos.getZ() - 0.04;
                spawnOldParticle(world, particle, x, y, z, false);
            } else {
                spawnOldParticle(world, particle, pos.getX() + 0.5, y, pos.getZ() + 0.5, false);
            }
        }
    }

    private static void spawnOldParticle(ServerWorld world, DustParticleEffect particle, double x, double y, double z, boolean xFixed) {
        double spread = 0.008;
        world.spawnParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        if (xFixed) {
            world.spawnParticles(particle, x, y, z + spread, 1, 0.0, 0.0, 0.0, 0.0);
        } else {
            world.spawnParticles(particle, x + spread, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static double coordinateFromVisualX(double min, double max, double visualX, boolean worldCoordinateGrowsRight) {
        return worldCoordinateGrowsRight
                ? min + (max - min) * visualX
                : max - (max - min) * visualX;
    }

    private static java.util.List<BlockPos> fieldFor(MinecraftServer server, BlockPos pos) {
        for (java.util.List<BlockPos> field : Boards.getState(server).getFields()) {
            if (field.contains(pos)) {
                return field;
            }
        }
        return java.util.List.of(pos);
    }

    private static Direction detectFrontSide(ServerWorld world, java.util.List<BlockPos> field) {
        Direction[] candidates = sideCandidates(field);
        int first = countHeads(world, field, candidates[0]);
        int second = countHeads(world, field, candidates[1]);
        return first >= second ? candidates[0] : candidates[1];
    }

    private static Direction[] sideCandidates(java.util.List<BlockPos> field) {
        BlockPos first = field.get(0);
        boolean sameX = field.stream().allMatch(pos -> pos.getX() == first.getX());
        boolean sameZ = field.stream().allMatch(pos -> pos.getZ() == first.getZ());
        if (sameX) {
            return new Direction[]{Direction.EAST, Direction.WEST};
        }
        if (sameZ) {
            return new Direction[]{Direction.SOUTH, Direction.NORTH};
        }
        return new Direction[]{Direction.SOUTH, Direction.NORTH};
    }

    private static int countHeads(ServerWorld world, java.util.List<BlockPos> field, Direction side) {
        int count = 0;
        for (BlockPos boardPos : field) {
            if (isHeadMarker(world.getBlockState(boardPos.offset(side)))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isHeadMarker(net.minecraft.block.BlockState state) {
        return state.getBlock() instanceof CodenamesHeadBlock || state.getBlock() instanceof AbstractSkullBlock;
    }

    private static Vector3f teamVector(String teamName) {
        String normalized = teamName.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("blue") || normalized.contains("син")) {
            return new Vector3f(0.2F, 0.55F, 1.0F);
        }
        return new Vector3f(1.0F, 0.15F, 0.15F);
    }
}