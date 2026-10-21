package com.pureneko.vjrm.recipe;

import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.io.IOException;
import java.util.*;

/** Reads the winning datapack resource rather than lossy client-synced recipe objects. */
public final class RecipeSource {
    public static final int MAX_JSON = 240000;
    public static Identifier resourceId(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json");
    }
    public static RecipeDocument read(MinecraftServer server, Identifier id) throws IOException {
        RecipeHolder<?> holder = server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id)).orElseThrow(() -> new IOException("vjrm.error.no_source"));
        var resource = server.getResourceManager().getResource(resourceId(id)).orElseThrow(() -> new IOException("vjrm.error.no_source"));
        try (var reader = resource.openAsReader()) {
            StringBuilder json = new StringBuilder(); char[] buffer = new char[4096]; int size;
            while ((size = reader.read(buffer)) != -1) {
                if (json.length() + size > MAX_JSON) throw new IOException("vjrm.error.source_size");
                json.append(buffer, 0, size);
            }
            var document = new RecipeDocument(JsonParser.parseString(json.toString()).getAsJsonObject());
            // Validate with the current server's registries before transmitting a draft.
            document.decode(server.registryAccess());
            return document;
        }
    }
    public record Sample(String serializer, String id) { }
    public static List<Sample> catalog(MinecraftServer server) {
        Map<String, Integer> counts = new HashMap<>(); List<Sample> samples = new ArrayList<>();
        int length = 0;
        var recipes = server.getRecipeManager().getRecipes().stream().sorted(Comparator.comparing(h -> h.id().toString())).toList();
        for (var holder : recipes) {
            String serializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer()).toString();
            if (counts.getOrDefault(serializer, 0) >= 64 || holder.id().identifier().toString().length() > 200) continue;
            if (server.getResourceManager().getResource(resourceId(holder.id().identifier())).isEmpty()) continue;
            length += serializer.length() + holder.id().identifier().toString().length();
            if (length > 150000 || samples.size() >= 4096) break;
            samples.add(new Sample(serializer, holder.id().identifier().toString())); counts.merge(serializer, 1, Integer::sum);
        }
        return List.copyOf(samples);
    }
    private RecipeSource() { }
}
