package kr.fallen.elemental.mixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The launcher stays FIRE damage, but penetrates innate mob fire immunity.
 * Fire Resistance, creative immunity and scripted invulnerability still apply. */
@Mixin(Entity.class)
public abstract class LauncherFireImmunityMixin {
    @Redirect(method="isInvulnerableTo",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;isFireImmune()Z"))
    private boolean elemental$launcherPenetration(Entity self,DamageSource source) {
        if(source.isOf(RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of("elemental_eyes","launcher"))))return false;
        return self.isFireImmune();
    }
}
