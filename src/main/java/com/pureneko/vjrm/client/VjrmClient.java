package com.pureneko.vjrm.client;
import net.fabricmc.api.ClientModInitializer;
public final class VjrmClient implements ClientModInitializer {
    public void onInitializeClient(){VjrmClientConfig.load();VjrmClientConfig.SPEC.save();VjrmKeybinds.register();ClientNetworking.register();VjrmClientEvents.register();RecipeIdentities.register();}
}
