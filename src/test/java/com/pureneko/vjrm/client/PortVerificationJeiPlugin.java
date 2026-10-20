package com.pureneko.vjrm.client;

import com.pureneko.vjrm.PortMachineRecipe;
import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class PortVerificationJeiPlugin implements IModPlugin {
    public record Display(PortMachineRecipe recipe){}
    private static final RecipeType<Display> TYPE=RecipeType.create("vjrm_verify","machine",Display.class);
    public static final Category CATEGORY=new Category();
    public ResourceLocation getPluginUid(){return new ResourceLocation("vjrm_verify","test");}
    public void registerCategories(IRecipeCategoryRegistration r){r.addRecipeCategories(CATEGORY);}
    public void registerRecipes(IRecipeRegistration r){var mc=Minecraft.getInstance();if(mc.level!=null)r.addRecipes(TYPE,mc.level.getRecipeManager().getAllRecipesFor(PortMachineRecipe.TYPE).stream().map(Display::new).toList());}
    public static final class Category implements IRecipeCategory<Display>{
        public RecipeType<Display> getRecipeType(){return TYPE;}
        public Component getTitle(){return Component.literal("Forge verification machine");}
        public IDrawable getIcon(){return null;}
        public IDrawable getBackground(){return new IDrawable(){public int getWidth(){return 150;}public int getHeight(){return 60;}public void draw(net.minecraft.client.gui.GuiGraphics g,int x,int y){g.fill(x,y,x+150,y+60,0xFF493D61);g.drawString(Minecraft.getInstance().font,"Forge 1.20.1 JEI / JEMI",x+4,y+4,0xFFFFFF,false);}};}
        public ResourceLocation getRegistryName(Display d){return d.recipe().getId();}
        public void setRecipe(IRecipeLayoutBuilder b,Display d,IFocusGroup focuses){b.addSlot(RecipeIngredientRole.INPUT,5,25).addIngredients(d.recipe().input());b.addSlot(RecipeIngredientRole.INPUT,40,25).addIngredientsUnsafe(java.util.List.of(d.recipe().fluid()));b.addSlot(RecipeIngredientRole.OUTPUT,120,25).addItemStack(d.recipe().result());}
    }
}
