package kr.fallen.elemental;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BeamPayload(double x, double y, double z, double ex, double ey, double ez, int style) implements CustomPayload {
    public static final Id<BeamPayload> ID = new Id<>(Identifier.of(ElementalEyes.MODID, "beam"));
    public static final PacketCodec<RegistryByteBuf, BeamPayload> CODEC = PacketCodec.of(
        (p,b) -> { b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); b.writeDouble(p.ex);
            b.writeDouble(p.ey); b.writeDouble(p.ez); b.writeVarInt(p.style); },
        b -> new BeamPayload(b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt()));
    public Id<? extends CustomPayload> getId() { return ID; }
}
