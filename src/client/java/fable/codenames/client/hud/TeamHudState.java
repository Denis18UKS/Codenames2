package fable.codenames.client.hud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TeamHudState {
    private static final Path CONFIG_PATH = Path.of("config", "codenames_hud.properties");
    private static boolean enabled = false;
    private static int x = 8;
    private static int y = 24;

    private TeamHudState() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(CONFIG_PATH)) {
            properties.load(stream);
            enabled = Boolean.parseBoolean(properties.getProperty("enabled", Boolean.toString(enabled)));
            x = parseInt(properties.getProperty("x"), x);
            y = parseInt(properties.getProperty("y"), y);
        } catch (IOException ignored) {
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static boolean toggle() {
        enabled = !enabled;
        save();
        return enabled;
    }

    public static int getX() {
        return x;
    }

    public static int getY() {
        return y;
    }

    public static void setPosition(int newX, int newY) {
        x = Math.max(0, newX);
        y = Math.max(0, newY);
    }

    public static void resetPosition() {
        x = 8;
        y = 24;
        save();
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("x", Integer.toString(x));
        properties.setProperty("y", Integer.toString(y));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream stream = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(stream, "Codenames HUD settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
