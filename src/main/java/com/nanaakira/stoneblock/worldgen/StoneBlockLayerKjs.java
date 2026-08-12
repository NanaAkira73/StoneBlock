package com.nanaakira.stoneblock.worldgen;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a layer configuration for StoneBlock world generation.
 * Format: "10x minecraft:stone" or "minecraft:stone"
 */
public class StoneBlockLayerKjs {
    public final String block;
    public final int size;
    private BlockState state;

    public StoneBlockLayerKjs(String layer) {
        String[] s = layer.split("x ", 2);
        if (s.length == 2) {
            block = s[1];
            size = Integer.parseInt(s[0]);
        } else {
            block = s[0];
            size = 1;
        }
    }

    @SuppressWarnings("deprecation")
    public BlockState getState() {
        if (state == null) {
            try {
                Block b = BuiltInRegistries.BLOCK.get(new ResourceLocation(block));
                state = b.defaultBlockState();
            } catch (Exception ex) {
                ex.printStackTrace();
                state = Blocks.RED_WOOL.defaultBlockState();
            }
        }
        return state;
    }
}