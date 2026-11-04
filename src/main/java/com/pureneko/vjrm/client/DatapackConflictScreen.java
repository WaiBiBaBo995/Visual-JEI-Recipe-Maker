package com.pureneko.vjrm.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Both conflict resolutions require an explicit choice; closing cancels. */
final class DatapackConflictScreen extends Screen {
    private final Screen parent;
    private final String owner;
    private final Runnable replace,move;
    DatapackConflictScreen(Screen parent,String owner,Runnable replace,Runnable move){super(Component.translatable("vjrm.manage.conflict_title"));this.parent=parent;this.owner=owner;this.replace=replace;this.move=move;}
    @Override protected void init(){
        int w=Math.min(310,width-20),x=(width-w)/2,y=height/2;
        addRenderableWidget(Button.builder(Component.translatable("vjrm.manage.replace_owner",owner),b->{minecraft.setScreen(parent);replace.run();}).bounds(x,y,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable("vjrm.manage.move_here"),b->{minecraft.setScreen(parent);move.run();}).bounds(x,y+24,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(x,y+48,w,20).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){super.render(g,x,y,delta);g.drawCenteredString(font,title,width/2,height/2-50,0xFFFFFFFF);g.drawWordWrap(font,Component.translatable("vjrm.manage.conflict",owner),(width-Math.min(310,width-20))/2,height/2-34,Math.min(310,width-20),0xFFDDDDDD);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
