package com.pureneko.vjrm;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.pureneko.vjrm.export.DatapackExporter;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import java.nio.file.*;
import java.util.*;
import static com.pureneko.vjrm.PortServerChecks.check;

/** Regression checks against live Fabric ingredient codecs and real recipe matching. */
public final class ComponentIngredientChecks {
    private static final ResourceLocation EXPORTED=ResourceLocation.parse("vjrm:component_input_export");
    public static ItemStack sample(HolderLookup.Provider registries,int variant){
        var json=JsonParser.parseString("""
            {"id":"minecraft:diamond_sword","count":1,"components":{
              "minecraft:enchantments":{"levels":{"minecraft:sharpness":5}},
              "minecraft:custom_data":{"vjrm_probe":"keep_me","value":123}
            }}
            """).getAsJsonObject();
        if(variant==0)json.getAsJsonObject("components").remove("minecraft:custom_data");
        if(variant==1)json.getAsJsonObject("components").remove("minecraft:enchantments");
        return ItemStack.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
    }
    public static RecipeDocument.Field input(RecipeDocument doc){return doc.fields().stream().filter(f->!f.output()).findFirst().orElseThrow();}
    public static void preserved(RecipeDocument doc,RecipeDocument.Field field,ItemStack source,HolderLookup.Provider registries){
        check(ItemStack.isSameItemSameComponents((ItemStack)doc.values(field,registries).getFirst(),source),"Preview lost item components: "+field.label());
        check(doc.matches(field,source,registries),"Original stack no longer matches");
    }
    private static void realMatching(Recipe<?> recipe,ItemStack source,MinecraftServer server){
        var shapeless=(ShapelessRecipe)recipe;
        check(shapeless.matches(CraftingInput.of(1,1,List.of(source)),server.overworld()),"Real recipe rejected matching stack");
        check(!shapeless.matches(CraftingInput.of(1,1,List.of(new ItemStack(source.getItem()))),server.overworld()),"Real recipe accepted ordinary item");
    }
    static int run(MinecraftServer server)throws Exception{
        var registries=server.registryAccess();int checks=0;
        for(int variant=0;variant<3;variant++)for(String type:RecipeDocument.TYPES){
            var source=sample(registries,variant);var before=source.copy();var doc=RecipeDocument.template(type);
            var field=input(doc);var path=field.path();doc.accept(field,source,1,registries);
            check(doc.fields().stream().anyMatch(f->f.path().equals(path)),"Component wrapper split into inner fields: "+type);
            check(doc.fields().stream().noneMatch(f->f.path().size()>path.size()&&f.path().subList(0,path.size()).equals(path)),"Nested component data became slots");
            preserved(doc,field,source,registries);
            check(!doc.matches(field,new ItemStack(source.getItem()),registries),"Plain item matched component ingredient");
            var extra=source.copy();extra.set(DataComponents.CUSTOM_NAME,Component.literal("extra component"));
            check(doc.matches(field,extra,registries),"Unspecified extra component was rejected");
            if(variant!=0){var changed=source.copy();var nbt=source.get(DataComponents.CUSTOM_DATA).copyTag();nbt.putInt("extra",1);changed.set(DataComponents.CUSTOM_DATA,CustomData.of(nbt));check(!doc.matches(field,changed,registries),"Changed custom_data was accepted");}
            if(variant!=1){var changed=source.copy();changed.remove(DataComponents.ENCHANTMENTS);check(!doc.matches(field,changed,registries),"Missing enchantments were accepted");}
            var output=doc.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();doc.accept(output,source,1,registries);
            check(ItemStack.isSameItemSameComponents(doc.decode(registries).getResultItem(registries),source),"Output lost components");
            var copied=RecipeDocument.copy(doc.decode(registries),registries);preserved(copied,input(copied),source,registries);
            check(ItemStack.isSameItemSameComponents(source,before),"Source stack mutated");checks++;
        }
        var source=sample(registries,2);var doc=RecipeDocument.template("crafting_shapeless");var field=input(doc);doc.accept(field,source,1,registries);
        realMatching(doc.decode(registries),source,server);checks++;
        var original=doc.get(field);doc.setQuantity(field,1);check(doc.get(field).equals(original),"Quantity rebuilt component predicate");
        doc.acceptRepeated(List.of(field),source,3,registries);var group=doc.fields().stream().filter(f->!f.output()).toList();
        doc.setRepeatedQuantity(group,5);check(doc.json().getAsJsonArray("ingredients").size()==5,"Repeated count did not change");
        for(var member:doc.fields().stream().filter(f->!f.output()).toList())preserved(doc,member,source,registries);checks++;
        for(String type:List.of("crafting_shaped","crafting_shapeless")){
            var added=RecipeDocument.template(type);if(type.equals("crafting_shaped"))added.remove(input(added));
            added.addCraftingInput(0,source,registries);added.decode(registries);
            var rich=added.fields().stream().filter(f->!f.output()&&added.matches(f,source,registries)&&!added.matches(f,new ItemStack(source.getItem()),registries)).findFirst().orElseThrow();
            preserved(added,rich,source,registries);added.remove(rich);added.decode(registries);checks++;
        }
        var nested=new RecipeDocument(JsonParser.parseString("{\"type\":\"test:machine\",\"input\":{\"ingredient\":{\"item\":\"minecraft:stone\"},\"count\":7},\"energy\":42}").getAsJsonObject());
        var sized=input(nested);nested.accept(sized,source,7,registries);var predicate=nested.get(sized).getAsJsonObject().get("ingredient").deepCopy();
        nested.setQuantity(sized,9);check(nested.quantity(sized)==9&&nested.get(sized).getAsJsonObject().get("ingredient").equals(predicate),"Sized count changed predicate");
        var leaf=new RecipeDocument.Field(List.of("input","ingredient"),false,false);nested.setQuantity(leaf,11);
        check(nested.quantity(leaf)==11&&nested.get(leaf).equals(predicate)&&nested.json().get("energy").getAsInt()==42,"Leaf count lost conditions or enclosing fields");checks++;
        var alternatives=RecipeDocument.template("smelting").json();var array=new JsonArray();array.add(original);array.add(JsonParser.parseString("{\"item\":\"minecraft:stone\"}"));
        var any=new JsonObject();any.addProperty("fabric:type","fabric:any");any.add("ingredients",array);alternatives.add("ingredient",any);
        var alt=new RecipeDocument(alternatives);check(alt.fields().size()==2,"Composite component predicate split into slots");alt.decode(registries);
        check(alt.matches(input(alt),source,registries)&&alt.matches(input(alt),new ItemStack(Items.STONE),registries)&&!alt.matches(input(alt),new ItemStack(Items.DIAMOND_SWORD),registries),"Composite alternatives lost their conditions");checks++;
        doc=RecipeDocument.template("crafting_shapeless");field=input(doc);doc.accept(field,source,1,registries);
        doc.tag(field,"minecraft:logs",1);doc.decode(registries);check(doc.matches(field,new ItemStack(Items.OAK_LOG),registries),"Tag retained component restriction");
        check(!doc.get(field).getAsJsonObject().has("fabric:type")&&!doc.get(field).getAsJsonObject().has("components"),"Tag left obsolete wrapper data");
        doc.accept(field,source,1,registries);doc.accept(field,new ItemStack(Items.DIAMOND_SWORD),1,registries);doc.decode(registries);
        check(doc.matches(field,new ItemStack(Items.DIAMOND_SWORD),registries)&&!doc.get(field).getAsJsonObject().has("fabric:type"),"Plain replacement retained conditions");checks++;
        var removed=new ItemStack(Items.DIAMOND_SWORD);removed.remove(DataComponents.ATTRIBUTE_MODIFIERS);
        doc.accept(field,removed,1,registries);preserved(doc,field,removed,registries);check(!doc.matches(field,new ItemStack(Items.DIAMOND_SWORD),registries),"Explicitly removed component not enforced");checks++;
        var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(DataComponents.STORED_ENCHANTMENTS,source.get(DataComponents.ENCHANTMENTS));
        doc.accept(field,book,1,registries);preserved(doc,field,book,registries);check(!doc.matches(field,new ItemStack(Items.ENCHANTED_BOOK),registries),"Stored enchantment requirement lost");checks++;
        doc.accept(field,source,1,registries);var pack=DatapackExporter.serverDatapackRoot(server,"vjrm_generated");DatapackExporter.writePackMeta(pack);
        boolean reloaded=server.getRecipeManager().byKey(EXPORTED).isPresent();
        if(reloaded){realMatching(server.getRecipeManager().byKey(EXPORTED).orElseThrow().value(),source,server);checks++;}
        var file=DatapackExporter.export(pack,"vjrm",EXPORTED.getPath(),doc.json(),true);
        var exported=new RecipeDocument(JsonParser.parseString(Files.readString(file)).getAsJsonObject());realMatching(exported.decode(registries),source,server);checks++;
        Path output=Path.of("../../build/verification/components");Files.createDirectories(output);
        Files.writeString(output.resolve("server-result.txt"),"PASS: "+checks+" component cases; matching, extra components, exact custom_data, component removal, stored enchantments, add/remove, wrappers, repeated quantities, output preservation, codec roundtrip and export; exported recipe loaded by RecipeManager="+reloaded+"\n");
        System.out.println("PASS component ingredients: "+checks+"; exported recipe reloaded="+reloaded);return checks;
    }
}
