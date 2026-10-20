package com.pureneko.vjrm.client;

import com.pureneko.vjrm.CreateChecks;
import com.pureneko.vjrm.recipe.RecipeDocument;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Exercises real Create JEI categories, source network and edits; no compile dependency on Create. */

public final class CreateClientChecks {
    private static final Path OUTPUT=Path.of("../../build/verification/client");
    private static int index,frames;
    private static Runnable success,nextFrame;
    private static Consumer<Throwable> failure;
    private static String capture;
    private static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void begin(Runnable onSuccess,Consumer<Throwable> onFailure){
        success=onSuccess;failure=onFailure;
        RecipeSources.catalog(samples->{try{
            var templates=VjrmJeiPlugin.templates(samples);
            for(String type:List.of("mixing","compacting","mechanical_crafting","sequenced_assembly","cutting","filling","emptying"))check(templates.stream().anyMatch(t->t.serializer().equals("create:"+type)),"Create type missing in JEI list: "+type);
            nextSource();
        }catch(Throwable ex){failure.accept(ex);}},error->failure.accept(new AssertionError(error)));
    }
    @SuppressWarnings("unchecked") private static void nextSource(){
        if(index>=CreateChecks.SOURCES.size()){proxy(0);return;}
        String path=CreateChecks.SOURCES.get(index++);
        RecipeSources.request(ResourceLocation.fromNamespaceAndPath("create",path),document->{try{
            var mc=Minecraft.getInstance();var editor=new RecipeEditorScreen(document,null,"vjrm:create_check",null);mc.setScreen(editor);
            var layout=(IRecipeLayoutDrawable<?>)((RecipePreview.Layout)field(editor,"layout")).nativeLayout();check(layout!=null,"No Create layout: "+path);
            var bindings=new HashMap<IRecipeSlotDrawable,RecipeDocument.Field>();((Map<RecipePreview.Slot,RecipeDocument.Field>)field(editor,"bindings")).forEach((s,f)->bindings.put((IRecipeSlotDrawable)s.nativeSlot(),f));
            for(var slot:layout.getRecipeSlotsView().getSlotViews()){
                if(!(slot instanceof IRecipeSlotDrawable drawable) || slot.isEmpty() || slot.getRole()!=RecipeIngredientRole.INPUT && slot.getRole()!=RecipeIngredientRole.OUTPUT)continue;
                check(bindings.containsKey(drawable),"Unbound Create slot: "+path+" "+slot.getRole());
                var f=bindings.get(drawable);
                check(slot.getAllIngredients().anyMatch(i->document.matches(f,i.getIngredient(),mc.level.registryAccess())),"Wrong binding "+path+" "+f.label());
            }
            // The copy controller must exist even though Create marks its datapack recipes isSpecial().
            var source=mc.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("create",path)).orElseThrow();
            var sourceLayout=VjrmJeiPlugin.sourceLayout(layout.getRecipeCategory(),source).orElseThrow();
            check(VjrmJeiPlugin.runtime().getRecipeManager().getRecipeButtonControllerFactories().stream().map(f->f.createButtonController(sourceLayout)).anyMatch(Objects::nonNull),"Create copy button filtered: "+path);
            check(!editor.proxyRecipe(),"Native Create recipe marked proxy: "+path);
            var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->!t.field().output()).findFirst().orElseThrow();
            if(path.equals("compacting/diorite_from_flint")){
                var originalCount=document.json().getAsJsonArray("ingredients").size();editor.accept(target.field(),new ItemStack(Items.EMERALD));
                check(document.json().getAsJsonArray("ingredients").size()==originalCount,"Condensed replacement changed quantity");
                var group=(Map<String,List<RecipeDocument.Field>>)field(editor,"repeatedBindings");check(!group.isEmpty(),"Condensed group is absent");
                var n=group.values().stream().filter(g->g.size()>1).findFirst().orElseThrow();
                ((EditBox)field(editor,"countBox")).setValue("3");
                var accept=RecipeEditorScreen.class.getDeclaredMethod("accept",RecipeDocument.Field.class,Object.class,boolean.class);accept.setAccessible(true);accept.invoke(editor,n.getFirst(),new ItemStack(Items.EMERALD),true);
                check(document.json().getAsJsonArray("ingredients").asList().stream().filter(e->e.isJsonObject()&&e.getAsJsonObject().has("item")&&e.getAsJsonObject().get("item").getAsString().equals("minecraft:emerald")).count()==3,"UI count failed for condensed ingredients");
            }else editor.accept(target.field(),new ItemStack(Items.EMERALD));
            check(document.values(target.field(),mc.level.registryAccess()).stream().anyMatch(v->v instanceof ItemStack item&&item.is(Items.EMERALD)),"Create input edit failed: "+path);
            if(path.equals("mixing/tea")){
                var fluid=editor.dropTargets(new FluidStack(Fluids.WATER,250)).stream().filter(t->!t.field().output()&&document.get(t.field()).getAsJsonObject().has("tag")).findFirst().orElseThrow();
                ((EditBox)field(editor,"countBox")).setValue("1");editor.accept(fluid.field(),new FluidStack(Fluids.WATER,1000));
                check(document.get(fluid.field()).getAsJsonObject().get("amount").getAsInt()==250,"Fluid drag changed original quantity");
                check(document.get(fluid.field()).getAsJsonObject().get("type").getAsString().equals("neoforge:single"),"Create fluid drag failed");
            }
            check(field(editor,"layout")!=null,"Layout lost after edit: "+path);document.decode(mc.level.registryAccess());
            if(path.equals("mechanical_crafting/crushing_wheel"))check(bindings.values().stream().noneMatch(f->f.label().startsWith("$crafting")),"Mechanical grid uses vanilla 3x3 mapping");
            if(path.equals("sequenced_assembly/precision_mechanism"))for(String f:List.of("ingredient","results/0","sequence/0/ingredients/1","sequence/1/ingredients/1","sequence/2/ingredients/1"))check(bindings.values().stream().anyMatch(v->v.label().equals(f)),"Sequence visible slot missing: "+f);
            capture="create-"+path.replace('/','-')+".png";frames=0;nextFrame=CreateClientChecks::nextSource;
            System.out.println("PASS Create JEI layout/bindings/input edit: "+path);
        }catch(Throwable ex){failure.accept(ex);}},error->failure.accept(new AssertionError(path+": "+error)));
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void proxy(int index){
        if(index>=3){success.run();return;}
        String uid=List.of("automatic_shapeless","automatic_packing","block_cutting").get(index);
        IRecipeCategory category=VjrmJeiPlugin.runtime().getRecipeManager().createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().toString().equals("create:"+uid)).findFirst().orElseThrow();
        Object display;try(var stream=VjrmJeiPlugin.runtime().getRecipeManager().createRecipeLookup(category.getRecipeType()).get()){display=stream.findFirst().orElseThrow();}
        var id=VjrmJeiPlugin.sourceId(category,display);
        RecipeSources.request(id,document->{try{
            var mc=Minecraft.getInstance();var type=document.type();var editor=new RecipeEditorScreen(document,category,"vjrm:proxy_check",null,display);mc.setScreen(editor);
            check(editor.proxyRecipe(),"Proxy label missing: "+uid);
            check(editor.sourceDescription().getString().contains(type),"True serializer missing in source label");
            var layout=(IRecipeLayoutDrawable<?>)((RecipePreview.Layout)field(editor,"layout")).nativeLayout();check(layout!=null,"No canonical layout for "+uid);
            check(layout.getRecipeCategory().getRecipeType().getUid().getNamespace().equals("minecraft"),"Proxy did not select canonical source layout: "+uid);
            var output=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().output()).findFirst().orElseThrow();editor.accept(output.field(),new ItemStack(Items.EMERALD));
            check(document.type().equals(type),"Proxy rewrote original serializer: "+uid);document.decode(mc.level.registryAccess());
            capture="create-proxy-"+uid+".png";frames=0;nextFrame=()->proxy(index+1);
            System.out.println("PASS Create proxy source label/canonical layout/type preservation: "+uid+" -> "+type);
        }catch(Throwable ex){failure.accept(ex);}},error->failure.accept(new AssertionError(error)));
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post event){
        if(capture==null||++frames<5)return;
        try{
            Files.createDirectories(OUTPUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUTPUT.resolve(capture));}
            capture=null;var next=nextFrame;nextFrame=null;next.run();
        }catch(Throwable ex){capture=null;failure.accept(ex);}
    }
}
