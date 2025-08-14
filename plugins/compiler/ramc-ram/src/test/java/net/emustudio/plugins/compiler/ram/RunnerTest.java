/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertTrue;

public class RunnerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "READ 5".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        Runner.main("--output", outputFile.getPath(), sourceFile.getPath());

        assertTrue(Files.size(outputFile.toPath()) > 0);
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
