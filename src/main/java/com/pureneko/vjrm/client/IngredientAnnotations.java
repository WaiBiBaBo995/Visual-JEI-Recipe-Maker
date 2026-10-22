package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import com.pureneko.vjrm.platform.FluidStack;
import java.util.*;

/** Source offsets, never text substitutions. Only confirmed ingredient fields are decorated. */
final class IngredientAnnotations {
    record Icon(int end,Object ingredient) { }
    private record StringValue(List<String> path,int end,String value) { }
    private static final Set<String> INGREDIENT_KEYS=Set.of("ingredient","ingredients","item","items","fluid","fluids","id","tag","value","values","content","children");

    static List<Icon> read(String source,JsonObject json,int offset,List<RecipeDocument.Field> manual){
        var mc=Minecraft.getInstance();if(mc.level==null)return List.of();
        try{
            var document=new RecipeDocument(json);for(var field:manual)if(JsonFieldPaths.exists(json,field.path()))document.addManualField(field);
            var strings=new ArrayList<StringValue>();
            var tokens=JsonSyntax.tokens(source.substring(offset));
            walk(source.substring(offset),tokens,new int[]{0},json,List.of(),strings,offset);
            var icons=new TreeMap<Integer,Icon>();
            for(var field:document.fields()){
                if(field.raw()||!field.chemical().isEmpty())continue;
                List<?> values;
                try{values=document.values(field,mc.level.registryAccess());}catch(RuntimeException ex){continue;}
                for(var string:strings){
                    if(!inside(string.path,field.path()))continue;
                    String id=string.value;
                    // Tags represent a set, rather than one exact item/fluid string.
                    if(id.startsWith("#")||!string.path.isEmpty()&&string.path.get(string.path.size()-1).equals("tag"))continue;
                    for(Object value:values){
                        String actual=value instanceof ItemStack item&&!item.isEmpty()?BuiltInRegistries.ITEM.getKey(item.getItem()).toString():
                            value instanceof FluidStack fluid&&!fluid.isEmpty()?BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString():"";
                        if(!actual.isEmpty()&&(actual.equals(id)||actual.equals("minecraft:"+id))){icons.put(string.end,new Icon(string.end,value));break;}
                    }
                }
            }
            return List.copyOf(icons.values());
        }catch(RuntimeException ex){return List.of();}
    }
    private static boolean inside(List<String> path,List<String> field){
        if(path.size()<field.size()||!path.subList(0,field.size()).equals(field))return false;
        for(String key:path.subList(field.size(),path.size()))if(!key.matches("[0-9]+")&&!INGREDIENT_KEYS.contains(key))return false;
        return true;
    }
    private static void walk(String source,List<JsonSyntax.Token> tokens,int[] at,JsonElement value,List<String> path,List<StringValue> out,int offset){
        if(value.isJsonObject()){
            expect(source,tokens,at,"{");
            for(var entry:value.getAsJsonObject().entrySet()){
                var key=tokens.get(at[0]++);
                if(!JsonParser.parseString(source.substring(key.start(),key.end())).getAsString().equals(entry.getKey()))throw new IllegalArgumentException("Ambiguous source key");
                expect(source,tokens,at,":");
                var next=new ArrayList<>(path);next.add(entry.getKey());walk(source,tokens,at,entry.getValue(),next,out,offset);
                if(at[0]<tokens.size()&&source.charAt(tokens.get(at[0]).start())==',')at[0]++;
            }
            expect(source,tokens,at,"}");
        }else if(value.isJsonArray()){
            expect(source,tokens,at,"[");
            for(int i=0;i<value.getAsJsonArray().size();i++){
                var next=new ArrayList<>(path);next.add(Integer.toString(i));walk(source,tokens,at,value.getAsJsonArray().get(i),next,out,offset);
                if(at[0]<tokens.size()&&source.charAt(tokens.get(at[0]).start())==',')at[0]++;
            }
            expect(source,tokens,at,"]");
        }else{
            var token=tokens.get(at[0]++);
            if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()){
                if(!JsonParser.parseString(source.substring(token.start(),token.end())).getAsString().equals(value.getAsString()))throw new IllegalArgumentException("Ambiguous source value");
                out.add(new StringValue(List.copyOf(path),offset+token.end(),value.getAsString()));
            }
        }
    }
    private static void expect(String source,List<JsonSyntax.Token> tokens,int[] at,String value){
        var token=tokens.get(at[0]++);if(!source.substring(token.start(),token.end()).equals(value))throw new IllegalArgumentException("Ambiguous source structure");
    }
    private IngredientAnnotations(){ }
}
