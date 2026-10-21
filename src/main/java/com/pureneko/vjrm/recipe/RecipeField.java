package com.pureneko.vjrm.recipe;

import net.minecraft.resources.ResourceLocation;

public record RecipeField(String name, FieldKind kind, int slot, boolean output) {
    public enum FieldKind { ITEM, FLUID, TEXT, NUMBER, JSON }
    public ResourceLocation key() { return new ResourceLocation("vjrm", name); }
}
