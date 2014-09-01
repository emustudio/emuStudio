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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ArchitectureLoaderTest {
    private static final String BASE_DIRECTORY = System.getProperty("user.dir") + "/src/test/resources/";

    @BeforeClass
    public static void setUpClass() {
        ArchitectureLoader.setConfigurationBaseDirectory(BASE_DIRECTORY);
    }

    @AfterClass
    public static void tearDownClass() {
        ArchitectureLoader.setConfigurationBaseDirectory(System.getProperty("user.dir"));
    }

    /**
     * Test of getInstance method, of class ArchitectureLoader.
     */
    @Test
    public void testGetInstance() {
        ArchitectureLoader result = ArchitectureLoader.getInstance();
        ArchitectureLoader expResult = ArchitectureLoader.getInstance();
        assertEquals(expResult, result);
    }

    /**
     * Test of getAllFileNames method, of class ArchitectureLoader.
     */
    @Test
    public void testGetAllFileNames() {
        String dirname = "src/test/resources/tmpfiles/";
        ArchitectureLoader.setConfigurationBaseDirectory(System.getProperty("user.dir"));
        String[] result = ArchitectureLoader.getAllFileNames(dirname, ".conf");
        assertNotNull(result);
        assertEquals(1, result.length);
        result = ArchitectureLoader.getAllFileNames(dirname, ".txt");
        assertNotNull(result);
        assertEquals(2, result.length);
        result = ArchitectureLoader.getAllFileNames(dirname, ".NONEXISTANT");
        assertNotNull(result);
        assertEquals(0, result.length);
        ArchitectureLoader.setConfigurationBaseDirectory(BASE_DIRECTORY);
    }

    /**
     * Test of deleteConfiguration method, of class ArchitectureLoader.
     */
    @Test
    public void testDeleteConfiguration() throws IOException {
        ArchitectureLoader instance = ArchitectureLoader.getInstance();

        File file = new File(BASE_DIRECTORY + ArchitectureLoader.CONFIGS_DIR + File.separator + "test.conf");
        assertTrue(file.exists());

        assertTrue(instance.deleteConfiguration("test"));
        assertFalse(file.exists());
        assertFalse(instance.deleteConfiguration("test"));

        assertFalse(file.exists());
        file.createNewFile();
        assertTrue(file.exists());
    }

    /**
     * Test of renameConfiguration method, of class ArchitectureLoader.
     */
    @Test
    public void testRenameConfiguration() {
        ArchitectureLoader instance = ArchitectureLoader.getInstance();

        File oldFile = new File(BASE_DIRECTORY + ArchitectureLoader.CONFIGS_DIR + File.separator + "test.conf");
        File newFile = new File(BASE_DIRECTORY + ArchitectureLoader.CONFIGS_DIR + File.separator + "newtest.conf");
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

    /**
     * Test of loadSchema method, of class ArchitectureLoader.
     */
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

    /**
     * Test of saveSchema method, of class ArchitectureLoader.
     */
    @Test
    @Ignore
    public void testSaveSchema() throws Exception {
    }

    /**
     * Test of readConfiguration method, of class ArchitectureLoader.
     *
    @Test
    public void testReadConfiguration() throws Exception {
        System.out.println("readConfiguration");
        String configName = "";
        boolean schema_too = false;
        ArchitectureLoader instance = null;
        Properties expResult = null;
        Properties result = instance.readConfiguration(configName, schema_too);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writeConfiguration method, of class ArchitectureLoader.
     *
    @Test
    public void testWriteConfiguration() throws Exception {
        System.out.println("writeConfiguration");
        String configName = "";
        Properties settings = null;
        ArchitectureLoader instance = null;
        instance.writeConfiguration(configName, settings);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of createArchitecture method, of class ArchitectureLoader.
     *
    @Test
    public void testCreateArchitecture() throws Exception {
        System.out.println("createArchitecture");
        String configName = "";
        ArchitectureLoader instance = null;
        ArchitectureManager expResult = null;
        ArchitectureManager result = instance.createArchitecture(configName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/
}
