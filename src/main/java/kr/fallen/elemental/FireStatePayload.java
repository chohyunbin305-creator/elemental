package kr.fallen.elemental;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FireStatePayload(boolean active) implements CustomPayload {
    public static final Id<FireStatePayload> ID =
            new Id<>(Identifier.of(ElementalEyes.MODID, "fire_state"));
    public static final PacketCodec<RegistryByteBuf, FireStatePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> buffer.writeBoolean(payload.active()),
            buffer -> new FireStatePayload(buffer.readBoolean()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
