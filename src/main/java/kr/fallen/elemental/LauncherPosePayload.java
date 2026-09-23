package kr.fallen.elemental;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public record LauncherPosePayload(UUID playerId,boolean active) implements CustomPayload {
    public static final Id<LauncherPosePayload> ID=new Id<>(Identifier.of(ElementalEyes.MODID,"launcher_pose"));
    public static final PacketCodec<RegistryByteBuf,LauncherPosePayload> CODEC=PacketCodec.of(
        (p,b)->{b.writeUuid(p.playerId);b.writeBoolean(p.active);},
        b->new LauncherPosePayload(b.readUuid(),b.readBoolean()));
    public Id<? extends CustomPayload> getId(){return ID;}
}
