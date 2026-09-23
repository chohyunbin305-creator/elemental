package kr.fallen.elemental;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

import static net.minecraft.server.command.CommandManager.literal;

public class ElementalEyes implements ModInitializer {
    public static final String MODID = "elemental_eyes";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(FireStatePayload.ID, FireStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ArcherStatePayload.ID, ArcherStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterStatePayload.ID,CharacterStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BeamPayload.ID,BeamPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LauncherPosePayload.ID,LauncherPosePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ControlPayload.ID,ControlPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(ControlPayload.ID,(payload,context)->
            context.server().execute(()->Characters.controls(context.player(),payload.primary(),payload.secondary())));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->Characters.clear());
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->Characters.disconnect(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    ToadSage.tick(player);
                    Archer.tick(player);
                    Characters.tick(player);
                }));
        ServerTickEvents.END_SERVER_TICK.register(server->Characters.worldTick());

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
            dispatcher.register(literal("elemental_archer_mode").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                if (p != null) Archer.toggleMode(p);
                return 1;
            }));
            dispatcher.register(literal("elemental_archer_volley").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                if (p != null) Archer.useVolley(p);
                return 1;
            }));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (ToadSage.isSpraying(player)) return ActionResult.FAIL;
            if (!world.isClient && entity instanceof LivingEntity target) {
                AttributeSystem.hit(player, target, null);
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) ->
                ToadSage.isSpraying(player)
                        ? TypedActionResult.fail(player.getStackInHand(hand))
                        : TypedActionResult.pass(player.getStackInHand(hand)));
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                ToadSage.isSpraying(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if(ToadSage.isSpraying(player))return ActionResult.FAIL;
            if(Characters.outlaw(player) && !player.isSneaking() && entity instanceof net.minecraft.entity.passive.AbstractHorseEntity horse
                && !horse.isBaby() && !horse.hasPassengers()) {
                if(!world.isClient)player.startRiding(horse);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
