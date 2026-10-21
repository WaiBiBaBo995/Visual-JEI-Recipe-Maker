package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Real UI flows for adding fields that the automatic scanner intentionally misses. */
final class ManualFieldChecks {
    private static RecipeDocument fixture(){
        var json=RecipeDocument.template("smelting").json();
        json.add("manual_payload",JsonParser.parseString("{\"hidden\":\"minecraft:emerald\",\"a/b~c\":[{\"token\":\"minecraft:gold_ingot\"}],\"custom\":{\"flag\":true}}"));
        return new RecipeDocument(json);
    }
    private static Button button(net.minecraft.client.gui.screens.Screen screen,String key){
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(Component.translatable(key))).findFirst().orElseThrow();
    }
    private static ManualFieldScreen open(RecipeEditorScreen editor){
        button(editor,"vjrm.manual.open").onPress(new net.minecraft.client.input.KeyEvent(257,0,0));check(Minecraft.getInstance().screen instanceof ManualFieldScreen,"No manual JSON field picker");
        return (ManualFieldScreen)Minecraft.getInstance().screen;
    }
    private static void choose(ManualFieldScreen screen,String path,String kind,boolean output)throws Exception{
        ((EditBox)field(screen,"pathBox")).setValue(path);
        for(int i=0;i<8&&!field(screen,"kind").equals(kind);i++)((Button)field(screen,"kindButton")).onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
        if((boolean)field(screen,"output")!=output)((Button)field(screen,"roleButton")).onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
        check(((Button)field(screen,"addButton")).active,"Existing JSON path was rejected: "+path);
        ((Button)field(screen,"addButton")).onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
    }
    static void run()throws Exception{
        var mc=Minecraft.getInstance();var previous=mc.screen;
        try{
            var document=fixture();var editor=new RecipeEditorScreen(document,null,"vjrm:manual_checks",null);mc.setScreen(editor);
            check(document.fields().stream().noneMatch(f->f.label().equals("manual_payload/hidden")),"Fixture no longer reproduces an undetected field");
            var target=editor.dropTargets(new ItemStack(Items.STONE)).stream().filter(t->t.field().output()).findFirst().orElseThrow().area();
            @SuppressWarnings("unchecked") var bindings=(Map<RecipePreview.Slot,RecipeDocument.Field>)field(editor,"bindings");
            var unbound=bindings.keySet().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow();bindings.remove(unbound);
            editor.mouseClicked(target.getX()+3,target.getY()+3,0);
            check(field(editor,"selectedSlot")!=null,"Unbound slot click missed: "+target);
            System.out.println("Manual selected role: "+((RecipePreview.Slot)field(editor,"selectedSlot")).getRole());
            var picker=open(editor);var pathBox=(EditBox)field(picker,"pathBox");
            picker.init(mc,320,240);pathBox=(EditBox)field(picker,"pathBox");
            @SuppressWarnings("unchecked") var rows=(List<Button>)field(picker,"rows");
            check(rows.size()>=3&&rows.get(rows.size()-1).getY()+20<=pathBox.getY(),"Small field picker hides the list or overlaps the path box");
            var search=picker.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
            search.setValue("manual_payload/hidden");rows.get(0).onPress(new net.minecraft.client.input.KeyEvent(257,0,0));check(pathBox.getValue().equals("/manual_payload/hidden"),"Choosing a JSON node did not select its path");search.setValue("");
            for(String invalid:List.of("/missing/path","/manual_payload/~2invalid","/type")){
                pathBox.setValue(invalid);check(!((Button)field(picker,"addButton")).active,"Invalid/manual type path accepted: "+invalid);
            }
            var unchanged=document.json();choose(picker,"/manual_payload/hidden","item",true);
            check(document.json().equals(unchanged),"Adding field metadata rewrote recipe JSON");
            var added=document.fields().stream().filter(f->f.label().equals("manual_payload/hidden")).findFirst().orElseThrow();
            check(added.output()&&!added.raw(),"Field type/role lost");
            @SuppressWarnings("unchecked") var rebound=(Map<RecipePreview.Slot,RecipeDocument.Field>)field(editor,"bindings");
            check(rebound.entrySet().stream().anyMatch(e->e.getKey().getRole()==RecipePreview.Role.OUTPUT&&e.getValue().equals(added)),"New field was not bound to the selected unbound slot; manual="+field(editor,"manualBindings")+"; actual="+rebound);
            editor.accept(added,new ItemStack(Items.DIAMOND));
            check(document.get(added).getAsString().equals("minecraft:diamond")&&document.json().get("result").equals(unchanged.get("result")),"Manual replacement modified the wrong JSON node");
            choose(open(editor),"/manual_payload/a~1b~0c/0/token","item",false);
            var escaped=document.manualFields().stream().filter(f->f.path().contains("a/b~c")).findFirst().orElseThrow();
            editor.accept(escaped,new ItemStack(Items.EMERALD));
            check(document.get(escaped).getAsString().equals("minecraft:emerald"),"Escaped JSON Pointer or array index failed");
            choose(open(editor),"/cookingtime","json",false);
            check(document.fields().stream().filter(f->f.label().equals("cookingtime")).count()==1,"Manual field duplicated auto field");
            var before=document.json();((EditBox)field(editor,"valueBox")).setValue("invalid");
            var apply=RecipeEditorScreen.class.getDeclaredMethod("applyValue");apply.setAccessible(true);apply.invoke(editor);
            check(document.json().equals(before),"Invalid raw JSON changed the recipe");
            for(String invalid:List.of("1,\"ignored\":5","1 2","", "null true")){
                ((EditBox)field(editor,"valueBox")).setValue(invalid);apply.invoke(editor);
                check(document.json().equals(before),"Trailing/empty raw JSON changed the recipe: "+invalid);
            }
            ((EditBox)field(editor,"valueBox")).setValue("321");apply.invoke(editor);
            check(document.json().get("cookingtime").getAsInt()==321&&document.manualFields().size()==3,"Raw field edit or refresh lost definitions");
            choose(open(editor),"/manual_payload/custom","json",false);
            ((EditBox)field(editor,"valueBox")).setValue("{\"flag\":false,\"numbers\":[1,2,3]}");apply.invoke(editor);
            check(document.json().getAsJsonObject("manual_payload").getAsJsonObject("custom").getAsJsonArray("numbers").size()==3,"Custom raw JSON structure was not applied");
            editor.init(mc,320,240);check(document.manualFields().size()==4,"Resize lost manual fields");
            editor.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
            picker=open(editor);pathBox=(EditBox)field(picker,"pathBox");pathBox.setValue("/manual_payload/custom");unchanged=document.json();
            ((Button)field(picker,"removeButton")).onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
            check(document.manualFields().size()==3&&document.json().equals(unchanged),"Removing a manual definition deleted recipe data");
            var replaced=document.json();replaced.remove("manual_payload");document.replace(replaced);check(document.manualFields().size()==1,"Stale paths were kept after JSON replacement");
            replaced=document.json();replaced.addProperty("type","minecraft:smoking");document.replace(replaced);check(document.manualFields().isEmpty(),"Changing recipe type kept old field definitions");
            System.out.println("PASS manual JSON field picker: missing fields, real selected-slot binding, item replacement, raw scalar/object apply and rollback, escaped paths, arrays, resize, removal and metadata isolation");
        }finally{mc.setScreen(previous);}
    }
    static void preview(){
        var editor=new RecipeEditorScreen(fixture(),null,"vjrm:manual_preview",null);Minecraft.getInstance().setScreen(editor);
        open(editor);
    }
}

