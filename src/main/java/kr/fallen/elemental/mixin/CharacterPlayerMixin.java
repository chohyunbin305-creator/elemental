package kr.fallen.elemental.mixin;
import kr.fallen.elemental.Characters;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PlayerEntity.class)
public abstract class CharacterPlayerMixin {
    @Inject(method="getHurtSound",at=@At("HEAD"),cancellable=true)
    private void elemental$metalHurt(DamageSource source,CallbackInfoReturnable<SoundEvent> cir) {
        if(Characters.android((PlayerEntity)(Object)this))cir.setReturnValue(SoundEvents.ENTITY_IRON_GOLEM_HURT);
    }
}
