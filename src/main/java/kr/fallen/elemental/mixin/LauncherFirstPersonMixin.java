package kr.fallen.elemental.mixin;
import kr.fallen.elemental.client.CharacterClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class LauncherFirstPersonMixin {
    @Inject(method="renderFirstPersonItem",at=@At("HEAD"))
    private void elemental$guardPose(AbstractClientPlayerEntity player,float tickDelta,float pitch,Hand hand,
        float swing,ItemStack stack,float equip,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci) {
        matrices.push();
        if(hand==Hand.MAIN_HAND && CharacterClient.isLaunching(player.getUuid())) {
            int side=player.getMainArm()==Arm.RIGHT?1:-1;
            // Vanilla fully drawn bow transforms, without creating a bow item.
            matrices.translate(-side*.2785682,.18344387,.15731531);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side*35.3f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-side*9.785f));
            matrices.translate(0,0,.04);
            matrices.scale(1,1,1.2f);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side*45));
        }
    }
    @Inject(method="renderFirstPersonItem",at=@At("RETURN"))
    private void elemental$guardPoseEnd(AbstractClientPlayerEntity player,float tickDelta,float pitch,Hand hand,
        float swing,ItemStack stack,float equip,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci) {
        matrices.pop();
    }
}
