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
    public static void register(){ClientPlayNetworking.registerGlobalReceiver(VjrmNetworking.CHANNEL,(client,handler,buf,sender)->{Object p=VjrmNetworking.decode(buf);client.execute(()->receive(p));});}
    public static boolean available(){return ClientPlayNetworking.canSend(VjrmNetworking.CHANNEL);}
    public static void send(Object p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();VjrmNetworking.encode(p,b);ClientPlayNetworking.send(VjrmNetworking.CHANNEL,b);}
}
