package com.pureneko.vjrm.recipe;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Client-owned field metadata. Recipe values and automatic bindings are never stored here. */
public final class RecipeFieldProfiles {
    private static final int MAX_BYTES=4*1024*1024, MAX_TYPES=256, MAX_FIELDS=1024, MAX_LAYOUTS=128, MAX_SLOTS=4096;
    private static final int MAX_PATH_LENGTH=4096, MAX_PATH_DEPTH=64;
    private static final Set<String> CHEMICALS=Set.of("","gas","infuse_type","pigment","slurry","chemical","aspect");
    private static final Set<String> ROLES=Set.of("INPUT","OUTPUT","CATALYST","RENDER_ONLY");
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Object FILE_LOCK=new Object();
    private final Path file;
    private volatile Map<String,Profile> profiles;

    private static final class Profile {
        final Map<List<String>,RecipeDocument.Field> fields=new LinkedHashMap<>();
        final Map<String,Map<Integer,String>> layouts=new LinkedHashMap<>();
        boolean empty(){return fields.isEmpty()&&layouts.values().stream().allMatch(Map::isEmpty);}
    }
    @FunctionalInterface private interface Change { void apply(Map<String,Profile> state)throws IOException; }
    public record Slot(String role,int x,int y,int width,int height) { }

    public RecipeFieldProfiles(Path file)throws IOException {
        if(file==null)throw invalid("Missing profile file");
        this.file=file.toAbsolutePath().normalize();
        profiles=read();
    }
    public List<RecipeDocument.Field> fields(String type){
        var profile=profiles.get(type);
        return profile==null?List.of():List.copyOf(profile.fields.values());
    }
    public Map<Integer,String> bindings(String type,String layout){
        var profile=profiles.get(type);
        var bindings=profile==null?null:profile.layouts.get(layout);
        return bindings==null?Map.of():Collections.unmodifiableMap(new LinkedHashMap<>(bindings));
    }
    public boolean hasProfile(String type){var profile=profiles.get(type);return profile!=null&&!profile.empty();}

    public void putField(String type,RecipeDocument.Field field)throws IOException {
        checkType(type);checkField(field);
        var copy=new RecipeDocument.Field(List.copyOf(field.path()),field.fluid(),field.output(),field.chemical(),field.raw());
        change(state->state.computeIfAbsent(type,key->new Profile()).fields.put(copy.path(),copy));
    }
    /** Adds a definition and its selected-slot binding in one durable transaction. */
    public void putFieldAndBind(String type,RecipeDocument.Field field,String layout,int slot)throws IOException {
        checkType(type);checkField(field);checkText(layout,256,false,"layout");checkSlot(slot);checkLabel(field.label());
        var copy=new RecipeDocument.Field(List.copyOf(field.path()),field.fluid(),field.output(),field.chemical(),field.raw());
        change(state->{
            var profile=state.computeIfAbsent(type,key->new Profile());
            profile.fields.put(copy.path(),copy);
            profile.layouts.computeIfAbsent(layout,key->new LinkedHashMap<>()).put(slot,copy.label());
        });
    }
    public void bind(String type,String layout,int slot,String pathLabel)throws IOException {
        checkType(type);checkText(layout,256,false,"layout");checkSlot(slot);checkLabel(pathLabel);
        change(state->state.computeIfAbsent(type,key->new Profile()).layouts.computeIfAbsent(layout,key->new LinkedHashMap<>()).put(slot,pathLabel));
    }
    public void removeField(String type,List<String> path)throws IOException {
        checkType(type);checkPath(path);String label=JsonFieldPaths.label(path);
        change(state->{
            var profile=state.get(type);if(profile==null)return;
            profile.fields.remove(path);
            profile.layouts.values().forEach(bindings->bindings.values().removeIf(label::equals));
            profile.layouts.values().removeIf(Map::isEmpty);
            if(profile.empty())state.remove(type);
        });
    }
    public void reset(String type)throws IOException {checkType(type);change(state->state.remove(type));}

    private void change(Change change)throws IOException {
        synchronized(FILE_LOCK){
            // Another editor may have saved since this instance was constructed.
            var next=read();change.apply(next);
            byte[] bytes=JSON.toJson(encode(next)).getBytes(StandardCharsets.UTF_8);
            if(bytes.length>MAX_BYTES)throw invalid("Profile file is too large");
            Path parent=file.getParent();Files.createDirectories(parent);
            Path temporary=Files.createTempFile(parent,file.getFileName()+".",".tmp");
            try{
                Files.write(temporary,bytes);
                try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException ex){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
                profiles=next;
            }finally{Files.deleteIfExists(temporary);}
        }
    }
    private Map<String,Profile> read()throws IOException {
        if(!Files.exists(file))return new LinkedHashMap<>();
        byte[] bytes;
        try(var input=Files.newInputStream(file)){bytes=input.readNBytes(MAX_BYTES+1);}
        if(bytes.length>MAX_BYTES)throw invalid("Profile file is too large");
        try{
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            JsonElement parsed;
            try(var reader=new JsonReader(new StringReader(text))){
                reader.setLenient(false);parsed=Streams.parse(reader);
                if(reader.peek()!=JsonToken.END_DOCUMENT)throw invalid("Trailing profile data");
            }
            return decode(parsed);
        }catch(IOException ex){throw ex;}
        catch(RuntimeException|StackOverflowError ex){throw new IOException("Invalid recipe field profiles",ex);}
    }
    private static Map<String,Profile> decode(JsonElement value)throws IOException {
        var root=object(value,"profile root");keys(root,Set.of("version","types"));
        var version=root.get("version");
        if(version==null||!version.isJsonPrimitive()||!version.getAsJsonPrimitive().isNumber()||!version.getAsString().equals("1"))throw invalid("Unsupported profile version");
        var types=object(root.get("types"),"types");limit(types.size(),MAX_TYPES,"recipe types");
        var result=new LinkedHashMap<String,Profile>();
        for(var entry:types.entrySet()){
            checkType(entry.getKey());var node=object(entry.getValue(),"recipe type");keys(node,Set.of("fields","layouts"));
            var profile=new Profile();var fields=node.get("fields");
            if(fields==null||!fields.isJsonArray())throw invalid("Expected profile fields array");
            limit(fields.getAsJsonArray().size(),MAX_FIELDS,"manual fields");
            for(var item:fields.getAsJsonArray()){
                var field=object(item,"field");keys(field,Set.of("path","fluid","output","chemical","raw"));
                var parts=field.get("path");if(parts==null||!parts.isJsonArray())throw invalid("Expected field path array");
                limit(parts.getAsJsonArray().size(),MAX_PATH_DEPTH,"path depth");
                var path=new ArrayList<String>();for(var part:parts.getAsJsonArray())path.add(string(part,"path segment"));
                var definition=new RecipeDocument.Field(List.copyOf(path),bool(field.get("fluid")),bool(field.get("output")),string(field.get("chemical"),"chemical"),bool(field.get("raw")));
                checkField(definition);
                if(profile.fields.put(definition.path(),definition)!=null)throw invalid("Duplicate field path");
            }
            var layouts=object(node.get("layouts"),"layouts");limit(layouts.size(),MAX_LAYOUTS,"layouts");
            for(var layout:layouts.entrySet()){
                checkText(layout.getKey(),256,false,"layout");var layoutNode=object(layout.getValue(),"layout");keys(layoutNode,Set.of("bindings"));
                var bindings=object(layoutNode.get("bindings"),"bindings");limit(bindings.size(),MAX_SLOTS,"slot bindings");
                var mapped=new LinkedHashMap<Integer,String>();
                for(var binding:bindings.entrySet()){
                    if(!binding.getKey().matches("0|[1-9][0-9]{0,3}"))throw invalid("Invalid slot index");
                    int slot=Integer.parseInt(binding.getKey());checkSlot(slot);
                    String label=string(binding.getValue(),"bound path");checkLabel(label);mapped.put(slot,label);
                }
                profile.layouts.put(layout.getKey(),mapped);
            }
            result.put(entry.getKey(),profile);
        }
        return result;
    }
    private static JsonObject encode(Map<String,Profile> state)throws IOException {
        limit(state.size(),MAX_TYPES,"recipe types");
        var root=new JsonObject();root.addProperty("version",1);var types=new JsonObject();root.add("types",types);
        for(var entry:state.entrySet()){
            var profile=entry.getValue();limit(profile.fields.size(),MAX_FIELDS,"manual fields");limit(profile.layouts.size(),MAX_LAYOUTS,"layouts");
            var node=new JsonObject();types.add(entry.getKey(),node);var fields=new JsonArray();node.add("fields",fields);
            for(var definition:profile.fields.values()){
                var field=new JsonObject();var path=new JsonArray();definition.path().forEach(path::add);field.add("path",path);
                field.addProperty("fluid",definition.fluid());field.addProperty("output",definition.output());field.addProperty("chemical",definition.chemical());field.addProperty("raw",definition.raw());fields.add(field);
            }
            var layouts=new JsonObject();node.add("layouts",layouts);
            for(var layout:profile.layouts.entrySet()){
                limit(layout.getValue().size(),MAX_SLOTS,"slot bindings");
                var layoutNode=new JsonObject();layouts.add(layout.getKey(),layoutNode);var bindings=new JsonObject();layoutNode.add("bindings",bindings);
                layout.getValue().forEach((slot,path)->bindings.addProperty(Integer.toString(slot),path));
            }
        }
        return root;
    }
    private static JsonObject object(JsonElement value,String name)throws IOException {
        if(value==null||!value.isJsonObject())throw invalid("Expected "+name+" object");return value.getAsJsonObject();
    }
    private static String string(JsonElement value,String name)throws IOException {
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw invalid("Expected "+name+" string");return value.getAsString();
    }
    private static boolean bool(JsonElement value)throws IOException {
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw invalid("Expected field boolean");return value.getAsBoolean();
    }
    private static void keys(JsonObject object,Set<String> allowed)throws IOException {
        if(!allowed.equals(object.keySet()))throw invalid("Unsupported profile properties");
    }
    private static void limit(int count,int maximum,String name)throws IOException {if(count>maximum)throw invalid("Too many "+name);}
    private static void checkText(String value,int maximum,boolean empty,String name)throws IOException {
        if(value==null||(!empty&&value.isEmpty())||value.length()>maximum||value.chars().anyMatch(c->c<32))throw invalid("Invalid "+name);
    }
    private static void checkType(String type)throws IOException {
        checkText(type,256,false,"recipe type");if(!type.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw invalid("Invalid recipe type ID");
    }
    private static void checkPath(List<String> path)throws IOException {
        if(path==null||path.isEmpty()||path.size()>MAX_PATH_DEPTH||path.equals(List.of("type")))throw invalid("Invalid field path");
        int length=0;for(String part:path){checkText(part,MAX_PATH_LENGTH,true,"path segment");length+=part.length()+1;}
        if(length>MAX_PATH_LENGTH)throw invalid("Field path is too long");
    }
    private static void checkField(RecipeDocument.Field field)throws IOException {
        if(field==null)throw invalid("Missing field");checkPath(field.path());
        if(field.chemical()==null||!CHEMICALS.contains(field.chemical()))throw invalid("Invalid chemical kind");
    }
    private static void checkLabel(String label)throws IOException {
        checkText(label,MAX_PATH_LENGTH,true,"bound path");
        try{var path=JsonFieldPaths.parse("/"+label);checkPath(path);if(!JsonFieldPaths.label(path).equals(label))throw invalid("Invalid escaped field path");}
        catch(IllegalArgumentException ex){throw new IOException("Invalid bound field path",ex);}
    }
    private static void checkSlot(int slot)throws IOException {if(slot<0||slot>=MAX_SLOTS)throw invalid("Invalid slot index");}
    private static IOException invalid(String message){return new IOException(message);}

    /** The fingerprint describes the layout, independent of recipe materials and screen scaling. */
    public static String layoutKey(String viewer,String category,boolean source,int width,int height,List<Slot> slots){
        try{
            checkText(viewer,128,false,"viewer");checkText(category,256,false,"category");
            if(width<1||height<1||width>65535||height>65535||slots==null)throw invalid("Invalid layout size");
            limit(slots.size(),MAX_SLOTS,"layout slots");
            var node=new JsonObject();node.addProperty("viewer",viewer);node.addProperty("category",category);node.addProperty("source",source);node.addProperty("width",width);node.addProperty("height",height);
            var positions=new JsonArray();node.add("slots",positions);
            for(var slot:slots){
                if(slot==null||slot.role()==null||!ROLES.contains(slot.role())||slot.x()<-65535||slot.x()>65535||slot.y()<-65535||slot.y()>65535||slot.width()<1||slot.width()>65535||slot.height()<1||slot.height()>65535)throw invalid("Invalid layout slot");
                var row=new JsonArray();row.add(slot.role());row.add(slot.x());row.add(slot.y());row.add(slot.width());row.add(slot.height());positions.add(row);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(node.toString().getBytes(StandardCharsets.UTF_8)));
        }catch(IOException ex){throw new IllegalArgumentException(ex.getMessage(),ex);}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
