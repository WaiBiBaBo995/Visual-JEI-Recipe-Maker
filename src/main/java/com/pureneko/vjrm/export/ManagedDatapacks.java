package com.pureneko.vjrm.export;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

/** All paths are constrained to owned directories; upstream packs are never written. */
public final class ManagedDatapacks {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MARKER = "vjrm.json";
    public static final String DEFAULT_PACK = "vjrm_generated";
    public record Pack(String name, int recipes, int disabled) { }
    public record Entry(String id, String kind, String source) { }
    public static final class Conflict extends IOException {
        public final String pack;
        public Conflict(String pack) { super("vjrm.manage.conflict"); this.pack=pack; }
    }
    private final Path root, backups;
    public ManagedDatapacks(Path root, Path backups) throws IOException {
        this.root=root.toAbsolutePath().normalize(); this.backups=backups.toAbsolutePath().normalize();
        checkLinks(this.root); checkLinks(this.backups);
    }
    public Path root(){return root;}
    public static boolean validName(String name) {
        return name != null && name.matches("[a-z0-9_-]{1,64}") && !name.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])");
    }
    private Path path(String name) throws IOException {
        if(!validName(name))throw new IOException("vjrm.manage.invalid_name");
        Path path=root.resolve(name); checkLinks(path); return path;
    }
    private Path owned(String name) throws IOException {
        Path path=path(name);
        if(!Files.isDirectory(path)||(!Files.isRegularFile(path.resolve(MARKER)) && !legacy(path)))throw new IOException("vjrm.manage.not_owned");
        return path;
    }
    private boolean legacy(Path path) throws IOException {
        if(!path.getFileName().toString().equals(DEFAULT_PACK)||!Files.isRegularFile(path.resolve("pack.mcmeta")))return false;
        return Files.readString(path.resolve("pack.mcmeta")).contains("VJRM generated recipes");
    }
    public List<Pack> packs() throws IOException {
        if(!Files.isDirectory(root))return List.of();
        List<Pack> result=new ArrayList<>();
        try(var children=Files.list(root)){
            for(Path p:children.sorted().toList()){
                checkLinks(p);
                if(!Files.isDirectory(p)||!validName(p.getFileName().toString()))continue;
                if(Files.isRegularFile(p.resolve(MARKER))||legacy(p))result.add(info(p));
            }
        }
        return result;
    }
    private Pack info(Path path) throws IOException {
        var entries=entriesAt(path); int disabled=(int)entries.stream().filter(e->e.kind.equals("disabled")).count();
        return new Pack(path.getFileName().toString(),entries.size()-disabled,disabled);
    }
    public Pack info(String name) throws IOException {return info(owned(name));}
    public List<Entry> entries(String name) throws IOException {return entriesAt(owned(name));}
    private List<Entry> entriesAt(Path pack) throws IOException {
        List<Entry> entries=new ArrayList<>(); Path data=pack.resolve("data");
        if(!Files.isDirectory(data))return entries;
        JsonObject sources=metadata(pack).has("sources")?metadata(pack).getAsJsonObject("sources"):new JsonObject();
        try(var files=Files.walk(data)){
            for(Path file:files.sorted().toList()){
                checkLinks(file);
                if(!Files.isRegularFile(file)||!file.toString().endsWith(".json"))continue;
                Path rel=data.relativize(file);
                if(rel.getNameCount()<3||!rel.getName(1).toString().equals("recipe"))continue;
                String tail=rel.subpath(2,rel.getNameCount()).toString().replace('\\','/');
                String id=rel.getName(0)+":"+tail.substring(0,tail.length()-5);
                ResourceLocation.parse(id);
                String source=sources.has(id)?sources.get(id).getAsString():"";
                JsonObject json=read(file);
                entries.add(new Entry(id,isDisabled(json)?"disabled":source.isEmpty()?"recipe":"override",source));
            }
        }
        entries.sort(Comparator.comparing(Entry::id)); return entries;
    }
    private JsonObject metadata(Path pack) throws IOException {
        Path file=pack.resolve(MARKER); checkLinks(file);
        return Files.exists(file)?read(file):new JsonObject();
    }
    private static JsonObject read(Path file) throws IOException {
        if(Files.size(file)>1_000_000)throw new IOException("vjrm.error.source_size");
        try{return JsonParser.parseString(Files.readString(file)).getAsJsonObject();}
        catch(RuntimeException ex){throw new IOException("vjrm.manage.corrupt",ex);}
    }
    public void create(String name) throws IOException {
        Path pack=path(name);
        if(Files.exists(pack))throw new FileAlreadyExistsException(name);
        Files.createDirectories(pack); DatapackExporter.writePackMeta(pack); mark(pack,new JsonObject());
    }
    private void mark(Path pack,JsonObject meta) throws IOException {
        meta.addProperty("version",1); meta.addProperty("name",pack.getFileName().toString()); atomic(pack.resolve(MARKER),meta);
    }
    public JsonObject get(String pack,ResourceLocation id) throws IOException {return read(recipeFile(owned(pack),id));}
    public String owner(ResourceLocation id,String except) throws IOException {
        for(Pack p:packs())if(!p.name.equals(except)&&Files.exists(recipeFile(owned(p.name),id)))return p.name;
        return "";
    }
    public void save(String name,ResourceLocation id,JsonObject json,boolean overwrite,boolean move,String source) throws IOException {
        Path pack=path(name); String conflict=owner(id,name);
        if(!conflict.isEmpty()&&!move)throw new Conflict(conflict);
        if(!Files.exists(pack))create(name); else owned(name);
        Path file=recipeFile(pack,id);
        if(Files.exists(file)&&!overwrite)throw new FileAlreadyExistsException(id.toString());
        JsonObject previousMeta=metadata(pack);
        JsonObject previous=Files.exists(file)?read(file):null;
        String previousSource=metadata(pack).has("sources")&&metadata(pack).getAsJsonObject("sources").has(id.toString())?metadata(pack).getAsJsonObject("sources").get(id.toString()).getAsString():"";
        atomic(file,json);
        try{
            JsonObject meta=metadata(pack); JsonObject sources=meta.has("sources")?meta.getAsJsonObject("sources"):new JsonObject();
            if(!source.isEmpty())sources.addProperty(id.toString(),source);
            else if(!previousSource.isEmpty())sources.addProperty(id.toString(),previousSource);
            meta.add("sources",sources); mark(pack,meta);
            if(!conflict.isEmpty())remove(conflict,id);
        }catch(IOException ex){
            try{if(previous==null)Files.deleteIfExists(file);else atomic(file,previous);mark(pack,previousMeta);}
            catch(IOException rollback){ex.addSuppressed(rollback);}throw ex;
        }
    }
    public void remove(String pack,ResourceLocation id) throws IOException {
        Path folder=owned(pack),file=recipeFile(folder,id);
        if(!Files.isRegularFile(file))throw new IOException("vjrm.manage.missing");
        JsonObject meta=metadata(folder),before=meta.deepCopy();
        if(meta.has("sources"))meta.getAsJsonObject("sources").remove(id.toString());mark(folder,meta);
        try{Files.delete(file);}catch(IOException ex){try{mark(folder,before);}catch(IOException rollback){ex.addSuppressed(rollback);}throw ex;}
    }
    public void move(String from,String to,ResourceLocation id) throws IOException {
        if(from.equals(to))return;
        JsonObject json=get(from,id);
        String source=entries(from).stream().filter(e->e.id.equals(id.toString())).findFirst().orElseThrow().source;
        save(to,id,json,false,true,source);
    }
    public void rename(String from,String to) throws IOException {
        Path source=owned(from),target=path(to);
        if(Files.exists(target))throw new FileAlreadyExistsException(to);
        Files.move(source,target);mark(target,metadata(target));
    }
    public void backup(String name) throws IOException {
        Path source=owned(name); mark(source,metadata(source)); Files.createDirectories(backups);
        Path target=backups.resolve(name+"_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,8));
        Files.move(source,target);Files.setLastModifiedTime(target,FileTime.fromMillis(System.currentTimeMillis()));
    }
    public List<Pack> backups() throws IOException {
        if(!Files.isDirectory(backups))return List.of();
        List<Pack> list=new ArrayList<>();try(var files=Files.list(backups)){
            for(Path p:files.sorted().toList()){checkLinks(p);if(Files.isDirectory(p)&&Files.isRegularFile(p.resolve(MARKER)))list.add(info(p));}
        }return list;
    }
    private Path backupPath(String key) throws IOException {
        if(!key.matches("[a-z0-9_-]{1,100}"))throw new IOException("vjrm.manage.invalid_name");
        Path path=backups.resolve(key);checkLinks(path);
        if(!Files.isDirectory(path)||!Files.isRegularFile(path.resolve(MARKER)))throw new IOException("vjrm.manage.missing");return path;
    }
    public void restore(String key,String name) throws IOException {
        Path source=backupPath(key),target=path(name);if(Files.exists(target))throw new FileAlreadyExistsException(name);
        for(var entry:entriesAt(source)){String owner=owner(ResourceLocation.parse(entry.id),name);if(!owner.isEmpty())throw new Conflict(owner);}
        Files.createDirectories(root);Files.move(source,target);mark(target,metadata(target));
    }
    public void purge(String key) throws IOException {deleteTree(backupPath(key));}
    public Path zip(String name) throws IOException {
        Path folder=owned(name),target=zipPath(name);checkLinks(target);Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),".vjrm-",".zip");
        try{
            try(var output=new java.util.zip.ZipOutputStream(Files.newOutputStream(temp));var files=Files.walk(folder)){
                for(Path file:files.sorted().toList()){
                    checkLinks(file);if(!Files.isRegularFile(file))continue;
                    output.putNextEntry(new java.util.zip.ZipEntry(folder.relativize(file).toString().replace('\\','/')));
                    Files.copy(file,output);output.closeEntry();
                }
            }
            Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}return target;
    }
    public Path zipPath(String name) throws IOException {
        if(!validName(name))throw new IOException("vjrm.manage.invalid_name");
        Path path=backups.getParent().resolve("exports").resolve(name+".zip");checkLinks(path);return path;
    }
    public void cleanup(int days) throws IOException {
        if(days< -1)throw new IllegalArgumentException("days");if(days==-1||!Files.isDirectory(backups))return;
        long cutoff=System.currentTimeMillis()-days*86_400_000L;
        for(Pack p:backups()){Path path=backupPath(p.name);if(Files.getLastModifiedTime(path).toMillis()<=cutoff)deleteTree(path);}
    }
    public static JsonObject disabledJson(){
        JsonObject json=new JsonObject(),condition=new JsonObject();condition.addProperty("condition","fabric:not");
        JsonObject always=new JsonObject();always.addProperty("condition","fabric:all_mods_loaded");always.add("values",new JsonArray());condition.add("value",always);
        JsonArray conditions=new JsonArray();conditions.add(condition);json.add("fabric:load_conditions",conditions);return json;
    }
    public static boolean isDisabled(JsonObject json){
        return json.equals(disabledJson());
    }
    public static Path recipeFile(Path pack,ResourceLocation id) throws IOException {
        Path root=pack.toAbsolutePath().normalize(),file=root.resolve("data").resolve(id.getNamespace()).resolve("recipe").resolve(id.getPath()+".json").normalize();
        if(!file.startsWith(root)||id.getPath().contains(".."))throw new IOException("vjrm.manage.invalid_name");checkLinks(file);return file;
    }
    private static void atomic(Path file,JsonObject json) throws IOException {
        checkLinks(file);Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),".vjrm-",".tmp");
        try{Files.writeString(temp,JSON.toJson(json));try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temp);}
    }
    private static void checkLinks(Path file) throws IOException {
        for(Path p=file.toAbsolutePath();p!=null;p=p.getParent()){
            if(Files.isSymbolicLink(p))throw new IOException("vjrm.manage.link");
            if(Files.exists(p,LinkOption.NOFOLLOW_LINKS)&&Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS).isOther())throw new IOException("vjrm.manage.link");
        }
    }
    private static void deleteTree(Path path) throws IOException {
        try(var files=Files.walk(path)){for(Path p:files.sorted(Comparator.reverseOrder()).toList()){checkLinks(p);Files.delete(p);}}
    }
}
