package fable.codenames.teleport;

import fable.codenames.board.BoardCellType;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.game.CodenamesRoundState;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class TeleportPointService {
    public static final String LOBBY = "lobby";
    public static final BlockPos DEFAULT_LOBBY_POS = new BlockPos(115, 139, -411);

    private TeleportPointService() {
    }

    public static void teleportPlayersToRoundRoom(MinecraftServer server) {
        TeleportPointState state = TeleportPointState.get(server);
        int room = CodenamesRoundState.get(server).nextRoomNumber();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            AbstractTeam team = player.getScoreboardTeam();
            PlayerRole role = Roles.getState(server).getRole(player.getUuid());
            if (team == null || role == null) {
                continue;
            }

            BlockPos pos = state.get(keyFor(room, team.getName(), role));
            if (pos == null) {
                continue;
            }

            teleport(player, pos);
        }
    }

    public static void teleportAllToLobby(MinecraftServer server) {
        BlockPos pos = TeleportPointState.get(server).get(LOBBY);
        if (pos == null) {
            pos = DEFAULT_LOBBY_POS;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            teleport(player, pos);
        }
    }

    public static void teleportToLobby(ServerPlayerEntity player) {
        BlockPos pos = TeleportPointState.get(player.getServer()).get(LOBBY);
        if (pos == null) {
            pos = DEFAULT_LOBBY_POS;
        }
        teleport(player, pos);
    }

    public static String keyFor(int room, String teamName, PlayerRole role) {
        if (role == PlayerRole.LIDER) {
            return "room" + room + "|leaders";
        }

        BoardCellType type = CodenamesGameService.expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            return "room" + room + "|red_guessing";
        }
        if (type == BoardCellType.BLUE) {
            return "room" + room + "|blue_guessing";
        }
        return "room" + room + "|unknown";
    }

    private static void teleport(ServerPlayerEntity player, BlockPos pos) {
        player.teleport(player.getServerWorld(), pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
    }
}
