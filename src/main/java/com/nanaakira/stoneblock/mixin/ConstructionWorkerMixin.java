package com.nanaakira.stoneblock.mixin;

import dev.ftb.mods.ftbteambases.data.construction.ConstructionWorker;
import dev.ftb.mods.ftblibrary.math.XZ;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 把 FTB Team Bases 的基地放置坐标从 region 中心 (255,255) 拉回世界原点 (0,0)，
 * 与原版 FTB StoneBlock 3 的基地/环形圆心坐标对齐。
 *
 * 原版 SB3 中基地出生在 (0,0)、环形石头世界圆心在 (0,0)、结构距离也相对 (0,0)。
 * 但 FTB Team Bases 的 getSpawnXZ() 硬编码返回 region 中心 (255,255)，
 * 导致三者偏移了约 360 格。这里覆盖为返回 (0,0) 让整个坐标体系回到原版。
 */
@Mixin(ConstructionWorker.class)
public interface ConstructionWorkerMixin {

    @Overwrite(remap = false)
    default XZ getSpawnXZ() {
        return XZ.of(0, 0);
    }
}
