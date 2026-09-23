package kr.fallen.elemental.mixin;
import kr.fallen.elemental.Characters;
import net.minecraft.item.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CrossbowItem.class)
public abstract class CrossbowSpeedMixin {
    @Inject(method="getPullTime",at=@At("RETURN"),cancellable=true)
    private static void elemental$fastReload(ItemStack stack,LivingEntity user,CallbackInfoReturnable<Integer> cir) {
        if(Characters.outlaw(user))cir.setReturnValue(Math.max(1,cir.getReturnValue()/3));
    }
}
