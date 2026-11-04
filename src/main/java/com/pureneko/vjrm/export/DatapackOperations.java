package com.pureneko.vjrm.export;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.util.*;

/** Shared file operations for local exports and server-owned world packs. */
public final class DatapackOperations {
    private static final Gson JSON=new Gson();
    private DatapackOperations(){}
    public static boolean mutates(String action){return Set.of("create","rename","delete","restore","purge","remove","enable","move","save","save_move","disable","disable_move").contains(action);}
    public static JsonObject page(List<?> all,int offset){
        if(offset<0||offset>all.size())throw new IllegalArgumentException("vjrm.manage.missing");
        JsonObject result=new JsonObject();int end=Math.min(all.size(),offset+128);
        result.add("items",JSON.toJsonTree(all.subList(offset,end)));result.addProperty("next",end<all.size()?end:-1);result.addProperty("total",all.size());return result;
    }
    public static JsonObject execute(ManagedDatapacks store,String action,String pack,String value,String id,String json,boolean overwrite,String source) throws IOException {
        JsonObject result=new JsonObject();
        switch(action){
            case "packs" -> {return page(store.packs(),offset(value));}
            case "entries" -> {return page(store.entries(pack),offset(value));}
            case "backups" -> {return page(store.backups(),offset(value));}
            case "create" -> store.create(pack);
            case "rename" -> store.rename(pack,value);
            case "delete" -> store.backup(pack);
            case "restore" -> store.restore(pack,value);
            case "purge" -> store.purge(pack);
            case "remove", "enable" -> store.remove(pack,new ResourceLocation(id));
            case "move" -> store.move(pack,value,new ResourceLocation(id));
            case "read" -> {return store.get(pack,new ResourceLocation(id));}
            case "save", "save_move" -> store.save(pack,new ResourceLocation(id),JsonParser.parseString(json).getAsJsonObject(),overwrite,action.equals("save_move"),source);
            case "disable", "disable_move" -> store.save(pack,new ResourceLocation(id),ManagedDatapacks.disabledJson(),overwrite,action.equals("disable_move"),source);
            case "export" -> {var file=store.zip(pack);result.addProperty("size",java.nio.file.Files.size(file));}
            case "download" -> {
                long position=Long.parseLong(value);var file=store.zipPath(pack);long size=java.nio.file.Files.size(file);
                if(position<0||position>size)throw new IOException("vjrm.manage.missing");
                byte[] bytes=new byte[(int)Math.min(98_304,size-position)];
                try(var input=new java.io.RandomAccessFile(file.toFile(),"r")){input.seek(position);input.readFully(bytes);}
                result.addProperty("data",Base64.getEncoder().encodeToString(bytes));result.addProperty("next",position+bytes.length);result.addProperty("size",size);
            }
            default -> throw new IllegalArgumentException("vjrm.manage.action");
        }
        return result;
    }
    private static int offset(String value){return value.isEmpty()?0:Integer.parseInt(value);}
}
