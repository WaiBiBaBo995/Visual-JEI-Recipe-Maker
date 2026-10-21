package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import com.pureneko.vjrm.client.LegacyGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** A transaction: edits remain a draft until every field passes the recipe serializer. */
final class NumericFieldsScreen extends VjrmScreen {
    private final Screen parent;
    private final RecipeDocument target,draft;
    private final Runnable changed;
    private final Map<List<String>,String> edits=new HashMap<>();
    private final List<Button> labels=new ArrayList<>();
    private final List<EditBox> boxes=new ArrayList<>();
    private List<RecipeDocument.NumericField> fields=List.of();
    private String query="";
    private int scroll;
    private Component status=Component.translatable("vjrm.numbers.hint");
    NumericFieldsScreen(Screen parent,RecipeDocument document,Runnable changed){
        super(Component.translatable("vjrm.numbers.title"));this.parent=parent;target=document;draft=new RecipeDocument(document.json());this.changed=changed;
    }
    @Override protected void init(){
        labels.clear();boxes.clear();
        int x=Math.max(12,(width-540)/2),w=Math.min(540,width-24),valueWidth=Math.max(85,w/3);
        var search=addRenderableWidget(new EditBox(font,x,28,w,18,Component.translatable("vjrm.numbers.search")));
        search.setHint(Component.translatable("vjrm.numbers.search"));search.setValue(query);
        search.setResponder(s->{query=s;scroll=0;filter();});
        for(int i=0;i<Math.max(1,(height-116)/24);i++){
            int row=i;
            labels.add(addRenderableWidget(Button.builder(Component.empty(),b->{}).bounds(x,54+i*24,w-valueWidth-6,20).build()));
            var box=addRenderableWidget(new EditBox(font,x+w-valueWidth,54+i*24,valueWidth,20,Component.translatable("vjrm.numbers.value")));box.setMaxLength(128);
            box.setResponder(s->{int index=scroll+row;if(index<fields.size())edits.put(fields.get(index).path(),s);});boxes.add(box);
        }
        addRenderableWidget(Button.builder(Component.translatable("vjrm.apply"),b->apply()).bounds(x,height-50,100,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(x+104,height-50,100,20).build());
        filter();setInitialFocus(search);
    }
    private void filter(){
        fields=draft.numericFields().stream().filter(f->f.label().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        scroll=Math.max(0,Math.min(scroll,Math.max(0,fields.size()-boxes.size())));
        for(int i=0;i<boxes.size();i++){
            int index=scroll+i;var label=labels.get(i);var box=boxes.get(i);label.visible=box.visible=index<fields.size();
            if(label.visible){var field=fields.get(index);label.setMessage(Component.literal(font.plainSubstrByWidth(field.label(),label.getWidth()-10)));label.setTooltip(Tooltip.create(Component.literal(field.label()+" = "+field.value())));box.setValue(edits.getOrDefault(field.path(),field.value()));}
        }
        if(draft.numericFields().isEmpty())status=Component.translatable("vjrm.numbers.empty");
    }
    private void apply(){
        var candidate=new RecipeDocument(draft.json());
        try{
            candidate.setNumbers(edits,minecraft.level.registryAccess());target.replace(candidate.json());changed.run();minecraft.setScreen(parent);
        }catch(RuntimeException ex){status=Component.translatable("vjrm.error.invalid",ex.getMessage()!=null&&ex.getMessage().startsWith("vjrm.error.")?Component.translatable(ex.getMessage()):ex.getMessage());}
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll-=(int)Math.signum(dy)*3;filter();return true;}
    @Override public void renderBackground(LegacyGraphics g,int x,int y,float pt){g.fill(0,0,width,height,0xF0151922);}
    @Override public void render(LegacyGraphics g,int x,int y,float pt){super.render(g,x,y,pt);g.drawCenteredString(font,title,width/2,10,0xFFFFFF);g.drawString(font,font.plainSubstrByWidth(status.getString(),width-24),12,height-20,0xFFE3C483);if(y>=height-23)g.renderTooltip(font,font.split(status,width-30),x,y);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
