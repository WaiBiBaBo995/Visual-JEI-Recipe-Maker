package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;

final class JsDraftScreen extends Screen {
    private static final Gson PRETTY=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Screen parent;
    private final Consumer<GtRecipeScript.Draft> save;
    private String draft;
    private JsonEditBox box;
    private Button apply;
    private long validateAt;
    private Component status=Component.empty();
    private boolean valid;
    JsDraftScreen(Screen parent,String script,Consumer<GtRecipeScript.Draft> save){super(Component.translatable("vjrm.js"));this.parent=parent;this.save=save;draft=script;}
    @Override protected void init(){
        box=addRenderableWidget(new JsonEditBox(font,16,40,width-32,Math.max(30,height-110),title,true));box.setValue(draft);box.moveToStart();
        box.setValueListener(value->{draft=value;box.setIngredientIcons(java.util.List.of());validateAt=Util.getMillis()+300;valid=false;if(apply!=null)apply.active=false;});
        int bw=Math.max(36,Math.min(120,(width-44)/4)),gap=4;
        apply=addRenderableWidget(Button.builder(Component.translatable("vjrm.apply"),b->apply()).bounds(16,height-34,bw,20).build());
        addRenderableWidget(Button.builder(Component.translatable("vjrm.json.format"),b->format()).bounds(16+bw+gap,height-34,bw,20).build());
        addRenderableWidget(Button.builder(Component.translatable("vjrm.json.copy"),b->{minecraft.keyboardHandler.setClipboard(box.getValue());status=Component.translatable("vjrm.js.copied");}).bounds(16+2*(bw+gap),height-34,bw,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(16+3*(bw+gap),height-34,bw,20).build());
        validate();setInitialFocus(box);
    }
    private void validate(){
        validateAt=0;
        try{var parsed=GtRecipeScript.parse(draft);box.setIngredientIcons(IngredientAnnotations.read(GtRecipeScript.annotationSource(draft),parsed.json(),GtRecipeScript.dataOffset(draft),parent instanceof RecipeEditorScreen editor?editor.annotationFields():java.util.List.of()));valid=true;status=Component.translatable("vjrm.js.valid");}
        catch(RuntimeException ex){valid=false;status=Component.translatable("vjrm.error.invalid",Component.translatable(String.valueOf(ex.getMessage())));}
        apply.active=valid;
    }
    private void apply(){
        try{var json=GtRecipeScript.parse(draft);if(!GtRecipeSupport.isDocument(json.json()))throw new IllegalArgumentException("vjrm.js.type");new RecipeDocument(json.json()).decode(minecraft.level.registryAccess());save.accept(json);minecraft.setScreen(parent);}
        catch(RuntimeException ex){status=Component.translatable("vjrm.error.invalid",Component.translatable(String.valueOf(ex.getMessage())));}
    }
    private void format(){
        try{box.replaceText(GtRecipeScript.generate(GtRecipeScript.parse(draft).json(),GtRecipeScript.parse(draft).id()));validate();}
        catch(RuntimeException ex){status=Component.translatable("vjrm.error.invalid",Component.translatable(String.valueOf(ex.getMessage())));}
        setFocused(box);box.setFocused(true);
    }
    @Override public void tick(){if(validateAt>0&&Util.getMillis()>=validateAt)validate();}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(hasControlDown()&&key==GLFW.GLFW_KEY_S){apply();return true;}
        if(hasControlDown()&&hasShiftDown()&&key==GLFW.GLFW_KEY_F){format();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float pt){g.fill(0,0,width,height,0xEE151922);}
    @Override public void render(GuiGraphics g,int x,int y,float pt){
        super.render(g,x,y,pt);g.drawString(font,title,16,8,0xFFFFFF);
        g.drawString(font,font.plainSubstrByWidth(Component.translatable("vjrm.js.help").getString(),width-32),16,24,0xFFAAAAAA);
        int cursor=box.cursor();String before=draft.substring(0,Math.min(cursor,draft.length()));int line=1+(int)before.chars().filter(c->c==10).count(),column=cursor-before.lastIndexOf((char)10);
        String position=line+":"+column;g.drawString(font,position,width-16-font.width(position),8,0xFF9CDCFE);
        g.drawString(font,font.plainSubstrByWidth(status.getString(),width-32),16,height-60,valid?0xFFB5CEA8:0xFFF48771);
        if(y>=height-64&&y<height-40)g.renderTooltip(font,font.split(status,Math.max(80,width-40)),x,y);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
