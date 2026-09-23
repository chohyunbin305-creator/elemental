package kr.fallen.elemental.mixin;
import kr.fallen.elemental.Characters;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class FoodSpeedMixin {
    @Inject(method="getMaxUseTime",at=@At("RETURN"),cancellable=true)
    private void elemental$fastFood(LivingEntity user,CallbackInfoReturnable<Integer> cir) {
        if(Characters.outlaw(user) && ((ItemStack)(Object)this).contains(DataComponentTypes.FOOD))
            cir.setReturnValue(Math.max(1,cir.getReturnValue()/2));
    }
}
