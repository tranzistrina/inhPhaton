package ru.khozain.inhphaton.visibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.khozain.inhphaton.InhPhatonPlugin;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VisibilityManager {
    private final InhPhatonPlugin plugin;
    private final Map<UUID,VisibilityState> states=new ConcurrentHashMap<>();
    public VisibilityManager(InhPhatonPlugin plugin){this.plugin=plugin;}
    public void setMode(@NotNull UUID entityId,@NotNull VisibilityMode newMode){if(newMode==VisibilityMode.VISIBLE){states.remove(entityId);Entity e=Bukkit.getEntity(entityId);if(e!=null)for(Player p:Bukkit.getOnlinePlayers())p.showEntity(plugin,e);return;}VisibilityState s=states.computeIfAbsent(entityId,k->new VisibilityState(newMode));s.setMode(newMode);applyToAll(entityId,s);}
    public boolean canSee(@NotNull UUID entityId,@NotNull UUID playerId){VisibilityState s=states.get(entityId);if(s==null)return true;return switch(s.getMode()){case HIDDEN->false;case ONLY->s.getAffected().contains(playerId);case EXCEPT->!s.getAffected().contains(playerId);case VISIBLE->true;};}
    public void showTo(@NotNull UUID entityId,@NotNull UUID playerId){VisibilityState s=states.computeIfAbsent(entityId,k->new VisibilityState(VisibilityMode.HIDDEN));if(s.getMode()==VisibilityMode.HIDDEN)s.setMode(VisibilityMode.ONLY);s.getAffected().add(playerId);Entity e=Bukkit.getEntity(entityId);Player p=Bukkit.getPlayer(playerId);if(e!=null&&p!=null)p.showEntity(plugin,e);}
    public void hideFrom(@NotNull UUID entityId,@NotNull UUID playerId){VisibilityState s=states.computeIfAbsent(entityId,k->new VisibilityState(VisibilityMode.VISIBLE));if(s.getMode()==VisibilityMode.VISIBLE)s.setMode(VisibilityMode.EXCEPT);s.getAffected().add(playerId);Entity e=Bukkit.getEntity(entityId);Player p=Bukkit.getPlayer(playerId);if(e!=null&&p!=null)p.hideEntity(plugin,e);}
    public void showOnlyTo(@NotNull UUID entityId,@NotNull Set<UUID> playerIds){VisibilityState s=states.computeIfAbsent(entityId,k->new VisibilityState(VisibilityMode.ONLY));s.setMode(VisibilityMode.ONLY);s.getAffected().clear();s.getAffected().addAll(playerIds);applyToAll(entityId,s);}
    public void hideFromAll(@NotNull UUID entityId){setMode(entityId,VisibilityMode.HIDDEN);}
    public void restoreFor(@NotNull Player player){UUID pid=player.getUniqueId();for(Map.Entry<UUID,VisibilityState> en:states.entrySet()){Entity e=Bukkit.getEntity(en.getKey());if(e==null)continue;boolean see=canSee(en.getKey(),pid);if(see)player.showEntity(plugin,e);else player.hideEntity(plugin,e);}}
    private void applyToAll(UUID entityId,VisibilityState s){Entity e=Bukkit.getEntity(entityId);if(e==null)return;for(Player p:Bukkit.getOnlinePlayers()){boolean see=switch(s.getMode()){case HIDDEN->false;case ONLY->s.getAffected().contains(p.getUniqueId());case EXCEPT->!s.getAffected().contains(p.getUniqueId());case VISIBLE->true;};if(see)p.showEntity(plugin,e);else p.hideEntity(plugin,e);}}
    public void shutdown(){for(UUID eid:new LinkedHashSet<>(states.keySet())){Entity e=Bukkit.getEntity(eid);if(e!=null)for(Player p:Bukkit.getOnlinePlayers())p.showEntity(plugin,e);}states.clear();}
    public void onConfigReload(boolean preserve){if(!preserve)states.clear();}
    public static final class VisibilityState{private VisibilityMode mode;private final Set<UUID> affected=ConcurrentHashMap.newKeySet();public VisibilityState(VisibilityMode mode){this.mode=mode;}public VisibilityMode getMode(){return mode;}public void setMode(VisibilityMode mode){this.mode=mode;}public Set<UUID> getAffected(){return affected;}}
    public @NotNull Map<UUID,VisibilityState> snapshot(){return Collections.unmodifiableMap(states);}
    public @Nullable VisibilityState get(UUID eid){return states.get(eid);}
    public Set<UUID> clear(){Set<UUID> out=new HashSet<>(states.keySet());for(UUID id:out)setMode(id,VisibilityMode.VISIBLE);return out;}
}