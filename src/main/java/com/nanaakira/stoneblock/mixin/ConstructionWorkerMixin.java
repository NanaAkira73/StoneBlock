package com.nanaakira.stoneblock.mixin;

import dev.ftb.mods.ftbteambases.data.construction.ConstructionWorker;
import dev.ftb.mods.ftblibrary.math.XZ;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConstructionWorker.class)
public interface ConstructionWorkerMixin {

    @Inject(method = "getSpawnXZ", at = @At("HEAD"), cancellable = true, remap = false)
    default void stoneblock$getSpawnXZ(CallbackInfoReturnable<XZ> cir) {
        cir.setReturnValue(XZ.of(0, 0));
    }
}
