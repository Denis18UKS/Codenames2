package fable.codenames.registry;

import fable.codenames.Codenames;
import fable.codenames.item.ModItems;
import fable.codenames.item.ModSpawnEggItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public class RegisterItems {
    public static void init() {
        Arrays.stream(ModItems.values()).forEach(item ->
                Registry.register(
                        Registries.ITEM,
                        new Identifier(Codenames.MOD_ID, item.getId()),
                        item.getItem()));

        ItemGroupEvents.modifyEntriesEvent(RegisterItemGroups.CUSTOM_ITEM_GROUP_KEY).register(entries -> {
            Arrays.stream(ModItems.values()).forEach(item -> entries.add(item.getItem()));
            entries.add(ModSpawnEggItems.CROWN_SPAWN_EGG.getItem());
        });

        Arrays.stream(ModSpawnEggItems.values()).forEach(spawnEgg ->
                Registry.register(
                        Registries.ITEM,
                        new Identifier(Codenames.MOD_ID, spawnEgg.getId()),
                        spawnEgg.getItem()));
    }
}
