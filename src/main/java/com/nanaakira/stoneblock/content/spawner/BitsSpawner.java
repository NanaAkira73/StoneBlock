package com.nanaakira.stoneblock.content.spawner;

import com.nanaakira.stoneblock.content.ContentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 生物群系自适应刷怪笼（从 FTB StoneBlock Companion 的 BitsSpawner 移植）。
 * 与普通刷怪笼的区别：它不刷固定的一种怪，而是根据所在生物群系，
 * 从 KubeJS 配置的刷怪表（SpawnerDataKjs）中动态选择要刷的实体。
 */
public class BitsSpawner extends SpawnerBlock {
    public BitsSpawner() {
        super(Properties.copy(Blocks.SPAWNER).strength(50.0f, 1200.0f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BitsSpawnerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ContentRegistry.BITS_SPAWNER_BLOCK_ENTITY.get(),
                level.isClientSide ? BitsSpawnerBlockEntity::clientTick : BitsSpawnerBlockEntity::serverTick);
    }
}
