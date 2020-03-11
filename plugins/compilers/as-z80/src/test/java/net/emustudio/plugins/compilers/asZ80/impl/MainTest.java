/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.asZ80.impl;

import net.emustudio.plugins.compilers.asZ80.Main;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MainTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "ld a,b".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        Main.main("--output", outputFile.getPath(), sourceFile.getPath());

        List<String> lines = Files.readAllLines(outputFile.toPath());

        assertEquals(2, lines.size());
        assertEquals(":010000007887", lines.get(0));
        assertEquals(":00000001FF", lines.get(1));
    }

    @Test
    public void testCommandLinePrintHelp() {
        Main.main("--help");
    }

    @Test
    public void testCommandLineNonexistantSourceFileDoesNotThrow() {
        Main.main("slfjkdf");
    }

    @Test
    public void testCommandLinePrintVersion() {
        Main.main("--version");
    }
}
