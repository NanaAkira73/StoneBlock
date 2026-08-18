package com.nanaakira.stoneblock.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Custom BiomeSource for StoneBlock dimensions.
 * Maps chunks to biomes based on distance from origin using KubeJS configuration.
 */
public class StoneBlockBiomeSource extends BiomeSource {
    public static final Codec<StoneBlockBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(arg -> arg.biomes)
    ).apply(instance, instance.stable(StoneBlockBiomeSource::new)));

    private final HolderSet<Biome> biomes;
    private final List<Holder<Biome>> biomeList;
    private boolean loggedOnce = false;

    private StoneBlockBiomeSource(HolderSet<Biome> b) {
        this.biomes = b;
        this.biomeList = b.stream().toList();
    }

    public StoneBlockBiomeSource(Registry<Biome> r) {
        this(HolderSet.direct(StoneBlockDataKjs.createBiomes(r)));
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(qx);
        int z = QuartPos.toBlock(qz);
        StoneBlockDataKjs config = StoneBlockDataKjs.getConfig(x, z);
        Holder<Biome> biome = biomeList.get(config.index);
        if (!loggedOnce) {
            loggedOnce = true;
            org.slf4j.LoggerFactory.getLogger(StoneBlockBiomeSource.class).info(
                    "[StoneBlock] getNoiseBiome first: block=({},{}) config.index={} biomeList.size={} biome={}",
                    x, z, config.index, biomeList.size(), biome);
        }
        return biome;
    }
}