package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import com.pureneko.vjrm.client.LegacyGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Searchable, scrollable choices; modded serializers use an actual datapack recipe as their template. */
final class RecipeTypeScreen extends VjrmScreen {
    private record Entry(String label,String search,RecipePreview.Category category,Runnable choose) { }
    private final RecipeEditorScreen parent;
    private final List<Entry> entries=new ArrayList<>();
    private List<Entry> filtered=List.of();
    private final List<Button> rows=new ArrayList<>();
    private String query="";
    private int scroll;
    private boolean requested;
    private Component status=Component.translatable("vjrm.types.loading");
    RecipeTypeScreen(RecipeEditorScreen parent){super(Component.translatable("vjrm.type"));this.parent=parent;}
    @Override protected void init(){
        rows.clear();
        int x=Math.max(8,(width-380)/2),w=Math.min(380,width-16);
        var search=addRenderableWidget(new EditBox(font,x,28,w,20,Component.translatable("vjrm.types.search")));
        search.setHint(Component.translatable("vjrm.types.search"));search.setValue(query);search.setResponder(s->{query=s;scroll=0;filter();});
        for(int i=0;i<Math.max(1,(height-86)/24);i++){
            final int row=i;
            rows.add(addRenderableWidget(Button.builder(Component.empty(),b->{int index=scroll+row;if(index<filtered.size())filtered.get(index).choose.run();}).bounds(x,54+i*24,w,20).build()));
        }
        setInitialFocus(search);
        if(!requested){
            requested=true;
            for(String type:RecipeDocument.TYPES){
                var document=RecipeDocument.template(type);
                if(minecraft.level!=null)RecipePreview.layout(document.decode(minecraft.level.registryAccess()),null,document).ifPresent(layout->{
                    String label=RecipePreview.typeLabel("minecraft:"+type,layout.getRecipeCategory()).getString();
                    entries.add(new Entry(label,label.toLowerCase(Locale.ROOT),layout.getRecipeCategory(),()->{parent.loadTemplate(document,null,null);minecraft.setScreen(parent);}));
                });
            }
            RecipeSources.catalog(samples->{
                for(var template:RecipePreview.templates(samples)){
                    if(template.category().getRecipeType().getUid().getNamespace().equals("minecraft") && RecipeDocument.TYPES.stream().anyMatch(t->template.serializer().equals("minecraft:"+t)))continue;
                    entries.add(new Entry(template.label(),template.label().toLowerCase(Locale.ROOT),template.category(),()->{
                        status=Component.translatable("vjrm.types.loading");
                        RecipeSources.request(template.id(),document->{if(minecraft.screen==this){parent.loadTemplate(document,template.category(),template.display());minecraft.setScreen(parent);}},error->status=Component.translatable(error));
                    }));
                }
                entries.sort(Comparator.comparing(Entry::label));status=Component.translatable("vjrm.types.hint");filter();
            },error->status=Component.translatable(error));
        }
        filter();
    }
    private void filter(){
        filtered=entries.stream().filter(e->e.search.contains(query.toLowerCase(Locale.ROOT))).toList();
        scroll=Math.max(0,Math.min(scroll,Math.max(0,filtered.size()-rows.size())));
        for(int i=0;i<rows.size();i++){
            var button=rows.get(i);int index=scroll+i;button.visible=index<filtered.size();
            if(button.visible){String label=filtered.get(index).label;button.setMessage(Component.literal(font.plainSubstrByWidth(label,button.getWidth()-48)));button.setTooltip(Tooltip.create(Component.literal(label)));}
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll-=Integer.signum((int)Math.signum(dy))*3;filter();return true;}
    @Override public void renderBackground(LegacyGraphics g,int x,int y,float pt){g.fill(0,0,width,height,0xF01B2029);}
    @Override public void render(LegacyGraphics g,int x,int y,float pt){
        super.render(g,x,y,pt);g.drawCenteredString(font,title,width/2,10,0xFFFFFF);
        g.drawString(font,font.plainSubstrByWidth(status.getString(),width-24),12,height-24,0xFFFFFF);
        for(int i=0;i<rows.size()&&scroll+i<filtered.size();i++){
            var button=rows.get(i);if(!button.visible)continue;var entry=filtered.get(scroll+i);
            RecipePreview.drawCategoryIcon(g,entry.category,button.getX()+3,button.getY()+2);
        }
        if(filtered.size()>rows.size())g.drawString(font,(scroll+1)+"–"+Math.min(scroll+rows.size(),filtered.size())+" / "+filtered.size(),width-100,height-12,0xAAAAAA);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
