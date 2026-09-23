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
            matrices.translate(-side*.25,.18,-.12);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side*20));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side*55));
        }
    }
    @Inject(method="renderFirstPersonItem",at=@At("RETURN"))
    private void elemental$guardPoseEnd(AbstractClientPlayerEntity player,float tickDelta,float pitch,Hand hand,
        float swing,ItemStack stack,float equip,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci) {
        matrices.pop();
    }
}
