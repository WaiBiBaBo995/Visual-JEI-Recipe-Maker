package com.pureneko.vjrm.client;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VjrmClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue COPY_BUTTON=BUILDER.define("showJeiCopyButton",false);
    public static final ForgeConfigSpec.BooleanValue EMI_COPY_BUTTON=BUILDER.define("showEmiCopyButton",true);
    public static final ForgeConfigSpec SPEC=BUILDER.build();
    public static boolean copyButton(){return net.minecraftforge.fml.ModList.get().isLoaded("emi")?EMI_COPY_BUTTON.get():COPY_BUTTON.get();}
    public static void toggle(){boolean show=!copyButton();COPY_BUTTON.set(show);EMI_COPY_BUTTON.set(show);SPEC.save();}
}
