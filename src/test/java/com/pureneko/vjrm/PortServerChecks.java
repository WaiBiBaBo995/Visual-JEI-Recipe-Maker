package com.pureneko.vjrm;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.export.DatapackExporter;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
import java.util.*;

/** Opt-in checks against live 26.1.2 registries and the complete vanilla recipe set. */
@EventBusSubscriber(modid="vjrm")
public final class PortServerChecks {
    public static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("vjrm.verifyServer"))return;
        var server=event.getServer();var registries=server.registryAccess();
        Path output=Path.of("../../build/verification/server");Files.createDirectories(output);
        try{
            for(String type:RecipeDocument.TYPES){
                var doc=RecipeDocument.template(type);doc.decode(registries);
                check(doc.fields().stream().anyMatch(f->!f.output()),"No input fields: "+type);
                var field=doc.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();
                doc.accept(field,new ItemStack(Items.DIAMOND),3,registries);
                var decoded=doc.decode(registries);
                var stack=decoded.display().getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(server.overworld()));
                check(stack.is(Items.DIAMOND)&&stack.getCount()==3,"Incorrect output: "+type);
                RecipeDocument.copy(decoded,registries).decode(registries);
                System.out.println("PASS 26.1.2 template "+type);
            }
            var shaped=RecipeDocument.template("crafting_shaped");
            check(shaped.fields().size()==10,"Compact shaped ingredients not detected");
            var input=shaped.fields().stream().filter(f->f.label().equals("key/A")).findFirst().orElseThrow();
            shaped.tag(input,"minecraft:logs",1);shaped.decode(registries);
            check(shaped.get(input).getAsString().equals("#minecraft:logs"),"Wrong tag JSON");
            check(shaped.matches(input,new ItemStack(Items.OAK_LOG),registries),"Compact tag did not match");
            shaped.accept(input,new ItemStack(Items.EMERALD),1,registries);shaped.decode(registries);
            check(shaped.get(input).getAsString().equals("minecraft:emerald"),"Compact item replacement failed");
            shaped.remove(input);shaped.addCraftingInput(0,new ItemStack(Items.DIAMOND));shaped.decode(registries);
            var alternate=RecipeDocument.template("smelting").json();
            alternate.add("ingredient",JsonParser.parseString("[\"minecraft:stone\",\"minecraft:cobblestone\"]"));
            var alt=new RecipeDocument(alternate);alt.decode(registries);
            check(alt.fields().size()==2,"Alternative ingredient split into multiple fields");
            input=alt.fields().stream().filter(f->!f.output()).findFirst().orElseThrow();
            check(alt.values(input,registries).size()==2,"Alternative values not shown");
            alt.accept(input,new ItemStack(Items.DIRT),1,registries);alt.decode(registries);
            var numbers=RecipeDocument.template("smelting");
            var time=numbers.numericFields().stream().filter(f->f.label().equals("cookingtime")).findFirst().orElseThrow();
            numbers.setNumber(time,"321",registries);check(numbers.json().get("cookingtime").getAsInt()==321,"Numeric editing failed");
            var before=numbers.json();
            try{numbers.setNumber(time,"1.25",registries);throw new AssertionError("Fractional time accepted");}catch(IllegalArgumentException expected){}
            check(numbers.json().equals(before),"Invalid numeric edit was not rolled back");
            int total=0,withInputs=0;
            for(var holder:server.getRecipeManager().getRecipes()){
                var id=holder.id().identifier();
                if(server.getResourceManager().getResource(RecipeSource.resourceId(id)).isEmpty())continue;
                var source=RecipeSource.read(server,id);source.decode(registries);total++;
                if(source.fields().stream().anyMatch(f->!f.output()))withInputs++;
            }
            check(total>1300&&withInputs>1200,"Vanilla source/field coverage too low: "+total+"/"+withInputs);
            var source=RecipeSource.read(server,Identifier.parse("vjrm:source_check"));
            check(source.json().get("source_marker").getAsInt()==37,"Source extension lost");
            Path pack=DatapackExporter.serverDatapackRoot(server,"vjrm_generated");
            if(Files.exists(pack.resolve("data/vjrm/recipe/port_export.json"))){
                var reloaded=RecipeSource.read(server,Identifier.parse("vjrm:port_export"));
                check(reloaded.json().get("cookingtime").getAsInt()==321,"Exported datapack was not loaded after restart");
                System.out.println("PASS exported 101.1 datapack loaded by real RecipeManager");
            }
            DatapackExporter.writePackMeta(pack);
            Path file=DatapackExporter.export(pack,"vjrm","port_export",numbers.json(),true);
            new RecipeDocument(JsonParser.parseString(Files.readString(file)).getAsJsonObject()).decode(registries);
            var meta=JsonParser.parseString(Files.readString(pack.resolve("pack.mcmeta"))).getAsJsonObject().getAsJsonObject("pack");
            check(meta.getAsJsonArray("min_format").get(0).getAsInt()==101,"Wrong datapack version");
            try{DatapackExporter.export(pack,"vjrm","../../../../escape",numbers.json(),false);throw new AssertionError("Path traversal accepted");}catch(java.io.IOException expected){}
            var catalog=RecipeSource.catalog(server);check(catalog.stream().allMatch(s->Identifier.tryParse(s.id())!=null),"Catalog has ResourceKey text instead of ID");
            Files.writeString(output.resolve("result.txt"),"PASS: 8 templates; compact items/tags/alternatives; edit, add, remove; numeric rollback; "+total+" source recipes ("+withInputs+" with inputs); export/redecode and pack format 101.1; catalog; traversal rejection\n");
            System.out.println("PASS VJRM 26.1.2 SERVER CHECKS: "+total+" recipes");
        }catch(Throwable ex){
            ex.printStackTrace();Files.writeString(output.resolve("result.txt"),"FAIL: "+ex);
        }finally{server.halt(false);}
    }
}
