package com.pureneko.vjrm.client;

import com.pureneko.vjrm.Vjrm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Vjrm.MOD_ID, dist = Dist.CLIENT)
public final class VjrmClient {
    public VjrmClient(net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,VjrmClientConfig.SPEC);
    }
}
