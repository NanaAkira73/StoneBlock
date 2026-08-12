package com.nanaakira.stoneblock.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
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
 * Structure piece for dungeon structures in StoneBlock dimensions.
 */
public class DungeonStructurePiece extends TemplateStructurePiece {
    public static final BlockIgnoreProcessor IGNORE_PROCESSOR = new BlockIgnoreProcessor(
            ImmutableList.of(Blocks.STRUCTURE_VOID, Blocks.STRUCTURE_BLOCK));

    public final String structureInstanceId;

    public DungeonStructurePiece(StructureTemplateManager structureManager, ResourceLocation id,
                                  BlockPos pos, Rotation rotation, StructureTemplate template, String sid) {
        super(WorldGenRegistry.DUNGEON_STRUCTURE_PIECE.get(), 0, structureManager,
                id, id.toString(), makeSettings(template, rotation), pos);
        structureInstanceId = sid;
    }

    public DungeonStructurePiece(StructureTemplateManager structureManager, CompoundTag tag) {
        super(WorldGenRegistry.DUNGEON_STRUCTURE_PIECE.get(), tag, structureManager,
                id -> makeSettings(structureManager.getOrCreate(id), Rotation.valueOf(tag.getString("Rot"))));
        structureInstanceId = tag.getString("SBStructure");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
        tag.putString("SBStructure", structureInstanceId);
    }

    @Override
    protected void handleDataMarker(String label, BlockPos pos, ServerLevelAccessor level,
                                     RandomSource random, BoundingBox boundingBox) {
    }

    private static StructurePlaceSettings makeSettings(StructureTemplate template, Rotation rotation) {
        Vec3i size = template.getSize();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setIgnoreEntities(true);
        settings.addProcessor(IGNORE_PROCESSOR);
        settings.setRotationPivot(new BlockPos(size.getX() / 2, size.getY() / 2, size.getZ() / 2));
        settings.setRotation(rotation);
        return settings;
    }
}