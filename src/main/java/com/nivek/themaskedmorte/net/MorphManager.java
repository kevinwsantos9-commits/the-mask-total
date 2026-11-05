package com.nivek.themaskedmorte.net;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nivek.themaskedmorte.ModItems;
import com.nivek.themaskedmorte.MaskItem;
import com.nivek.themaskedmorte.TheMaskedMorte;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class MorphManager {
    private static final Map<UUID, String> MORPHS = new HashMap<>();
    private static final Identifier HEALTH_ID = Identifier.of(TheMaskedMorte.MOD_ID, "morph_health");

    private MorphManager() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(MorphPayload.ID, MorphPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            for (Map.Entry<UUID, String> e : MORPHS.entrySet()) {
                ServerPlayNetworking.send(handler.player, new MorphPayload(e.getKey(), e.getValue(), false));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (MORPHS.remove(handler.player.getUuid()) != null) applyHealth(handler.player, 0.0f);
        });

        // Tirou a mascara da cabeca -> desfaz o morph
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0 || MORPHS.isEmpty()) return;
            for (UUID id : new ArrayList<>(MORPHS.keySet())) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                if (p == null) {
                    MORPHS.remove(id);
                } else if (!hasMask(p)) {
                    reset(p);
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("morph")
                    .executes(ctx -> morphNearest(ctx.getSource()))
                    .then(CommandManager.literal("reset").executes(ctx -> resetCmd(ctx.getSource())))
                    .then(CommandManager.argument("mob", IdentifierArgumentType.identifier())
                            .suggests((c, b) -> CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), b))
                            .executes(ctx -> morphType(ctx.getSource(), IdentifierArgumentType.getIdentifier(ctx, "mob")))));
            dispatcher.register(CommandManager.literal("unmorph").executes(ctx -> resetCmd(ctx.getSource())));
        });
    }


    /** true se o jogador esta transformado exatamente nesse tipo de mob (usado pelo mixin de alvo). */
    public static boolean isSameSpecies(UUID id, EntityType<?> type) {
        String cur = MORPHS.get(id);
        return cur != null && cur.equals(Registries.ENTITY_TYPE.getId(type).toString());
    }

    /** mobMax <= 0 remove o ajuste (vida normal). Mantem a mesma proporcao de vida. */
    private static void applyHealth(ServerPlayerEntity p, float mobMax) {
        EntityAttributeInstance inst = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (inst == null) return;
        if (!p.isAlive()) {
            inst.removeModifier(HEALTH_ID);
            return;
        }
        float oldMax = p.getMaxHealth();
        float ratio = oldMax > 0 ? p.getHealth() / oldMax : 1.0f;
        inst.removeModifier(HEALTH_ID);
        if (mobMax > 0) {
            inst.addTemporaryModifier(new EntityAttributeModifier(
                    HEALTH_ID, mobMax - inst.getBaseValue(), EntityAttributeModifier.Operation.ADD_VALUE));
        }
        p.setHealth(Math.max(1.0f, ratio * p.getMaxHealth()));
    }

    /** mobs da mesma especie que ja estavam te perseguindo largam o alvo. */
    private static void clearAggro(ServerPlayerEntity p, EntityType<?> type) {
        for (MobEntity m : p.getServerWorld().getEntitiesByClass(MobEntity.class,
                p.getBoundingBox().expand(64.0), mob -> mob.getType() == type && mob.getTarget() == p)) {
            m.setTarget(null);
        }
    }

    private static boolean hasMask(ServerPlayerEntity p) {
        return p.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof MaskItem;
    }

    private static int morphNearest(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        if (!hasMask(p)) {
            src.sendError(Text.literal("Você precisa estar usando a Máscara do Masked Morte no rosto."));
            return 0;
        }
        List<LivingEntity> list = p.getServerWorld().getEntitiesByClass(LivingEntity.class,
                p.getBoundingBox().expand(24.0),
                e -> e != p && e.isAlive() && !(e instanceof PlayerEntity) && !(e instanceof ArmorStandEntity));
        if (list.isEmpty()) {
            src.sendError(Text.literal("Nenhum mob por perto (raio de 24 blocos)."));
            return 0;
        }
        list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        return morphTo(src, p, list.get(0).getType());
    }

    private static int morphType(ServerCommandSource src, Identifier id) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        if (!hasMask(p)) {
            src.sendError(Text.literal("Você precisa estar usando a Máscara do Masked Morte no rosto."));
            return 0;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
        if (type == null) {
            src.sendError(Text.literal("Mob desconhecido: " + id));
            return 0;
        }
        Entity test = type.create(p.getServerWorld());
        if (!(test instanceof LivingEntity) || test instanceof PlayerEntity || test instanceof ArmorStandEntity) {
            src.sendError(Text.literal("Você não pode virar isso. Escolha um mob vivo."));
            return 0;
        }
        return morphTo(src, p, type);
    }

    private static int morphTo(ServerCommandSource src, ServerPlayerEntity p, EntityType<?> type) {
        String id = Registries.ENTITY_TYPE.getId(type).toString();
        if (id.equals(MORPHS.get(p.getUuid()))) {
            src.sendError(Text.literal("Você já é esse mob."));
            return 0;
        }
        MORPHS.put(p.getUuid(), id);
        Entity probe = type.create(p.getServerWorld());
        applyHealth(p, probe instanceof LivingEntity le ? le.getMaxHealth() : 0.0f);
        clearAggro(p, type);
        effects(p);
        broadcast(p.getServer(), p.getUuid(), id, true);
        src.sendFeedback(() -> Text.literal("Você se transformou em ").append(type.getName()).append("!"), false);
        return 1;
    }

    private static int resetCmd(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        if (!MORPHS.containsKey(p.getUuid())) {
            src.sendError(Text.literal("Você não está transformado."));
            return 0;
        }
        reset(p);
        src.sendFeedback(() -> Text.literal("Você voltou ao normal."), false);
        return 1;
    }

    private static void reset(ServerPlayerEntity p) {
        MORPHS.remove(p.getUuid());
        applyHealth(p, 0.0f);
        effects(p);
        broadcast(p.getServer(), p.getUuid(), "", true);
    }

    private static void effects(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.5f);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 1.6f);
    }

    private static void broadcast(MinecraftServer server, UUID id, String type, boolean animate) {
        MorphPayload payload = new MorphPayload(id, type, animate);
        for (ServerPlayerEntity sp : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(sp, payload);
        }
    }
}
