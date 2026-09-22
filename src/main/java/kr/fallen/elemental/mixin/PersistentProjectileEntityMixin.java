package kr.fallen.elemental.mixin;

import kr.fallen.elemental.Archer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void elemental$countArcherHit(EntityHitResult hit, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (hit.getEntity() instanceof LivingEntity
                && self.getOwner() instanceof ServerPlayerEntity player) {
            Archer.onArrowHit(player);
        }
    }
}
