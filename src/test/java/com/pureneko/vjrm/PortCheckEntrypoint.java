package com.pureneko.vjrm;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class PortCheckEntrypoint implements ModInitializer {
    public void onInitialize(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        if(Boolean.getBoolean("vjrm.verifyClient"))com.pureneko.vjrm.network.RecipeNetwork.PERMISSION.set(0);
        if(Boolean.getBoolean("vjrm.verifyServer"))try{PortServerChecks.run(server);}catch(Exception ex){throw new RuntimeException(ex);}
    });}
}
