/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.configuration;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.P;
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
