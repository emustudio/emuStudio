package net.emustudio.application.configuration;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import org.junit.Test;

import java.awt.*;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class PluginConfigTest {

    @Test
    public void testAbsolutePathInPluginConfigWontBeRelativized() {
        PluginConfig config = PluginConfig.create(
            PLUGIN_TYPE.CPU, "myName", "/this/is/absolute/path.jar", new Point(0, 0)
        );
        ConfigFiles configFiles = new ConfigFiles();
        assertEquals(Path.of("/this/is/absolute/path.jar"), config.getPluginPath(configFiles));
    }

    @Test
    public void testRelativePathInPluginConfigWillBeRelativized() {
        PluginConfig config = PluginConfig.create(
            PLUGIN_TYPE.CPU, "myName", "relativepath.jar", new Point(0, 0)
        );
        ConfigFiles configFiles = new ConfigFiles();
        assertEquals(Path.of(System.getProperty("user.dir"), "cpu", "relativepath.jar"), config.getPluginPath(configFiles));
    }
}
