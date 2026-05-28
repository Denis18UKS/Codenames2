package fable.codenames.chat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

public class TeamChatState extends PersistentState {
    public static final String KEY = "codenames_team_chat";
    private static final int MAX_MESSAGES = 200;

    private final List<TeamChatMessage> messages = new ArrayList<>();

    public static TeamChatState createFromNbt(NbtCompound nbt) {
        TeamChatState state = new TeamChatState();
        NbtList messagesNbt = nbt.getList("messages", 10);
        for (int i = 0; i < messagesNbt.size(); i++) {
            state.messages.add(TeamChatMessage.fromNbt(messagesNbt.getCompound(i)));
        }
        return state;
    }

    public List<TeamChatMessage> getMessages() {
        return List.copyOf(this.messages);
    }

    public void addMessage(TeamChatMessage message) {
        this.messages.add(message);
        if (this.messages.size() > MAX_MESSAGES) {
            this.messages.subList(0, this.messages.size() - MAX_MESSAGES).clear();
        }
        markDirty();
    }

    public void clearMessages() {
        this.messages.clear();
        markDirty();
    }

    public void removeLatestMessage(String teamName, String content) {
        for (int i = this.messages.size() - 1; i >= 0; i--) {
            TeamChatMessage message = this.messages.get(i);
            if (message.teamName().equals(teamName) && message.content().equals(content)) {
                this.messages.remove(i);
                markDirty();
                return;
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        this.messages.forEach(message -> list.add(message.toNbt()));
        nbt.put("messages", list);
        return nbt;
    }
}
