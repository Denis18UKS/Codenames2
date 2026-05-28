package fable.codenames.chat;

import fable.codenames.Codenames;
import net.minecraft.util.Identifier;

public final class TeamChatPackets {
    public static final Identifier OPEN = new Identifier(Codenames.MOD_ID, "open_team_chat");
    public static final Identifier SYNC = new Identifier(Codenames.MOD_ID, "sync_team_chat");
    public static final Identifier SEND = new Identifier(Codenames.MOD_ID, "send_team_chat_message");
    public static final Identifier REQUEST_OPEN = new Identifier(Codenames.MOD_ID, "request_open_team_chat");
    public static final Identifier REQUEST_CLEAR = new Identifier(Codenames.MOD_ID, "request_clear_team_chat");
    public static final Identifier DISPUTE_CLUE = new Identifier(Codenames.MOD_ID, "dispute_clue");
    public static final Identifier MOVE_BANNER = new Identifier(Codenames.MOD_ID, "move_team_chat_banner");
    public static final Identifier OPEN_MOVE_BANNER = new Identifier(Codenames.MOD_ID, "open_team_chat_banner_move");
    public static final Identifier OPEN_TEXT_LAYOUT = new Identifier(Codenames.MOD_ID, "open_team_chat_text_layout");
    public static final Identifier APPLY_CONFIG = new Identifier(Codenames.MOD_ID, "apply_team_chat_config");
    public static final Identifier UPDATE_TEXT_LAYOUT = new Identifier(Codenames.MOD_ID, "update_team_chat_text_layout");
    public static final Identifier SYNC_TEXT_LAYOUT = new Identifier(Codenames.MOD_ID, "sync_team_chat_text_layout");
    public static final Identifier TOGGLE_VANILLA_CHAT = new Identifier(Codenames.MOD_ID, "toggle_vanilla_chat");
    public static final Identifier SYNC_VANILLA_CHAT = new Identifier(Codenames.MOD_ID, "sync_vanilla_chat");
    public static final Identifier FORCE_VANILLA_CHAT = new Identifier(Codenames.MOD_ID, "force_vanilla_chat");

    private TeamChatPackets() {
    }
}
