package fable.codenames.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;

import java.util.Locale;

public enum ModBlocks {
    MY_BLOCK(new MyBlock(FabricBlockSettings.create())),
    MY_BLOCK_WITH_BLOCK_ENTITY(new MyBlockWithEntityBlock(FabricBlockSettings.create())),
    TEAM_CHAT(new TeamChatBlock(FabricBlockSettings.create().mapColor(MapColor.OAK_TAN).nonOpaque().noCollision().strength(1.0F))),
    CLICK_BUTTON(new ClickButtonBlock(FabricBlockSettings.create().mapColor(MapColor.STONE_GRAY).nonOpaque().strength(1.0F))),
    START_BUTTON(new ClickButtonBlock(FabricBlockSettings.create().mapColor(MapColor.STONE_GRAY).nonOpaque().strength(1.0F), fable.codenames.block.entity.ClickButtonBlockEntity.Mode.RESET));

    private final String id;
    private final Block block;

    <T extends Block> ModBlocks(T block) {
        this.id = this.toString().toLowerCase(Locale.ROOT);
        this.block = block;
    }

    public String getId() {
        return this.id;
    }

    public Block getBlock() {
        return this.block;
    }
}
