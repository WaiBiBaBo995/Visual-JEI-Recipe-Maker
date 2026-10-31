package com.pureneko.vjrm;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

/** Output predicates use the 26.1.2 ingredient codec, while stack results keep id/count. */
public final class ItemPredicateChecks {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static RecipeDocument document(String json){return new RecipeDocument(JsonParser.parseString(json).getAsJsonObject());}
    public static void run(HolderLookup.Provider registries){
        var ops=registries.createSerializationContext(JsonOps.INSTANCE);
        var doc=document("{\"type\":\"test:machine\",\"input\":\"minecraft:stone\",\"results\":[{\"basePredicate\":\"#minecraft:logs\",\"count\":2}],\"time\":100}");
        var output=doc.fields().stream().filter(f->f.label().equals("results/0/basePredicate")).findFirst().orElseThrow();
        check(output.output()&&doc.quantity(output)==2,"Output predicate lost its role or outer count");
        var unchanged=doc.json();
        doc.addManualField(new RecipeDocument.Field(output.path(),false,true));
        doc.tag(output,"minecraft:logs",3);
        check(doc.get(output).getAsString().equals("#minecraft:logs")&&doc.quantity(output)==3,"Tag output lost native string format or outer count");
        check(doc.matches(output,new ItemStack(Items.OAK_LOG),registries),"Output tag cannot bind its representative item");
        check(doc.values(output,registries).stream().anyMatch(v->v instanceof ItemStack s&&s.is(Items.OAK_LOG)&&s.getCount()==3),"Tag output preview lost candidates or count");
        doc.accept(output,new ItemStack(Items.IRON_INGOT),doc.quantity(output),registries);
        check(doc.get(output).getAsString().equals("minecraft:iron_ingot")&&doc.quantity(output)==3,"Tag-to-item wrote stack JSON or lost outer count");
        check(Ingredient.CODEC.parse(ops,doc.get(output)).getOrThrow().test(new ItemStack(Items.IRON_INGOT)),"Item output does not decode as an Ingredient");
        doc.tag(output,"minecraft:logs",5);
        check(doc.quantity(output)==5&&Ingredient.CODEC.parse(ops,doc.get(output)).getOrThrow().test(new ItemStack(Items.OAK_LOG)),"Item-to-tag output failed codec validation");
        check(doc.json().get("input").equals(unchanged.get("input"))&&doc.json().get("time").equals(unchanged.get("time")),"Output edit changed unrelated metadata");
        var before=doc.json();
        try{doc.tag(output,"bad tag",9);throw new AssertionError("Invalid tag accepted");}
        catch(IllegalArgumentException expected){check(doc.json().equals(before),"Invalid tag mutated recipe");}

        for(String shape:List.of("\"#minecraft:logs\"","\"minecraft:oak_log\"","[\"minecraft:oak_log\",\"minecraft:birch_log\"]","{\"ingredient\":\"#minecraft:logs\",\"count\":2}")){
            var other=document("{\"type\":\"test:machine\",\"output\":"+shape+"}");
            var field=other.fields().getFirst();
            check(other.fields().size()==1&&field.label().equals("output")&&field.output(),"Output alternatives split into independent fields or lost role: "+shape);
            check(!other.values(field,registries).isEmpty(),"Output predicate has no preview: "+shape);
            other.accept(field,new ItemStack(Items.DIAMOND),4,registries);
            var leaf=other.get(field);
            if(leaf.isJsonObject())leaf=leaf.getAsJsonObject().get("ingredient");
            check(leaf.isJsonPrimitive()&&Ingredient.CODEC.parse(ops,leaf).getOrThrow().test(new ItemStack(Items.DIAMOND)),"Replacement failed native ingredient codec: "+shape);
            other.tag(field,"minecraft:logs",4);
            check(other.matches(field,new ItemStack(Items.OAK_LOG),registries),"Tag replacement did not survive refresh: "+shape);
        }
        var sized=document("{\"type\":\"test:machine\",\"input\":{\"ingredient\":\"minecraft:stone\",\"count\":7}}");
        var leafField=new RecipeDocument.Field(List.of("input","ingredient"),false,false);
        check(sized.quantity(leafField)==7,"Manual predicate binding lost outer count");
        sized.accept(leafField,new ItemStack(Items.EMERALD),9,registries);
        sized.tag(leafField,"minecraft:logs",11);
        check(sized.quantity(leafField)==11&&sized.get(leafField).isJsonPrimitive(),"Leaf predicate count was not updated outside the ingredient");

        // Changing a slot's role must not change the actual codec used by its JSON field.
        var vanilla=RecipeDocument.template("smelting");
        var input=vanilla.fields().stream().filter(f->!f.output()).findFirst().orElseThrow();
        var roleOverride=new RecipeDocument.Field(input.path(),false,true);
        vanilla.addManualField(roleOverride);
        vanilla.tag(roleOverride,"minecraft:logs",1);vanilla.decode(registries);
        vanilla.accept(roleOverride,new ItemStack(Items.EMERALD),1,registries);vanilla.decode(registries);
        var stack=vanilla.fields().stream().filter(f->f.label().equals("result")).findFirst().orElseThrow();
        before=vanilla.json();
        try{vanilla.tag(stack,"minecraft:logs",2);throw new AssertionError("ItemStack output accepted a tag");}
        catch(IllegalArgumentException expected){check(vanilla.json().equals(before),"Rejected stack tag mutated recipe");}
        vanilla.accept(stack,new ItemStack(Items.DIAMOND),3,registries);vanilla.decode(registries);
        check(vanilla.get(stack).getAsJsonObject().get("id").getAsString().equals("minecraft:diamond")&&vanilla.quantity(stack)==3,"Ordinary stack output changed its codec");
        System.out.println("PASS 26.1.2 item/tag output predicates: compact strings, alternatives, nested counts, preview/binding, role overrides and ItemStack restrictions");
    }
}

