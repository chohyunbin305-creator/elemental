package kr.fallen.elemental.mixin;

import kr.fallen.elemental.Archer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.item.BowItem.class)
public abstract class BowItemMixin {
    @ModifyVariable(method = "onStoppedUsing", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int elemental$rapidFullCharge(int remainingUseTicks, ItemStack stack,
                                          World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity player && Archer.isRapid(player)) {
            int max = stack.getMaxUseTime(user);
            int used = max - remainingUseTicks;
            if (used >= 2) return max - 20;
        }
        return remainingUseTicks;
    }

    @Inject(method = "shoot", at = @At("TAIL"))
    private void elemental$modifyArrow(LivingEntity shooter, ProjectileEntity projectile,
                                       int index, float speed, float divergence, float yaw,
                                       LivingEntity target, CallbackInfo ci) {
        if (!(shooter instanceof ServerPlayerEntity player)
                || !(projectile instanceof PersistentProjectileEntity arrow)
                || !Archer.isArcher(player)) return;

        arrow.addCommandTag("elemental_archer_arrow");
        if (Archer.isRapid(player)) {
            arrow.setDamage(arrow.getDamage() * Archer.RAPID_DAMAGE_MULTIPLIER);
            arrow.addCommandTag("elemental_rapid_arrow");
        } else {
            arrow.setDamage(arrow.getDamage() * Archer.FOCUS_DAMAGE_MULTIPLIER);
        }
    }
}
