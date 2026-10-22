package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import java.nio.file.*;
import java.util.*;

/** Real viewer rendering, source-offset recognition, wrapping and exact clipboard regression checks. */
@net.neoforged.fml.common.EventBusSubscriber(modid="vjrm",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class IconClientChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void onTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(Boolean.getBoolean("vjrm.verifyIcons"))tick();}
    @net.neoforged.bus.api.SubscribeEvent public static void onRender(net.neoforged.neoforge.client.event.RenderFrameEvent.Post e){if(Boolean.getBoolean("vjrm.verifyIcons"))rendered();}

    private static final Path OUT=Path.of("../../build/verification/icons").toAbsolutePath().normalize();
    private static int stage,frames,ingredients,categories;private static boolean done;
    private static long started=System.currentTimeMillis();
    private static RecipeEditorScreen editor;private static String capture;
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static void tick(){
        if(done)return;var mc=Minecraft.getInstance();
        try{
            if(System.currentTimeMillis()-started>240000)throw new AssertionError("Icon check timeout at "+stage);
            if(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("BackupConfirmScreen")){mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().contains("Skip")||b.getMessage().getString().contains("跳过")).findFirst().ifPresent(Button::onPress);return;}
            if(mc.level==null||mc.player==null||!RecipePreview.available())return;
            if(stage==0&&++frames>60){
                if(!Boolean.getBoolean("vjrm.verifyEmi")&&!System.getProperty("vjrm.viewer","jei").equals("emi"))EditorFeatureChecks.run();
                var original=(RecipePreview.Provider)fieldStatic(RecipePreview.class,"provider");
                RecipePreview.install((RecipePreview.Provider)java.lang.reflect.Proxy.newProxyInstance(RecipePreview.class.getClassLoader(),new Class[]{RecipePreview.Provider.class},(proxy,method,args)->{
                    try{Object value=method.invoke(original,args);if(!done&&method.getName().equals("drawIngredientIcon")){check(Boolean.TRUE.equals(value),"Viewer failed to render ingredient icon");ingredients++;}if(!done&&method.getName().equals("drawCategoryIcon")&&Boolean.TRUE.equals(value))categories++;return value;}
                    catch(java.lang.reflect.InvocationTargetException ex){throw ex.getCause();}
                }));
                String duplicate="{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:stone\"},\"ingredient\":{\"item\":\"minecraft:dirt\"}}";
                check(IngredientAnnotations.read(duplicate,JsonSyntax.parse(duplicate),0,List.of()).isEmpty(),"Duplicate keys decorated the wrong source value");
                var document=RecipeDocument.template("smelting");var data=document.json();
                var fluid=new JsonObject();fluid.addProperty("id","minecraft:water");fluid.addProperty("amount",1000);data.add("fluid_input",fluid);
                data.addProperty("custom_value","minecraft:emerald");data.addProperty("group","minecraft:stone");data.addProperty("comment","minecraft:diamond");
                document.replace(data);document.addManualField(new RecipeDocument.Field(List.of("custom_value"),false,false));
                editor=new RecipeEditorScreen(document,null,"vjrm:icons",null);
                mc.setScreen(new JsonDraftScreen(editor,document.json(),document::replace));
                var box=(JsonEditBox)field(mc.screen,"box");
                @SuppressWarnings("unchecked") var icons=(List<IngredientAnnotations.Icon>)field(box,"ingredientIcons");
                check(icons.size()==4,"Expected input/output/fluid/manual icons, got "+icons.size()+"; fields="+document.fields());
                String source=box.getValue();for(var icon:icons)check(source.charAt(icon.end()-1)=='\"',"Icon is not after a quoted value");
                check(icons.stream().noneMatch(i->source.substring(0,i.end()).endsWith("\"minecraft:diamond\"")),"Comment decorated as ingredient");
                var narrow=new AnnotatedTextLayout(mc.font,source,85,icons);
                for(var line:narrow.lines){check(narrow.x(line.start(),line.end())<=85,"Wrapped icon overlaps right edge");for(int at=line.start();at<=line.end();at++)check(narrow.hit(narrow.lines.indexOf(line),narrow.x(line.start(),at))==at,"Caret hit test drift at "+at);}
                String previous=mc.keyboardHandler.getClipboard();
                try{boolean copied=false;for(int retry=0;retry<8&&!copied;retry++){mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("vjrm.json.copy").getString())).findFirst().orElseThrow().onPress();copied=source.equals(mc.keyboardHandler.getClipboard());if(!copied)Thread.sleep(25);}check(copied,"Icon text changed or system clipboard remained unavailable");}
                finally{mc.keyboardHandler.setClipboard(previous);}
                box.setValue("{invalid");check(((List<?>)field(box,"ingredientIcons")).isEmpty(),"Stale icons survived invalid text");
                box.setValue(source);var validate=JsonDraftScreen.class.getDeclaredMethod("validate");validate.setAccessible(true);validate.invoke(mc.screen);
                box.moveToStart();box.setFocused(false);
                var flow=(AnnotatedTextLayout)field(box,"visual");var bounds=new JsonArray();double scale=mc.getWindow().getGuiScale();
                for(var icon:icons){
                    var line=flow.lines.stream().filter(l->icon.end()>l.start()&&icon.end()<=l.end()).findFirst().orElseThrow();
                    var rect=new JsonObject();rect.addProperty("kind",icon.ingredient() instanceof net.minecraft.world.item.ItemStack?"item":"fluid");
                    rect.addProperty("x",(box.getX()+36+flow.x(line.start(),icon.end())-17)*scale);rect.addProperty("y",(box.getY()+4+flow.lines.indexOf(line)*18)*scale);rect.addProperty("size",16*scale);bounds.add(rect);
                }
                Files.createDirectories(OUT);Files.writeString(OUT.resolve("icon-bounds.json"),bounds.toString());
                capture="json-icons.png";stage=1;frames=0;
            }else if(stage==2){
                mc.setScreen(new RecipeTypeScreen(editor));capture="type-icons.png";stage=3;frames=0;
            }else if(stage==4){
                check(ingredients>0&&categories>0,"Actual JSON/category icons were not rendered: "+ingredients+" / "+categories);
                var data=((RecipeDocument)field(editor,"document")).json();data.addProperty("type","gtceu:chemical_reactor");
                String script=GtRecipeScript.generate(data,"vjrm:icons").replace("\"ingredient\"", "/* preserved comment */ \"ingredient\"");
                mc.setScreen(new JsDraftScreen(editor,script,draft->{}));var box=(JsonEditBox)field(mc.screen,"box");
                check(((List<?>)field(box,"ingredientIcons")).size()==4,"JS item/fluid/manual icons lost after comment offsets");
                check(box.getValue().equals(script),"JS text changed by decorations");box.setFocused(false);
                capture="js-icons.png";stage=5;frames=0;
            }else if(stage==6){
                check(ingredients>0&&categories>0,"Actual icons were never drawn");
                Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"PASS: exact item/fluid/manual-field string annotations; ordinary ID exclusion; native viewer ingredient/category icons rendered; wrapping/caret/hit-test; invalid-draft clearing; clipboard unchanged; screenshots\n");
                String viewer=Boolean.getBoolean("vjrm.verifyEmi")||System.getProperty("vjrm.viewer","jei").equals("emi")?"emi":"jei";
                var archive=OUT.resolve(viewer);Files.createDirectories(archive);
                try(var files=Files.list(OUT)){for(var file:files.filter(p->p.toString().endsWith(".png")||p.getFileName().toString().equals("icon-bounds.json")||p.getFileName().toString().equals("result.txt")).toList())Files.copy(file,archive.resolve(file.getFileName()),StandardCopyOption.REPLACE_EXISTING);}
                System.out.println("PASS VJRM ICON CHECKS: ingredient draws="+ingredients+", category draws="+categories);done=true;mc.stop();
            }
        }catch(Throwable ex){fail(ex);}
    }
    private static Object fieldStatic(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    static void rendered(){
        if(done||capture==null||++frames<35||(stage==1&&ingredients<20)||(stage==3&&categories<40))return;
        try{Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(capture));}capture=null;stage++;frames=0;}
        catch(Throwable ex){fail(ex);}
    }
    private static void fail(Throwable ex){ex.printStackTrace();done=true;try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}Minecraft.getInstance().stop();}
}
