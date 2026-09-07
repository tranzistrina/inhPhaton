package ru.khozain.inhphaton.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.khozain.inhphaton.InhPhatonPlugin;
import ru.khozain.inhphaton.effect.PersonalEffect;
import ru.khozain.inhphaton.phantom.PhantomSpec;
import ru.khozain.inhphaton.scenario.ScenarioService;

import java.util.Set;
import java.util.UUID;

public final class GodsAPI {
    private static GodsAPI instance;

    public static synchronized GodsAPI get() {
        if (instance == null) instance = new GodsAPI();
        return instance;
    }

    private GodsAPI() {}

    private InhPhatonPlugin plugin() { return InhPhatonPlugin.getInstance(); }
    private ScenarioService svc() { return plugin().scenarios(); }

    public @Nullable ScenarioService.Scenario createPhantom(@NotNull UUID owner,
                                                            @NotNull PhantomSpec spec,
                                                            @NotNull Set<UUID> observers) {
        return svc().createPhantom(owner, spec, observers);
    }

    public boolean removePhantom(@NotNull UUID scenarioId) {
        return svc().removeScenario(scenarioId);
    }

    public boolean movePhantom(@NotNull UUID scenarioId, @NotNull Location newLoc) {
        ScenarioService.Scenario s = svc().byId(scenarioId);
        if (s == null) return false;
        plugin().phantoms().move(s.phantom().id(), newLoc);
        return true;
    }

    public void showTo(@NotNull UUID scenarioId, @NotNull UUID playerId) {
        ScenarioService.Scenario s = svc().byId(scenarioId);
        if (s != null) plugin().phantoms().showTo(s.phantom().id(), playerId);
    }

    public void hideFrom(@NotNull UUID scenarioId, @NotNull UUID playerId) {
        ScenarioService.Scenario s = svc().byId(scenarioId);
        if (s != null) plugin().phantoms().hideFrom(s.phantom().id(), playerId);
    }

    public void showOnlyTo(@NotNull UUID scenarioId, @NotNull Set<UUID> playerIds) {
        ScenarioService.Scenario s = svc().byId(scenarioId);
        if (s == null) return;
        for (UUID o : s.observers()) plugin().phantoms().hideFrom(s.phantom().id(), o);
        for (UUID o : playerIds) plugin().phantoms().showTo(s.phantom().id(), o);
    }

    public void hideFromAll(@NotNull UUID scenarioId) {
        ScenarioService.Scenario s = svc().byId(scenarioId);
        if (s == null) return;
        for (UUID o : new java.util.LinkedHashSet<>(s.observers())) {
            plugin().phantoms().hideFrom(s.phantom().id(), o);
        }
    }

    public void playPersonalEffect(@NotNull Player player, @NotNull String effectId,
                                   @NotNull PersonalEffect effect) {
        plugin().effects().playTo(player.getUniqueId(), effectId, effect);
    }
}