package com.pureneko.vjrm.client;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
public final class VjrmClientEvents {
    private static boolean toggleHeld;
    public static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            RecipeSources.tick();if(mc.screen==null)toggleHeld=false;
            while(VjrmKeybinds.OPEN.consumeClick())if(mc.level!=null)mc.setScreen(new RecipeEditorScreen());
            while(VjrmKeybinds.COPY_BUTTON.consumeClick())toggle();
        });
        ScreenEvents.AFTER_INIT.register((mc,screen,width,height)->{
            ScreenKeyboardEvents.allowKeyPress(screen).register((s,key,scan,modifiers)->{
                if(s instanceof net.minecraft.client.gui.screens.controls.KeyBindsScreen||s.getFocused() instanceof net.minecraft.client.gui.components.EditBox||s.getFocused() instanceof JsonEditBox)return true;
                if(VjrmKeybinds.COPY_BUTTON.matches(key,scan)){if(!toggleHeld){toggleHeld=true;toggle();}return false;}return true;
            });
            ScreenKeyboardEvents.afterKeyRelease(screen).register((s,key,scan,modifiers)->{if(VjrmKeybinds.COPY_BUTTON.matches(key,scan))toggleHeld=false;});
        });
    }
    private static void toggle(){
        VjrmClientConfig.toggle();var player=Minecraft.getInstance().player;
        if(player!=null)player.displayClientMessage(net.minecraft.network.chat.Component.translatable(VjrmClientConfig.copyButton()?"vjrm.copy_button.on":"vjrm.copy_button.off"),true);
    }
}
