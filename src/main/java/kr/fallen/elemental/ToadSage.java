package kr.fallen.elemental;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import org.joml.Vector3f;

import java.util.*;

public final class ToadSage {
    private ToadSage() {}

    public interface Oiled {
        boolean elemental$isOiled();
        void elemental$oil(int ticks);
        int elemental$oilTicks();
        void elemental$burn(int ticks);
        int elemental$burnTicks();
    }

    private static final Map<UUID, Integer> oilCd = new HashMap<>();
    private static final Map<UUID, Integer> fireCd = new HashMap<>();
    private static final Map<UUID, Integer> spray = new HashMap<>();

    private static final Identifier HEALTH = Identifier.of(ElementalEyes.MODID, "toad_health");
    private static final Identifier REACH = Identifier.of(ElementalEyes.MODID, "toad_reach");
    private static final Identifier JUMP = Identifier.of(ElementalEyes.MODID, "toad_jump");
    private static final Identifier FIRE_SLOW = Identifier.of(ElementalEyes.MODID, "fire_slow");

    private static final DustParticleEffect FIRE_ORANGE =
            new DustParticleEffect(new Vector3f(1.0f, 0.28f, 0.02f), 1.8f);
    private static final DustParticleEffect FIRE_YELLOW =
            new DustParticleEffect(new Vector3f(1.0f, 0.58f, 0.05f), 1.45f);
    private static final DustParticleEffect FIRE_RED =
            new DustParticleEffect(new Vector3f(0.9f, 0.08f, 0.01f), 1.6f);
    private static final DustParticleEffect OIL_BROWN =
            new DustParticleEffect(new Vector3f(0.38f, 0.20f, 0.07f), 1.25f);

    public static boolean isToad(ServerPlayerEntity player) {
        return OriginBridge.has(player, "elemental_eyes:toad_sage");
    }

    public static void skillOil(ServerPlayerEntity player) {
        if (!isToad(player) || oilCd.getOrDefault(player.getUuid(), 0) > 0) return;
        oilCd.put(player.getUuid(), 240);

        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1).normalize();

        // A short, messy cone of oil rather than a single thin line.
        for (int i = 2; i <= 22; i++) {
            double z = i * 0.52;
            double spread = 0.035 + z * 0.055;
            Vec3d q = start.add(dir.multiply(z));
            world.spawnParticles(OIL_BROWN, q.x, q.y, q.z, 3, spread, spread * 0.65, spread, 0.012);
            if ((i & 1) == 0) {
                world.spawnParticles(ParticleTypes.FALLING_HONEY, q.x, q.y, q.z, 2,
                        spread * 0.7, spread * 0.45, spread * 0.7, 0.02);
            }
        }

        LivingEntity target = target(player, 12.0, 1.25);
        if (target != null) {
            ((Oiled) target).elemental$oil(140);
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.9f, 0.72f);
            world.spawnParticles(OIL_BROWN, target.getX(), target.getBodyY(0.5), target.getZ(),
                    28, 0.55, 0.7, 0.55, 0.025);
            world.spawnParticles(ParticleTypes.FALLING_HONEY, target.getX(), target.getBodyY(0.55), target.getZ(),
                    16, 0.45, 0.65, 0.45, 0.035);
        }
    }

    public static void skillFire(ServerPlayerEntity player) {
        if (!isToad(player) || fireCd.getOrDefault(player.getUuid(), 0) > 0) return;
        fireCd.put(player.getUuid(), 160);
        spray.put(player.getUuid(), 20); // about 1 second
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 0.72f);
    }

    public static void tick(ServerPlayerEntity player) {
        dec(oilCd, player);
        dec(fireCd, player);

        if (!isToad(player)) {
            spray.remove(player.getUuid());
            remove(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH);
            remove(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, REACH);
            remove(player, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP);
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            return;
        }

        ensure(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, 4.0);
        ensure(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, REACH, 0.75);
        ensure(player, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP, 0.14);

        int ticks = spray.getOrDefault(player.getUuid(), 0);
        if (ticks > 0) {
            ensure(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW, -0.015);
            spray.put(player.getUuid(), ticks - 1);

            // Dense visual cloud every tick; damage every 4 ticks.
            firePulse(player, ticks % 4 == 0);
            if (ticks % 5 == 0) {
                player.getServerWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.45f, 0.78f);
            }

            if (ticks == 1) {
                remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW);
            }
        } else {
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW);
        }
    }

    private static void firePulse(ServerPlayerEntity player, boolean dealDamage) {
        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getEyePos().add(0, -0.10, 0);
        Vec3d dir = player.getRotationVec(1).normalize();

        // Naruto-style "whoosh": a thick expanding orange/red cloud.
        for (int i = 2; i <= 18; i++) {
            double z = i * 0.43;
            double spread = 0.10 + z * 0.22;
            Vec3d q = start.add(dir.multiply(z));

            world.spawnParticles(FIRE_ORANGE, q.x, q.y, q.z, 5,
                    spread, spread * 0.65, spread, 0.018);
            world.spawnParticles(FIRE_YELLOW, q.x, q.y, q.z, 3,
                    spread * 0.75, spread * 0.50, spread * 0.75, 0.014);
            world.spawnParticles(FIRE_RED, q.x, q.y, q.z, 2,
                    spread * 0.9, spread * 0.55, spread * 0.9, 0.012);

            if ((i & 1) == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, q.x, q.y, q.z, 2,
                        spread * 0.65, spread * 0.45, spread * 0.65, 0.018);
            }
        }

        if (!dealDamage) return;

        Box area = player.getBoundingBox()
                .stretch(dir.multiply(8.0))
                .expand(2.7);

        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e != player && e.isAlive() && !e.isSpectator())) {

            Vec3d to = target.getBoundingBox().getCenter().subtract(start);
            double forward = to.dotProduct(dir);
            if (forward < 0.2 || forward > 8.0) continue;

            Vec3d nearestOnAxis = dir.multiply(forward);
            double side = to.subtract(nearestOnAxis).length();
            double allowedRadius = 0.55 + forward * 0.25;
            if (side > allowedRadius) continue;
            if (!player.canSee(target)) continue;

            // Ordinary flame damage: armor applies, and Fire Resistance blocks it.
            if (!target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                target.damage(player.getDamageSources().playerAttack(player), 1.2f);
            }

            // Attribute reaction is separate, so oil can ignite from any FIRE-tagged skill.
            AttributeSystem.hit(player, target, Element.FIRE);
        }
    }

    public static void igniteOil(LivingEntity target) {
        Oiled oiled = (Oiled) target;
        if (!oiled.elemental$isOiled()) return;

        oiled.elemental$oil(0);
        oiled.elemental$burn(100); // refreshed by FIRE hits; remains 5 seconds after the last flame hit

        if (target.getWorld() instanceof ServerWorld world) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.8f, 0.85f);
            world.spawnParticles(FIRE_ORANGE, target.getX(), target.getBodyY(0.5), target.getZ(),
                    34, 0.65, 0.8, 0.65, 0.045);
            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(),
                    18, 0.55, 0.75, 0.55, 0.05);
        }
    }

    public static void statusTick(LivingEntity entity, Oiled state) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;

        int oil = state.elemental$oilTicks();
        if (oil > 0 && oil % 6 == 0) {
            world.spawnParticles(OIL_BROWN,
                    entity.getX(), entity.getBodyY(0.50), entity.getZ(),
                    4, 0.38, 0.55, 0.38, 0.012);
            world.spawnParticles(ParticleTypes.FALLING_HONEY,
                    entity.getX(), entity.getBodyY(0.58), entity.getZ(),
                    2, 0.30, 0.48, 0.30, 0.018);
        }

        int burn = state.elemental$burnTicks();
        if (burn > 0) {
            if (burn % 20 == 0) {
                // This is intentionally non-fire-tagged damage so Fire Resistance cannot nullify it.
                entity.damage(entity.getDamageSources().magic(), 1.0f);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.55f, 0.9f + world.random.nextFloat() * 0.2f);
            }
            if (burn % 3 == 0) {
                world.spawnParticles(FIRE_ORANGE,
                        entity.getX(), entity.getBodyY(0.45), entity.getZ(),
                        4, 0.38, 0.55, 0.38, 0.025);
                world.spawnParticles(ParticleTypes.FLAME,
                        entity.getX(), entity.getBodyY(0.50), entity.getZ(),
                        3, 0.34, 0.55, 0.34, 0.03);
                world.spawnParticles(ParticleTypes.SMOKE,
                        entity.getX(), entity.getBodyY(0.72), entity.getZ(),
                        1, 0.26, 0.35, 0.26, 0.012);
            }
        }
    }

    private static LivingEntity target(ServerPlayerEntity player, double range, double radius) {
        Vec3d a = player.getEyePos();
        Vec3d b = a.add(player.getRotationVec(1).multiply(range));
        Box box = player.getBoundingBox().stretch(player.getRotationVec(1).multiply(range)).expand(radius);
        EntityHitResult hit = net.minecraft.entity.projectile.ProjectileUtil.raycast(
                player, a, b, box,
                e -> e instanceof LivingEntity && !e.isSpectator(),
                range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void ensure(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                               Identifier id, double value) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPersistentModifier(new EntityAttributeModifier(
                    id, value, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void remove(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                               Identifier id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }

    private static void dec(Map<UUID, Integer> map, ServerPlayerEntity player) {
        int value = map.getOrDefault(player.getUuid(), 0);
        if (value > 0) map.put(player.getUuid(), value - 1);
    }
}
