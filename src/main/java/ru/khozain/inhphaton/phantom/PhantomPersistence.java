package ru.khozain.inhphaton.phantom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.khozain.inhphaton.InhPhatonPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PhantomPersistence {
    private final InhPhatonPlugin plugin;
    private final File file;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;

    public PhantomPersistence(InhPhatonPlugin plugin) {
        this.plugin=plugin;
        this.file=new File(plugin.getDataFolder(),"phantoms.yml");
        this.markerKey=new NamespacedKey(plugin,"phantom");
        this.idKey=new NamespacedKey(plugin,"phantom-id");
    }

    public NamespacedKey markerKey(){return markerKey;}
    public NamespacedKey idKey(){return idKey;}

    public void mark(@NotNull Entity entity,@NotNull PhantomSpec spec){
        PersistentDataContainer pdc=entity.getPersistentDataContainer();
        pdc.set(markerKey,PersistentDataType.BYTE,(byte)1);
        pdc.set(idKey,PersistentDataType.STRING,spec.getId().toString());
        entity.setPersistent(true);
    }

    public void save(@NotNull List<PhantomInstance> instances){
        YamlConfiguration y=new YamlConfiguration();
        y.set("version",1);
        int i=0;
        for(PhantomInstance inst:instances){
            String p="phantoms."+i++;
            PhantomSpec s=inst.spec();
            Location loc=inst.currentLocation();
            y.set(p+".id",s.getId().toString());
            y.set(p+".type",s.getEntityType().name());
            y.set(p+".world",loc.getWorld()==null?null:loc.getWorld().getName());
            y.set(p+".x",loc.getX()); y.set(p+".y",loc.getY()); y.set(p+".z",loc.getZ());
            y.set(p+".yaw",loc.getYaw()); y.set(p+".pitch",loc.getPitch());
            y.set(p+".ttl",s.getTtlSeconds()); y.set(p+".created-at",s.getCreatedAtMs());
            y.set(p+".owner",s.getOwnerId()==null?null:s.getOwnerId().toString());
            y.set(p+".observers",inst.observers().stream().map(UUID::toString).toList());
        }
        try{plugin.getDataFolder().mkdirs();y.save(file);}
        catch(IOException e){plugin.getLogger().warning("Failed to save phantoms.yml: "+e.getMessage());}
    }

    public List<StoredPhantom> load(){
        if(!file.isFile())return List.of();
        YamlConfiguration y=YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root=y.getConfigurationSection("phantoms");
        if(root==null)return List.of();
        List<StoredPhantom> out=new ArrayList<>();
        for(String key:root.getKeys(false)){
            String p="phantoms."+key;
            try{
                UUID id=UUID.fromString(y.getString(p+".id"));
                org.bukkit.entity.EntityType type=org.bukkit.entity.EntityType.valueOf(y.getString(p+".type","ZOMBIE"));
                World world=Bukkit.getWorld(y.getString(p+".world",""));
                if(world==null)continue;
                Location loc=new Location(world,y.getDouble(p+".x"),y.getDouble(p+".y"),y.getDouble(p+".z"),
                    (float)y.getDouble(p+".yaw"),(float)y.getDouble(p+".pitch"));
                long ttl=y.getLong(p+".ttl",0);
                long created=y.getLong(p+".created-at",System.currentTimeMillis());
                String ownerRaw=y.getString(p+".owner");
                UUID owner=ownerRaw==null?null:UUID.fromString(ownerRaw);
                List<UUID> observers=new ArrayList<>();
                for(String raw:y.getStringList(p+".observers"))try{observers.add(UUID.fromString(raw));}catch(IllegalArgumentException ignored){}
                out.add(new StoredPhantom(id,type,loc,ttl,created,owner,observers));
            }catch(Exception e){plugin.getLogger().warning("Skipping invalid phantom entry "+key+": "+e.getMessage());}
        }
        return out;
    }

    public void clear(){if(file.exists()&&!file.delete())plugin.getLogger().warning("Could not delete "+file);}

    public record StoredPhantom(UUID id,org.bukkit.entity.EntityType type,Location location,
                                long ttlSeconds,long createdAtMs,UUID ownerId,List<UUID> observers){}
}