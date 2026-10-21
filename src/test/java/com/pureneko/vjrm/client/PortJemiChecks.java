package com.pureneko.vjrm.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.jemi.JemiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import com.pureneko.vjrm.recipe.RecipeDocument;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

public final class PortJemiChecks {
    public static dev.emi.emi.api.recipe.EmiRecipe rawRecipe(){
        return dev.emi.emi.api.EmiApi.getRecipeManager().getRecipes().stream().filter(dev.emi.emi.jemi.JemiRecipe.class::isInstance).map(r->(dev.emi.emi.jemi.JemiRecipe<?>)r).filter(r->r.category.getRecipeType().getUid().toString().equals("vjrm_verify:raw_recipe")).findFirst().orElseThrow(()->new AssertionError("Raw recipe category was not converted by JEMI"));
    }
    public static void run(){
        var recipe=EmiApi.getRecipeManager().getRecipes().stream().filter(r->r instanceof JemiRecipe&&r.getCategory().getId().toString().equals("vjrm_verify:machine")).findFirst().orElseThrow(()->new AssertionError("JEI compatibility recipe missing from EMI"));
        RecipeSources.request(recipe.getBackingRecipe().getId(),document->{try{
            var editor=new RecipeEditorScreen(document,RecipePreview.category(recipe.getCategory()),"vjrm:forge_jemi_machine",null,recipe);Minecraft.getInstance().setScreen(editor);
            check((boolean)field(editor,"usingSourceLayout"),"JEMI source UI missing");
            editor.accept(document.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow(),new ItemStack(Items.EMERALD));
            check(field(editor,"layout") instanceof EmiNativePreview,"JEMI wrapper did not retain complete native UI");
            var layout=(RecipePreview.Layout)field(editor,"layout");var output=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow();
            check(((dev.emi.emi.api.widget.SlotWidget)output.nativeSlot()).getStack().getEmiStacks().get(0).getItemStack().is(Items.EMERALD),"JEMI output renderer ignored the edit");
            System.out.println("PASS Forge JEMI compatibility category background and widget renderer");PortClientChecks.viewerReady=true;
        }catch(Throwable ex){PortClientChecks.fail(ex);}},error->PortClientChecks.fail(new AssertionError(error)));
    }
}
