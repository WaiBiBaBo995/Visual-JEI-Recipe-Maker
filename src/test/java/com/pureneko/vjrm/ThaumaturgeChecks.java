package com.pureneko.vjrm;

import com.pureneko.vjrm.recipe.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import java.nio.file.*;

/** Exercises the actual optional mod's codec and datapack registries. */
public final class ThaumaturgeChecks {
    public static void run(MinecraftServer server)throws Exception{
        if(!Boolean.getBoolean("vjrm.verifyThaum"))return;
        check(AspectSupport.available(),"AspectInstance missing");int recipes=0,aspects=0,components=0;
        for(var holder:server.getRecipeManager().getRecipes()){
            var id=holder.id().identifier();if(!id.getNamespace().equals("thaumaturge")||server.getResourceManager().getResource(RecipeSource.resourceId(id)).isEmpty())continue;
            var doc=RecipeSource.read(server,id);var original=doc.json();doc.decode(server.registryAccess());
            for(var field:doc.fields()){
                var before=doc.get(field).deepCopy();
                if(field.aspect()){
                    Object value=AspectSupport.fromId("thaumaturge:ignis",17,server.registryAccess());
                    doc.accept(field,value,17,server.registryAccess());doc.decode(server.registryAccess());
                    check(doc.matches(field,value,server.registryAccess())&&doc.quantity(field)==17,"Aspect id/amount not retained");
                    check(AspectSupport.amount(doc.values(field,server.registryAccess()).getFirst())==17,"Wrong native ingredient amount");aspects++;
                }else if(field.path().getFirst().equals("components")){
                    doc.accept(field,new ItemStack(Items.DIAMOND),1,server.registryAccess());doc.decode(server.registryAccess());components++;
                }
                doc.set(field,before);
            }
            if(original.has("components")&&original.get("components").isJsonArray())check(doc.fields().stream().filter(f->f.path().getFirst().equals("components")).count()==original.getAsJsonArray("components").size(),"Components collapsed into alternative list: "+id);
            check(doc.json().equals(original),"Unknown recipe fields changed");recipes++;
        }
        check(aspects>0&&components>0,"No real aspect/components coverage");
        var out=Path.of("../../build/verification/thaum-server");Files.createDirectories(out);Files.writeString(out.resolve("result.txt"),"PASS: "+recipes+" Thaumaturge recipes; "+aspects+" aspect replacements and amounts; "+components+" independent component replacements; codec/registry roundtrips\n");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
