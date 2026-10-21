package com.pureneko.vjrm.recipe;

import com.google.gson.JsonElement;
import java.util.*;
import java.util.stream.Collectors;

/** JSON Pointer paths keep literal slashes, tildes and array indices unambiguous. */
public final class JsonFieldPaths {
    public record Node(List<String> path,String preview) {
        public String pointer(){return "/"+label(path);}
    }
    public static String label(List<String> path){return path.stream().map(p->p.replace("~","~0").replace("/","~1")).collect(Collectors.joining("/"));}
    public static List<String> parse(String text){
        if(text==null||text.isBlank())throw new IllegalArgumentException("vjrm.error.field_path");
        String pointer=text.startsWith("/")?text.substring(1):text;
        var parts=new ArrayList<String>();
        for(String part:pointer.split("/",-1)){
            if(part.matches(".*~(?:[^01]|$).*"))throw new IllegalArgumentException("vjrm.error.field_path");
            parts.add(part.replace("~1","/").replace("~0","~"));
        }
        return List.copyOf(parts);
    }
    public static JsonElement get(JsonElement root,List<String> path){
        JsonElement value=root;
        try{
            for(String part:path){
                if(value==null)throw new IllegalArgumentException();
                if(value.isJsonArray()){
                    if(!part.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException();
                    value=value.getAsJsonArray().get(Integer.parseInt(part));
                }else if(value.isJsonObject())value=value.getAsJsonObject().get(part);
                else throw new IllegalArgumentException();
            }
            if(value==null)throw new IllegalArgumentException();
            return value;
        }catch(RuntimeException ex){throw new IllegalArgumentException("vjrm.error.field_path",ex);}
    }
    public static boolean exists(JsonElement root,List<String> path){try{get(root,path);return true;}catch(IllegalArgumentException ex){return false;}}
    public static List<Node> nodes(JsonElement root){
        var out=new ArrayList<Node>();walk(root,List.of(),out);return List.copyOf(out);
    }
    private static void walk(JsonElement value,List<String> path,List<Node> out){
        if(!path.isEmpty())out.add(new Node(List.copyOf(path),value.toString()));
        if(out.size()>=4096||path.size()>=64)return;
        if(value.isJsonObject())for(var entry:value.getAsJsonObject().entrySet()){
            var next=new ArrayList<>(path);next.add(entry.getKey());walk(entry.getValue(),next,out);if(out.size()>=4096)break;
        }else if(value.isJsonArray())for(int i=0;i<value.getAsJsonArray().size();i++){
            var next=new ArrayList<>(path);next.add(Integer.toString(i));walk(value.getAsJsonArray().get(i),next,out);if(out.size()>=4096)break;
        }
    }
    private JsonFieldPaths(){}
}

