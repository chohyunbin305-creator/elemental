package kr.fallen.elemental.mixin;

import kr.fallen.elemental.ToadSage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ToadSage.Oiled {
    @Unique private int elemental$oil;
    @Unique private int elemental$burn;
    @Unique private boolean elemental$bypassGuard;

    @Inject(method = "tick", at = @At("TAIL"))
    private void elemental$tick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ToadSage.statusTick(entity, this);
        if (elemental$oil > 0) elemental$oil--;
        if (elemental$burn > 0) elemental$burn--;
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void elemental$oilBypassesFireResistance(DamageSource source, float amount,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (elemental$bypassGuard || elemental$oil <= 0 || !source.isIn(DamageTypeTags.IS_FIRE)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) return;

        // Oil makes external fire mechanics (lava, fireballs, fire, other FIRE attacks)
        // get through Fire Resistance. This does NOT start the special Toad Sage DOT.
        elemental$bypassGuard = true;
        boolean result = self.damage(self.getDamageSources().magic(), amount);
        elemental$bypassGuard = false;
        cir.setReturnValue(result);
    }

    @Override public boolean elemental$isOiled() { return elemental$oil > 0; }
    @Override public void elemental$oil(int ticks) { elemental$oil = ticks; }
    @Override public int elemental$oilTicks() { return elemental$oil; }
    @Override public void elemental$burn(int ticks) { elemental$burn = ticks; }
    @Override public int elemental$burnTicks() { return elemental$burn; }
}
