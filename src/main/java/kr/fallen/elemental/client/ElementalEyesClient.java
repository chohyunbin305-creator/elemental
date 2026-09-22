package kr.fallen.elemental.client;

import kr.fallen.elemental.FireStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ElementalEyesClient implements ClientModInitializer {
    private KeyBinding secondary;
    private boolean wasDown;
    private static final Set<UUID> FLAME_ACTIVE = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FireStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.active()) FLAME_ACTIVE.add(payload.playerId());
                    else FLAME_ACTIVE.remove(payload.playerId());
                }));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.player.networkHandler == null) {
            wasDown = false;
            FLAME_ACTIVE.clear();
            return;
        }

        if (FLAME_ACTIVE.contains(client.player.getUuid())) {
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
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

    public static boolean isFlameActive(UUID playerId) {
        return FLAME_ACTIVE.contains(playerId);
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
