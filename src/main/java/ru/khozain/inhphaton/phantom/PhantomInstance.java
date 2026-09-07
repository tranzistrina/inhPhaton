package ru.khozain.inhphaton.phantom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.khozain.inhphaton.InhPhatonPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhantomInstance {
    private final InhPhatonPlugin plugin;
    private final PhantomSpec spec;
    private final Set<UUID> observers=ConcurrentHashMap.newKeySet();
    private final long spawnTick;
    private volatile Location currentLocation;
    @Nullable private Entity backingEntity;
    private volatile boolean removed;

    public PhantomInstance(InhPhatonPlugin plugin,PhantomSpec spec){
        this.plugin=plugin;this.spec=spec;this.spawnTick=plugin.getServer().getCurrentTick();
        this.currentLocation=spec.getInitialLocation();
    }
    public PhantomSpec spec(){return spec;}
    public UUID id(){return spec.getId();}
    public long spawnTick(){return spawnTick;}
    public Set<UUID> observers(){return observers;}
    public boolean isRemoved(){return removed;}
    public @Nullable Entity getBackingEntity(){return backingEntity;}
    public void setBackingEntity(@Nullable Entity e){backingEntity=e;}
    public Location currentLocation(){return currentLocation.clone();}

    public void addObserver(@NotNull UUID id){
        if(!observers.add(id))return;
        if(backingEntity!=null){
            Player p=plugin.getServer().getPlayer(id);
            if(p!=null)p.showEntity(plugin,backingEntity);
        }
    }
    public void removeObserver(@NotNull UUID id){
        if(!observers.remove(id))return;
        if(backingEntity!=null){
            Player p=plugin.getServer().getPlayer(id);
            if(p!=null)p.hideEntity(plugin,backingEntity);
        }
    }
    public void teleport(@NotNull Location newLoc){
        this.currentLocation=newLoc.clone();
        if(backingEntity!=null&&!backingEntity.isDead()){
            backingEntity.teleport(newLoc);
            return;
        }
        for(UUID oid:observers){
            Player p=plugin.getServer().getPlayer(oid);
            if(p!=null)ru.khozain.inhphaton.compat.PacketAbstraction.sendEntityTeleport(p,id(),newLoc);
        }
    }
    public void remove(){
        if(removed)return;
        removed=true;
        Entity entity=backingEntity;
        if(entity!=null&&!entity.isDead())entity.remove();
        backingEntity=null;
        observers.clear();
    }
}