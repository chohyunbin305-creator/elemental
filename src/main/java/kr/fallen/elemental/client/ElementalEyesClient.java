package kr.fallen.elemental.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.lang.reflect.Field;

public class ElementalEyesClient implements ClientModInitializer {
    private KeyBinding secondary;
    private boolean wasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.player.networkHandler == null) {
            wasDown = false;
            return;
        }

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

    private static KeyBinding findBinding(MinecraftClient client, String translationKey) {
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
