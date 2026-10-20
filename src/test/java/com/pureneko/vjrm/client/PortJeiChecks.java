package com.pureneko.vjrm.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import com.pureneko.vjrm.PortMachineRecipe;
import com.pureneko.vjrm.recipe.RecipeDocument;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;
public final class PortJeiChecks {
    public static void run()throws Exception{
        var mc=Minecraft.getInstance();var recipe=mc.level.getRecipeManager().getAllRecipesFor(PortMachineRecipe.TYPE).get(0);
        RecipeSources.request(recipe.getId(),document->{try{
            var editor=new RecipeEditorScreen(document,PortVerificationJeiPlugin.CATEGORY,"vjrm:forge_jei_machine",null,new PortVerificationJeiPlugin.Display(recipe));mc.setScreen(editor);
            check((boolean)field(editor,"usingSourceLayout"),"JEI wrapper source layout missing");editor.accept(document.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow(),new ItemStack(Items.EMERALD));check(field(editor,"layout")!=null,"JEI UI disappeared");
            System.out.println("PASS Forge native JEI wrapper layout and edited preview");
            PortClientChecks.viewerReady=true;
        }catch(Throwable ex){PortClientChecks.fail(ex);}},error->PortClientChecks.fail(new AssertionError(error)));
    }
}
