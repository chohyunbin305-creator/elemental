package kr.fallen.elemental.mixin;

import kr.fallen.elemental.client.ElementalEyesClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void elemental$flameBreathPose(
            AbstractClientPlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
        if (hand == Hand.MAIN_HAND && ElementalEyesClient.isFlameActive(player.getUuid())) {
            cir.setReturnValue(BipedEntityModel.ArmPose.TOOT_HORN);
        }
        if (hand == Hand.MAIN_HAND && kr.fallen.elemental.client.CharacterClient.isLaunching(player.getUuid())) {
            cir.setReturnValue(BipedEntityModel.ArmPose.BLOCK);
        }
    }
}
