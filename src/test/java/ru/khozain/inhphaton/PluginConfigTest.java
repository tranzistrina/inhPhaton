package ru.khozain.inhphaton;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigTest {
    @Test
    void paperPluginYamlIsParseable() throws Exception {
        InputStream in=getClass().getClassLoader().getResourceAsStream("paper-plugin.yml");
        assertNotNull(in,"paper-plugin.yml must be present in test resources");
        Map<String,Object> root=new Yaml().load(new InputStreamReader(in,StandardCharsets.UTF_8));
        assertNotNull(root);
        assertEquals("ru.khozain.inhphaton.InhPhatonPlugin",root.get("main"));
        assertTrue(root.containsKey("permissions"));
    }

    @Test
    void yamlFixtureStructure() {
        String yml="""
            limits:
              max-phantoms-per-player: 10
              global-phantom-limit: 100
              default-ttl-seconds: 30
              load-radius: 32
              unload-radius: 40
            behavior:
              auto-cleanup: true
              cleanup-interval-ticks: 50
              preserve-on-reload: true
              after-restart-policy: restore
              log-events: false
              debug: true
            packets:
              spawn-batch-size: 8
              spawn-batch-delay-ticks: 2
            """;
        Map<String,Object> root=new Yaml().load(yml);
        @SuppressWarnings("unchecked")
        Map<String,Object> limits=(Map<String,Object>)root.get("limits");
        assertEquals(10,limits.get("max-phantoms-per-player"));
        assertEquals(100,limits.get("global-phantom-limit"));
        assertEquals(30,limits.get("default-ttl-seconds"));
    }
}