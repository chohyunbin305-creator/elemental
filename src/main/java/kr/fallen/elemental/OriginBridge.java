package kr.fallen.elemental;
import net.minecraft.entity.player.PlayerEntity;
import java.lang.reflect.*;import java.util.*;
public final class OriginBridge {
 private OriginBridge(){}
 public static boolean has(PlayerEntity p,String wanted){
  try{Class<?> c=Class.forName("io.github.apace100.origins.origin.Origin");Method get=c.getMethod("get",PlayerEntity.class);Map<?,?> m=(Map<?,?>)get.invoke(null,p);for(Object o:m.values()){Object id=c.getMethod("getId").invoke(o);if(wanted.equals(String.valueOf(id)))return true;}}catch(Throwable ignored){}
  return false;
 }
}
