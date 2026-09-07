package ru.khozain.inhphaton.effect;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.khozain.inhphaton.InhPhatonPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalEffectManager {
    private final InhPhatonPlugin plugin; private final Map<UUID,Map<String,PersonalEffect>> effects=new ConcurrentHashMap<>();
    public PersonalEffectManager(InhPhatonPlugin plugin){this.plugin=plugin;}
    public void playTo(@NotNull UUID playerId,@NotNull String effectId,@NotNull PersonalEffect effect){
        effects.computeIfAbsent(playerId,k->new ConcurrentHashMap<>()).put(effectId,effect);
        Player p=plugin.getServer().getPlayer(playerId); if(p!=null) effect.play(p);
        if(plugin.config().isLogEvents())plugin.getLogger().info(String.format("[effect] %s -> %s (type=%s)",playerId,effectId,effect.getType()));
    }
    public void clearFor(@NotNull UUID playerId){effects.remove(playerId);}
    public void clearEffect(@NotNull UUID playerId,@NotNull String effectId){Map<String,PersonalEffect> m=effects.get(playerId);if(m!=null)m.remove(effectId);}
    public void shutdown(){effects.clear();}
}