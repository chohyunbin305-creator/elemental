package kr.fallen.elemental.client;

import kr.fallen.elemental.FireStatePayload;
import kr.fallen.elemental.ArcherStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
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
    private static boolean archerEnabled;
    private static boolean archerRapid;
    private static int archerCharges;
    private static int archerHitProgress;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FireStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.active()) FLAME_ACTIVE.add(payload.playerId());
                    else FLAME_ACTIVE.remove(payload.playerId());
                }));
        ClientPlayNetworking.registerGlobalReceiver(ArcherStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    archerEnabled = payload.enabled();
                    archerRapid = payload.rapid();
                    archerCharges = payload.charges();
                    archerHitProgress = payload.hitProgress();
                }));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(this::renderArcherHud);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.player.networkHandler == null) {
            wasDown = false;
            FLAME_ACTIVE.clear();
            archerEnabled = false;
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

    public static boolean isArcherRapid() {
        return archerEnabled && archerRapid;
    }

    private void renderArcherHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!archerEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int x = context.getScaledWindowWidth() / 2 + 20;
        int y = context.getScaledWindowHeight() - 77;

        context.drawTextWithShadow(client.textRenderer,
                archerRapid ? "연사" : "집중", x, y - 10,
                archerRapid ? 0xFF8FD8FF : 0xFFFFD77A);
        for (int i = 0; i < 3; i++) {
            int left = x + i * 18;
            context.fill(left, y, left + 15, y + 6, 0xD018202A);
            context.fill(left + 1, y + 1, left + 14, y + 5,
                    i < archerCharges ? 0xFF67D7C4 : 0xFF394550);
        }
        if (archerCharges < 3) {
            int width = (int) Math.round(51.0 * archerHitProgress / 6.0);
            context.fill(x, y + 8, x + 51, y + 10, 0xB018202A);
            context.fill(x, y + 8, x + width, y + 10, 0xFF9BB8D0);
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
