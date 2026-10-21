package com.pureneko.vjrm.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
public final class VjrmNetworking {
    public static final ResourceLocation CHANNEL=new ResourceLocation("vjrm","main");
    public static void encode(Object p,FriendlyByteBuf b){
        if(p instanceof RecipeNetwork.ExportRequest v){b.writeByte(0);v.encode(b);}
        else if(p instanceof RecipeNetwork.ExportResult v){b.writeByte(1);v.encode(b);}
        else if(p instanceof RecipeSourceNetwork.Request v){b.writeByte(2);v.encode(b);}
        else if(p instanceof RecipeSourceNetwork.Response v){b.writeByte(3);v.encode(b);}
        else if(p instanceof RecipeSourceNetwork.CatalogRequest v){b.writeByte(4);v.encode(b);}
        else if(p instanceof RecipeSourceNetwork.Catalog v){b.writeByte(5);v.encode(b);}
        else throw new IllegalArgumentException("Unknown VJRM message");
    }
    public static Object decode(FriendlyByteBuf b){return switch(b.readUnsignedByte()){
        case 0->RecipeNetwork.ExportRequest.decode(b);case 1->RecipeNetwork.ExportResult.decode(b);
        case 2->RecipeSourceNetwork.Request.decode(b);case 3->RecipeSourceNetwork.Response.decode(b);
        case 4->RecipeSourceNetwork.CatalogRequest.decode(b);case 5->RecipeSourceNetwork.Catalog.decode(b);
        default->throw new IllegalArgumentException("Unknown VJRM packet");};}
    private static void receive(Object p,ServerPlayer player){
        if(p instanceof RecipeNetwork.ExportRequest request)reply(player,RecipeNetwork.processExport(request,player));
        else if(p instanceof RecipeSourceNetwork.Request||p instanceof RecipeSourceNetwork.CatalogRequest)RecipeSourceNetwork.handle(p,player);
    }
    public static void sendToServer(Object p){com.pureneko.vjrm.client.ClientNetworking.send(p);}
    public static boolean available(){return com.pureneko.vjrm.client.ClientNetworking.available();}
    public static boolean exportAvailable(){return available();}
    public static void register(){ServerPlayNetworking.registerGlobalReceiver(CHANNEL,(server,player,handler,buf,sender)->{Object p=decode(buf);server.execute(()->receive(p,player));});}
    public static void reply(ServerPlayer player,Object p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();encode(p,b);ServerPlayNetworking.send(player,CHANNEL,b);}
}
