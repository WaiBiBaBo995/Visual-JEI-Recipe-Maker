package com.pureneko.vjrm.client;

import com.pureneko.vjrm.IngredientDataChecks;
import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import static com.pureneko.vjrm.client.EditorFeatureChecks.*;

final class IngredientDataClientChecks {
    private static RecipeEditorScreen.DropTarget target(RecipeEditorScreen editor,String path,ItemStack stack){return editor.dropTargets(stack).stream().filter(t->t.field().label().equals(path)).findFirst().orElseThrow(()->new AssertionError("Missing ingredient binding: "+path));}
    private static void drag(RecipeEditorScreen editor,RecipeEditorScreen.DropTarget target)throws Exception{
        var method=RecipeEditorScreen.class.getDeclaredMethod("inventoryRect",int.class);method.setAccessible(true);var rect=(Rect2i)method.invoke(editor,0);float scale=(float)field(editor,"uiScale");
        double x=(rect.getX()+9)*scale,y=(rect.getY()+9)*scale,tx=target.area().getX()+target.area().getWidth()/2.,ty=target.area().getY()+target.area().getHeight()/2.;
        editor.mouseClicked(x,y,0);editor.mouseDragged(tx,ty,0,tx-x,ty-y);editor.mouseReleased(tx,ty,0);
    }
    private static void select(RecipeEditorScreen editor,RecipeEditorScreen.DropTarget target)throws Exception{
        double x=target.area().getX()+target.area().getWidth()/2.,y=target.area().getY()+target.area().getHeight()/2.;editor.mouseClicked(x,y,0);editor.mouseReleased(x,y,0);
        check(((RecipeDocument.Field)field(editor,"selected")).label().equals(target.field().label()),"Slot selection failed");
    }
    private static void apply(RecipeEditorScreen editor){editor.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(Component.translatable("vjrm.apply"))).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(257,0,0));}
    static void run()throws Exception{
        var mc=Minecraft.getInstance();var registries=mc.level.registryAccess();var saved=mc.player.getInventory().getItem(0).copy();var screen=mc.screen;var source=IngredientDataChecks.sample(registries,2);
        try{
            mc.player.getInventory().setItem(0,source.copy());var doc=RecipeDocument.template("crafting_shapeless");var editor=new RecipeEditorScreen(doc,null,"vjrm:data_regression",screen);mc.setScreen(editor);
            drag(editor,target(editor,"ingredients/0",source));IngredientDataChecks.preserved(doc,IngredientDataChecks.input(doc),source,registries);
            var layout=(RecipePreview.Layout)field(editor,"layout");check(layout.getRecipeSlotsView().getSlotViews().stream().filter(s->s.getRole()==RecipePreview.Role.INPUT).flatMap(RecipePreview.Slot::getAllIngredients).anyMatch(v->v.getIngredient() instanceof ItemStack s&&ItemStack.isSameItemSameComponents(s,source)),"Native preview lost input data");
            select(editor,target(editor,"ingredients/0",source));var predicate=doc.get(IngredientDataChecks.input(doc)).deepCopy();((EditBox)field(editor,"countBox")).setValue("1");apply(editor);
            check(doc.json().getAsJsonArray("ingredients").size()==1,"Input quantity changed ingredient multiplicity");for(var f:doc.fields().stream().filter(f->!f.output()).toList())check(doc.get(f).equals(predicate),"Quantity control changed predicate");
            var stackable=new ItemStack(Items.DIAMOND);stackable.applyComponents(source.getComponentsPatch());
            editor.accept(target(editor,"result",stackable).field(),stackable);select(editor,target(editor,"result",stackable));((EditBox)field(editor,"countBox")).setValue("3");apply(editor);
            var output=doc.fields().stream().filter(RecipeDocument.Field::output).findFirst().orElseThrow();check(doc.quantity(output)==3&&ItemStack.isSameItemSameComponents((ItemStack)doc.values(output,registries).get(0),stackable),"Output quantity control lost data");
            editor=new RecipeEditorScreen(new RecipeDocument(doc.json()),null,"vjrm:data_reopen",screen);mc.setScreen(editor);doc=(RecipeDocument)field(editor,"document");
            var empty=editor.dropTargets(source).stream().filter(t->t.field().label().startsWith("$crafting/")).findFirst().orElseThrow();drag(editor,empty);
            check(doc.json().getAsJsonArray("ingredients").size()==2,"Empty slot did not add item");for(var f:doc.fields().stream().filter(f->!f.output()).toList())IngredientDataChecks.preserved(doc,f,source,registries);
            var icons=IngredientAnnotations.read(doc.json().toString(),doc.json(),0,java.util.List.of());check(icons.stream().anyMatch(icon->icon.ingredient() instanceof ItemStack stack&&ItemStack.isSameItemSameComponents(stack,source)),"Text icons lost input data");
            var input=target(editor,"ingredients/0",source);editor.accept(input.field(),new ItemStack(Items.DIAMOND_SWORD));check(doc.matches(input.field(),new ItemStack(Items.DIAMOND_SWORD),registries),"Plain drop retained data");
            editor.accept(input.field(),source);select(editor,target(editor,"ingredients/0",source));((EditBox)field(editor,"valueBox")).setValue("#minecraft:logs");apply(editor);check(doc.matches(input.field(),new ItemStack(Items.OAK_LOG),registries),"Tag control retained data");

            check(ItemStack.isSameItemSameComponents(mc.player.getInventory().getItem(0),source)&&mc.player.getInventory().getItem(0).getCount()==source.getCount(),"Inventory drag mutated source");System.out.println("PASS ingredient data client: drag, preview, quantity, reopen, empty slot, icons, replacements and inventory preservation");
        }finally{mc.player.getInventory().setItem(0,saved);mc.setScreen(screen);}
    }
}
