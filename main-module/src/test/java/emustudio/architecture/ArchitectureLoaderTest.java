/*
 * ArchitectureLoaderTest.java
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2012, Peter Jakubčo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.architecture;

import emustudio.drawing.Schema;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ArchitectureLoaderTest {

    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        ArchitectureLoader.setConfigurationBaseDirectory(getBaseDirectory().toFile().getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() {
        ArchitectureLoader.setConfigurationBaseDirectory(System.getProperty("user.dir"));
    }

    public static Path getBaseDirectory() throws URISyntaxException {
        URL resourceUrl = ArchitectureLoaderTest.class.getResource("/");
        return Paths.get(resourceUrl.toURI());
    }

    @Test
    public void testGetInstance() {
        ArchitectureLoader result = ArchitectureLoader.getInstance();
        ArchitectureLoader expResult = ArchitectureLoader.getInstance();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetAllFileNames() {
        String tmpfilesDirname = "tmpfiles";
        String[] result = ArchitectureLoader.getAllFileNames(tmpfilesDirname, ".conf");
        assertEquals(1, result.length);
        result = ArchitectureLoader.getAllFileNames(tmpfilesDirname, ".txt");
        assertEquals(2, result.length);
        result = ArchitectureLoader.getAllFileNames(tmpfilesDirname, ".NONEXISTANT");
        assertEquals(0, result.length);
    }

    @Test
    public void testDeleteConfiguration() throws Exception {
        ArchitectureLoader instance = ArchitectureLoader.getInstance();

        File file = getBaseDirectory().resolve(ArchitectureLoader.CONFIGS_DIR).resolve("test.conf").toFile();
        System.out.println(file.getAbsolutePath() + " : " + file.exists());
        assertTrue(file.exists());

        assertTrue(instance.deleteConfiguration("test"));
        assertFalse(file.exists());
        assertFalse(instance.deleteConfiguration("test"));

        assertFalse(file.exists());
        file.createNewFile();
        assertTrue(file.exists());
    }

    @Test
    public void testRenameConfiguration() throws URISyntaxException {
        ArchitectureLoader instance = ArchitectureLoader.getInstance();

        File oldFile = getBaseDirectory().resolve(ArchitectureLoader.CONFIGS_DIR).resolve("test.conf")
                .toFile();
        File newFile = getBaseDirectory().resolve(ArchitectureLoader.CONFIGS_DIR).resolve("newtest.conf")
                .toFile();

        System.out.println(oldFile.getAbsolutePath() + " : " + oldFile.exists());

        assertTrue(oldFile.exists());
        assertFalse(newFile.exists());

        assertTrue(instance.renameConfiguration("newtest", "test"));
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());
        assertFalse(instance.renameConfiguration("newtest", "test"));
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());

        assertTrue(instance.renameConfiguration("test", "newtest"));
        assertTrue(oldFile.exists());
        assertFalse(newFile.exists());
    }

    @Test
    public void testLoadSchema() throws Exception {
        String configName = "tmp";
        ArchitectureLoader instance = ArchitectureLoader.getInstance();
        Schema result = instance.loadSchema(configName);
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
