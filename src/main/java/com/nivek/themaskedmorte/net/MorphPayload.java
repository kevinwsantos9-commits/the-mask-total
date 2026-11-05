package com.nivek.themaskedmorte.net;

import com.nivek.themaskedmorte.TheMaskedMorte;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

/** type vazio = voltar ao normal. animate = toca a animacao dos clones antes de aplicar. */
public record MorphPayload(UUID player, String type, boolean animate) implements CustomPayload {
    public static final CustomPayload.Id<MorphPayload> ID =
            new CustomPayload.Id<>(Identifier.of(TheMaskedMorte.MOD_ID, "morph"));

    public static final PacketCodec<ByteBuf, MorphPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, MorphPayload::player,
            PacketCodecs.STRING, MorphPayload::type,
            PacketCodecs.BOOL, MorphPayload::animate,
            MorphPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
