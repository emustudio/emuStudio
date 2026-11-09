/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugin.compiler.AbstractCompiler;
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

    private static int run(String... args) {
        return AbstractCompiler.runCommandLine("brainc-brainduck", args, CompilerBrainduck::new);
    }

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), ">>".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        assertEquals(0, run("--output", outputFile.getPath(), sourceFile.getPath()));

        List<String> lines = Files.readAllLines(outputFile.toPath());

        assertEquals(2, lines.size());
        assertEquals(":020000000101FC", lines.get(0));
        assertEquals(":00000001FF", lines.get(1));
    }

    @Test
    public void testCommandLinePrintHelp() {
        assertEquals(0, run("--help"));
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
    public void testCommandLineShortHelp() {
        assertEquals(0, run("-h"));
    }

    @Test
    public void testCommandLineShortVersion() {
        assertEquals(0, run("-v"));
    }

    @Test
    public void testCommandLineShortOutput() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), ">".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        assertEquals(0, run("-o", outputFile.getPath(), sourceFile.getPath()));

        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertEquals(2, lines.size());
        assertEquals(":0100000001FE", lines.get(0));
        assertEquals(":00000001FF", lines.get(1));
    }

    @Test
    public void testCommandLineNoArguments() {
        assertEquals(1, run());
    }

    @Test
    public void testCommandLineOutputWithoutFile() {
        // --output without a following filename and no input file
        assertEquals(1, run("--output"));
    }
}
