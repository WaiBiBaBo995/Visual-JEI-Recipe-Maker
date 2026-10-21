package com.pureneko.vjrm.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

/** Forge 1.20 recipes still carry their own source identifier. */
final class RecipeIdentities {
    static ResourceLocation id(Object display){return display instanceof Recipe<?> recipe?recipe.getId():null;}
    private RecipeIdentities(){}
}

