package kr.fallen.elemental.mixin;

import kr.fallen.elemental.Archer;
import net.minecraft.entity.damage.DamageSource;
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
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void elemental$cancelRapidKnockback(LivingEntity target, DamageSource source,
                                                 CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        if (self.getCommandTags().contains("elemental_rapid_arrow")) {
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "onEntityHit", at = @At(value="INVOKE", target="Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean elemental$countArcherHit(net.minecraft.entity.Entity target, DamageSource source, float amount) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        boolean previous=Archer.RAPID_HIT.get();
        Archer.RAPID_HIT.set(self.getCommandTags().contains("elemental_rapid_arrow"));
        boolean successful;
        try { successful=target.damage(source,amount); }
        finally { Archer.RAPID_HIT.set(previous); }
        if (successful && target instanceof LivingEntity
                && self.getCommandTags().contains("elemental_archer_arrow")
                && !self.getCommandTags().contains("elemental_rapid_arrow")
                && self.getOwner() instanceof ServerPlayerEntity player && target!=player) {
            Archer.onArrowHit(player);
        }
        return successful;
    }
}
