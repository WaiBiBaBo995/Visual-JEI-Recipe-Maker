package com.pureneko.vjrm;
import net.fabricmc.api.ModInitializer;
public final class Vjrm implements ModInitializer {
    public static final String MOD_ID="vjrm";
    public void onInitialize(){com.pureneko.vjrm.network.RecipeNetwork.PERMISSION.load();com.pureneko.vjrm.network.RecipeNetwork.PERMISSION.save();com.pureneko.vjrm.network.VjrmNetworking.register();
        for(var entry:net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.entrySet())
            if(entry.getKey().identifier().getNamespace().equals("minecraft"))net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(entry.getValue());}
}
