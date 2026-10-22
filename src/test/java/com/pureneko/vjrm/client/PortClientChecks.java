package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import java.nio.file.*;
import java.util.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

@EventBusSubscriber(modid="vjrm",value=Dist.CLIENT)
public final class PortClientChecks {
    private static final Path OUT=Path.of("../../build/verification/client");
    private static final long START=System.currentTimeMillis();
    private static int stage,frames;
    private static boolean done;
    public static boolean viewerReady;
    private static RecipeEditorScreen editor;
    private static String exportPath;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(Boolean.getBoolean("vjrm.verifyIcons")||Boolean.getBoolean("vjrm.verifyGt")||!Boolean.getBoolean("vjrm.verifyClient")||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-START>180000)throw new AssertionError("Client verification timed out at "+stage);
            if(mc.level==null||mc.player==null||!RecipePreview.available())return;
            if(stage==0){
                stage=1;RecipeSources.catalog(samples->{try{
                    check(samples.stream().anyMatch(s->s.serializer().equals("vjrm:test_machine")),"Source catalog missing custom serializer");
                    RecipeSources.request(new ResourceLocation("vjrm","source_check"),document->{try{editor=new RecipeEditorScreen(document,null,"vjrm:forge_ui",null);mc.setScreen(editor);stage=2;}catch(Throwable ex){fail(ex);}},error->fail(new AssertionError(error)));
                }catch(Throwable ex){fail(ex);}},error->fail(new AssertionError(error)));
            }else if(stage==2&&++frames>15){
                for(int[] size:new int[][]{{427,240},{854,480}}){editor.init(mc,size[0],size[1]);check(Math.abs(editor.guiLeft()+editor.guiWidth()/2.0-size[0]/2.0)<=1,"Editor is not centered");check(field(editor,"layout")!=null,"No recipe UI");
                    var target=editor.dropTargets(new ItemStack(Items.EMERALD)).stream().filter(t->t.field().label().equals("key/A")).findFirst().orElseThrow();editor.accept(target.field(),new ItemStack(Items.EMERALD));
                    check(((RecipeDocument)field(editor,"document")).json().getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString().equals("minecraft:emerald"),"Slot edit failed");
                }
                EditorFeatureChecks.run();mc.setScreen(editor);
                exportPath="forge_network_"+System.nanoTime();((EditBox)field(editor,"idBox")).setValue("vjrm:"+exportPath);
                mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().op(mc.player.getGameProfile()));stage=3;frames=0;
            }else if(stage==3&&++frames>10){((Button)field(editor,"serverButton")).onPress();stage=4;frames=0;
            }else if(stage==4){
                var file=com.pureneko.vjrm.export.DatapackExporter.serverDatapackRoot(mc.getSingleplayerServer(),"vjrm_generated").resolve("data/vjrm/recipes/"+exportPath+".json");
                if(!Files.exists(file))return;check(Files.readString(file).contains("source_marker"),"Network export lost raw metadata");((Button)field(editor,"clientButton")).onPress();stage=5;frames=0;
            }
        }catch(Throwable ex){fail(ex);}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(Boolean.getBoolean("vjrm.verifyIcons")||Boolean.getBoolean("vjrm.verifyGt")||!Boolean.getBoolean("vjrm.verifyClient")||done||event.phase!=TickEvent.Phase.END||stage<5||++frames<20)return;
        try{
            Files.createDirectories(OUT);
            if(stage==9){if((boolean)Class.forName("com.pureneko.vjrm.client.EmiCopyChecks").getMethod("rendered",Path.class).invoke(null,OUT))finish();return;}
            if(stage==10){var checks=Class.forName("com.pureneko.vjrm.client.RawRecipeCopyChecks");var method=checks.getDeclaredMethod("rendered",Path.class);method.setAccessible(true);if((boolean)method.invoke(null,OUT))finish();return;}
            if(stage==5){capture("forge-editor.png");viewerReady=false;
                if(net.minecraftforge.fml.ModList.get().isLoaded("emi"))Class.forName("com.pureneko.vjrm.client.PortEmiChecks").getMethod("run").invoke(null);
                else Class.forName("com.pureneko.vjrm.client.PortJeiChecks").getMethod("run").invoke(null);
                stage=6;frames=0;return;
            }
            if(!viewerReady)return;
            if(stage==6){capture("forge-machine-ui.png");if(System.getProperty("vjrm.viewer").equals("both")){viewerReady=false;Class.forName("com.pureneko.vjrm.client.PortJemiChecks").getMethod("run").invoke(null);stage=7;frames=0;return;}}
            else if(stage==7)capture("forge-jemi-ui.png");
            else capture("forge-mekanism-ui.png");
            if(stage<8&&Boolean.getBoolean("vjrm.verifyMekanism")){viewerReady=false;Class.forName("com.pureneko.vjrm.client.PortMekanismClientChecks").getMethod("run").invoke(null);stage=8;frames=0;return;}
            if(net.minecraftforge.fml.ModList.get().isLoaded("emi")){Class.forName("com.pureneko.vjrm.client.EmiCopyChecks").getMethod("start").invoke(null);stage=9;frames=0;return;}
            var checks=Class.forName("com.pureneko.vjrm.client.RawRecipeCopyChecks");var start=checks.getDeclaredMethod("start");start.setAccessible(true);start.invoke(null);stage=10;frames=0;
        }catch(Throwable ex){fail(ex);}
    }
    private static void capture(String name)throws Exception{try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(name));}}
    private static void finish()throws Exception{Files.writeString(OUT.resolve("result.txt"),"PASS: Forge 1.20.1 "+System.getProperty("vjrm.viewer")+", centered editor, source/catalog network, server/client export, raw metadata, numeric/JSON editor, source names, slot tooltips and production EMI copy button\n");done=true;Minecraft.getInstance().stop();}
    public static void fail(Throwable ex){ex.printStackTrace();done=true;try{Files.createDirectories(OUT);capture("failure.png");Files.writeString(OUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}Minecraft.getInstance().stop();}
}
