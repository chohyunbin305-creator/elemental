package kr.fallen.elemental.client;

import kr.fallen.elemental.FireStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.lang.reflect.Field;

public class ElementalEyesClient implements ClientModInitializer {
    private KeyBinding secondary;
    private boolean wasDown;
    private static boolean flameActive;
    private ItemStack heldBeforeFlame;
    private int heldSlot = -1;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FireStatePayload.ID, (payload, context) ->
                context.client().execute(() -> flameActive = payload.active()));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.player.networkHandler == null) {
            wasDown = false;
            flameActive = false;
            heldBeforeFlame = null;
            heldSlot = -1;
            return;
        }

        updateHornPose(client);

        if (secondary == null) {
            secondary = findBinding(client, "key.origins.secondary_active");
            if (secondary == null) return;
        }

        boolean down = secondary.isPressed();
        if (wasDown && !down) {
            client.player.networkHandler.sendChatCommand("elemental_fire_stop");
        }
        wasDown = down;
    }

    private void updateHornPose(MinecraftClient client) {
        if (flameActive) {
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);

            if (heldBeforeFlame == null) {
                heldSlot = client.player.getInventory().selectedSlot;
                heldBeforeFlame = client.player.getInventory().getStack(heldSlot).copy();
            }
            client.player.getInventory().setStack(heldSlot, new ItemStack(Items.GOAT_HORN));
            if (!client.player.isUsingItem()) client.player.setCurrentHand(Hand.MAIN_HAND);
        } else if (heldBeforeFlame != null) {
            client.player.stopUsingItem();
            client.player.getInventory().setStack(heldSlot, heldBeforeFlame);
            heldBeforeFlame = null;
            heldSlot = -1;
        }
    }

    private static KeyBinding findBinding(MinecraftClient client, String translationKey) {
        // Origins keeps its modded key binding in a public static field rather than
        // a dedicated GameOptions field. Resolve it reflectively so this addon does
        // not need Origins on its compile classpath.
        try {
            Class<?> originsClient = Class.forName("io.github.apace100.origins.OriginsClient");
            Field field = originsClient.getField("secondaryActiveKeyBinding");
            Object value = field.get(null);
            if (value instanceof KeyBinding binding &&
                    translationKey.equals(binding.getTranslationKey())) {
                return binding;
            }
        } catch (Throwable ignored) {}

        // Fallback for alternate Origins builds that expose the binding through
        // GameOptions directly.
        try {
            for (Field field : client.options.getClass().getDeclaredFields()) {
                if (!KeyBinding.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(client.options);
                if (value instanceof KeyBinding binding &&
                        translationKey.equals(binding.getTranslationKey())) {
                    return binding;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
