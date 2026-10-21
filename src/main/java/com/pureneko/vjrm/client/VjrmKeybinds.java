package com.pureneko.vjrm.client;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
public final class VjrmKeybinds {
    public static final KeyMapping OPEN=new KeyMapping("key.vjrm.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"key.categories.vjrm");
    public static final KeyMapping COPY_BUTTON=new KeyMapping("key.vjrm.copy_button",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,"key.categories.vjrm");
    public static void register(){KeyBindingHelper.registerKeyBinding(OPEN);KeyBindingHelper.registerKeyBinding(COPY_BUTTON);}
}
