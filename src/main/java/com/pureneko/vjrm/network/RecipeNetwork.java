package com.pureneko.vjrm.network;

import com.google.gson.JsonParser;
import com.pureneko.vjrm.export.DatapackExporter;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class RecipeNetwork {
    public static final com.pureneko.vjrm.platform.ConfigValue<Integer> PERMISSION=new com.pureneko.vjrm.platform.ConfigValue<>("vjrm-server.json","exportPermissionLevel",2);
    private static final Map<UUID,Long> LAST_EXPORT=new HashMap<>();
    public record ExportRequest(String id,String json,boolean overwrite) {
        public void encode(FriendlyByteBuf b){b.writeUtf(id,200);b.writeUtf(json,24000);b.writeBoolean(overwrite);}
        public static ExportRequest decode(FriendlyByteBuf b){return new ExportRequest(b.readUtf(200),b.readUtf(24000),b.readBoolean());}
    }
    public record ExportResult(boolean success,String detail) {
        public void encode(FriendlyByteBuf b){b.writeBoolean(success);b.writeUtf(detail,2048);}
        public static ExportResult decode(FriendlyByteBuf b){return new ExportResult(b.readBoolean(),b.readUtf(2048));}
    }
    public static boolean exportFits(String json){return json.length()<=24000&&json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=30000;}
    public static void sendToServer(Object message){VjrmNetworking.sendToServer(message);}
    static void reply(ServerPlayer player,Object message){VjrmNetworking.reply(player,message);}
    public static ExportResult processExport(ExportRequest request,ServerPlayer player){
        try{
            if(!player.hasPermissions(PERMISSION.get()))return new ExportResult(false,"permission");
            if(!exportFits(request.json()))return new ExportResult(false,"vjrm.error.size");
            long now=System.currentTimeMillis();Long previous=LAST_EXPORT.put(player.getUUID(),now);
            if(previous!=null&&now-previous<1000)return new ExportResult(false,"rate");
            var id=new ResourceLocation(request.id());var document=new RecipeDocument(JsonParser.parseString(request.json()).getAsJsonObject());document.decode(player.level().getServer().registryAccess(),id);
            var root=DatapackExporter.serverDatapackRoot(player.level().getServer(),"vjrm_generated").toAbsolutePath();DatapackExporter.writePackMeta(root);
            DatapackExporter.export(root,id.getNamespace(),id.getPath(),document.json(),request.overwrite());return new ExportResult(true,id.toString());
        }catch(Exception ex){com.mojang.logging.LogUtils.getLogger().warn("Failed to export recipe {}",request.id(),ex);String detail=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();return new ExportResult(false,detail.substring(0,Math.min(2000,detail.length())));}
    }
}
