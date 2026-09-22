package kr.fallen.elemental;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
public class ElementalEyes implements ModInitializer {
 public static final String MODID="elemental_eyes";
 @Override public void onInitialize(){
  ServerTickEvents.END_SERVER_TICK.register(server->server.getPlayerManager().getPlayerList().forEach(ToadSage::tick));
  AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{if(!world.isClient&&entity instanceof LivingEntity target)AttributeSystem.hit(player,target,null);return ActionResult.PASS;});
 }
}
