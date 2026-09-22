package kr.fallen.elemental;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FireStatePayload(UUID playerId, boolean active) implements CustomPayload {
    public static final Id<FireStatePayload> ID =
            new Id<>(Identifier.of(ElementalEyes.MODID, "fire_state"));
    public static final PacketCodec<RegistryByteBuf, FireStatePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> {
                buffer.writeUuid(payload.playerId());
                buffer.writeBoolean(payload.active());
            },
            buffer -> new FireStatePayload(buffer.readUuid(), buffer.readBoolean()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
