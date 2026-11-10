/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.world;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jspecify.annotations.Nullable;

public class MobSpawnSettingsBuilder extends MobSpawnSettings.Builder {
    private final Set<MobCategory> typesView = Collections.unmodifiableSet(this.spawnsByCategory.keySet());
    private final Set<EntityType<?>> costView = Collections.unmodifiableSet(this.mobSpawnCosts.keySet());

    public MobSpawnSettingsBuilder(MobSpawnSettings orig) {
        orig.definedCategories().forEach(k -> forCategory(k).addAll(orig.getMobsInCategory(k)));
        this.mobSpawnCosts.putAll(orig.allSpawnCosts());
    }

    public Set<MobCategory> getSpawnerTypes() {
        return this.typesView;
    }

    public WeightedList.@Nullable Builder<MobSpawnSettings.SpawnerData> getSpawner(MobCategory type) {
        return this.spawnsByCategory.get(type);
    }

    public Set<EntityType<?>> getEntityTypes() {
        return this.costView;
    }

    public MobSpawnSettings.@Nullable MobSpawnCost getCost(EntityType<?> type) {
        return this.mobSpawnCosts.get(type);
    }

    public MobSpawnSettingsBuilder disablePlayerSpawn() {
        return this;
    }


    public MobSpawnSettingsBuilder removeSpawns(Predicate<Weighted<MobSpawnSettings.SpawnerData>> filter) {
        //Mekanism shim: vanilla's WeightedList.Builder is backed by an immutable list builder, so rebuild the lists without the removed entries
        for (java.util.Map.Entry<MobCategory, WeightedList.Builder<MobSpawnSettings.SpawnerData>> entry : this.spawnsByCategory.entrySet()) {
            java.util.List<Weighted<MobSpawnSettings.SpawnerData>> spawns = entry.getValue().getList();
            if (spawns.stream().anyMatch(filter)) {
                WeightedList.Builder<MobSpawnSettings.SpawnerData> rebuilt = WeightedList.builder();
                for (Weighted<MobSpawnSettings.SpawnerData> spawn : spawns) {
                    if (!filter.test(spawn)) {
                        rebuilt.add(spawn);
                    }
                }
                entry.setValue(rebuilt);
            }
        }
        return this;
    }

    public MobSpawnSettingsBuilder removeSpawnCost(EntityType<?>... entityTypes) {
        for (EntityType<?> entityType : entityTypes) {
            this.mobSpawnCosts.remove(entityType);
        }
        return this;
    }
}
