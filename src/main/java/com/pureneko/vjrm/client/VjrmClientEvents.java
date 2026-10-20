package com.pureneko.vjrm.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "vjrm", value = Dist.CLIENT)
public final class VjrmClientEvents {
    private static boolean toggleHeld;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        RecipeSources.tick();
        while (VjrmKeybinds.OPEN.consumeClick()) if(Minecraft.getInstance().level!=null) Minecraft.getInstance().setScreen(new RecipeEditorScreen());
        while (VjrmKeybinds.COPY_BUTTON.consumeClick()) toggle();
    }
    private static void toggle(){
        VjrmClientConfig.toggle();var player=Minecraft.getInstance().player;
        if(player!=null)player.displayClientMessage(net.minecraft.network.chat.Component.translatable(VjrmClientConfig.copyButton()?"vjrm.copy_button.on":"vjrm.copy_button.off"),true);
    }
    @SubscribeEvent public static void screenKey(net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre event){
        if(event.getScreen() instanceof net.minecraft.client.gui.screens.options.controls.KeyBindsScreen)return;
        if(event.getScreen().getFocused() instanceof net.minecraft.client.gui.components.EditBox || event.getScreen().getFocused() instanceof JsonEditBox)return;
        if(VjrmKeybinds.COPY_BUTTON.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(event.getKeyCode(),event.getScanCode()))){
            if(!toggleHeld){toggleHeld=true;toggle();}event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void screenKeyReleased(net.neoforged.neoforge.client.event.ScreenEvent.KeyReleased.Pre event){
        if(VjrmKeybinds.COPY_BUTTON.matches(event.getKeyCode(),event.getScanCode()))toggleHeld=false;
    }
    @SubscribeEvent public static void keyReleased(net.neoforged.neoforge.client.event.InputEvent.Key event){
        if(event.getAction()==org.lwjgl.glfw.GLFW.GLFW_RELEASE&&VjrmKeybinds.COPY_BUTTON.matches(event.getKey(),event.getScanCode()))toggleHeld=false;
    }
}
