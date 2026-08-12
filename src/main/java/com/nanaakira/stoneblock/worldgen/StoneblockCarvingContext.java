package com.nanaakira.stoneblock.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.CarvingContext;

import java.util.Optional;
import java.util.function.Function;

/**
 * Custom CarvingContext for StoneBlock dimensions.
 * Always returns bedrock as the top material for carving operations.
 */
public class StoneblockCarvingContext extends CarvingContext {
    public StoneblockCarvingContext(NoiseBasedChunkGenerator generator, RegistryAccess registryAccess,
                                    LevelHeightAccessor heightAccessor, NoiseChunk noiseChunk,
                                    RandomState randomState, SurfaceRules.RuleSource ruleSource) {
        super(generator, registryAccess, heightAccessor, noiseChunk, randomState, ruleSource);
    }

    @Override
    @Deprecated
    public Optional<BlockState> topMaterial(Function<BlockPos, Holder<Biome>> biomeFunction,
                                            ChunkAccess chunk, BlockPos pos, boolean hasFluid) {
        return Optional.of(Blocks.BEDROCK.defaultBlockState());
    }
}