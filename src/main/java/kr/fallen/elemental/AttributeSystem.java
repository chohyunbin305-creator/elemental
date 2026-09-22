package kr.fallen.elemental;
import net.minecraft.entity.LivingEntity;
public final class AttributeSystem {
 private AttributeSystem(){}
 public static void hit(LivingEntity attacker,LivingEntity target,Element element){
  if(element!=Element.FIRE || !(target instanceof ToadSage.Oiled o)) return;
  if(o.elemental$isOiled()){
   ToadSage.igniteOil(target);
  } else if(o.elemental$burnTicks()>0){
   o.elemental$burn(100);
  }
 }
}
