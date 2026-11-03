/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.SourceToken;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SourceAddressMapTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void exactMappingUsesCompiledBankAndCommonRegionUsesBankZero() throws Exception {
        Path source = source("program.asm", "NOP\nHALT\n");
        SourceCodeAnnotation banked = annotation(source, 1, 0);
        SourceCodeAnnotation common = annotation(source, 2, 0);
        MemoryContext<Byte> memory = memory(8, 2, 4, 1, Map.of(1, Set.of(banked), 6, Set.of(common)));

        SourceAddressMap map = SourceAddressMap.capture(null, memory, null,
                Map.of(source, "NOP\nHALT\n"));

        SourceAddressMap.Location first = map.addressOf(SourceCodePosition.of(1, 0, source.toString())).orElseThrow();
        assertEquals(1, first.address);
        assertEquals(1, first.bank);
        assertTrue(map.sourceAt(1, 0).isEmpty());
        assertEquals(1, map.sourceAt(1, 1).orElseThrow().position.line);
        assertEquals(0, map.sourceAt(6, 1).orElseThrow().bank);
        assertEquals(2, map.sourceAt(6, 0).orElseThrow().position.line);
        assertTrue(map.sourceAt(2, 1).isEmpty());
    }

    @Test
    public void instructionSpanStopsBeforeNextCompilerMapping() throws Exception {
        Path source = source("program.asm", "LD A,1\nDB 2\n");
        Compiler compiler = compiler("LD A,1\nDB 2\n", List.of(
                new SourceToken(Token.RESERVED, 0, "LD", null, null, null)));
        MemoryContext<Byte> memory = memory(8, 1, 8, 0,
                Map.of(0, Set.of(annotation(source, 1, 0)), 2, Set.of(annotation(source, 2, 0))));
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.disassemble(0)).thenReturn(new DisassembledInstruction(0, "LD A,1", "3E01"));
        when(disassembler.getNextInstructionPosition(0)).thenReturn(4);

        SourceAddressMap map = SourceAddressMap.capture(compiler, memory, disassembler,
                Map.of(source, "LD A,1\nDB 2\n"));

        assertTrue(map.sourceAt(1, 0).orElseThrow().instruction);
        assertEquals(1, map.sourceAt(1, 0).orElseThrow().position.line);
        assertEquals(2, map.sourceAt(2, 0).orElseThrow().position.line);
        assertFalse(map.sourceAt(2, 0).orElseThrow().instruction);
        assertTrue(map.sourceAt(3, 0).isEmpty());
    }

    @Test
    public void changedAnnotationInvalidatesPreviouslyCapturedAddress() throws Exception {
        Path source = source("program.asm", "NOP\n");
        Map<Integer, Set<SourceCodeAnnotation>> annotations = new HashMap<>();
        annotations.put(0, Set.of(annotation(source, 1, 0)));
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, annotations);
        SourceAddressMap map = SourceAddressMap.capture(null, memory, null, Map.of(source, "NOP\n"));
        assertTrue(map.sourceAt(0, 0).isPresent());

        // A later compilation can publish new annotation objects for the same positions.
        annotations.put(0, Set.of(annotation(source, 1, 0)));

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
    }

    @Test
    public void changedMemoryLayoutInvalidatesCapturedLocations() throws Exception {
        Path source = source("program.asm", "NOP\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 0))));
        SourceAddressMap map = SourceAddressMap.capture(null, memory, null, Map.of(source, "NOP\n"));

        when(memory.getSize()).thenReturn(8);

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
    }

    @Test
    public void multipleAddressesOnOneSourceLineHaveNoUniqueForwardMapping() throws Exception {
        Path source = source("program.asm", "DB 1,2\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0,
                Map.of(0, Set.of(annotation(source, 1, 3)), 1, Set.of(annotation(source, 1, 5))));

        SourceAddressMap map = SourceAddressMap.capture(null, memory, null, Map.of(source, "DB 1,2\n"));

        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
        assertEquals(3, map.sourceAt(0, 0).orElseThrow().position.column);
        assertEquals(5, map.sourceAt(1, 0).orElseThrow().position.column);
    }

    @Test
    public void multipleAnnotationsAtOneAddressAreIgnored() throws Exception {
        Path source = source("program.asm", "NOP\nHALT\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0,
                Map.of(0, Set.of(annotation(source, 1, 0), annotation(source, 2, 0))));

        SourceAddressMap map = SourceAddressMap.capture(null, memory, null,
                Map.of(source, "NOP\nHALT\n"));

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
    }

    @Test
    public void staleCompilerInputIsRejectedDuringCapture() throws Exception {
        Path source = source("program.asm", "HALT\n");

        IOException failure = assertThrows(IOException.class,
                () -> SourceAddressMap.capture(null, null, null, Map.of(source, "NOP\n")));

        assertTrue(failure.getMessage().contains("Source changed during compilation"));
    }

    @Test
    public void annotationFromUncapturedInputIsRejected() throws Exception {
        Path source = source("program.asm", "NOP\n");
        Path included = source("include.asm", "HALT\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0,
                Map.of(0, Set.of(annotation(included, 1, 0))));

        IOException failure = assertThrows(IOException.class,
                () -> SourceAddressMap.capture(null, memory, null, Map.of(source, "NOP\n")));

        assertTrue(failure.getMessage().contains("Source was not captured before compilation"));
    }

    @Test
    public void editorLineEndingNormalizationDoesNotHideExternalSourceChanges() throws Exception {
        Path source = source("program.asm", "NOP\r\nHALT\r\n");
        SourceAddressMap map = SourceAddressMap.capture(null, null, null,
                Map.of(source, "NOP\r\nHALT\r\n"));
        assertTrue(map.matches(source, "NOP\nHALT\n"));
        assertTrue(map.inputsUnchanged());

        Files.writeString(source, "HALT\r\n");

        assertFalse(map.inputsUnchanged());
        assertFalse(map.matches(source, "HALT\n"));
        assertTrue(map.matches(source, "NOP\nHALT\n"));
    }

    @Test
    public void includeCaptureResolvesRelativeFilesAndStopsIncludeCycles() throws Exception {
        Path root = source("root.asm", "include child.asm\n");
        Path child = source("child.asm", "include root.asm\n");
        Compiler compiler = mock(Compiler.class);
        LexicalAnalyzer lexer = mock(LexicalAnalyzer.class);
        when(compiler.createLexer()).thenReturn(lexer);
        when(lexer.analyze("include child.asm\n")).thenReturn(List.of(
                new SourceToken(Token.INCLUDE_PATH, 8, "child.asm", null, null, null, "child.asm")));
        when(lexer.analyze("include root.asm\n")).thenReturn(List.of(
                new SourceToken(Token.INCLUDE_PATH, 8, "root.asm", null, null, null, "root.asm")));

        Map<Path, String> inputs = SourceAddressMap.captureInputs(compiler, root);

        assertEquals(Map.of(root, "include child.asm\n", child, "include root.asm\n"), inputs);
        assertThrows(UnsupportedOperationException.class, () -> inputs.put(root, "changed"));
    }

    @Test
    public void labelOnPreviousLineMapsToInstructionButConstantRemainsSourceOnly() throws Exception {
        String text = "entry:\nNOP\nvalue EQU 1\n";
        Path source = source("program.asm", text);
        Compiler compiler = compiler(text, List.of(
                new SourceToken(Token.SYMBOL_DEFINITION, 0, "entry:", "entry", null, null),
                new SourceToken(Token.RESERVED, 7, "NOP", null, null, null),
                new SourceToken(Token.SYMBOL_DEFINITION, 11, "value", "value", null, null)));
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 2, 0))));

        SourceAddressMap map = SourceAddressMap.capture(compiler, memory, null, Map.of(source, text));

        assertEquals(0, map.addressOf(SourceCodePosition.of(1, 0, source.toString())).orElseThrow().address);
        assertEquals(2, map.symbols().size());
        SourceAddressMap.Symbol entry = map.symbols().get(0);
        assertEquals("entry", entry.name);
        assertEquals(0, entry.location.orElseThrow().address);
        assertFalse(entry.sourceOnly);
        SourceAddressMap.Symbol constant = map.symbols().get(1);
        assertEquals("value", constant.name);
        assertTrue(constant.location.isEmpty());
        assertTrue(constant.sourceOnly);
    }

    private Path source(String name, String text) throws IOException {
        Path file = temporary.newFile(name).toPath().toAbsolutePath().normalize();
        Files.writeString(file, text);
        return file;
    }

    private static SourceCodeAnnotation annotation(Path file, int line, int column) {
        return new SourceCodeAnnotation(1, SourceCodePosition.of(line, column, file.toString()));
    }

    private static Compiler compiler(String text, List<Token> tokens) {
        Compiler compiler = mock(Compiler.class);
        LexicalAnalyzer lexer = mock(LexicalAnalyzer.class);
        when(compiler.createLexer()).thenReturn(lexer);
        when(lexer.analyze(text)).thenReturn(tokens);
        return compiler;
    }

    @SuppressWarnings("unchecked")
    private static MemoryContext<Byte> memory(int size, int bankCount, int common, int selectedBank,
                                               Map<Integer, Set<SourceCodeAnnotation>> contents) {
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        MemoryContextAnnotations annotations = mock(MemoryContextAnnotations.class);
        when(memory.getSize()).thenReturn(size);
        when(memory.getBanksCount()).thenReturn(bankCount);
        when(memory.getCommonBoundary()).thenReturn(common);
        when(memory.getSelectedBank()).thenReturn(selectedBank);
        when(memory.annotations()).thenReturn(annotations);
        when(annotations.getAll(SourceCodeAnnotation.class)).thenReturn(contents);
        when(annotations.get(anyInt(), eq(SourceCodeAnnotation.class))).thenAnswer(
                call -> contents.getOrDefault(call.getArgument(0), Set.of()));
        return memory;
    }
}
