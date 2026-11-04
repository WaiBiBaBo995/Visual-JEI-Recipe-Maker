package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.export.*;
import com.pureneko.vjrm.network.DatapackManagementNetwork;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import com.pureneko.vjrm.network.RecipeNetwork;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public final class DatapackManagementClient {
    private record Pending(Screen owner,long until,Consumer<DatapackManagementNetwork.Response> callback){}
    private static final Map<Integer,Pending> pending=new HashMap<>();
    private static int sequence;
    private DatapackManagementClient(){}
    public static Path exportRoot(){return Minecraft.getInstance().gameDirectory.toPath().resolve("vjrm_exports");}
    public static ManagedDatapacks local() throws java.io.IOException {
        return new ManagedDatapacks(exportRoot(),Minecraft.getInstance().gameDirectory.toPath().resolve("config/vjrm/backups"));
    }
    public static boolean available(){return Minecraft.getInstance().getConnection()!=null&&net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(DatapackManagementNetwork.CHANNEL);}
    public static void request(Screen owner,boolean world,String action,String pack,String value,String id,String json,boolean overwrite,Consumer<DatapackManagementNetwork.Response> callback){
        int token=++sequence;
        if(world){
            if(!available()){callback.accept(new DatapackManagementNetwork.Response(token,false,"vjrm.manage.no_server","{}"));return;}
            pending.put(token,new Pending(owner,System.currentTimeMillis()+20000,callback));
            var request=new DatapackManagementNetwork.Request(token,action,pack,value,id,json,overwrite);var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();request.encode(b);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(DatapackManagementNetwork.CHANNEL,b);return;
        }
        try{
            String source="";
            if(action.startsWith("save")){
                var document=new RecipeDocument(JsonParser.parseString(json).getAsJsonObject());
                document.decode(Minecraft.getInstance().level.registryAccess());
                source=owner instanceof RecipeEditorScreen editor?editor.overrideSource():"";
            }
            var result=DatapackOperations.execute(local(),action,pack,value,id,json,overwrite,source);
            callback.accept(new DatapackManagementNetwork.Response(token,true,action,result.toString()));
        }catch(Exception ex){
            var data=new JsonObject();String detail=String.valueOf(ex.getMessage());
            if(ex instanceof ManagedDatapacks.Conflict conflict){detail="conflict";data.addProperty("pack",conflict.pack);}
            if(ex instanceof FileAlreadyExistsException)detail="exists";
            callback.accept(new DatapackManagementNetwork.Response(token,false,detail,data.toString()));
        }
    }
    public static void receive(DatapackManagementNetwork.Response response){
        var p=pending.remove(response.token());
        if(p!=null&&Minecraft.getInstance().screen==p.owner)p.callback.accept(response);
    }
    public static void tick(){
        long now=System.currentTimeMillis();var screen=Minecraft.getInstance().screen;
        var expired=pending.entrySet().stream().filter(e->e.getValue().owner!=screen||e.getValue().until<now).toList();
        for(var e:expired){pending.remove(e.getKey());if(e.getValue().owner==screen)e.getValue().callback.accept(new DatapackManagementNetwork.Response(e.getKey(),false,"vjrm.error.source_timeout","{}"));}
    }
    public static net.minecraft.network.chat.Component error(String detail){
        String key=switch(detail){case "permission"->"vjrm.result.permission";case "rate"->"vjrm.result.rate";case "exists"->"vjrm.manage.exists";default->detail;};
        return key.startsWith("vjrm.")?net.minecraft.network.chat.Component.translatable(key):net.minecraft.network.chat.Component.literal(key);
    }
}
