/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.framework.P;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class PluginConfigTest {

    @Test
    public void testAbsolutePathInPluginConfigWontBeRelativized() {
        PluginConfig config = PluginConfig.create(
                "xx", PLUGIN_TYPE.CPU, "myName", System.getProperty("user.dir") + File.separator + "path.jar",
                P.of(0, 0), Config.inMemory()
        );
        assertEquals(Path.of(System.getProperty("user.dir") + File.separator + "path.jar"), config.getPluginPath());
    }

    @Test
    public void testRelativePathInPluginConfigWillBeRelativized() {
        PluginConfig config = PluginConfig.create(
                "xx", PLUGIN_TYPE.CPU, "myName", "relativepath.jar", P.of(0, 0), Config.inMemory()
        );
        assertEquals(Path.of(System.getProperty("user.dir"), "cpu", "relativepath.jar"), config.getPluginPath());
    }
}
