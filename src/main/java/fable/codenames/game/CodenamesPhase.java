package fable.codenames.game;

public enum CodenamesPhase {
    STOPPED("stopped"),
    WAITING_CLUE("waiting_clue"),
    GUESSING("guessing"),
    FINISHED("finished");

    private final String id;

    CodenamesPhase(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public static CodenamesPhase fromId(String id) {
        for (CodenamesPhase phase : values()) {
            if (phase.id.equals(id)) {
                return phase;
            }
        }
        return STOPPED;
    }
}
