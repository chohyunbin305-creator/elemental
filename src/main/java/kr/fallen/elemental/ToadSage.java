package kr.fallen.elemental;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.particle.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import java.util.*;
public final class ToadSage {
 private ToadSage(){}
 public interface Oiled {boolean elemental$isOiled();void elemental$oil(int t);int elemental$oilTicks();void elemental$burn(int t);int elemental$burnTicks();}
 private static final Map<UUID,Integer> oilCd=new HashMap<>(),fireCd=new HashMap<>(),spray=new HashMap<>();
 private static final Identifier HEALTH=Identifier.of(ElementalEyes.MODID,"toad_health"),REACH=Identifier.of(ElementalEyes.MODID,"toad_reach"),JUMP=Identifier.of(ElementalEyes.MODID,"toad_jump");
 public static boolean isToad(ServerPlayerEntity p){return OriginBridge.has(p,"elemental_eyes:toad_sage");}
 public static void skillOil(ServerPlayerEntity p){if(!isToad(p)||oilCd.getOrDefault(p.getUuid(),0)>0)return;oilCd.put(p.getUuid(),240);LivingEntity t=target(p,12,1.15);ServerWorld w=p.getServerWorld();Vec3d a=p.getEyePos(),d=p.getRotationVec(1).normalize();for(int i=1;i<=24;i++){double z=i*.48,spread=.025*z;Vec3d q=a.add(d.multiply(z));w.spawnParticles(ParticleTypes.FALLING_HONEY,q.x,q.y,q.z,3,spread,spread*.7,spread,.018);}if(t!=null){((Oiled)t).elemental$oil(140);w.spawnParticles(ParticleTypes.FALLING_HONEY,t.getX(),t.getBodyY(.5),t.getZ(),22,.55,.65,.55,.045);}}
 public static void skillFire(ServerPlayerEntity p){if(!isToad(p)||fireCd.getOrDefault(p.getUuid(),0)>0)return;fireCd.put(p.getUuid(),160);spray.put(p.getUuid(),30);}
 public static void tick(ServerPlayerEntity p){dec(oilCd,p);dec(fireCd,p);if(!isToad(p)){spray.remove(p.getUuid());return;}ensure(p,EntityAttributes.GENERIC_MAX_HEALTH,HEALTH,4.0);ensure(p,EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,REACH,.75);ensure(p,EntityAttributes.GENERIC_JUMP_STRENGTH,JUMP,.08);int s=spray.getOrDefault(p.getUuid(),0);if(s>0){spray.put(p.getUuid(),s-1);if(s%5==0)firePulse(p);}}
 private static void ensure(ServerPlayerEntity p,RegistryEntry<EntityAttribute> a,Identifier id,double v){EntityAttributeInstance x=p.getAttributeInstance(a);if(x!=null&&x.getModifier(id)==null)x.addPersistentModifier(new EntityAttributeModifier(id,v,EntityAttributeModifier.Operation.ADD_VALUE));}
 private static void firePulse(ServerPlayerEntity p){ServerWorld w=p.getServerWorld();Vec3d a=p.getEyePos(),d=p.getRotationVec(1).normalize();for(int i=1;i<=16;i++){double z=i*.44,sp=.035*z;Vec3d q=a.add(d.multiply(z));w.spawnParticles(ParticleTypes.FLAME,q.x,q.y,q.z,2,sp,sp*.65,sp,.005);w.spawnParticles(ParticleTypes.SMOKE,q.x,q.y,q.z,3,sp*.8,sp*.55,sp*.8,.025);}LivingEntity t=target(p,7,1.25);if(t!=null){t.damage(p.getDamageSources().playerAttack(p),1.5f);AttributeSystem.hit(p,t,Element.FIRE);}}
 public static void igniteOil(LivingEntity t){Oiled o=(Oiled)t;if(!o.elemental$isOiled())return;o.elemental$oil(0);o.elemental$burn(80);ServerWorld w=(ServerWorld)t.getWorld();w.spawnParticles(ParticleTypes.FLAME,t.getX(),t.getBodyY(.5),t.getZ(),24,.6,.7,.6,.1);}
 public static void burnTick(LivingEntity e,Oiled o){if(o.elemental$burnTicks()>0&&o.elemental$burnTicks()%20==0)e.damage(e.getDamageSources().magic(),1f);}
 private static LivingEntity target(ServerPlayerEntity p,double r,double rad){Vec3d a=p.getEyePos(),b=a.add(p.getRotationVec(1).multiply(r));Box box=p.getBoundingBox().stretch(p.getRotationVec(1).multiply(r)).expand(rad);EntityHitResult h=net.minecraft.entity.projectile.ProjectileUtil.raycast(p,a,b,box,e->e instanceof LivingEntity&&!e.isSpectator(),r*r);return h!=null&&h.getEntity() instanceof LivingEntity l?l:null;}
 private static void dec(Map<UUID,Integer>m,ServerPlayerEntity p){int v=m.getOrDefault(p.getUuid(),0);if(v>0)m.put(p.getUuid(),v-1);}
}
