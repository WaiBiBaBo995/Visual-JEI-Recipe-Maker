package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.jemi.*;
import dev.emi.emi.jemi.widget.JemiSlotWidget;
import dev.emi.emi.jemi.widget.JemiTankWidget;
import dev.emi.emi.api.widget.SlotWidget;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.crafting.Recipe;
import java.util.*;

/** Optional JEMI boundary: never load this class in a runtime with no JEI API. */
final class EmiJemiPreview {
    static dev.emi.emi.api.stack.EmiStack stack(Object value){return JemiUtil.getStack(value);}
    static Optional<RecipePreview.Layout> rebuild(Recipe<?> recipe,RecipePreview.Category preferred,RecipeDocument document){
        if(JemiPlugin.runtime==null)return Optional.empty();
        var provider=new JeiPreviewProvider(JemiPlugin.runtime);
        IRecipeCategory<?> category=null;
        if(preferred!=null){
            if(preferred.nativeCategory() instanceof JemiCategory jemi)category=jemi.category;
            else if(preferred.nativeCategory() instanceof IRecipeCategory<?> jei)category=jei;
            else category=JemiPlugin.runtime.getRecipeManager().createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(preferred.getRecipeType().getUid())).findFirst().orElse(null);
        }
        return provider.layout(recipe,category==null?null:provider.category(category),document);
    }
    static Optional<RecipePreview.Role> role(SlotWidget slot){
        if(slot instanceof JemiTankWidget){
            try{var field=JemiTankWidget.class.getDeclaredField("slot");field.setAccessible(true);var tank=(dev.emi.emi.jemi.impl.JemiRecipeSlot)field.get(slot);return Optional.of(RecipePreview.Role.valueOf(tank.getRole().name()));}
            catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot read the JEMI tank role",ex);}
        }
        return slot instanceof JemiSlotWidget jemi?Optional.of(RecipePreview.Role.valueOf(jemi.slot.getRole().name())):Optional.empty();
    }
}
