package fable.codenames.role;

public enum PlayerRole {
    LIDER("lider"),
    GUESSING("guessing");

    private final String id;

    PlayerRole(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public static PlayerRole fromId(String id) {
        for (PlayerRole role : values()) {
            if (role.id.equalsIgnoreCase(id)) {
                return role;
            }
        }

        return null;
    }
}
