package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.screen.*;
import net.minecraft.client.*;
import java.nio.file.*;
import java.util.List;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Exercises the actual EMI recipe page with production defaults (decorators disabled). */
public final class EmiCopyChecks {
    private static boolean previousDecorators,previousJei,previousEmi;
    private static int frames,phase;
    private static RecipeScreen page;
    private static WidgetGroup group;
    private static dev.emi.emi.api.widget.Widget copy;
    private static double previousX,previousY;
    public static void start()throws Exception{
        previousDecorators=EmiConfig.showRecipeDecorators;
        previousJei=VjrmClientConfig.COPY_BUTTON.get();previousEmi=VjrmClientConfig.EMI_COPY_BUTTON.get();
        EmiConfig.showRecipeDecorators=false;
        VjrmClientConfig.COPY_BUTTON.set(false);VjrmClientConfig.EMI_COPY_BUTTON.set(true);
        var recipe=EmiApi.getRecipeManager().getRecipes().stream().filter(r->r.getId()!=null&&r.getId().toString().equals("vjrm:source_check")).findFirst().orElseThrow(()->new AssertionError("No real source recipe in EMI"));
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei"))recipe=EmiApi.getRecipeManager().getRecipes().stream().filter(r->r.getCategory().getId().toString().equals("vjrm_verify:raw")).findFirst().orElseThrow();
        EmiApi.displayRecipe(recipe);
        check(Minecraft.getInstance().screen instanceof RecipeScreen,"EMI did not open its recipe page");
        page=(RecipeScreen)Minecraft.getInstance().screen;
        var f=RecipeScreen.class.getDeclaredField("currentPage");f.setAccessible(true);
        @SuppressWarnings("unchecked") var groups=(List<WidgetGroup>)f.get(page);
        var selectedRecipe=recipe;group=groups.stream().filter(g->g.recipe==selectedRecipe).findFirst().orElseThrow();
        var buttons=group.widgets.stream().filter(w->w.getClass().getSimpleName().equals("CopyWidget")).toList();
        check(buttons.size()==1,"Production EMI page must have exactly one VJRM button, got "+buttons.size());
        copy=buttons.get(0);var b=copy.getBounds();check(b.width()==12&&b.height()==12,"Old JEI visibility setting hides the EMI copy button");
        check(group.widgets.stream().filter(w->w!=copy).map(w->w.getBounds()).noneMatch(o->b.x()<o.x()+o.width()&&b.x()+b.width()>o.x()&&b.y()<o.y()+o.height()&&b.y()+b.height()>o.y()),"Copy button overlaps an existing EMI control");
        VjrmClientConfig.toggle();check(copy.getBounds().width()==0,"EMI copy visibility toggle did not hide");
        VjrmClientConfig.toggle();check(copy.getBounds().width()==12,"EMI copy visibility toggle did not restore");
        check(!copy.getTooltip(b.x()+2,b.y()+2).isEmpty(),"Copy button has no hover hint");
        previousX=Minecraft.getInstance().mouseHandler.xpos();previousY=Minecraft.getInstance().mouseHandler.ypos();
        hover(group.x()+b.x()+6,group.y()+b.y()+6);
        phase=0;frames=0;
    }
    public static boolean rendered(Path out)throws Exception{
        if(++frames<20)return false;
        if(phase==3){
            try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(out.resolve("manual-fields.png"));}
            return true;
        }
        if(phase==0){
            try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(out.resolve("emi-copy-page.png"));}
            var b=copy.getBounds();
            check(page.mouseClicked(group.x()+b.x()+6,group.y()+b.y()+6,0),"Actual EMI page did not handle copy click");
            phase=1;frames=0;return false;
        }
        if(!(Minecraft.getInstance().screen instanceof RecipeEditorScreen editor))return false;
        var document=(RecipeDocument)field(editor,"document");
        check(document.json().get("source_marker").getAsInt()==37,"EMI copy lost raw source metadata");
        check(document.type().equals("minecraft:crafting_shaped"),"EMI copy changed source serializer");
        check(editor.sourceDescription().getString().contains("minecraft:crafting_shaped")&&editor.sourceDescription().getString().contains(RecipePreview.typeLabel("minecraft:crafting_shaped",RecipePreview.category(group.recipe.getCategory())).getString()),"Copied source has no ID/name");
        var layout=(RecipePreview.Layout)field(editor,"layout");
        var output=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow();
        check(output.tooltip().stream().anyMatch(c->c.getString().contains("minecraft:diamond")),"Native EMI output hover lost its ingredient ID");
        var target=editor.dropTargets(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)).stream().filter(t->t.field().output()).findFirst().orElseThrow().area();
        if(phase==1){hover(target.getX()+3,target.getY()+3);phase=2;frames=0;return false;}
        try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(out.resolve("emi-copy-editor.png"));}
        EmiConfig.showRecipeDecorators=previousDecorators;
        var mouse=Minecraft.getInstance().mouseHandler;
        for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mouse,name.equals("xpos")?previousX:previousY);}
        VjrmClientConfig.COPY_BUTTON.set(previousJei);VjrmClientConfig.EMI_COPY_BUTTON.set(previousEmi);VjrmClientConfig.SPEC.save();
        System.out.println("PASS real EMI recipe-page copy click with dev decorators disabled, one nonoverlapping button, visibility toggle, source name and ingredient tooltip");
        ManualFieldChecks.preview();phase=3;frames=0;return false;
    }
    private static void hover(int x,int y)throws Exception{
        var mc=Minecraft.getInstance();var window=mc.getWindow();
        for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,name.equals("xpos")?x*(double)window.getScreenWidth()/window.getGuiScaledWidth():y*(double)window.getScreenHeight()/window.getGuiScaledHeight());}
    }
}

