package fable.codenames.role;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class RoleDisplay {
    private RoleDisplay() {
    }

    public static void refreshPlayer(ServerPlayerEntity player, PlayerRole role) {
        if (role == PlayerRole.LIDER) {
            Text prefixedName = buildLeaderName(player);
            player.setCustomName(prefixedName);
            player.setCustomNameVisible(true);
            return;
        }

        player.setCustomName(null);
        player.setCustomNameVisible(false);
    }

    public static void refreshAll(MinecraftServer server) {
        RoleState state = Roles.getState(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            refreshPlayer(player, state.getRole(player.getUuid()));
        }
    }

    private static Text buildLeaderName(ServerPlayerEntity player) {
        MutableText name = Text.literal("Лидер ").formatted(Formatting.GOLD);
        AbstractTeam team = player.getScoreboardTeam();
        Text playerName = player.getName().copy();
        if (team != null) {
            playerName = team.decorateName(playerName);
        }
        return name.append(playerName);
    }
}
