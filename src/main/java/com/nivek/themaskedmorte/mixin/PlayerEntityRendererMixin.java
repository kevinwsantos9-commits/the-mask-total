package com.nivek.themaskedmorte.mixin;

import com.nivek.themaskedmorte.client.MorphClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void tmm$render(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (MorphClient.isRendering()) return;
        if (!MorphClient.isActive(player.getUuid())) return;
        ci.cancel();
        MorphClient.renderAll((PlayerEntityRenderer) (Object) this, player, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    // esconde o braco do jogador em primeira pessoa quando ele e um mob
    @Inject(method = "renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void tmm$hideRightArm(MatrixStack matrices, VertexConsumerProvider vcp, int light,
                                  AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (MorphClient.isMorphed(player.getUuid())) ci.cancel();
    }

    @Inject(method = "renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void tmm$hideLeftArm(MatrixStack matrices, VertexConsumerProvider vcp, int light,
                                 AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (MorphClient.isMorphed(player.getUuid())) ci.cancel();
    }
}
