package com.nanaakira.stoneblock.content;

import com.nanaakira.stoneblock.StoneBlock;
import com.nanaakira.stoneblock.content.spawner.BitsSpawner;
import com.nanaakira.stoneblock.content.spawner.BitsSpawnerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * StoneBlock 方块/物品/方块实体注册表。
 * bits_spawner 是从 FTB StoneBlock Companion 移植的生物群系自适应刷怪笼。
 */
public class ContentRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StoneBlock.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StoneBlock.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StoneBlock.MODID);

    public static final RegistryObject<Block> BITS_SPAWNER = BLOCKS.register("bits_spawner", BitsSpawner::new);
    public static final RegistryObject<Item> BITS_SPAWNER_ITEM = ITEMS.register("bits_spawner",
            () -> new BlockItem(BITS_SPAWNER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BitsSpawnerBlockEntity>> BITS_SPAWNER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("bits_spawner",
                    () -> BlockEntityType.Builder.of(BitsSpawnerBlockEntity::new, BITS_SPAWNER.get()).build(null));

    public static final List<DeferredRegister<?>> REGISTERS = List.of(BLOCKS, ITEMS, BLOCK_ENTITIES);
}
