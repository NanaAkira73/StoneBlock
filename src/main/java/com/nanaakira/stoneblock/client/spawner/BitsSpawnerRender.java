package com.nanaakira.stoneblock.client.spawner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nanaakira.stoneblock.content.spawner.BitsSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * bits_spawner 的方块实体渲染（从 FTB StoneBlock Companion 移植）。
 * 显示一个旋转的凋零作为刷怪笼的"预览实体"。
 */
public class BitsSpawnerRender implements BlockEntityRenderer<BitsSpawnerBlockEntity> {
    public BitsSpawnerRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BitsSpawnerBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        Entity entity = blockEntity.getOrCreateDisplayEntity();
        if (entity != null) {
            float g = 0.53125f;
            float h = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if ((double) h > 1.0) {
                g /= h;
            }
            poseStack.translate(0.0, 0.4f, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) Mth.lerp(partialTicks, blockEntity.oSpin, blockEntity.spin) * 10.0f));
            poseStack.translate(0.0, -0.2f, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0f));
            poseStack.scale(g, g, g);
            Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, 0.0f, partialTicks, poseStack, buffer, packedLight);
        }
        poseStack.popPose();
    }
}
