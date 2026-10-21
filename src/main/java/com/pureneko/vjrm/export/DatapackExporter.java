package com.pureneko.vjrm.export;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.file.*;

public final class DatapackExporter {
    private DatapackExporter() {}
    public static Path export(Path root, String namespace, String path, JsonObject recipe, boolean overwrite) throws IOException {
        if (!namespace.matches("[a-z0-9_.-]+") || namespace.equals(".") || namespace.equals("..") || !path.matches("[a-z0-9/_.-]+")) throw new IOException("Invalid recipe id");
        for (String segment : path.split("/", -1)) if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) throw new IOException("Invalid recipe path");
        root = root.toAbsolutePath().normalize();
        Path recipes = root.resolve("data").resolve(namespace).resolve("recipes");
        Path file = recipes.resolve(path + ".json").normalize();
        if (!file.startsWith(recipes)) throw new IOException("Path escapes recipes directory");
        if (Files.exists(file) && !overwrite) throw new FileAlreadyExistsException(file.toString());
        Files.createDirectories(file.getParent());
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(recipe), overwrite ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING} : new StandardOpenOption[]{StandardOpenOption.CREATE_NEW});
        return file;
    }
    public static void writePackMeta(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"VJRM generated recipes\",\"pack_format\":15}}", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    public static Path serverDatapackRoot(MinecraftServer server, String name) { return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("datapacks").resolve(name); }
}
