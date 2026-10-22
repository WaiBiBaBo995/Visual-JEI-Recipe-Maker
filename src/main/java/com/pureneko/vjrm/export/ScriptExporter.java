package com.pureneko.vjrm.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** All paths are relative to the client/server installation, never to a world/datapack. */
public final class ScriptExporter {
    public static Path export(Path gameRoot,String namespace,String path,String script,boolean overwrite)throws IOException{
        if(!namespace.matches("[a-z0-9_.-]+")||!path.matches("[a-z0-9_./-]+"))throw new IOException("vjrm.error.path");
        for(String segment:(namespace+"/"+path).split("/",-1))if(segment.isEmpty()||segment.endsWith(".")||segment.equals(".")||segment.equals("..")||segment.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\\..*)?"))throw new IOException("vjrm.error.path");
        Path root=gameRoot.toAbsolutePath().normalize().resolve("kubejs/server_scripts/vjrm"),file=root.resolve(namespace).resolve(path+".js").normalize();
        if(!file.startsWith(root))throw new IOException("vjrm.error.path");
        // Refuse symlink escapes in existing directory components.
        for(Path parent=file.getParent();parent!=null;parent=parent.getParent())if(Files.isSymbolicLink(parent))throw new IOException("vjrm.error.path");
        if(Files.isSymbolicLink(file))throw new IOException("vjrm.error.path");
        Files.createDirectories(file.getParent());
        if(overwrite)Files.writeString(file,script,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        else Files.writeString(file,script,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        return file;
    }
    private ScriptExporter(){}
}
