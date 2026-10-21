package com.pureneko.vjrm.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import java.util.*;

public final class RecipeAdapterRegistry {
    private static final List<RecipeAdapter<?>> ADAPTERS = new ArrayList<>();
    private RecipeAdapterRegistry() {}
    public static void bootstrap() { }
    public static void register(RecipeAdapter<?> adapter) { ADAPTERS.add(adapter); }
    public static List<RecipeAdapter<?>> all() { return List.copyOf(ADAPTERS); }
    public static Optional<RecipeAdapter<?>> find(Recipe<?> recipe) { return ADAPTERS.stream().filter(a -> a.supports(recipe)).findFirst(); }
    public static Optional<RecipeAdapter<?>> byType(Identifier type) { return ADAPTERS.stream().filter(a -> a.type().equals(type)).findFirst(); }
}
