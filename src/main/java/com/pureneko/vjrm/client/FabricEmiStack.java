package com.pureneko.vjrm.client;
import com.pureneko.vjrm.platform.FluidStack;
import dev.emi.emi.api.stack.EmiStack;
final class FabricEmiStack {static EmiStack of(FluidStack fluid){return EmiStack.of(fluid.getFluid(),fluid.getTag(),fluid.getAmount());}}
