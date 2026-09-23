package kr.fallen.elemental;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ControlPayload(boolean primary, boolean secondary) implements CustomPayload {
    public static final Id<ControlPayload> ID = new Id<>(Identifier.of(ElementalEyes.MODID, "controls"));
    public static final PacketCodec<RegistryByteBuf, ControlPayload> CODEC = PacketCodec.of(
        (p,b) -> { b.writeBoolean(p.primary); b.writeBoolean(p.secondary); },
        b -> new ControlPayload(b.readBoolean(), b.readBoolean()));
    public Id<? extends CustomPayload> getId() { return ID; }
}
