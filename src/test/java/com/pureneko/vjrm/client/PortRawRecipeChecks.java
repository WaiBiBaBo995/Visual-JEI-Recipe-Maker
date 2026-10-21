package com.pureneko.vjrm.client;

import mezz.jei.api.*;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import java.util.*;

/** The category intentionally has no ID hook, reproducing raw Recipe integrations. */
@JeiPlugin
public final class PortRawRecipeChecks implements IModPlugin {
    public static Recipe<?> raw;
    public static final ResourceLocation SOURCE=new ResourceLocation("vjrm:source_check");
    @SuppressWarnings({"rawtypes","unchecked"})
    public static final RecipeType<Recipe<?>> TYPE=(RecipeType)RecipeType.create("vjrm_verify","raw",Recipe.class);
    public static final Category CATEGORY=new Category();
    public ResourceLocation getPluginUid(){return new ResourceLocation("vjrm_verify:raw_plugin");}
    public static void registerSync(){}
    private static Recipe<?> recipe(){return Minecraft.getInstance().level.getRecipeManager().byKey(SOURCE).orElseThrow();}
    public void registerCategories(IRecipeCategoryRegistration registration){CATEGORY.helpers=registration.getJeiHelpers();registration.addRecipeCategories(CATEGORY);}
    public void registerRecipes(IRecipeRegistration registration){raw=recipe();if(raw!=null)registration.addRecipes(TYPE,List.of(raw));}
    public static class Category implements IRecipeCategory<Recipe<?>> {
        mezz.jei.api.helpers.IJeiHelpers helpers;
        public RecipeType<Recipe<?>> getRecipeType(){return TYPE;}
        public Component getTitle(){return Component.literal("Raw recipe without ID hook");}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return null;}
        public int getWidth(){return 160;}public int getHeight(){return 55;}
        public void setRecipe(IRecipeLayoutBuilder builder,Recipe<?> recipe,IFocusGroup focus){
            builder.addSlot(RecipeIngredientRole.INPUT,12,24).addItemStack(new ItemStack(Items.STONE));
            var helper=helpers.getPlatformFluidHelper();
            Object fluid=helper.create(net.minecraft.world.level.material.Fluids.WATER,81000);
            builder.addSlot(RecipeIngredientRole.INPUT,75,24).addIngredientsUnsafe(List.of(fluid));
            builder.addSlot(RecipeIngredientRole.OUTPUT,125,24).addItemStack(new ItemStack(Items.DIAMOND));
        }
        public void draw(Recipe<?> recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphics g,double x,double y){
            g.fill(0,0,160,55,0xFF334B55);g.drawString(Minecraft.getInstance().font,"Raw recipe",5,6,0xFFFFFFFF,false);
        }
    }
}
