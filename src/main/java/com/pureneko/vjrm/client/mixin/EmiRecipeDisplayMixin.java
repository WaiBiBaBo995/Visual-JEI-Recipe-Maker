package com.pureneko.vjrm.client.mixin;

import com.pureneko.vjrm.client.VjrmEmiPlugin;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="dev.emi.emi.screen.RecipeDisplay",remap=false)
public abstract class EmiRecipeDisplayMixin {
    @Inject(method="getWidgets",at=@At("RETURN"),remap=false)
    private void vjrm$copyButton(int x,int y,int width,int height,CallbackInfoReturnable<WidgetGroup> callback){
        VjrmEmiPlugin.addCopyButton(callback.getReturnValue());
    }
}
