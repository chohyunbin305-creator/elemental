package kr.fallen.elemental;
import net.minecraft.entity.LivingEntity;
public final class AttributeSystem {
 private AttributeSystem(){}
 public static void hit(LivingEntity attacker,LivingEntity target,Element element){
  if(element==Element.FIRE && target instanceof ToadSage.Oiled o && o.elemental$isOiled()){
   ToadSage.igniteOil(target);
  }
 }
}
