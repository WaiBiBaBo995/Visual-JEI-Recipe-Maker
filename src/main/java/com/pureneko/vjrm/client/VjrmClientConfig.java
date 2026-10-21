package com.pureneko.vjrm.client;
import com.pureneko.vjrm.platform.ConfigValue;
public final class VjrmClientConfig {
    public static final ConfigValue<Boolean> COPY_BUTTON=new ConfigValue<>("vjrm-client.json","showJeiCopyButton",false);
    public static final Settings SPEC=new Settings();
    public static final class Settings {public void save(){COPY_BUTTON.save();}}
    public static void load(){COPY_BUTTON.load();}
    public static boolean copyButton(){return COPY_BUTTON.get();}
    public static void toggle(){boolean show=!copyButton();COPY_BUTTON.set(show);SPEC.save();}
}
