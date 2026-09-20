package kr.fallen.elemental;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.util.math.Vec3d;

public final class ElementLogic {
    private ElementLogic(){}
    public static final int ELEMENT_TIME=100;
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    public static boolean internalDamage(){return INTERNAL.get();}
    public static void onHit(LivingEntity attacker,LivingEntity target){
        Element incoming=ElementalEyes.attackingElement(attacker); if(incoming==null)return;
        ElementCarrier c=(ElementCarrier)target; Element old=c.elementalEyes$getElement();
        if(old==null||old==incoming){c.elementalEyes$setElement(incoming,ELEMENT_TIME);applyBase(attacker,target,incoming);return;}
        c.elementalEyes$clearElement(); String r=reaction(old,incoming); if(r!=null)react(attacker,target,r);
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
    private static boolean defenderHas(LivingEntity e,Element element){return ElementalEyes.attackingElement(e)==element;}
    private static void applyBase(LivingEntity attacker,LivingEntity target,Element e){
        switch(e){
            case FIRE->{if(!defenderHas(target,Element.WATER)){fixed(target,.5f);target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,40,0));}}
            case WATER->{if(!defenderHas(target,Element.WIND)){target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,30,0));attacker.heal(.5f);}}
            case ELECTRIC->{if(!defenderHas(target,Element.EARTH)){fixed(target,1f);attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,40,0));}}
            case WIND->{if(!defenderHas(target,Element.ELECTRIC)){target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,50,0));attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,50,0));}}
            case EARTH->{}
        }
    }
    private static void react(LivingEntity attacker,LivingEntity target,String r){
        ElementCarrier c=(ElementCarrier)target;
        switch(r){
            case "vaporize"->fixed(target,5f);
            case "freeze"->{c.elementalEyes$setReaction(r,60);target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,5));target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,60,3));}
            case "electro_charged"->c.elementalEyes$setReaction(r,80);
            case "overload"->{c.elementalEyes$setReaction(r,30);target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,30,0));Vec3d d=target.getPos().subtract(attacker.getPos()).normalize().multiply(1.1);target.addVelocity(d.x,.35,d.z);target.velocityModified=true;}
            case "erosion"->{c.elementalEyes$setReaction(r,80);target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,80,2));target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,80,1));}
            case "discharge"->fixed(target,2f);
            case "wildfire"->c.elementalEyes$setReaction(r,100);
        }
    }
    public static void tick(LivingEntity e){
        ElementCarrier c=(ElementCarrier)e;String r=c.elementalEyes$getReaction();int t=c.elementalEyes$getReactionTicks();
        if(r==null||t<=0)return;if("electro_charged".equals(r)&&t%10==0)fixed(e,.5f);if("wildfire".equals(r)&&t%10==0)fixed(e,1f);
    }
    public static void fixed(LivingEntity e,float amount){
        if(e.getWorld().isClient)return;try{INTERNAL.set(true);e.damage(e.getDamageSources().magic(),amount);}finally{INTERNAL.set(false);}
    }
}
