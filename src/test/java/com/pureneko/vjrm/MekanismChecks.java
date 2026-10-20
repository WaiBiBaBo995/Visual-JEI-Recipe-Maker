package com.pureneko.vjrm;

import com.pureneko.vjrm.recipe.*;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("vjrm") @PrefixGameTestTemplate(false)
public final class MekanismChecks {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    @GameTest(template="empty",timeoutTicks=400)
    public static void chemicals(GameTestHelper helper)throws Exception{
        if(!Boolean.getBoolean("vjrm.verifyMekanism")){helper.succeed();return;}
        var server=helper.getLevel().getServer();var registries=server.registryAccess();
        Map<String,Integer> found=new TreeMap<>(),edited=new TreeMap<>();int fields=0;
        for(var recipe:server.getRecipeManager().getRecipes()){
            if(!recipe.getId().getNamespace().equals("mekanism"))continue;
            RecipeDocument doc;try{doc=RecipeSource.read(server,recipe.getId());}catch(Exception ex){continue;}
            for(var field:doc.fields())if(!field.chemical().isEmpty()){
                fields++;var values=doc.values(field,registries);if(values.isEmpty())continue;
                found.merge(field.chemical(),1,Integer::sum);check(doc.matches(field,values.get(0),registries),"Chemical matching failed: "+recipe.getId()+"/"+field.label());
                if(edited.getOrDefault(field.chemical(),0)>=4)continue;
                var original=doc.json();long amount=doc.chemicalAmount(field)+1;
                doc.accept(field,values.get(0),amount,registries);doc.decode(registries,recipe.getId());
                check(doc.chemicalAmount(field)==amount,"Chemical amount was truncated");
                check(doc.get(field).toString().contains(ChemicalSupport.id(values.get(0))),"Chemical id missing");
                edited.merge(field.chemical(),1,Integer::sum);doc.replace(original);
            }
        }
        for(String kind:List.of("gas","infuse_type","pigment","slurry"))check(edited.getOrDefault(kind,0)>0,"Mekanism chemical family not exercised: "+kind);
        var oxygen=ChemicalSupport.fromId("mekanism:oxygen",1,"gas",com.mojang.serialization.JsonOps.INSTANCE);
        var alias=new RecipeDocument(JsonParser.parseString("{\"type\":\"mekanism:compressing\",\"input_chemical\":{\"gas\":\"mekanism:oxygen\",\"amount\":2,\"extension\":37}}").getAsJsonObject());
        var field=alias.fields().get(0);check(field.label().equals("input_chemical")&&field.chemical().equals("gas"),"input_chemical not recognized");
        alias.accept(field,oxygen,9876543210L,registries);check(alias.get(field).getAsJsonObject().get("extension").getAsInt()==37&&alias.chemicalAmount(field)==9876543210L,"Chemical precision/extension lost");
        check(!alias.values(field,registries).isEmpty(),"Large chemical amount not deserialized");
        var ingredient=new RecipeDocument(JsonParser.parseString("{\"type\":\"mekanism:compressing\",\"itemInput\":{\"ingredient\":{\"item\":\"minecraft:iron_ingot\"},\"amount\":4}}").getAsJsonObject());
        check(ingredient.quantity(ingredient.fields().get(0))==4,"Mekanism item wrapper amount lost");
        String result="PASS: Mekanism real serializers: "+fields+" chemical fields, families="+found+", edited="+edited+", input_chemical alias, long amounts, extension preservation and item wrapper counts";
        var file=java.nio.file.Path.of("../../build/verification/server/mekanism-result.txt");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,result);System.out.println(result);helper.succeed();
    }
}
