package com.pureneko.vjrm;

import com.pureneko.vjrm.recipe.RecipeAdapterRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Vjrm.MOD_ID)
public final class Vjrm {
    public static final String MOD_ID = "vjrm";
    public Vjrm(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, com.pureneko.vjrm.network.RecipeNetwork.SPEC);
        RecipeAdapterRegistry.bootstrap();
    }
}
