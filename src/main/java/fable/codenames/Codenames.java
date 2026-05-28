package fable.codenames;

import fable.codenames.board.BoardSync;
import fable.codenames.board.BoardAttackInteractionService;
import fable.codenames.board.BoardInteractionService;
import fable.codenames.board.BoardSelectionSync;
import fable.codenames.board.BoardVoteService;
import fable.codenames.chat.TeamChatNetworking;
import fable.codenames.config.ModConfig;
import fable.codenames.config.ModConfigManager;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.game.GameTimerSync;
import fable.codenames.game.LobbySpawnService;
import fable.codenames.pedestal.PedestalService;
import fable.codenames.protection.WorldProtectionService;
import fable.codenames.protection.FlowerPotProtectionService;
import fable.codenames.registry.*;
import fable.codenames.score.TeamScoreSync;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

import java.io.File;

public class Codenames implements ModInitializer {
    public static final String MOD_ID = "codenames";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + File.separator + "/config/" + MOD_ID + ".json";
    public static final ModConfig CONFIG = ModConfigManager.loadConfig(CONFIG_FILE_PATH);

    public static void loggerInfo(String message) {
        LOGGER.info(MOD_ID + ": " + message);
    }

    @Override
    public void onInitialize() {
        GeckoLib.initialize();
        RegisterBlocks.init();
        RegisterBlockEntityTypes.init();
        RegisterGameRules.init();
        RegisterItems.init();
        RegisterItemGroups.init();
        RegisterEntityTypes.init();
        RegisterStatusEffects.init();
        RegisterCommands.init();
        BoardAttackInteractionService.init();
        BoardInteractionService.init();
        BoardVoteService.init();
        CodenamesGameService.init();
        LobbySpawnService.init();
        PedestalService.init();
        WorldProtectionService.init();
        FlowerPotProtectionService.init();
        BoardSync.init();
        BoardSelectionSync.init();
        GameTimerSync.init();
        TeamScoreSync.init();
        TeamChatNetworking.init();
    }
}
