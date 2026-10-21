package com.pureneko.vjrm.client;

import com.google.gson.JsonObject;
import com.pureneko.vjrm.recipe.*;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.jemi.JemiRecipe;
import dev.emi.emi.screen.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.Path;
import java.util.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Actual Astral JEMI page plus manual editing of its previously undetectable Lumen field. */
final class AstralEmiCopyChecks {
    private static int phase,frames,index,originalAmount;
    private static ResourceLocation source;
    private static RecipeScreen page;
    private static WidgetGroup group;
    private static dev.emi.emi.api.widget.Widget button;
    private static RecipeEditorScreen editor;
    private static ManualFieldScreen picker;
    private static JsonObject expected;
    static void start()throws Exception{
        JemiRecipe<?> recipe=null;
        for(var candidate:EmiApi.getRecipeManager().getRecipes()){
            if(!(candidate instanceof JemiRecipe<?> jemi)||!jemi.category.getRecipeType().getUid().toString().equals("astralsorcery:altar_crafting"))continue;
            var lumen=(List<?>)jemi.recipe.getClass().getMethod("getRequiredLumen").invoke(jemi.recipe);
            if(!lumen.isEmpty()){recipe=jemi;break;}
        }
        check(recipe!=null,"No real Astral altar recipe with Lumen input");
        source=RecipeIdentities.id(recipe.recipe);check(source!=null,"Astral JEMI recipe has no source identity");
        check(recipe.originalId==null,"Astral test no longer reproduces missing JEI registry ID");
        EmiApi.displayRecipe(recipe);page=(RecipeScreen)Minecraft.getInstance().screen;
        var current=RecipeScreen.class.getDeclaredField("currentPage");current.setAccessible(true);
        @SuppressWarnings("unchecked") var groups=(List<WidgetGroup>)current.get(page);
        var selected=recipe;group=groups.stream().filter(g->g.recipe==selected).findFirst().orElseThrow();
        var buttons=group.widgets.stream().filter(w->w.getClass().getSimpleName().equals("CopyWidget")).toList();
        check(buttons.size()==1,"Real Astral EMI page has no VJRM copy button");button=buttons.get(0);phase=0;frames=0;
    }
    static boolean rendered(Path out)throws Exception{
        if(++frames<20)return false;var mc=Minecraft.getInstance();
        if(phase==0){
            capture(out,"astral-emi-copy-page.png");var b=button.getBounds();check(page.mouseClicked(group.x()+b.x()+4,group.y()+b.y()+4,0),"Astral EMI copy button did not handle click");phase=1;frames=0;return false;
        }
        if(phase==1){
            if(!(mc.screen instanceof RecipeEditorScreen screen))return false;editor=screen;
            var document=(RecipeDocument)field(editor,"document");expected=RecipeSource.read(mc.getSingleplayerServer(),source).json();
            check(document.json().equals(expected),"Real Astral EMI copy lost raw JSON");capture(out,"astral-emi-editor.png");
            var required=expected.getAsJsonArray("required_lumen");index=0;while(index<required.size()&&required.get(index).getAsJsonObject().get("amount").getAsInt()<=0)index++;
            check(index<required.size(),"Astral Lumen fixture has no positive amount");originalAmount=required.get(index).getAsJsonObject().get("amount").getAsInt();
            String path="required_lumen/"+index;check(document.fields().stream().noneMatch(f->f.label().equals(path)),"Lumen fixture is already auto-detected");
            var layout=(RecipePreview.Layout)field(editor,"layout");var slot=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.INPUT&&s.getAllIngredients().anyMatch(v->v.getIngredient().getClass().getName().endsWith(".lumen.LumenStack"))).findFirst().orElseThrow();
            var bounds=RecipeEditorScreen.class.getDeclaredMethod("screenRect",RecipePreview.Slot.class);bounds.setAccessible(true);var rect=(Rect2i)bounds.invoke(editor,slot);float scale=(float)field(editor,"uiScale");editor.mouseClicked((rect.getX()+2)*scale,(rect.getY()+2)*scale,0);
            editor.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(Component.translatable("vjrm.manual.open"))).findFirst().orElseThrow().onPress();
            picker=(ManualFieldScreen)mc.screen;((EditBox)field(picker,"pathBox")).setValue("/"+path);check(((Button)field(picker,"addButton")).active,"Could not select real Lumen JSON node");phase=2;frames=0;return false;
        }
        if(phase==2){
            capture(out,"astral-manual-lumen.png");((Button)field(picker,"addButton")).onPress();
            var document=(RecipeDocument)field(editor,"document");check(document.json().equals(expected),"Registering real Lumen field changed recipe data");
            var changed=expected.getAsJsonArray("required_lumen").get(index).getAsJsonObject();changed.addProperty("amount",originalAmount+1);
            ((EditBox)field(editor,"valueBox")).setValue(changed.toString());var apply=RecipeEditorScreen.class.getDeclaredMethod("applyValue");apply.setAccessible(true);apply.invoke(editor);
            check(document.json().equals(expected),"Manual Lumen edit changed other fields or failed validation");
            var layout=(RecipePreview.Layout)field(editor,"layout");boolean updated=false;
            for(var slot:layout.getRecipeSlotsView().getSlotViews())for(var value:slot.getAllIngredients().toList())if(value.getIngredient().getClass().getName().endsWith(".lumen.LumenStack")&&((Number)value.getIngredient().getClass().getMethod("getAmount").invoke(value.getIngredient())).intValue()==originalAmount+1)updated=true;
            check(updated,"Native Astral UI did not rebuild its edited custom Lumen ingredient");phase=3;frames=0;return false;
        }
        capture(out,"astral-manual-lumen-edited.png");System.out.println("PASS actual Astral JEMI copy button, raw JSON, manual custom Lumen binding/edit and native UI refresh: "+source);return true;
    }
    private static void capture(Path out,String name)throws Exception{try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(out.resolve(name));}}
}
