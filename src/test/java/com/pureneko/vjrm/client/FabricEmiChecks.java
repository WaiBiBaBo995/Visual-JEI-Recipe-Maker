package com.pureneko.vjrm.client;
import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.platform.FluidStack;
import dev.emi.emi.api.EmiApi;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import java.nio.file.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;
public final class FabricEmiChecks {
    private static int stage,frames;private static boolean done;
    private static long started=System.currentTimeMillis();
    private static final Path OUT=Path.of("../../build/verification/client");
    public static void tick(){
        if(done||!Boolean.getBoolean("vjrm.verifyClient"))return;
        var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-started>240000)throw new AssertionError("EMI check timed out at "+stage);
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||!RecipePreview.available()||EmiApi.getRecipeManager().getRecipes().isEmpty())return;
            if(stage==0){
                boolean jei=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei");
                if(!jei)try{Class.forName("mezz.jei.api.IModPlugin");throw new AssertionError("JEI classes leaked into EMI-only runtime");}catch(ClassNotFoundException expected){}
                var fluid=(FluidStack)VjrmEmiPlugin.ingredient(FabricEmiStack.of(new FluidStack(Fluids.WATER,81000)));
                check(fluid.getFluid()==Fluids.WATER&&fluid.getAmount()==81000,"Native EMI fluid bridge lost Fabric bucket units");
                stage=1;RecipeSources.catalog(samples->{try{
                    check(!RecipePreview.templates(samples).isEmpty(),"EMI source templates missing");
                    for(String type:RecipeDocument.TYPES){
                        var editor=new RecipeEditorScreen(RecipeDocument.template(type),null,"vjrm:emi",null);mc.setScreen(editor);
                        check(field(editor,"layout") instanceof RecipePreview.Layout,"EMI native preview missing for "+type);
                    }
                    var doc=RecipeDocument.template("crafting_shaped");var editor=new RecipeEditorScreen(doc,null,"vjrm:emi",null);mc.setScreen(editor);
                    for(int[] size:new int[][]{{427,240},{854,480}}){
                        editor.init(mc,size[0],size[1]);check(Math.abs(editor.guiLeft()+editor.guiWidth()/2.0-size[0]/2.0)<=1,"EMI horizontal centering failed");
                        check(Math.abs(editor.guiTop()+editor.guiHeight()/2.0-size[1]/2.0)<=1,"EMI vertical centering failed");
                        var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().label().equals("key/A")).findFirst().orElseThrow();
                        editor.accept(target.field(),VjrmEmiPlugin.ingredient(dev.emi.emi.api.stack.EmiStack.of(Items.EMERALD)));
                        check(doc.json().getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString().equals("minecraft:emerald"),"EMI item drop failed");
                    }
                    EditorFeatureChecks.run();stage=2;frames=0;
                }catch(Throwable ex){fail(ex);}},e->fail(new AssertionError(e)));
            }else if(stage==2&&++frames>15){Files.createDirectories(OUT);EmiCopyChecks.start();stage=3;}
            else if(stage==3&&EmiCopyChecks.rendered(OUT)){
                String result="PASS: Fabric EMI native previews for 8 types, centered sizes, native fluid units, item drag, numeric/JSON/manual fields, actual EMI page button/copy, tooltip; JEI loaded="+net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei");
                Files.writeString(OUT.resolve("result.txt"),result);Files.writeString(OUT.resolve(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")?"result-both.txt":"result-emi.txt"),result);
                System.out.println(result);done=true;mc.stop();
            }
        }catch(Throwable ex){fail(ex);}
    }
    private static void fail(Throwable ex){ex.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}Minecraft.getInstance().stop();}
}
