package com.pureneko.vjrm;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Disk-backed metadata checks; also runnable without launching a client or server. */
public final class FieldProfileChecks {
    @FunctionalInterface private interface Operation { void run()throws Exception; }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void rejects(Operation operation)throws Exception {
        try{operation.run();throw new AssertionError("Invalid profile data was accepted");}catch(IOException expected){ }
    }
    private static void unchanged(Path file,byte[] original)throws Exception {check(Arrays.equals(Files.readAllBytes(file),original),"Rejected profile operation changed original file");}
    public static int run()throws Exception {
        int checks=0;
        Path directory=Files.createTempDirectory("vjrm-field-profile-checks-");
        Path file=directory.resolve("config/vjrm/field_profiles.json");
        var profiles=new RecipeFieldProfiles(file);
        check(!profiles.hasProfile("test:machine")&&profiles.fields("test:machine").isEmpty()&&!Files.exists(file),"Reading a missing profile created data");checks++;
        var first=new RecipeDocument.Field(List.of("results","0","basePredicate"),false,true);
        var escaped=new RecipeDocument.Field(List.of("payload","a/b~c","0","value"),false,false,"gas",false);
        var raw=new RecipeDocument.Field(List.of("custom"),false,false,"",true);
        var fluid=new RecipeDocument.Field(List.of("fluid_input"),true,false);
        profiles.putField("test:machine",first);profiles.putField("test:machine",escaped);profiles.putField("test:machine",raw);profiles.putField("test:machine",fluid);
        profiles.bind("test:machine","layout-a",2,first.label());profiles.bind("test:machine","layout-b",7,first.label());
        // Automatically detected fields can be rebound without creating a manual definition.
        profiles.bind("test:machine","layout-a",1,"input");
        profiles.bind("test:machine","layout-a",0,escaped.label());
        var reloaded=new RecipeFieldProfiles(file);
        check(reloaded.hasProfile("test:machine")&&reloaded.fields("test:machine").equals(List.of(first,escaped,raw,fluid)),"Metadata did not survive a new store instance");
        check(reloaded.bindings("test:machine","layout-a").equals(Map.of(2,first.label(),1,"input",0,escaped.label())),"Bindings or escaped paths did not survive reload");checks++;
        var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        check(root.get("version").getAsInt()==1&&root.getAsJsonObject("types").getAsJsonObject("test:machine").getAsJsonObject("layouts").getAsJsonObject("layout-a").has("bindings"),"Unexpected profile schema");checks++;
        try{reloaded.fields("test:machine").clear();throw new AssertionError("Mutable field list exposed");}catch(UnsupportedOperationException expected){ }
        try{reloaded.fields("test:machine").get(0).path().clear();throw new AssertionError("Mutable field path exposed");}catch(UnsupportedOperationException expected){ }
        try{reloaded.bindings("test:machine","layout-a").clear();throw new AssertionError("Mutable bindings exposed");}catch(UnsupportedOperationException expected){ }checks++;

        // A recipe lacking an optional path must not delete a type-wide stored definition.
        var lacking=JsonParser.parseString("{\"type\":\"test:machine\",\"input\":{\"item\":\"minecraft:stone\"}}").getAsJsonObject();
        var applicable=reloaded.fields("test:machine").stream().filter(field->JsonFieldPaths.exists(lacking,field.path())).toList();
        check(applicable.isEmpty()&&new RecipeFieldProfiles(file).fields("test:machine").size()==4,"Missing recipe paths erased stored fields");checks++;
        var updated=new RecipeDocument.Field(first.path(),false,false,"slurry",false);
        profiles.putField("test:machine",updated);
        check(new RecipeFieldProfiles(file).fields("test:machine").size()==4&&new RecipeFieldProfiles(file).fields("test:machine").get(0).equals(updated),"Updating a path duplicated it or lost kind/role");checks++;
        profiles.putField("other:machine",raw);profiles.bind("other:machine","layout-a",2,raw.label());
        profiles.removeField("test:machine",first.path());
        reloaded=new RecipeFieldProfiles(file);
        check(reloaded.fields("test:machine").size()==3&&!reloaded.bindings("test:machine","layout-a").containsKey(2)&&reloaded.bindings("test:machine","layout-b").isEmpty(),"Removing a definition left bindings in another layout");
        check(reloaded.bindings("test:machine","layout-a").get(1).equals("input")&&reloaded.bindings("other:machine","layout-a").get(2).equals(raw.label()),"Deleting a field affected unrelated bindings or recipe types");checks++;

        var editorA=new RecipeFieldProfiles(file);var editorB=new RecipeFieldProfiles(file);
        var extra=new RecipeDocument.Field(List.of("extra"),false,true);
        editorA.putField("test:machine",extra);editorB.bind("test:machine","layout-c",5,extra.label());
        editorA.putField("test:machine",new RecipeDocument.Field(raw.path(),false,true,"",true));
        reloaded=new RecipeFieldProfiles(file);
        check(reloaded.fields("test:machine").stream().anyMatch(f->f.equals(extra))&&reloaded.bindings("test:machine","layout-c").get(5).equals("extra"),"Two editors overwrote each other's changes");checks++;
        editorB.reset("test:machine");reloaded=new RecipeFieldProfiles(file);
        check(!reloaded.hasProfile("test:machine")&&reloaded.hasProfile("other:machine"),"Reset affected another recipe type");checks++;

        var combined=new RecipeDocument.Field(List.of("combined","predicate"),false,true);
        profiles.putFieldAndBind("test:combined",combined,"combined-layout",3);
        reloaded=new RecipeFieldProfiles(file);
        check(reloaded.fields("test:combined").equals(List.of(combined))&&reloaded.bindings("test:combined","combined-layout").equals(Map.of(3,combined.label())),"Combined definition and binding did not survive reload");checks++;
        var atomicBefore=Files.readAllBytes(file);
        var rejectedField=new RecipeDocument.Field(List.of("must_not_persist"),false,true);
        rejects(()->profiles.putFieldAndBind("test:combined",rejectedField,"combined-layout",4096));
        unchanged(file,atomicBefore);
        check(new RecipeFieldProfiles(file).fields("test:combined").equals(List.of(combined)),"Rejected combined operation partially saved a field");checks++;

        var positions=List.of(new RecipeFieldProfiles.Slot("INPUT",0,1,18,18),new RecipeFieldProfiles.Slot("OUTPUT",80,1,18,18));
        String layout=RecipeFieldProfiles.layoutKey("jei","test:category",false,100,40,positions);
        check(layout.matches("[0-9a-f]{64}")&&layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",false,100,40,List.copyOf(positions))),"Layout key is not stable SHA-256");
        check(!layout.equals(RecipeFieldProfiles.layoutKey("emi","test:category",false,100,40,positions))&&!layout.equals(RecipeFieldProfiles.layoutKey("jei","other:category",false,100,40,positions))&&!layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",true,100,40,positions)),"Viewer/category/source layouts share a key");
        check(!layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",false,101,40,positions))&&!layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",false,100,41,positions))&&!layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",false,100,40,List.of(positions.get(1),positions.get(0)))),"Layout size or slot order missing from fingerprint");checks++;
        var moved=List.of(new RecipeFieldProfiles.Slot("INPUT",1,1,18,18),positions.get(1));
        var rerole=List.of(new RecipeFieldProfiles.Slot("CATALYST",0,1,18,18),positions.get(1));
        check(!layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",false,100,40,moved))&&!layout.equals(RecipeFieldProfiles.layoutKey("jei","test:category",false,100,40,rerole)),"Slot position or role missing from fingerprint");checks++;
        var valid=Files.readAllBytes(file);
        rejects(()->profiles.bind("test:machine","layout",-1,"input"));
        rejects(()->profiles.bind("test:machine","layout",4096,"input"));
        rejects(()->profiles.bind("test:machine","layout",0,"bad~2escape"));
        rejects(()->profiles.putField("test:machine",new RecipeDocument.Field(List.of("type"),false,false)));
        rejects(()->profiles.putField("test:machine",new RecipeDocument.Field(List.of("input"),false,false,"unknown",false)));
        rejects(()->profiles.putField("bad type",raw));unchanged(file,valid);checks++;

        for(String corrupt:List.of("{", "{}", "[]", "{\"version\":2,\"types\":{}}", "{\"version\":1.5,\"types\":{}}", "{\"version\":\"1\",\"types\":{}}", "{\"version\":1,\"types\":{}} true", "{version:1,types:{}}", "{\"version\":1,/*comment*/\"types\":{}}", "{\"version\":1,\"types\":{\"test:machine\":{\"fields\":[],\"layouts\":{\"x\":{\"bindings\":{\"99999999999\":\"input\"}}}}}}")){
            Files.writeString(file,corrupt,StandardCharsets.UTF_8);byte[] original=Files.readAllBytes(file);
            rejects(()->new RecipeFieldProfiles(file));
            rejects(()->editorA.putField("test:machine",raw));unchanged(file,original);checks++;
        }
        Files.write(file,new byte[]{(byte)0xff,(byte)0xfe});byte[] invalidUtf8=Files.readAllBytes(file);
        rejects(()->new RecipeFieldProfiles(file));rejects(()->editorA.reset("other:machine"));unchanged(file,invalidUtf8);checks++;
        Files.writeString(file," ".repeat(4*1024*1024+1),StandardCharsets.UTF_8);var large=Files.readAllBytes(file);
        rejects(()->new RecipeFieldProfiles(file));rejects(()->editorA.reset("other:machine"));unchanged(file,large);checks++;
        Files.write(file,valid);
        try(var entries=Files.list(file.getParent())){check(entries.noneMatch(path->path.getFileName().toString().endsWith(".tmp")),"Temporary files leaked after saving");}checks++;
        System.out.println("PASS recipe field profiles: disk reload, immutable metadata, optional paths, updates/removal/reset, layout isolation, two-editor merge, corruption protection and atomic saves ("+checks+")");
        return checks;
    }
    public static void main(String[] arguments)throws Exception {run();}
}
