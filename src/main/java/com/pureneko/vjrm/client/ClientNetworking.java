package com.pureneko.vjrm.client;
import com.pureneko.vjrm.network.*;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
public final class ClientNetworking {
    private static void receive(Object packet){
        if(packet instanceof RecipeNetwork.ExportResult p)ExportResultHandler.handle(p);
        else if(packet instanceof RecipeSourceNetwork.Response p)RecipeSources.receive(p);
        else if(packet instanceof RecipeSourceNetwork.Catalog p)RecipeSources.receive(p);
    }
    public static void register(){ClientPlayNetworking.registerGlobalReceiver(VjrmNetworking.Envelope.TYPE,(p,c)->c.client().execute(()->receive(p.packet())));}
    public static boolean available(){return ClientPlayNetworking.canSend(VjrmNetworking.Envelope.TYPE);}
    public static void send(Object p){ClientPlayNetworking.send(new VjrmNetworking.Envelope(p));}
}
