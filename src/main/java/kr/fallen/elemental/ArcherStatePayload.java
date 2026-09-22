package kr.fallen.elemental;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ArcherStatePayload(boolean enabled, boolean rapid, int charges, int hitProgress)
        implements CustomPayload {
    public static final Id<ArcherStatePayload> ID =
            new Id<>(Identifier.of(ElementalEyes.MODID, "archer_state"));
    public static final PacketCodec<RegistryByteBuf, ArcherStatePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> {
                buffer.writeBoolean(payload.enabled());
                buffer.writeBoolean(payload.rapid());
                buffer.writeVarInt(payload.charges());
                buffer.writeVarInt(payload.hitProgress());
            },
            buffer -> new ArcherStatePayload(
                    buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
