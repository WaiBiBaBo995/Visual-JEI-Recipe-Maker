package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import mezz.jei.api.gui.buttons.*;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.*;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Uses the registered JEI copy controller on an actual raw Recipe category. */
final class RawRecipeCopyChecks {
    private static int phase,frames;
    private static ResourceLocation expected;
    private static boolean visibility,emiVisibility;
    static void start()throws Exception{
        var runtime=VjrmJeiPlugin.runtime();var recipe=RawRecipeJeiChecks.recipe();
        IRecipeCategory<?> category=RawRecipeJeiChecks.CATEGORY;Object display=recipe;
        if(Boolean.getBoolean("vjrm.verifyAstral")){
            int checked=0,categories=0;
            for(var c:runtime.getRecipeManager().createRecipeCategoryLookup().get().toList()){
                if(!c.getRecipeType().getUid().getNamespace().equals("astralsorcery"))continue;
                var recipes=recipes(c);if(recipes.isEmpty())continue;categories++;
                for(var value:recipes){check(VjrmJeiPlugin.sourceId(c,value)!=null,"Astral recipe source was not resolved: "+c.getRecipeType().getUid());checked++;}
                if(c.getRecipeType().getUid().getPath().equals("altar_crafting")){category=c;display=recipes.get(0);}
            }
            check(categories>=8&&checked>20,"Astral recipe categories were not fully loaded");
            check(category.getRecipeType().getUid().getNamespace().equals("astralsorcery"),"No real Astral altar category");
            System.out.println("PASS real Astral Sorcery raw recipe identity: "+categories+" categories, "+checked+" recipes");
        }
        expected=VjrmJeiPlugin.sourceId(category,display);check(expected!=null,"No source ID for raw recipe");
        var layout=VjrmJeiPlugin.sourceLayout(category,display).orElseThrow();
        var controller=runtime.getRecipeManager().getRecipeButtonControllerFactories().stream().map(f->f.createButtonController(layout)).filter(java.util.Objects::nonNull).filter(c->c.getClass().getName().startsWith("com.pureneko.vjrm.client.VjrmJeiPlugin")).findFirst().orElseThrow(()->new AssertionError("JEI raw recipe has no VJRM copy button"));
        visibility=VjrmClientConfig.COPY_BUTTON.get();emiVisibility=VjrmClientConfig.EMI_COPY_BUTTON.get();VjrmClientConfig.COPY_BUTTON.set(true);
        class State implements IButtonState {
            boolean visible;
            public void setIcon(mezz.jei.api.gui.drawable.IDrawable icon){}
            public void setActive(boolean active){}
            public void setVisible(boolean visible){this.visible=visible;}
            public void setForcePressed(boolean pressed){}
        }
        var state=new State();controller.initState(state);check(state.visible,"JEI copy controller is hidden when enabled");
        var input=(IJeiUserInput)java.lang.reflect.Proxy.newProxyInstance(IJeiUserInput.class.getClassLoader(),new Class[]{IJeiUserInput.class},(proxy,method,args)->{if(method.getName().equals("isSimulate"))return false;throw new UnsupportedOperationException(method.getName());});
        check(controller.onPress(input),"JEI copy button did not handle press");phase=0;frames=0;
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static java.util.List<?> recipes(IRecipeCategory category){return VjrmJeiPlugin.runtime().getRecipeManager().createRecipeLookup(category.getRecipeType()).get().toList();}
    static boolean rendered(Path out)throws Exception{
        if(++frames<20)return false;var mc=Minecraft.getInstance();
        if(phase==1){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve("manual-fields.png"));}
            return true;
        }
        if(!(mc.screen instanceof RecipeEditorScreen editor))return false;
        var document=(RecipeDocument)field(editor,"document");
        var source=RecipeSource.read(mc.getSingleplayerServer(),expected);
        check(document.json().equals(source.json()),"JEI copy did not preserve exact raw JSON");
        check(field(editor,"layout")!=null,"Raw recipe editor lost its native UI");
        if(Boolean.getBoolean("vjrm.verifyAstral"))check(document.type().startsWith("astralsorcery:"),"Astral copy used another serializer");
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(Boolean.getBoolean("vjrm.verifyAstral")?"astral-editor.png":"raw-recipe-copy-editor.png"));}
        VjrmClientConfig.COPY_BUTTON.set(visibility);VjrmClientConfig.EMI_COPY_BUTTON.set(emiVisibility);
        System.out.println("PASS registered JEI raw-recipe copy button and exact source JSON: "+expected);
        ManualFieldChecks.preview();phase=1;frames=0;return false;
    }
}

