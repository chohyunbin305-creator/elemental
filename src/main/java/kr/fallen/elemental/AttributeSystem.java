package kr.fallen.elemental;

import net.minecraft.entity.LivingEntity;

public final class AttributeSystem {
    private AttributeSystem() {}

    public static void hit(LivingEntity attacker, LivingEntity target, Element element) {
        // Global attribute reactions are intentionally not hardcoded here.
        // Character-specific FIRE/oil behavior is handled by ToadSage and the damage mixin.
    }
}
