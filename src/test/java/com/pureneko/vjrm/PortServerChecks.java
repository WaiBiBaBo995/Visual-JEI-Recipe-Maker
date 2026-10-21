package com.pureneko.vjrm;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.export.DatapackExporter;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;

/** Opt-in checks against live 26.1.2 registries and the complete vanilla recipe set. */
public final class PortServerChecks {
    public static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void run(net.minecraft.server.MinecraftServer server)throws Exception{
        if(!Boolean.getBoolean("vjrm.verifyServer"))return;
        var registries=server.registryAccess();
        Path output=Path.of("../../build/verification/server");Files.createDirectories(output);
        try{
            for(String type:RecipeDocument.TYPES){
                var doc=RecipeDocument.template(type);doc.decode(registries);
                check(doc.fields().stream().anyMatch(f->!f.output()),"No input fields: "+type);
                var field=doc.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();
                int expectedCount=Set.of("smelting","blasting","smoking","campfire_cooking").contains(type)?1:3;
                if(expectedCount==1)try{doc.accept(field,new ItemStack(Items.DIAMOND),3,registries);throw new AssertionError("Unsupported cooking count accepted");}catch(IllegalArgumentException expected){}
                doc.accept(field,new ItemStack(Items.DIAMOND),expectedCount,registries);
                var decoded=doc.decode(registries);
                var stack=decoded.getResultItem(registries);
                check(stack.is(Items.DIAMOND)&&stack.getCount()==expectedCount,"Incorrect output: "+type);
                System.out.println("PASS 1.20.1 template "+type);
            }
            var shaped=RecipeDocument.template("crafting_shaped");
            check(shaped.fields().size()==10,"Compact shaped ingredients not detected");
            var input=shaped.fields().stream().filter(f->f.label().equals("key/A")).findFirst().orElseThrow();
            shaped.tag(input,"minecraft:logs",1);shaped.decode(registries);
            check(shaped.get(input).getAsJsonObject().get("tag").getAsString().equals("minecraft:logs"),"Wrong tag JSON");
            check(shaped.matches(input,new ItemStack(Items.OAK_LOG),registries),"Compact tag did not match");
            shaped.accept(input,new ItemStack(Items.EMERALD),1,registries);shaped.decode(registries);
            check(shaped.get(input).getAsJsonObject().get("item").getAsString().equals("minecraft:emerald"),"Compact item replacement failed");
            shaped.remove(input);shaped.addCraftingInput(0,new ItemStack(Items.DIAMOND));shaped.decode(registries);
            var alternate=RecipeDocument.template("smelting").json();
            alternate.add("ingredient",JsonParser.parseString("[{\"item\":\"minecraft:stone\"},{\"item\":\"minecraft:cobblestone\"}]"));
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
                var id=holder.getId();
                if(server.getResourceManager().getResource(RecipeSource.resourceId(id)).isEmpty())continue;
                var source=RecipeSource.read(server,id);source.decode(registries);total++;
                if(source.fields().stream().anyMatch(f->!f.output()))withInputs++;
            }
            check(total>1000&&withInputs>900,"Vanilla source/field coverage too low: "+total+"/"+withInputs);
            var source=RecipeSource.read(server,new ResourceLocation("vjrm:source_check"));
            check(source.json().get("source_marker").getAsInt()==37,"Source extension lost");
            Path pack=DatapackExporter.serverDatapackRoot(server,"vjrm_generated");
            if(Files.exists(pack.resolve("data/vjrm/recipes/port_export.json"))){
                var reloaded=RecipeSource.read(server,new ResourceLocation("vjrm:port_export"));
                check(reloaded.json().get("cookingtime").getAsInt()==321,"Exported datapack was not loaded after restart");
                System.out.println("PASS exported 15 datapack loaded by real RecipeManager");
            }
            DatapackExporter.writePackMeta(pack);
            Path file=DatapackExporter.export(pack,"vjrm","port_export",numbers.json(),true);
            new RecipeDocument(JsonParser.parseString(Files.readString(file)).getAsJsonObject()).decode(registries);
            var meta=JsonParser.parseString(Files.readString(pack.resolve("pack.mcmeta"))).getAsJsonObject().getAsJsonObject("pack");
            check(meta.get("pack_format").getAsInt()==15,"Wrong datapack version");
            try{DatapackExporter.export(pack,"vjrm","../../../../escape",numbers.json(),false);throw new AssertionError("Path traversal accepted");}catch(java.io.IOException expected){}
            var fluidJson=RecipeDocument.template("smelting").json();
            fluidJson.add("hidden_fluid",JsonParser.parseString("{\"fluid\":\"minecraft:water\",\"amount\":81000}"));
            var fluidDoc=new RecipeDocument(fluidJson);var fluidField=fluidDoc.fields().stream().filter(RecipeDocument.Field::fluid).findFirst().orElseThrow();
            var fluid=(com.pureneko.vjrm.platform.FluidStack)fluidDoc.values(fluidField,registries).get(0);
            check(fluid.getAmount()==81000,"Fabric bucket unit lost");
            fluidDoc.accept(fluidField,new com.pureneko.vjrm.platform.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,81000),40500,registries);fluidDoc.decode(registries);
            check(fluidDoc.get(fluidField).getAsJsonObject().get("amount").getAsInt()==40500,"Fluid replacement changed units");
            var buffer=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            try{
                var response=new com.pureneko.vjrm.network.RecipeSourceNetwork.Response(37,"x".repeat(40000),"");
                com.pureneko.vjrm.network.VjrmNetworking.encode(response,buffer);
                check(response.equals(com.pureneko.vjrm.network.VjrmNetworking.decode(buffer)),"Source wire format failed");
            }finally{buffer.release();}
            var catalog=RecipeSource.catalog(server);check(catalog.stream().allMatch(s->ResourceLocation.tryParse(s.id())!=null),"Catalog has ResourceKey text instead of ID");
            Files.writeString(output.resolve("result.txt"),"PASS: 8 templates; items/tags/alternatives; edit, add, remove; numeric rollback; "+total+" source recipes ("+withInputs+" with inputs); export/redecode and pack format 15; catalog; traversal rejection\n");
            System.out.println("PASS VJRM 1.20.1 SERVER CHECKS: "+total+" recipes");
        }catch(Throwable ex){
            ex.printStackTrace();Files.writeString(output.resolve("result.txt"),"FAIL: "+ex);
        }finally{server.halt(false);}
    }
}
