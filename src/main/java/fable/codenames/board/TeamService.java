package fable.codenames.board;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TeamService {
    private TeamService() {
    }

    public static String getTeamName(ServerPlayerEntity player) {
        AbstractTeam team = player.getScoreboardTeam();
        return team != null ? team.getName() : null;
    }
}
