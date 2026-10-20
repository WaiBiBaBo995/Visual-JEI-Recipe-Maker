package com.pureneko.vjrm.network;

import com.pureneko.vjrm.recipe.RecipeSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import java.util.*;

public final class RecipeSourceNetwork {
    public record Request(int token,String id){
        public void encode(FriendlyByteBuf b){b.writeVarInt(token);b.writeUtf(id,200);}
        public static Request decode(FriendlyByteBuf b){return new Request(b.readVarInt(),b.readUtf(200));}
    }
    public record Response(int token,String json,String error){
        public void encode(FriendlyByteBuf b){b.writeVarInt(token);b.writeUtf(json,RecipeSource.MAX_JSON);b.writeUtf(error,200);}
        public static Response decode(FriendlyByteBuf b){return new Response(b.readVarInt(),b.readUtf(RecipeSource.MAX_JSON),b.readUtf(200));}
    }
    public record CatalogRequest(int token){
        public void encode(FriendlyByteBuf b){b.writeVarInt(token);}
        public static CatalogRequest decode(FriendlyByteBuf b){return new CatalogRequest(b.readVarInt());}
    }
    public record Catalog(int token,List<RecipeSource.Sample> samples){
        public void encode(FriendlyByteBuf b){b.writeVarInt(token);b.writeVarInt(samples.size());for(var s:samples){b.writeUtf(s.serializer(),200);b.writeUtf(s.id(),200);}}
        public static Catalog decode(FriendlyByteBuf b){int token=b.readVarInt(),size=b.readVarInt();if(size<0||size>4096)throw new IllegalArgumentException("catalog size");var samples=new ArrayList<RecipeSource.Sample>();for(int i=0;i<size;i++)samples.add(new RecipeSource.Sample(b.readUtf(200),b.readUtf(200)));return new Catalog(token,List.copyOf(samples));}
    }
    private record CachedCatalog(long until,List<RecipeSource.Sample> samples){}
    private static final Map<net.minecraft.server.MinecraftServer,CachedCatalog> CACHE=new WeakHashMap<>();
    static void register(){
        var channel=RecipeNetwork.CHANNEL;
        channel.messageBuilder(Request.class,2,NetworkDirection.PLAY_TO_SERVER).encoder(Request::encode).decoder(Request::decode).consumerMainThread((p,c)->{
            var player=c.get().getSender();if(player==null)return;
            try{RecipeNetwork.reply(player,new Response(p.token(),RecipeSource.read(player.server,new ResourceLocation(p.id())).json().toString(),""));}
            catch(Exception ex){String error=String.valueOf(ex.getMessage());RecipeNetwork.reply(player,new Response(p.token(),"",error.startsWith("vjrm.error.")?error:"vjrm.error.no_source"));}
        }).add();
        channel.messageBuilder(Response.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder(Response::encode).decoder(Response::decode).consumerMainThread((p,c)->com.pureneko.vjrm.client.RecipeSources.receive(p)).add();
        channel.messageBuilder(CatalogRequest.class,4,NetworkDirection.PLAY_TO_SERVER).encoder(CatalogRequest::encode).decoder(CatalogRequest::decode).consumerMainThread((p,c)->{
            var player=c.get().getSender();if(player==null)return;long now=System.currentTimeMillis();var cached=CACHE.get(player.server);
            if(cached==null||cached.until()<now){cached=new CachedCatalog(now+1000,RecipeSource.catalog(player.server));CACHE.put(player.server,cached);}
            RecipeNetwork.reply(player,new Catalog(p.token(),cached.samples()));
        }).add();
        channel.messageBuilder(Catalog.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder(Catalog::encode).decoder(Catalog::decode).consumerMainThread((p,c)->com.pureneko.vjrm.client.RecipeSources.receive(p)).add();
    }
}
