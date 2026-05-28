package fable.codenames.game;

import fable.codenames.teleport.TeleportPointService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public final class LobbySpawnService {
    private LobbySpawnService() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(LobbySpawnService::setWorldSpawn);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            setWorldSpawn(server);
            server.execute(() -> {
                ensureAdventureIfSurvival(handler.player);
                if (CodenamesGameService.isLobbyReady(server)) {
                    TeleportPointService.teleportToLobby(handler.player);
                }
            });
        });
    }

    public static void ensureAdventureIfSurvival(ServerPlayerEntity player) {
        if (player.interactionManager.getGameMode() == GameMode.SURVIVAL) {
            player.changeGameMode(GameMode.ADVENTURE);
        }
    }

    private static void setWorldSpawn(MinecraftServer server) {
        server.getOverworld().setSpawnPos(TeleportPointService.DEFAULT_LOBBY_POS, 0.0F);
    }
}
