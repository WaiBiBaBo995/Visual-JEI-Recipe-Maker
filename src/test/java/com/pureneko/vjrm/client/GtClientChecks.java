package com.pureneko.vjrm.client;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="vjrm",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class GtClientChecks {
 private static int stage,frames;private static boolean done;private static final long START=System.currentTimeMillis();
 private static RecipeEditorScreen editor;private static String capture;private static Path serverFile;
 private static final Path OUT=Path.of("../../build/verification/gt-client");
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("vjrm.verifyGt")||done )return;
  var mc=Minecraft.getInstance();
  try{
   if(System.currentTimeMillis()-START>420000)throw new AssertionError("GT client timeout at "+stage);
   if(mc.level==null&&mc.screen!=null&&++frames%100==0)System.out.println("GT SCREEN "+mc.screen.getClass().getName()+" "+mc.screen.getTitle().getString());
   if(mc.level==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("BackupConfirmScreen")){
    mc.screen.children().stream().filter(c->c instanceof Button).map(c->(Button)c).filter(b->b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinSkipButton").getString())).findFirst().ifPresent(Button::onPress);
   }
   if(mc.level==null||mc.player==null||!RecipePreview.available()||mc.getOverlay()!=null)return;
   if(stage==0){
    stage=1;
    String helper=net.neoforged.fml.ModList.get().isLoaded("emi")?"GtEmiSource":"GtJeiSource";
    var source=(Object[])Class.forName("com.pureneko.vjrm.client."+helper).getDeclaredMethod("choose").invoke(null);
    if(source==null){stage=0;return;}
    RecipeSources.request((ResourceLocation)source[0],doc->{editor=new RecipeEditorScreen(doc,source[1],"vjrm:chemical_reactor/client_"+System.nanoTime(),null,source[2]);mc.setScreen(editor);stage=2;frames=0;},error->fail(new AssertionError(error)));
   }else if(stage==2&&++frames>40){
    check(field(editor,"layout")!=null,"Native GT UI missing");
    var doc=(RecipeDocument)field(editor,"document");var bindings=(Map<?,?>)field(editor,"bindings");check(bindings.size()>=2,"GT ingredients not bound: "+bindings.size());
    check(editor.children().stream().filter(c->c instanceof Button).map(c->((Button)c).getMessage().getString()).anyMatch(t->t.equals(net.minecraft.network.chat.Component.translatable("vjrm.js").getString())),"Edit JS button missing");
    var input=doc.fields().stream().filter(f->!f.fluid()&&!f.output()&&f.chemical().isEmpty()).findFirst().orElseThrow();editor.accept(input,new ItemStack(Items.DIAMOND));doc.decode(mc.level.registryAccess());
    var fluid=doc.fields().stream().filter(RecipeDocument.Field::fluid).findFirst().orElseThrow();var values=doc.values(fluid,mc.level.registryAccess());check(!values.isEmpty(),"GT fluid values absent");doc.accept(fluid,values.get(0),121,mc.level.registryAccess());doc.decode(mc.level.registryAccess());call(editor,"refresh");
    var nativeValues=((RecipePreview.Layout)field(editor,"layout")).getRecipeSlotsView().getSlotViews().stream().flatMap(s->s.getAllIngredients()).map(RecipePreview.Value::getIngredient).toList();
    check(nativeValues.stream().anyMatch(v->v instanceof ItemStack item&&item.is(Items.DIAMOND)),"Native GT item remained stale");
    check(!(boolean)field(editor,"usingSourceLayout"),"GT preview stayed on original recipe");capture="gt-editor.png";stage=3;frames=0;
   }else if(stage==4){
    call(editor,"openDraft");check(mc.screen instanceof JsDraftScreen,"Wrong draft language");var screen=(JsDraftScreen)mc.screen;var box=(JsonEditBox)field(screen,"box");
    var parsed=GtRecipeScript.parse(box.getValue());parsed.json().addProperty("duration",137);box.replaceText("// VJRM JS check\n"+GtRecipeScript.generate(parsed.json(),parsed.id()));call(screen,"format");
    screen.children().stream().filter(c->c instanceof Button).map(c->(Button)c).filter(b->b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("vjrm.json.copy").getString())).findFirst().orElseThrow().onPress();
    check(mc.keyboardHandler.getClipboard().equals(box.getValue()),"JS clipboard failed");
    check(JsSyntax.tokens(box.getValue()).stream().anyMatch(t->t.kind()==JsonSyntax.Kind.FUNCTION),"No JS function highlighting");box.moveToStart();capture="gt-js-editor.png";stage=5;frames=0;
   }else if(stage==6){
    call(mc.screen,"apply");check(mc.screen==editor,"JS apply failed");var doc=(RecipeDocument)field(editor,"document");check(doc.json().get("duration").getAsInt()==137,"JS edit not applied");
    var export=RecipeEditorScreen.class.getDeclaredMethod("export",boolean.class);export.setAccessible(true);export.invoke(editor,true);
    String id=GtRecipeScript.fullId((String)field(editor,"recipeId"),doc.type());var resource=ResourceLocation.parse(id);Path clientFile=mc.gameDirectory.toPath().resolve("kubejs/server_scripts/vjrm").resolve(resource.getNamespace()).resolve(resource.getPath()+".js");check(Files.exists(clientFile),"Client script export missing: "+field(editor,"status"));
    var idBox=(EditBox)field(editor,"idBox");String serverId="vjrm:chemical_reactor/server_"+System.nanoTime();idBox.setValue(serverId);serverFile=mc.gameDirectory.toPath().resolve("kubejs/server_scripts/vjrm/vjrm/chemical_reactor/"+serverId.substring(serverId.lastIndexOf('/')+1)+".js");export.invoke(editor,false);stage=7;frames=0;
   }else if(stage==7&&++frames>60){
    check(Files.exists(serverFile),"Network server script export missing: "+field(editor,"status"));check(GtRecipeScript.parse(Files.readString(serverFile)).json().get("duration").getAsInt()==137,"Server script lost edit");
    Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"PASS: real GT native preview and item/fluid bindings; JS edit/apply, highlighting, format, clipboard; client and permission-checked server script exports\n");String viewer=net.neoforged.fml.ModList.get().isLoaded("emi")?"emi":"jei";
    Files.copy(OUT.resolve("result.txt"),OUT.resolve("result-"+viewer+".txt"),StandardCopyOption.REPLACE_EXISTING);
    var shots=OUT.resolve(viewer);Files.createDirectories(shots);for(String name:List.of("gt-editor.png","gt-js-editor.png"))Files.copy(OUT.resolve(name),shots.resolve(name),StandardCopyOption.REPLACE_EXISTING);
    done=true;mc.stop();
   }
  }catch(Throwable ex){fail(ex);}
 }
 @net.neoforged.bus.api.SubscribeEvent public static void rendered(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event){
  if(done||capture==null )return;
  if(++frames<10)return;
  try{Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(capture));}capture=null;stage=stage==3?4:6;frames=0;}catch(Throwable ex){fail(ex);}
 }
 private static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 private static void call(Object object,String method)throws Exception{var m=object.getClass().getDeclaredMethod(method);m.setAccessible(true);m.invoke(object);}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void fail(Throwable ex){if(done)return;done=true;ex.printStackTrace();try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"FAIL: "+ex);}catch(Exception ignored){}Minecraft.getInstance().stop();}
}
