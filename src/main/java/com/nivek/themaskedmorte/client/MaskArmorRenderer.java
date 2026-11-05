package com.nivek.themaskedmorte.client;

import com.nivek.themaskedmorte.TheMaskedMorte;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Modelo 3D da mascara: placa facial, bochechas, sobrancelha, nariz e queixo. */
public class MaskArmorRenderer implements ArmorRenderer {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of(TheMaskedMorte.MOD_ID, "mask"), "main");
    private static final Identifier TEXTURE =
            Identifier.of(TheMaskedMorte.MOD_ID, "textures/entity/mask.png");

    private ModelPart part;

    public static TexturedModelData createModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        // placa frontal 9x9x1
        root.addChild("plate", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.5f, -8.5f, -5.0f, 9.0f, 9.0f, 1.0f), ModelTransform.NONE);
        // laterais (bochechas) 1x9x4
        root.addChild("side_l", ModelPartBuilder.create().uv(0, 10)
                .cuboid(4.0f, -8.5f, -5.0f, 1.0f, 9.0f, 4.0f), ModelTransform.NONE);
        root.addChild("side_r", ModelPartBuilder.create().uv(10, 10)
                .cuboid(-5.0f, -8.5f, -5.0f, 1.0f, 9.0f, 4.0f), ModelTransform.NONE);
        // sobrancelha saliente
        root.addChild("brow", ModelPartBuilder.create().uv(20, 0)
                .cuboid(-4.5f, -6.4f, -6.0f, 9.0f, 1.0f, 2.0f), ModelTransform.NONE);
        // nariz
        root.addChild("nose", ModelPartBuilder.create().uv(42, 0)
                .cuboid(-1.0f, -4.5f, -6.0f, 2.0f, 4.0f, 1.0f), ModelTransform.NONE);
        // queixo
        root.addChild("chin", ModelPartBuilder.create().uv(20, 3)
                .cuboid(-3.5f, 0.0f, -5.0f, 7.0f, 2.0f, 1.0f), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 32);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        if (part == null) {
            part = MinecraftClient.getInstance().getEntityModelLoader().getModelPart(LAYER);
        }
        matrices.push();
        contextModel.head.rotate(matrices);
        part.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)),
                light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
