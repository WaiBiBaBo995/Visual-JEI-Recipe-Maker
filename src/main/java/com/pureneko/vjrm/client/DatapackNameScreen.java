package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.export.ManagedDatapacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

/** Owns its request so failed validation keeps the same input and dialog open. */
final class DatapackNameScreen extends Screen {
    private final Screen parent;
    private final boolean world;
    private final String action,pack,id;
    private final Consumer<String> success;
    private final Set<String> existing=new HashSet<>();
    private String value;
    private EditBox input;
    private Button confirm;
    private Component error=Component.empty();
    private boolean busy,loaded,loadingNames;
    private int left,top,boxWidth,epoch;
    DatapackNameScreen(Screen parent,boolean world,String action,String pack,String id,String initial,Consumer<String> success){
        super(Component.translatable("vjrm.manage.name_title."+action));this.parent=parent;this.world=world;this.action=action;this.pack=pack;this.id=id;this.value=initial;this.success=success;
    }
    @Override protected void init(){
        boxWidth=Math.min(340,width-24);left=(width-boxWidth)/2;top=Math.max(8,height/2-65);
        input=addRenderableWidget(new EditBox(font,left,top+27,boxWidth,20,Component.translatable("vjrm.manage.name")));
        input.setMaxLength(64);input.setValue(value);input.setHint(Component.translatable("vjrm.manage.name"));
        input.setResponder(text->{value=text;error=Component.empty();update();});
        confirm=addRenderableWidget(Button.builder(Component.translatable("gui.ok"),b->submit()).bounds(left,top+103,(boxWidth-6)/2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(left+(boxWidth+6)/2,top+103,(boxWidth-6)/2,20).build());
        setInitialFocus(input);update();
        if(action.equals("move")&&!loaded)loadNames(0,++epoch);
    }
    private void loadNames(int offset,int current){
        loadingNames=true;update();
        DatapackManagementClient.request(this,world,"packs","",Integer.toString(offset),"","",false,response->{
            if(current!=epoch)return;
            if(!response.success()){loadingNames=false;error=DatapackManagementClient.error(response.detail());update();return;}
            var page=JsonParser.parseString(response.json()).getAsJsonObject();
            for(var item:page.getAsJsonArray("items"))existing.add(item.getAsJsonObject().get("name").getAsString());
            int next=page.get("next").getAsInt();if(next>=0)loadNames(next,current);else{loaded=true;loadingNames=false;update();}
        });
    }
    private void update(){
        if(confirm!=null){confirm.active=!busy&&!loadingNames;confirm.setMessage(Component.translatable(action.equals("move")&&!loaded&&!loadingNames?"vjrm.manage.retry_names":"gui.ok"));}
        if(input!=null)input.setEditable(!busy);
    }
    private void submit(){
        if(busy||loadingNames)return;
        if(action.equals("move")&&!loaded){error=Component.empty();existing.clear();loadNames(0,++epoch);return;}
        String name=value.trim();
        if(!ManagedDatapacks.validName(name)){error=Component.translatable("vjrm.manage.invalid_name");return;}
        busy=true;error=Component.empty();update();
        DatapackManagementClient.request(this,world,action,pack,name,id,"",false,response->{
            busy=false;update();
            if(!response.success()){
                if(response.detail().equals("conflict"))error=Component.translatable("vjrm.manage.conflict",JsonParser.parseString(response.json()).getAsJsonObject().get("pack").getAsString());
                else error=DatapackManagementClient.error(response.detail());
                return;
            }
            minecraft.setScreen(parent);success.accept(name);
        });
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&confirm.active){submit();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        super.render(g,x,y,delta);g.drawCenteredString(font,title,width/2,top+5,0xFFFFFFFF);
        Component hint=Component.translatable("vjrm.manage.name_help."+action);
        if(action.equals("move")&&loaded&&ManagedDatapacks.validName(value.trim())&&!existing.contains(value.trim()))hint=Component.translatable("vjrm.manage.move_create",value.trim());
        g.drawWordWrap(font,error.getString().isEmpty()?hint:error,left,top+55,boxWidth,error.getString().isEmpty()?0xFFBBBBBB:0xFFFFAA88);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){if(!busy)minecraft.setScreen(parent);}
}
