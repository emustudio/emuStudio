/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.architecture;

import emustudio.drawing.Schema;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import static emustudio.architecture.ComputerConfigTest.getBaseDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationImplTest {

    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        ComputerConfig.setConfigBaseDir(getBaseDirectory().toFile().getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() {
        ComputerConfig.setConfigBaseDir(System.getProperty("user.dir"));
    }

    @Test
    public void testLoadSchema() throws Exception {
        String configName = "tmp";
        File configFile = ComputerConfig.getConfigDir().resolve(configName + ".conf").toFile();

        Properties properties = new Properties();
        properties.load(new FileInputStream(configFile));

        ConfigurationImpl configuration = new ConfigurationImpl(configName, properties);

        Schema result = configuration.loadSchema();
        assertNotNull(result);
        assertEquals(configName, result.getConfigName());
        assertNotNull(result.getCompilerElement());
        assertNotNull(result.getCpuElement());
        assertNotNull(result.getMemoryElement());
        assertEquals(1, result.getDeviceElements().size());
        assertEquals("brainc-brainduck-0.15.1-SNAPSHOT", result.getCompilerElement().getPluginName());
        assertEquals("brainduck-cpu-0.14.1-SNAPSHOT", result.getCpuElement().getPluginName());
        assertEquals("brainduck-mem-0.12.1-SNAPSHOT", result.getMemoryElement().getPluginName());
        assertEquals("terminal-brainduck-0.13.1-SNAPSHOT", result.getDeviceElements().get(0).getPluginName());
        assertEquals(20, result.getGridGap());
        assertTrue(result.isGridUsed());
        assertEquals(3, result.getConnectionLines().size());
    }

}
