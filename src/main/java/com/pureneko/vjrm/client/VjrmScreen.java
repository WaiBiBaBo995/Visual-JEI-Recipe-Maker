package com.pureneko.vjrm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

/** Keeps virtual editor coordinates while using vanilla's typed input events. */
public abstract class VjrmScreen extends Screen {
    static int inputModifiers;
    protected VjrmScreen(Component title){super(title);}
    public void init(Minecraft minecraft,int width,int height){init(width,height);}
    @Override public final void extractRenderState(GuiGraphicsExtractor graphics,int x,int y,float delta){render(new LegacyGraphics(graphics),x,y,delta);}
    public void render(LegacyGraphics graphics,int x,int y,float delta){super.extractRenderState(graphics.nativeGraphics,x,y,delta);}
    protected float backgroundScale(){return 1;}
    @Override public final void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float delta){
        graphics.pose().pushMatrix();float scale=backgroundScale();graphics.pose().scale(scale,scale);
        renderBackground(new LegacyGraphics(graphics),(int)(x/scale),(int)(y/scale),delta);graphics.pose().popMatrix();
    }
    public void renderBackground(LegacyGraphics graphics,int x,int y,float delta){}
    private static MouseButtonEvent mouse(double x,double y,int button){return new MouseButtonEvent(x,y,new MouseButtonInfo(button,inputModifiers));}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){inputModifiers=event.modifiers();return mouseClicked(event.x(),event.y(),event.button());}
    public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(mouse(x,y,button),false);}
    @Override public boolean mouseReleased(MouseButtonEvent event){return mouseReleased(event.x(),event.y(),event.button());}
    public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(mouse(x,y,button));}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){return mouseDragged(event.x(),event.y(),event.button(),dx,dy);}
    public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(mouse(x,y,button),dx,dy);}
    @Override public boolean keyPressed(KeyEvent event){inputModifiers=event.modifiers();return keyPressed(event.key(),event.scancode(),event.modifiers());}
    @Override public boolean keyReleased(KeyEvent event){inputModifiers=event.modifiers();return super.keyReleased(event);}
    public boolean keyPressed(int key,int scan,int modifiers){inputModifiers=modifiers;return super.keyPressed(new KeyEvent(key,scan,modifiers));}
    @Override public boolean charTyped(CharacterEvent event){return event.codepoint()<=Character.MAX_VALUE?charTyped((char)event.codepoint(),inputModifiers):super.charTyped(event);}
    public boolean charTyped(char character,int modifiers){return super.charTyped(new CharacterEvent(character));}
    public static boolean hasControlDown(){return new KeyEvent(0,0,inputModifiers).hasControlDownWithQuirk();}
    public static boolean hasShiftDown(){return (inputModifiers&org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT)!=0;}
}
