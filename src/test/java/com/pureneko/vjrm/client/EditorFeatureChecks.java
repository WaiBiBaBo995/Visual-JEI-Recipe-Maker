package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.item.*;
import java.util.*;

/** Public mouse/key paths and real screen widgets, rather than copies of the implementation. */
final class EditorFeatureChecks {
    static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void run()throws Exception{
        ManualFieldChecks.run();
        var mc=Minecraft.getInstance();var doc=RecipeDocument.template("smelting");
        var editor=new RecipeEditorScreen(doc,null,"vjrm:features",null);mc.setScreen(editor);
        check(((Button)field(editor,"fieldButton")).getY()+18<=(int)field(editor,"previewY"),"Field controls overlap or sit below recipe UI");
        check(editor.sourceDescription().getString().contains("minecraft:smelting")&&!editor.sourceDescription().getString().equals(net.minecraft.network.chat.Component.translatable("vjrm.source_type","minecraft:smelting").getString()),"Recipe source has no display name");
        var layout=(RecipePreview.Layout)field(editor,"layout");
        var tooltips=layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.OUTPUT).findFirst().orElseThrow().tooltip();
        check(tooltips.stream().anyMatch(c->c.getString().contains("minecraft:stone"))&&tooltips.stream().anyMatch(c->c.getString().equals(new ItemStack(Items.STONE).getHoverName().getString())),"Ingredient tooltip has no name/id");
        var output=editor.dropTargets(new ItemStack(Items.STONE)).stream().filter(t->t.field().output()).findFirst().orElseThrow();
        var before=doc.json();double x=output.area().getX()+2,y=output.area().getY()+2;
        editor.mouseClicked(x,y,1);check(doc.json().equals(before),"Rejected right-click mutated output");
        check(field(editor,"clickReason")!=null,"Missing right-click reason");
        float scale=(float)field(editor,"uiScale");check(Math.abs((int)field(editor,"reasonX")*scale-x)<2,"Right-click popup is not at the mouse");
        check((long)field(editor,"reasonUntil")>net.minecraft.util.Util.getMillis(),"Right-click popup expired immediately");
        var input=editor.dropTargets(new ItemStack(Items.STONE)).stream().filter(t->!t.field().output()).findFirst().orElseThrow();
        editor.mouseClicked(input.area().getX()+2,input.area().getY()+2,1);check(doc.json().equals(before),"Required ingredient was deleted");
        var single=RecipeDocument.template("crafting_shapeless");var one=new RecipeEditorScreen(single,null,"vjrm:last",null);mc.setScreen(one);
        var only=one.dropTargets(new ItemStack(Items.STONE)).stream().filter(t->!t.field().output()&&!t.field().label().startsWith("$crafting")).findFirst().orElseThrow();
        var saved=single.json();one.mouseClicked(only.area().getX()+2,only.area().getY()+2,1);
        check(single.json().equals(saved)&&field(one,"clickReason")!=null,"Last input was removed or has no reason");
        var numeric=new NumericFieldsScreen(editor,doc,()->{});mc.setScreen(numeric);
        @SuppressWarnings("unchecked") var boxes=(List<EditBox>)field(numeric,"boxes");
        @SuppressWarnings("unchecked") var fields=(List<RecipeDocument.NumericField>)field(numeric,"fields");
        for(int i=0;i<fields.size()&&i<boxes.size();i++)if(fields.get(i).label().equals("cookingtime"))boxes.get(i).setValue("321");
        numeric.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(net.minecraft.network.chat.Component.translatable("vjrm.apply"))).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
        check(doc.json().get("cookingtime").getAsInt()==321,"Numeric UI did not apply cooking time");
        numeric=new NumericFieldsScreen(editor,doc,()->{});mc.setScreen(numeric);
        @SuppressWarnings("unchecked") var invalidBoxes=(List<EditBox>)field(numeric,"boxes");
        @SuppressWarnings("unchecked") var invalidFields=(List<RecipeDocument.NumericField>)field(numeric,"fields");
        for(int i=0;i<invalidFields.size()&&i<invalidBoxes.size();i++)if(invalidFields.get(i).label().equals("cookingtime"))invalidBoxes.get(i).setValue("not a number");
        numeric.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(net.minecraft.network.chat.Component.translatable("vjrm.apply"))).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
        check(mc.screen==numeric&&doc.json().get("cookingtime").getAsInt()==321,"Invalid numeric edit was applied");numeric.onClose();
        var json=new JsonDraftScreen(editor,doc.json(),doc::replace);mc.setScreen(json);var box=(JsonEditBox)field(json,"box");
        box.setValue("");box.setFocused(true);box.charTyped('{',0);check(box.getValue().equals("{}")&&box.cursor()==1,"Bracket completion failed");
        box.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);check(box.getValue().equals("{\n  \n}"),"Automatic indentation failed");
        box.charTyped('"',0);check(box.getValue().equals("{\n  \"\"\n}"),"Quote completion failed");
        box.setValue("{\"type\": invalid}");json.tick();((Button)field(json,"apply")).onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
        check(doc.json().get("cookingtime").getAsInt()==321,"Invalid JSON mutated recipe");
        box.setValue(new com.google.gson.Gson().toJson(doc.json()));
        var format=JsonDraftScreen.class.getDeclaredMethod("format");format.setAccessible(true);format.invoke(json);
        check(box.getValue().contains("\n")&&JsonSyntax.parse(box.getValue()).equals(doc.json()),"Formatting changed recipe content");
        String previousClipboard=mc.keyboardHandler.getClipboard();
        try{
            var copy=json.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(net.minecraft.network.chat.Component.translatable("vjrm.json.copy"))).findFirst().orElseThrow();
            box.setValue("{\n  \"draft\": invalid\n}");String exact=box.getValue();var documentBefore=doc.json();copy.onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
            check(mc.keyboardHandler.getClipboard().equals(exact),"Copy button did not copy the exact draft");check(doc.json().equals(documentBefore)&&mc.screen==json,"Copy button applied or closed the draft");
            json.init(mc,320,240);for(var child:json.children())if(child instanceof Button b)check(b.getX()>=0&&b.getX()+b.getWidth()<=320,"JSON footer buttons overflow a small screen");
            box=(JsonEditBox)field(json,"box");box.setValue(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(doc.json()));json.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        }finally{mc.keyboardHandler.setClipboard(previousClipboard);}
        System.out.println("PASS editor features: centered layout, right-click reasons/rollback, numeric UI apply/reject, JSON completion/indent/format");
    }
}
