package kr.fallen.elemental.mixin;
import kr.fallen.elemental.Characters;
import net.minecraft.entity.passive.AbstractHorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractHorseEntity.class)
public abstract class OutlawHorseMixin {
    @Inject(method={"isSaddled","isTame"},at=@At("HEAD"),cancellable=true)
    private void elemental$outlawRider(CallbackInfoReturnable<Boolean> cir) {
        if(Characters.outlaw(((AbstractHorseEntity)(Object)this).getFirstPassenger()))cir.setReturnValue(true);
    }
}
