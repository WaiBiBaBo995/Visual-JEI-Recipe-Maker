package com.pureneko.vjrm.client;
import com.pureneko.vjrm.platform.FluidStack;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.helpers.IJeiHelpers;
/** Keep optional JEI classes outside the common fluid representation. */
final class FabricFluidBridge {
    static Object wrap(Object value){
        if(!(value instanceof IJeiFluidIngredient ingredient))return value;
        var variant=ingredient.getFluidVariant();var fluid=new FluidStack(variant.getFluid(),(int)Math.min(Integer.MAX_VALUE,ingredient.getAmount()));fluid.applyComponents(variant.getComponentsPatch());return fluid;
    }
    static Object unwrap(Object value,IJeiHelpers helpers){
        if(!(value instanceof FluidStack fluid))return value;
        return helpers.getPlatformFluidHelper().create(fluid.getFluid().builtInRegistryHolder(),fluid.getAmount(),fluid.getComponentsPatch());
    }
}
