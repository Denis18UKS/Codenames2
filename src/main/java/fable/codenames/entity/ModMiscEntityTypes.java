package fable.codenames.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

import java.util.Locale;

public enum ModMiscEntityTypes {

    HOLOGRAM_PROJECTOR(FabricEntityTypeBuilder.create(SpawnGroup.MISC, HologramProjectorEntity::new)
            .dimensions(EntityDimensions.fixed(0.1f, 0.1f))
            .trackRangeBlocks(32)
            .trackedUpdateRate(1)
            .build()),

    XOD_KOMAND_PROJECTOR(FabricEntityTypeBuilder
            .create(SpawnGroup.MISC, XodKomandProjectorEntity::new)
            .dimensions(EntityDimensions.fixed(0.1f, 0.1f))
            .trackRangeBlocks(32)
            .trackedUpdateRate(1)
            .build()),
            
    PASS_TURN_HOLOGRAM(FabricEntityTypeBuilder
            .<PassTurnHologramEntity>create(SpawnGroup.MISC, 
                (type, world) -> new PassTurnHologramEntity(type, world))
            .dimensions(EntityDimensions.fixed(0.3f, 0.3f))
            .trackRangeBlocks(32)
            .trackedUpdateRate(1)
            .build());

    private final String id;
    private final EntityType<?> entityType;

    ModMiscEntityTypes(EntityType<?> entityType) {
        this.id = this.toString().toLowerCase(Locale.ROOT);
        this.entityType = entityType;
    }

    public String getId() {
        return this.id;
    }

    public EntityType<?> getEntityType() {
        return this.entityType;
    }
}