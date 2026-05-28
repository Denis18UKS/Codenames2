package fable.codenames.block;

import fable.codenames.Codenames;
import fable.codenames.registry.RegisterItemGroups;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HeadBlocks {
    private static final String MANIFEST = "/assets/codenames/heads.txt";
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    private HeadBlocks() {
    }

    public static void init() {
        for (HeadDefinition definition : readDefinitions()) {
            Block block = new CodenamesHeadBlock(FabricBlockSettings.create()
                    .mapColor(MapColor.OAK_TAN)
                    .nonOpaque()
                    .strength(1.0F));
            BLOCKS.put(definition.id(), block);

            Registry.register(Registries.BLOCK, new Identifier(Codenames.MOD_ID, definition.id()), block);
            BlockItem item = Registry.register(Registries.ITEM, new Identifier(Codenames.MOD_ID, definition.id()), new BlockItem(block, new FabricItemSettings()));
            ItemGroupEvents.modifyEntriesEvent(RegisterItemGroups.CUSTOM_ITEM_GROUP_KEY).register(content -> content.add(item));
        }
    }

    public static Map<String, Block> getBlocks() {
        return Map.copyOf(BLOCKS);
    }

    private static Iterable<HeadDefinition> readDefinitions() {
        InputStream stream = HeadBlocks.class.getResourceAsStream(MANIFEST);
        if (stream == null) {
            Codenames.loggerInfo("Head manifest not found: " + MANIFEST);
            return java.util.List.of();
        }

        java.util.List<HeadDefinition> definitions = new java.util.ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.replace("\uFEFF", "").trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\|", 2);
                if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                    definitions.add(new HeadDefinition(parts[0], parts[1]));
                }
            }
        } catch (IOException exception) {
            Codenames.loggerInfo("Failed to read head manifest: " + exception.getMessage());
        }
        return definitions;
    }

    private record HeadDefinition(String id, String texture) {
    }
}
