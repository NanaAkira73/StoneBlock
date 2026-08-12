package com.nanaakira.stoneblock.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

/**
 * Dungeon structure that generates in StoneBlock dimensions.
 */
public class DungeonStructureFeature extends Structure {
    public static final Codec<DungeonStructureFeature> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 100).fieldOf("size").forGetter(s -> s.maxDepth),
            Codec.intRange(-300, 300).fieldOf("min_height").forGetter(s -> s.minHeight),
            Codec.intRange(-300, 300).fieldOf("max_height").forGetter(s -> s.maxHeight),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("min_structure_distance").forGetter(s -> s.minStructureDistance),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max_structure_distance").forGetter(s -> s.maxStructureDistance)
    ).apply(instance, DungeonStructureFeature::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    private final int minHeight;
    private final int maxHeight;
    private final int minStructureDistance;
    private final int maxStructureDistance;

    public DungeonStructureFeature(StructureSettings settings, Holder<StructureTemplatePool> startPool,
                                    int maxDepth, int minHeight, int maxHeight,
                                    int minStructureDistance, int maxStructureDistance) {
        super(settings);
        this.startPool = startPool;
        this.maxDepth = maxDepth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minStructureDistance = minStructureDistance;
        this.maxStructureDistance = maxStructureDistance;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMinBlockX();
        int z = context.chunkPos().getMinBlockZ();

        double distance = circularDistance(BlockPos.ZERO, new Vec3i(x, 0, z));

        if (distance < minStructureDistance || distance > maxStructureDistance) {
            return Optional.empty();
        }

        WorldgenRandom random = context.random();
        int y = Mth.clamp(
                (int) Mth.randomBetween(random, context.heightAccessor().getMinBuildHeight(),
                        context.heightAccessor().getMaxBuildHeight()),
                minHeight, maxHeight
        );
        BlockPos blockPos = new BlockPos(context.chunkPos().getMiddleBlockX(), y,
                context.chunkPos().getMiddleBlockZ());

        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                Optional.empty(),
                this.maxDepth,
                blockPos,
                false,
                Optional.empty(),
                128
        );
    }

    @Override
    public StructureType<?> type() {
        return WorldGenRegistry.DUNGEON_STRUCTURE_TYPE.get();
    }

    public static double circularDistance(BlockPos from, Vec3i to) {
        float dx = to.getX() - from.getX();
        float dy = to.getY() - from.getY();
        float dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}