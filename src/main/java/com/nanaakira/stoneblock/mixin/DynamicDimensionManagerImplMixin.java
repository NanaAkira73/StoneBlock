package com.nanaakira.stoneblock.mixin;

import com.nanaakira.stoneblock.worldgen.StoneBlockDataKjs;
import dev.ftb.mods.ftbteambases.worldgen.chunkgen.ChunkGenerators;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects FTB Team Bases' dynamic private-dimension generator to use the
 * StoneBlock chunk generator, so each team's private base dimension becomes an
 * independent stone ring world centred on their base.
 */
@Mixin(targets = "dev.ftb.mods.ftbteambases.util.forge.DynamicDimensionManagerImpl", remap = false)
public abstract class DynamicDimensionManagerImplMixin {

    @Redirect(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbteambases/worldgen/chunkgen/ChunkGenerators;makeGenerator(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/world/level/chunk/ChunkGenerator;"
            ),
            remap = false
    )
    private static ChunkGenerator stoneblock$redirectMakeGenerator(ChunkGenerators instance, RegistryAccess registryAccess, ResourceLocation prebuiltStructureId) {
        return StoneBlockDataKjs.createChunkGenerator(registryAccess);
    }
}
