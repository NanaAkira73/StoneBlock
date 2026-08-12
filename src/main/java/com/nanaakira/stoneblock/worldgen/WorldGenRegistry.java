package com.nanaakira.stoneblock.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Central registry for all world generation components.
 * Handles registration of biome sources, chunk generators, structures, and structure pieces.
 */
public class WorldGenRegistry {
    public static final String MODID = "stoneblock";

    // Biome source registry
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MODID);

    // Chunk generator registry
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    // Structure type registry
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);

    // Structure piece registry
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);

    // All registries in a list for easy registration
    public static final List<DeferredRegister<?>> REGISTERS = List.of(
            BIOME_SOURCES,
            CHUNK_GENERATORS,
            STRUCTURE_TYPES,
            STRUCTURE_PIECES
    );

    // === Biome Source ===
    public static final RegistryObject<Codec<StoneBlockBiomeSource>> STONE_BLOCK_BIOME_SOURCE =
            BIOME_SOURCES.register("stoneblock_biome_source", () -> StoneBlockBiomeSource.CODEC);

    // === Chunk Generator ===
    public static final RegistryObject<Codec<StoneblockChunkGenerator>> STONE_BLOCK_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("stoneblock_chunk_generator", () -> StoneblockChunkGenerator.CODEC);

    // === Structures ===
    public static final RegistryObject<StructureType<StartStructure>> START_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("start", () -> () -> StartStructure.CODEC);

    public static final RegistryObject<StructureType<DungeonStructureFeature>> DUNGEON_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("dungeon_structure_feature", () -> () -> DungeonStructureFeature.CODEC);

    // === Structure Pieces ===
    public static final RegistryObject<StructurePieceType> START_STRUCTURE_PIECE =
            STRUCTURE_PIECES.register("start",
                    () -> (ctx, tag) -> new StartStructurePiece(ctx.structureTemplateManager(), tag));

    public static final RegistryObject<StructurePieceType> DUNGEON_STRUCTURE_PIECE =
            STRUCTURE_PIECES.register("dungeon",
                    () -> (ctx, tag) -> new DungeonStructurePiece(ctx.structureTemplateManager(), tag));
}