package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

public final class PortMekanismClientChecks {
    public static void run(){
        RecipeSources.catalog(samples->{try{
            var template=RecipePreview.templates(samples).stream().filter(t->t.serializer().equals("mekanism:compressing")&&t.category().nativeCategory()!=null).findFirst().orElseThrow(()->new AssertionError("Mekanism compressing category missing"));
            RecipeSources.request(template.id(),document->{try{
                var mc=Minecraft.getInstance();var editor=new RecipeEditorScreen(document,template.category(),"vjrm:mekanism_ui",null,template.display());mc.setScreen(editor);
                var chemical=document.fields().stream().filter(f->!f.chemical().isEmpty()&&!f.output()).findFirst().orElseThrow();
                var oxygen=ChemicalSupport.fromId("mekanism:oxygen",1,chemical.chemical(),com.mojang.serialization.JsonOps.INSTANCE);
                var target=editor.dropTargets(oxygen).stream().filter(t->t.field().equals(chemical)).findFirst().orElseThrow(()->new AssertionError("Mekanism chemical slot has no binding"));
                long before=document.chemicalAmount(chemical);editor.accept(chemical,oxygen);
                check(ChemicalSupport.id(document.values(chemical,mc.level.registryAccess()).get(0)).equals("mekanism:oxygen")&&document.chemicalAmount(chemical)==before,"Mekanism drag replacement lost type/amount");
                editor.mouseClicked(target.area().getX()+3,target.area().getY()+3,0);
                ((EditBox)field(editor,"valueBox")).setValue("mekanism:hydrogen");((EditBox)field(editor,"countBox")).setValue("9876543210");
                var apply=RecipeEditorScreen.class.getDeclaredMethod("applyValue");apply.setAccessible(true);apply.invoke(editor);
                check(document.chemicalAmount(chemical)==9876543210L&&ChemicalSupport.id(document.values(chemical,mc.level.registryAccess()).get(0)).equals("mekanism:hydrogen"),"Chemical ID/long amount UI failed");
                var layout=(RecipePreview.Layout)field(editor,"layout");check(layout!=null,"Mekanism UI disappeared");
                var slot=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.INPUT&&s.getAllIngredients().anyMatch(v->ChemicalSupport.isStack(v.getIngredient()))).findFirst().orElseThrow();
                check(slot.getAllIngredients().anyMatch(v->ChemicalSupport.id(v.getIngredient()).equals("mekanism:hydrogen")),"Mekanism preview did not update its chemical");
                check(slot.tooltip().stream().anyMatch(c->c.getString().contains("mekanism:hydrogen"))&&slot.tooltip().size()>1,"Chemical hover lost its name/id/native details");
                System.out.println("PASS Mekanism actual UI: chemical binding, native replacement, ID editing and long amount");PortClientChecks.viewerReady=true;
            }catch(Throwable ex){PortClientChecks.fail(ex);}},error->PortClientChecks.fail(new AssertionError(error)));
        }catch(Throwable ex){PortClientChecks.fail(ex);}},error->PortClientChecks.fail(new AssertionError(error)));
    }
}
