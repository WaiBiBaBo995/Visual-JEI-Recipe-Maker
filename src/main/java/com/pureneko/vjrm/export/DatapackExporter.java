package com.pureneko.vjrm.export;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.file.*;

public final class DatapackExporter {
    private DatapackExporter() {}
    public static Path export(Path root, String namespace, String path, JsonObject recipe, boolean overwrite) throws IOException {
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9/_.-]+")) throw new IOException("Invalid recipe id");
        root = root.toAbsolutePath().normalize();
        Path file = root.resolve("data").resolve(namespace).resolve("recipe").resolve(path + ".json").normalize();
        if (!file.startsWith(root.toAbsolutePath().normalize())) throw new IOException("Path escapes datapack");
        if (Files.exists(file) && !overwrite) throw new FileAlreadyExistsException(file.toString());
        Files.createDirectories(file.getParent());
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(recipe), overwrite ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING} : new StandardOpenOption[]{StandardOpenOption.CREATE_NEW});
        return file;
    }
    public static void writePackMeta(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"VJRM generated recipes\",\"pack_format\":48}}", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    public static Path serverDatapackRoot(MinecraftServer server, String name) { return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("datapacks").resolve(name); }
}
