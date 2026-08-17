package com.nanaakira.stoneblock.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.chunk.CarvingMask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom chunk generator for StoneBlock dimensions.
 * Generates terrain based on KubeJS layer configuration instead of vanilla noise.
 */
public class StoneblockChunkGenerator extends NoiseBasedChunkGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneblockChunkGenerator.class);

    @SuppressWarnings("deprecation")
    public static final ResourceLocation STRUCTURE_SET_TAG = new ResourceLocation("stoneblock", "stoneblock_structure_set");

    public static final Codec<StoneblockChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(StoneblockChunkGenerator::generatorSettings),
            ResourceLocation.CODEC.fieldOf("prebuilt_structure").forGetter(g -> g.prebuiltStructure)
    ).apply(instance, instance.stable(StoneblockChunkGenerator::new)));

    private static final int SIZE = StoneBlockDataKjs.SIZE;
    private static final int HEIGHT = StoneBlockDataKjs.HEIGHT;

    private static NoiseRouter none() {
        return new NoiseRouter(
                DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(),
                DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(),
                DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(),
                DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(),
                DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero()
        );
    }

    public static final NoiseGeneratorSettings SETTINGS = new NoiseGeneratorSettings(
            NoiseSettings.create(-SIZE, HEIGHT, 1, 2),
            Blocks.STONE.defaultBlockState(),
            Blocks.WATER.defaultBlockState(),
            none(),
            SurfaceRules.state(Blocks.STONE.defaultBlockState()),
            Collections.emptyList(),
            32,
            false,
            false,
            false,
            false
    );

    private Set<Biome> biomesWithCarvers;
    public final ResourceLocation prebuiltStructure;

    public StoneblockChunkGenerator(BiomeSource biomeSource,
                                     Holder<NoiseGeneratorSettings> settings, ResourceLocation prebuiltStructure) {
        super(biomeSource, settings);
        this.prebuiltStructure = prebuiltStructure;
    }

    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return 0;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup,
                                                     RandomState randomState, long seed) {
        // 只生成 stoneblock_structure_set 标签中的结构集，阻止原版结构（矿坑、村庄等）生成
        HolderSet<StructureSet> structures = structureSetLookup.getOrThrow(
                TagKey.create(Registries.STRUCTURE_SET, STRUCTURE_SET_TAG));
        return ChunkGeneratorStructureState.createForFlat(randomState, seed, this.biomeSource, structures.stream());
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunkAccess, GenerationStep.Carving carving) {
        if (biomesWithCarvers == null) {
            biomesWithCarvers = new HashSet<>();
            Registry<Biome> biomeRegistry = region.registryAccess().registryOrThrow(Registries.BIOME);
            for (StoneBlockDataKjs data : StoneBlockDataKjs.BIOMES) {
                if (data.carvers) {
                    biomesWithCarvers.add(biomeRegistry.getOrThrow(data.biome));
                }
            }
        }

        Climate.Sampler sampler = randomState.sampler();
        BiomeManager biomeManager1 = biomeManager.withDifferentSource(
                (ix, jx, kx) -> biomeSource.getNoiseBiome(ix, jx, kx, sampler)
        );

        ChunkPos chunkPos = chunkAccess.getPos();
        ProtoChunk protoChunk = (ProtoChunk) chunkAccess;
        NoiseChunk noiseChunk = protoChunk.getOrCreateNoiseChunk((chunk) ->
                NoiseChunk.forChunk(chunk, randomState,
                        Beardifier.forStructuresInChunk(structureManager, chunk.getPos()),
                        this.generatorSettings().value(),
                        (x, y, z) -> new Aquifer.FluidStatus(0, Blocks.AIR.defaultBlockState()),
                        Blender.empty())
        );
        CarvingContext carvingContext = new StoneblockCarvingContext(this, region.registryAccess(),
                chunkAccess.getHeightAccessorForGeneration(), noiseChunk, randomState,
                this.generatorSettings().value().surfaceRule());
        CarvingMask carvingMask = protoChunk.getOrCreateCarvingMask(carving);

        for (int x = -8; x <= 8; ++x) {
            for (int z = -8; z <= 8; ++z) {
                ChunkPos chunkPos1 = new ChunkPos(chunkPos.x + x, chunkPos.z + z);
                ChunkAccess chunkAccess1 = region.getChunk(chunkPos1.x, chunkPos1.z);
                Holder<Biome> biome1 = biomeSource.getNoiseBiome(QuartPos.fromBlock(chunkPos1.getMinBlockX()), 0,
                        QuartPos.fromBlock(chunkPos1.getMinBlockZ()), sampler);
                chunkAccess1.carverBiome(() -> biome1.value().getGenerationSettings());

                if (biomesWithCarvers.contains(biome1.value())) {
                    var carvers = biome1.value().getGenerationSettings().getCarvers(carving);
                    int i = 0;
                    for (Holder<ConfiguredWorldCarver<?>> carver : carvers) {
                        var configuredWorldCarver = carver.value();
                        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(seed + i));
                        worldgenRandom.setLargeFeatureSeed(seed + i, chunkPos1.x, chunkPos1.z);
                        if (configuredWorldCarver.isStartChunk(worldgenRandom)) {
                            configuredWorldCarver.carve(carvingContext, chunkAccess,
                                    biomeManager1::getBiome, worldgenRandom,
                                    NoAquifer.INSTANCE, chunkPos1, carvingMask);
                        }
                        i++;
                    }
                }
            }
        }
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunkAccess) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    private boolean fillFromNoiseLogged = false;

    @Override
    @NotNull
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                         RandomState randomState,
                                                         StructureManager structureManager,
                                                         ChunkAccess chunkAccess) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        Heightmap heightmap1 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        int minY = chunkAccess.getMinBuildHeight();
        int maxY = chunkAccess.getMaxBuildHeight();
        int cx = chunkAccess.getPos().getMinBlockX();
        int cz = chunkAccess.getPos().getMinBlockZ();

        if (!fillFromNoiseLogged) {
            fillFromNoiseLogged = true;
            LOGGER.info("[StoneBlock] fillFromNoise first call: chunk=({},{}) minY={} maxY={} BIOMES={} totalDistance={} centerX={} centerZ={}",
                    cx, cz, minY, maxY, StoneBlockDataKjs.BIOMES.size(), StoneBlockDataKjs.totalDistance,
                    StoneBlockDataKjs.centerX, StoneBlockDataKjs.centerZ);
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int ax = cx + x;
                int az = cz + z;
                StoneBlockDataKjs config = StoneBlockDataKjs.getConfig(ax, az);
                for (int y = minY; y < maxY; y++) {
                    BlockState state = config.getState(ax, y, az);
                    chunkAccess.setBlockState(mutableBlockPos.set(x, y, z), state, false);
                    heightmap1.update(x, y, z, state);
                    heightmap2.update(x, y, z, state);
                }
            }
        }

        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return -SIZE - 1;
    }

    @Override
    public int getMinY() {
        return -SIZE;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor level,
                             RandomState randomState) {
        return SIZE - 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
                                      RandomState randomState) {
        BlockState[] blockStates = new BlockState[HEIGHT];
        StoneBlockDataKjs.getConfig(x, z).fillColumn(x, z, blockStates);
        return new NoiseColumn(level.getMinBuildHeight(), blockStates);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {
    }

    public static class NoAquifer implements Aquifer {
        public static final NoAquifer INSTANCE = new NoAquifer();

        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext context, double value) {
            return Blocks.CAVE_AIR.defaultBlockState();
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return false;
        }
    }
}