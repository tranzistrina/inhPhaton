package ru.khozain.inhphaton.scenario;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Location;
import ru.khozain.inhphaton.InhPhatonPlugin;
import ru.khozain.inhphaton.effect.PersonalEffect;
import ru.khozain.inhphaton.phantom.PhantomInstance;
import ru.khozain.inhphaton.phantom.PhantomSpec;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScenarioService {
    private final InhPhatonPlugin plugin;
    private final Set<Scenario> scenarios=ConcurrentHashMap.newKeySet();
    public ScenarioService(InhPhatonPlugin plugin){this.plugin=plugin;}
    public @Nullable Scenario createPhantom(@NotNull UUID ownerId,@NotNull PhantomSpec spec,@NotNull Set<UUID> observers){
        PhantomInstance inst=plugin.phantoms().create(spec);if(inst==null)return null;for(UUID o:observers)plugin.phantoms().showTo(inst.id(),o);Scenario sc=new Scenario(inst,observers,ownerId,spec.getTtlSeconds());scenarios.add(sc);return sc;
    }
    public boolean removeScenario(@NotNull UUID scenarioId){Scenario s=byId(scenarioId);if(s==null)return false;scenarios.remove(s);plugin.phantoms().remove(s.phantom().id());return true;}
    public boolean removeAll(@NotNull UUID ownerId){boolean changed=false;for(Scenario s:new LinkedHashSet<>(scenarios))if(ownerId.equals(s.ownerId())){removeScenario(s.id());changed=true;}return changed;}
    public void playEffectTo(@NotNull UUID scenarioId,@NotNull UUID playerId,@NotNull String effectId,@NotNull PersonalEffect effect){if(byId(scenarioId)!=null)plugin.effects().playTo(playerId,effectId,effect);}
    public void shutdown(){for(Scenario s:new LinkedHashSet<>(scenarios))plugin.phantoms().remove(s.phantom().id());scenarios.clear();}
    public Set<Scenario> all(){return new LinkedHashSet<>(scenarios);}
    public @Nullable Scenario byId(UUID id){for(Scenario s:scenarios)if(s.id().equals(id))return s;return null;}
    public static final class Scenario{
        private final UUID id=UUID.randomUUID();private final PhantomInstance phantom;private final Set<UUID> observers;private final UUID ownerId;private final long ttlSeconds;private final long createdAtMs=System.currentTimeMillis();
        public Scenario(PhantomInstance p,Set<UUID> obs,UUID owner,long ttl){phantom=p;observers=new LinkedHashSet<>(obs);ownerId=owner;ttlSeconds=ttl;}
        public UUID id(){return id;}public PhantomInstance phantom(){return phantom;}public Set<UUID> observers(){return observers;}public UUID ownerId(){return ownerId;}public long ttlSeconds(){return ttlSeconds;}public long createdAtMs(){return createdAtMs;}
        public boolean isExpired(){return ttlSeconds>0&&(System.currentTimeMillis()-createdAtMs)>ttlSeconds*1000L;}
    }
}