package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import com.mojang.serialization.*;
import java.lang.reflect.*;
import java.util.*;

/** Optional Mekanism API boundary; no Mekanism classes are required when the mod is absent. */
public final class ChemicalSupport {
    private static final String API="mekanism.api.";
    private static final Class<?> STACK=load(API+"chemical.ChemicalStack");
    private static Class<?> load(String name){try{return Class.forName(name,false,ChemicalSupport.class.getClassLoader());}catch(ClassNotFoundException|LinkageError ex){return null;}}
    public static boolean unified(){return STACK!=null&&!Modifier.isAbstract(STACK.getModifiers());}
    public static Object unwrap(Object value){
        if(value!=null&&value.getClass().getName().equals(API+"chemical.merged.BoxedChemicalStack"))try{return value.getClass().getMethod("getChemicalStack").invoke(value);}catch(ReflectiveOperationException ignored){}
        return value;
    }
    public static boolean isStack(Object value){return STACK!=null&&STACK.isInstance(unwrap(value));}
    public static String stackKind(Object value){
        value=unwrap(value);if(!isStack(value))return "";
        if(unified())return "chemical";
        String name=value.getClass().getName();return name.contains(".infuse.")?"infuse_type":name.contains(".pigment.")?"pigment":name.contains(".slurry.")?"slurry":"gas";
    }
    public static String id(Object value){try{return STACK.getMethod("getTypeRegistryName").invoke(unwrap(value)).toString();}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.chemical",ex);}}
    public static Object resized(Object value,long amount){
        try{value=STACK.getMethod("copy").invoke(unwrap(value));STACK.getMethod("setAmount",long.class).invoke(value,amount);return value;}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("vjrm.error.chemical",ex);}
    }
    public static JsonElement leaf(JsonElement node){return node.isJsonObject()&&node.getAsJsonObject().has("ingredient")?node.getAsJsonObject().get("ingredient"):node;}
    public static String kind(JsonElement node,List<String> path,JsonObject recipe){
        if(path.isEmpty()||path.stream().anyMatch(p->p.toLowerCase(Locale.ROOT).contains("condition")))return "";
        if(node.isJsonArray()){
            String last=path.get(path.size()-1);if(Set.of("ingredients","results","outputs","sequence").contains(last))return "";
            String kind="";for(var child:node.getAsJsonArray()){String found=kind(child,path,recipe);if(found.isEmpty()||!kind.isEmpty()&&!kind.equals(found))return "";kind=found;}return kind;
        }
        node=leaf(node);if(!node.isJsonObject())return "";var object=node.getAsJsonObject();
        for(String key:List.of("gas","infuse_type","pigment","slurry","chemical"))if(object.has(key))return key;
        String label=String.join("/",path).toLowerCase(Locale.ROOT),type=recipe.has("type")?recipe.get("type").getAsString():"";
        boolean context=label.contains("chemical")||label.contains("gas")||label.contains("slurry")||label.contains("infus")||label.contains("pigment")||recipe.has("chemicalType")&&(label.contains("input")||label.contains("output"))||type.equals("mekanism:chemical_infusing")&&label.contains("input")||Set.of("mekanism:activating","mekanism:centrifuging").contains(type)&&label.contains("input");
        if(context&&(object.has("tag")||object.has("id")||object.has("ingredients")||object.has("type"))){
            if(unified()||label.contains("input_chemical")||label.contains("output_chemical"))return unified()?"chemical":legacyKind(recipe,label,type);
            return legacyKind(recipe,label,type);
        }
        if(unified()&&type.startsWith("mekanism:")&&object.has("id")&&object.has("amount")&&!object.has("count"))return "chemical";
        return "";
    }
    private static String legacyKind(JsonObject recipe,String label,String type){
        if(recipe.has("chemicalType"))return recipe.get("chemicalType").getAsString();
        if(label.contains("slurry"))return "slurry";if(label.contains("pigment")||type.contains("pigment")||type.equals("mekanism:painting"))return "pigment";
        if(label.contains("infus")||type.contains("metallurgic_infusing"))return "infuse_type";return "gas";
    }
    public static long amount(JsonElement value){
        if(value.isJsonArray())return value.getAsJsonArray().isEmpty()?1:amount(value.getAsJsonArray().get(0));
        if(value.isJsonObject()){var o=value.getAsJsonObject();if(o.has("amount"))return o.get("amount").getAsBigDecimal().longValueExact();if(o.has("ingredient"))return amount(o.get("ingredient"));}return 1;
    }
    @SuppressWarnings("unchecked")
    public static List<?> values(JsonElement json,String kind,boolean output,DynamicOps<JsonElement> ops){
        if(STACK==null)return List.of();
        if(leaf(json)!=json){var flat=leaf(json).deepCopy();if(flat.isJsonObject())flat.getAsJsonObject().addProperty("amount",amount(json));json=flat;}
        try{
            if(unified()){
                var type=Class.forName(output?API+"chemical.ChemicalStack":API+"recipes.ingredients.ChemicalStackIngredient");
                var codec=(Codec<Object>)type.getField("CODEC").get(null);
                var parsed=codec.parse(ops,json).result();if(parsed.isEmpty())return List.of();
                if(output)return List.of(parsed.get());
                return (List<?>)type.getMethod("getRepresentations").invoke(parsed.get());
            }
            var access=Class.forName(API+"recipes.ingredients.creator.IngredientCreatorAccess");
            String creator=switch(kind){case "infuse_type"->"infusion";case "pigment"->"pigment";case "slurry"->"slurry";default->"gas";};
            var object=access.getMethod(creator).invoke(null);
            var parsed=Class.forName(API+"recipes.ingredients.creator.IIngredientCreator").getMethod("deserialize",JsonElement.class).invoke(object,json);
            return (List<?>)Class.forName(API+"recipes.ingredients.InputIngredient").getMethod("getRepresentations").invoke(parsed);
        }catch(ReflectiveOperationException|RuntimeException ex){return List.of();}
    }
    public static Object fromId(String id,long amount,String kind,DynamicOps<JsonElement> ops){
        var json=new JsonObject();json.addProperty(unified()?"chemical":kind,id);json.addProperty("amount",amount);
        var values=values(json,kind,false,ops);if(values.isEmpty())throw new IllegalArgumentException("vjrm.error.chemical");return values.get(0);
    }
    /** Replaces the selected chemical expression while preserving amount and unrelated extension fields. */
    public static JsonElement replace(JsonElement original,String kind,boolean output,String id,boolean tag,long amount){
        var root=original.deepCopy();var atom=leaf(root);var changed=atom.isJsonObject()?atom.getAsJsonObject():new JsonObject();
        for(String key:List.of("gas","infuse_type","pigment","slurry","chemical","id","tag","ingredients","children","base","excluded"))changed.remove(key);
        if(changed.has("type")&&changed.get("type").getAsString().startsWith("mekanism:"))changed.remove("type");
        changed.addProperty(tag?"tag":kind.equals("chemical")?(output?"id":"chemical"):kind,id);
        if(root!=atom&&root.isJsonObject()){root.getAsJsonObject().add("ingredient",changed);root.getAsJsonObject().addProperty("amount",amount);}else{changed.addProperty("amount",amount);root=changed;}
        return root;
    }
    private ChemicalSupport(){}
}
