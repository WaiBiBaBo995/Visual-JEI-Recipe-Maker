package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.jemi.JemiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Loaded only with both APIs present. Exercises EMI's real JEI compatibility conversion. */
public final class EmiJemiChecks {
    public static void run()throws Exception{
        var mc=Minecraft.getInstance();
        var converted=EmiApi.getRecipeManager().getRecipes().stream().filter(JemiRecipe.class::isInstance).map(r->(JemiRecipe<?>)r).filter(r->r.category.getRecipeType().getUid().toString().equals("vjrm_verify:test_machine")).findFirst().orElseThrow(()->new AssertionError("JEMI did not convert the test JEI category"));
        var holder=converted.getBackingRecipe();var document=RecipeDocument.copy(holder.value(),mc.level.registryAccess());
        var editor=new RecipeEditorScreen(document,RecipePreview.category(converted.getCategory()),"vjrm:jemi_machine",null,converted);mc.setScreen(editor);
        var layout=(RecipePreview.Layout)field(editor,"layout");check(layout instanceof EmiNativePreview,"JEMI full widget tree was not selected");
        editor.accept(document.fields().stream().filter(f->f.output()).findFirst().orElseThrow(),new ItemStack(Items.EMERALD));
        layout=(RecipePreview.Layout)field(editor,"layout");check(layout.nativeLayout()==converted,"JEMI source layout was replaced after editing");
        var slot=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow();
        check(((SlotWidget)slot.nativeSlot()).getStack().getEmiStacks().getFirst().getItemStack().is(Items.EMERALD),"JEMI slot renderer did not update");
        check(!converted.getOutputs().getFirst().getItemStack().is(Items.EMERALD),"Editing changed the registered JEMI recipe");
        VerificationJeiPlugin.backgroundDraws=0;
        System.out.println("PASS actual JEMI category conversion, full widget layout, edited slots and source isolation");
    }
    public static void verifyRendered(){check(VerificationJeiPlugin.backgroundDraws>0,"JEMI did not invoke the original JEI category draw method");}
    public static void runCreate()throws Exception{
        var mc=Minecraft.getInstance();
        for(String path:com.pureneko.vjrm.CreateChecks.SOURCES){
            var document=com.pureneko.vjrm.recipe.RecipeSource.read(mc.getSingleplayerServer(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create",path));
            var editor=new RecipeEditorScreen(document,null,"vjrm:create_emi_ui",null);mc.setScreen(editor);
            var layout=(RecipePreview.Layout)field(editor,"layout");
            check(layout!=null&&layout.nativeLayout() instanceof mezz.jei.api.gui.IRecipeLayoutDrawable<?>,"EMI lost Create's original UI: "+path);
            var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->!t.field().output()&&!t.field().fluid()&&!t.field().label().startsWith("$crafting")).findFirst().orElseThrow();
            editor.accept(target.field(),new ItemStack(Items.EMERALD));
            layout=(RecipePreview.Layout)field(editor,"layout");check(layout!=null&&layout.nativeLayout() instanceof mezz.jei.api.gui.IRecipeLayoutDrawable<?>,"Create UI disappeared after editing: "+path);
        }
        System.out.println("PASS EMI+JEMI complete Create UI and input edit: 16 native recipes, mechanical crafting and sequenced assembly");
    }
}
