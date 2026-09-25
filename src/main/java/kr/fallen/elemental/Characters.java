package kr.fallen.elemental;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.effect.*;
import net.minecraft.particle.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

/** Server-authoritative held skills. All durations and cooldowns are measured in ticks. */
public final class Characters {
    private Characters() {}
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final List<RopeShot> SHOTS = new ArrayList<>();
    private static final Map<UUID, Binding> BINDINGS = new HashMap<>();
    private static final Map<UUID, Bleed> BLEEDS = new HashMap<>();
    private static final Identifier SPEED = Identifier.of(ElementalEyes.MODID,"character_speed");
    private static final Identifier ATTACK = Identifier.of(ElementalEyes.MODID,"android_attack");
    private static final DustParticleEffect BROWN = new DustParticleEffect(new Vector3f(.40f,.23f,.10f),.8f);
    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(.65f,.06f,.04f),.55f);
    private static class State {
        int kind, gauge, mainCd, subCd, decay, roll;
        boolean primary, secondary, display, firing, aiming;
        long heartbeat;
        Vec3d rollDirection = Vec3d.ZERO;
    }
    private record Bleed(LivingEntity target, ServerPlayerEntity owner, long end) {}
    private record Binding(LivingEntity target, ServerPlayerEntity owner, long end) {}
    private static class RopeShot {
        final ServerPlayerEntity owner;
        final ServerWorld world;
        Vec3d pos, velocity;
        int age;
        RopeShot(ServerPlayerEntity p) { owner=p; world=p.getServerWorld(); pos=p.getEyePos(); velocity=p.getRotationVec(1).multiply(1.3); }
    }
    public static boolean android(Entity e) { return e instanceof PlayerEntity p && (p.getWorld().isClient ? p.getCommandTags().contains("elemental_android_client") : OriginBridge.has(p,"elemental_eyes:android")); }
    public static boolean outlaw(Entity e) { return e instanceof PlayerEntity p && (p.getWorld().isClient ? p.getCommandTags().contains("elemental_outlaw_client") : OriginBridge.has(p,"elemental_eyes:outlaw")); }
    public static boolean frozen(ServerPlayerEntity p) { State s=STATES.get(p.getUuid()); return s!=null && s.firing; }
    public static int kind(PlayerEntity p) { return android(p)?1:outlaw(p)?2:0; }
    public static void controls(ServerPlayerEntity p, boolean primary, boolean secondary) {
        State s=STATES.computeIfAbsent(p.getUuid(),k->new State());
        s.heartbeat=p.getServerWorld().getTime();
        int k=kind(p);
        if (k==0 || !p.isAlive() || p.isSpectator()) { s.primary=false; s.secondary=false; return; }
        if (primary && !s.primary && s.mainCd==0) {
            if(k==1 && !s.firing) s.display=true;
            if(k==2 && !s.aiming && !p.hasVehicle()) {
                Vec3d look=p.getRotationVec(1);
                s.rollDirection=new Vec3d(-look.x,0,-look.z).normalize().multiply(1.33);
                s.roll=4; s.mainCd=200;
            }
        }
        if (secondary && !s.secondary && s.subCd==0) {
            if(k==1 && s.gauge>0 && !p.hasVehicle()) { stopDisplay(p,s); s.firing=true; }
            if(k==2 && s.roll==0) s.aiming=true;
        }
        if (!primary) stopDisplay(p,s);
        if (!secondary) {
            stopLauncher(s);
            if(s.aiming) { s.aiming=false; s.subCd=300; SHOTS.add(new RopeShot(p));
                sound(p,SoundEvents.ENTITY_FISHING_BOBBER_THROW,.8f,1f); }
        }
        s.primary=primary; s.secondary=secondary;
    }
    private static void stopDisplay(ServerPlayerEntity p, State s) {
        if(s.display) { s.display=false; s.mainCd=100; s.decay=40; sound(p,SoundEvents.BLOCK_PISTON_CONTRACT,.6f,1.1f); }
    }
    private static void stopLauncher(State s) { if(s.firing) { s.firing=false; s.subCd=500; } }
    public static void tick(ServerPlayerEntity p) {
        int k=kind(p);
        State s=STATES.computeIfAbsent(p.getUuid(),id->new State());
        if(s.kind!=k || !p.isAlive() || p.isSpectator()) {
            s=new State(); s.kind=k; STATES.put(p.getUuid(),s);
        }
        if(s.mainCd>0)s.mainCd--; if(s.subCd>0)s.subCd--;
        if(p.getServerWorld().getTime()-s.heartbeat>15) {
            stopDisplay(p,s); stopLauncher(s); s.aiming=false; s.primary=false; s.secondary=false;
        }
        modifier(p,EntityAttributes.GENERIC_ATTACK_DAMAGE,ATTACK,k==1?1.0:0,EntityAttributeModifier.Operation.ADD_VALUE);
        double slow=0;
        if(k==1) {
            p.setAir(p.getMaxAir());
            if(p.isTouchingWater()) { slow=-.65; p.setSprinting(false); p.setSwimming(false);
                Vec3d v=p.getVelocity(); p.setVelocity(v.x*.8,Math.min(v.y,.035),v.z*.8); p.velocityModified=true; }
            if(s.display) {
                slow=Math.min(slow,-.25);
                if(p.age%4==0) {
                    int visible=p.getServerWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(40),
                        e->e!=p && e.isAlive() && !e.isSpectator() && p.canSee(e)
                        && e.getEyePos().subtract(p.getEyePos()).normalize().dotProduct(p.getRotationVec(1))>.78).size();
                    s.gauge=Math.min(100,s.gauge+Math.min(5,visible));
                }
            } else if(s.decay>0)s.decay--;
            else if(!s.firing && p.age%10==0 && s.gauge>0)s.gauge--;
            if(s.firing) {
                slow=-1; p.setVelocity(Vec3d.ZERO); p.velocityModified=true; p.fallDistance=0;
                if(p.age%2==0) {
                    if(s.gauge<=0)stopLauncher(s);
                    else { s.gauge--; Vec3d a=p.getEyePos(), b=a.add(p.getRotationVec(1).multiply(32));
                        beam(p,a,b,1);
                        if(p.age%4==0) for(LivingEntity e:lineTargets(p,a,b,1.25))e.damage(damage(p,"launcher"),.75f);
                        if(p.age%12==0) {
                            sound(p,SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,.55f,1.8f);
                            sound(p,SoundEvents.ENTITY_WITHER_SHOOT,.45f,.75f);
                            sound(p,SoundEvents.ENTITY_PHANTOM_FLAP,.55f,.7f);
                        }
                        if(s.gauge==0)stopLauncher(s);
                    }
                }
            }
            // Extra natural strikes only in exposed rain during a thunderstorm (~one / 3 minutes).
            ServerWorld w=p.getServerWorld();
            if(p.age%20==0 && w.isThundering() && w.hasRain(p.getBlockPos()) && w.random.nextInt(180)==0) {
                LightningEntity bolt=EntityType.LIGHTNING_BOLT.create(w);
                if(bolt!=null) { bolt.refreshPositionAfterTeleport(p.getPos()); w.spawnEntity(bolt); }
            }
        }
        if(k==2) {
            if(s.aiming)slow=-.45;
            if(s.roll>0) {
                s.roll--;
                Vec3d dash = s.roll == 0 ? Vec3d.ZERO : s.rollDirection;
                p.setVelocity(dash.x,Math.min(p.getVelocity().y,0),dash.z); p.velocityModified=true;
                p.getServerWorld().spawnParticles(ParticleTypes.POOF,p.getX(),p.getY()+.2,p.getZ(),2,.14,.05,.14,.01);
            }
        }
        modifier(p,EntityAttributes.GENERIC_MOVEMENT_SPEED,SPEED,slow,EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if(p.age%2==0)ServerPlayNetworking.send(p,new CharacterStatePayload(k,s.gauge,s.display,s.firing,s.mainCd,s.subCd));
        if(p.age%2==0) {
            LauncherPosePayload pose=new LauncherPosePayload(p.getUuid(),s.firing);
            for(ServerPlayerEntity viewer:p.getServerWorld().getPlayers())
                if(viewer.squaredDistanceTo(p)<16384)ServerPlayNetworking.send(viewer,pose);
        }
    }
    public static void disconnect(ServerPlayerEntity p) { STATES.remove(p.getUuid()); }
    public static float incoming(LivingEntity e, DamageSource d, float amount) {
        if(!android(e))return amount;
        if(d.isIn(DamageTypeTags.IS_FIRE))return amount*2;
        if(d.isOf(DamageTypes.LIGHTNING_BOLT) || d.isIn(TagKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of(ElementalEyes.MODID,"is_electric"))))return amount*.5f;
        return amount;
    }
    public static DamageSource damage(ServerPlayerEntity owner,String type) {
        return new DamageSource(owner.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE)
            .entryOf(RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Identifier.of(ElementalEyes.MODID,type))),owner);
    }
    public static List<LivingEntity> lineTargets(ServerPlayerEntity p,Vec3d a,Vec3d b,double radius) {
        List<LivingEntity> result=new ArrayList<>();
        for(LivingEntity e:p.getServerWorld().getEntitiesByClass(LivingEntity.class,new Box(a,b).expand(radius+1),
            e->e!=p && e.isAlive() && !e.isSpectator())) {
            if(e.getBoundingBox().expand(radius).contains(a) || e.getBoundingBox().expand(radius).raycast(a,b).isPresent())result.add(e);
        }
        return result;
    }
    public static void beam(ServerPlayerEntity owner,Vec3d a,Vec3d b,int style) {
        BeamPayload packet=new BeamPayload(a.x,a.y,a.z,b.x,b.y,b.z,style);
        for(ServerPlayerEntity viewer:owner.getServerWorld().getPlayers())
            if(viewer.squaredDistanceTo(a)<16384)ServerPlayNetworking.send(viewer,packet);
    }
    public static void bleed(LivingEntity target,ServerPlayerEntity owner) {
        BLEEDS.put(target.getUuid(),new Bleed(target,owner,owner.getServerWorld().getTime()+80));
    }
    public static void worldTick() {
        for(Iterator<Bleed> it=BLEEDS.values().iterator();it.hasNext();) {
            Bleed b=it.next(); LivingEntity t=b.target;
            if(!t.isAlive() || t.isRemoved() || !(t.getWorld() instanceof ServerWorld w) || w.getTime()>=b.end) {it.remove();continue;}
            if(w.getTime()%10==0)w.spawnParticles(RED,t.getX(),t.getBodyY(.5),t.getZ(),2,.25,.3,.25,0);
            if(w.getTime()%20==0)t.damage(damage(b.owner,"bleeding"),.25f);
        }
        for(Iterator<RopeShot> it=SHOTS.iterator();it.hasNext();) {
            RopeShot shot=it.next();
            if(++shot.age>40 || !shot.owner.isAlive() || shot.owner.getWorld()!=shot.world) {it.remove();continue;}
            Vec3d next=shot.pos.add(shot.velocity);
            BlockHitResult block=shot.world.raycast(new RaycastContext(shot.pos,next,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,shot.owner));
            Vec3d end=block.getType()==HitResult.Type.MISS?next:block.getPos();
            LivingEntity closest=null; double nearest=Double.MAX_VALUE;
            for(LivingEntity e:lineTargets(shot.owner,shot.pos,end,.20)) {
                Optional<Vec3d> hit=e.getBoundingBox().expand(.20).raycast(shot.pos,end);
                double dist=hit.isPresent()?hit.get().squaredDistanceTo(shot.pos):0;
                if(dist<nearest){nearest=dist;closest=e;}
            }
            beam(shot.owner,shot.pos,end,2);
            if(closest!=null) {BINDINGS.put(closest.getUuid(),new Binding(closest,shot.owner,shot.world.getTime()+100));it.remove();continue;}
            if(block.getType()!=HitResult.Type.MISS){it.remove();continue;}
            shot.pos=next; shot.velocity=shot.velocity.multiply(.99).add(0,-.05,0);
        }
        for(Iterator<Binding> it=BINDINGS.values().iterator();it.hasNext();) {
            Binding b=it.next(); LivingEntity t=b.target; ServerPlayerEntity p=b.owner;
            if(!t.isAlive() || t.isRemoved() || !p.isAlive() || p.isDisconnected() || !outlaw(p)
                || t.getWorld()!=p.getWorld() || p.getWorld().getTime()>=b.end || t.squaredDistanceTo(p)>4096) {it.remove();continue;}
            t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,2,5,false,false,true));
            Vec3d toward=p.getPos().subtract(t.getPos()); double dist=toward.length();
            if(dist>2.5) {Vec3d pull=toward.normalize().multiply(Math.min(.45,(dist-2.5)*.09));
                t.setVelocity(pull.x,Math.max(t.getVelocity().y,Math.min(.15,pull.y)),pull.z);t.velocityModified=true;}
            if(p.age%2==0) {
                beam(p,p.getPos().add(0,1,0),t.getPos().add(0,t.getHeight()*.5,0),2);
                for(int i=0;i<12;i++) {double angle=i*Math.PI/6;
                    p.getServerWorld().spawnParticles(BROWN,t.getX()+Math.cos(angle)*(t.getWidth()*.6+.1),t.getBodyY(.5),
                        t.getZ()+Math.sin(angle)*(t.getWidth()*.6+.1),1,0,0,0,0);}
            }
        }
    }
    public static void clear() {STATES.clear();SHOTS.clear();BINDINGS.clear();BLEEDS.clear();}
    private static void modifier(PlayerEntity p,net.minecraft.registry.entry.RegistryEntry<EntityAttribute> type,
        Identifier id,double value,EntityAttributeModifier.Operation op) {
        EntityAttributeInstance a=p.getAttributeInstance(type);if(a==null)return;
        var old=a.getModifier(id);if(old!=null && old.value()==value)return;
        if(old!=null)a.removeModifier(id);
        if(value!=0)a.addTemporaryModifier(new EntityAttributeModifier(id,value,op));
    }
    private static void sound(ServerPlayerEntity p,SoundEvent sound,float volume,float pitch) {
        p.getServerWorld().playSound(null,p.getX(),p.getY(),p.getZ(),sound,SoundCategory.PLAYERS,volume,pitch);
    }
}
