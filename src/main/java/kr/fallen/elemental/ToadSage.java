package kr.fallen.elemental;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.particle.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;

public final class ToadSage {
 private ToadSage(){}
 public interface Oiled {boolean elemental$isOiled();void elemental$oil(int ticks);int elemental$oilTicks();void elemental$burn(int ticks);int elemental$burnTicks();}
 private static final Map<UUID,Integer> oilCd=new HashMap<>(), fireCd=new HashMap<>(), spray=new HashMap<>();
 private static final Identifier TOUGH=Identifier.of(ElementalEyes.MODID,"toad_toughness");
 public static boolean isToad(ServerPlayerEntity p){return OriginBridge.has(p,"elemental_eyes:toad_sage");}
 public static void skillOil(ServerPlayerEntity p){
  if(!isToad(p)||oilCd.getOrDefault(p.getUuid(),0)>0)return; oilCd.put(p.getUuid(),240);
  LivingEntity t=target(p,12,0.8); if(t==null)return;
  ((Oiled)t).elemental$oil(140); ServerWorld w=p.getServerWorld();
  Vec3d a=p.getEyePos(),b=t.getBodyY(.55)>0?new Vec3d(t.getX(),t.getBodyY(.55),t.getZ()):t.getPos();
  for(int i=1;i<=18;i++){Vec3d q=a.lerp(b,i/18.0);w.spawnParticles(ParticleTypes.FALLING_HONEY,q.x,q.y,q.z,2,.08,.08,.08,.01);}
  w.spawnParticles(ParticleTypes.FALLING_HONEY,t.getX(),t.getBodyY(.5),t.getZ(),18,.45,.55,.45,.04);
 }
 public static void skillFire(ServerPlayerEntity p){
  if(!isToad(p)||fireCd.getOrDefault(p.getUuid(),0)>0)return;fireCd.put(p.getUuid(),160);spray.put(p.getUuid(),30);
 }
 public static void tick(ServerPlayerEntity p){
  dec(oilCd,p);dec(fireCd,p);
  if(!isToad(p)){spray.remove(p.getUuid());return;}
  int s=spray.getOrDefault(p.getUuid(),0);if(s>0){spray.put(p.getUuid(),s-1);if(s%5==0)firePulse(p);}
 }
 private static void firePulse(ServerPlayerEntity p){
  ServerWorld w=p.getServerWorld();Vec3d start=p.getEyePos(),dir=p.getRotationVec(1).normalize();
  for(int i=1;i<=14;i++){Vec3d q=start.add(dir.multiply(i*.5));w.spawnParticles(ParticleTypes.FLAME,q.x,q.y,q.z,4,.16,.16,.16,.03);w.spawnParticles(ParticleTypes.SMOKE,q.x,q.y,q.z,1,.12,.12,.12,.01);}
  LivingEntity t=target(p,7,1.05);if(t!=null){t.damage(p.getDamageSources().playerAttack(p),1.5f);AttributeSystem.hit(p,t,Element.FIRE);}
 }
 public static void igniteOil(LivingEntity t){Oiled o=(Oiled)t;if(!o.elemental$isOiled())return;o.elemental$oil(0);o.elemental$burn(80);ServerWorld w=(ServerWorld)t.getWorld();w.spawnParticles(ParticleTypes.FLASH,t.getX(),t.getBodyY(.5),t.getZ(),1,0,0,0,0);w.spawnParticles(ParticleTypes.FLAME,t.getX(),t.getBodyY(.5),t.getZ(),30,.55,.65,.55,.12);}
 public static void burnTick(LivingEntity e,Oiled o){if(o.elemental$burnTicks()>0&&o.elemental$burnTicks()%20==0)e.damage(e.getDamageSources().magic(),1f);}
 private static LivingEntity target(ServerPlayerEntity p,double range,double radius){
  Vec3d a=p.getEyePos(),b=a.add(p.getRotationVec(1).multiply(range));Box box=p.getBoundingBox().stretch(p.getRotationVec(1).multiply(range)).expand(radius);
  EntityHitResult hit=net.minecraft.entity.projectile.ProjectileUtil.raycast(p,a,b,box,e->e instanceof LivingEntity&&!e.isSpectator(),range*range);
  return hit!=null&&hit.getEntity() instanceof LivingEntity l?l:null;
 }
 private static void dec(Map<UUID,Integer> m,ServerPlayerEntity p){int v=m.getOrDefault(p.getUuid(),0);if(v>0)m.put(p.getUuid(),v-1);}
}
