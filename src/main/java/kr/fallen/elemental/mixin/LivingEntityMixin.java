package kr.fallen.elemental.mixin;
import kr.fallen.elemental.ToadSage;
import net.minecraft.entity.*;import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ToadSage.Oiled {
 @Unique private int elemental$oil,elemental$burn;
 @Inject(method="tick",at=@At("TAIL"))private void elemental$tick(CallbackInfo ci){LivingEntity e=(LivingEntity)(Object)this;if(elemental$oil>0)elemental$oil--;if(elemental$burn>0){ToadSage.burnTick(e,this);elemental$burn--;}}
 public boolean elemental$isOiled(){return elemental$oil>0;}public void elemental$oil(int t){elemental$oil=t;}public int elemental$oilTicks(){return elemental$oil;}public void elemental$burn(int t){elemental$burn=t;}public int elemental$burnTicks(){return elemental$burn;}
}
