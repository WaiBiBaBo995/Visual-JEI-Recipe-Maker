package com.pureneko.vjrm;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

/** Machine outputs may use Ingredient's item/tag codec instead of ItemStack's id codec. */
public final class ItemPredicateChecks {
    private static final String ARC_FURNACE="""
        {
          "neoforge:conditions": [{"type":"neoforge:not","value":{"type":"neoforge:tag_empty","tag":"c:ingots/brass"}}],
          "type":"immersiveengineering:arc_furnace",
          "additives":[{"tag":"c:ingots/zinc"}],"input":{"tag":"c:ingots/copper"},
          "results":[{"basePredicate":{"tag":"c:ingots/brass"},"count":2}],
          "energy":51200,"time":100
        }
        """;

    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static RecipeDocument document(String json){return new RecipeDocument(JsonParser.parseString(json).getAsJsonObject());}

    static int run(HolderLookup.Provider registries)throws Exception{
        int checks=0;
        var ops=registries.createSerializationContext(JsonOps.INSTANCE);
        var doc=document(ARC_FURNACE);
        var output=doc.fields().stream().filter(f->f.label().equals("results/0/basePredicate")).findFirst().orElseThrow();
        check(output.output()&&doc.quantity(output)==2,"Sized output predicate lost its role or enclosing count");checks++;
        var unchanged=doc.json();
        doc.addManualField(new RecipeDocument.Field(output.path(),false,true));
        doc.tag(output,"minecraft:logs",3);
        var predicate=doc.get(output);
        check(predicate.equals(JsonParser.parseString("{\"tag\":\"minecraft:logs\"}")),"Tag output was rewritten as a stack");
        check(Ingredient.CODEC.parse(ops,predicate).getOrThrow().test(new ItemStack(Items.OAK_LOG)),"Tag output does not decode as an Ingredient");
        check(doc.quantity(output)==3&&doc.matches(output,new ItemStack(Items.OAK_LOG),registries),"Tag output cannot bind its representative item");
        check(doc.values(output,registries).stream().anyMatch(v->v instanceof ItemStack s&&s.is(Items.OAK_LOG)&&s.getCount()==3),"Tag output preview lost its candidates or count");checks++;
        doc.accept(output,new ItemStack(Items.IRON_INGOT),doc.quantity(output),registries);
        check(doc.get(output).equals(JsonParser.parseString("{\"item\":\"minecraft:iron_ingot\"}")),"Tag-to-item output used id/count instead of item");
        check(Ingredient.CODEC.parse(ops,doc.get(output)).getOrThrow().test(new ItemStack(Items.IRON_INGOT))&&doc.quantity(output)==3,"Item output failed codec validation or lost enclosing count");checks++;
        doc.tag(output,"minecraft:logs",5);
        check(!doc.get(output).getAsJsonObject().has("item")&&doc.quantity(output)==5,"Item-to-tag output retained item or lost count");
        for(String key:List.of("neoforge:conditions","input","additives","energy","time"))check(doc.json().get(key).equals(unchanged.get(key)),"Predicate edit changed unrelated field: "+key);checks++;
        var before=doc.json();
        try{doc.tag(output,"bad tag",2);throw new AssertionError("Invalid tag accepted");}
        catch(IllegalArgumentException expected){check(doc.json().equals(before),"Invalid tag changed recipe");checks++;}

        // The same codecs occur in single outputs, alternatives, and nested sized ingredients.
        for(String shape:List.of("{\"tag\":\"minecraft:logs\"}","{\"item\":\"minecraft:oak_log\"}","[{\"item\":\"minecraft:oak_log\"},{\"tag\":\"minecraft:logs\"}]","{\"ingredient\":{\"tag\":\"minecraft:logs\"},\"count\":2}")){
            var other=document("{\"type\":\"test:machine\",\"output\":"+shape+"}");
            var field=other.fields().getFirst();
            check(!other.values(field,registries).isEmpty(),"Output predicate has no preview: "+shape);
            other.accept(field,new ItemStack(Items.DIAMOND),4,registries);
            var leaf=other.get(field);
            if(leaf.isJsonObject()&&leaf.getAsJsonObject().has("ingredient"))leaf=leaf.getAsJsonObject().get("ingredient");
            check(Ingredient.CODEC.parse(ops,leaf).getOrThrow().test(new ItemStack(Items.DIAMOND)),"Output replacement does not decode as an ingredient: "+shape);
            check(!leaf.getAsJsonObject().has("id")&&!leaf.getAsJsonObject().has("count"),"Stack fields leaked into output predicate");
            other.tag(field,"minecraft:logs",4);
            check(other.matches(field,new ItemStack(Items.OAK_LOG),registries),"Tag replacement did not survive refresh: "+shape);checks++;
        }

        // Binding the leaf of a sized input must preserve and edit its parent count as well.
        var input=document("{\"type\":\"test:machine\",\"input\":{\"ingredient\":{\"item\":\"minecraft:stone\"},\"count\":7}}");
        var inputLeaf=new RecipeDocument.Field(List.of("input","ingredient"),false,false);
        check(input.quantity(inputLeaf)==7,"Manually bound sized input lost count");
        input.accept(inputLeaf,new ItemStack(Items.EMERALD),9,registries);
        input.tag(inputLeaf,"minecraft:logs",11);
        check(input.quantity(inputLeaf)==11&&!input.get(inputLeaf).getAsJsonObject().has("count"),"Leaf input count moved into predicate");checks++;

        var vanilla=RecipeDocument.template("smelting");
        var stack=vanilla.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();
        before=vanilla.json();
        try{vanilla.tag(stack,"minecraft:logs",2);throw new AssertionError("ItemStack output accepted a tag");}
        catch(IllegalArgumentException expected){check(vanilla.json().equals(before),"Rejected stack tag mutated recipe");checks++;}
        vanilla.accept(stack,new ItemStack(Items.DIAMOND),3,registries);
        check(vanilla.decode(registries).getResultItem(registries).is(Items.DIAMOND)&&vanilla.quantity(stack)==3,"Ordinary stack output changed codecs");checks++;

        // When IE is installed, validate the reproduction against its actual recipe serializer.
        if(net.neoforged.fml.ModList.get().isLoaded("immersiveengineering")){
            var ie=document(ARC_FURNACE);ie.decode(registries);
            var field=ie.fields().stream().filter(f->f.label().equals("results/0/basePredicate")).findFirst().orElseThrow();
            ie.tag(field,"c:ingots/iron",3);ie.decode(registries);
            ie.accept(field,new ItemStack(Items.GOLD_INGOT),4,registries);ie.decode(registries);
            ie.tag(field,"c:ingots/copper",5);ie.decode(registries);
            var exported=ie.json();
            var path=com.pureneko.vjrm.export.DatapackExporter.export(java.nio.file.Path.of("build/verification/ie-predicate"),"vjrm","predicate_"+System.nanoTime(),exported,false);
            var reloaded=document(java.nio.file.Files.readString(path));reloaded.decode(registries);
            check(reloaded.json().getAsJsonArray("results").get(0).getAsJsonObject().get("count").getAsInt()==5,"IE export lost outer result count");
            System.out.println("PASS actual Immersive Engineering arc furnace serializer: tag -> item -> tag and exported JSON");checks++;
        }
        System.out.println("PASS item/tag output predicates, nested counts, preview/matching and ItemStack output restrictions ("+checks+")");
        return checks;
    }
}
