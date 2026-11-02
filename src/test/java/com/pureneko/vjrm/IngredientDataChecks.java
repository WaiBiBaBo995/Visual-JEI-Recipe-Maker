package com.pureneko.vjrm;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.world.item.*;
import java.util.*;

/** Runs against each loader's real ingredient serializers and recipe decoder. */
public final class IngredientDataChecks {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static ItemStack sample(net.minecraft.core.HolderLookup.Provider registries,int variant){
        var json=JsonParser.parseString("""
            {"id":"minecraft:diamond_sword","components":{"minecraft:custom_data":{"vjrm_probe":"keep_me"},"minecraft:enchantments":{"minecraft:sharpness":5}}}
            """).getAsJsonObject();
        if(variant==0)json.getAsJsonObject("components").remove("minecraft:custom_data");
        if(variant==1)json.getAsJsonObject("components").remove("minecraft:enchantments");
        return net.minecraft.world.item.ItemStackTemplate.CODEC.parse(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),json).getOrThrow().create();
    }
    public static RecipeDocument.Field input(RecipeDocument doc){return doc.fields().stream().filter(f->!f.output()).findFirst().orElseThrow();}
    public static void preserved(RecipeDocument doc,RecipeDocument.Field field,ItemStack stack,net.minecraft.core.HolderLookup.Provider registries){
        check(ItemStack.isSameItemSameComponents((ItemStack)doc.values(field,registries).get(0),stack),"Preview lost data: "+field.label());
        check(doc.matches(field,stack,registries),"Matching input rejected");
        check(!doc.matches(field,new ItemStack(stack.getItem()),registries),"Ordinary input accepted");
    }
    public static int run(net.minecraft.core.HolderLookup.Provider registries)throws Exception{
        int checks=0;
        for(int variant=0;variant<3;variant++)for(String type:RecipeDocument.TYPES){
            var stack=sample(registries,variant);var before=stack.copy();var doc=RecipeDocument.template(type);var field=input(doc);
            doc.accept(field,stack,1,registries);doc.decode(registries);preserved(doc,field,stack,registries);
            check(doc.fields().stream().noneMatch(f->f.path().size()>field.path().size()&&f.path().subList(0,field.path().size()).equals(field.path())),"Wrapper split into fields");
            var wrong=stack.copy();wrong.remove(variant==0?net.minecraft.core.component.DataComponents.ENCHANTMENTS:net.minecraft.core.component.DataComponents.CUSTOM_DATA);check(!doc.matches(field,wrong,registries),"Different data accepted");
            if(variant!=0){var changed=stack.copy();var data=changed.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();data.putInt("extra",1);changed.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));check(!doc.matches(field,changed,registries),"custom_data used subset matching");}
            var extra=stack.copy();extra.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("extra"));
            check(doc.matches(field,extra,registries)==true,"Extra-data matching policy changed");
            var predicate=doc.get(field).deepCopy();doc.setQuantity(field,1);check(predicate.equals(doc.get(field)),"Quantity rebuilt predicate");
            var json=doc.json().toString();var reopen=new RecipeDocument(JsonParser.parseString(json).getAsJsonObject());reopen.decode(registries);preserved(reopen,input(reopen),stack,registries);
            check(ItemStack.isSameItemSameComponents(stack,before),"Source stack mutated");checks++;
        }
        var stack=sample(registries,2);var doc=RecipeDocument.template("crafting_shapeless");var field=input(doc);doc.accept(field,stack,1,registries);
        var machineJson=new JsonObject();machineJson.addProperty("type","test:machine");machineJson.add("result",doc.get(field).deepCopy());
        var machine=new RecipeDocument(machineJson);var machineOutput=machine.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();preserved(machine,machineOutput,stack,registries);
        machine.tag(machineOutput,"minecraft:logs",1);check(machine.matches(machineOutput,new ItemStack(Items.OAK_LOG),registries),"Component output predicate retained item restriction");checks++;
        doc.setRepeatedQuantity(List.of(field),3);for(var f:doc.fields().stream().filter(f->!f.output()).toList())preserved(doc,f,stack,registries);doc.decode(registries);checks++;
        for(String type:List.of("crafting_shaped","crafting_shapeless")){
            var added=RecipeDocument.template(type);if(type.equals("crafting_shaped"))added.remove(input(added));
            added.addCraftingInput(0,stack,registries);added.decode(registries);
            var rich=added.fields().stream().filter(f->!f.output()&&added.matches(f,stack,registries)).findFirst().orElseThrow();preserved(added,rich,stack,registries);
            added.remove(rich);added.decode(registries);checks++;
        }
        doc=RecipeDocument.template("crafting_shapeless");field=input(doc);doc.accept(field,stack,1,registries);doc.tag(field,"minecraft:logs",1);doc.decode(registries);
        check(doc.matches(field,new ItemStack(Items.OAK_LOG),registries),"Tag retained data predicate");
        doc.accept(field,stack,1,registries);doc.accept(field,new ItemStack(stack.getItem()),1,registries);doc.decode(registries);
        check(doc.matches(field,new ItemStack(stack.getItem()),registries),"Plain replacement retained data predicate");checks++;
        var removed=new ItemStack(Items.DIAMOND_SWORD);removed.remove(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        doc.accept(field,removed,1,registries);preserved(doc,field,removed,registries);doc.decode(registries);checks++;
        System.out.println("PASS ingredient data regression: "+checks+" cases");return checks;
    }
    public static void export(net.minecraft.server.MinecraftServer server)throws Exception{
        var registries=server.registryAccess();var source=sample(registries,2);var doc=RecipeDocument.template("crafting_shapeless");doc.accept(input(doc),source,1,registries);
        var pack=com.pureneko.vjrm.export.DatapackExporter.serverDatapackRoot(server,"vjrm_generated");
        var id=net.minecraft.resources.Identifier.parse("vjrm:ingredient_data_export");var file=pack.resolve("data/vjrm/recipe/ingredient_data_export.json");
        if(java.nio.file.Files.exists(file)){
            check(server.getRecipeManager().getRecipes().stream().anyMatch(recipe->recipe.id().identifier().equals(id)),"Exported data recipe not loaded by RecipeManager");
            var loaded=com.pureneko.vjrm.recipe.RecipeSource.read(server,id);preserved(loaded,input(loaded),source,registries);System.out.println("PASS ingredient data export loaded by RecipeManager");
        }
        com.pureneko.vjrm.export.DatapackExporter.writePackMeta(pack);
        file=com.pureneko.vjrm.export.DatapackExporter.export(pack,"vjrm","ingredient_data_export",doc.json(),true);
        var exported=new RecipeDocument(JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject());exported.decode(registries);preserved(exported,input(exported),source,registries);
        System.out.println("PASS ingredient data file export and native decoding");
    }
}
