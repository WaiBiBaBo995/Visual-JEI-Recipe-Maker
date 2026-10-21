package com.pureneko.vjrm.client;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import java.util.IdentityHashMap;

/** Index the recipes NeoForge actually syncs; vanilla clients no longer have a RecipeManager. */
@EventBusSubscriber(modid="vjrm",value=Dist.CLIENT)
public final class RecipeIdentities {
    private static final IdentityHashMap<Recipe<?>,Identifier> ids=new IdentityHashMap<>();
    @SubscribeEvent public static void received(RecipesReceivedEvent event){
        ids.clear();for(var holder:event.getRecipeMap().values())ids.put(holder.value(),holder.id().identifier());
    }
    @SubscribeEvent public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event){ids.clear();}
    static Identifier id(Object display){
        if(display instanceof RecipeHolder<?> holder)return holder.id().identifier();
        return display instanceof Recipe<?> recipe?ids.get(recipe):null;
    }
    private RecipeIdentities(){}
}
