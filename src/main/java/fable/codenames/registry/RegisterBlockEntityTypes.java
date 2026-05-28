package fable.codenames.registry;

import fable.codenames.Codenames;
import fable.codenames.block.entity.ModBlockEntityTypes;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public class RegisterBlockEntityTypes {
    public static void init() {
        Arrays.stream(ModBlockEntityTypes.values()).forEach(value -> registerBlockEntity(value.getId(), value.getBlockEntityType()));
    }

    private static void registerBlockEntity(String id, BlockEntityType<? extends BlockEntity> blockEntityType) {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(Codenames.MOD_ID, id), blockEntityType);
    }
}
