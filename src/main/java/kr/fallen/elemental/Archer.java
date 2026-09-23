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
    public static final ThreadLocal<Boolean> RAPID_HIT = ThreadLocal.withInitial(()->false);
    private Archer() {}

    public static final double FOCUS_DAMAGE_MULTIPLIER = 1.50;
    public static final double RAPID_DAMAGE_MULTIPLIER = 0.12;
    public static final int HITS_PER_CHARGE = 3;
    public static final int MAX_CHARGES = 2;
    public static final double VOLLEY_RANGE = 48.0;
    public static final float VOLLEY_DAMAGE = 9.0f;

    private static final Identifier DRAW_SPEED =
            Identifier.of(ElementalEyes.MODID, "archer_draw_speed");
    private static final Map<UUID, Boolean> rapidMode = new HashMap<>();
    private static final Map<UUID, Integer> charges = new HashMap<>();
    private static final Map<UUID, Integer> hitProgress = new HashMap<>();
    // Entity identity keeps client prediction separate from the integrated server.
    private static final Map<net.minecraft.entity.player.PlayerEntity, RapidCycle> rapidCycles = new java.util.WeakHashMap<>();
    private static final class RapidCycle { int shots; long ready; }
    public static boolean canRapidFire(net.minecraft.entity.player.PlayerEntity p) {
        RapidCycle cycle=rapidCycles.get(p);
        return cycle==null || p.getWorld().getTime()>=cycle.ready;
    }
    public static int rapidFired(net.minecraft.entity.player.PlayerEntity p) {
        RapidCycle cycle=rapidCycles.computeIfAbsent(p,k->new RapidCycle());
        int cooldown=++cycle.shots>=5?20:4;
        if(cycle.shots>=5)cycle.shots=0;
        cycle.ready=p.getWorld().getTime()+cooldown;
        return cooldown;
    }

    private static final DustParticleEffect BEAM =
            new DustParticleEffect(new Vector3f(0.70f, 0.88f, 1.0f), 1.15f);

    public static boolean isArcher(ServerPlayerEntity player) {
        return OriginBridge.has(player, "elemental_eyes:archer");
    }

    public static boolean isRapid(ServerPlayerEntity player) {
        return isArcher(player) && rapidMode.getOrDefault(player.getUuid(), false);
    }

    public static boolean isRapidPlayer(net.minecraft.entity.player.PlayerEntity player) {
        return player instanceof ServerPlayerEntity server ? isRapid(server) : player.getCommandTags().contains("elemental_rapid_client");
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
        Vec3d a=player.getEyePos(), b=a.add(player.getRotationVec(1).multiply(VOLLEY_RANGE));
        Characters.beam(player,a,b,0);
        player.getServerWorld().playSound(null,player.getX(),player.getY(),player.getZ(),
            SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,SoundCategory.PLAYERS,.8f,1.25f);
        player.getServerWorld().playSound(null,player.getX(),player.getY(),player.getZ(),
            SoundEvents.ENTITY_WARDEN_SONIC_BOOM,SoundCategory.PLAYERS,.75f,1.15f);
        for(LivingEntity target:Characters.lineTargets(player,a,b,.22)) {
            if(target.damage(Characters.damage(player,"halo_point"),VOLLEY_DAMAGE))Characters.bleed(target,player);
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
