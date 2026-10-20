package com.pureneko.vjrm;

import com.google.gson.JsonParser;
import com.pureneko.vjrm.recipe.*;
import com.pureneko.vjrm.export.DatapackExporter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.nio.file.*;
import java.util.*;

/** Tests the actual optional Create jar and its registered codecs/resources, without a compile dependency. */
public final class CreateChecks {
    public static final List<String> SOURCES=List.of("mixing/tea","mixing/brass_ingot","compacting/diorite_from_flint","crushing/deepslate_coal_ore","milling/dripstone_block","pressing/cardboard","cutting/andesite_alloy","splashing/sand","haunting/blackstone","filling/builders_tea","emptying/builders_tea","deploying/cogwheel","mechanical_crafting/crushing_wheel","sequenced_assembly/precision_mechanism","sequenced_assembly/sturdy_sheet","sequenced_assembly/track");
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static int run(MinecraftServer server) throws Exception {
        var registries=server.registryAccess();int checks=0;
        for(String path:SOURCES){
            var doc=RecipeSource.read(server,ResourceLocation.fromNamespaceAndPath("create",path));
            check(doc.type().startsWith("create:"),"Native Create source changed type: "+path);
            check(doc.decode(registries).isSpecial(),"Expected Create recipe-book special marker: "+path);
            for(var field:doc.fields()){
                var draft=new RecipeDocument(doc.json());
                if(field.fluid())draft.accept(field,new FluidStack(Fluids.WATER,250),250,registries);
                else draft.accept(field,new ItemStack(Items.DIAMOND),2,registries);
                draft.decode(registries);
                check(draft.type().equals(doc.type()),"Type lost on editing "+path);
                checks++;
            }
            for(var number:doc.numericFields())if(Set.of("processing_time","processingTime","loops","chance").contains(number.path().getLast())){
                String changed=number.path().getLast().equals("chance")?"0.375":number.path().getLast().equals("loops")?"3":"123";
                var numeric=new RecipeDocument(doc.json());numeric.setNumber(number,changed,registries);
                check(numeric.get(new RecipeDocument.Field(number.path(),false,false)).getAsBigDecimal().compareTo(new java.math.BigDecimal(changed))==0,"Create numeric field edit failed: "+number.label());checks++;
            }
            var output=doc.fields().stream().filter(f->f.output()).findFirst().orElseThrow();
            doc.accept(output,output.fluid()?new FluidStack(Fluids.WATER,500):new ItemStack(Items.EMERALD),output.fluid()?500:2,registries);
            var file=DatapackExporter.export(Path.of("build/verification/create-pack"),"vjrm_create_check",path,doc.json(),true);
            new RecipeDocument(JsonParser.parseString(Files.readString(file)).getAsJsonObject()).decode(registries);
            System.out.println("PASS Create source/edit/export/redecode: "+path);checks++;
        }
        var tea=RecipeSource.read(server,ResourceLocation.parse("create:mixing/tea"));
        var milk=tea.fields().stream().filter(f->f.fluid() && tea.get(f).getAsJsonObject().has("tag")).findFirst().orElseThrow();
        check(tea.values(milk,registries).stream().anyMatch(v->v instanceof FluidStack),"Fluid tag was classified as an item");
        tea.accept(milk,new FluidStack(Fluids.WATER,250),750,registries);tea.decode(registries);
        check(tea.get(milk).getAsJsonObject().get("type").getAsString().equals("neoforge:single"),"Tag discriminator not switched to single");
        tea.tag(milk,"c:milk");tea.decode(registries);
        check(tea.get(milk).getAsJsonObject().get("type").getAsString().equals("neoforge:tag") && tea.get(milk).getAsJsonObject().get("amount").getAsInt()==750,"Fluid tag discriminator/amount lost");checks++;
        var repeated=RecipeSource.read(server,ResourceLocation.parse("create:compacting/diorite_from_flint"));
        var first=repeated.fields().stream().filter(f->!f.output()&&!f.fluid()).findFirst().orElseThrow();
        var group=repeated.fields().stream().filter(f->!f.output()&&!f.fluid()&&repeated.get(f).equals(repeated.get(first))).toList();
        check(group.size()>1,"Fixture has no repeated inputs");
        repeated.acceptRepeated(group,new ItemStack(Items.EMERALD),3,registries);repeated.decode(registries);
        check(repeated.json().getAsJsonArray("ingredients").asList().stream().filter(e->e.isJsonObject()&&e.getAsJsonObject().has("item")&&e.getAsJsonObject().get("item").getAsString().equals("minecraft:emerald")).count()==3,"Repeated ingredient count not changed");checks++;
        var emeralds=repeated.fields().stream().filter(f->!f.output()&&!f.fluid()&&repeated.get(f).getAsJsonObject().has("item")&&repeated.get(f).getAsJsonObject().get("item").getAsString().equals("minecraft:emerald")).toList();
        repeated.tagRepeated(emeralds,"c:ingots/iron",2);repeated.decode(registries);
        check(repeated.json().getAsJsonArray("ingredients").asList().stream().filter(e->e.isJsonObject()&&e.getAsJsonObject().has("tag")&&e.getAsJsonObject().get("tag").getAsString().equals("c:ingots/iron")).count()==2,"Repeated tag quantity not changed");checks++;
        tea.tag(milk,"c:milk",500);tea.decode(registries);check(tea.quantity(milk)==500,"Fluid tag amount edit failed");checks++;
        var seq=RecipeSource.read(server,ResourceLocation.parse("create:sequenced_assembly/precision_mechanism"));
        check(seq.fields().stream().anyMatch(f->f.label().equals("sequence/0/ingredients/1")),"Nested ingredient list collapsed to alternatives");
        var original=seq.json();var f=seq.fields().stream().filter(a->a.label().equals("sequence/0/ingredients/1")).findFirst().orElseThrow();
        seq.accept(f,new ItemStack(Items.DIAMOND),1,registries);seq.decode(registries);
        check(seq.json().get("results").equals(original.get("results")) && seq.json().get("loops").equals(original.get("loops")) && seq.json().get("transitional_item").equals(original.get("transitional_item")),"Sequence weights/loops/transitional item lost");
        check(seq.json().getAsJsonArray("results").get(0).getAsJsonObject().get("chance").getAsDouble()>1,"Sequence weight clamped to probability");checks++;
        var catalog=RecipeSource.catalog(server);
        for(String type:List.of("mixing","compacting","mechanical_crafting","sequenced_assembly","cutting","filling","emptying"))check(catalog.stream().anyMatch(s->s.serializer().equals("create:"+type)),"Catalog excluded "+type);checks++;
        var allowed=new net.minecraft.server.level.ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CreateCheck"),net.minecraft.server.level.ClientInformation.createDefault()){
            @Override public boolean hasPermissions(int level){return true;}
        };
        var nativeRecipe=RecipeSource.read(server,ResourceLocation.parse("create:mixing/brass_ingot"));
        var exported=RecipeDocumentChecks.send(new com.pureneko.vjrm.network.RecipeNetwork.ExportRequest("vjrm:create_network_"+System.nanoTime(),nativeRecipe.json().toString(),false),allowed);
        check(exported.success(),"Server network still rejects recipe-book special: "+exported.detail());checks++;
        System.out.println("PASS "+checks+" real Create recipe/codec checks");return checks;
    }
}
