package com.pureneko.vjrm.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import java.util.List;

public interface RecipeAdapter<T extends Recipe<?>> {
    ResourceLocation type();
    boolean supports(Recipe<?> recipe);
    List<RecipeField> fields(Recipe<?> recipe);
    JsonObject write(RecipeEditorState state);
    RecipeEditorState read(Recipe<?> recipe, JsonObject source);
    default ItemStack result(Recipe<?> recipe) { return recipe.getResultItem(null); }
}
