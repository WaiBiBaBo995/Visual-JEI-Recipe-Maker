package com.pureneko.vjrm.recipe;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class RecipeEditorState {
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<String, String> text = new HashMap<>();
    private JsonObject extra = new JsonObject();
    public void setItem(int slot, ItemStack stack) { items.put(slot, stack.copy()); }
    public ItemStack item(int slot) { return items.getOrDefault(slot, ItemStack.EMPTY); }
    public Map<Integer, ItemStack> items() { return Collections.unmodifiableMap(items); }
    public void putText(String key, String value) { text.put(key, value); }
    public String text(String key, String fallback) { return text.getOrDefault(key, fallback); }
    public JsonObject extra() { return extra; }
    public void extra(JsonObject value) { extra = value.deepCopy(); }
}
