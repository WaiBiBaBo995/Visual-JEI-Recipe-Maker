package com.pureneko.vjrm.network;

import com.google.gson.*;
import com.pureneko.vjrm.export.*;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid="vjrm")
public final class DatapackManagementNetwork {
    public record Request(int token,String action,String pack,String value,String id,String json,boolean overwrite) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("vjrm","datapack_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{
            b.writeVarInt(p.token);b.writeUtf(p.action,32);b.writeUtf(p.pack,100);b.writeUtf(p.value,128);b.writeUtf(p.id,200);b.writeUtf(p.json,24000);b.writeBoolean(p.overwrite);
        },b->new Request(b.readVarInt(),b.readUtf(32),b.readUtf(100),b.readUtf(128),b.readUtf(200),b.readUtf(24000),b.readBoolean()));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Response(int token,boolean success,String detail,String json) implements CustomPacketPayload {
        public static final Type<Response> TYPE=new Type<>(Identifier.fromNamespaceAndPath("vjrm","datapack_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p)->{
            b.writeVarInt(p.token);b.writeBoolean(p.success);b.writeUtf(p.detail,512);b.writeUtf(p.json,240000);
        },b->new Response(b.readVarInt(),b.readBoolean(),b.readUtf(512),b.readUtf(240000)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private record Rate(long since,int count){}
    private static final Map<UUID,Rate> LAST_REQUEST=new HashMap<>();
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("1").optional();
        registrar.playToServer(Request.TYPE,Request.CODEC,DatapackManagementNetwork::handle);
        registrar.playToClient(Response.TYPE,Response.CODEC,(p,c)->com.pureneko.vjrm.client.DatapackManagementClient.receive(p));
    }
    public static ManagedDatapacks store(MinecraftServer server) throws java.io.IOException {
        return new ManagedDatapacks(server.getWorldPath(LevelResource.ROOT).resolve("datapacks"),backupRoot(server));
    }
    public static Path backupRoot(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/vjrm/backups");}
    public static String source(MinecraftServer server,Identifier id) throws java.io.IOException {
        Set<String> owned=new HashSet<>();for(var p:store(server).packs())owned.add("file/"+p.name());
        var stack=server.getResourceManager().getResourceStack(RecipeSource.resourceId(id));
        for(int i=stack.size()-1;i>=0;i--)if(!owned.contains(stack.get(i).sourcePackId()))return stack.get(i).sourcePackId();
        return "";
    }
    private static void supported(MinecraftServer server,Identifier id) throws java.io.IOException {
        var holder=server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id)).orElseThrow(()->new java.io.IOException("vjrm.error.no_source"));
        if(server.getResourceManager().getResource(RecipeSource.resourceId(id)).isEmpty())throw new java.io.IOException("vjrm.manage.runtime");
    }
    public static void handle(Request request,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        if(!player.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(RecipeNetwork.PERMISSION.get())))){context.reply(new Response(request.token,false,"permission",""));return;}
        try{
            long now=System.currentTimeMillis();Rate previous=LAST_REQUEST.get(player.getUUID());
            Rate current=previous==null||now-previous.since>1000?new Rate(now,1):new Rate(previous.since,previous.count+1);
            LAST_REQUEST.put(player.getUUID(),current);if(current.count>60)throw new IllegalArgumentException("rate");
            ManagedDatapacks store=store(player.level().getServer());JsonObject result;
            String action=request.action;
            {
                String upstream="";
                if(action.startsWith("save")){
                    var doc=new RecipeDocument(JsonParser.parseString(request.json).getAsJsonObject());
                    doc.decode(player.registryAccess());upstream=source(player.level().getServer(),Identifier.parse(request.id));
                }else if(action.startsWith("disable")){
                    var id=Identifier.parse(request.id);supported(player.level().getServer(),id);upstream=source(player.level().getServer(),id);
                    if(upstream.isEmpty())throw new IllegalArgumentException("vjrm.manage.generated_remove");
                }
                result=DatapackOperations.execute(store,action,request.pack,request.value,request.id,request.json,request.overwrite,upstream);
                if(DatapackOperations.mutates(action))result.addProperty("priority",prioritize(player.level().getServer()));
            }
            String json=result.toString();if(json.length()>240000)throw new IllegalArgumentException("vjrm.error.source_size");
            context.reply(new Response(request.token,true,action,json));
        }catch(Exception ex){
            JsonObject data=new JsonObject();String detail=String.valueOf(ex.getMessage());
            if(ex instanceof ManagedDatapacks.Conflict conflict){detail="conflict";data.addProperty("pack",conflict.pack);}
            else if(ex instanceof FileAlreadyExistsException){detail="exists";}
            if(!detail.startsWith("vjrm.")&&!Set.of("conflict","exists","rate").contains(detail)){
                com.mojang.logging.LogUtils.getLogger().warn("VJRM datapack operation failed: {}",request.action,ex);detail="vjrm.manage.io_error";
            }
            context.reply(new Response(request.token,false,detail.substring(0,Math.min(500,detail.length())),data.toString()));
        }
    }
    /** Changes selection for the next manual /reload, without triggering reload itself. */
    public static boolean prioritize(MinecraftServer server){
        try{
            var repository=server.getPackRepository();repository.reload();
            var selected=new ArrayList<>(repository.getSelectedIds());
            for(var pack:store(server).packs()){
                String id="file/"+pack.name();selected.remove(id);if(repository.isAvailable(id))selected.add(id);
            }
            repository.setSelected(selected);
            List<String> actual=new ArrayList<>(repository.getSelectedIds());
            Set<String> managed=new HashSet<>();for(var pack:store(server).packs())managed.add("file/"+pack.name());
            boolean reached=false;
            for(String id:actual){if(managed.contains(id))reached=true;else if(reached)return false;}
            return actual.containsAll(managed);
        }catch(Exception ex){com.mojang.logging.LogUtils.getLogger().warn("Unable to prioritize VJRM packs",ex);return false;}
    }
    @EventBusSubscriber(modid="vjrm")
    public static final class Lifecycle {
        @SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){clean(event.getServer());}
        @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event){clean(event.getServer());LAST_REQUEST.clear();}
        private static void clean(MinecraftServer server){
            try{store(server).cleanup(RecipeNetwork.BACKUP_RETENTION_DAYS.get());}
            catch(Exception ex){com.mojang.logging.LogUtils.getLogger().error("Failed to clean VJRM datapack backups",ex);}
        }
    }
}
