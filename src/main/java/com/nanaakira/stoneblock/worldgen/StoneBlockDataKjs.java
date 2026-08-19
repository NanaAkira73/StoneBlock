package com.nanaakira.stoneblock.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * KubeJS-driven configuration system for StoneBlock world generation.
 * Controls biome distribution, block layers, and prebuilt structures.
 */
public final class StoneBlockDataKjs {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoneBlockDataKjs.class);
    private static final Random RANDOM = new Random();

    public static boolean debugMode = false;
    public static final List<StoneBlockDataKjs> BIOMES = new ArrayList<>();
    public static final Map<String, PrebuiltStructure> PREBUILT_STRUCTURES = new LinkedHashMap<>();
    public static final PositionalRandomFactory RANDOM_FACTORY = new LegacyRandomSource.LegacyPositionalRandomFactory(0L);

    public static int totalDistance = 1;
    public static ResourceLocation lobbyStructure = null;
    public static final int SIZE = 128;
    public static final int HEIGHT = SIZE * 2;

    // 环形石头世界的圆心（方块坐标）。默认 (0, 0) 与原版 FTB StoneBlock 3 对齐：
    // 基地（由 ConstructionWorkerMixin 强制放在原点）、环形圆心、结构距离基准点三者一致。
    // 可通过 KubeJS 脚本在 startup 阶段修改（例如 StoneblockData.centerX = 0）。
    public static int centerX = 0;
    public static int centerZ = 0;

    public static void reset() {
        PREBUILT_STRUCTURES.clear();
        BIOMES.clear();
        totalDistance = 0;
        lobbyStructure = null;
    }

    public static void setLobbyStructure(ResourceLocation location) {
        lobbyStructure = location;
    }

    public static PrebuiltStructure addStart(ResourceLocation id, Component name, String author) {
        PrebuiltStructure start = new PrebuiltStructure(id, name, author);
        PREBUILT_STRUCTURES.put(id.toString(), start);
        return start;
    }

    public static PrebuiltStructure addStart(ResourceLocation id, Component name) {
        return addStart(id, name, "NanaAkira");
    }

    public static StoneBlockDataKjs addBiome(ResourceLocation biome, int size) {
        StoneBlockDataKjs config = new StoneBlockDataKjs(ResourceKey.create(Registries.BIOME, biome), totalDistance);
        BIOMES.add(config);
        BIOMES.sort(Comparator.comparingInt(StoneBlockDataKjs::getOrder));
        for (int i = 0; i < BIOMES.size(); i++) {
            BIOMES.get(i).index = i;
        }
        totalDistance += size;
        return config;
    }

    public static StoneBlockDataKjs getConfig(int distance) {
        int dist = distance % totalDistance;
        for (StoneBlockDataKjs config : BIOMES) {
            if (dist >= config.startsAt) {
                return config;
            }
        }
        return BIOMES.get(0);
    }

    public static StoneBlockDataKjs getConfig(int x, int z) {
        int dx = x - centerX;
        int dz = z - centerZ;
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        StoneBlockDataKjs data = getConfig(distance);
        if (data.blend > 0) {
            int d = RANDOM_FACTORY.at(x, 0, z).nextInt(data.blend);
            if (d > 0) {
                return getConfig(distance + d);
            }
        }
        return data;
    }

    public static int getColor(ServerLevel level, BlockPos pos) {
        if (pos.getX() == centerX && pos.getZ() == centerZ) {
            return 0xFF00FF00;
        }
        return getConfig(pos.getX(), pos.getZ()).biomeColor;
    }

    public static List<Holder<Biome>> createBiomes(Registry<Biome> r) {
        List<Holder<Biome>> list = new ArrayList<>();
        for (StoneBlockDataKjs config : BIOMES) {
            list.add(r.getHolderOrThrow(config.biome));
        }
        return list;
    }

    public static void finish() {
        HashSet<String> uniqueIds = new HashSet<>();
        for (StoneBlockDataKjs config : BIOMES) {
            config.uniqueId = config.biome.location().getPath();
            int c = 2;
            while (uniqueIds.contains(config.uniqueId)) {
                config.uniqueId = config.biome.location().getPath() + "_" + c;
                c++;
            }
            uniqueIds.add(config.uniqueId);
            config.finishConfig();
        }
    }

    public static int getDefaultMaxDistance() {
        return Math.min(Mth.ceil(totalDistance * 1.1D), 10000);
    }

    /**
     * Create a StoneblockChunkGenerator for FTB Team Bases' dynamic dimensions.
     * Used by the mixin redirect so each team's private base dimension becomes
     * an independent stone ring world.
     */
    public static ChunkGenerator createChunkGenerator(RegistryAccess registryAccess) {
        LOGGER.info("[StoneBlock] createChunkGenerator called: BIOMES={}, totalDistance={}, centerX={}, centerZ={}",
                BIOMES.size(), totalDistance, centerX, centerZ);
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        StoneBlockBiomeSource biomeSource = new StoneBlockBiomeSource(biomeRegistry);
        Holder<NoiseGeneratorSettings> settings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("stoneblock", "stoneblock")));
        // prebuilt structure is not used here; FTB Team Bases places the base itself
        return new StoneblockChunkGenerator(biomeSource, settings, new ResourceLocation("stoneblock", "none"));
    }

    public int index;
    public String uniqueId;
    public final ResourceKey<Biome> biome;
    private final int startsAt;
    private final List<StoneBlockLayerKjs> layerList;
    private StoneBlockLayerKjs[] layers;
    public int biomeColor;
    public int blend;
    public boolean carvers;

    public StoneBlockDataKjs(ResourceKey<Biome> biome, int startsAt) {
        this.biome = biome;
        this.startsAt = startsAt;
        this.layerList = new ArrayList<>();
        this.biomeColor = RANDOM.nextInt() | 0xFF000000;
        this.blend = 40;
        this.carvers = false;
    }

    @Override
    public String toString() {
        return uniqueId;
    }

    public void setColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        biomeColor = 0xFF000000 | b << 16 | g << 8 | r;
    }

    public void addLayer(String layer) {
        layerList.add(new StoneBlockLayerKjs(layer));
    }

    public void finishConfig() {
        if (layerList.isEmpty()) {
            layerList.add(new StoneBlockLayerKjs("minecraft:stone"));
            LOGGER.error("No layers added in {}", biome.location());
        }
        layers = new StoneBlockLayerKjs[SIZE];
        StoneBlockLayerKjs lastLayer = layerList.get(layerList.size() - 1);
        Arrays.fill(layers, lastLayer);

        int index = 0;
        breakAll:
        for (StoneBlockLayerKjs layer : layerList) {
            if (layer == lastLayer) {
                break;
            }
            for (int y = 0; y < layer.size; y++) {
                layers[index] = layer;
                index++;
                if (index == layers.length) {
                    break breakAll;
                }
            }
        }

        if (debugMode) {
            LOGGER.info("=== Debug layer output of {} ===", biome.location());
            for (int i = 0; i < layers.length; i++) {
                LOGGER.info(String.format("[%03d] %s", i, layers[i].block));
            }
        }
    }

    public StoneBlockLayerKjs getLayer(int y) {
        return layers[Mth.clamp(Math.abs(y), 0, layers.length - 1)];
    }

    public BlockState getState(int x, int y, int z) {
        if (!debugMode && (y == -SIZE || y == SIZE - 1)) {
            return Blocks.BEDROCK.defaultBlockState();
        } else if (debugMode && ((x >> 6) & 1) == (((z >> 6) & 1))) {
            return Blocks.AIR.defaultBlockState();
        }
        int ay = Math.abs(y);
        RandomSource random = RANDOM_FACTORY.at(x, y, z);
        return getLayer(ay + random.nextInt(7) - 3).getState();
    }

    public void fillColumn(int x, int z, BlockState[] states) {
        for (int i = 0; i < states.length; i++) {
            states[i] = getState(x, i - HEIGHT / 2, z);
        }
    }

    private int getOrder() {
        return -startsAt;
    }
}