package com.pureneko.vjrm;

import com.google.gson.JsonParser;
import com.pureneko.vjrm.export.*;
import net.minecraft.resources.Identifier;
import java.nio.file.*;

final class DatapackManagementChecks {
    static int run() throws Exception {
        Path base=Path.of("build/verification/managed-pack-check-"+System.nanoTime()).toAbsolutePath();
        ManagedDatapacks packs=new ManagedDatapacks(base.resolve("datapacks"),base.resolve("data/vjrm/backups"));
        packs.create("alpha");packs.create("beta");
        var id=Identifier.fromNamespaceAndPath("vjrm_check","sample/recipe");
        var recipe=com.pureneko.vjrm.recipe.RecipeDocument.template("smelting").json();
        packs.save("alpha",id,recipe,false,false,"external-pack");
        if(!packs.entries("alpha").getFirst().kind().equals("override"))throw new AssertionError("External override was not recorded");
        try{packs.save("beta",id,recipe,false,false,"");throw new AssertionError("Cross-pack recipe conflict was accepted");}
        catch(ManagedDatapacks.Conflict conflict){if(!conflict.pack.equals("alpha"))throw conflict;}
        packs.move("alpha","beta",id);
        if(!packs.entries("alpha").isEmpty()||!packs.entries("beta").getFirst().source().equals("external-pack"))throw new AssertionError("Moving a recipe lost its source marker");
        packs.save("beta",id,ManagedDatapacks.disabledJson(),true,false,"");
        if(!packs.entries("beta").getFirst().kind().equals("disabled"))throw new AssertionError("Disable condition was not written");
        packs.remove("beta",id);
        if(!packs.entries("beta").isEmpty())throw new AssertionError("Removing a disable condition failed");
        Path zip=packs.zip("beta");if(!Files.isRegularFile(zip)||Files.size(zip)==0)throw new AssertionError("Datapack ZIP was not created");
        packs.backup("beta");String backup=packs.backups().getFirst().name();
        packs.restore(backup,"restored");if(!packs.entries("restored").isEmpty())throw new AssertionError("Backup restore failed");
        packs.backup("restored");packs.cleanup(0);if(!packs.backups().isEmpty())throw new AssertionError("Zero-day retention did not purge backups");
        packs.backup("alpha");String retained=packs.backups().getFirst().name();
        packs.cleanup(-1);if(packs.backups().isEmpty())throw new AssertionError("Permanent retention removed a backup");
        packs.cleanup(7);if(packs.backups().isEmpty())throw new AssertionError("Recent backup expired too early");
        Files.setLastModifiedTime(base.resolve("data/vjrm/backups").resolve(retained),java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()-8L*86400000));
        packs.cleanup(7);if(!packs.backups().isEmpty())throw new AssertionError("Expired backup was not removed");
        try{packs.create("../outside");throw new AssertionError("Traversal accepted");}catch(java.io.IOException expected){}
        Path foreign=base.resolve("datapacks/foreign");Files.createDirectories(foreign);Files.writeString(foreign.resolve("pack.mcmeta"),"{}");
        try{packs.save("foreign",id,recipe,true,false,"");throw new AssertionError("Foreign pack was modified");}catch(java.io.IOException expected){}
        try{packs.backup("foreign");throw new AssertionError("Foreign pack was deleted");}catch(java.io.IOException expected){}
        if(!Files.exists(foreign.resolve("pack.mcmeta")))throw new AssertionError("Foreign data was lost");
        System.out.println("PASS managed datapacks: conflicts, source metadata, moves, disable/restore, ZIP, backups, retention and ownership boundaries");
        return 13;
    }
    private DatapackManagementChecks(){}
}
