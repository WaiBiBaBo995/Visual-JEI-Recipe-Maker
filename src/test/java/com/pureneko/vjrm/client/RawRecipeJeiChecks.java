package com.pureneko.vjrm.client;

import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import java.util.*;

/** Reproduces Astral Sorcery: raw Recipe values, no getRegistryName override. */
@JeiPlugin
public final class RawRecipeJeiChecks implements IModPlugin {
    @SuppressWarnings({"rawtypes","unchecked"})
    public static final RecipeType<Recipe<?>> TYPE=new RecipeType(ResourceLocation.fromNamespaceAndPath("vjrm_verify","raw_recipe"),(Class)Recipe.class);
    public static final Category CATEGORY=new Category();
    public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("vjrm_verify","raw_recipe_plugin");}
    public void registerCategories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(CATEGORY);}
    public void registerRecipes(IRecipeRegistration registration){
        if(Minecraft.getInstance().level!=null)registration.addRecipes(TYPE,List.of(recipe()));
    }
    public static Recipe<?> recipe(){return Minecraft.getInstance().level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("vjrm","source_check")).orElseThrow().value();}
    public static final class Category implements IRecipeCategory<Recipe<?>> {
        public RecipeType<Recipe<?>> getRecipeType(){return TYPE;}
        public Component getTitle(){return Component.literal("Raw recipe without category ID");}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return null;}
        public int getWidth(){return 160;}
        public int getHeight(){return 55;}
        public void setRecipe(IRecipeLayoutBuilder builder,Recipe<?> recipe,IFocusGroup focus){
            var input=recipe.getIngredients().stream().filter(i->!i.isEmpty()).findFirst().orElseThrow();
            builder.addSlot(RecipeIngredientRole.INPUT,12,24).addIngredients(input);
            builder.addSlot(RecipeIngredientRole.OUTPUT,125,24).addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
        }
        public void draw(Recipe<?> recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics graphics,double mx,double my){
            graphics.fill(0,0,160,55,0xFF334B55);graphics.drawString(Minecraft.getInstance().font,"Raw recipe: missing ID",5,6,0xFFFFFF,false);
        }
    }
}

