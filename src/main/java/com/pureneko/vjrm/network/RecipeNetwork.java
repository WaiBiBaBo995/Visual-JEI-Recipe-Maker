package com.pureneko.vjrm.network;

import com.google.gson.JsonParser;
import com.pureneko.vjrm.export.DatapackExporter;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

public final class RecipeNetwork {
    private static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue PERMISSION=BUILDER.defineInRange("exportPermissionLevel",2,0,4);
    public static final ForgeConfigSpec SPEC=BUILDER.build();
    public static final SimpleChannel CHANNEL=NetworkRegistry.ChannelBuilder.named(new ResourceLocation("vjrm","main"))
        .networkProtocolVersion(()->"1")
        .clientAcceptedVersions(v->v.equals("1")||v.equals(NetworkRegistry.ABSENT)||v.equals(NetworkRegistry.ACCEPTVANILLA))
        .serverAcceptedVersions(v->v.equals("1")||v.equals(NetworkRegistry.ABSENT)||v.equals(NetworkRegistry.ACCEPTVANILLA)).simpleChannel();
    private static final Map<UUID,Long> LAST_EXPORT=new HashMap<>();
    public record ExportRequest(String id,String json,boolean overwrite) {
        public void encode(FriendlyByteBuf b){b.writeUtf(id,200);b.writeUtf(json,24000);b.writeBoolean(overwrite);}
        public static ExportRequest decode(FriendlyByteBuf b){return new ExportRequest(b.readUtf(200),b.readUtf(24000),b.readBoolean());}
    }
    public record ExportResult(boolean success,String detail) {
        public void encode(FriendlyByteBuf b){b.writeBoolean(success);b.writeUtf(detail,2048);}
        public static ExportResult decode(FriendlyByteBuf b){return new ExportResult(b.readBoolean(),b.readUtf(2048));}
    }
    public static void register(){
        CHANNEL.messageBuilder(ExportRequest.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(ExportRequest::encode).decoder(ExportRequest::decode).consumerMainThread((p,c)->handleExport(p,c.get())).add();
        CHANNEL.messageBuilder(ExportResult.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(ExportResult::encode).decoder(ExportResult::decode).consumerMainThread((p,c)->com.pureneko.vjrm.client.ExportResultHandler.handle(p)).add();
        RecipeSourceNetwork.register();
    }
    public static boolean exportFits(String json){return json.length()<=24000&&json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=30000;}
    public static void sendToServer(Object message){CHANNEL.sendToServer(message);}
    static void reply(ServerPlayer player,Object message){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);}
    public static void handleExport(ExportRequest request,NetworkEvent.Context context){
        var player=context.getSender();if(player==null)return;
        reply(player,processExport(request,player));
    }
    public static ExportResult processExport(ExportRequest request,ServerPlayer player){
        try{
            if(!player.hasPermissions(PERMISSION.get()))return new ExportResult(false,"permission");
            if(!exportFits(request.json()))return new ExportResult(false,"vjrm.error.size");
            long now=System.currentTimeMillis();Long previous=LAST_EXPORT.put(player.getUUID(),now);
            if(previous!=null&&now-previous<1000)return new ExportResult(false,"rate");
            var id=new ResourceLocation(request.id());var submitted=JsonParser.parseString(request.json()).getAsJsonObject();
            if(submitted.has("vjrm:script")){
                if(!com.pureneko.vjrm.recipe.GtRecipeSupport.kubeLoaded())throw new IllegalArgumentException("vjrm.gt.kube_required");
                String script=submitted.get("vjrm:script").getAsString();var draft=com.pureneko.vjrm.recipe.GtRecipeScript.parse(script);
                if(!draft.id().equals(id.toString())||!com.pureneko.vjrm.recipe.GtRecipeSupport.isDocument(draft.json()))throw new IllegalArgumentException("vjrm.js.type");
                new RecipeDocument(draft.json()).decode(player.server.registryAccess());
                com.pureneko.vjrm.export.ScriptExporter.export(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get(),id.getNamespace(),id.getPath(),script,request.overwrite());
                return new ExportResult(true,id.toString());
            }
            var document=new RecipeDocument(submitted);document.decode(player.server.registryAccess(),id);
            var root=DatapackExporter.serverDatapackRoot(player.server,"vjrm_generated").toAbsolutePath();DatapackExporter.writePackMeta(root);
            DatapackExporter.export(root,id.getNamespace(),id.getPath(),document.json(),request.overwrite());return new ExportResult(true,id.toString());
        }catch(Exception ex){com.mojang.logging.LogUtils.getLogger().warn("Failed to export recipe {}",request.id(),ex);String detail=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();return new ExportResult(false,detail.substring(0,Math.min(2000,detail.length())));}
    }
}
