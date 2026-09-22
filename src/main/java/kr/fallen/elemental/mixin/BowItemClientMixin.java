package kr.fallen.elemental.mixin;

import kr.fallen.elemental.client.ElementalEyesClient;
import net.minecraft.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public abstract class BowItemClientMixin {
    @Inject(method = "getPullProgress", at = @At("HEAD"), cancellable = true)
    private static void elemental$rapidPullVisual(int useTicks, CallbackInfoReturnable<Float> cir) {
        if (ElementalEyesClient.isArcherRapid() && useTicks >= 2) cir.setReturnValue(1.0f);
    }
}
