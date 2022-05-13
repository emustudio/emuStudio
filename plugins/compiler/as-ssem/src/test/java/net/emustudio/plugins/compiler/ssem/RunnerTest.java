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
package net.emustudio.plugins.compiler.ssem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RunnerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "0 sto 22\n".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        Runner.main("--output", outputFile.getPath(), sourceFile.getPath());

        byte[] bytes = Files.readAllBytes(outputFile.toPath());

        // 32 words of 32 bits + 4 bytes for startLine
        assertEquals(33 * 4, bytes.length);

        byte[] expected = new byte[33 * 4];
        expected[4] = 0x68;
        expected[5] = 0x6;
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testCommandLinePrintHelp() {
        Runner.main("--help");
    }

    @Test
    public void testCommandLineNonexistantSourceFileDoesNotThrow() {
        Runner.main("slfjkdf");
    }

    @Test
    public void testCommandLinePrintVersion() {
        Runner.main("--version");
    }
}
