package ru.khozain.inhphaton.phantom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
    private final PhantomPersistence persistence;

    public PhantomManager(InhPhatonPlugin plugin){this.plugin=plugin;this.persistence=new PhantomPersistence(plugin);}

    public @Nullable PhantomInstance create(@NotNull PhantomSpec spec){
        if(!canCreate(spec))return null;
        Location loc=spec.getInitialLocation();
        if(loc.getWorld()==null||!spec.getEntityType().isSpawnable()||spec.getEntityType()==org.bukkit.entity.EntityType.PLAYER)return null;
        final Entity backing;
        try{backing=loc.getWorld().spawnEntity(loc,spec.getEntityType());}
        catch(Throwable t){plugin.getLogger().warning("[phantom] entity spawn failed: "+t.getMessage());return null;}
        configure(backing,spec);
        for(Player p:Bukkit.getOnlinePlayers())p.hideEntity(plugin,backing);
        persistence.mark(backing,spec);

        PhantomInstance inst=new PhantomInstance(plugin,spec);
        inst.setBackingEntity(backing);
        phantoms.put(spec.getId(),inst);
        if(plugin.config().isLogEvents())plugin.getLogger().info(String.format(
            "[phantom] created id=%s backing=%s type=%s owner=%s loc=%s,%s,%s ttl=%ds",
            spec.getId(),backing.getUniqueId(),spec.getEntityType(),spec.getOwnerId(),
            loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),spec.getTtlSeconds()));
        return inst;
    }

    private void configure(Entity e,PhantomSpec spec){
        e.setInvulnerable(spec.isInvulnerable());
        e.setGravity(spec.hasGravity());
        e.setSilent(spec.isSilent());
        e.setGlowing(spec.isGlowing());
        if(spec.getCustomName()!=null){e.setCustomName(spec.getCustomName());e.setCustomNameVisible(spec.isCustomNameVisible());}
        if(e instanceof LivingEntity living){
            living.setAI(false);
            living.setCollidable(false);
        }
        e.setPersistent(true);
    }

    public boolean canCreate(@NotNull PhantomSpec spec){
        if(phantoms.size()>=plugin.config().getGlobalPhantomLimit())return false;
        UUID owner=spec.getOwnerId();
        if(owner!=null){
            int count=0;for(PhantomInstance p:phantoms.values())if(owner.equals(p.spec().getOwnerId()))count++;
            if(count>=plugin.config().getMaxPhantomsPerPlayer())return false;
        }
        return true;
    }
    public @Nullable PhantomInstance get(UUID id){return phantoms.get(id);}
    public List<PhantomInstance> all(){return List.copyOf(phantoms.values());}
    public boolean remove(@NotNull UUID id){
        PhantomInstance inst=phantoms.remove(id);if(inst==null)return false;
        for(UUID obs:new LinkedHashSet<>(inst.observers())){Set<UUID> set=byObserver.get(obs);if(set!=null){set.remove(id);if(set.isEmpty())byObserver.remove(obs);}}
        inst.remove();
        return true;
    }
    public void showTo(@NotNull UUID id,@NotNull UUID playerId){
        PhantomInstance inst=phantoms.get(id);if(inst==null)return;
        if(inst.observers().add(playerId)){
            byObserver.computeIfAbsent(playerId,k->ConcurrentHashMap.newKeySet()).add(id);
            Player p=Bukkit.getPlayer(playerId);Entity e=inst.getBackingEntity();
            if(p!=null&&e!=null&&!e.isDead())p.showEntity(plugin,e);
        }
    }
    public void hideFrom(@NotNull UUID id,@NotNull UUID playerId){
        PhantomInstance inst=phantoms.get(id);if(inst==null)return;
        if(inst.observers().remove(playerId)){
            Set<UUID> set=byObserver.get(playerId);if(set!=null){set.remove(id);if(set.isEmpty())byObserver.remove(playerId);}
            Player p=Bukkit.getPlayer(playerId);Entity e=inst.getBackingEntity();
            if(p!=null&&e!=null&&!e.isDead())p.hideEntity(plugin,e);
        }
    }
    public void move(@NotNull UUID id,@NotNull Location newLoc){PhantomInstance inst=phantoms.get(id);if(inst!=null)inst.teleport(newLoc);}
    public void rebindEntity(@NotNull Entity entity){
        String raw=entity.getPersistentDataContainer().get(persistence.idKey(),org.bukkit.persistence.PersistentDataType.STRING);
        if(raw==null)return;
        try{
            UUID id=UUID.fromString(raw);
            PhantomInstance inst=phantoms.get(id);
            if(inst!=null){
                inst.setBackingEntity(entity);
                for(Player p:Bukkit.getOnlinePlayers())p.hideEntity(plugin,entity);
            }
        }catch(IllegalArgumentException ignored){}
    }
    public int clearForPlayer(@NotNull UUID playerId,boolean destroy){
        Set<UUID> ids=byObserver.remove(playerId);if(ids==null||ids.isEmpty())return 0;int n=0;
        for(UUID pid:new LinkedHashSet<>(ids)){PhantomInstance inst=phantoms.get(pid);if(inst==null)continue;inst.removeObserver(playerId);if(destroy){phantoms.remove(pid);inst.remove();n++;}}
        return n;
    }
    public void shutdown(){shutdown(false);}
    public void shutdown(boolean preserve){
        if(preserve){persistence.save(all());phantoms.clear();byObserver.clear();return;}
        for(PhantomInstance inst:new LinkedHashSet<>(phantoms.values()))inst.remove();
        phantoms.clear();byObserver.clear();persistence.clear();
    }
    public void save(){persistence.save(all());}
    public int restore(){
        if(plugin.config().getAfterRestartPolicy()!=ru.khozain.inhphaton.config.PluginConfig.AfterRestartPolicy.RESTORE)return 0;
        int restored=0;
        for(PhantomPersistence.StoredPhantom stored:persistence.load()){
            if(stored.ttlSeconds()>0 && System.currentTimeMillis()-stored.createdAtMs()>stored.ttlSeconds()*1000L)continue;
            Entity backing=null;
            for(Entity e:stored.location().getWorld().getEntities()){
                String id=e.getPersistentDataContainer().get(persistence.idKey(),org.bukkit.persistence.PersistentDataType.STRING);
                if(stored.id().toString().equals(id)){backing=e;break;}
            }
            PhantomSpec spec=PhantomSpec.builder().id(stored.id()).createdAtMs(stored.createdAtMs()).entityType(stored.type()).location(stored.location()).ttlSeconds(stored.ttlSeconds()).ownerId(stored.ownerId()).build();
            PhantomInstance inst=new PhantomInstance(plugin,spec);inst.setBackingEntity(backing);
            if(backing==null){try{backing=stored.location().getWorld().spawnEntity(stored.location(),stored.type());configure(backing,spec);persistence.mark(backing,spec);inst.setBackingEntity(backing);}catch(Throwable t){continue;}}
            for(Player p:Bukkit.getOnlinePlayers())p.hideEntity(plugin,backing);
            phantoms.put(spec.getId(),inst);
            for(UUID observer:stored.observers()){inst.observers().add(observer);byObserver.computeIfAbsent(observer,k->ConcurrentHashMap.newKeySet()).add(spec.getId());Player p=Bukkit.getPlayer(observer);if(p!=null&&backing!=null&&!backing.isDead()&&p.getWorld()==backing.getWorld()&&p.getLocation().distanceSquared(backing.getLocation())<=plugin.config().getLoadRadius()*plugin.config().getLoadRadius())p.showEntity(plugin,backing);}
            restored++;
        }
        persistence.clear();
        if(restored>0)plugin.getLogger().info("[phantom] restored "+restored+" phantom(s)");
        return restored;
    }
    public void persistForReload(){persistence.save(all());}
    public void onConfigReload(boolean preserve){if(!preserve)shutdown();}
    public int cleanupByTtl(){long now=System.currentTimeMillis();int n=0;for(UUID id:new LinkedHashSet<>(phantoms.keySet())){PhantomInstance inst=phantoms.get(id);if(inst!=null&&inst.spec().isExpired(now)){remove(id);n++;}}return n;}
    public Set<UUID> observersOf(UUID phantomId){PhantomInstance inst=phantoms.get(phantomId);return inst==null?Collections.emptySet():Collections.unmodifiableSet(inst.observers());}
    public @Nullable World worldOf(UUID phantomId){PhantomInstance inst=phantoms.get(phantomId);return inst==null?null:inst.currentLocation().getWorld();}
    public boolean exists(UUID phantomId){return phantoms.containsKey(phantomId);}
    public int size(){return phantoms.size();}
}