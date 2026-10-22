package com.pureneko.vjrm.client.mixin;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.*;
import java.util.*;

/** Retain the tooltip overload targeted by GTCEu while using current JEI's List API. */
@Pseudo
@Mixin(targets="mezz.jei.forge.platform.FluidHelper",remap=false,priority=2000)
public abstract class JeiFluidHelperMixin {
    @Shadow(remap=false) public abstract void getTooltip(List<Component> tooltip,FluidStack ingredient,TooltipFlag flag);
    public void getTooltip(ITooltipBuilder tooltip,FluidStack ingredient,TooltipFlag flag){
        var lines=new ArrayList<Component>();getTooltip(lines,ingredient,flag);tooltip.addAll(lines);
    }
}
