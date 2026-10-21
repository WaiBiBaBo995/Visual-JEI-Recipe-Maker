package com.pureneko.vjrm.client;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import java.util.IdentityHashMap;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public final class RecipeIdentities {
    private static final IdentityHashMap<Recipe<?>,Identifier> ids=new IdentityHashMap<>();
    public static void register(){
        ClientRecipeSynchronizedEvent.EVENT.register((mc,synced)->{ids.clear();for(var h:synced.recipes())ids.put(h.value(),h.id().identifier());});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->ids.clear());
    }
    static Identifier id(Object display){if(display instanceof RecipeHolder<?> holder)return holder.id().identifier();return display instanceof Recipe<?> recipe?ids.get(recipe):null;}
}
