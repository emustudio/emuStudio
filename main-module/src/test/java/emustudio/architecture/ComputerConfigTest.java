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

public class ComputerConfigTest {

    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        ComputerConfig.setConfigBaseDir(getBaseDirectory().toFile().getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() {
        ComputerConfig.setConfigBaseDir(System.getProperty("user.dir"));
    }

    static Path getBaseDirectory() throws URISyntaxException {
        URL resourceUrl = ComputerConfigTest.class.getResource("/");
        return Paths.get(resourceUrl.toURI());
    }

    @Test
    public void testGetAllFileNames() throws URISyntaxException {
        Path tmpfilesDirname = getBaseDirectory().resolve("tmpfiles");
        String[] result = ComputerConfig.getAllFiles(tmpfilesDirname, ".conf");
        assertEquals(1, result.length);
        result = ComputerConfig.getAllFiles(tmpfilesDirname, ".txt");
        assertEquals(2, result.length);
        result = ComputerConfig.getAllFiles(tmpfilesDirname, ".NONEXISTANT");
        assertEquals(0, result.length);
    }

    @Test
    public void testDeleteConfiguration() throws Exception {
        File file = ComputerConfig.getConfigDir().resolve("test.conf").toFile();
        System.out.println("Going to delete file: " + file.getAbsolutePath() + " : " + file.exists());
        assertTrue(file.exists());

        assertTrue(ComputerConfig.delete("test"));
        assertFalse(file.exists());
        assertFalse(ComputerConfig.delete("test"));

        assertFalse(file.exists());
        file.createNewFile();
        assertTrue(file.exists());
    }

    @Test
    public void testRenameConfiguration() throws URISyntaxException {
        File oldFile = ComputerConfig.getConfigDir().resolve("test.conf").toFile();
        File newFile = ComputerConfig.getConfigDir().resolve("newtest.conf").toFile();

        System.out.println(oldFile.getAbsolutePath() + " : " + oldFile.exists());

        assertTrue(oldFile.exists());
        assertFalse(newFile.exists());

        assertTrue(ComputerConfig.rename("newtest", "test"));
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());
        assertFalse(ComputerConfig.rename("newtest", "test"));
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());

        assertTrue(ComputerConfig.rename("test", "newtest"));
        assertTrue(oldFile.exists());
        assertFalse(newFile.exists());
    }

}
