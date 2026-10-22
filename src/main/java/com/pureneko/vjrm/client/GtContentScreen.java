package com.pureneko.vjrm.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Select an item/fluid and optional per-tick channel before appending a GT capability entry. */
final class GtContentScreen extends Screen {
    record Addition(boolean fluid,boolean perTick,String value,String amount){}
    private final Screen parent;
    private final Consumer<Addition> save;
    private boolean fluid,perTick;
    private String value,amount;
    private Component status=Component.empty();
    GtContentScreen(Screen parent,boolean output,boolean fluid,String value,String amount,Consumer<Addition> save){
        super(Component.translatable(output?"vjrm.gt.add_output":"vjrm.gt.add_input"));this.parent=parent;this.fluid=fluid;this.value=value;this.amount=amount;this.save=save;
    }
    protected void init(){
        int x=width/2-140,y=height/2-55;
        var input=addRenderableWidget(new EditBox(font,x,y,280,20,Component.translatable("vjrm.value")));input.setMaxLength(200);input.setValue(value);input.setResponder(v->value=v);
        var count=addRenderableWidget(new EditBox(font,x,y+25,280,20,Component.translatable("vjrm.amount")));count.setMaxLength(10);count.setValue(amount);count.setResponder(v->amount=v);
        addRenderableWidget(Button.builder(kind(),b->{fluid=!fluid;b.setMessage(kind());if(value.isBlank()||value.equals("minecraft:stone")||value.equals("minecraft:water"))input.setValue(fluid?"minecraft:water":"minecraft:stone");}).bounds(x,y+50,138,20).build());
        addRenderableWidget(Button.builder(tickLabel(),b->{perTick=!perTick;b.setMessage(tickLabel());}).bounds(x+142,y+50,138,20).build());
        addRenderableWidget(Button.builder(Component.translatable("vjrm.apply"),b->{try{save.accept(new Addition(fluid,perTick,value,amount));minecraft.setScreen(parent);}catch(RuntimeException ex){status=Component.translatable("vjrm.error.invalid",Component.translatable(String.valueOf(ex.getMessage())));}}).bounds(x,y+75,138,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(x+142,y+75,138,20).build());setInitialFocus(input);
    }
    private Component kind(){return Component.translatable(fluid?"vjrm.manual.kind.fluid":"vjrm.manual.kind.item");}
    private Component tickLabel(){return Component.translatable(perTick?"vjrm.gt.per_tick":"vjrm.gt.once");}
    @Override public void render(GuiGraphics g,int x,int y,float pt){
        g.fill(0,0,width,height,0xF0151922);super.render(g,x,y,pt);g.drawCenteredString(font,title,width/2,height/2-80,0xFFFFFF);g.drawCenteredString(font,status,width/2,height/2+55,0xFFF48771);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
