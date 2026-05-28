package fable.codenames.head;

import fable.codenames.block.HeadBlocks;
import net.minecraft.block.Block;

import java.util.List;
import java.util.Map;

public final class HeadDeck {
    public static final int UNUSED_WEIGHT = 50;
    public static final int USED_WEIGHT = 1;

    private HeadDeck() {
    }

    public static List<Entry> entries(HeadDeckState state) {
        return HeadBlocks.getBlocks().entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue(), state.isUsed(entry.getKey()) ? USED_WEIGHT : UNUSED_WEIGHT))
                .toList();
    }

    public static Map<String, Block> blocks() {
        return HeadBlocks.getBlocks();
    }

    public record Entry(String id, Block block, int weight) {
    }
}
