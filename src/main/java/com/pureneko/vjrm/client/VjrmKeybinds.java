package com.pureneko.vjrm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "vjrm", value = Dist.CLIENT)
public final class VjrmKeybinds {
    private static final KeyMapping.Category CATEGORY=new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath("vjrm","main"));
    public static final KeyMapping OPEN = new KeyMapping("key.vjrm.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY);
    public static final KeyMapping COPY_BUTTON = new KeyMapping("key.vjrm.copy_button", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.registerCategory(CATEGORY); event.register(OPEN); event.register(COPY_BUTTON); }
}
