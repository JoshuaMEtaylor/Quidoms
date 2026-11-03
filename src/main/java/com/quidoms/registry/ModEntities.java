package com.quidoms.registry;

import com.quidoms.QuidomsMod;
import com.quidoms.entity.CitizenEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, QuidomsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CitizenEntity>> CITIZEN =
            ENTITIES.register("citizen", () -> EntityType.Builder.of(CitizenEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(QuidomsMod.MOD_ID, "citizen"))));

    private ModEntities() {
    }
}
