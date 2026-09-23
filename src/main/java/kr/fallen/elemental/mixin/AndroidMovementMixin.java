package kr.fallen.elemental.mixin;
import kr.fallen.elemental.Characters;
import net.minecraft.entity.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class AndroidMovementMixin {
    @Inject(method="setSwimming",at=@At("HEAD"),cancellable=true)
    private void elemental$noSwim(boolean swimming,CallbackInfo ci) {
        if(swimming && Characters.android((Entity)(Object)this))ci.cancel();
    }
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void elemental$launcherLock(MovementType type,Vec3d movement,CallbackInfo ci) {
        if((Object)this instanceof ServerPlayerEntity p && Characters.frozen(p))ci.cancel();
    }
}
