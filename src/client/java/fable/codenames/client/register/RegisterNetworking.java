package fable.codenames.client.register;

import fable.codenames.board.BoardCellType;
import fable.codenames.board.BoardSelectionSync;
import fable.codenames.board.BoardSync;
import fable.codenames.chat.TeamChatMessage;
import fable.codenames.chat.TeamChatPackets;
import fable.codenames.client.chat.ForcedVanillaChatHud;
import fable.codenames.client.chat.TeamChatTextLayout;
import fable.codenames.client.board.BoardClientState;
import fable.codenames.client.chat.VanillaChatState;
import fable.codenames.client.chat.TeamChatClientState;
import fable.codenames.client.game.GameTimerClientState;
import fable.codenames.client.screen.TeamChatBannerInputScreen;
import fable.codenames.client.screen.TeamChatBannerMoveScreen;
import fable.codenames.client.screen.TeamChatTextLayoutScreen;
import fable.codenames.client.score.TeamScoreClientState;
import fable.codenames.game.CodenamesPhase;
import fable.codenames.game.GameTimerSync;
import fable.codenames.score.TeamScoreSync;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegisterNetworking {

    private RegisterNetworking() {}

    public static void init() {

        ClientPlayNetworking.registerGlobalReceiver(BoardSync.CHANNEL_ID,
                (client, handler, buf, responseSender) -> {

                    int size = buf.readVarInt();
                    Map<BlockPos, BoardCellType> cells = new LinkedHashMap<>();

                    for (int i = 0; i < size; i++) {
                        cells.put(buf.readBlockPos(), buf.readEnumConstant(BoardCellType.class));
                    }

                    boolean canSeeAnswers = buf.readBoolean();

                    client.execute(() -> {
                        BoardClientState.setCells(cells);
                        BoardClientState.setCanSeeAnswers(canSeeAnswers);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(BoardSelectionSync.CHANNEL_ID,
                (client, handler, buf, responseSender) -> {

                    int size = buf.readVarInt();
                    java.util.ArrayList<BoardClientState.VoteIndicator> indicators =
                            new java.util.ArrayList<>(size);

                    for (int i = 0; i < size; i++) {
                        indicators.add(new BoardClientState.VoteIndicator(
                                buf.readString(),
                                buf.readBlockPos(),
                                buf.readBlockPos(),
                                buf.readVarInt(),
                                buf.readLong()));
                    }

                    client.execute(() -> BoardClientState.setVoteIndicators(indicators));
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamScoreSync.CHANNEL_ID,
                (client, handler, buf, responseSender) -> {

                    int size = buf.readVarInt();
                    Map<String, Integer> scores = new LinkedHashMap<>();

                    for (int i = 0; i < size; i++) {
                        scores.put(buf.readString(), buf.readVarInt());
                    }

                    client.execute(() -> TeamScoreClientState.setScores(scores));
                });

        // =========================
        // 🔥 ИСПРАВЛЕННЫЙ ТАЙМЕР
        // =========================
        ClientPlayNetworking.registerGlobalReceiver(GameTimerSync.CHANNEL_ID,
                (client, handler, buf, responseSender) -> {

                    boolean active = buf.readBoolean();
                    String teamName = buf.readString();
                    CodenamesPhase phase = CodenamesPhase.fromId(buf.readString());
                    long remainingTicks = buf.readLong();
                    long totalTicks = buf.readLong();
                    boolean canPassTurn = buf.readBoolean();

                    client.execute(() -> {

                        // ❗ ВАЖНО: если сервер сказал "не активен" — полностью сбрасываем UI
                        if (!active || totalTicks <= 0L) {
                            GameTimerClientState.clear();
                            return;
                        }

                        GameTimerClientState.update(
                                true,
                                teamName,
                                phase,
                                remainingTicks,
                                totalTicks,
                                canPassTurn
                        );
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.OPEN,
                (client, handler, buf, responseSender) -> {

                    boolean hasBannerPos = buf.readBoolean();
                    BlockPos bannerPos = hasBannerPos ? buf.readBlockPos() : null;
                    String teamName = buf.readString();
                    boolean canSend = buf.readBoolean();
                    List<TeamChatMessage> messages = readMessages(buf);

                    client.execute(() -> {
                        TeamChatClientState.update(teamName, canSend, messages);
                        if (bannerPos != null) {
                            client.setScreen(new TeamChatBannerInputScreen(bannerPos));
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.SYNC,
                (client, handler, buf, responseSender) -> {

                    String teamName = buf.readString();
                    boolean canSend = buf.readBoolean();
                    List<TeamChatMessage> messages = readMessages(buf);

                    client.execute(() -> {
                        TeamChatClientState.update(teamName, canSend, messages);
                        if (!canSend && client.currentScreen instanceof TeamChatBannerInputScreen) {
                            client.setScreen(null);
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.OPEN_MOVE_BANNER,
                (client, handler, buf, responseSender) -> {

                    BlockPos bannerPos = buf.readBlockPos();
                    client.execute(() ->
                            client.setScreen(new TeamChatBannerMoveScreen(bannerPos)));
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.OPEN_TEXT_LAYOUT,
                (client, handler, buf, responseSender) ->
                        client.execute(() ->
                                client.setScreen(new TeamChatTextLayoutScreen())));

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.SYNC_TEXT_LAYOUT,
                (client, handler, buf, responseSender) -> {

                    int leftTextX = buf.readVarInt();
                    int rightTextX = buf.readVarInt();
                    int textY = buf.readVarInt();
                    float textScale = buf.readFloat();

                    client.execute(() ->
                            TeamChatTextLayout.applySynced(leftTextX, rightTextX, textY, textScale));
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.SYNC_VANILLA_CHAT,
                (client, handler, buf, responseSender) -> {

                    boolean disabled = buf.readBoolean();
                    client.execute(() -> VanillaChatState.setDisabled(disabled));
                });

        ClientPlayNetworking.registerGlobalReceiver(TeamChatPackets.FORCE_VANILLA_CHAT,
                (client, handler, buf, responseSender) -> {

                    net.minecraft.text.Text message = buf.readText();
                    client.execute(() ->
                            ForcedVanillaChatHud.add(message));
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BoardClientState.clear();
            TeamScoreClientState.setScores(Map.of());
            TeamChatClientState.clear();
            GameTimerClientState.clear();
            VanillaChatState.setDisabled(false);
            TeamChatTextLayout.load();
        });
    }

    private static List<TeamChatMessage> readMessages(net.minecraft.network.PacketByteBuf buf) {
        int size = buf.readVarInt();
        java.util.ArrayList<TeamChatMessage> messages = new java.util.ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            messages.add(TeamChatMessage.read(buf));
        }

        return List.copyOf(messages);
    }
}