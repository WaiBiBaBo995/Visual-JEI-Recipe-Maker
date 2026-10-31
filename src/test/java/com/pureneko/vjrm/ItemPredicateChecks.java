package com.pureneko.vjrm;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

/** Machine outputs may use Ingredient's item/tag format instead of the 1.20 item/count/NBT stack format. */
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

    static int run(RegistryAccess registries)throws Exception{
        int checks=0;
        var doc=document(ARC_FURNACE);
        var output=doc.fields().stream().filter(f->f.label().equals("results/0/basePredicate")).findFirst().orElseThrow();
        check(output.output()&&doc.quantity(output)==2,"Sized output predicate lost its role or enclosing count");checks++;
        var unchanged=doc.json();
        doc.addManualField(new RecipeDocument.Field(output.path(),false,true));
        doc.tag(output,"minecraft:logs",3);
        var predicate=doc.get(output);
        check(predicate.equals(JsonParser.parseString("{\"tag\":\"minecraft:logs\"}")),"Tag output was rewritten as a stack");
        check(Ingredient.fromJson(predicate).test(new ItemStack(Items.OAK_LOG)),"Tag output does not decode as an Ingredient");
        check(doc.quantity(output)==3&&doc.matches(output,new ItemStack(Items.OAK_LOG),registries),"Tag output cannot bind its representative item");
        check(doc.values(output,registries).stream().anyMatch(v->v instanceof ItemStack s&&s.is(Items.OAK_LOG)&&s.getCount()==3),"Tag output preview lost its candidates or count");checks++;
        doc.accept(output,new ItemStack(Items.IRON_INGOT),doc.quantity(output),registries);
        check(doc.get(output).equals(JsonParser.parseString("{\"item\":\"minecraft:iron_ingot\"}")),"Tag-to-item output used id/count instead of item");
        check(Ingredient.fromJson(doc.get(output)).test(new ItemStack(Items.IRON_INGOT))&&doc.quantity(output)==3,"Item output failed codec validation or lost enclosing count");checks++;
        doc.tag(output,"minecraft:logs",5);
        check(!doc.get(output).getAsJsonObject().has("item")&&doc.quantity(output)==5,"Item-to-tag output retained item or lost count");
        for(String key:List.of("neoforge:conditions","input","additives","energy","time"))check(doc.json().get(key).equals(unchanged.get(key)),"Predicate edit changed unrelated field: "+key);checks++;
        var before=doc.json();
        try{doc.tag(output,"bad tag",2);throw new AssertionError("Invalid tag accepted");}
        catch(IllegalArgumentException expected){check(doc.json().equals(before),"Invalid tag changed recipe");checks++;}

        // The same codecs occur in single outputs, alternatives, and nested sized ingredients.
        for(String shape:List.of("{\"tag\":\"minecraft:logs\"}","[{\"item\":\"minecraft:oak_log\"},{\"tag\":\"minecraft:logs\"}]","{\"ingredient\":{\"tag\":\"minecraft:logs\"},\"count\":2}")){
            var other=document("{\"type\":\"test:machine\",\"output\":"+shape+"}");
            var field=other.fields().get(0);
            check(!other.values(field,registries).isEmpty(),"Output predicate has no preview: "+shape);
            other.accept(field,new ItemStack(Items.DIAMOND),4,registries);
            var leaf=other.get(field);
            if(leaf.isJsonObject()&&leaf.getAsJsonObject().has("ingredient"))leaf=leaf.getAsJsonObject().get("ingredient");
            check(Ingredient.fromJson(leaf).test(new ItemStack(Items.DIAMOND)),"Output replacement does not decode as an ingredient: "+shape);
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

        var vanilla=RecipeDocument.template("crafting_shapeless");
        var stack=vanilla.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();
        before=vanilla.json();
        try{vanilla.tag(stack,"minecraft:logs",2);throw new AssertionError("ItemStack output accepted a tag");}
        catch(IllegalArgumentException expected){check(vanilla.json().equals(before),"Rejected stack tag mutated recipe");checks++;}
        vanilla.accept(stack,new ItemStack(Items.DIAMOND),3,registries);
        check(vanilla.decode(registries).getResultItem(registries).is(Items.DIAMOND)&&vanilla.quantity(stack)==3,"Ordinary stack output changed codecs");checks++;

        // In 1.20 an item/count object is an ordinary ItemStack, even for mod outputs.
        for(String result:List.of("{\"item\":\"minecraft:stone\",\"count\":2}","{\"item\":\"minecraft:stone\"}","{\"item\":\"minecraft:stone\",\"count\":2,\"nbt\":\"{CustomModelData:3}\"}")){
            var stacks=document("{\"type\":\"test:machine\",\"output\":"+result+"}");
            var field=stacks.fields().get(0);var original=stacks.json();
            try{stacks.tag(field,"minecraft:logs",2);throw new AssertionError("Ambiguous or concrete stack accepted tag");}
            catch(IllegalArgumentException expected){check(stacks.json().equals(original),"Rejected stack edit changed JSON");}
            var named=new ItemStack(Items.EMERALD);named.setTag(net.minecraft.nbt.TagParser.parseTag("{CustomModelData:37}"));
            stacks.accept(field,named,4,registries);
            check(stacks.get(field).getAsJsonObject().get("count").getAsInt()==4&&stacks.get(field).getAsJsonObject().has("nbt"),"Stack count/NBT was lost");
            check(stacks.matches(field,named,registries),"Stack NBT matching changed");checks++;
        }
        // A recognized bare tag stays a predicate through same-type editor refreshes.
        var remembered=document("{\"type\":\"test:machine\",\"output\":{\"tag\":\"minecraft:logs\"}}");
        var rememberedField=remembered.fields().get(0);
        remembered.accept(rememberedField,new ItemStack(Items.DIAMOND),1,registries);
        remembered.replace(remembered.json());
        remembered.tag(rememberedField,"minecraft:logs",1);
        check(remembered.matches(rememberedField,new ItemStack(Items.OAK_LOG),registries),"Refreshed predicate forgot its format");checks++;
        // Export/reload a contextual item predicate; its enclosing count must stay editable.
        doc.accept(output,new ItemStack(Items.GOLD_INGOT),6,registries);
        var file=com.pureneko.vjrm.export.DatapackExporter.export(java.nio.file.Path.of("../../build/verification/item-predicate"),"vjrm","predicate_"+System.nanoTime(),doc.json(),false);
        var reloaded=document(java.nio.file.Files.readString(file));
        var reloadedField=reloaded.fields().stream().filter(f->f.label().equals("results/0/basePredicate")).findFirst().orElseThrow();
        check(reloaded.quantity(reloadedField)==6,"Reloaded sized item lost its quantity");
        reloaded.tag(reloadedField,"minecraft:logs",7);
        check(reloaded.quantity(reloadedField)==7&&reloaded.matches(reloadedField,new ItemStack(Items.OAK_LOG),registries),"Reloaded item predicate became an ItemStack");checks++;
        System.out.println("PASS item/tag output predicates, nested counts, preview/matching and ItemStack output restrictions ("+checks+")");
        return checks;
    }
}
