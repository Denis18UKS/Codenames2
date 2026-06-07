package fable.codenames.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;

import java.util.Locale;

public enum ModItems {
    OSPORIT(new MyItem(new FabricItemSettings())),
    HOLOGRAM_PROJECTOR(new HologramProjectorItem(new FabricItemSettings().maxCount(1))),
    XOD_KOMAND_PROJECTOR(new XodKomandProjectorItem(new FabricItemSettings().maxCount(1))),
    BOARD_CONFIGURATOR(new BoardConfiguratorItem(new FabricItemSettings().maxCount(1))),
    PEDESTAL_CONFIGURATOR(new PedestalConfiguratorItem(new FabricItemSettings().maxCount(1))),
    TELEPORT_CONFIGURATOR(new TeleportConfiguratorItem(new FabricItemSettings().maxCount(1))),
    TEAM_CHAT_CONFIGURATOR(new TeamChatConfiguratorItem(new FabricItemSettings().maxCount(1))),
    TEAM_CHAT_TEXT_CONFIGURATOR(new TeamChatTextConfiguratorItem(new FabricItemSettings().maxCount(1)));

    public final String id;
    public final Item item;

    <T extends Item> ModItems(T item) {
        this.id = this.toString().toLowerCase(Locale.ROOT);
        this.item = item;
    }

    public String getId() {
        return this.id;
    }

    public Item getItem() {
        return this.item;
    }
}
