package com.pureneko.vjrm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "vjrm", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class VjrmKeybinds {
    public static final KeyMapping OPEN = new KeyMapping("key.vjrm.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.vjrm");
    public static final KeyMapping COPY_BUTTON = new KeyMapping("key.vjrm.copy_button", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.vjrm");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); event.register(COPY_BUTTON); }
}
