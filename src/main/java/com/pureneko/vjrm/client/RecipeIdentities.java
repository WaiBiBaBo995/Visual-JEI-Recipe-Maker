package com.pureneko.vjrm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import java.util.IdentityHashMap;

/** Some JEI integrations register raw Recipe values and leave getRegistryName unset. */
final class RecipeIdentities {
    private static RecipeManager manager;
    private static final IdentityHashMap<Recipe<?>,ResourceLocation> ids=new IdentityHashMap<>();
    static ResourceLocation id(Object display){
        if(display instanceof RecipeHolder<?> holder)return holder.id();
        if(!(display instanceof Recipe<?> recipe)||Minecraft.getInstance().level==null)return null;
        var current=Minecraft.getInstance().level.getRecipeManager();
        if(manager!=current){manager=current;ids.clear();}
        var cached=ids.get(recipe);
        if(cached!=null&&current.byKey(cached).map(h->h.value()==recipe).orElse(false))return cached;
        // Rebuild on a miss, also covering resource reloads on the same RecipeManager instance.
        ids.clear();for(var holder:current.getRecipes())ids.put(holder.value(),holder.id());
        return ids.get(recipe);
    }
    private RecipeIdentities(){}
}

