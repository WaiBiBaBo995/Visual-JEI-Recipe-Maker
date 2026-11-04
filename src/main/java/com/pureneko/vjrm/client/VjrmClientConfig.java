package com.pureneko.vjrm.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VjrmClientConfig {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue COPY_BUTTON=BUILDER.define("showJeiCopyButton",false);
    public static final ModConfigSpec.BooleanValue EMI_COPY_BUTTON=BUILDER.define("showEmiCopyButton",true);
    public static final ModConfigSpec.IntValue BACKUP_RETENTION_DAYS=BUILDER.comment("Deleted local-pack backups: 0 = clear on client exit/next world entry, positive = days to retain, -1 = keep until manually purged.").defineInRange("backupRetentionDays",0,-1,36500);
    public static final ModConfigSpec SPEC=BUILDER.build();
    public static boolean copyButton(){return net.neoforged.fml.ModList.get().isLoaded("emi")?EMI_COPY_BUTTON.get():COPY_BUTTON.get();}
    public static void toggle(){boolean show=!copyButton();COPY_BUTTON.set(show);EMI_COPY_BUTTON.set(show);SPEC.save();}
}
