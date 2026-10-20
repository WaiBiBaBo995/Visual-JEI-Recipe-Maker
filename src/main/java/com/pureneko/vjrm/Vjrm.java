package com.pureneko.vjrm;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
@Mod(Vjrm.MOD_ID)
public final class Vjrm {
    public static final String MOD_ID="vjrm";
    public Vjrm(){
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,com.pureneko.vjrm.network.RecipeNetwork.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,com.pureneko.vjrm.client.VjrmClientConfig.SPEC);
        com.pureneko.vjrm.network.RecipeNetwork.register();
        com.pureneko.vjrm.recipe.RecipeAdapterRegistry.bootstrap();
    }
}
