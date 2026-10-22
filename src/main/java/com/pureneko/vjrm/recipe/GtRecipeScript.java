package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.util.regex.Pattern;

/** A reversible KubeJS recipe declaration. No JavaScript is executed by the editor/network. */
public final class GtRecipeScript {
    private static final Gson PRETTY=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern HEADER=Pattern.compile("\\s*ServerEvents\\.recipes\\s*\\(\\s*event\\s*=>\\s*\\{\\s*event\\.recipes\\s*\\[\\s*(\"[^\"]+\")\\s*]\\s*\\[\\s*(\"[^\"]+\")\\s*]\\s*\\(\\s*(\"[^\"]+\")\\s*\\)\\s*\\.merge\\s*\\(");
    private static final Pattern FOOTER=Pattern.compile("\\s*\\)\\s*;?\\s*}\\s*\\)\\s*;?\\s*");
    public record Draft(JsonObject json,String id,String text){}
    public static String fullId(String id,String type){
        var value=ResourceLocation.parse(id);var machine=ResourceLocation.parse(type);
        String path=value.getPath(),prefix=machine.getPath()+"/";
        return ResourceLocation.fromNamespaceAndPath(value.getNamespace().equals("minecraft")?machine.getNamespace():value.getNamespace(),path.startsWith(prefix)?path:prefix+path).toString();
    }
    public static String generate(JsonObject json,String id){
        var machine=ResourceLocation.parse(json.get("type").getAsString());var full=ResourceLocation.parse(fullId(id,machine.toString()));
        String argument=full.getNamespace()+":"+full.getPath().substring(machine.getPath().length()+1);
        return "ServerEvents.recipes(event => {\n  event.recipes["+quote(machine.getNamespace())+"]["+quote(machine.getPath())+"]("+quote(argument)+").merge("+PRETTY.toJson(json).replace("\n","\n  ")+");\n});\n";
    }
    public static Draft parse(String text){
        // Remove comments only for recognizing the declaration; retain the original text for export.
        String source=withoutComments(text);var header=HEADER.matcher(source);
        if(!header.lookingAt())throw new IllegalArgumentException("vjrm.js.structure");
        int start=header.end(),end=objectEnd(source,start);
        JsonObject json=parseData(source.substring(start,end));
        String type=JsonParser.parseString(header.group(1)).getAsString()+":"+JsonParser.parseString(header.group(2)).getAsString();
        if(json.has("type")&&!json.get("type").getAsString().equals(type))throw new IllegalArgumentException("vjrm.js.type");
        json.addProperty("type",type);var footer=FOOTER.matcher(source.substring(end));
        if(!footer.matches())throw new IllegalArgumentException("vjrm.js.structure");
        return new Draft(json,fullId(JsonParser.parseString(header.group(3)).getAsString(),type),text);
    }
    /** Comment removal preserves offsets, so annotations refer to the original script. */
    public static String annotationSource(String text){return withoutComments(text);}
    public static int dataOffset(String text){
        String source=withoutComments(text);var header=HEADER.matcher(source);
        if(!header.lookingAt())throw new IllegalArgumentException("vjrm.js.structure");
        return header.end();
    }
    private static JsonObject parseData(String text){
        try(var reader=new com.google.gson.stream.JsonReader(new java.io.StringReader(text))){
            reader.setLenient(false);
            var value=com.google.gson.internal.Streams.parse(reader);
            if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT||!value.isJsonObject())throw new IllegalArgumentException("vjrm.js.structure");
            return value.getAsJsonObject();
        }catch(java.io.IOException ex){throw new IllegalArgumentException("vjrm.js.structure",ex);}
    }
    private static int objectEnd(String text,int offset){
        int i=offset;while(i<text.length()&&Character.isWhitespace(text.charAt(i)))i++;
        if(i==text.length()||text.charAt(i)!='{')throw new IllegalArgumentException("vjrm.js.structure");
        int depth=0;char quote=0;boolean escape=false;
        for(;i<text.length();i++){
            char c=text.charAt(i);
            if(quote!=0){if(escape)escape=false;else if(c=='\\')escape=true;else if(c==quote)quote=0;continue;}
            if(c=='\"'||c=='\''){quote=c;continue;}
            if(c=='{'||c=='[')depth++;else if(c=='}'||c==']'){if(--depth==0)return i+1;}
        }
        throw new IllegalArgumentException("vjrm.js.structure");
    }
    private static String withoutComments(String text){
        var out=new StringBuilder(text);char quote=0;boolean escape=false;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(quote!=0){if(escape)escape=false;else if(c=='\\')escape=true;else if(c==quote)quote=0;continue;}
            if(c=='\"'||c=='\''||c=='`'){quote=c;continue;}
            if(c=='/'&&i+1<text.length()&&text.charAt(i+1)=='/'){
                while(i<text.length()&&text.charAt(i)!='\n')out.setCharAt(i++,' ');i--;
            }else if(c=='/'&&i+1<text.length()&&text.charAt(i+1)=='*'){
                int end=text.indexOf("*/",i+2);if(end<0)throw new IllegalArgumentException("vjrm.js.structure");
                for(int n=i;n<end+2;n++)if(text.charAt(n)!='\n')out.setCharAt(n,' ');i=end+1;
            }
        }
        return out.toString();
    }
    private static String quote(String value){return new JsonPrimitive(value).toString();}
    private GtRecipeScript(){}
}
