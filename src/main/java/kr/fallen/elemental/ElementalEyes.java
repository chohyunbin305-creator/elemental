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
    public static final String MODID = "elemental_eyes";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerManager().getPlayerList().forEach(ToadSage::tick));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("elemental_oil").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                if (p != null) ToadSage.skillOil(p);
                return 1;
            }));
            dispatcher.register(literal("elemental_fire").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                if (p != null) ToadSage.startFire(p);
                return 1;
            }));
            dispatcher.register(literal("elemental_fire_stop").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                if (p != null) ToadSage.stopFire(p);
                return 1;
            }));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && entity instanceof LivingEntity target) {
                AttributeSystem.hit(player, target, null);
            }
            return ActionResult.PASS;
        });
    }
}
