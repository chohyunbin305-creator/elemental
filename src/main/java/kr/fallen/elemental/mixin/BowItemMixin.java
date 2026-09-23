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
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void elemental$clickFire(World world, net.minecraft.entity.player.PlayerEntity user,
        net.minecraft.util.Hand hand, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.util.TypedActionResult<ItemStack>> cir) {
        if (!Archer.isRapidPlayer(user)) return;
        ItemStack stack=user.getStackInHand(hand);
        if(!Archer.canRapidFire(user) || user.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            cir.setReturnValue(net.minecraft.util.TypedActionResult.fail(stack)); return;
        }
        if(!user.isCreative() && user.getProjectileType(stack).isEmpty()) {
            cir.setReturnValue(net.minecraft.util.TypedActionResult.fail(stack)); return;
        }
        user.setCurrentHand(hand);
        ((net.minecraft.item.BowItem)(Object)this).onStoppedUsing(stack,world,user,stack.getMaxUseTime(user)-20);
        user.clearActiveItem();
        user.getItemCooldownManager().set(stack.getItem(),Archer.rapidFired(user));
        cir.setReturnValue(net.minecraft.util.TypedActionResult.success(stack,world.isClient));
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
