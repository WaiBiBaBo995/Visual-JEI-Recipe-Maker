package com.pureneko.vjrm.platform;
import com.google.gson.*;
import java.nio.file.*;
/** Small loader-independent JSON setting, stored in Fabric's config directory. */
public final class ConfigValue<T> {
    private final String file,key;private final T fallback;private T value;
    public ConfigValue(String file,String key,T fallback){this.file=file;this.key=key;this.fallback=fallback;value=fallback;}
    public T get(){return value;} public void set(T value){this.value=value;}
    private Path path(){return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(file);}
    @SuppressWarnings("unchecked") public void load(){
        try{if(Files.exists(path())){var j=JsonParser.parseString(Files.readString(path())).getAsJsonObject();if(j.has(key)){
            Object v=fallback instanceof Boolean?j.get(key).getAsBoolean():j.get(key).getAsInt();
            if(v instanceof Integer i&&(i<0||i>4))throw new IllegalArgumentException("Permission level must be 0..4");value=(T)v;
        }}}catch(Exception ex){value=fallback;com.mojang.logging.LogUtils.getLogger().warn("Cannot load VJRM setting {}",key,ex);}
    }
    public void save(){try{
        var path=path();Files.createDirectories(path.getParent());JsonObject j=new JsonObject();
        if(Files.exists(path))try{j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();}catch(RuntimeException ignored){}
        if(value instanceof Boolean b)j.addProperty(key,b);else if(value instanceof Number n)j.addProperty(key,n);
        Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(j));
    }catch(Exception ex){com.mojang.logging.LogUtils.getLogger().warn("Cannot save VJRM setting {}",key,ex);}}
}
