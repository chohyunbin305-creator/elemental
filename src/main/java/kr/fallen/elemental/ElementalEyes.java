package kr.fallen.elemental;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import static net.minecraft.server.command.CommandManager.literal;
public class ElementalEyes implements ModInitializer {
 public static final String MODID="elemental_eyes";
 @Override public void onInitialize(){
  ServerTickEvents.END_SERVER_TICK.register(server->server.getPlayerManager().getPlayerList().forEach(ToadSage::tick));
  CommandRegistrationCallback.EVENT.register((d,r,e)->{
   d.register(literal("elemental_oil").executes(c->{ServerPlayerEntity p=c.getSource().getPlayer();if(p!=null)ToadSage.skillOil(p);return 1;}));
   d.register(literal("elemental_fire").executes(c->{ServerPlayerEntity p=c.getSource().getPlayer();if(p!=null)ToadSage.skillFire(p);return 1;}));
  });
  AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{if(!world.isClient&&entity instanceof LivingEntity target)AttributeSystem.hit(player,target,null);return ActionResult.PASS;});
 }
}
