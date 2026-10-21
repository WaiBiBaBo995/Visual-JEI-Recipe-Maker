package com.pureneko.vjrm.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import java.util.List;

public interface RecipeAdapter<T extends Recipe<?>> {
    Identifier type();
    boolean supports(Recipe<?> recipe);
    List<RecipeField> fields(Recipe<?> recipe);
    JsonObject write(RecipeEditorState state);
    RecipeEditorState read(Recipe<?> recipe, JsonObject source);
    default ItemStack result(Recipe<?> recipe) { return recipe.display().stream().findFirst().map(d->d.result().resolveForFirstStack(new net.minecraft.util.context.ContextMap.Builder().create(net.minecraft.world.item.crafting.display.SlotDisplayContext.CONTEXT))).orElse(ItemStack.EMPTY); }
}
