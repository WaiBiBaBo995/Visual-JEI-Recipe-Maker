package com.pureneko.vjrm.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

abstract class LegacyScrollWidget extends AbstractTextAreaWidget {
    LegacyScrollWidget(int x,int y,int width,int height,Component title){super(x,y,width,height,title,AbstractScrollArea.defaultSettings(4));}
    @Override protected final void extractContents(GuiGraphicsExtractor graphics,int x,int y,float delta){renderContents(new LegacyGraphics(graphics),x,y,delta);}
    protected abstract void renderContents(LegacyGraphics graphics,int x,int y,float delta);
    protected boolean withinContentAreaPoint(double x,double y){return x>=getX()&&x<getRight()&&y>=getY()&&y<getBottom();}
    @Override public boolean keyPressed(KeyEvent event){VjrmScreen.inputModifiers=event.modifiers();return keyPressed(event.key(),event.scancode(),event.modifiers());}
    public boolean keyPressed(int key,int scan,int modifiers){return super.keyPressed(new KeyEvent(key,scan,modifiers));}
    @Override public boolean charTyped(CharacterEvent event){return event.codepoint()<=Character.MAX_VALUE&&charTyped((char)event.codepoint(),VjrmScreen.inputModifiers);}
    public boolean charTyped(char c,int modifiers){return super.charTyped(new CharacterEvent(c));}
    private static MouseButtonEvent mouse(double x,double y,int b){return new MouseButtonEvent(x,y,new MouseButtonInfo(b,VjrmScreen.inputModifiers));}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){VjrmScreen.inputModifiers=event.modifiers();return mouseClicked(event.x(),event.y(),event.button());}
    public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(mouse(x,y,b),false);}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){return mouseDragged(event.x(),event.y(),event.button(),dx,dy);}
    public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged(mouse(x,y,b),dx,dy);}
}
