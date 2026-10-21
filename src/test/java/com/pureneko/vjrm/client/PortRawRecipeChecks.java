package com.pureneko.vjrm.client;

import mezz.jei.api.*;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import java.util.*;

/** The category intentionally has no ID hook, reproducing raw Recipe integrations. */
@JeiPlugin
public final class PortRawRecipeChecks implements IModPlugin {
    public static Recipe<?> raw;
    public static final Identifier SOURCE=Identifier.parse("vjrm:source_check");
    @SuppressWarnings({"rawtypes","unchecked"})
    public static final IRecipeType<Recipe<?>> TYPE=(IRecipeType)IRecipeType.create("vjrm_verify","raw",Recipe.class);
    public static final Category CATEGORY=new Category();
    public Identifier getPluginUid(){return Identifier.parse("vjrm_verify:raw_plugin");}
    public static void registerSync(){net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent.EVENT.register((mc,synced)->{
        raw=synced.recipes().stream().filter(h->h.id().identifier().equals(SOURCE)).map(RecipeHolder::value).findFirst().orElse(null);
    });}
    public void registerCategories(IRecipeCategoryRegistration registration){CATEGORY.helpers=registration.getJeiHelpers();registration.addRecipeCategories(CATEGORY);}
    public void registerRecipes(IRecipeRegistration registration){if(raw!=null)registration.addRecipes(TYPE,List.of(raw));}
    public static class Category implements IRecipeCategory<Recipe<?>> {
        mezz.jei.api.helpers.IJeiHelpers helpers;
        public IRecipeType<Recipe<?>> getRecipeType(){return TYPE;}
        public Component getTitle(){return Component.literal("Raw recipe without ID hook");}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return null;}
        public int getWidth(){return 160;}public int getHeight(){return 55;}
        public void setRecipe(IRecipeLayoutBuilder builder,Recipe<?> recipe,IFocusGroup focus){
            builder.addSlot(RecipeIngredientRole.INPUT,12,24).addItemStack(new ItemStack(Items.STONE));
            var helper=helpers.getPlatformFluidHelper();
            Object fluid=helper.create(net.minecraft.world.level.material.Fluids.WATER.builtInRegistryHolder(),81000);
            builder.addSlot(RecipeIngredientRole.INPUT,75,24).addIngredientsUnsafe(List.of(fluid));
            builder.addSlot(RecipeIngredientRole.OUTPUT,125,24).addItemStack(new ItemStack(Items.DIAMOND));
        }
        public void draw(Recipe<?> recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphicsExtractor g,double x,double y){
            g.fill(0,0,160,55,0xFF334B55);g.text(Minecraft.getInstance().font,"Raw recipe",5,6,0xFFFFFFFF,false);
        }
    }
}
