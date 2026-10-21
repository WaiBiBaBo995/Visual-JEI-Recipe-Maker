package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import dev.emi.emi.api.*;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;


public final class EmiClientChecks {
    private static final Path OUT=Path.of("../../build/verification/client");
    private static long started=System.currentTimeMillis();
    private static int stage,frames;
    private static boolean done;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("vjrm.verifyEmi")||done)return;
        var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-started>150000)throw new AssertionError("EMI verification timed out, stage "+stage);
            if(mc.level==null||mc.player==null||!RecipePreview.available())return;
            if(stage==0){
                if(EmiApi.getRecipeManager().getRecipes().isEmpty()||dev.emi.emi.screen.EmiScreenManager.isDisabled())return;
                stage=1;
                RecipeSources.catalog(samples->{try{
                    var templates=RecipePreview.templates(samples);EditorFeatureChecks.check(!templates.isEmpty(),"No EMI templates");
                    EditorFeatureChecks.check(templates.stream().anyMatch(t->t.serializer().equals("vjrm:test_machine")),"JSON-backed modded type missing");
                    if(!net.neoforged.fml.ModList.get().isLoaded("jei")){
                        try{Class.forName("mezz.jei.api.runtime.IJeiRuntime");throw new AssertionError("JEI leaked onto EMI-only runtime");}catch(ClassNotFoundException expected){}
                    }
                    var doc=RecipeDocument.template("crafting_shaped");var editor=new RecipeEditorScreen(doc,null,"vjrm:emi",null);mc.setScreen(editor);
                    for(int[] size:new int[][]{{427,240},{854,480}}){
                        editor.init(mc,size[0],size[1]);EditorFeatureChecks.check(Math.abs(editor.guiLeft()+editor.guiWidth()/2.0-size[0]/2.0)<=1,"EMI editor off center");
                        EditorFeatureChecks.check(Math.abs(editor.guiTop()+editor.guiHeight()/2.0-size[1]/2.0)<=1,"EMI editor off center vertically");
                        var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().label().equals("key/A")).findFirst().orElseThrow();
                        editor.accept(target.field(),VjrmEmiPlugin.ingredient(EmiStack.of(Items.EMERALD)));
                        EditorFeatureChecks.check(doc.json().getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString().equals("minecraft:emerald"),"EMI drop failed");
                    }
                    EditorFeatureChecks.run();
                    editor.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());mc.setScreen(editor);stage=2;frames=0;
                }catch(Throwable ex){fail(ex);}},error->fail(new AssertionError(error)));
            }
        }catch(Throwable ex){fail(ex);}
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post event){
        if(!Boolean.getBoolean("vjrm.verifyEmi")||done||stage<2||++frames<20)return;
        try{
            if(stage==6){if(EmiCopyChecks.rendered(OUT)){if(Boolean.getBoolean("vjrm.verifyAstral")){AstralEmiCopyChecks.start();stage=7;frames=0;}else finishVerified();}return;}
            if(stage==7){if(AstralEmiCopyChecks.rendered(OUT))finishVerified();return;}
            if(stage==3){
                EditorFeatureChecks.check(VerificationEmiPlugin.backgroundDraws>0,"Native EMI background was never drawn");
                screenshot("emi-native-machine.png");
                if(net.neoforged.fml.ModList.get().isLoaded("jei")){Class.forName("com.pureneko.vjrm.client.EmiJemiChecks").getMethod("run").invoke(null);stage=4;frames=0;}else finish();return;
            }
            if(stage==4){
                var checks=Class.forName("com.pureneko.vjrm.client.EmiJemiChecks");checks.getMethod("verifyRendered").invoke(null);screenshot("emi-jemi-machine.png");
                if(net.neoforged.fml.ModList.get().isLoaded("create")){checks.getMethod("runCreate").invoke(null);stage=5;frames=0;}else finish();return;
            }
            if(stage==5){screenshot("emi-jemi-create.png");finish();return;}
            var editor=(RecipeEditorScreen)Minecraft.getInstance().screen;
            EditorFeatureChecks.check(!dev.emi.emi.screen.EmiScreenBase.getCurrent().isEmpty(),"EMI does not recognize the editor bounds");
            int sidebarX=-1,sidebarY=-1;Object value=null;
            for(int y=25;y<editor.height-30&&value==null;y+=9)for(int x=editor.guiLeft()+editor.guiWidth()+10;x<editor.width-10;x+=9){
                var hovered=EmiApi.getHoveredStack(x,y,false).getStack();Object candidate=VjrmEmiPlugin.ingredient(hovered);
                if(candidate instanceof ItemStack item&&!item.isEmpty()){sidebarX=x;sidebarY=y;value=candidate;break;}
            }
            EditorFeatureChecks.check(value!=null,"EMI sidebar is missing or has no usable item");
            var target=editor.dropTargets(value).stream().filter(t->t.field().label().equals("key/A")).findFirst().orElseThrow();
            double tx=target.area().getX()+2,ty=target.area().getY()+2;
            editor.mouseClicked(sidebarX,sidebarY,0);editor.mouseDragged(tx,ty,0,tx-sidebarX,ty-sidebarY);editor.mouseReleased(tx,ty,0);
            var document=(RecipeDocument)EditorFeatureChecks.field(editor,"document");
            EditorFeatureChecks.check(document.matches(target.field(),value,Minecraft.getInstance().level.registryAccess()),"Real EMI sidebar mouse drag did not edit JSON");
            Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve("emi-editor.png"));}
            EmiLayoutChecks.run();VerificationEmiPlugin.backgroundDraws=0;stage=3;frames=0;
        }catch(Throwable ex){fail(ex);}
    }
    private static void screenshot(String name)throws Exception{Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(name));}}
    private static void finish()throws Exception{
        EmiCopyChecks.start();stage=6;frames=0;
    }
    private static void finishVerified()throws Exception{
        Files.writeString(OUT.resolve("result.txt"),"PASS: complete native EMI layouts, backgrounds/text/animations, actual JEMI conversion when present, updated item/fluid renderers, grid bindings/insertion, source isolation, actual sidebar mouse drag, centered UI and numeric/JSON editing\n");
        System.out.println("PASS VJRM EMI CLIENT CHECKS");done=true;Minecraft.getInstance().stop();
    }
    private static void fail(Throwable ex){ex.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}Minecraft.getInstance().stop();}
}
