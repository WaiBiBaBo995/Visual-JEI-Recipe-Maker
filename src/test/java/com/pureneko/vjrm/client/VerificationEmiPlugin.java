package com.pureneko.vjrm.client;

import com.pureneko.vjrm.TestMachineRecipe;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.*;

/** A real native EMI category with a background, text, arrow, item slots and a tank. */
@EmiEntrypoint
public final class VerificationEmiPlugin implements EmiPlugin {
    static EmiRecipe display;
    static int backgroundDraws;
    public void register(EmiRegistry registry){
        var category=new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("vjrm_verify","native_machine"),EmiStack.of(Items.FURNACE));registry.addCategory(category);
        var holder=registry.getRecipeManager().getAllRecipesFor(TestMachineRecipe.TYPE).getFirst();var recipe=holder.value();
        display=new EmiRecipe(){
            public EmiRecipeCategory getCategory(){return category;}
            public ResourceLocation getId(){return holder.id();}
            public List<EmiIngredient> getInputs(){return List.of(EmiIngredient.of(Arrays.stream(recipe.input().getItems()).map(EmiStack::of).toList(),recipe.input().count()),EmiIngredient.of(Arrays.stream(recipe.fluidInput().getFluids()).map(NeoForgeEmiStack::of).toList(),recipe.fluidInput().amount()));}
            public List<EmiStack> getOutputs(){return List.of(EmiStack.of(recipe.result()));}
            public int getDisplayWidth(){return 150;}
            public int getDisplayHeight(){return 66;}
            public void addWidgets(WidgetHolder widgets){
                widgets.addDrawable(0,0,150,66,(g,x,y,d)->{backgroundDraws++;g.fill(0,0,150,66,0xFF34445B);g.renderOutline(0,0,150,66,0xFF80B8E8);});
                widgets.addText(net.minecraft.network.chat.Component.literal("Native EMI machine"),5,5,0xFFFFFF,false);
                widgets.addText(net.minecraft.network.chat.Component.literal("duration: "+recipe.duration()),5,53,0xFFFFFF,false);
                widgets.addTexture(EmiTexture.EMPTY_ARROW,75,24);
                widgets.addSlot(getInputs().get(0),5,24);
                widgets.addTank(getInputs().get(1),40,20,18,30,1000);
                widgets.addSlot(getOutputs().getFirst(),120,24).recipeContext(this);
            }
        };
        registry.addRecipe(display);
    }
}
