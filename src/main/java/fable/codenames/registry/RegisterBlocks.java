package fable.codenames.registry;

import fable.codenames.Codenames;
import fable.codenames.block.HeadBlocks;
import fable.codenames.block.ModBlocks;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public class RegisterBlocks {
    public static void init() {
        Arrays.stream(ModBlocks.values()).forEach(value -> registerBlockWithItem(value.getId(), value.getBlock()));
        HeadBlocks.init();
    }

    public static void registerBlockWithItem(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(Codenames.MOD_ID, name), block);
        BlockItem item = Registry.register(Registries.ITEM, new Identifier(Codenames.MOD_ID, name), new BlockItem(block, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(RegisterItemGroups.CUSTOM_ITEM_GROUP_KEY).register(content -> content.add(item));
    }
}
