package com.pureneko.vjrm;
import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.export.ScriptExporter;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="vjrm")
public final class GtServerChecks {
 @net.minecraftforge.eventbus.api.SubscribeEvent
 public static void started(net.minecraftforge.event.server.ServerStartedEvent event)throws Exception{
  if(!Boolean.getBoolean("vjrm.verifyGtServer"))return;
  var server=event.getServer();var out=Path.of("../../build/verification/gt-server");Files.createDirectories(out);
  try{
   var recipes=server.getRecipeManager().getRecipes().stream().filter(holder->GtRecipeSupport.isRecipe(holder)).toList();check(recipes.size()>1000,"GT recipes unavailable");
   int checked=0,items=0,fluids=0;var types=new java.util.HashMap<String,Integer>();RecipeDocument sample=null;
   for(var holder:recipes){
    var doc=GtRecipeSupport.copy(holder,server.registryAccess());String type=doc.type();if(types.getOrDefault(type,0)>=2)continue;types.merge(type,1,Integer::sum);
    doc.decode(server.registryAccess());var original=doc.json();String code=GtRecipeScript.generate(original,"vjrm:compat_check");var draft=GtRecipeScript.parse("// comment\n"+code);check(original.equals(draft.json()),"Script lost metadata");
    for(var field:doc.fields()){
     if(field.raw()||!field.chemical().isEmpty())continue;
     if(!field.fluid()){var before=doc.get(field).deepCopy();doc.accept(field,new ItemStack(Items.DIAMOND),2,server.registryAccess());doc.decode(server.registryAccess());doc.set(field,before);items++;}
     else{var before=doc.get(field).deepCopy();var values=doc.values(field,server.registryAccess());if(!values.isEmpty()){doc.accept(field,values.get(0),121,server.registryAccess());doc.decode(server.registryAccess());check(doc.quantity(field)==121,"Fluid amount lost");doc.set(field,before);fluids++;}}
    }
    check(doc.json().equals(original),"Unedited metadata changed");checked++;if(type.equals("gtceu:macerator"))sample=doc;
   }
   check(items>0&&fluids>0&&sample!=null,"No samples: items="+items+",fluids="+fluids+",types="+types.keySet());
   var added=new RecipeDocument(JsonParser.parseString("{\"type\":\"gtceu:chemical_reactor\",\"duration\":100}").getAsJsonObject());
   var addedItem=added.addGtContent(false,false,false,"minecraft:diamond","2",server.registryAccess());
   var addedFluid=added.addGtContent(true,true,false,"minecraft:water","123",server.registryAccess());
   var addedTick=added.addGtContent(false,true,true,"minecraft:water","5",server.registryAccess());
   added.decode(server.registryAccess());check(added.quantity(addedItem)==2&&added.quantity(addedFluid)==123&&!added.values(addedFluid,server.registryAccess()).isEmpty(),"GT append codec/quantity failed");
   added.remove(addedTick);added.decode(server.registryAccess());
   var reloadId=new ResourceLocation("vjrm:macerator/compat_check");var loaded=server.getRecipeManager().byKey(reloadId);
   if(loaded.isPresent()){var reloaded=GtRecipeSupport.copy(loaded.get(),server.registryAccess());check(reloaded.json().get("duration").getAsInt()==137,"KubeJS reload value changed");System.out.println("PASS actual KubeJS GT script loaded with exact ID and duration");}
   sample.setNumber(new RecipeDocument.NumericField(java.util.List.of("duration"),"137"),"137",server.registryAccess());
   String script=GtRecipeScript.generate(sample.json(),reloadId.toString());
   var file=ScriptExporter.export(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get(),"vjrm","macerator/compat_check",script,true);check(Files.readString(file).equals(script),"Script file changed");
   try{ScriptExporter.export(out,"vjrm","../escape",script,false);throw new AssertionError("Traversal accepted");}catch(java.io.IOException expected){}
   try{GtRecipeScript.parse(script+"ServerEvents.recipes(e => {});");throw new AssertionError("Extra executable JS accepted by visual draft");}catch(IllegalArgumentException expected){}
   try{GtRecipeScript.parse(script.replace(".merge({", ".merge({\n    \"unknown\": injectedCall(),\n"));throw new AssertionError("Executable payload value accepted");}catch(RuntimeException expected){}
   Files.writeString(out.resolve("result.txt"),"PASS: "+recipes.size()+" GT recipes; "+checked+" across "+types.size()+" types; item edits="+items+", fluid edits="+fluids+"; lossless JS; export; KubeJS reload="+loaded.isPresent()+"\n");
  }catch(Throwable ex){ex.printStackTrace();Files.writeString(out.resolve("result.txt"),"FAIL: "+ex);}
  finally{server.halt(false);}
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
