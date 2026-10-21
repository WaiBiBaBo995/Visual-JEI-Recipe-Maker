package com.pureneko.vjrm.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VjrmClientConfig {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue COPY_BUTTON=BUILDER.define("showJeiCopyButton",false);
    public static final ModConfigSpec SPEC=BUILDER.build();
    public static boolean copyButton(){return COPY_BUTTON.get();}
    public static void toggle(){boolean show=!copyButton();COPY_BUTTON.set(show);SPEC.save();}
}
