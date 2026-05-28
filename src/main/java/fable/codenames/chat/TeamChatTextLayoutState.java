package fable.codenames.chat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class TeamChatTextLayoutState extends PersistentState {
    public static final String KEY = "codenames_team_chat_text_layout";

    private int leftTextX = 4;
    private int rightTextX = 2;
    private int textY = 5;
    private float textScale = 0.5F;

    public static TeamChatTextLayoutState createFromNbt(NbtCompound nbt) {
        TeamChatTextLayoutState state = new TeamChatTextLayoutState();
        state.leftTextX = nbt.getInt("leftTextX");
        state.rightTextX = nbt.getInt("rightTextX");
        state.textY = nbt.getInt("textY");
        state.textScale = nbt.contains("textScale") ? nbt.getFloat("textScale") : 0.5F;
        return state;
    }

    public void set(int leftTextX, int rightTextX, int textY, float textScale) {
        this.leftTextX = leftTextX;
        this.rightTextX = rightTextX;
        this.textY = textY;
        this.textScale = textScale;
        markDirty();
    }

    public int leftTextX() { return leftTextX; }
    public int rightTextX() { return rightTextX; }
    public int textY() { return textY; }
    public float textScale() { return textScale; }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("leftTextX", leftTextX);
        nbt.putInt("rightTextX", rightTextX);
        nbt.putInt("textY", textY);
        nbt.putFloat("textScale", textScale);
        return nbt;
    }
}
