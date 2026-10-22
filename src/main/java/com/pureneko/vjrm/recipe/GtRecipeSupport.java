package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import com.mojang.serialization.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

/** GTCEu is optional. Keep its full capability maps, conditions and chance metadata. */
public final class GtRecipeSupport {
    public static boolean isRecipe(Object value){return value!=null&&java.util.Set.of("com.gregtechceu.gtceu.api.recipe.GTRecipe","com.gregtechceu.gtceu.api.recipe.kind.GTRecipe").contains(value.getClass().getName());}
    public static boolean isDocument(JsonObject json){
        if(!json.has("type")||!json.get("type").isJsonPrimitive())return false;
        var id=ResourceLocation.tryParse(json.get("type").getAsString());
        var type=id==null?null:BuiltInRegistries.RECIPE_TYPE.get(id);
        return type!=null&&java.util.Set.of("com.gregtechceu.gtceu.api.recipe.GTRecipeType","com.gregtechceu.gtceu.api.recipe.kind.GTRecipeType").contains(type.getClass().getName());
    }
    public static ResourceLocation id(Object value){
        if(!isRecipe(value))return null;
        try{return (ResourceLocation)value.getClass().getMethod("getId").invoke(value);}catch(ReflectiveOperationException ex){return null;}
    }
    private static final java.util.concurrent.atomic.AtomicLong PREVIEWS=new java.util.concurrent.atomic.AtomicLong(System.nanoTime());
    public static Recipe<?> freshPreview(Recipe<?> recipe){
        if(isRecipe(recipe))try{
            var preview=new ResourceLocation("vjrm","preview/"+Long.toUnsignedString(PREVIEWS.incrementAndGet(),36));
            try{recipe.getClass().getMethod("setId",ResourceLocation.class).invoke(recipe,preview);}
            catch(NoSuchMethodException ex){recipe.getClass().getField("id").set(recipe,preview);}
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.layout",ex);}
        return recipe;
    }
    public static Recipe<?> preparePreview(Recipe<?> recipe){
        if(isRecipe(recipe)&&id(recipe)==null)try{
            var preview=new ResourceLocation("vjrm","preview");
            try{recipe.getClass().getMethod("setId",ResourceLocation.class).invoke(recipe,preview);}
            catch(NoSuchMethodException ex){recipe.getClass().getField("id").set(recipe,preview);}
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.layout",ex);}
        return recipe;
    }
    public static boolean kubeLoaded(){return net.minecraftforge.fml.ModList.get().isLoaded("kubejs");}
    public static RecipeDocument copy(Recipe<?> recipe,net.minecraft.core.RegistryAccess registries){try{
            var serializer=Class.forName("com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer");
            @SuppressWarnings("unchecked") Codec<Object> codec=(Codec<Object>)serializer.getField("CODEC").get(null);
            var lookup=(net.minecraft.core.RegistryAccess)Class.forName("com.gregtechceu.gtceu.api.registry.GTRegistries").getMethod("builtinRegistry").invoke(null);
            return new RecipeDocument(codec.encodeStart(net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,lookup),recipe).getOrThrow(false,error->{}).getAsJsonObject());
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.no_source",ex);}}
    public static void checkCapacity(JsonObject json,boolean output,boolean fluid,int size){
        try{
            var type=BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.tryParse(json.get("type").getAsString()));
            Object cap=null;
            for(String prefix:java.util.List.of("com.gregtechceu.gtceu.api.capability.recipe.","com.gregtechceu.gtceu.api.recipe.capability."))try{cap=Class.forName(prefix+(fluid?"Fluid":"Item")+"RecipeCapability").getField("CAP").get(null);break;}catch(ClassNotFoundException ignored){}
            if(cap==null)throw new IllegalArgumentException("vjrm.gt.capacity");
            int max=-1;
            for(var method:type.getClass().getMethods())if(method.getName().equals(output?"getMaxOutputs":"getMaxInputs")&&method.getParameterCount()==1&&method.getParameterTypes()[0].isInstance(cap)){max=((Number)method.invoke(type,cap)).intValue();break;}
            if(size>=max)throw new IllegalArgumentException("vjrm.gt.capacity");
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.gt.capacity",ex);}
    }
    private GtRecipeSupport(){}
}
