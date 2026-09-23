package kr.fallen.elemental.mixin;

import kr.fallen.elemental.Characters;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class CharacterLivingMixin {
    @Inject(method="takeKnockback",at=@At("HEAD"),cancellable=true)
    private void elemental$rapidNoKnockback(double strength,double x,double z,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if(kr.fallen.elemental.Archer.RAPID_HIT.get())ci.cancel();
    }
    @ModifyVariable(method="damage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float elemental$resistances(float amount, DamageSource source, float original) {
        return Characters.incoming((LivingEntity)(Object)this,source,amount);
    }
    @Inject(method="damage",at=@At("HEAD"),cancellable=true)
    private void elemental$noDrowning(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir) {
        if(source.isOf(DamageTypes.DROWN) && Characters.android((LivingEntity)(Object)this))cir.setReturnValue(false);
    }
}
