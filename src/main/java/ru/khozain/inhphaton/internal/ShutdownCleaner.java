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
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.khozain.inhphaton.InhPhatonPlugin;

public final class ShutdownCleaner implements Listener {
    private static final Logger LOG=LoggerFactory.getLogger("inhPhaton.lifecycle");
    private final InhPhatonPlugin plugin; private final BukkitRunnable cleanupTask;
    public ShutdownCleaner(InhPhatonPlugin plugin){this.plugin=plugin;InhPhatonPlugin p=plugin;cleanupTask=new BukkitRunnable(){@Override public void run(){if(!p.config().isAutoCleanup())return;int removed=p.phantoms().cleanupByTtl();if(removed>0&&p.config().isLogEvents())LOG.info("auto-cleanup removed {} phantoms",removed);}};cleanupTask.runTaskTimer(plugin,plugin.config().getCleanupIntervalTicks(),plugin.config().getCleanupIntervalTicks());}
    @EventHandler(priority=EventPriority.MONITOR) public void onJoin(PlayerJoinEvent e){Bukkit.getScheduler().runTaskLater(plugin,()->{plugin.visibility().restoreFor(e.getPlayer());restorePhantomsFor(e.getPlayer().getUniqueId());},2L);}
    @EventHandler(priority=EventPriority.MONITOR) public void onQuit(PlayerQuitEvent e){plugin.phantoms().clearForPlayer(e.getPlayer().getUniqueId(),false);plugin.effects().clearFor(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR) public void onTeleport(PlayerTeleportEvent e){if(e.isCancelled())return;Bukkit.getScheduler().runTask(plugin,()->{plugin.visibility().restoreFor(e.getPlayer());restorePhantomsFor(e.getPlayer().getUniqueId());});}
    @EventHandler(priority=EventPriority.MONITOR) public void onWorldChange(PlayerChangedWorldEvent e){Bukkit.getScheduler().runTask(plugin,()->{plugin.visibility().restoreFor(e.getPlayer());restorePhantomsFor(e.getPlayer().getUniqueId());});}
    @EventHandler(priority=EventPriority.MONITOR) public void onDeath(PlayerDeathEvent e){Bukkit.getScheduler().runTaskLater(plugin,()->{plugin.visibility().restoreFor(e.getEntity());restorePhantomsFor(e.getEntity().getUniqueId());},2L);}
    private void restorePhantomsFor(java.util.UUID uid){org.bukkit.entity.Player p=Bukkit.getServer().getPlayer(uid);if(p==null||!p.isOnline())return;for(var inst:plugin.phantoms().all()){if(!inst.observers().contains(uid))continue;double dist=inst.spec().getInitialLocation().distanceSquared(p.getLocation());int radius=plugin.config().getLoadRadius();if(dist>radius*radius)continue;if(inst.getBackingEntity()!=null)p.showEntity(plugin,inst.getBackingEntity());else ru.khozain.inhphaton.compat.PacketAbstraction.sendSpawn(p,inst.id());}}
    public void shutdown(){try{cleanupTask.cancel();}catch(Throwable t){ }}
}