package kr.fallen.elemental.client;
import kr.fallen.elemental.ElementalEyes;
import net.fabricmc.api.ClientModInitializer;import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;import net.minecraft.client.option.KeyBinding;import net.minecraft.client.util.InputUtil;import net.minecraft.network.PacketByteBuf;import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;import org.lwjgl.glfw.GLFW;
public class ElementalEyesClient implements ClientModInitializer{
 private KeyBinding s1,s2;
 public void onInitializeClient(){s1=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.elemental_eyes.skill1",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,"category.elemental_eyes"));s2=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.elemental_eyes.skill2",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_V,"category.elemental_eyes"));ClientTickEvents.END_CLIENT_TICK.register(c->{while(s1.wasPressed())ClientPlayNetworking.send(ElementalEyes.SKILL1,PacketByteBufs.empty());while(s2.wasPressed())ClientPlayNetworking.send(ElementalEyes.SKILL2,PacketByteBufs.empty());});}
}
