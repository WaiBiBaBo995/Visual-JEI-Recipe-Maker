package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.export.*;
import com.pureneko.vjrm.network.DatapackManagementNetwork.Response;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
import java.io.*;

public final class DatapackManagementScreen extends Screen {
    private enum View { PACKS, ENTRIES, BACKUPS }
    private final Screen parent;
    private boolean world=true,busy;
    private View view=View.PACKS;
    private String pack=ManagedDatapacks.DEFAULT_PACK,selected="",query="";
    private JsonArray items=new JsonArray();
    private List<JsonObject> visible=List.of();
    private final List<Button> rows=new ArrayList<>(),actions=new ArrayList<>();
    private EditBox searchBox;
    private String lastClicked="";
    private long lastClick;
    private Component status=Component.empty();
    private int scroll,generation,left,top,panelWidth,panelHeight,listTop;
    private OutputStream download;
    private Path downloadTemp;
    public DatapackManagementScreen(Screen parent){super(Component.translatable("screen.vjrm.manage"));this.parent=parent;}
    private static Component t(String key,Object... args){return Component.translatable("vjrm.manage."+key,args);}
    private Button button(int x,int y,int w,Component text,Button.OnPress handler){return addRenderableWidget(Button.builder(text,handler).bounds(x,y,w,20).build());}
    @Override protected void init(){
        clearWidgets();rows.clear();actions.clear();
        panelWidth=Math.min(580,width-16);panelHeight=Math.min(420,height-16);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int tabWidth=(panelWidth-12)/4;
        button(left,top+22,tabWidth,t(world?"server":"client"),b->{world=!world;view=View.PACKS;query="";selected="";rebuild();});
        button(left+tabWidth+4,top+22,tabWidth,t("refresh"),b->refresh(false));
        button(left+(tabWidth+4)*2,top+22,tabWidth,t("backups"),b->{view=View.BACKUPS;query="";selected="";rebuild();});
        button(left+(tabWidth+4)*3,top+22,tabWidth,t("back"),b->onClose());
        searchBox=addRenderableWidget(new EditBox(font,left,top+58,panelWidth,18,t("search")));
        searchBox.setMaxLength(200);searchBox.setValue(query);searchBox.setHint(t("search"));searchBox.setResponder(text->{query=text;scroll=0;filter();});
        switch(view){
            case PACKS->{
                action(0,"rename",()->nameDialog("rename"));action(1,"delete_pack",this::deletePack);action(2,"export",this::exportPack);
            }
            case ENTRIES->{
                action(0,"edit",this::editEntry);action(1,"remove",this::removeEntry);action(2,"move",()->nameDialog("move"));action(3,"export",this::exportPack);
            }
            case BACKUPS->{action(0,"restore",()->nameDialog("restore"));action(1,"purge",this::purge);}
        }
        listTop=top+110;int count=Math.max(1,(panelHeight-180)/23);
        for(int i=0;i<count;i++){int index=i;rows.add(button(left,listTop+i*23,panelWidth,Component.empty(),b->select(scroll+index)));}
        refresh(false);setInitialFocus(searchBox);
    }
    private void action(int index,String key,Runnable run){
        int columns=view==View.ENTRIES?4:view==View.PACKS?3:2,w=(panelWidth-4*(columns-1))/columns;
        actions.add(button(left+index*(w+4),top+82,w,t(key),b->{if(!busy)run.run();}));
    }
    private void rebuild(){scroll=0;lastClicked="";lastClick=0;items=new JsonArray();status=Component.empty();init(minecraft,width,height);}
    private void showPacks(){view=View.PACKS;selected="";query="";rebuild();}
    private void refresh(boolean keepStatus){
        if(!keepStatus)status=t("loading");busy=true;int current=++generation;items=new JsonArray();filter();fetch(0,current);
    }
    private void fetch(int offset,int current){
        String action=switch(view){case PACKS->"packs";case ENTRIES->"entries";case BACKUPS->"backups";};
        call(world,action,pack,Integer.toString(offset),"","",false,response->{
            if(current!=generation)return;
            if(!response.success()){failed(response);return;}
            var result=JsonParser.parseString(response.json()).getAsJsonObject();
            result.getAsJsonArray("items").forEach(items::add);int next=result.get("next").getAsInt();filter();
            if(next>=0){fetch(next,current);return;}
            busy=false;if(status.getString().isEmpty()||status.equals(t("loading")))status=t("ready",items.size());filter();
        });
    }
    private void call(boolean server,String action,String pack,String value,String id,String json,boolean overwrite,java.util.function.Consumer<Response> callback){
        DatapackManagementClient.request(this,server,action,pack,value,id,json,overwrite,callback);
    }
    private void failed(Response response){busy=false;status=t("error",DatapackManagementClient.error(response.detail()));filter();}
    private void mutate(String action,String target,String value,String id,boolean replace){
        busy=true;++generation;status=t("loading");filter();
        call(world,action,target,value,id,"",replace,response->{
            if(!response.success()){
                failed(response);return;
            }
            status=t("reload_hint");
            if(action.equals("rename")){pack=value;selected="";}
            if(action.equals("delete")){selected="";}
            var result=JsonParser.parseString(response.json()).getAsJsonObject();
            if(result.has("priority")&&!result.get("priority").getAsBoolean())status=t("priority_failed");
            refresh(true);
        });
    }
    private JsonObject selection(){return visible.stream().filter(o->key(o).equals(selected)).findFirst().orElse(null);}
    private static String key(JsonObject item){return item.has("id")?item.get("id").getAsString():item.get("name").getAsString();}
    private void select(int index){
        if(index<0||index>=visible.size())return;
        String key=key(visible.get(index));long now=net.minecraft.Util.getMillis();
        boolean doubleClick=key.equals(lastClicked)&&now-lastClick<=250;
        selected=key;lastClicked=key;lastClick=now;
        if(view==View.PACKS)pack=selected;
        if(doubleClick&&view==View.PACKS){view=View.ENTRIES;selected="";query="";rebuild();}
        else if(doubleClick&&view==View.ENTRIES){lastClicked="";lastClick=0;editEntry();}
        else filter();
    }
    private void nameDialog(String action){
        if(selection()==null)return;
        String target=selected,id=action.equals("move")?selected:"",initial="";
        if(action.equals("rename"))initial=selected;
        if(action.equals("restore")){int end=selected.lastIndexOf('_');end=end<0?-1:selected.lastIndexOf('_',end-1);initial=end<0?selected:selected.substring(0,end);}
        if(action.equals("move"))target=pack;
        minecraft.setScreen(new DatapackNameScreen(this,world,action,target,id,initial,name->{
            if(action.equals("rename"))pack=name;
            selected="";status=t("reload_hint");refresh(true);
        }));
    }
    private void deletePack(){
        var item=selection();if(item==null)return;String target=selected;
        minecraft.setScreen(new ConfirmScreen(ok->{minecraft.setScreen(this);if(ok)mutate("delete",target,"","",false);},t("confirm_delete",target,item.get("recipes").getAsInt(),item.get("disabled").getAsInt()),t("confirm_delete_body")));
    }
    private void purge(){if(selected.isEmpty())return;String key=selected;minecraft.setScreen(new ConfirmScreen(ok->{minecraft.setScreen(this);if(ok)mutate("purge",key,"","",false);},t("purge_title"),t("purge_body",key)));}
    private void removeEntry(){
        var entry=selection();if(entry==null)return;String id=selected;
        minecraft.setScreen(new ConfirmScreen(ok->{minecraft.setScreen(this);if(ok)mutate("remove",pack,"",id,false);},t("remove_title",id),t(entry.get("kind").getAsString().equals("recipe")?"remove_body":"restore_body")));
    }
    private void editEntry(){
        var entry=selection();if(entry==null||entry.get("kind").getAsString().equals("disabled")){status=t("restore_first");return;}
        String id=selected,target=pack;int current=++generation;busy=true;filter();
        call(world,"read",target,"",id,"",false,response->{
            if(current!=generation)return;
            busy=false;filter();
            if(!response.success()){failed(response);return;}
            try{minecraft.setScreen(new RecipeEditorScreen(new RecipeDocument(JsonParser.parseString(response.json()).getAsJsonObject()),null,id,this).targetPack(target,true,entry.has("source")?entry.get("source").getAsString():""));}
            catch(Exception ex){status=t("error",ex.getMessage());}
        });
    }
    private void exportPack(){
        String target=view==View.PACKS?selected:pack;if(target.isEmpty())return;
        Path dest=DatapackManagementClient.exportRoot().resolve("exports").resolve(target+".zip");
        Runnable export=()->{busy=true;status=t("loading");filter();call(world,"export",target,"","","",false,response->{
            if(!response.success()){failed(response);return;}
            try{
                Files.createDirectories(dest.getParent());downloadTemp=Files.createTempFile(dest.getParent(),".vjrm-",".part");download=Files.newOutputStream(downloadTemp);download(target,0,dest);
            }catch(IOException ex){abortDownload();busy=false;status=t("error",ex.getMessage());}
        });};
        if(Files.exists(dest))minecraft.setScreen(new ConfirmScreen(ok->{minecraft.setScreen(this);if(ok)export.run();},t("export_exists"),Component.literal(dest.toString())));else export.run();
    }
    private void download(String target,long offset,Path dest){
        call(world,"download",target,Long.toString(offset),"","",false,response->{
            if(!response.success()){abortDownload();failed(response);return;}
            try{
                var data=JsonParser.parseString(response.json()).getAsJsonObject();download.write(Base64.getDecoder().decode(data.get("data").getAsString()));
                long next=data.get("next").getAsLong(),size=data.get("size").getAsLong();
                if(next<size){download(target,next,dest);return;}
                download.close();download=null;Files.move(downloadTemp,dest,StandardCopyOption.REPLACE_EXISTING);downloadTemp=null;busy=false;status=t("exported",dest.toString());filter();
            }catch(Exception ex){abortDownload();busy=false;status=t("error",ex.getMessage());filter();}
        });
    }
    private void abortDownload(){try{if(download!=null)download.close();if(downloadTemp!=null)Files.deleteIfExists(downloadTemp);}catch(IOException ignored){}download=null;downloadTemp=null;}
    private void filter(){
        String q=query.toLowerCase(Locale.ROOT);visible=new ArrayList<>();
        for(var element:items){var item=element.getAsJsonObject();if(item.toString().toLowerCase(Locale.ROOT).contains(q))visible.add(item);}
        scroll=Math.max(0,Math.min(scroll,Math.max(0,visible.size()-rows.size())));
        for(int i=0;i<rows.size();i++){
            Button b=rows.get(i);int index=scroll+i;b.visible=index<visible.size();b.active=!busy;if(!b.visible)continue;
            var item=visible.get(index);String label=key(item);
            if(item.has("recipes"))label+="  ·  "+t("counts",item.get("recipes").getAsInt(),item.get("disabled").getAsInt()).getString();
            if(item.has("kind"))label+="  ·  "+t("kind."+item.get("kind").getAsString()).getString();
            if(key(item).equals(selected))label="> "+label;
            b.setMessage(Component.literal(font.plainSubstrByWidth(label,b.getWidth()-10)));
            b.setTooltip(Tooltip.create(Component.literal(key(item)+(item.has("source")&&!item.get("source").getAsString().isEmpty()?"\n"+item.get("source").getAsString():""))));
        }
        for(Button b:actions)b.active=!busy;
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        super.render(g,x,y,delta);g.drawCenteredString(font,title,width/2,top+4,0xFFFFFFFF);
        g.drawString(font,t("search"),left,top+47,0xFFBBBBBB);
        if(view==View.ENTRIES)g.drawWordWrap(font,t("target",pack),left,top+panelHeight-70,panelWidth,0xFFE3C483);
        g.drawWordWrap(font,status,left,top+panelHeight-53,panelWidth,0xFFEEEEEE);
        g.drawWordWrap(font,t("view."+view.name().toLowerCase(Locale.ROOT)),left,top+panelHeight-27,panelWidth,0xFFAAAAAA);
    }
    @Override public boolean mouseScrolled(double x,double y,double dy){scroll-=(int)Math.signum(dy)*3;filter();return true;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){abortDownload();if(view!=View.PACKS)showPacks();else minecraft.setScreen(parent);}
    @Override public void removed(){abortDownload();}
}
