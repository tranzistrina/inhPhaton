package ru.khozain.inhphaton;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigTest {
    @Test
    void paperPluginYamlIsParseable() throws Exception {
        InputStream in=getClass().getClassLoader().getResourceAsStream("paper-plugin.yml");
        assertNotNull(in,"paper-plugin.yml must be present in test resources");
        YamlConfiguration root=YamlConfiguration.loadConfiguration(
            new InputStreamReader(in,StandardCharsets.UTF_8));
        assertEquals("ru.khozain.inhphaton.InhPhatonPlugin",root.getString("main"));
        assertEquals("26.2",root.getString("api-version"));
        assertTrue(root.isConfigurationSection("permissions"));
    }

    @Test
    void yamlFixtureStructure() {
        YamlConfiguration root=new YamlConfiguration();
        root.set("limits.max-phantoms-per-player",10);
        root.set("limits.global-phantom-limit",100);
        root.set("limits.default-ttl-seconds",30);
        root.set("behavior.after-restart-policy","restore");
        assertEquals(10,root.getInt("limits.max-phantoms-per-player"));
        assertEquals(100,root.getInt("limits.global-phantom-limit"));
        assertEquals(30,root.getInt("limits.default-ttl-seconds"));
        assertEquals("restore",root.getString("behavior.after-restart-policy"));
    }
}