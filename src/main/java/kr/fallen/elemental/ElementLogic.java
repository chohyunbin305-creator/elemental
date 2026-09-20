package kr.fallen.elemental;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class ElementLogic {
    private ElementLogic(){}
    public static final int ELEMENT_TIME=100;
    public static final Identifier WEATHERING_ARMOR=Identifier.of(ElementalEyes.MODID,"weathering_armor");
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    public static boolean internalDamage(){return INTERNAL.get();}

    public static void onHit(LivingEntity attacker,LivingEntity target){
        Element incoming=ElementalEyes.attackingElement(attacker); if(incoming==null)return;
        if(incoming==Element.FIRE)((ElementCarrier)target).elementalEyes$markFireHit();
        ElementCarrier c=(ElementCarrier)target; Element old=c.elementalEyes$getElement();
        if(old==null||old==incoming){
            c.elementalEyes$setElement(incoming,ELEMENT_TIME);
            applyBase(attacker,target,incoming); burst(target,incoming); return;
        }
        c.elementalEyes$clearElement();
        String r=reaction(old,incoming);
        if(r!=null){react(attacker,target,r);reactionBurst(target,r,old,incoming);}
    }
    private static String reaction(Element a,Element b){
        if(pair(a,b,Element.FIRE,Element.WATER))return "vaporize";
        if(pair(a,b,Element.WIND,Element.WATER))return "freeze";
        if(pair(a,b,Element.ELECTRIC,Element.WATER))return "electro_charged";
        if(pair(a,b,Element.FIRE,Element.ELECTRIC))return "overload";
        if(pair(a,b,Element.WIND,Element.EARTH))return "erosion";
        if(pair(a,b,Element.ELECTRIC,Element.WIND))return "discharge";
        if(pair(a,b,Element.FIRE,Element.WIND))return "wildfire";
        return null;
    }
    private static boolean pair(Element a,Element b,Element x,Element y){return(a==x&&b==y)||(a==y&&b==x);}
    private static boolean defenderHas(LivingEntity e,Element x){return ElementalEyes.attackingElement(e)==x;}

    private static void applyBase(LivingEntity attacker,LivingEntity target,Element e){
        switch(e){
            case FIRE->{if(!defenderHas(target,Element.WATER)){target.setOnFireFor(5);fixed(target,.5f);target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,60,0,false,false,true));}}
            case WATER->{if(!defenderHas(target,Element.WIND)){target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,50,0,false,false,true));attacker.heal(.5f);}}
            case ELECTRIC->{if(!defenderHas(target,Element.EARTH)){fixed(target,1f);attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,60,0,false,false,true));}}
            case WIND->{if(!defenderHas(target,Element.ELECTRIC)){applyWeathering(target);attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,60,0,false,false,true));}}
            case EARTH->{}
        }
    }
    public static void applyWeathering(LivingEntity e){
        EntityAttributeInstance armor=e.getAttributeInstance(EntityAttributes.GENERIC_ARMOR); if(armor==null)return;
        armor.removeModifier(WEATHERING_ARMOR);
        armor.addTemporaryModifier(new EntityAttributeModifier(WEATHERING_ARMOR,-4.0,EntityAttributeModifier.Operation.ADD_VALUE));
    }
    public static void clearWeathering(LivingEntity e){
        EntityAttributeInstance armor=e.getAttributeInstance(EntityAttributes.GENERIC_ARMOR); if(armor!=null)armor.removeModifier(WEATHERING_ARMOR);
    }

    private static void react(LivingEntity attacker,LivingEntity target,String r){
        ElementCarrier c=(ElementCarrier)target;
        switch(r){
            case "vaporize"->fixed(target,5f);
            case "freeze"->{c.elementalEyes$setReaction(r,60);target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,5,false,false,true));target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,60,3,false,false,true));}
            case "electro_charged"->c.elementalEyes$setReaction(r,80);
            case "overload"->{c.elementalEyes$setReaction(r,30);target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,30,0,false,false,true));Vec3d d=target.getPos().subtract(attacker.getPos()).normalize().multiply(1.25);target.addVelocity(d.x,.42,d.z);target.velocityModified=true;}
            case "erosion"->{c.elementalEyes$setReaction(r,80);target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,80,2,false,false,true));target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,80,1,false,false,true));}
            case "discharge"->fixed(target,2f);
            case "wildfire"->c.elementalEyes$setReaction(r,100);
        }
    }
    public static void tick(LivingEntity e){
        ElementCarrier c=(ElementCarrier)e; Element el=c.elementalEyes$getElement(); int et=c.elementalEyes$getElementTicks();
        if(el!=null&&et%8==0)aura(e,el);
        if(el==Element.FIRE&&et>0&&et%20==0){e.setOnFireFor(2);fixed(e,.5f);}
        if(el==Element.WIND&&et>0)applyWeathering(e);
        String r=c.elementalEyes$getReaction();int t=c.elementalEyes$getReactionTicks();if(r==null||t<=0)return;
        if("electro_charged".equals(r)&&t%10==0){fixed(e,.5f);spark(e);}
        if("wildfire".equals(r)&&t%10==0){e.setOnFireFor(2);fixed(e,1f);flame(e);}
    }

    private static ServerWorld sw(LivingEntity e){return e.getWorld() instanceof ServerWorld w?w:null;}
    private static void aura(LivingEntity e,Element x){ServerWorld w=sw(e);if(w==null)return;var p=switch(x){case FIRE->ParticleTypes.FLAME;case WATER->ParticleTypes.SPLASH;case ELECTRIC->ParticleTypes.ELECTRIC_SPARK;case WIND->ParticleTypes.CLOUD;case EARTH->ParticleTypes.HAPPY_VILLAGER;};w.spawnParticles(p,e.getX(),e.getBodyY(.55),e.getZ(),4,.38,.48,.38,.025);}
    private static void burst(LivingEntity e,Element x){ServerWorld w=sw(e);if(w==null)return;var p=switch(x){case FIRE->ParticleTypes.FLAME;case WATER->ParticleTypes.SPLASH;case ELECTRIC->ParticleTypes.ELECTRIC_SPARK;case WIND->ParticleTypes.CLOUD;case EARTH->ParticleTypes.HAPPY_VILLAGER;};w.spawnParticles(p,e.getX(),e.getBodyY(.5),e.getZ(),18,.65,.7,.65,.12);}
    private static void reactionBurst(LivingEntity e,String r,Element a,Element b){
        ServerWorld w=sw(e);if(w==null)return;burst(e,a);burst(e,b);
        w.spawnParticles(ParticleTypes.FLASH,e.getX(),e.getBodyY(.55),e.getZ(),1,0,0,0,0);
        switch(r){
            case "vaporize"->w.spawnParticles(ParticleTypes.CLOUD,e.getX(),e.getBodyY(.5),e.getZ(),35,.7,.8,.7,.18);
            case "freeze"->w.spawnParticles(ParticleTypes.SNOWFLAKE,e.getX(),e.getBodyY(.5),e.getZ(),35,.7,.8,.7,.15);
            case "electro_charged","discharge"->w.spawnParticles(ParticleTypes.ELECTRIC_SPARK,e.getX(),e.getBodyY(.5),e.getZ(),45,.8,.8,.8,.28);
            case "overload"->w.spawnParticles(ParticleTypes.EXPLOSION,e.getX(),e.getBodyY(.5),e.getZ(),3,.25,.25,.25,.05);
            case "erosion"->w.spawnParticles(ParticleTypes.POOF,e.getX(),e.getBodyY(.5),e.getZ(),40,.8,.8,.8,.22);
            case "wildfire"->w.spawnParticles(ParticleTypes.LARGE_SMOKE,e.getX(),e.getBodyY(.5),e.getZ(),30,.7,.8,.7,.12);
        }
    }
    private static void spark(LivingEntity e){ServerWorld w=sw(e);if(w!=null)w.spawnParticles(ParticleTypes.ELECTRIC_SPARK,e.getX(),e.getBodyY(.5),e.getZ(),9,.45,.55,.45,.12);}
    private static void flame(LivingEntity e){ServerWorld w=sw(e);if(w!=null)w.spawnParticles(ParticleTypes.FLAME,e.getX(),e.getBodyY(.5),e.getZ(),10,.45,.55,.45,.08);}
    public static void fixed(LivingEntity e,float amount){if(e.getWorld().isClient)return;try{INTERNAL.set(true);e.damage(e.getDamageSources().magic(),amount);}finally{INTERNAL.set(false);}}
}
