package kr.fallen.elemental.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
public class ElementalEyesClient implements ClientModInitializer{
 private KeyBinding s1,s2;
 public void onInitializeClient(){
  s1=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.elemental_eyes.skill1",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,"category.elemental_eyes"));
  s2=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.elemental_eyes.skill2",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_V,"category.elemental_eyes"));
  ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.player==null||c.player.networkHandler==null)return;while(s1.wasPressed())c.player.networkHandler.sendChatCommand("elemental_oil");while(s2.wasPressed())c.player.networkHandler.sendChatCommand("elemental_fire");});
 }
}
