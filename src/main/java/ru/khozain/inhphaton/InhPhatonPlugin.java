package ru.khozain.inhphaton;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.khozain.inhphaton.commands.GodCommand;
import ru.khozain.inhphaton.config.PluginConfig;
import ru.khozain.inhphaton.effect.PersonalEffectManager;
import ru.khozain.inhphaton.internal.DebugManager;
import ru.khozain.inhphaton.internal.ShutdownCleaner;
import ru.khozain.inhphaton.phantom.PhantomManager;
import ru.khozain.inhphaton.scenario.ScenarioService;
import ru.khozain.inhphaton.visibility.VisibilityManager;

import java.util.Objects;

public final class InhPhatonPlugin extends JavaPlugin {
    public static final String PERMISSION_ADMIN = "god.admin";
    public static final String PERMISSION_PHANTOM = "god.phantom";
    public static final String PERMISSION_VISIBILITY = "god.visibility";
    public static final String PERMISSION_DEBUG = "god.debug";
    public static final String PERMISSION_RELOAD = "god.reload";

    private static InhPhatonPlugin instance;
    private static final Logger LOG = LoggerFactory.getLogger("inhPhaton");

    private PluginConfig config;
    private DebugManager debugManager;
    private VisibilityManager visibilityManager;
    private PhantomManager phantomManager;
    private PersonalEffectManager effectManager;
    private ScenarioService scenarioService;
    private ShutdownCleaner shutdownCleaner;
    private GodCommand godCommand;

    public static InhPhatonPlugin getInstance() {
        return Objects.requireNonNull(instance, "inhPhaton is not enabled yet");
    }

    @Override
    public void onEnable() {
        instance = this;
        long startMs = System.currentTimeMillis();

        this.config = PluginConfig.loadOrDefault(this);
        this.debugManager = new DebugManager(config.isDebug());
        this.visibilityManager = new VisibilityManager(this);
        this.phantomManager = new PhantomManager(this);
        this.effectManager = new PersonalEffectManager(this);
        this.scenarioService = new ScenarioService(this);

        this.shutdownCleaner = new ShutdownCleaner(this);

        this.godCommand = new GodCommand(this);
        this.godCommand.register();

        getServer().getPluginManager().registerEvents(shutdownCleaner, this);

        long ms = System.currentTimeMillis() - startMs;
        LOG.info("inhPhaton enabled in {} ms (limits: perPlayer={}, global={}, ttl={}s)",
            ms, config.getMaxPhantomsPerPlayer(), config.getGlobalPhantomLimit(),
            config.getDefaultTtlSeconds());
    }

    @Override
    public void onDisable() {
        try {
            if (godCommand != null) godCommand.unregister();
            if (shutdownCleaner != null) shutdownCleaner.shutdown();
            if (scenarioService != null) scenarioService.shutdown();
            if (effectManager != null) effectManager.shutdown();
            if (phantomManager != null) phantomManager.shutdown();
            if (visibilityManager != null) visibilityManager.shutdown();
        } catch (Throwable t) {
            LOG.warn("Error during shutdown: {}", t.getMessage());
        } finally {
            instance = null;
            LOG.info("inhPhaton disabled");
        }
    }

    public void reload() {
        this.config = PluginConfig.loadOrDefault(this);
        boolean preserve = config.isPreserveOnReload();
        this.debugManager.setEnabled(config.isDebug());
        this.phantomManager.onConfigReload(preserve);
        this.visibilityManager.onConfigReload(preserve);
        LOG.info("inhPhaton reloaded (preserve={})", preserve);
    }

    public PluginConfig config() { return config; }
    public DebugManager debug() { return debugManager; }
    public VisibilityManager visibility() { return visibilityManager; }
    public PhantomManager phantoms() { return phantomManager; }
    public PersonalEffectManager effects() { return effectManager; }
    public ScenarioService scenarios() { return scenarioService; }
}