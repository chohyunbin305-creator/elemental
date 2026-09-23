package kr.fallen.elemental;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CharacterStatePayload(int kind, int gauge, boolean display, boolean firing,
                                   int mainCooldown, int subCooldown) implements CustomPayload {
    public static final Id<CharacterStatePayload> ID = new Id<>(Identifier.of(ElementalEyes.MODID, "character_state"));
    public static final PacketCodec<RegistryByteBuf, CharacterStatePayload> CODEC = PacketCodec.of(
        (p,b) -> { b.writeVarInt(p.kind); b.writeVarInt(p.gauge); b.writeBoolean(p.display);
            b.writeBoolean(p.firing); b.writeVarInt(p.mainCooldown); b.writeVarInt(p.subCooldown); },
        b -> new CharacterStatePayload(b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt()));
    public Id<? extends CustomPayload> getId() { return ID; }
}
