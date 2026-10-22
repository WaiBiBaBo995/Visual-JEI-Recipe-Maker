package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;

/** Optional Thaumaturge API boundary. Uses its registry-aware codec, including addon aspects. */
public final class AspectSupport {
    private static final String API="com.leclowndu93150.thaumaturge.api.aspect.";
    private static final Class<?> INSTANCE=load();
    private static Class<?> load(){try{return Class.forName(API+"AspectInstance");}catch(ClassNotFoundException|LinkageError ex){return null;}}
    public static boolean available(){return INSTANCE!=null;}
    public static boolean isStack(Object value){return INSTANCE!=null&&INSTANCE.isInstance(value);}
    @SuppressWarnings("unchecked") private static Codec<Object> codec(){
        try{return (Codec<Object>)INSTANCE.getField("CODEC").get(null);}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.aspect",ex);}
    }
    public static Object fromJson(JsonElement json,HolderLookup.Provider registries){
        if(!available())throw new IllegalArgumentException("vjrm.error.aspect");
        return codec().parse(registries.createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
    }
    public static Object fromId(String id,int amount,HolderLookup.Provider registries){
        var json=new JsonObject();json.addProperty("aspect",id);json.addProperty("amount",amount);return fromJson(json,registries);
    }
    public static String id(Object value){
        try{var holder=(Holder<?>)INSTANCE.getMethod("aspect").invoke(value);return holder.unwrapKey().orElseThrow().identifier().toString();}
        catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.aspect",ex);}
    }
    public static int amount(Object value){
        try{return ((Number)INSTANCE.getMethod("amount").invoke(value)).intValue();}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.aspect",ex);}
    }
    public static Component name(Object value){
        try{return (Component)Class.forName(API+"AspectComponents").getMethod("name",Holder.class).invoke(null,INSTANCE.getMethod("aspect").invoke(value));}
        catch(ReflectiveOperationException ex){return Component.literal(id(value));}
    }
    private AspectSupport(){}
}
