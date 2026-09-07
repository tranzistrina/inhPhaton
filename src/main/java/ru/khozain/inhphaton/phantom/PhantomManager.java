package ru.khozain.inhphaton.phantom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.khozain.inhphaton.InhPhatonPlugin;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhantomManager {
    private final InhPhatonPlugin plugin;
    private final Map<UUID,PhantomInstance> phantoms=new ConcurrentHashMap<>();
    private final Map<UUID,Set<UUID>> byObserver=new ConcurrentHashMap<>();
    public PhantomManager(InhPhatonPlugin plugin){this.plugin=plugin;}
    public @Nullable PhantomInstance create(@NotNull PhantomSpec spec){if(!canCreate(spec))return null;PhantomInstance inst=new PhantomInstance(plugin,spec);phantoms.put(spec.getId(),inst);if(plugin.config().isLogEvents())plugin.getLogger().info(String.format("[phantom] created id=%s type=%s owner=%s loc=%s,%s,%s ttl=%ds",spec.getId(),spec.getEntityType(),spec.getOwnerId(),spec.getInitialLocation().getBlockX(),spec.getInitialLocation().getBlockY(),spec.getInitialLocation().getBlockZ(),spec.getTtlSeconds()));return inst;}
    public boolean canCreate(@NotNull PhantomSpec spec){if(phantoms.size()>=plugin.config().getGlobalPhantomLimit())return false;UUID owner=spec.getOwnerId();if(owner!=null){int n=0;for(PhantomInstance p:phantoms.values())if(owner.equals(p.spec().getOwnerId()))n++;if(n>=plugin.config().getMaxPhantomsPerPlayer())return false;}return true;}
    public @Nullable PhantomInstance get(UUID id){return phantoms.get(id);}
    public List<PhantomInstance> all(){return List.copyOf(phantoms.values());}
    public boolean remove(@NotNull UUID id){PhantomInstance inst=phantoms.remove(id);if(inst==null)return false;for(UUID obs:new LinkedHashSet<>(inst.observers())){Set<UUID> set=byObserver.get(obs);if(set!=null){set.remove(id);if(set.isEmpty())byObserver.remove(obs);}}inst.remove();if(plugin.config().isLogEvents())plugin.getLogger().info("[phantom] removed id="+id);return true;}
    public void showTo(@NotNull UUID id,@NotNull UUID playerId){PhantomInstance inst=phantoms.get(id);if(inst==null)return;if(inst.observers().add(playerId)){byObserver.computeIfAbsent(playerId,k->ConcurrentHashMap.newKeySet()).add(id);Player p=Bukkit.getPlayer(playerId);if(p!=null)sendSpawnTo(inst,p);}}
    public void hideFrom(@NotNull UUID id,@NotNull UUID playerId){PhantomInstance inst=phantoms.get(id);if(inst==null)return;if(inst.observers().remove(playerId)){Set<UUID> set=byObserver.get(playerId);if(set!=null){set.remove(id);if(set.isEmpty())byObserver.remove(playerId);}Player p=Bukkit.getPlayer(playerId);if(p!=null)sendDespawnTo(inst,p);}}
    public void move(@NotNull UUID id,@NotNull Location newLoc){PhantomInstance inst=phantoms.get(id);if(inst!=null)inst.teleport(newLoc);}
    public int clearForPlayer(@NotNull UUID playerId,boolean destroy){Set<UUID> ids=byObserver.remove(playerId);if(ids==null||ids.isEmpty())return 0;int n=0;for(UUID pid:new LinkedHashSet<>(ids)){PhantomInstance inst=phantoms.get(pid);if(inst==null)continue;inst.removeObserver(playerId);if(destroy){phantoms.remove(pid);inst.remove();n++;}}return n;}
    private void sendSpawnTo(PhantomInstance inst,Player viewer){Entity backing=inst.getBackingEntity();if(backing!=null){viewer.showEntity(plugin,backing);return;}ru.khozain.inhphaton.compat.PacketAbstraction.sendSpawn(viewer,inst.id());}
    private void sendDespawnTo(PhantomInstance inst,Player viewer){Entity backing=inst.getBackingEntity();if(backing!=null){viewer.hideEntity(plugin,backing);return;}ru.khozain.inhphaton.compat.PacketAbstraction.sendEntityDestroy(viewer,inst.id());}
    public void shutdown(){for(PhantomInstance inst:new LinkedHashSet<>(phantoms.values()))inst.remove();phantoms.clear();byObserver.clear();}
    public void onConfigReload(boolean preserve){if(!preserve)shutdown();}
    public int cleanupByTtl(){long now=System.currentTimeMillis();int n=0;for(UUID id:new LinkedHashSet<>(phantoms.keySet())){PhantomInstance inst=phantoms.get(id);if(inst!=null&&inst.spec().isExpired(now)){remove(id);n++;}}return n;}
    public Set<UUID> observersOf(UUID phantomId){PhantomInstance inst=phantoms.get(phantomId);return inst==null?Collections.emptySet():Collections.unmodifiableSet(inst.observers());}
    public @Nullable World worldOf(UUID phantomId){PhantomInstance inst=phantoms.get(phantomId);return inst==null?null:inst.spec().getInitialLocation().getWorld();}
    public boolean exists(UUID phantomId){return phantoms.containsKey(phantomId);}
    public int size(){return phantoms.size();}
}