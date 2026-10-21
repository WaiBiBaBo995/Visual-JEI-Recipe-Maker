package com.pureneko.vjrm.network;

import com.pureneko.vjrm.recipe.RecipeSource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.*;

@EventBusSubscriber(modid="vjrm")
public final class RecipeSourceNetwork {
    public record Request(int token, String id) implements CustomPacketPayload {
        public static final Type<Request> TYPE = RecipeSourceNetwork.type("source_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC = StreamCodec.of((b,p)->{b.writeVarInt(p.token); b.writeUtf(p.id,200);},b->new Request(b.readVarInt(),b.readUtf(200)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Response(int token, String json, String error) implements CustomPacketPayload {
        public static final Type<Response> TYPE = RecipeSourceNetwork.type("source_response");
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC = StreamCodec.of((b,p)->{b.writeVarInt(p.token);b.writeUtf(p.json,RecipeSource.MAX_JSON);b.writeUtf(p.error,200);},b->new Response(b.readVarInt(),b.readUtf(RecipeSource.MAX_JSON),b.readUtf(200)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record CatalogRequest(int token) implements CustomPacketPayload {
        public static final Type<CatalogRequest> TYPE = RecipeSourceNetwork.type("catalog_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,CatalogRequest> CODEC = StreamCodec.of((b,p)->b.writeVarInt(p.token),b->new CatalogRequest(b.readVarInt()));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Catalog(int token, List<RecipeSource.Sample> samples) implements CustomPacketPayload {
        public static final Type<Catalog> TYPE = RecipeSourceNetwork.type("catalog");
        public static final StreamCodec<RegistryFriendlyByteBuf,Catalog> CODEC = StreamCodec.of((b,p)->{
            b.writeVarInt(p.token);b.writeVarInt(p.samples.size());
            for(var s:p.samples){b.writeUtf(s.serializer(),200);b.writeUtf(s.id(),200);}
        },b->{int token=b.readVarInt(),n=b.readVarInt();if(n<0||n>4096)throw new IllegalArgumentException("catalog size");var entries=new ArrayList<RecipeSource.Sample>();for(int i=0;i<n;i++)entries.add(new RecipeSource.Sample(b.readUtf(200),b.readUtf(200)));return new Catalog(token,List.copyOf(entries));});
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private record CachedCatalog(long until,List<RecipeSource.Sample> samples) { }
    private static final Map<net.minecraft.server.MinecraftServer,CachedCatalog> CATALOG_CACHE = new WeakHashMap<>();
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("vjrm",path));}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        var r=event.registrar("1").optional();
        r.playToServer(Request.TYPE,Request.CODEC,RecipeSourceNetwork::handle);
        r.playToClient(Response.TYPE,Response.CODEC,(p,c)->com.pureneko.vjrm.client.RecipeSources.receive(p));
        r.playToServer(CatalogRequest.TYPE,CatalogRequest.CODEC,(p,c)->{
            if(!(c.player() instanceof ServerPlayer player))return;
            long now=System.currentTimeMillis();var cached=CATALOG_CACHE.get(player.level().getServer());
            if(cached==null||cached.until<now){cached=new CachedCatalog(now+1000,RecipeSource.catalog(player.level().getServer()));CATALOG_CACHE.put(player.level().getServer(),cached);}
            c.reply(new Catalog(p.token(),cached.samples));
        });
        r.playToClient(Catalog.TYPE,Catalog.CODEC,(p,c)->com.pureneko.vjrm.client.RecipeSources.receive(p));
    }
    public static void handle(Request request, IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        try{context.reply(new Response(request.token(),RecipeSource.read(player.level().getServer(),Identifier.parse(request.id())).json().toString(),""));}
        catch(Exception ex){String error=String.valueOf(ex.getMessage());context.reply(new Response(request.token(),"",error.startsWith("vjrm.error.")?error:"vjrm.error.no_source"));}
    }
}
