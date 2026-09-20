package kr.fallen.elemental.mixin;

import kr.fallen.elemental.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ElementCarrier {
    @Unique private Element elementalEyes$element;
    @Unique private int elementalEyes$elementTicks;
    @Unique private String elementalEyes$reaction;
    @Unique private int elementalEyes$reactionTicks;
    protected LivingEntityMixin(EntityType<?> type,net.minecraft.world.World world){super(type,world);}
    @Inject(method="tick",at=@At("TAIL")) private void elementalEyes$tick(CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;
        if(elementalEyes$elementTicks>0&&--elementalEyes$elementTicks<=0)elementalEyes$element=null;
        if(elementalEyes$reactionTicks>0&&--elementalEyes$reactionTicks<=0)elementalEyes$reaction=null;
        if(!getWorld().isClient)ElementLogic.tick(self);
    }
    @Inject(method="damage",at=@At("HEAD")) private void elementalEyes$damage(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(ElementLogic.internalDamage()||getWorld().isClient)return;
        if(source.getAttacker() instanceof LivingEntity attacker&&source.getSource()==attacker)ElementLogic.onHit(attacker,(LivingEntity)(Object)this);
    }
    @Inject(method="takeKnockback",at=@At("HEAD"),cancellable=true) private void elementalEyes$earthKnockback(double strength,double x,double z,CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;if(ElementalEyes.attackingElement(self)==Element.EARTH)ci.cancel();
    }
    @Inject(method="handleFallDamage",at=@At("HEAD"),cancellable=true) private void elementalEyes$earthFall(float fallDistance,float damageMultiplier,DamageSource source,CallbackInfoReturnable<Boolean> cir){
        LivingEntity self=(LivingEntity)(Object)this;if(ElementalEyes.attackingElement(self)==Element.EARTH)cir.setReturnValue(false);
    }
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL")) private void elementalEyes$write(NbtCompound nbt,CallbackInfo ci){
        if(elementalEyes$element!=null)nbt.putString("ElementalEyesElement",elementalEyes$element.name());nbt.putInt("ElementalEyesElementTicks",elementalEyes$elementTicks);
        if(elementalEyes$reaction!=null)nbt.putString("ElementalEyesReaction",elementalEyes$reaction);nbt.putInt("ElementalEyesReactionTicks",elementalEyes$reactionTicks);
    }
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL")) private void elementalEyes$read(NbtCompound nbt,CallbackInfo ci){
        if(nbt.contains("ElementalEyesElement"))try{elementalEyes$element=Element.valueOf(nbt.getString("ElementalEyesElement"));}catch(Exception ignored){}
        elementalEyes$elementTicks=nbt.getInt("ElementalEyesElementTicks");elementalEyes$reaction=nbt.contains("ElementalEyesReaction")?nbt.getString("ElementalEyesReaction"):null;elementalEyes$reactionTicks=nbt.getInt("ElementalEyesReactionTicks");
    }
    public Element elementalEyes$getElement(){return elementalEyes$element;} public int elementalEyes$getElementTicks(){return elementalEyes$elementTicks;}
    public void elementalEyes$setElement(Element e,int ticks){elementalEyes$element=e;elementalEyes$elementTicks=ticks;} public void elementalEyes$clearElement(){elementalEyes$element=null;elementalEyes$elementTicks=0;}
    public String elementalEyes$getReaction(){return elementalEyes$reaction;} public int elementalEyes$getReactionTicks(){return elementalEyes$reactionTicks;}
    public void elementalEyes$setReaction(String id,int ticks){elementalEyes$reaction=id;elementalEyes$reactionTicks=Math.max(elementalEyes$reactionTicks,ticks);}
}
