package fable.codenames.registry;

import fable.codenames.Codenames;
import fable.codenames.item.ModItems;
import fable.codenames.item.ModItemGroups;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

import java.util.Arrays;

public class RegisterItemGroups {
    public static final RegistryKey<ItemGroup> CUSTOM_ITEM_GROUP_KEY = 
        RegistryKey.of(RegistryKeys.ITEM_GROUP, 
            Identifier.of(Codenames.MOD_ID, "item_group"));

    public static void init() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_ITEM_GROUP_KEY,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.OSPORIT.getItem()))
                .displayName(Text.translatable("group." + Codenames.MOD_ID + ".item_group"))
                .entries((context, entries) ->{
                    entries.add(ModItems.OSPORIT.getItem());
                })
                .build());
    }
}
