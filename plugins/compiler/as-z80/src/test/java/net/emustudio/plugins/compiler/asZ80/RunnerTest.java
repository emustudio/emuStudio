/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RunnerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "ld a,b".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        Runner.main("--output", outputFile.getPath(), sourceFile.getPath());

        List<String> lines = Files.readAllLines(outputFile.toPath());

        assertEquals(2, lines.size());
        assertEquals(":010000007887", lines.get(0));
        assertEquals(":00000001FF", lines.get(1));
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
