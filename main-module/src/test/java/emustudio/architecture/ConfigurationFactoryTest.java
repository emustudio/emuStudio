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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationFactoryTest {

    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        ConfigurationFactory.setConfigurationBaseDirectory(getBaseDirectory().toFile().getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() {
        ConfigurationFactory.setConfigurationBaseDirectory(System.getProperty("user.dir"));
    }

    public static Path getBaseDirectory() throws URISyntaxException {
        URL resourceUrl = ConfigurationFactoryTest.class.getResource("/");
        return Paths.get(resourceUrl.toURI());
    }

    @Test
    public void testGetAllFileNames() {
        String tmpfilesDirname = "tmpfiles";
        String[] result = ConfigurationFactory.getAllFileNames(tmpfilesDirname, ".conf");
        assertEquals(1, result.length);
        result = ConfigurationFactory.getAllFileNames(tmpfilesDirname, ".txt");
        assertEquals(2, result.length);
        result = ConfigurationFactory.getAllFileNames(tmpfilesDirname, ".NONEXISTANT");
        assertEquals(0, result.length);
    }

    @Test
    public void testDeleteConfiguration() throws Exception {
        File file = getBaseDirectory().resolve(ConfigurationFactory.CONFIGS_DIR).resolve("test.conf").toFile();
        System.out.println(file.getAbsolutePath() + " : " + file.exists());
        assertTrue(file.exists());

        assertTrue(ConfigurationFactory.delete("test"));
        assertFalse(file.exists());
        assertFalse(ConfigurationFactory.delete("test"));

        assertFalse(file.exists());
        file.createNewFile();
        assertTrue(file.exists());
    }

    @Test
    public void testRenameConfiguration() throws URISyntaxException {
        File oldFile = getBaseDirectory().resolve(ConfigurationFactory.CONFIGS_DIR).resolve("test.conf")
                .toFile();
        File newFile = getBaseDirectory().resolve(ConfigurationFactory.CONFIGS_DIR).resolve("newtest.conf")
                .toFile();

        System.out.println(oldFile.getAbsolutePath() + " : " + oldFile.exists());

        assertTrue(oldFile.exists());
        assertFalse(newFile.exists());

        assertTrue(ConfigurationFactory.rename("newtest", "test"));
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());
        assertFalse(ConfigurationFactory.rename("newtest", "test"));
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());

        assertTrue(ConfigurationFactory.rename("test", "newtest"));
        assertTrue(oldFile.exists());
        assertFalse(newFile.exists());
    }

}
