package com.pureneko.vjrm.client;
import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.api.*;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import java.util.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;
public final class PortEmiChecks {
    public static void run()throws Exception{
        var mc=Minecraft.getInstance();
        if(!net.minecraftforge.fml.ModList.get().isLoaded("jei"))try{Class.forName("mezz.jei.api.runtime.IJeiRuntime");throw new AssertionError("JEI leaked into EMI-only runtime");}catch(ClassNotFoundException expected){}
        check(!EmiApi.getRecipeManager().getRecipes().isEmpty(),"EMI is not loaded");
        var recipe=PortVerificationEmiPlugin.display;
        RecipeSources.request(recipe.getId(),document->{try{
            var editor=new RecipeEditorScreen(document,RecipePreview.category(recipe.getCategory()),"vjrm:forge_emi_machine",null,recipe);mc.setScreen(editor);
            check((boolean)field(editor,"usingSourceLayout"),"Native EMI source layout was not selected");editor.accept(document.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow(),new ItemStack(Items.EMERALD));
            var layout=(RecipePreview.Layout)field(editor,"layout");check(layout instanceof EmiNativePreview,"Native EMI UI disappeared");
            var output=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow();check(((SlotWidget)output.nativeSlot()).getStack().getEmiStacks().get(0).getItemStack().is(Items.EMERALD),"Native renderer did not update");
            System.out.println("PASS Forge native EMI layout and edited original widget renderer");
            PortClientChecks.viewerReady=true;
        }catch(Throwable ex){PortClientChecks.fail(ex);}},error->PortClientChecks.fail(new AssertionError(error)));
    }
}
