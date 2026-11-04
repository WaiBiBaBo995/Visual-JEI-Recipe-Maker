package com.pureneko.vjrm.client;

import com.pureneko.vjrm.recipe.RecipeDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
import org.lwjgl.glfw.GLFW;

/** Public widgets and real packets exercise navigation, dialogs and editor exports. */
final class DatapackUiChecks {
    private static DatapackManagementScreen screen;
    private static RecipeEditorScreen editor;
    private static String pack,renamed,moved,recipeId,localName;
    private static int stage,frames;
    private static long until;
    private static volatile boolean reloaded;
    private static volatile Throwable reloadFailure;
    private static void reload(boolean present){
        reloaded=false;reloadFailure=null;
        var server=Minecraft.getInstance().getSingleplayerServer();
        server.execute(()->server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((ignored,error)->{
            try{
                if(error!=null)throw new AssertionError("Datapack reload failed",error);
                check(server.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("vjrm:source_check")).isPresent()==present,"Reload did not apply disable/restore");
            }catch(Throwable ex){reloadFailure=ex;}finally{reloaded=true;}
        }));
    }
    static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    private static void press(Screen current,String key){
        current.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().equals(Component.translatable(key))).findFirst().orElseThrow(()->new AssertionError("Missing button "+key)).onPress();
    }
    private static Button findRow(String query)throws Exception{
        ((EditBox)field(screen,"searchBox")).setValue(query);
        @SuppressWarnings("unchecked") var rows=(List<Button>)field(screen,"rows");
        return rows.stream().filter(b->b.visible&&b.getMessage().getString().contains(query)).findFirst().orElseThrow(()->new AssertionError("Missing row "+query));
    }
    private static void openPack(String name)throws Exception{
        Button row=findRow(name);double x=row.getX()+5,y=row.getY()+5;
        screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
        check(field(screen,"view").toString().equals("PACKS"),"Single click opened pack");
        screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
        check(field(screen,"view").toString().equals("ENTRIES"),"Double click did not open pack");
    }
    private static void dialogName(String value){
        check(Minecraft.getInstance().screen instanceof DatapackNameScreen,"Expected name dialog");
        var current=Minecraft.getInstance().screen;
        current.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().setValue(value);
    }
    private static boolean saved()throws Exception{
        Component status=(Component)field(editor,"status");
        if(status.getString().startsWith(Component.translatable("vjrm.error.invalid","").getString().strip()))throw new AssertionError(status.getString());
        return status.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents&&Set.of("vjrm.exported","vjrm.manage.reload_hint","vjrm.manage.priority_failed").contains(contents.getKey());
    }
    static boolean frame(Path output)throws Exception{
        var mc=Minecraft.getInstance();
        if(editor==null){
            pack="ui_flow_"+System.nanoTime();renamed=pack+"_renamed";moved=pack+"_moved";recipeId="vjrm:"+pack;localName=pack+"_local";
            DatapackManagementClient.local().create(localName);
            editor=new RecipeEditorScreen(RecipeDocument.template("smelting"),null,recipeId,mc.screen).sourceRecipe("vjrm:source_check");
            mc.setScreen(editor);until=System.currentTimeMillis()+90000;return false;
        }
        if(System.currentTimeMillis()>until)throw new AssertionError("Datapack UI timed out at "+stage+" screen="+mc.screen);
        if(mc.screen instanceof DatapackManagementScreen manager&&(boolean)field(manager,"busy"))return false;
        if(mc.screen instanceof DatapackNameScreen dialog&&(boolean)field(dialog,"busy"))return false;
        if(stage==0){
            var box=(DatapackNameBox)field(editor,"packBox");check(box.getValue().equals("vjrm_generated"),"First editor has wrong default pack");
            box.setValue("");press(editor,"vjrm.export.server");check(mc.screen==editor&&box.isFocused(),"Empty pack did not retain and focus editor");
            check(((Component)field(editor,"status")).equals(Component.translatable("vjrm.manage.name_required")),"Missing empty name prompt");
            box.setValue(pack);press(editor,"vjrm.export.server");stage=1;
        }else if(stage==1&&saved()){
            // Reopening refreshes both the local and server completion catalogs.
            mc.setScreen(new Screen(Component.empty()){});mc.setScreen(editor);stage=2;
        }else if(stage==2){
            var box=(DatapackNameBox)field(editor,"packBox");
            @SuppressWarnings("unchecked") var names=(SortedMap<String,Integer>)field(box,"names");if(!names.containsKey(pack))return false;
            check(names.containsKey(localName),"Local completion missing");
            editor.setFocused(box);box.setValue(pack);check(editor.keyPressed(GLFW.GLFW_KEY_DOWN,0,0),"Down did not select completion");
            check(editor.keyPressed(GLFW.GLFW_KEY_TAB,0,0)&&Set.of(pack,localName).contains(box.getValue()),"Tab did not accept completion");
            box.setValue("ui_");check(editor.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0)&&mc.screen==editor,"Esc on completion closed editor");
            box.setValue(pack);frames=0;stage=201;
        }else if(stage==201){
            if(++frames<10)return false;
            Files.createDirectories(output);try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve("datapack-name-completion.png"));}
            var box=(DatapackNameBox)field(editor,"packBox");float scale=(float)field(editor,"uiScale");
            double x=((int)field(box,"popupX")+5)*scale,y=((int)field(box,"popupY")+5)*scale;
            editor.mouseClicked(x,y,0);editor.mouseReleased(x,y,0);
            check(box.getValue().equals(pack)&&!box.showing(),"Mouse did not accept the completion popup");
            press(editor,"vjrm.manage.open");screen=(DatapackManagementScreen)mc.screen;stage=3;
        }else if(stage==3){
            check(screen.children().stream().filter(EditBox.class::isInstance).count()==1,"Manager still has a name field");
            for(String key:List.of("create","open","recipes","choose"))check(screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).noneMatch(b->b.getMessage().equals(Component.translatable("vjrm.manage."+key))),"Removed manager button remains: "+key);
            openPack(pack);stage=4;
        }else if(stage==4){
            Button row=findRow(recipeId);double x=row.getX()+5,y=row.getY()+5;
            screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
            check(mc.screen==screen,"Single click opened recipe editor");
            screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);stage=5;
        }else if(stage==5){
            if(!(mc.screen instanceof RecipeEditorScreen current))return false;
            check(field(current,"targetPack").equals(pack),"Managed edit lost target pack");
            check(field(current,"recipeId").equals(recipeId)&&(boolean)field(current,"overwrite"),"Double-click edit lost ID/overwrite mode");
            check(field(current,"parent")==screen,"Double-click edit lost parent manager");
            current.onClose();stage=6;
        }else if(stage==6){press(screen,"vjrm.manage.export");stage=7;
        }else if(stage==7){
            Path zip=DatapackManagementClient.exportRoot().resolve("exports").resolve(pack+".zip");check(Files.isRegularFile(zip),"ZIP missing");
            try(var file=new java.util.zip.ZipFile(zip.toFile())){check(file.getEntry("data/vjrm/recipes/"+pack+".json")!=null,"Downloaded ZIP lacks recipe");}
            press(screen,"vjrm.manage.back");check(mc.screen==screen&&field(screen,"view").toString().equals("PACKS"),"Back from entries left manager");stage=8;
        }else if(stage==8){
            findRow(pack).onPress();press(screen,"vjrm.manage.rename");
            check(((EditBox)field(mc.screen,"input")).getValue().equals(pack),"Rename dialog did not prefill name");
            dialogName("../bad");press(mc.screen,"gui.ok");check(mc.screen instanceof DatapackNameScreen&&((EditBox)field(mc.screen,"input")).getValue().equals("../bad"),"Invalid name closed dialog/lost input");
            dialogName(renamed);press(mc.screen,"gui.ok");stage=9;
        }else if(stage==9){
            if(mc.screen instanceof DatapackNameScreen)return false;openPack(renamed);stage=10;
        }else if(stage==10){findRow(recipeId).onPress();press(screen,"vjrm.manage.move");check(((EditBox)field(mc.screen,"input")).getValue().isEmpty(),"Move dialog must start empty");stage=11;
        }else if(stage==11){
            if(!(boolean)field(mc.screen,"loaded"))return false;
            dialogName(moved);frames=0;stage=111;
        }else if(stage==111){
            if(++frames<10)return false;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve("datapack-move-dialog.png"));}
            press(mc.screen,"gui.ok");stage=12;
        }else if(stage==12){
            if(mc.screen instanceof DatapackNameScreen)return false;
            check(((List<?>)field(screen,"visible")).isEmpty(),"Moved recipe remains in old pack");
            screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);check(mc.screen==screen&&field(screen,"view").toString().equals("PACKS"),"Esc did not return to packs");stage=13;
        }else if(stage==13){openPack(moved);stage=14;
        }else if(stage==14){
            if(++frames<10)return false;
            Files.createDirectories(output);try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve("datapack-manager.png"));}
            findRow(recipeId);screen.onClose();stage=15;
        }else if(stage==15){
            findRow(moved).onPress();press(screen,"vjrm.manage.delete_pack");check(mc.screen instanceof ConfirmScreen,"No delete confirmation");press(mc.screen,"gui.yes");stage=16;
        }else if(stage==16){press(screen,"vjrm.manage.backups");stage=17;
        }else if(stage==17){
            findRow(moved).onPress();press(screen,"vjrm.manage.restore");
            check(((EditBox)field(mc.screen,"input")).getValue().equals(moved),"Restore did not prefill original name");
            dialogName(renamed);press(mc.screen,"gui.ok");stage=18;
        }else if(stage==18){
            if(!(mc.screen instanceof DatapackNameScreen))throw new AssertionError("Restore overwrote existing pack");
            if(((Component)field(mc.screen,"error")).getString().isEmpty())return false;
            check(((EditBox)field(mc.screen,"input")).getValue().equals(renamed),"Conflict lost input");
            dialogName(moved);press(mc.screen,"gui.ok");stage=19;
        }else if(stage==19){
            if(mc.screen instanceof DatapackNameScreen)return false;
            screen.onClose();stage=20;
        }else if(stage==20){
            findRow(moved);screen.onClose();check(mc.screen==editor,"Pack list Back did not return to editor");
            ((EditBox)field(editor,"packBox")).setValue(moved);press(editor,"vjrm.manage.disable");
            check(mc.screen instanceof ConfirmScreen,"Disable did not go directly to confirmation");press(mc.screen,"gui.no");check(mc.screen==editor,"Cancel disable did not return editor");
            check(((EditBox)field(editor,"packBox")).getValue().equals(moved),"Dialog lost pack input");press(editor,"vjrm.manage.disable");frames=0;stage=211;
        }else if(stage==211){
            if(++frames<10)return false;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve("datapack-disable-confirm.png"));}
            press(mc.screen,"gui.yes");stage=21;
        }else if(stage==21&&saved()){
            reload(false);stage=212;
        }else if(stage==212&&reloaded){
            if(reloadFailure!=null)throw new AssertionError(reloadFailure);
            press(editor,"vjrm.manage.open");screen=(DatapackManagementScreen)mc.screen;stage=22;
        }else if(stage==22){openPack(moved);stage=23;
        }else if(stage==23){
            Button rule=findRow("vjrm:source_check");check(rule.getMessage().getString().contains(Component.translatable("vjrm.manage.kind.disabled").getString()),"Direct disable did not write original source ID");
            rule.onPress();rule.onPress();
            check(mc.screen==screen&&((Component)field(screen,"status")).equals(Component.translatable("vjrm.manage.restore_first")),"Double click silently restored/opened disabled recipe");
            press(screen,"vjrm.manage.remove");press(mc.screen,"gui.yes");stage=24;
        }else if(stage==24){
            check(((List<?>)field(screen,"visible")).isEmpty(),"Restore left disable rule");reload(true);stage=241;
        }else if(stage==241&&reloaded){
            if(reloadFailure!=null)throw new AssertionError(reloadFailure);
            screen.onClose();stage=25;
        }else if(stage==25){findRow(moved).onPress();press(screen,"vjrm.manage.delete_pack");press(mc.screen,"gui.yes");stage=26;
        }else if(stage==26){
            var local=DatapackManagementClient.local();local.backup(localName);for(var backup:local.backups())if(backup.name().startsWith(localName+"_"))local.purge(backup.name());
            System.out.println("PASS datapack UI: editor auto-creates export pack, prefix completions/Tab/Esc, double-click navigation, rename/move/restore dialogs and validation, direct original-ID disable, ZIP download, deletion and backup restore");return true;
        }
        return false;
    }
}
