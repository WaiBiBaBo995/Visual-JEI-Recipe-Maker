package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.network.RecipeNetwork;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Run in the real client with JEI: typed input, network requests and render-state extraction. */
public final class PortClientChecks {
    private static int stage,frames;
    private static final long started=System.currentTimeMillis();
    private static boolean done,capturing;
    private static RecipeEditorScreen editor;
    private static final Path OUTPUT=Path.of("../../build/verification/client");
    private static final ResourceLocation SOURCE=new ResourceLocation("vjrm:source_check");
    private static final String EXPORT_ID="vjrm:client_export_"+System.nanoTime();
    @SuppressWarnings({"rawtypes","unchecked"})
    private static IRecipeLayoutDrawable<?> sourceLayout(){
        var runtime=VjrmJeiPlugin.runtime();
        for(IRecipeCategory category:runtime.getRecipeManager().createRecipeCategoryLookup().get().toList()){
            try(var recipes=runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get()){
                var it=recipes.iterator();while(it.hasNext()){
                    Object value=it.next();
                    if(SOURCE.equals(VjrmJeiPlugin.sourceId(category,value)))return VjrmJeiPlugin.sourceLayout(category,value).orElseThrow();
                }
            }
        }
        throw new AssertionError("Custom datapack recipe missing from JEI");
    }
    public static void tick(){
        if(!Boolean.getBoolean("vjrm.verifyClient")||done)return;
        var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-started>240000)throw new AssertionError("Client checks timed out at "+stage);
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||VjrmJeiPlugin.runtime()==null)return;
            if(stage==0){
                stage=1;RecipeSources.request(SOURCE,doc->{
                    try{
                        check(doc.json().get("source_marker").getAsInt()==37,"Network lost original JSON");
                        editor=new RecipeEditorScreen(doc,null,"vjrm:client_export",null);mc.setScreen(editor);stage=2;frames=0;
                    }catch(Throwable ex){fail(ex);}
                },e->fail(new AssertionError(e)));
            }else if(stage==2&&++frames>15){
                if(!VjrmJeiPlugin.runtime().getIngredientListOverlay().isListDisplayed()&&frames<180)return;
                check(VjrmJeiPlugin.runtime().getIngredientListOverlay().isListDisplayed(),"JEI sidebar missing; size="+mc.getWindow().getGuiScaledWidth()+"x"+mc.getWindow().getGuiScaledHeight());
                check(field(editor,"layout")!=null,"No native JEI preview");
                var source=sourceLayout();
                var factories=VjrmJeiPlugin.runtime().getRecipeManager().getRecipeButtonControllerFactories();
                var controller=factories.stream().filter(f->f.getClass().getName().startsWith(VjrmJeiPlugin.class.getName()+"$" )).map(f->f.createButtonController(source)).filter(Objects::nonNull).findFirst().orElseThrow();
                class State implements mezz.jei.api.gui.buttons.IButtonState{
                    boolean visible;
                    public void setIcon(mezz.jei.api.gui.drawable.IDrawable icon){}
                    public void setActive(boolean v){}public void setVisible(boolean v){visible=v;}public void setForcePressed(boolean v){}
                }
                boolean previous=VjrmClientConfig.copyButton();
                try{VjrmClientConfig.COPY_BUTTON.set(false);var state=new State();controller.initState(state);check(!state.visible,"Copy button hide failed");VjrmClientConfig.COPY_BUTTON.set(true);controller.updateState(state);check(state.visible,"Copy button show failed");}
                finally{VjrmClientConfig.COPY_BUTTON.set(previous);}
                check(SOURCE.equals(VjrmJeiPlugin.sourceId(source.getRecipeCategory(),source.getRecipe())),"Wrong source recipe ID");
                var inventory=mc.player.getInventory();var saved=inventory.getItem(0).copy();
                try{
                    inventory.setItem(0,new ItemStack(Items.EMERALD,17));
                    for(int[] size:new int[][]{{427,240},{854,480}}){
                        editor.init(mc,size[0],size[1]);
                        check(Math.abs(editor.guiLeft()+editor.guiWidth()/2.-size[0]/2.)<=1,"Horizontal centering failed");
                        check(Math.abs(editor.guiTop()+editor.guiHeight()/2.-size[1]/2.)<=1,"Vertical centering failed");
                        var method=RecipeEditorScreen.class.getDeclaredMethod("inventoryRect",int.class);method.setAccessible(true);
                        var rect=(Rect2i)method.invoke(editor,0);float scale=(float)field(editor,"uiScale");
                        double x=(rect.getX()+9)*scale,y=(rect.getY()+9)*scale;
                        var doc=(RecipeDocument)field(editor,"document");var before=doc.json();
                        
                        editor.mouseClicked(x,y,0);editor.mouseReleased(x,y,0);check(doc.json().equals(before),"Click mutated recipe");
                        var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().label().equals("key/A")).findFirst().orElseThrow().area();
                        double tx=target.getX()+4,ty=target.getY()+4;
                        editor.mouseClicked(x,y,0);editor.mouseDragged(tx,ty,0,tx-x,ty-y);editor.mouseReleased(tx,ty,0);
                        check(doc.json().getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString().equals("minecraft:emerald"),"Typed drag failed");
                        check(inventory.getItem(0).getCount()==17,"Inventory consumed");
                    }
                }finally{inventory.setItem(0,saved);}
                editor.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
                stage=3;frames=0;
            }else if(stage==4){
                stage=5;RecipeSources.catalog(samples->{
                    try{check(!VjrmJeiPlugin.templates(samples).isEmpty(),"Template catalog empty");EditorFeatureChecks.run();stage=6;frames=0;}
                    catch(Throwable ex){fail(ex);}
                },e->fail(new AssertionError(e)));
            }else if(stage==7){
                mc.setScreen(new NumericFieldsScreen(editor,RecipeDocument.template("smelting"),()->{}));stage=8;frames=0;
            }else if(stage==9){ManualFieldChecks.preview();stage=10;frames=0;}
            else if(stage==11){
                check(PortRawRecipeChecks.raw!=null,"NeoForge raw recipe sync missing");
                check(SOURCE.equals(VjrmJeiPlugin.sourceId(PortRawRecipeChecks.CATEGORY,PortRawRecipeChecks.raw)),"Raw recipe without ID hook lost copy support");
                mc.setScreen(editor);
                var rawLayout=VjrmJeiPlugin.sourceLayout(PortRawRecipeChecks.CATEGORY,PortRawRecipeChecks.raw).orElseThrow();
                var controller=VjrmJeiPlugin.runtime().getRecipeManager().getRecipeButtonControllerFactories().stream().filter(f->f.getClass().getName().startsWith(VjrmJeiPlugin.class.getName()+"$")).map(f->f.createButtonController(rawLayout)).filter(Objects::nonNull).findFirst().orElseThrow();
                var input=(mezz.jei.api.gui.inputs.IJeiUserInput)java.lang.reflect.Proxy.newProxyInstance(mezz.jei.api.gui.inputs.IJeiUserInput.class.getClassLoader(),new Class[]{mezz.jei.api.gui.inputs.IJeiUserInput.class},(proxy,method,args)->method.getReturnType()==boolean.class?false:method.getReturnType()==int.class?0:null);
                check(controller.onPress(input),"Raw recipe copy button did not handle press");stage=13;frames=0;
            }else if(stage==13 && mc.screen instanceof RecipeEditorScreen copied && copied!=editor){
                var doc=(RecipeDocument)field(copied,"document");
                check(doc.json().get("source_marker").getAsInt()==37,"Actual copy button lost original JSON");
                check(field(copied,"layout")!=null,"Copied raw recipe has no native UI");
                var fluidTarget=copied.dropTargets(new com.pureneko.vjrm.platform.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,81000)).stream().filter(t->t.field().fluid()).findFirst().orElseThrow();
                copied.accept(fluidTarget.field(),new com.pureneko.vjrm.platform.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,81000));
                check(doc.json().getAsJsonObject("hidden_fluid").get("amount").getAsInt()==81000,"Fabric fluid drag changed source quantity");
                System.out.println("PASS actual raw recipe copy button");
                hoverOutput(copied);stage=14;frames=0;
            }else if(stage==14 && mc.screen instanceof RecipeEditorScreen copied){hoverOutput(copied);
            }else if(stage==15){
                mc.setScreen(editor);stage=12;frames=0;
                com.pureneko.vjrm.network.VjrmNetworking.sendToServer(new RecipeNetwork.ExportRequest(EXPORT_ID,((RecipeDocument)field(editor,"document")).json().toString(),true));
            }else if(stage==12&&++frames>30){
                var server=mc.getSingleplayerServer();check(server!=null,"No integrated server");
                var path=com.pureneko.vjrm.export.DatapackExporter.serverDatapackRoot(server,"vjrm_generated").resolve("data/vjrm/recipes/"+EXPORT_ID.substring(5)+".json");
                if(!Files.exists(path))return;
                new RecipeDocument(com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject()).decode(mc.level.registryAccess());
                Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("result.txt"),"PASS: source request/catalog/export network; JEI native UI, sidebar, raw recipe ID fallback and actual copy button; 2 sizes centered; typed drag without inventory consumption; numeric apply/rollback; tooltips; JSON completion, indentation, format, copy; manual field picker and binding; render screenshots\n");
                Files.copy(OUTPUT.resolve("result.txt"),OUTPUT.resolve("result-jei.txt"),StandardCopyOption.REPLACE_EXISTING);
                System.out.println("PASS VJRM fabric-1.20.1 CLIENT CHECKS");done=true;mc.stop();
            }
        }catch(Throwable ex){fail(ex);}
    }
    public static void rendered(){
        if(!Boolean.getBoolean("vjrm.verifyClient")||done||capturing)return;
        String name=switch(stage){case 3->"vanilla-editor.png";case 6->"json-editor.png";case 8->"numeric-editor.png";case 10->"manual-fields.png";case 14->"raw-recipe-copied.png";default->null;};
        if(name==null||++frames<12)return;
        capturing=true;
        try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){
            Files.createDirectories(OUTPUT);image.writeToFile(OUTPUT.resolve(name));stage++;frames=0;capturing=false;
        }catch(Throwable ex){fail(ex);}
    }
    private static void hoverOutput(RecipeEditorScreen screen)throws Exception{
        var target=screen.dropTargets(new ItemStack(Items.DIAMOND)).stream().filter(t->t.field().output()).findFirst().orElseThrow().area();
        var mc=Minecraft.getInstance();var window=mc.getWindow();
        for(String name:new String[]{"xpos","ypos"}){
            var f=net.minecraft.client.MouseHandler.class.getDeclaredField(name);f.setAccessible(true);
            f.setDouble(mc.mouseHandler,name.equals("xpos")?(target.getX()+3)*(double)window.getScreenWidth()/window.getGuiScaledWidth():(target.getY()+3)*(double)window.getScreenHeight()/window.getGuiScaledHeight());
        }
    }
    private static void fail(Throwable ex){
        ex.printStackTrace();done=true;
        try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}
        Minecraft.getInstance().stop();
    }
}
