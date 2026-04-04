/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
    public void testCommandLineShortOutput() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "0 stp\n".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        Runner.main("-o", outputFile.getPath(), sourceFile.getPath());

        byte[] bytes = Files.readAllBytes(outputFile.toPath());
        assertEquals(33 * 4, bytes.length);
    }

    @Test
    public void testCommandLinePrintHelp() {
        Runner.main("--help");
    }

    @Test
    public void testCommandLinePrintHelpShort() {
        Runner.main("-h");
    }

    @Test
    public void testCommandLineNonexistantSourceFileDoesNotThrow() {
        Runner.main("slfjkdf");
    }

    @Test
    public void testCommandLinePrintVersion() {
        Runner.main("--version");
    }

    @Test
    public void testCommandLinePrintVersionShort() {
        Runner.main("-v");
    }

    @Test
    public void testCommandLineNoArguments() {
        Runner.main();
    }

    @Test
    public void testCommandLineOutputWithoutFileSpecified() {
        // --output is the last argument, no output file follows, no input file either
        Runner.main("--output");
    }
}
