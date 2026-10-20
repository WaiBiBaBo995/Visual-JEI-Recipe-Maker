package com.pureneko.vjrm.client;

import com.pureneko.vjrm.PortMachineRecipe;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.forge.ForgeEmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.api.render.EmiTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.*;

@EmiEntrypoint
public final class PortVerificationEmiPlugin implements EmiPlugin {
    public static EmiRecipe display;
    public void register(EmiRegistry registry){
        var category=new EmiRecipeCategory(new ResourceLocation("vjrm_verify","native"),EmiStack.of(Items.FURNACE));registry.addCategory(category);
        var r=registry.getRecipeManager().getAllRecipesFor(PortMachineRecipe.TYPE).get(0);
        display=new EmiRecipe(){
            public EmiRecipeCategory getCategory(){return category;}
            public ResourceLocation getId(){return r.getId();}
            public List<EmiIngredient> getInputs(){return List.of(EmiIngredient.of(r.input()),ForgeEmiStack.of(r.fluid()));}
            public List<EmiStack> getOutputs(){return List.of(EmiStack.of(r.result()));}
            public int getDisplayWidth(){return 150;}public int getDisplayHeight(){return 66;}
            public void addWidgets(WidgetHolder w){w.addDrawable(0,0,150,66,(g,x,y,d)->{g.fill(0,0,150,66,0xFF34445B);g.drawString(net.minecraft.client.Minecraft.getInstance().font,"Forge 1.20.1 EMI",4,5,0xFFFFFF,false);});w.addTexture(EmiTexture.EMPTY_ARROW,75,25);w.addSlot(getInputs().get(0),5,25);w.addTank(getInputs().get(1),40,20,18,30,1000);w.addSlot(getOutputs().get(0),120,25).recipeContext(this);}
        };registry.addRecipe(display);
    }
}
