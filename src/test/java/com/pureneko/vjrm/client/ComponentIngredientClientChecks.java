package com.pureneko.vjrm.client;

import com.google.gson.*;
import com.pureneko.vjrm.ComponentIngredientChecks;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.nio.file.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

/** Exercise public inventory drag, controls, re-opening and both native browser layouts. */
final class ComponentIngredientClientChecks {
    private static RecipeEditorScreen.DropTarget target(RecipeEditorScreen editor,String path,ItemStack stack){
        return editor.dropTargets(stack).stream().filter(t->t.field().label().equals(path)).findFirst().orElseThrow(()->new AssertionError("Missing whole ingredient binding: "+path));
    }
    private static void drag(RecipeEditorScreen editor,RecipeEditorScreen.DropTarget target)throws Exception{
        var method=RecipeEditorScreen.class.getDeclaredMethod("inventoryRect",int.class);method.setAccessible(true);
        var rect=(Rect2i)method.invoke(editor,0);float scale=(float)field(editor,"uiScale");
        double x=(rect.getX()+9)*scale,y=(rect.getY()+9)*scale,tx=target.area().getX()+target.area().getWidth()/2.,ty=target.area().getY()+target.area().getHeight()/2.;
        editor.mouseClicked(x,y,0);editor.mouseDragged(tx,ty,0,tx-x,ty-y);editor.mouseReleased(tx,ty,0);
    }
    private static void displayed(RecipeEditorScreen editor,RecipePreview.Role role,ItemStack expected)throws Exception{
        var layout=(RecipePreview.Layout)field(editor,"layout");
        check(layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==role).flatMap(RecipePreview.Slot::getAllIngredients)
            .anyMatch(v->v.getIngredient() instanceof ItemStack stack&&ItemStack.isSameItemSameComponents(stack,expected)),"Native "+role+" preview lost components");
    }
    private static void apply(RecipeEditorScreen editor){
        editor.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(b->b.getMessage().equals(Component.translatable("vjrm.apply"))).findFirst().orElseThrow().onPress();
    }
    private static void select(RecipeEditorScreen editor,RecipeEditorScreen.DropTarget target)throws Exception{
        double x=target.area().getX()+target.area().getWidth()/2.,y=target.area().getY()+target.area().getHeight()/2.;
        editor.mouseClicked(x,y,0);editor.mouseReleased(x,y,0);
        check(((RecipeDocument.Field)field(editor,"selected")).label().equals(target.field().label()),"Slot selection missed "+target.field().label());
    }
    static void run()throws Exception{
        var mc=Minecraft.getInstance();var registries=mc.level.registryAccess();var savedScreen=mc.screen;var saved=mc.player.getInventory().getItem(0).copy();
        var source=ComponentIngredientChecks.sample(registries,2);source.set(DataComponents.CUSTOM_NAME,Component.literal("Component regression"));
        try{
            mc.player.getInventory().setItem(0,source.copy());
            var doc=RecipeDocument.template("crafting_shapeless");var editor=new RecipeEditorScreen(doc,null,"vjrm:components",savedScreen);mc.setScreen(editor);
            drag(editor,target(editor,"ingredients/0",source));
            ComponentIngredientChecks.preserved(doc,ComponentIngredientChecks.input(doc),source,registries);displayed(editor,RecipePreview.Role.INPUT,source);
            drag(editor,target(editor,"result",source));displayed(editor,RecipePreview.Role.OUTPUT,source);
            check(ItemStack.isSameItemSameComponents(mc.player.getInventory().getItem(0),source)&&mc.player.getInventory().getItem(0).getCount()==source.getCount(),"Inventory changed during drag");
            var input=target(editor,"ingredients/0",source);select(editor,input);
            var predicate=doc.get(input.field()).deepCopy();((EditBox)field(editor,"countBox")).setValue("1");apply(editor);
            check(doc.get(input.field()).equals(predicate),"Input quantity control rebuilt predicate");
            var output=target(editor,"result",source);select(editor,output);
            var beforeInvalidCount=doc.json();
            ((EditBox)field(editor,"countBox")).setValue("3");apply(editor);
            check(doc.json().equals(beforeInvalidCount),"Oversized non-stackable output did not roll back");
            var stackable=new ItemStack(Items.DIAMOND);stackable.applyComponents(source.getComponentsPatch());
            editor.accept(output.field(),stackable);select(editor,target(editor,"result",stackable));
            ((EditBox)field(editor,"countBox")).setValue("3");apply(editor);
            check(doc.quantity(output.field())==3,"Output quantity control failed: "+field(editor,"status"));displayed(editor,RecipePreview.Role.OUTPUT,stackable);
            var icons=IngredientAnnotations.read(doc.json().toString(),doc.json(),0,java.util.List.of());
            check(icons.size()==2&&icons.stream().allMatch(i->i.ingredient() instanceof ItemStack s&&(ItemStack.isSameItemSameComponents(s,source)||ItemStack.isSameItemSameComponents(s,stackable))),"Component ingredient text icons missing or lost components");
            editor=new RecipeEditorScreen(new RecipeDocument(doc.json()),null,"vjrm:components_reopen",savedScreen);mc.setScreen(editor);
            target(editor,"ingredients/0",source);displayed(editor,RecipePreview.Role.INPUT,source);
            var empty=editor.dropTargets(source).stream().filter(t->t.field().label().startsWith("$crafting/")).findFirst().orElseThrow();drag(editor,empty);
            var reopened=(RecipeDocument)field(editor,"document");
            check(reopened.json().getAsJsonArray("ingredients").size()==2,"Empty slot did not add ingredient");
            for(var f:reopened.fields().stream().filter(f->!f.output()).toList())ComponentIngredientChecks.preserved(reopened,f,source,registries);
            input=target(editor,"ingredients/0",source);editor.accept(input.field(),new ItemStack(Items.DIAMOND_SWORD));
            check(reopened.matches(input.field(),new ItemStack(Items.DIAMOND_SWORD),registries),"Plain drop retained old component conditions");
            editor.accept(input.field(),source);input=target(editor,"ingredients/0",source);select(editor,input);
            ((EditBox)field(editor,"valueBox")).setValue("#minecraft:logs");apply(editor);
            check(reopened.matches(input.field(),new ItemStack(Items.OAK_LOG),registries)&&!reopened.get(input.field()).getAsJsonObject().has("fabric:type"),"Tag control retained old component conditions");

            // Copying a different native predicate and changing only its quantity must not tighten it.
            var subset=RecipeDocument.template("crafting_shapeless").json();
            subset.getAsJsonArray("ingredients").set(0,JsonParser.parseString("{\"fabric:type\":\"fabric:custom_data\",\"base\":{\"item\":\"minecraft:diamond_sword\"},\"nbt\":{\"probe\":1}}"));
            var subsetDoc=new RecipeDocument(subset);editor=new RecipeEditorScreen(subsetDoc,null,"vjrm:subset",savedScreen);mc.setScreen(editor);
            input=target(editor,"ingredients/0",source);select(editor,input);
            ((EditBox)field(editor,"countBox")).setValue("1");apply(editor);check(subsetDoc.json().equals(subset),"Quantity control rewrote copied custom predicate");
            Path out=Path.of("../../build/verification/components");Files.createDirectories(out);
            String viewer=RecipePreview.providerId().contains("Emi")?"emi":"jei";
            Files.writeString(out.resolve("client-"+viewer+".txt"),"PASS: "+viewer+" inventory drag/input/output components; source inventory intact; whole wrapper binding and re-opening; quantity controls; added empty slot; plain/tag replacements; JSON icons; copied predicate preservation\n");
            System.out.println("PASS component client checks: "+viewer);
        }finally{mc.player.getInventory().setItem(0,saved);mc.setScreen(savedScreen);}
    }
}
