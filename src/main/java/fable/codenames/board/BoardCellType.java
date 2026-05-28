package fable.codenames.board;

import net.minecraft.text.Text;

public enum BoardCellType {
    UNASSIGNED("unassigned", 0xFFF5A623),
    RED("red", 0xFFFF5555),
    BLUE("blue", 0xFF55AAFF),
    NEUTRAL("neutral", 0xFF5A5A5A),
    ASSASSIN("assassin", 0xFF111111);

    private final String id;
    private final int color;

    BoardCellType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return this.id;
    }

    public int getColor() {
        return this.color;
    }

    public Text getLabel() {
        return Text.literal(switch (this) {
            case UNASSIGNED -> "Не назначено";
            case RED -> "Красная команда";
            case BLUE -> "Синяя команда";
            case NEUTRAL -> "Нейтральный";
            case ASSASSIN -> "Убийца";
        });
    }

    public BoardCellType next() {
        return switch (this) {
            case UNASSIGNED -> RED;
            case RED -> BLUE;
            case BLUE -> NEUTRAL;
            case NEUTRAL -> ASSASSIN;
            case ASSASSIN -> UNASSIGNED;
        };
    }

    public static BoardCellType fromId(String id) {
        for (BoardCellType value : values()) {
            if (value.id.equalsIgnoreCase(id)) {
                return value;
            }
        }
        return UNASSIGNED;
    }
}
