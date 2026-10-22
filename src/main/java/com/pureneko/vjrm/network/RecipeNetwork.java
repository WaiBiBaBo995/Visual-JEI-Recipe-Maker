package com.pureneko.vjrm.network;

import com.google.gson.JsonParser;
import com.pureneko.vjrm.export.DatapackExporter;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;

@EventBusSubscriber(modid="vjrm", bus=EventBusSubscriber.Bus.MOD)
public final class RecipeNetwork {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue PERMISSION=BUILDER.defineInRange("exportPermissionLevel",2,0,4);
    public static final ModConfigSpec SPEC=BUILDER.build();
    private static final Map<UUID,Long> LAST_EXPORT=new HashMap<>();
    public record ExportRequest(String id,String json,boolean overwrite) implements CustomPacketPayload {
        public static final Type<ExportRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("vjrm","export"));
        public static final StreamCodec<RegistryFriendlyByteBuf,ExportRequest> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.id,200);b.writeUtf(p.json,24000);b.writeBoolean(p.overwrite);},b->new ExportRequest(b.readUtf(200),b.readUtf(24000),b.readBoolean()));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record ExportResult(boolean success,String detail) implements CustomPacketPayload {
        public static final Type<ExportResult> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("vjrm","export_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf,ExportResult> CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.success);b.writeUtf(p.detail,2048);},b->new ExportResult(b.readBoolean(),b.readUtf(2048)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("1").optional();
        registrar.playToServer(ExportRequest.TYPE,ExportRequest.CODEC,RecipeNetwork::handleExport);
        registrar.playToClient(ExportResult.TYPE,ExportResult.CODEC,(payload,context)->com.pureneko.vjrm.client.ExportResultHandler.handle(payload,context));
    }
    public static void handleExport(ExportRequest request,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        try{
            if(!player.hasPermissions(PERMISSION.get())){context.reply(new ExportResult(false,"permission"));return;}
            long now=System.currentTimeMillis();Long previous=LAST_EXPORT.put(player.getUUID(),now);
            if(previous!=null && now-previous<1000){context.reply(new ExportResult(false,"rate"));return;}
            ResourceLocation id=ResourceLocation.parse(request.id());
            var submitted=JsonParser.parseString(request.json()).getAsJsonObject();
            if(submitted.has("vjrm:script")){
                if(!com.pureneko.vjrm.recipe.GtRecipeSupport.kubeLoaded())throw new IllegalArgumentException("vjrm.gt.kube_required");
                String script=submitted.get("vjrm:script").getAsString();var draft=com.pureneko.vjrm.recipe.GtRecipeScript.parse(script);
                if(!draft.id().equals(id.toString())||!com.pureneko.vjrm.recipe.GtRecipeSupport.isDocument(draft.json()))throw new IllegalArgumentException("vjrm.js.type");
                new RecipeDocument(draft.json()).decode(player.registryAccess());
                com.pureneko.vjrm.export.ScriptExporter.export(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get(),id.getNamespace(),id.getPath(),script,request.overwrite());
                context.reply(new ExportResult(true,id.toString()));return;
            }
            var document=new RecipeDocument(submitted);
            document.decode(player.registryAccess());
            var root=DatapackExporter.serverDatapackRoot(player.server,"vjrm_generated").toAbsolutePath();
            DatapackExporter.writePackMeta(root);
            DatapackExporter.export(root,id.getNamespace(),id.getPath(),document.json(),request.overwrite());
            context.reply(new ExportResult(true,id.toString()));
        }catch(Exception ex){String detail=String.valueOf(ex.getMessage());context.reply(new ExportResult(false,detail.substring(0,Math.min(2000,detail.length()))));}
    }
}
