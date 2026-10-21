package com.pureneko.vjrm.client;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
public final class VjrmKeybinds {
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("vjrm","main"));
    public static final KeyMapping OPEN=new KeyMapping("key.vjrm.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,CATEGORY);
    public static final KeyMapping COPY_BUTTON=new KeyMapping("key.vjrm.copy_button",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,CATEGORY);
    public static void register(){KeyMappingHelper.registerKeyMapping(OPEN);KeyMappingHelper.registerKeyMapping(COPY_BUTTON);}
}
