package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid="vjrm",value=Dist.CLIENT)
public final class ThaumClientChecks {
    private static final Path OUT=Path.of("../../build/verification/thaum-client");
    private static final long START=System.currentTimeMillis();
    private static int stage,frames;private static boolean done,capturing;private static RecipeEditorScreen editor;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("vjrm.verifyThaum")||done)return;var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-START>300000)throw new AssertionError("Thaum client timeout "+stage);
            if(mc.level==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("BackupConfirmScreen"))mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button).map(c->(net.minecraft.client.gui.components.Button)c).filter(b->b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinSkipButton").getString())).findFirst().ifPresent(b->b.onPress(new net.minecraft.client.input.KeyEvent(257,0,0)));
            if(mc.level==null||mc.player==null||VjrmJeiPlugin.runtime()==null||mc.getOverlay()!=null)return;
            if(stage==0){stage=1;RecipeSources.request(Identifier.parse("thaumaturge:infusion/verdant_charm"),doc->{editor=new RecipeEditorScreen(doc,null,"vjrm:thaum_check",null);mc.setScreen(editor);stage=2;frames=0;},e->fail(new AssertionError(e)));}
            else if(stage==2&&++frames>30){
                check(field(editor,"layout")!=null,"Thaum native recipe UI missing");var bindings=(Map<?,RecipeDocument.Field>)field(editor,"bindings");

                check(bindings.values().stream().filter(f->f.path().getFirst().equals("components")).count()==4,"Components not independently bound: "+bindings.values());
                check(bindings.values().stream().filter(RecipeDocument.Field::aspect).count()==3,"Aspect slots not bound");
                var doc=(RecipeDocument)field(editor,"document");var component=doc.fields().stream().filter(f->f.label().equals("components/1")).findFirst().orElseThrow();
                editor.accept(component,new ItemStack(Items.DIAMOND));check(doc.matches(component,new ItemStack(Items.DIAMOND),mc.level.registryAccess()),"Component drop not applied");
                var aspect=doc.fields().stream().filter(RecipeDocument.Field::aspect).findFirst().orElseThrow();set(editor,"selected",aspect);
                ((EditBox)field(editor,"valueBox")).setValue("thaumaturge:ignis");((EditBox)field(editor,"countBox")).setValue("31");call(editor,"applyValue");
                check(doc.get(aspect).getAsJsonObject().get("aspect").getAsString().equals("thaumaturge:ignis")&&doc.quantity(aspect)==31,"Aspect text/amount edit failed");
                var value=doc.values(aspect,mc.level.registryAccess()).getFirst();check(IngredientTooltips.of(value).stream().anyMatch(c->c.getString().contains("thaumaturge:ignis")),"Aspect identity tooltip missing");
                var target=editor.dropTargets(value).getFirst().area();var window=mc.getWindow();
                for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,name.equals("xpos")?(target.getX()+3)*(double)window.getScreenWidth()/window.getGuiScaledWidth():(target.getY()+3)*(double)window.getScreenHeight()/window.getGuiScaledHeight());}
                stage=3;frames=0;
            }else if(stage==4){
                stage=5;frames=0;
                RecipeSources.request(Identifier.parse("thaumaturge:infusion/elemental_pickaxe"),doc->{editor=new RecipeEditorScreen(doc,null,"vjrm:duplicate_components",null);mc.setScreen(editor);},e->fail(new AssertionError(e)));
            }else if(stage==5&&++frames>30){
                var doc=(RecipeDocument)field(editor,"document");var bound=(Map<?,RecipeDocument.Field>)field(editor,"bindings");
                var parts=doc.fields().stream().filter(f->f.path().getFirst().equals("components")).toList();
                check(bound.values().stream().filter(f->f.path().getFirst().equals("components")).distinct().count()==parts.size(),"Duplicate components are ambiguous");
                var duplicated=parts.stream().filter(f->parts.stream().anyMatch(other->!f.equals(other)&&doc.get(f).equals(doc.get(other)))).findFirst().orElseThrow();
                var other=parts.stream().filter(f->!f.equals(duplicated)&&doc.get(f).equals(doc.get(duplicated))).findFirst().orElseThrow();var unchanged=doc.get(other).deepCopy();
                editor.accept(duplicated,new ItemStack(Items.DIAMOND));check(doc.get(other).equals(unchanged),"Editing one component changed its identical sibling");
                stage=6;frames=0;
            }else if(stage==7){Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"PASS: real Thaumaturge infusion UI; 4 independent components (including component predicates); 3 native aspect slots; duplicate components independently bound/edited; item drop and aspect id/amount editing; identity tooltip; screenshot\n");done=true;mc.stop();}
        }catch(Throwable ex){fail(ex);}
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post event){
        if(done||!Boolean.getBoolean("vjrm.verifyThaum")||(stage!=3&&stage!=6)||capturing||++frames<15)return;capturing=true;
        Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(),image->{try(image){Files.createDirectories(OUT);image.writeToFile(OUT.resolve(stage==3?"thaum-aspect-editor.png":"thaum-duplicate-components.png"));stage=stage==3?4:7;capturing=false;frames=0;}catch(Throwable ex){fail(ex);}});
    }
    private static Object field(Object value,String name)throws Exception{var f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    private static void set(Object value,String name,Object content)throws Exception{var f=value.getClass().getDeclaredField(name);f.setAccessible(true);f.set(value,content);}
    private static void call(Object value,String method)throws Exception{var m=value.getClass().getDeclaredMethod(method);m.setAccessible(true);m.invoke(value);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void fail(Throwable ex){done=true;ex.printStackTrace();try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}Minecraft.getInstance().stop();}
}
