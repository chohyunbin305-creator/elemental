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
public abstract class BowItemMixin extends net.minecraft.item.RangedWeaponItem {
    protected BowItemMixin(net.minecraft.item.Item.Settings settings) { super(settings); }
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
        // Load and shoot on the authoritative server only. Client prediction must
        // not consume ammunition or install a cooldown before the use packet.
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            java.util.List<ItemStack> projectiles = load(stack, user.getProjectileType(stack), user);
            if (projectiles.isEmpty()) {
                cir.setReturnValue(net.minecraft.util.TypedActionResult.fail(stack)); return;
            }
            shootAll(serverWorld, user, hand, stack, projectiles, 3.0f, 1.0f, true, null);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                net.minecraft.sound.SoundEvents.ENTITY_ARROW_SHOOT,
                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f,
                1.0f / (world.random.nextFloat() * .4f + 1.2f) + .5f);
            user.incrementStat(net.minecraft.stat.Stats.USED.getOrCreateStat((net.minecraft.item.BowItem)(Object)this));
            user.getItemCooldownManager().set(stack.getItem(), Archer.rapidFired(user));
        }
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
