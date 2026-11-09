/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextNotFoundException;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.io.FileType;
import net.emustudio.emulib.application.settings.PluginSettings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CompilerBrainduckTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testGetDescription() {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        assertNotNull(compiler.getDescription());
        assertFalse(compiler.getDescription().isEmpty());
    }

    @Test
    public void testGetSourceFileTypes() {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        List<FileType> types = compiler.getSourceFileTypes();
        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertEquals(List.of("b"), types.get(0).getExtensions());
    }

    @Test
    public void testCreateLexer() {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        LexicalAnalyzer lexer = compiler.createLexer();
        assertNotNull(lexer);
    }

    @Test
    public void testCompileWithNoMemory() throws Exception {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);

        List<CompilerMessage> messages = new ArrayList<>();
        compiler.addCompilerListener(messages::add);
        compiler.initialize();

        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), ">>".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        compiler.compile(sourceFile.toPath(), Optional.of(outputFile.toPath()));

        // Should have a warning about memory not available
        boolean hasMemoryWarning = messages.stream()
                .anyMatch(m -> m.getFormattedMessage().contains("Memory is not available"));
        assertTrue("Expected warning about memory not available", hasMemoryWarning);
    }

    @Test
    public void testCompileNonexistentFileDoesNotThrow() throws Exception {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);

        List<CompilerMessage> messages = new ArrayList<>();
        compiler.addCompilerListener(messages::add);
        compiler.initialize();

        compiler.compile(java.nio.file.Path.of("nonexistent-file.b"), Optional.empty());

        boolean hasError = messages.stream()
                .anyMatch(m -> m.getType() == CompilerMessage.Type.ERROR);
        assertTrue("Expected compilation error for nonexistent file", hasError);
    }

    @Test
    public void testInitializeWithInvalidMemoryCellType() throws Exception {
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemory(0, Byte.class)).andThrow(new ContextNotFoundException("Unexpected memory cell type")).anyTimes();
        replay(pool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(pool).anyTimes();
        replay(applicationApi);

        CompilerBrainduck compiler = new CompilerBrainduck(0L, applicationApi, PluginSettings.UNAVAILABLE);
        // Should not throw, just log a warning
        compiler.initialize();
    }

    @Test
    public void testCompileWithDefaultOutputPath() throws Exception {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        compiler.initialize();

        File sourceFile = folder.newFile("test.b");
        Files.write(sourceFile.toPath(), ">".getBytes(), StandardOpenOption.WRITE);

        compiler.compile(sourceFile.toPath(), Optional.empty());

        // Verify .hex file was created alongside the source
        File hexFile = new File(sourceFile.getParent(), "test.hex");
        assertTrue("Expected .hex output file to be created", hexFile.exists());
    }

    @Test
    public void testGetVersion() {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        String version = compiler.getVersion();
        assertNotNull(version);
    }

    @Test
    public void testGetCopyright() {
        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        String copyright = compiler.getCopyright();
        assertNotNull(copyright);
    }
}

