package com.nanaakira.stoneblock.integration.kubejs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * bits_spawner 的刷怪表配置（从 FTB StoneBlock Companion 的 SpawnerDataKjs 移植）。
 * 通过 KubeJS 绑定为全局变量 stoneblockEntitiesData，脚本可调用：
 *   stoneblockEntitiesData.setMinSpawnAmount(12);
 *   stoneblockEntitiesData.setMaxSpawnAmount(24);
 *   stoneblockEntitiesData.createBuilder()
 *       .addEntity("minecraft:creeper", "ftbdungeons:has_structure/stone_dungeon")
 *       .addEntity("minecraft:skeleton", null)
 *       .build();
 */
public class SpawnerDataKjs {
    public static List<SpawnableEntity> entitiesToSpawn = new ArrayList<>();

    public static int minSpawnAmount = 5;
    public static int maxSpawnAmount = 20;

    public static void setMinSpawnAmount(int minSpawnAmount) {
        SpawnerDataKjs.minSpawnAmount = minSpawnAmount;
    }

    public static void setMaxSpawnAmount(int maxSpawnAmount) {
        SpawnerDataKjs.maxSpawnAmount = maxSpawnAmount;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public static final class Builder {
        public List<SpawnableEntity> entities = new ArrayList<>();

        public Builder() {
        }

        public Builder addEntity(ResourceLocation entityType, @Nullable ResourceLocation biome) {
            if (biome != null) {
                TagKey<Biome> biomeTagKey = TagKey.create(Registries.BIOME, biome);
                entities.add(new SpawnableEntity(entityType, biomeTagKey));
            } else {
                entities.add(new SpawnableEntity(entityType, null));
            }
            return this;
        }

        public void build() {
            SpawnerDataKjs.entitiesToSpawn.clear();
            SpawnerDataKjs.entitiesToSpawn.addAll(entities);
        }
    }

    public record SpawnableEntity(
            ResourceLocation entityId,
            @Nullable TagKey<Biome> allowedBiome
    ) {
    }
}
