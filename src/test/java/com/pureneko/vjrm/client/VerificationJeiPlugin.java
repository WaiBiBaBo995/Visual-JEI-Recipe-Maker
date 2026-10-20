package com.pureneko.vjrm.client;

import com.pureneko.vjrm.TestMachineRecipe;
import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Deliberately uses a wrapper object instead of Recipe or RecipeHolder. Dev/test only. */
@JeiPlugin
public final class VerificationJeiPlugin implements IModPlugin {
    public record Display(RecipeHolder<TestMachineRecipe> holder) { }
    public static final RecipeType<Display> TYPE=RecipeType.create("vjrm_verify","test_machine",Display.class);
    public static int backgroundDraws;
    public static final Category CATEGORY=new Category();
    public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("vjrm_verify","verification");}
    public void registerCategories(IRecipeCategoryRegistration r){r.addRecipeCategories(CATEGORY);}
    public void registerRecipes(IRecipeRegistration r){
        var level=Minecraft.getInstance().level;
        if(level!=null)r.addRecipes(TYPE,level.getRecipeManager().getAllRecipesFor(TestMachineRecipe.TYPE).stream().map(Display::new).toList());
    }
    public static final class Category implements IRecipeCategory<Display> {
        public RecipeType<Display> getRecipeType(){return TYPE;}
        public Component getTitle(){return Component.literal("Verification machine");}
        public IDrawable getIcon(){return null;}
        public int getWidth(){return 150;}
        public int getHeight(){return 60;}
        public ResourceLocation getRegistryName(Display display){return display.holder.id();}
        @Override public void draw(Display display,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double mouseX,double mouseY){
            backgroundDraws++;g.fill(0,0,150,60,0xFF493D61);g.renderOutline(0,0,150,60,0xFFBD9FE5);
            g.drawString(Minecraft.getInstance().font,"JEI UI via EMI / JEMI",5,5,0xFFFFFF,false);
            g.drawString(Minecraft.getInstance().font,"duration: "+display.holder().value().duration(),5,47,0xFFFFFF,false);
            g.drawString(Minecraft.getInstance().font,"->",85,24,0xFFFFFF,false);
        }
        public void setRecipe(IRecipeLayoutBuilder b,Display d,IFocusGroup f){
            var r=d.holder.value();
            b.addSlot(RecipeIngredientRole.INPUT,5,20).addItemStacks(java.util.List.of(r.input().getItems()));
            b.addSlot(RecipeIngredientRole.INPUT,40,20).addIngredientsUnsafe(java.util.List.of(r.fluidInput().getFluids()));
            b.addSlot(RecipeIngredientRole.OUTPUT,115,20).addItemStack(r.result());
        }
    }
}
