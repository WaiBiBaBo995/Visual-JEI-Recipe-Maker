package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** The Forge GT ingredient has its own alternatives and ranged-amount serialization. */
final class GtFluidSupport {
    static int amount(JsonElement raw){
        var json=raw.getAsJsonObject();
        if(json.has("count_provider")){
            var count=json.get("count_provider");
            if(count.isJsonPrimitive())return count.getAsInt();
            if(count.isJsonObject()&&count.getAsJsonObject().has("max_inclusive"))return count.getAsJsonObject().get("max_inclusive").getAsInt();
        }
        if(json.has("amount"))return json.get("amount").getAsInt();
        if(json.has("inner"))return amount(json.get("inner"));
        return 1000;
    }
    static List<FluidStack> values(JsonElement json){
        try{
            var type=Class.forName("com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient");
            var ingredient=type.getMethod("fromJson",JsonElement.class).invoke(null,json);
            if(json.isJsonObject()&&json.getAsJsonObject().has("count_provider"))return List.of((FluidStack)ingredient.getClass().getMethod("getMaxSizeStack").invoke(ingredient));
            return Arrays.asList((FluidStack[])type.getMethod("getStacks").invoke(ingredient));
        }catch(ReflectiveOperationException ex){return List.of();}
    }
    static JsonObject replace(JsonElement raw,String id,boolean tag,int count){
        if(count<1)throw new IllegalArgumentException("vjrm.error.amount");
        var json=raw.getAsJsonObject().deepCopy();
        if(json.has("inner")){
            json.add("inner",replace(json.get("inner"),id,tag,count));
            if(count!=amount(raw)){json.addProperty("count_provider",count);if(json.has("sampledCount"))json.addProperty("sampledCount",-1);}
        }else{
            var value=new JsonObject();value.addProperty(tag?"tag":"fluid",id);json.add("value",value);json.addProperty("amount",count);
        }
        return json;
    }
    private GtFluidSupport(){}
}
