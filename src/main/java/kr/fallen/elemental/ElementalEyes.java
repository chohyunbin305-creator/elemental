package kr.fallen.elemental;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class ElementalEyes implements ModInitializer {
 public static final String MODID="elemental_eyes";
 public static final Identifier SKILL1=Identifier.of(MODID,"skill1");
 public static final Identifier SKILL2=Identifier.of(MODID,"skill2");
 @Override public void onInitialize(){
  ServerPlayNetworking.registerGlobalReceiver(SKILL1,(server,player,h,r)->server.execute(()->ToadSage.skillOil(player)));
  ServerPlayNetworking.registerGlobalReceiver(SKILL2,(server,player,h,r)->server.execute(()->ToadSage.skillFire(player)));
  ServerTickEvents.END_SERVER_TICK.register(server->server.getPlayerManager().getPlayerList().forEach(ToadSage::tick));
  AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
   if(!world.isClient&&entity instanceof LivingEntity target) AttributeSystem.hit(player,target,null);
   return ActionResult.PASS;
  });
 }
}
