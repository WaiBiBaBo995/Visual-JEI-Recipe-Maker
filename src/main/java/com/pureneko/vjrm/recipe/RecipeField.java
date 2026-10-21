package com.pureneko.vjrm.recipe;

import net.minecraft.resources.Identifier;

public record RecipeField(String name, FieldKind kind, int slot, boolean output) {
    public enum FieldKind { ITEM, FLUID, TEXT, NUMBER, JSON }
    public Identifier key() { return Identifier.fromNamespaceAndPath("vjrm", name); }
}
