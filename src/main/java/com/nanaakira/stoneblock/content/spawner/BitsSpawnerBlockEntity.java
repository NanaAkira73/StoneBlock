package com.nanaakira.stoneblock.content.spawner;

import com.nanaakira.stoneblock.content.ContentRegistry;
import com.nanaakira.stoneblock.integration.kubejs.SpawnerDataKjs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 生物群系自适应刷怪笼的方块实体（从 FTB StoneBlock Companion 移植）。
 * 刷怪表由 KubeJS 通过 SpawnerDataKjs 配置。
 */
public class BitsSpawnerBlockEntity extends BlockEntity {
    private static final EntityTypeTest<Entity, Mob> MOB_TEST = EntityTypeTest.forClass(Mob.class);

    private short spawnDelay = 20;
    private short minDelay = 50;
    private short maxDelay = 300;
    private short spawnRange = 5;

    public double spin;
    public double oSpin;
    private final int requiredPlayerRange = 16;
    private final Random random = new Random();

    private Entity displayEntity;

    public BitsSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ContentRegistry.BITS_SPAWNER_BLOCK_ENTITY.get(), pos, state);
    }

    private boolean isNearPlayer(Level level, BlockPos pos) {
        return level.hasNearbyAlivePlayer((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, this.requiredPlayerRange);
    }

    public void clientTick(Level level, BlockPos pos) {
        if (!this.isNearPlayer(level, pos)) {
            this.oSpin = this.spin;
        } else {
            double d0 = (double) pos.getX() + level.random.nextDouble();
            double d1 = (double) pos.getY() + level.random.nextDouble();
            double d2 = (double) pos.getZ() + level.random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.ENCHANT, d0, d1, d2, 0.0, 0.0, 0.0);
            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            }
            this.oSpin = this.spin;
            this.spin = (this.spin + (double) (1000.0f / ((float) this.spawnDelay + 200.0f))) % 360.0;
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (!this.isNearPlayer(level, pos)) {
            return;
        }

        if (this.spawnDelay > 0) {
            this.spawnDelay--;
            return;
        }

        if (level.getBrightness(LightLayer.BLOCK, pos) >= 13) {
            this.spawnDelay = delay();
            return;
        }

        for (int i = 0; i < Math.max(SpawnerDataKjs.minSpawnAmount, random.nextInt(SpawnerDataKjs.maxSpawnAmount)); i++) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            // 最多尝试 5 次寻找有效生成位置
            boolean validLocationFound = false;
            for (int a = 0; a < 5; a++) {
                x = pos.getX() + (random.nextInt(this.spawnRange) * (random.nextFloat() > 0.5 ? -1 : 1));
                y = pos.getY() + 1;
                z = pos.getZ() + (random.nextInt(this.spawnRange) * (random.nextFloat() > 0.5 ? -1 : 1));
                var blockPos = new BlockPos(x, y, z);

                if (level.getBlockState(blockPos).isAir() && level.getBlockState(blockPos.above()).isAir()) {
                    validLocationFound = true;
                    break;
                }
            }

            if (!validLocationFound) {
                continue;
            }

            CompoundTag compound = new CompoundTag();
            List<SpawnerDataKjs.SpawnableEntity> validMobsForBiome = getValidMobsForBiome();
            if (validMobsForBiome.isEmpty()) {
                this.spawnDelay = delay();
                return;
            }

            SpawnerDataKjs.SpawnableEntity entityId = validMobsForBiome.get(random.nextInt(validMobsForBiome.size()));

            compound.putString("id", entityId.entityId().toString());
            final int tmpX = x, tmpY = y, tmpZ = z;
            Entity entity = EntityType.loadEntityRecursive(compound, level, e -> {
                e.moveTo(tmpX, tmpY, tmpZ);
                return e;
            });

            if (entity == null) {
                this.spawnDelay = delay();
                return;
            }

            // 加 5 以考虑怪物游走范围
            AABB aabb = new AABB(new BlockPos(x, y, z)).inflate(this.spawnRange + 5);
            // 区域内已有 20 只就不刷
            if (level.getEntities(MOB_TEST, aabb, Entity::isAlive).size() >= 20) {
                this.spawnDelay = delay();
                return;
            }

            entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0f, 0.0f);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
            }

            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                this.spawnDelay = delay();
                return;
            }

            level.levelEvent(2004, pos, 0);
            if (entity instanceof Mob mob) {
                mob.spawnAnim();
            }
        }

        this.spawnDelay = this.delay();
    }

    private List<SpawnerDataKjs.SpawnableEntity> getValidMobsForBiome() {
        if (level == null) {
            return new ArrayList<>();
        }

        Holder<Biome> biome = level.getBiome(this.getBlockPos());
        return SpawnerDataKjs.entitiesToSpawn.stream()
                .filter(e -> e.allowedBiome() == null || biome.is(e.allowedBiome()))
                .toList();
    }

    private short delay() {
        return (short) (this.maxDelay <= this.minDelay ? this.minDelay : this.minDelay + this.random.nextInt(this.maxDelay - this.minDelay));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BitsSpawnerBlockEntity blockEntity) {
        blockEntity.clientTick(level, pos);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BitsSpawnerBlockEntity blockEntity) {
        blockEntity.serverTick((ServerLevel) level, pos);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.spawnDelay = tag.getShort("spawnDelay");
        this.minDelay = tag.getShort("minDelay");
        this.maxDelay = tag.getShort("maxDelay");
        this.spawnRange = tag.getShort("spawnRange");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putShort("spawnDelay", this.spawnDelay);
        tag.putShort("minDelay", this.minDelay);
        tag.putShort("maxDelay", this.maxDelay);
        tag.putShort("spawnRange", this.spawnRange);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    @Nullable
    public Entity getOrCreateDisplayEntity() {
        if (this.displayEntity == null) {
            this.displayEntity = EntityType.WITHER.create(this.level);
        }
        return this.displayEntity;
    }
}
