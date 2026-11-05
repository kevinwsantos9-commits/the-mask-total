package com.nivek.themaskedmorte.client;

import com.nivek.themaskedmorte.net.MorphPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Estado do morph no cliente: forma atual, animacao dos clones e renderizacao. */
public final class MorphClient {
    public static final int DURATION = 16;      // ticks da animacao (0.8s)
    private static final double MAX_DIST = 2.4; // distancia maxima dos clones

    private record Anim(int start, String target) {}

    private static final Map<UUID, String> MORPHS = new HashMap<>();
    private static final Map<UUID, Anim> ANIMS = new HashMap<>();
    private static final Map<UUID, LivingEntity> DUMMIES = new HashMap<>();
    private static int ticks = 0;
    private static boolean rendering = false;

    private MorphClient() {}

    public static void handle(MorphPayload p) {
        if (p.animate()) {
            ANIMS.put(p.player(), new Anim(ticks, p.type()));
        } else {
            apply(p.player(), p.type());
        }
    }

    public static void clear() {
        MORPHS.clear();
        ANIMS.clear();
        DUMMIES.clear();
    }

    private static void apply(UUID id, String type) {
        DUMMIES.remove(id);
        if (type.isEmpty()) MORPHS.remove(id);
        else MORPHS.put(id, type);
    }

    public static boolean isActive(UUID id) {
        return MORPHS.containsKey(id) || ANIMS.containsKey(id);
    }

    public static boolean isMorphed(UUID id) {
        return MORPHS.containsKey(id);
    }

    // ---------------------------------------------------------------- tick
    public static void tick(MinecraftClient mc) {
        ticks++;
        ClientWorld w = mc.world;
        if (w == null) return;

        Iterator<Map.Entry<UUID, Anim>> it = ANIMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Anim> e = it.next();
            PlayerEntity p = w.getPlayerByUuid(e.getKey());
            int age = ticks - e.getValue().start();
            if (p != null) {
                if (age == 0) burst(w, p, 12);
                animParticles(w, p, age);
            }
            if (age >= DURATION) {
                apply(e.getKey(), e.getValue().target());
                if (p != null) burst(w, p, 30);
                it.remove();
            }
        }

        for (Map.Entry<UUID, String> e : MORPHS.entrySet()) {
            PlayerEntity p = w.getPlayerByUuid(e.getKey());
            if (p == null) continue;
            LivingEntity d = dummy(p, e.getValue());
            if (d == null) continue;
            double dx = p.getX() - p.prevX;
            double dz = p.getZ() - p.prevZ;
            float speed = Math.min((float) Math.sqrt(dx * dx + dz * dz) * 4.0f, 1.0f);
            d.limbAnimator.updateLimbs(speed, 0.4f);
            d.age++;
        }
    }

    private static void burst(ClientWorld w, PlayerEntity p, int n) {
        for (int i = 0; i < n; i++) {
            double vx = (w.random.nextDouble() - 0.5) * 0.5;
            double vy = w.random.nextDouble() * 0.3;
            double vz = (w.random.nextDouble() - 0.5) * 0.5;
            w.addParticle(ParticleTypes.POOF, p.getX(), p.getY() + 0.9, p.getZ(), vx, vy, vz);
            if (i % 3 == 0) w.addParticle(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), vx * 2, vy, vz * 2);
        }
    }

    private static void animParticles(ClientWorld w, PlayerEntity p, int age) {
        double out = Math.sin(Math.min(age / (double) DURATION, 1.0) * Math.PI);
        double rad = Math.toRadians(p.bodyYaw);
        double rx = -Math.cos(rad), rz = -Math.sin(rad);
        for (int side = -1; side <= 1; side += 2) {
            double d = side * MAX_DIST * out;
            double x = p.getX() + rx * d, z = p.getZ() + rz * d;
            w.addParticle(ParticleTypes.CLOUD, x, p.getY() + 0.3, z, 0, 0.02, 0);
            w.addParticle(ParticleTypes.ELECTRIC_SPARK, x, p.getY() + 1.2, z, 0, 0, 0);
            w.addParticle(ParticleTypes.CLOUD, x, p.getY() + 1.0, z, -rx * side * 0.1, 0.0, -rz * side * 0.1);
        }
    }

    // ------------------------------------------------------------ render
    public static void renderAll(PlayerEntityRenderer r, AbstractClientPlayerEntity p, float yaw, float td,
                                 MatrixStack m, VertexConsumerProvider v, int light) {
        rendering = true;
        try {
            drawForm(r, p, MORPHS.get(p.getUuid()), yaw, td, m, v, light);

            Anim a = ANIMS.get(p.getUuid());
            if (a != null) {
                float prog = MathHelper.clamp((ticks - a.start() + td) / (float) DURATION, 0.0f, 1.0f);
                double out = Math.sin(prog * Math.PI);
                float by = MathHelper.lerpAngleDegrees(td, p.prevBodyYaw, p.bodyYaw);
                double rx = -Math.cos(Math.toRadians(by));
                double rz = -Math.sin(Math.toRadians(by));
                String current = MORPHS.get(p.getUuid());
                double[] trail = {1.0, 0.55};
                for (int side = -1; side <= 1; side += 2) {
                    for (double f : trail) {
                        double d = side * f * MAX_DIST * out;
                        m.push();
                        m.translate(rx * d, 0.0, rz * d);
                        drawForm(r, p, current, yaw, td, m, v, light);
                        m.pop();
                    }
                }
            }
        } finally {
            rendering = false;
        }
    }

    public static boolean isRendering() {
        return rendering;
    }

    private static void drawForm(PlayerEntityRenderer r, AbstractClientPlayerEntity p, String type, float yaw,
                                 float td, MatrixStack m, VertexConsumerProvider v, int light) {
        LivingEntity d = type == null ? null : dummy(p, type);
        if (d == null) {
            r.render(p, yaw, td, m, v, light);
            return;
        }
        copyState(p, d);
        MinecraftClient.getInstance().getEntityRenderDispatcher().render(d, 0.0, 0.0, 0.0, yaw, td, m, v, light);
    }

    private static LivingEntity dummy(PlayerEntity p, String type) {
        LivingEntity d = DUMMIES.get(p.getUuid());
        if (d != null && Registries.ENTITY_TYPE.getId(d.getType()).toString().equals(type)) return d;
        Identifier id = Identifier.tryParse(type);
        if (id == null) return null;
        Optional<EntityType<?>> t = Registries.ENTITY_TYPE.getOrEmpty(id);
        if (t.isEmpty()) return null;
        Entity e = t.get().create(p.getWorld());
        if (!(e instanceof LivingEntity le)) return null;
        DUMMIES.put(p.getUuid(), le);
        return le;
    }

    private static void copyState(PlayerEntity p, LivingEntity d) {
        d.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
        d.prevX = p.prevX;
        d.prevY = p.prevY;
        d.prevZ = p.prevZ;
        d.prevYaw = p.prevYaw;
        d.prevPitch = p.prevPitch;
        d.bodyYaw = p.bodyYaw;
        d.prevBodyYaw = p.prevBodyYaw;
        d.headYaw = p.headYaw;
        d.prevHeadYaw = p.prevHeadYaw;
        d.hurtTime = p.hurtTime;
        d.deathTime = p.deathTime;
        d.handSwinging = p.handSwinging;
        d.handSwingProgress = p.handSwingProgress;
        d.lastHandSwingProgress = p.lastHandSwingProgress;
        d.setSneaking(p.isSneaking());
        d.setInvisible(p.isInvisible());
        d.setCustomName(p.getName());
        d.setCustomNameVisible(true);
    }
}
