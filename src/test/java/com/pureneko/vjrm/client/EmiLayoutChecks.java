package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.*;
import dev.emi.emi.recipe.EmiCookingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import java.util.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

public final class EmiLayoutChecks {
    @SuppressWarnings("unchecked") public static void run()throws Exception{
        var mc=Minecraft.getInstance();
        for(String type:RecipeDocument.TYPES){
            var doc=RecipeDocument.template(type);var editor=new RecipeEditorScreen(doc,null,"vjrm:native_ui",null);mc.setScreen(editor);
            var layout=(RecipePreview.Layout)field(editor,"layout");check(layout instanceof EmiNativePreview,"Missing native EMI layout for "+type);
            var widgets=(List<Widget>)field(layout,"widgets");check(widgets.stream().anyMatch(w->!(w instanceof SlotWidget)),"Recipe UI reduced to slots: "+type);
        }
        var shaped=RecipeDocument.template("crafting_shaped").json();shaped.add("pattern",com.google.gson.JsonParser.parseString("[\"ABC\",\"DEF\"]"));var keys=shaped.getAsJsonObject("key");for(String key:List.of("G","H","I"))keys.remove(key);
        var grid=new RecipeDocument(shaped);var gridEditor=new RecipeEditorScreen(grid,null,"vjrm:emi_grid",null);mc.setScreen(gridEditor);
        var gridLayout=(RecipePreview.Layout)field(gridEditor,"layout");var input=gridLayout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.INPUT).toList();
        check(input.get(0).fieldPath().equals("key/A")&&input.get(3).fieldPath().equals("key/D")&&input.get(6).fieldPath().equals("$crafting/6"),"Native EMI 3x2 grid uses wrong bindings");
        gridEditor.accept(new RecipeDocument.Field(List.of("$crafting","6"),false,false),new ItemStack(Items.EMERALD));
        check(grid.json().getAsJsonArray("pattern").get(0).getAsString().equals("ABC")&&grid.json().getAsJsonArray("pattern").get(1).getAsString().equals("DEF"),"Native EMI empty-cell insert overwrote existing inputs");
        var cooking=RecipeDocument.template("smelting");var cookEditor=new RecipeEditorScreen(cooking,null,"vjrm:cooking",null);mc.setScreen(cookEditor);
        cooking.setNumber(cooking.numericFields().stream().filter(f->f.label().equals("cookingtime")).findFirst().orElseThrow(),"321",mc.level.registryAccess());mc.setScreen(cookEditor);
        var cooked=(EmiCookingRecipe)((RecipePreview.Layout)field(cookEditor,"layout")).nativeLayout();
        check(((net.minecraft.world.item.crafting.AbstractCookingRecipe)field(cooked,"recipe")).getCookingTime()==321,"Cooking animation kept stale time");
        var original=VerificationEmiPlugin.display;var holder=mc.level.getRecipeManager().byKey(original.getId()).orElseThrow();var document=RecipeDocument.copy(holder.value(),mc.level.registryAccess());
        var editor=new RecipeEditorScreen(document,RecipePreview.category(original.getCategory()),"vjrm:emi_machine",null,original);mc.setScreen(editor);
        check((boolean)field(editor,"usingSourceLayout"),"Native modded source layout was not selected");
        editor.accept(document.fields().stream().filter(f->f.output()).findFirst().orElseThrow(),new ItemStack(Items.EMERALD));
        editor.accept(document.fields().stream().filter(f->f.fluid()).findFirst().orElseThrow(),new FluidStack(Fluids.LAVA,250));
        var layout=(RecipePreview.Layout)field(editor,"layout");check(layout instanceof EmiNativePreview&&layout.nativeLayout()==original,"Source graphics were replaced with a generic layout after editing");
        var output=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow();
        check(((SlotWidget)output.nativeSlot()).getStack().getEmiStacks().getFirst().getItemStack().is(Items.EMERALD),"Native slot renderer did not receive edited output");
        var tank=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.nativeSlot() instanceof TankWidget).findFirst().orElseThrow();
        check(((SlotWidget)tank.nativeSlot()).getStack().getEmiStacks().getFirst().getKey()==Fluids.LAVA&&tank.getRect().getHeight()==30,"Tank renderer or geometry lost after editing");
        check(!original.getOutputs().getFirst().getItemStack().is(Items.EMERALD),"Editing mutated EMI's registered recipe");
        System.out.println("PASS full native EMI UI, all vanilla layouts, grid bindings/insertion, cooking time refresh, original background/tank renderers and source isolation");
    }
}
