package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.recipe.RecipeDocument.Field;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Select real JSON nodes, including structures intentionally ignored by automatic detection. */
final class ManualFieldScreen extends Screen {
    private final Screen parent;
    private final RecipeDocument document;
    private final Consumer<Field> add;
    private final Consumer<List<String>> remove;
    private final List<JsonFieldPaths.Node> nodes;
    private final List<Button> rows=new ArrayList<>();
    private List<JsonFieldPaths.Node> filtered=List.of();
    private final List<String> kinds=new ArrayList<>(List.of("item","fluid","json"));
    private EditBox pathBox;
    private Button addButton,removeButton,kindButton,roleButton;
    private String query="",path="",kind="json";
    private boolean output;
    private int scroll;
    private Component status=Component.empty();
    ManualFieldScreen(Screen parent,RecipeDocument document,Field initial,Consumer<Field> add,Consumer<List<String>> remove){
        super(com.pureneko.vjrm.recipe.GtRecipeSupport.isDocument(document.json())?Component.translatable("vjrm.gt.manual_title"):t("title"));this.parent=parent;this.document=document;this.add=add;this.remove=remove;nodes=JsonFieldPaths.nodes(document.json());
        if(net.minecraftforge.fml.ModList.get().isLoaded("mekanism"))kinds.addAll(ChemicalSupport.unified()?List.of("chemical"):List.of("gas","infuse_type","pigment","slurry"));
        if(initial!=null){path="/"+initial.label();kind=initial.raw()?"json":!initial.chemical().isEmpty()?initial.chemical():initial.fluid()?"fluid":"item";output=initial.output();}
    }
    private static Component t(String key,Object... args){return Component.translatable("vjrm.manual."+key,args);}
    private int left(){return Math.max(8,(width-560)/2);}
    private int contentWidth(){return Math.min(560,width-16);}
    @Override protected void init(){
        rows.clear();int x=left(),w=contentWidth();
        var search=addRenderableWidget(new EditBox(font,x,28,w,20,t("search")));search.setHint(t("search"));search.setValue(query);
        search.setResponder(text->{query=text;scroll=0;filter();});
        for(int i=0;i<Math.max(1,(height-168)/22);i++){
            final int row=i;rows.add(addRenderableWidget(Button.builder(Component.empty(),b->{
                int index=scroll+row;if(index<filtered.size()){pathBox.setValue(filtered.get(index).pointer());setFocused(pathBox);}
            }).bounds(x,54+i*22,w,20).build()));
        }
        pathBox=addRenderableWidget(new EditBox(font,x,height-110,w,20,t("path")));pathBox.setMaxLength(4096);pathBox.setHint(t("path"));pathBox.setTooltip(Tooltip.create(t("help")));pathBox.setValue(path);
        pathBox.setResponder(text->{path=text;update();});
        kindButton=addRenderableWidget(Button.builder(Component.empty(),b->{kind=kinds.get((kinds.indexOf(kind)+1)%kinds.size());update();}).bounds(x,height-86,(w-4)/2,20).build());
        roleButton=addRenderableWidget(Button.builder(Component.empty(),b->{output=!output;update();}).bounds(x+(w-4)/2+4,height-86,w-(w-4)/2-4,20).build());
        int bw=(w-8)/3;
        addButton=addRenderableWidget(Button.builder(t("add"),b->{
            try{add.accept(field());minecraft.setScreen(parent);}catch(RuntimeException ex){status=Component.translatable("vjrm.error.invalid",Component.translatable(String.valueOf(ex.getMessage())));}
        }).bounds(x,height-30,bw,20).build());
        removeButton=addRenderableWidget(Button.builder(t("remove"),b->{remove.accept(JsonFieldPaths.parse(path));minecraft.setScreen(parent);}).bounds(x+bw+4,height-30,bw,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(x+2*(bw+4),height-30,w-2*(bw+4),20).build());
        filter();update();setInitialFocus(search);
    }
    private Field field(){
        var parts=JsonFieldPaths.parse(path);JsonFieldPaths.get(document.json(),parts);
        if(parts.equals(List.of("type")))throw new IllegalArgumentException("vjrm.error.field_path");
        return new Field(parts,kind.equals("fluid"),output,Set.of("item","fluid","json").contains(kind)?"":kind,kind.equals("json"));
    }
    private void update(){
        if(addButton==null)return;
        kindButton.setMessage(t("kind",t("kind."+kind)));kindButton.setTooltip(Tooltip.create(t(kind.equals("json")?"raw_help":"drag_help")));roleButton.setMessage(t(output?"output":"input"));
        try{
            var field=field();addButton.active=true;
            removeButton.active=document.manualFields().stream().anyMatch(f->f.path().equals(field.path()));
            status=t("selected","/"+field.label());
        }catch(RuntimeException ex){addButton.active=false;removeButton.active=false;status=t("hint");}
    }
    private void filter(){
        String search=query.toLowerCase(Locale.ROOT);filtered=nodes.stream().filter(n->(n.pointer()+" "+n.preview()).toLowerCase(Locale.ROOT).contains(search)).toList();
        scroll=Math.max(0,Math.min(scroll,Math.max(0,filtered.size()-rows.size())));
        for(int i=0;i<rows.size();i++){
            var button=rows.get(i);int index=scroll+i;button.visible=index<filtered.size();
            if(button.visible){var node=filtered.get(index);button.setMessage(Component.literal(font.plainSubstrByWidth(node.pointer()+" = "+node.preview(),button.getWidth()-10)));button.setTooltip(Tooltip.create(Component.literal(node.pointer()+"\n"+node.preview().substring(0,Math.min(1000,node.preview().length())))));}
        }
    }
    private boolean scroll(double amount){scroll-=(int)Math.signum(amount)*3;filter();return true;}
    @Override public boolean mouseScrolled(double x,double y,double dy){return scroll(dy);}
    @Override public void renderBackground(GuiGraphics g){g.fill(0,0,width,height,0xF0151922);}
    @Override public void render(GuiGraphics g,int x,int y,float pt){
        renderBackground(g);super.render(g,x,y,pt);int start=left(),w=contentWidth();
        g.drawString(font,title,start,9,0xFFFFFF);
        try{var node=JsonFieldPaths.get(document.json(),JsonFieldPaths.parse(path));g.drawString(font,font.plainSubstrByWidth(node.toString(),w),start,height-62,0xFFCE9178);}catch(RuntimeException ignored){}
        g.drawString(font,font.plainSubstrByWidth(status.getString(),w),start,height-48,addButton.active?0xFFB5CEA8:0xFFAAAAAA);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}

