package com.nanaakira.stoneblock.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Structure piece for the starting island in StoneBlock dimensions.
 */
public class StartStructurePiece extends TemplateStructurePiece {
    public static final BlockIgnoreProcessor IGNORE_PROCESSOR = new BlockIgnoreProcessor(
            ImmutableList.of(Blocks.STRUCTURE_VOID, Blocks.STRUCTURE_BLOCK));

    public final ResourceLocation startId;

    public StartStructurePiece(StructureTemplateManager structureManager, ResourceLocation id,
                                BlockPos pos, StructureTemplate template) {
        super(WorldGenRegistry.START_STRUCTURE_PIECE.get(), 0, structureManager,
                id, id.toString(), makeSettings(template), pos);
        startId = id;
    }

    @SuppressWarnings("deprecation")
    public StartStructurePiece(StructureTemplateManager structureManager, CompoundTag tag) {
        super(WorldGenRegistry.START_STRUCTURE_PIECE.get(), tag, structureManager,
                id -> makeSettings(structureManager.getOrCreate(id)));
        startId = new ResourceLocation(tag.getString("Template"));
    }

    public static StructurePlaceSettings makeSettings(StructureTemplate template) {
        Vec3i size = template.getSize();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setIgnoreEntities(true);
        settings.addProcessor(IGNORE_PROCESSOR);
        settings.setRotationPivot(new BlockPos(size.getX() / 2, size.getY() / 2, size.getZ() / 2));
        settings.setRotation(Rotation.NONE);
        return settings;
    }

    @Override
    protected void handleDataMarker(String label, BlockPos pos, ServerLevelAccessor level,
                                     RandomSource random, BoundingBox boundingBox) {
        if (label.equalsIgnoreCase("spawn_point")) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.getLevel().getServer().getGameRules().getRule(
                    net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS).set(0, level.getLevel().getServer());
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
    }
}