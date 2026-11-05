package com.nivek.themaskedmorte.mixin;

import com.nivek.themaskedmorte.net.MorphManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    // mob nao escolhe como alvo um jogador transformado na mesma especie dele
    @Inject(method = "setTarget(Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void tmm$sameSpecies(LivingEntity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity player
                && MorphManager.isSameSpecies(player.getUuid(), ((MobEntity) (Object) this).getType())) {
            ci.cancel();
        }
    }
}
