package com.pureneko.vjrm.client;

import com.google.gson.JsonParser;
import com.pureneko.vjrm.network.RecipeSourceNetwork;
import com.pureneko.vjrm.recipe.RecipeDocument;
import com.pureneko.vjrm.recipe.RecipeSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

/** Requests have a lifetime; a reply cannot reopen an editor after the player has left it. */
public final class RecipeSources {
    private record Pending(long until, Consumer<RecipeDocument> success, Consumer<String> failure) { }
    private record CatalogPending(long until, Consumer<List<RecipeSource.Sample>> success, Consumer<String> failure) { }
    private static final Map<Integer,Pending> pending=new HashMap<>();
    private static final Map<Integer,CatalogPending> catalogs=new HashMap<>();
    private static int sequence;
    public static boolean available(){return com.pureneko.vjrm.network.VjrmNetworking.available();}
    public static void request(Identifier id,Consumer<RecipeDocument> success,Consumer<String> failure){
        if(!available()){failure.accept("vjrm.error.server_source");return;}
        int token=++sequence;pending.put(token,new Pending(System.currentTimeMillis()+15000,success,failure));
        com.pureneko.vjrm.network.VjrmNetworking.sendToServer(new RecipeSourceNetwork.Request(token,id.toString()));
    }
    public static void catalog(Consumer<List<RecipeSource.Sample>> success,Consumer<String> failure){
        if(!available()){failure.accept("vjrm.error.server_source");return;}
        int token=++sequence;catalogs.put(token,new CatalogPending(System.currentTimeMillis()+15000,success,failure));
        com.pureneko.vjrm.network.VjrmNetworking.sendToServer(new RecipeSourceNetwork.CatalogRequest(token));
    }
    public static void receive(RecipeSourceNetwork.Response response){
        var p=pending.remove(response.token());if(p==null)return;
        if(!response.error().isEmpty()){p.failure.accept(response.error());return;}
        try{p.success.accept(new RecipeDocument(JsonParser.parseString(response.json()).getAsJsonObject()));}
        catch(RuntimeException ex){p.failure.accept("vjrm.error.no_source");}
    }
    public static void receive(RecipeSourceNetwork.Catalog catalog){var p=catalogs.remove(catalog.token());if(p!=null)p.success.accept(catalog.samples());}
    public static void tick(){
        long now=System.currentTimeMillis();boolean disconnected=Minecraft.getInstance().level==null;
        var expired=pending.values().stream().filter(p->disconnected||p.until<now).toList();pending.values().removeAll(expired);
        var expiredCatalogs=catalogs.values().stream().filter(p->disconnected||p.until<now).toList();catalogs.values().removeAll(expiredCatalogs);
        if(!disconnected){expired.forEach(p->p.failure.accept("vjrm.error.source_timeout"));expiredCatalogs.forEach(p->p.failure.accept("vjrm.error.source_timeout"));}
    }
}
