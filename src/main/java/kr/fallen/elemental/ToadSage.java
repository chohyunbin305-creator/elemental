package kr.fallen.elemental;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
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
    private static final Identifier SAFE_FALL = Identifier.of(ElementalEyes.MODID, "toad_safe_fall");
    private static final Identifier FIRE_SLOW = Identifier.of(ElementalEyes.MODID, "fire_slow");

    private static final DustParticleEffect FIRE_ORANGE =
            new DustParticleEffect(new Vector3f(1.0f, 0.30f, 0.02f), 1.9f);
    private static final DustParticleEffect FIRE_YELLOW =
            new DustParticleEffect(new Vector3f(1.0f, 0.62f, 0.05f), 1.55f);
    private static final DustParticleEffect FIRE_RED =
            new DustParticleEffect(new Vector3f(0.92f, 0.07f, 0.01f), 1.65f);
    private static final DustParticleEffect OIL_BROWN =
            new DustParticleEffect(new Vector3f(0.34f, 0.17f, 0.055f), 1.35f);
    private static final DustParticleEffect OIL_GOLD =
            new DustParticleEffect(new Vector3f(0.88f, 0.58f, 0.12f), 1.45f);

    public static boolean isToad(ServerPlayerEntity player) {
        return OriginBridge.has(player, "elemental_eyes:toad_sage");
    }

    public static boolean isSpraying(PlayerEntity player) {
        return spray.getOrDefault(player.getUuid(), 0) > 0;
    }

    public static void skillOil(ServerPlayerEntity player) {
        if (!isToad(player) || oilCd.getOrDefault(player.getUuid(), 0) > 0) return;
        oilCd.put(player.getUuid(), 240);

        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1).normalize();

        // Faster-looking, longer and wider spray. Yellow/gold dominates over brown.
        for (int i = 2; i <= 28; i++) {
            double z = i * 0.56;
            double spread = 0.06 + z * 0.06;
            Vec3d q = start.add(dir.multiply(z));
            world.spawnParticles(OIL_GOLD, q.x, q.y, q.z, 5,
                    spread, spread * 0.62, spread, 0.035);
            world.spawnParticles(ParticleTypes.FALLING_HONEY, q.x, q.y, q.z, 4,
                    spread * 0.78, spread * 0.48, spread * 0.78, 0.045);
            if ((i & 1) == 0) {
                world.spawnParticles(OIL_BROWN, q.x, q.y, q.z, 2,
                        spread * 0.70, spread * 0.50, spread * 0.70, 0.022);
            }
        }

        // Oil coats every visible target inside the spray cone, not just the first hit.
        Box area = player.getBoundingBox().stretch(dir.multiply(15.5)).expand(2.6);
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3d to = target.getBoundingBox().getCenter().subtract(start);
            double forward = to.dotProduct(dir);
            if (forward < 0.2 || forward > 15.5) continue;

            double side = to.subtract(dir.multiply(forward)).length();
            double allowedRadius = 0.72 + forward * 0.105;
            if (side > allowedRadius || !player.canSee(target)) continue;

            ((Oiled) target).elemental$oil(160);
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 1.2f, 0.72f);
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.PLAYERS, 0.8f, 0.62f);
            oilBurst(world, target, 38);
        }
    }

    public static void startFire(ServerPlayerEntity player) {
        if (!isToad(player) || fireCd.getOrDefault(player.getUuid(), 0) > 0) return;
        fireCd.put(player.getUuid(), 160);
        spray.put(player.getUuid(), 100); // hard cap: 5 seconds
        sendFireState(player, true);
        player.getServerWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.9f, 0.68f);
    }

    public static void stopFire(ServerPlayerEntity player) {
        if (spray.remove(player.getUuid()) != null) {
            sendFireState(player, false);
        }
        remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW);
    }

    public static void tick(ServerPlayerEntity player) {
        dec(oilCd, player);
        dec(fireCd, player);

        if (!isToad(player)) {
            if (spray.remove(player.getUuid()) != null) {
                sendFireState(player, false);
            }
            remove(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH);
            remove(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, REACH);
            remove(player, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP);
            remove(player, EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, SAFE_FALL);
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            return;
        }

        ensure(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, 4.0);
        ensure(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, REACH, 0.75);
        ensure(player, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP, 0.30);
        ensure(player, EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, SAFE_FALL, 2.0);

        int ticks = spray.getOrDefault(player.getUuid(), 0);
        if (ticks > 0) {
            // Noticeably slow while maintaining the breath.
            ensure(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FIRE_SLOW, -0.070);
            spray.put(player.getUuid(), ticks - 1);

            firePulse(player, ticks % 4 == 0);

            // Repeating low whoosh while the key is held.
            if (ticks % 8 == 0) {
                player.getServerWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                        SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.48f, 0.62f);
            }
            if (ticks % 5 == 0) {
                player.getServerWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.32f, 0.78f);
            }

            if (ticks == 1) {
                sendFireState(player, false);
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

        for (int i = 2; i <= 25; i++) {
            double z = i * 0.44;
            double spread = 0.11 + z * 0.23;
            Vec3d q = start.add(dir.multiply(z));

            world.spawnParticles(FIRE_ORANGE, q.x, q.y, q.z, 5,
                    spread, spread * 0.64, spread, 0.025);
            world.spawnParticles(FIRE_YELLOW, q.x, q.y, q.z, 4,
                    spread * 0.78, spread * 0.50, spread * 0.78, 0.022);
            world.spawnParticles(FIRE_RED, q.x, q.y, q.z, 2,
                    spread * 0.90, spread * 0.55, spread * 0.90, 0.018);
            if ((i & 1) == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, q.x, q.y, q.z, 2,
                        spread * 0.66, spread * 0.44, spread * 0.66, 0.020);
            }
        }

        if (!dealDamage) return;

        Box area = player.getBoundingBox().stretch(dir.multiply(11.0)).expand(3.3);
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator())) {

            Vec3d to = target.getBoundingBox().getCenter().subtract(start);
            double forward = to.dotProduct(dir);
            if (forward < 0.2 || forward > 11.0) continue;

            double side = to.subtract(dir.multiply(forward)).length();
            double allowedRadius = 0.62 + forward * 0.245;
            if (side > allowedRadius || !player.canSee(target)) continue;

            // Baseline breath damage is ordinary fire damage and is blocked by Fire Resistance.
            if (!target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                target.damage(player.getDamageSources().playerAttack(player), 1.2f);
                target.setOnFireFor(2);
            }

            // Only this character's flame breath can start the special 13-second oil burn.
            if (target instanceof Oiled oiled && oiled.elemental$isOiled()) {
                igniteOil(target);
            }
        }
    }

    public static void igniteOil(LivingEntity target) {
        Oiled oiled = (Oiled) target;
        if (!oiled.elemental$isOiled() || oiled.elemental$burnTicks() > 0) return;

        // First ignition fixes the timer at 13 seconds. It never refreshes.
        oiled.elemental$oil(0);
        oiled.elemental$burn(260);

        if (target.getWorld() instanceof ServerWorld world) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.9f, 0.82f);
            world.spawnParticles(FIRE_ORANGE, target.getX(), target.getBodyY(0.5), target.getZ(),
                    36, 0.65, 0.82, 0.65, 0.045);
            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(),
                    18, 0.55, 0.75, 0.55, 0.05);
        }
    }

    public static void statusTick(LivingEntity entity, Oiled state) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;

        int oil = state.elemental$oilTicks();
        if (oil > 0 && oil % 5 == 0) {
            // Sticky oil lightly slows its victim without particles or a HUD icon.
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 10, 0, true, false, false));
        }
        if (oil > 0 && oil % 4 == 0) {
            // Highly visible clumps around the oiled victim.
            world.spawnParticles(OIL_GOLD,
                    entity.getX(), entity.getBodyY(0.48), entity.getZ(),
                    7, 0.48, 0.72, 0.48, 0.020);
            world.spawnParticles(ParticleTypes.FALLING_HONEY,
                    entity.getX(), entity.getBodyY(0.56), entity.getZ(),
                    5, 0.40, 0.68, 0.40, 0.030);
            world.spawnParticles(OIL_BROWN,
                    entity.getX(), entity.getBodyY(0.42), entity.getZ(),
                    2, 0.44, 0.60, 0.44, 0.014);
        }

        int burn = state.elemental$burnTicks();
        if (burn > 0) {
            // Special combo DOT: ignores Fire Resistance.
            if (burn % 20 == 0) {
                entity.damage(entity.getDamageSources().magic(), 1.0f);
            }
            if (burn % 10 == 0) {
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS,
                        1.05f, 0.88f + world.random.nextFloat() * 0.18f);
            }
            if (burn % 3 == 0) {
                world.spawnParticles(FIRE_ORANGE,
                        entity.getX(), entity.getBodyY(0.45), entity.getZ(),
                        4, 0.38, 0.58, 0.38, 0.028);
                world.spawnParticles(ParticleTypes.FLAME,
                        entity.getX(), entity.getBodyY(0.50), entity.getZ(),
                        3, 0.34, 0.58, 0.34, 0.032);
                world.spawnParticles(ParticleTypes.SMOKE,
                        entity.getX(), entity.getBodyY(0.72), entity.getZ(),
                        1, 0.27, 0.36, 0.27, 0.014);
            }
        }
    }

    private static void oilBurst(ServerWorld world, LivingEntity target, int count) {
        world.spawnParticles(OIL_GOLD, target.getX(), target.getBodyY(0.5), target.getZ(),
                count, 0.62, 0.78, 0.62, 0.035);
        world.spawnParticles(ParticleTypes.FALLING_HONEY, target.getX(), target.getBodyY(0.55), target.getZ(),
                count / 2, 0.52, 0.72, 0.52, 0.045);
        world.spawnParticles(OIL_BROWN, target.getX(), target.getBodyY(0.45), target.getZ(),
                Math.max(4, count / 5), 0.50, 0.66, 0.50, 0.025);
    }

    private static void ensure(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                               Identifier id, double value) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;

        EntityAttributeModifier current = instance.getModifier(id);
        if (current == null || current.value() != value
                || current.operation() != EntityAttributeModifier.Operation.ADD_VALUE) {
            if (current != null) instance.removeModifier(id);
            instance.addPersistentModifier(new EntityAttributeModifier(
                    id, value, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void remove(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                               Identifier id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) != null) instance.removeModifier(id);
    }

    private static void sendFireState(ServerPlayerEntity source, boolean active) {
        FireStatePayload payload = new FireStatePayload(source.getUuid(), active);
        source.getServer().getPlayerManager().getPlayerList().forEach(player ->
                ServerPlayNetworking.send(player, payload));
    }

    private static void dec(Map<UUID, Integer> map, ServerPlayerEntity player) {
        int value = map.getOrDefault(player.getUuid(), 0);
        if (value > 0) map.put(player.getUuid(), value - 1);
    }
}
