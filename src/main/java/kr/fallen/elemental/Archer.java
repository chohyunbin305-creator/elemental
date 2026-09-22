package kr.fallen.elemental;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BowItem;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Archer {
    private Archer() {}

    public static final double FOCUS_DAMAGE_MULTIPLIER = 1.50;
    public static final double RAPID_DAMAGE_MULTIPLIER = 0.15;
    public static final int HITS_PER_CHARGE = 6;
    public static final int MAX_CHARGES = 3;
    public static final double VOLLEY_RANGE = 18.0;
    public static final float VOLLEY_DAMAGE = 4.0f;

    private static final Identifier DRAW_SPEED =
            Identifier.of(ElementalEyes.MODID, "archer_draw_speed");
    private static final Map<UUID, Boolean> rapidMode = new HashMap<>();
    private static final Map<UUID, Integer> charges = new HashMap<>();
    private static final Map<UUID, Integer> hitProgress = new HashMap<>();

    private static final DustParticleEffect BEAM =
            new DustParticleEffect(new Vector3f(0.70f, 0.88f, 1.0f), 1.15f);

    public static boolean isArcher(ServerPlayerEntity player) {
        return OriginBridge.has(player, "elemental_eyes:archer");
    }

    public static boolean isRapid(ServerPlayerEntity player) {
        return isArcher(player) && rapidMode.getOrDefault(player.getUuid(), false);
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isArcher(player)) {
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, DRAW_SPEED);
            rapidMode.remove(player.getUuid());
            charges.remove(player.getUuid());
            hitProgress.remove(player.getUuid());
            if (player.age % 20 == 0) sync(player, false);
            return;
        }

        if (player.isUsingItem() && player.getActiveItem().getItem() instanceof BowItem) {
            // Vanilla bow movement is about 20% of walking speed; this raises it to roughly 35%.
            ensure(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, DRAW_SPEED, 0.075);
        } else {
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, DRAW_SPEED);
        }

        if (player.age % 20 == 0) sync(player, true);
    }

    public static void toggleMode(ServerPlayerEntity player) {
        if (!isArcher(player)) return;
        boolean rapid = !rapidMode.getOrDefault(player.getUuid(), false);
        rapidMode.put(player.getUuid(), rapid);
        player.sendMessage(Text.literal(rapid ? "연사 모드" : "집중 모드"), true);
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.55f, rapid ? 1.35f : 0.82f);
        sync(player, true);
    }

    public static void onArrowHit(ServerPlayerEntity player) {
        if (!isArcher(player)) return;
        UUID id = player.getUuid();
        int stored = charges.getOrDefault(id, 0);
        if (stored >= MAX_CHARGES) return;

        int hits = hitProgress.getOrDefault(id, 0) + 1;
        if (hits >= HITS_PER_CHARGE) {
            hits = 0;
            charges.put(id, stored + 1);
            player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.62f, 1.45f);
        }
        hitProgress.put(id, hits);
        sync(player, true);
    }

    public static void useVolley(ServerPlayerEntity player) {
        if (!isArcher(player)) return;
        UUID id = player.getUuid();
        int stored = charges.getOrDefault(id, 0);
        if (stored <= 0) {
            player.sendMessage(Text.literal("충전된 관통 화살이 없습니다."), true);
            return;
        }

        charges.put(id, stored - 1);
        fireVolley(player);
        sync(player, true);
    }

    private static void fireVolley(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getEyePos().add(0, -0.12, 0);
        Vec3d forward = player.getRotationVec(1).normalize();
        Vec3d side = new Vec3d(-forward.z, 0, forward.x);
        if (side.lengthSquared() < 0.001) side = new Vec3d(1, 0, 0);
        else side = side.normalize();

        world.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.55f);

        for (double lane : new double[]{-0.72, 0.0, 0.72}) {
            Vec3d laneStart = start.add(side.multiply(lane));
            for (double d = 0.5; d <= VOLLEY_RANGE; d += 0.55) {
                Vec3d point = laneStart.add(forward.multiply(d));
                world.spawnParticles(BEAM, point.x, point.y, point.z,
                        2, 0.035, 0.035, 0.035, 0.0);
                if (((int) (d * 10)) % 11 == 0) {
                    world.spawnParticles(ParticleTypes.CRIT, point.x, point.y, point.z,
                            1, 0.02, 0.02, 0.02, 0.0);
                }
            }

            Box area = new Box(laneStart, laneStart.add(forward.multiply(VOLLEY_RANGE))).expand(0.7);
            for (LivingEntity target : world.getEntitiesByClass(
                    LivingEntity.class, area,
                    e -> e != player && e.isAlive() && !e.isSpectator())) {
                Vec3d to = target.getBoundingBox().getCenter().subtract(laneStart);
                double along = to.dotProduct(forward);
                if (along < 0 || along > VOLLEY_RANGE) continue;
                double distance = to.subtract(forward.multiply(along)).length();
                if (distance <= 0.62 + target.getWidth() * 0.35) {
                    // Sonic-boom damage bypasses shields; no visibility check means walls are penetrated.
                    target.damage(target.getDamageSources().sonicBoom(player), VOLLEY_DAMAGE);
                }
            }
        }
    }

    private static void sync(ServerPlayerEntity player, boolean enabled) {
        ServerPlayNetworking.send(player, new ArcherStatePayload(
                enabled,
                enabled && rapidMode.getOrDefault(player.getUuid(), false),
                enabled ? charges.getOrDefault(player.getUuid(), 0) : 0,
                enabled ? hitProgress.getOrDefault(player.getUuid(), 0) : 0));
    }

    private static void ensure(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                               Identifier id, double value) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        EntityAttributeModifier current = instance.getModifier(id);
        if (current == null || current.value() != value) {
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
}
