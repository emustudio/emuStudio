/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunnerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static int run(String... args) {
        return AbstractCompiler.runCommandLine("raspc-rasp", args, CompilerRASP::new);
    }

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "READ 5".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        assertEquals(0, run("--output", outputFile.getPath(), sourceFile.getPath()));

        assertTrue(Files.size(outputFile.toPath()) > 0);
    }

    @Test
    public void testCommandLineShortOption() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "HALT".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        assertEquals(0, run("-o", outputFile.getPath(), sourceFile.getPath()));

        assertTrue(Files.size(outputFile.toPath()) > 0);
    }

    @Test
    public void testCommandLinePrintHelp() {
        assertEquals(0, run("--help"));
    }

    @Test
    public void testCommandLinePrintHelpShort() {
        assertEquals(0, run("-h"));
    }

    @Test
    public void testCommandLineNonexistantSourceFileDoesNotThrow() {
        assertEquals(1, run("slfjkdf"));
    }

    @Test
    public void testCommandLinePrintVersion() {
        assertEquals(0, run("--version"));
    }

    @Test
    public void testCommandLinePrintVersionShort() {
        assertEquals(0, run("-v"));
    }

    @Test
    public void testCommandLineNoArgs() {
        assertEquals(1, run());
    }
}

