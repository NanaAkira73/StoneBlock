package com.nanaakira.stoneblock.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Structure that generates the starting island at chunk (0, 0) in StoneBlock dimensions.
 */
public class StartStructure extends Structure {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartStructure.class);

    public static final Codec<StartStructure> CODEC = simpleCodec(StartStructure::new);

    public StartStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMinBlockX();
        int z = context.chunkPos().getMinBlockZ();

        if (x != 0 || z != 0) {
            return Optional.empty();
        }

        if (!(context.chunkGenerator() instanceof StoneblockChunkGenerator stoneblockChunkGenerator)) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            StructureTemplateManager structureManager = context.structureTemplateManager();
            ChunkPos chunkPos = context.chunkPos();
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(context.seed(), chunkPos.x, chunkPos.z);

            PrebuiltStructure start = StoneBlockDataKjs.PREBUILT_STRUCTURES.get(
                    stoneblockChunkGenerator.prebuiltStructure.toString());
            if (start == null) {
                LOGGER.warn("Unable to find [{}] in the prebuilt structure list",
                        stoneblockChunkGenerator.prebuiltStructure);
                return;
            }

            StructureTemplate template = structureManager.getOrCreate(start.id);
            StructurePlaceSettings placeSettings = StartStructurePiece.makeSettings(template);
            BlockPos spawnPos = locateSpawn(template, placeSettings);

            int px = -spawnPos.getX();
            int py = -spawnPos.getY();
            int pz = -spawnPos.getZ();
            BlockPos blockPos = new BlockPos(px, py, pz);

            builder.addPiece(new StartStructurePiece(structureManager, start.id, blockPos, template));
        });
    }

    @Override
    public StructureType<?> type() {
        return WorldGenRegistry.START_STRUCTURE_TYPE.get();
    }

    public static BlockPos locateSpawn(StructureTemplate template, StructurePlaceSettings placeSettings) {
        BlockPos spawnPos = BlockPos.ZERO;
        for (var info : template.filterBlocks(BlockPos.ZERO, placeSettings, Blocks.STRUCTURE_BLOCK)) {
            if (info.nbt() != null && StructureMode.valueOf(info.nbt().getString("mode")) == StructureMode.DATA) {
                LOGGER.info("Found data block at [{}] with data [{}]", info.pos(), info.nbt().getString("metadata"));
                if (info.nbt().getString("metadata").equalsIgnoreCase("spawn_point")) {
                    spawnPos = info.pos();
                }
            }
        }
        return spawnPos;
    }
}