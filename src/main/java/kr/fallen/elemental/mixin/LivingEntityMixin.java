package kr.fallen.elemental.mixin;

import kr.fallen.elemental.ToadSage;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ToadSage.Oiled {
    @Unique private int elemental$oil;
    @Unique private int elemental$burn;

    @Inject(method = "tick", at = @At("TAIL"))
    private void elemental$tick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        ToadSage.statusTick(entity, this);

        if (elemental$oil > 0) elemental$oil--;
        if (elemental$burn > 0) elemental$burn--;
    }

    @Override public boolean elemental$isOiled() { return elemental$oil > 0; }
    @Override public void elemental$oil(int ticks) { elemental$oil = ticks; }
    @Override public int elemental$oilTicks() { return elemental$oil; }
    @Override public void elemental$burn(int ticks) { elemental$burn = ticks; }
    @Override public int elemental$burnTicks() { return elemental$burn; }
}
