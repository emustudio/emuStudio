/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
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

    private static int run(String... args) {
        return AbstractCompiler.runCommandLine("as-ssem", args, SSEMCompiler::new);
    }

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "0 sto 22\n".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        assertEquals(0, run("--output", outputFile.getPath(), sourceFile.getPath()));

        byte[] bytes = Files.readAllBytes(outputFile.toPath());

        // 32 words of 32 bits + 4 bytes for startLine
        assertEquals(33 * 4, bytes.length);

        byte[] expected = new byte[33 * 4];
        expected[4] = 0x68;
        expected[5] = 0x6;
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testCommandLineShortOutput() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "0 stp\n".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        assertEquals(0, run("-o", outputFile.getPath(), sourceFile.getPath()));

        byte[] bytes = Files.readAllBytes(outputFile.toPath());
        assertEquals(33 * 4, bytes.length);
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
    public void testCommandLineNoArguments() {
        assertEquals(1, run());
    }

    @Test
    public void testCommandLineOutputWithoutFileSpecified() {
        // --output is the last argument, no output file follows, no input file either
        assertEquals(1, run("--output"));
    }
}
