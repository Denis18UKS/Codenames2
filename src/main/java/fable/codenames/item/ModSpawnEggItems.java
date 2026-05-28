package fable.codenames.item;

import fable.codenames.entity.ModMobEntityTypes;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SpawnEggItem;

import java.util.Locale;

public enum ModSpawnEggItems {
    MY_HOSTILE_SPAWN_EGG(new SpawnEggItem(ModMobEntityTypes.MY_HOSTILE.getEntityType(), 0xafafaf, 0xfafafa, new FabricItemSettings())),
    MY_PASSIVE_SPAWN_EGG(new SpawnEggItem(ModMobEntityTypes.MY_PASSIVE.getEntityType(), 0xfafafa, 0xafafaf, new FabricItemSettings())),
    CROWN_SPAWN_EGG(new SpawnEggItem(ModMobEntityTypes.CROWN.getEntityType(), 0xE0C04F, 0x9D1F2D, new FabricItemSettings()));

    public final String id;
    public final SpawnEggItem item;

    <T extends SpawnEggItem> ModSpawnEggItems(T item) {
        this.id = this.toString().toLowerCase(Locale.ROOT);
        this.item = item;
    }

    public String getId() {
        return this.id;
    }

    public SpawnEggItem getItem() {
        return this.item;
    }
}
