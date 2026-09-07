package ru.khozain.inhphaton.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PluginConfig {
    private static final Logger LOG=LoggerFactory.getLogger("inhPhaton.config");
    private final int maxPhantomsPerPlayer,globalPhantomLimit,defaultTtlSeconds,loadRadius,unloadRadius;
    private final boolean autoCleanup,preserveOnReload,logEvents,debug;
    private final long cleanupIntervalTicks;
    private final AfterRestartPolicy afterRestartPolicy;
    private final int spawnBatchSize,spawnBatchDelayTicks;

    public enum AfterRestartPolicy { DROP, RESTORE }

    private PluginConfig(Builder b){
        maxPhantomsPerPlayer=b.maxPhantomsPerPlayer; globalPhantomLimit=b.globalPhantomLimit;
        defaultTtlSeconds=b.defaultTtlSeconds; loadRadius=b.loadRadius; unloadRadius=b.unloadRadius;
        autoCleanup=b.autoCleanup; cleanupIntervalTicks=b.cleanupIntervalTicks;
        preserveOnReload=b.preserveOnReload; afterRestartPolicy=b.afterRestartPolicy;
        logEvents=b.logEvents; debug=b.debug; spawnBatchSize=b.spawnBatchSize; spawnBatchDelayTicks=b.spawnBatchDelayTicks;
    }

    public static PluginConfig loadOrDefault(Plugin plugin){
        Path file=plugin.getDataFolder().toPath().resolve("config.yml");
        try{
            if(!Files.exists(file)){
                plugin.getDataFolder().mkdirs();
                Files.writeString(file,defaultYaml(),StandardCharsets.UTF_8);
                LOG.info("Created default config.yml");
            }
            return parse(YamlConfiguration.loadConfiguration(file.toFile()));
        }catch(Throwable t){
            LOG.warn("Failed to load config.yml, falling back to defaults: {}",t.getMessage());
            return Builder.defaults().build();
        }
    }

    private static PluginConfig parse(YamlConfiguration root){
        Builder b=new Builder();
        ConfigurationSection limits=root.getConfigurationSection("limits");
        ConfigurationSection behavior=root.getConfigurationSection("behavior");
        ConfigurationSection packets=root.getConfigurationSection("packets");
        b.maxPhantomsPerPlayer=Math.max(1,intOr(limits,"max-phantoms-per-player",25));
        b.globalPhantomLimit=Math.max(1,intOr(limits,"global-phantom-limit",500));
        b.defaultTtlSeconds=Math.max(0,intOr(limits,"default-ttl-seconds",0));
        b.loadRadius=Math.max(1,intOr(limits,"load-radius",64));
        b.unloadRadius=Math.max(b.loadRadius,intOr(limits,"unload-radius",80));
        b.autoCleanup=boolOr(behavior,"auto-cleanup",true);
        b.cleanupIntervalTicks=Math.max(1,longOr(behavior,"cleanup-interval-ticks",100));
        b.preserveOnReload=boolOr(behavior,"preserve-on-reload",false);
        String policy=stringOr(behavior,"after-restart-policy","drop");
        b.afterRestartPolicy="restore".equalsIgnoreCase(policy)?AfterRestartPolicy.RESTORE:AfterRestartPolicy.DROP;
        b.logEvents=boolOr(behavior,"log-events",true);
        b.debug=boolOr(behavior,"debug",false);
        b.spawnBatchSize=Math.max(1,intOr(packets,"spawn-batch-size",16));
        b.spawnBatchDelayTicks=Math.max(1,intOr(packets,"spawn-batch-delay-ticks",1));
        return b.build();
    }

    private static int intOr(ConfigurationSection s,String k,int d){return s==null?d:s.getInt(k,d);}
    private static long longOr(ConfigurationSection s,String k,long d){return s==null?d:s.getLong(k,d);}
    private static boolean boolOr(ConfigurationSection s,String k,boolean d){return s==null?d:s.getBoolean(k,d);}
    private static String stringOr(ConfigurationSection s,String k,String d){return s==null?d:s.getString(k,d);}

    private static String defaultYaml(){return """
# inhPhaton — Боги
limits:
  max-phantoms-per-player: 25
  global-phantom-limit: 500
  default-ttl-seconds: 0
  load-radius: 64
  unload-radius: 80
behavior:
  auto-cleanup: true
  cleanup-interval-ticks: 100
  preserve-on-reload: false
  after-restart-policy: drop
  log-events: true
  debug: false
packets:
  spawn-batch-size: 16
  spawn-batch-delay-ticks: 1
""";}

    public int getMaxPhantomsPerPlayer(){return maxPhantomsPerPlayer;}
    public int getGlobalPhantomLimit(){return globalPhantomLimit;}
    public int getDefaultTtlSeconds(){return defaultTtlSeconds;}
    public int getLoadRadius(){return loadRadius;}
    public int getUnloadRadius(){return unloadRadius;}
    public boolean isAutoCleanup(){return autoCleanup;}
    public long getCleanupIntervalTicks(){return cleanupIntervalTicks;}
    public boolean isPreserveOnReload(){return preserveOnReload;}
    public AfterRestartPolicy getAfterRestartPolicy(){return afterRestartPolicy;}
    public boolean isLogEvents(){return logEvents;}
    public boolean isDebug(){return debug;}
    public int getSpawnBatchSize(){return spawnBatchSize;}
    public int getSpawnBatchDelayTicks(){return spawnBatchDelayTicks;}

    public static final class Builder{
        int maxPhantomsPerPlayer=25,globalPhantomLimit=500,defaultTtlSeconds=0,loadRadius=64,unloadRadius=80;
        boolean autoCleanup=true,preserveOnReload=false,logEvents=true,debug=false; long cleanupIntervalTicks=100;
        AfterRestartPolicy afterRestartPolicy=AfterRestartPolicy.DROP; int spawnBatchSize=16,spawnBatchDelayTicks=1;
        public static Builder defaults(){return new Builder();}
        public PluginConfig build(){return new PluginConfig(this);}
    }
}