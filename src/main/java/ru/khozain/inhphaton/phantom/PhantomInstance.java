package ru.khozain.inhphaton.phantom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.khozain.inhphaton.InhPhatonPlugin;
import ru.khozain.inhphaton.compat.PacketAbstraction;
import ru.khozain.inhphaton.visibility.VisibilityManager;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhantomInstance {
    private final InhPhatonPlugin plugin; private final PhantomSpec spec; private final Set<UUID> observers=ConcurrentHashMap.newKeySet(); private final long spawnTick;
    @Nullable private Entity backingEntity; private boolean removed;
    public PhantomInstance(InhPhatonPlugin plugin,PhantomSpec spec){this.plugin=plugin;this.spec=spec;spawnTick=plugin.getServer().getCurrentTick();}
    public PhantomSpec spec(){return spec;} public UUID id(){return spec.getId();} public long spawnTick(){return spawnTick;} public Set<UUID> observers(){return observers;}
    public boolean isRemoved(){return removed;} public @Nullable Entity getBackingEntity(){return backingEntity;} public void setBackingEntity(@Nullable Entity e){backingEntity=e;}
    public void addObserver(@NotNull UUID id){observers.add(id);VisibilityManager vm=plugin.visibility();if(backingEntity!=null){Player p=plugin.getServer().getPlayer(id);if(p!=null)p.showEntity(plugin,backingEntity);vm.showTo(backingEntity.getUniqueId(),id);}}
    public void removeObserver(@NotNull UUID id){observers.remove(id);if(backingEntity!=null){Player p=plugin.getServer().getPlayer(id);if(p!=null)p.hideEntity(plugin,backingEntity);plugin.visibility().hideFrom(backingEntity.getUniqueId(),id);}}
    public void teleport(@NotNull Location newLoc){if(backingEntity!=null&&!backingEntity.isDead()){backingEntity.teleport(newLoc);return;}for(UUID oid:observers){Player p=plugin.getServer().getPlayer(oid);if(p!=null)PacketAbstraction.sendEntityTeleport(p,spec.getId(),newLoc);}}
    public void remove(){removed=true;for(UUID oid:new LinkedHashSet<>(observers)){Player p=plugin.getServer().getPlayer(oid);if(p!=null)PacketAbstraction.sendEntityDestroy(p,spec.getId());}observers.clear();if(backingEntity!=null&&!backingEntity.isDead()){backingEntity.remove();backingEntity=null;}}
}