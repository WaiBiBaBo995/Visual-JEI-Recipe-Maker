package com.pureneko.vjrm.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public final class PortClientEntrypoint implements ClientModInitializer {
    public void onInitializeClient(){if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei"))PortRawRecipeChecks.registerSync();ClientTickEvents.END_CLIENT_TICK.register(mc->{
        PortClientChecks.tick();PortClientChecks.rendered();
    });}
}
