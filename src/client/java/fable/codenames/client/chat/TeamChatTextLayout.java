package fable.codenames.client.chat;

import net.minecraft.client.MinecraftClient;
import fable.codenames.chat.TeamChatPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class TeamChatTextLayout {
    public static final int DEFAULT_LEFT_X = 4;
    public static final int DEFAULT_RIGHT_X = 2;
    public static final int DEFAULT_Y = 5;
    public static final float DEFAULT_SCALE = 0.5F;
    private static final int MIN_X = 0;
    private static final int MAX_X = 20;
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 10;
    private static final float MIN_SCALE = 0.35F;
    private static final float MAX_SCALE = 1.0F;
    private static int leftTextX = DEFAULT_LEFT_X;
    private static int rightTextX = DEFAULT_RIGHT_X;
    private static int textY = DEFAULT_Y;
    private static float textScale = DEFAULT_SCALE;
    private static Side activeSide = Side.LEFT;

    private TeamChatTextLayout() {
    }

    public static void load() {
        resetLocalDefaults();
    }

    public static void save() {
        sendUpdate();
    }

    public static int textX() {
        return textX(false);
    }

    public static int textX(boolean own) {
        return own ? rightTextX : leftTextX;
    }

    public static int textY() {
        return textY;
    }

    public static float textScale() {
        return textScale;
    }

    public static float minTextScale() {
        return MIN_SCALE;
    }

    public static Side activeSide() {
        return activeSide;
    }

    public static void toggleSide() {
        activeSide = activeSide == Side.LEFT ? Side.RIGHT : Side.LEFT;
    }

    public static void setActiveSide(Side side) {
        activeSide = side;
    }

    public static void move(int dx, int dy) {
        if (activeSide == Side.LEFT) {
            leftTextX = clamp(leftTextX + dx, MIN_X, MAX_X);
        } else {
            rightTextX = clamp(rightTextX + dx, MIN_X, MAX_X);
        }
        textY = clamp(textY + dy, MIN_Y, MAX_Y);
        save();
    }

    public static void setPosition(Side side, int x, int y) {
        if (side == Side.LEFT) {
            leftTextX = clamp(x, MIN_X, MAX_X);
        } else {
            rightTextX = clamp(x, MIN_X, MAX_X);
        }
        textY = clamp(y, MIN_Y, MAX_Y);
        save();
    }

    public static void scale(float delta) {
        textScale = clamp(textScale + delta, MIN_SCALE, MAX_SCALE);
        save();
    }

    public static void reset() {
        resetLocalDefaults();
        save();
    }

    public static void applySynced(int leftX, int rightX, int y, float scale) {
        leftTextX = clamp(leftX, MIN_X, MAX_X);
        rightTextX = clamp(rightX, MIN_X, MAX_X);
        textY = clamp(y, MIN_Y, MAX_Y);
        textScale = clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public enum Side {
        LEFT,
        RIGHT
    }

    private static void sendUpdate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return;
        }
        var buf = PacketByteBufs.create();
        buf.writeVarInt(leftTextX);
        buf.writeVarInt(rightTextX);
        buf.writeVarInt(textY);
        buf.writeFloat(textScale);
        ClientPlayNetworking.send(TeamChatPackets.UPDATE_TEXT_LAYOUT, buf);
    }

    private static void resetLocalDefaults() {
        leftTextX = DEFAULT_LEFT_X;
        rightTextX = DEFAULT_RIGHT_X;
        textY = DEFAULT_Y;
        textScale = DEFAULT_SCALE;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
