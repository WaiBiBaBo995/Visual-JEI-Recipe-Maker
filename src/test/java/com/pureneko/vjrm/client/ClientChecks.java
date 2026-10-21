package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;

/** Opt-in in-process UI regression test, driven through the screen's public input methods. */

public final class ClientChecks {
    private static int stage,frames;
    private static long started=System.currentTimeMillis();
    private static String capture;
    private static boolean done;
    private static RecipeEditorScreen editor;
    private static final Path OUTPUT=Path.of("../../build/verification/client");
    private static Object field(Object object,String name) throws Exception {var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("vjrm.verifyClient")||Boolean.getBoolean("vjrm.verifyEmi")||done)return;
        var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-started>150000)throw new AssertionError("Client verification timed out at stage "+stage);
            if(mc.level==null||mc.player==null||VjrmJeiPlugin.runtime()==null)return;
            if(stage==0){
                stage=1;
                RecipeSources.request(ResourceLocation.fromNamespaceAndPath("vjrm","source_check"),document->{
                    try{editor=new RecipeEditorScreen(document,null,"vjrm:ui_check",null);mc.setScreen(editor);stage=2;frames=0;}
                    catch(Throwable ex){fail(ex);}
                },error->fail(new AssertionError(error)));
            }else if(stage==2&&++frames>15){
                check(VjrmJeiPlugin.runtime().getIngredientListOverlay().isListDisplayed(),"JEI sidebar is missing");
                check(editor.children().stream().filter(c->c instanceof Button).map(c->((Button)c).getMessage().getString()).noneMatch(s->s.equals("Clear slot")||s.equals("Back")),"Redundant buttons remain");
                var layout=(IRecipeLayoutDrawable<?>)((RecipePreview.Layout)field(editor,"layout")).nativeLayout();check(layout!=null,"No preview layout");
                var source=mc.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("vjrm","source_check")).orElseThrow();
                var sourceLayout=VjrmJeiPlugin.sourceLayout(layout.getRecipeCategory(),source).orElseThrow();
                var controller=VjrmJeiPlugin.runtime().getRecipeManager().getRecipeButtonControllerFactories().stream().map(f->f.createButtonController(sourceLayout)).filter(java.util.Objects::nonNull).findFirst().orElseThrow();
                class State implements mezz.jei.api.gui.buttons.IButtonState {
                    boolean visible=true;
                    public void setIcon(mezz.jei.api.gui.drawable.IDrawable icon){ }
                    public void setActive(boolean active){ }
                    public void setVisible(boolean value){visible=value;}
                    public void setForcePressed(boolean value){ }
                }
                boolean previousVisibility=VjrmClientConfig.copyButton();
                try{
                    VjrmClientConfig.COPY_BUTTON.set(false);var state=new State();controller.initState(state);check(!state.visible,"Copy button should start hidden");
                    VjrmClientConfig.toggle();controller.updateState(state);check(state.visible,"Copy toggle did not show button");
                    check(Files.readString(mc.gameDirectory.toPath().resolve("config/vjrm-client.toml")).contains("showJeiCopyButton = true"),"Copy toggle was not persisted");
                }finally{VjrmClientConfig.COPY_BUTTON.set(previousVisibility);VjrmClientConfig.SPEC.save();}
                var previous=mc.player.getInventory().getItem(0).copy();
                try{
                    mc.player.getInventory().setItem(0,new ItemStack(Items.EMERALD,17));
                    for(int[] size:new int[][]{{427,240},{854,480}}){
                        editor.init(mc,size[0],size[1]);
                        check(Math.abs(editor.guiLeft()+editor.guiWidth()/2.0-size[0]/2.0)<=1,"Editor is not horizontally centered");
                        check(Math.abs(editor.guiTop()+editor.guiHeight()/2.0-size[1]/2.0)<=1,"Editor is not vertically centered");
                        var method=RecipeEditorScreen.class.getDeclaredMethod("inventoryRect",int.class);method.setAccessible(true);
                        var rect=(Rect2i)method.invoke(editor,0);float scale=(float)field(editor,"uiScale");
                        double x=(rect.getX()+9)*scale,y=(rect.getY()+9)*scale;
                        var before=((RecipeDocument)field(editor,"document")).json();
                        editor.mouseClicked(x,y,0);editor.mouseReleased(x,y,0);
                        check(before.equals(((RecipeDocument)field(editor,"document")).json()),"Click alone changed recipe");
                        var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().label().equals("key/A")).findFirst().orElseThrow();
                        double tx=target.area().getX()+target.area().getWidth()/2.0,ty=target.area().getY()+target.area().getHeight()/2.0;
                        editor.mouseClicked(x,y,0);editor.mouseDragged(tx,ty,0,tx-x,ty-y);editor.mouseReleased(tx,ty,0);
                        var document=(RecipeDocument)field(editor,"document");
                        check(document.json().getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString().equals("minecraft:emerald"),"Inventory drag did not edit target");
                        check(mc.player.getInventory().getItem(0).getCount()==17,"Inventory was consumed");
                    }
                }finally{mc.player.getInventory().setItem(0,previous);}
                editor.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
                capture="vanilla-editor.png";stage=3;
            }else if(stage==4){
                stage=5;
                RecipeSources.catalog(samples->{
                    try{
                        check(VjrmJeiPlugin.templates(samples).stream().anyMatch(t->t.serializer().equals("vjrm:test_machine")),"Wrapped modded type missing from list");
                        RecipeSources.request(ResourceLocation.fromNamespaceAndPath("vjrm","test_machine"),document->{
                            try{
                                var holder=mc.level.getRecipeManager().getAllRecipesFor(com.pureneko.vjrm.TestMachineRecipe.TYPE).getFirst();
                                editor=new RecipeEditorScreen(document,VerificationJeiPlugin.CATEGORY,"vjrm:ui_machine",null,new VerificationJeiPlugin.Display(holder));mc.setScreen(editor);
                                check((boolean)field(editor,"usingSourceLayout"),"Wrapper layout fallback was not used");
                                var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().output()).findFirst().orElseThrow();
                                editor.accept(target.field(),new ItemStack(Items.EMERALD));
                                var layout=(IRecipeLayoutDrawable<?>)((RecipePreview.Layout)field(editor,"layout")).nativeLayout();
                                check(layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT).anyMatch(s->s.getDisplayedIngredient().map(i->i.getIngredient() instanceof ItemStack stack&&stack.is(Items.EMERALD)).orElse(false)),"Wrapped preview did not update");
                                check(((RecipeDocument)field(editor,"document")).json().get("source_marker").getAsInt()==37,"Original extra JSON was lost");
                                capture="wrapped-editor.png";frames=0;stage=6;
                            }catch(Throwable ex){fail(ex);}
                        },error->fail(new AssertionError(error)));
                    }catch(Throwable ex){fail(ex);}
                },error->fail(new AssertionError(error)));
            }else if(stage==7){
                EditorFeatureChecks.run();capture="json-editor.png";stage=10;frames=0;
            }else if(stage==11){
                mc.setScreen(new NumericFieldsScreen(editor,RecipeDocument.template("smelting"),()->{}));capture="numeric-editor.png";stage=12;frames=0;
            }else if(stage==13){
                stage=8;
                if(Boolean.getBoolean("vjrm.verifyCreate"))CreateClientChecks.begin(()->stage=9,ClientChecks::fail);else stage=9;
            }else if(stage==9){
                RawRecipeCopyChecks.start();stage=14;
            }else if(stage==15){
                Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("result.txt"),"PASS: source network, JEI sidebar, copy button visibility/persistence, 2 UI sizes, drag-only inventory without consumption, wrapped type catalog and edited preview, original fields preserved; real Create checks = "+Boolean.getBoolean("vjrm.verifyCreate")+"\n");
                System.out.println("PASS VJRM CLIENT CHECKS");done=true;mc.stop();
            }
        }catch(Throwable ex){fail(ex);}
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post event){
        if(Boolean.getBoolean("vjrm.verifyClient")&&!done&&stage==14){try{if(RawRecipeCopyChecks.rendered(OUTPUT))stage=15;}catch(Throwable ex){fail(ex);}return;}
        if(!Boolean.getBoolean("vjrm.verifyClient")||done||capture==null)return;
        if(++frames<10)return;
        try{
            Files.createDirectories(OUTPUT);
            try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUTPUT.resolve(capture));}
            capture=null;stage=switch(stage){case 3->4;case 10->11;case 12->13;default->7;};frames=0;
        }catch(Throwable ex){fail(ex);}
    }
    private static void fail(Throwable ex){
        ex.printStackTrace();done=true;
        try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){ }
        Minecraft.getInstance().stop();
    }
}
