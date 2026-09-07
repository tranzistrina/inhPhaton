package ru.khozain.inhphaton.internal;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.khozain.inhphaton.InhPhatonPlugin;

import java.util.UUID;

public final class ShutdownCleaner implements Listener {
    private static final Logger LOG=LoggerFactory.getLogger("inhPhaton.lifecycle");
    private final InhPhatonPlugin plugin;
    private final BukkitRunnable cleanupTask;

    public ShutdownCleaner(InhPhatonPlugin plugin){
        this.plugin=plugin;
        cleanupTask=new BukkitRunnable(){
            @Override public void run(){
                if(!plugin.config().isAutoCleanup())return;
                int removed=plugin.phantoms().cleanupByTtl();
                if(removed>0&&plugin.config().isLogEvents())LOG.info("auto-cleanup removed {} phantoms",removed);
            }
        };
        cleanupTask.runTaskTimer(plugin,plugin.config().getCleanupIntervalTicks(),plugin.config().getCleanupIntervalTicks());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e){
        for(org.bukkit.entity.Entity entity:e.getChunk().getEntities()){
            plugin.phantoms().rebindEntity(entity);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e){
        Bukkit.getScheduler().runTaskLater(plugin,()->restorePlayerState(e.getPlayer()),2L);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e){
        // Не удаляем игрока из observers: это желаемая видимость, а не online-сессия.
        // Так при rejoin можно восстановить phantom state.
        plugin.effects().clearFor(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e){
        if(e.isCancelled())return;
        Bukkit.getScheduler().runTask(plugin,()->restorePlayerState(e.getPlayer()));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e){
        Bukkit.getScheduler().runTask(plugin,()->restorePlayerState(e.getPlayer()));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e){
        Bukkit.getScheduler().runTaskLater(plugin,()->restorePlayerState(e.getEntity()),2L);
    }

    private void restorePlayerState(org.bukkit.entity.Player player){
        plugin.visibility().restoreFor(player);
        UUID uid=player.getUniqueId();
        int load=plugin.config().getLoadRadius();
        int unload=plugin.config().getUnloadRadius();
        for(var inst:plugin.phantoms().all()){
            var loc=inst.currentLocation();
            if(loc.getWorld()==null||player.getWorld()!=loc.getWorld())continue;
            double dist=loc.distanceSquared(player.getLocation());
            if(!inst.observers().contains(uid))continue;
            var entity=inst.getBackingEntity();
            if(entity==null||entity.isDead())continue;
            if(dist<=load*load){
                player.showEntity(plugin,entity);
            }else if(dist>=unload*unload){
                player.hideEntity(plugin,entity);
            }
        }
    }

    public void shutdown(){
        try{cleanupTask.cancel();}catch(Throwable ignored){}
    }
}