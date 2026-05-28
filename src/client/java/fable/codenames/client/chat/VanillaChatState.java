package fable.codenames.client.chat;

public final class VanillaChatState {
    private static boolean disabled;

    private VanillaChatState() {
    }

    public static boolean isDisabled() {
        return disabled;
    }

    public static boolean setDisabled(boolean value) {
        boolean changed = disabled != value;
        disabled = value;
        return changed;
    }

    public static boolean toggle() {
        disabled = !disabled;
        return disabled;
    }
}
