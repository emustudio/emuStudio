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
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SourceAddressMapTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test
    public void sourceChangedAfterInputCaptureCannotBeBlessedByCompileFinish() throws Exception {
        String rootText = "include \"defs.asm\"\nnop\n";
        String includedText = "VALUE equ 42\n";
        Path root = source("main.asm", rootText);
        Path included = source("defs.asm", includedText);
        Compiler compiler = compiler(Map.of(rootText, List.of(include(rootText, "defs.asm")), includedText, List.of(definition(includedText, "VALUE"))));
        Map<Path, String> inputs = SourceAddressMap.captureInputs(compiler, root);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, root, 4, 2, 0);

        Files.writeString(root, "nop\ninclude \"defs.asm\"\n"); // Old line 2 now names another source construct.
        assertThrows(IOException.class, () -> SourceAddressMap.capture(compiler, memory, null, inputs));
        Files.writeString(root, rootText);
        Files.writeString(included, "VALUE equ 99\n"); // Constants affect emitted code without their own address map.
        assertThrows(IOException.class, () -> SourceAddressMap.capture(compiler, memory, null, inputs));
    }

    @Test
    public void includeClosureIsImmutableCycleSafeAndRetainsUnmappedDeclarations() throws Exception {
        String rootText = "include \"a.asm\"\nentry: nop\n";
        String aText = "include \"b.asm\"\nVALUE equ 42\n";
        String bText = "include \"a.asm\"\nOTHER equ 7\n";
        Path root = source("main.asm", rootText);
        Path a = source("a.asm", aText);
        Path b = source("b.asm", bText);
        Compiler compiler = compiler(Map.of(
                rootText, List.of(include(rootText, "a.asm"), definition(rootText, "entry:")),
                aText, List.of(include(aText, "b.asm"), definition(aText, "VALUE")),
                bText, List.of(include(bText, "a.asm"), definition(bText, "OTHER"))));
        Map<Path, String> inputs = SourceAddressMap.captureInputs(compiler, root.getParent().resolve("./main.asm"));
        assertEquals(Map.of(root, rootText, a, aText, b, bText), inputs);
        assertThrows(UnsupportedOperationException.class, () -> inputs.put(root, "changed"));
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, root, 4, 2, 7);

        SourceAddressMap map = SourceAddressMap.capture(compiler, memory, null, inputs);
        Map<String, SourceAddressMap.Symbol> symbols = map.symbols().stream()
                .collect(Collectors.toMap(symbol -> symbol.name, symbol -> symbol));
        assertEquals(4, symbols.get("entry").location.get().address);
        assertEquals(a.toString(), symbols.get("VALUE").position.fileName);
        assertFalse(symbols.get("VALUE").location.isPresent());
        assertEquals(b.toString(), symbols.get("OTHER").position.fileName);
        assertFalse(symbols.get("OTHER").location.isPresent());
        assertTrue(map.matches(a, aText));
    }

    @Test
    public void sourceAnnotationsOutsideCapturedInputClosureRejectTheWholeSnapshot() throws Exception {
        Path root = source("main.asm", "nop\n");
        Path unknown = source("unseen.asm", "nop\n");
        Map<Path, String> inputs = SourceAddressMap.captureInputs(null, root);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, root, 4, 1, 0);
        annotate(memory, unknown, 8, 1, 0);

        assertThrows(IOException.class, () -> SourceAddressMap.capture(null, memory, null, inputs));
    }

    @Test
    public void editorMatchesNormalizeCrLfButCompileInputValidationRetainsExactText() throws Exception {
        Path source = source("windows.asm", "nop\r\ninr a\r\n");
        Map<Path, String> inputs = SourceAddressMap.captureInputs(null, source);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 4, 2, 0);
        SourceAddressMap map = SourceAddressMap.capture(null, memory, null, inputs);

        assertTrue(map.matches(source, "nop\ninr a\n"));
        assertFalse(map.matches(source, "nop\ninr b\n"));
        Files.writeString(source, "nop\ninr a\n");
        assertThrows(IOException.class, () -> SourceAddressMap.capture(null, memory, null, inputs));
    }

    @Test
    public void capturesCompileBankAndNormalizesCommonMemoryWithoutChangingSelection() throws Exception {
        Path source = source("main.asm", "nop\nnop\n");
        DebugMemory memory = new DebugMemory(32, 2, 16);
        memory.selected = 1;
        annotate(memory, source, 2, 1, 0);
        annotate(memory, source, 20, 2, 0);
        SourceAddressMap map = capture(null, memory, null, source);

        assertEquals(1, memory.selected);
        memory.selected = 0;
        assertEquals(1, map.addressOf(at(source, 1, 0)).get().bank);
        assertEquals(0, map.addressOf(at(source, 2, 0)).get().bank);
        assertFalse(map.sourceAt(2, 0).isPresent());
        assertEquals(2, map.sourceAt(2, 1).get().address);
        assertEquals(0, map.sourceAt(20, 1).get().bank);
        assertEquals(0, map.sourceAt(20, 0).get().bank);
        assertFalse(map.sourceAt(2, -1).isPresent());
        assertFalse(map.sourceAt(2, 2).isPresent());
        assertEquals(0, memory.selected);
    }

    @Test
    public void separateIncludePathsWithSameFilenameKeepTheirMappingsAndTextSnapshots() throws Exception {
        Path first = files.newFolder("first").toPath().resolve("code.asm");
        Path second = files.newFolder("second").toPath().resolve("code.asm");
        Files.writeString(first, "nop\n");
        Files.writeString(second, "inr a\n");
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, first, 2, 1, 0);
        annotate(memory, second, 4, 1, 0);
        SourceAddressMap map = capture(null, memory, null, first, second);

        assertEquals(2, map.addressOf(at(first, 1, 0)).get().address);
        assertEquals(4, map.addressOf(at(second, 1, 0)).get().address);
        assertTrue(map.matches(first.getParent().resolve("../first/code.asm"), "nop\n"));
        assertTrue(map.matches(second, "inr a\n"));
        assertFalse(map.matches(second, "inr b\n"));
        Files.writeString(second, "inr b\n"); // Saving an edit must not make the old source match.
        assertFalse(map.matches(second, Files.readString(second)));
        assertFalse(map.matches(files.getRoot().toPath().resolve("uncompiled.asm"), "nop\n"));
    }

    @Test
    public void multipleAddressesOnOneSourceLineAreNotSilentlyResolved() throws Exception {
        String text = "entry: nop\nnop\n";
        Path source = source("ambiguous.asm", text);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 0, 1, 7);
        annotate(memory, source, 3, 1, 7);
        annotate(memory, source, 4, 2, 0);
        Compiler compiler = compiler(text, definition(text, "entry:"));
        SourceAddressMap map = capture(compiler, memory, null, source);

        assertFalse(map.addressOf(at(source, 1, 0)).isPresent());
        assertFalse(map.symbols().get(0).location.isPresent());
        assertEquals(0, map.sourceAt(0, 0).get().address);
        assertEquals(3, map.sourceAt(3, 0).get().address);
    }

    @Test
    public void removedAndReplacedAnnotationObjectsInvalidateCapturedMappings() throws Exception {
        String text = "entry: nop\n";
        Path source = source("stale.asm", text);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 4, 1, 7);
        SourceAddressMap map = capture(compiler(text, definition(text, "entry:")), memory, null, source);
        assertTrue(map.symbols().get(0).location.isPresent());

        memory.annotations.removeAll(7, 4);
        assertFalse(map.sourceAt(4, 0).isPresent());
        assertFalse(map.addressOf(at(source, 1, 0)).isPresent());
        assertFalse(map.symbols().get(0).location.isPresent());
        annotate(memory, source, 4, 1, 7); // Equal data is a new compiler provenance, not the old mapping.
        assertFalse(map.addressOf(at(source, 1, 0)).isPresent());
        assertTrue(capture(null, memory, null, source).addressOf(at(source, 1, 0)).isPresent());
    }

    @Test
    public void symbolsMapSameLineAndStandaloneLabelsButDoNotGuessAcrossDirectives() throws Exception {
        String text = "same: nop\nstand:\n; comment\n  mvi a, 1\nbeforeOrg:\norg 20\nnop\nVALUE equ 42\nmac macro\nendm\n";
        Path source = source("symbols.asm", text);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 0, 1, 6);
        annotate(memory, source, 1, 4, 2);
        annotate(memory, source, 20, 7, 0);
        Compiler compiler = compiler(text,
                definition(text, "same:"), token(text, "nop", Token.RESERVED),
                definition(text, "stand:"), token(text, "; comment", Token.COMMENT),
                token(text, "mvi", Token.RESERVED), definition(text, "beforeOrg:"),
                token(text, "org", Token.PREPROCESSOR), definition(text, "VALUE"),
                definition(text, "mac"));
        SourceAddressMap map = capture(compiler, memory, null, source);
        Map<String, SourceAddressMap.Symbol> symbols = map.symbols().stream()
                .collect(Collectors.toMap(symbol -> symbol.name, symbol -> symbol));

        assertEquals(0, symbols.get("same").location.get().address);
        assertEquals(1, symbols.get("stand").location.get().address);
        assertEquals(1, map.addressOf(at(source, 2, 0)).get().address);
        assertFalse(symbols.get("beforeOrg").location.isPresent());
        assertFalse(symbols.get("VALUE").location.isPresent());
        assertFalse(symbols.get("mac").location.isPresent());
        assertFalse(map.addressOf(at(source, 5, 0)).isPresent());
        assertTrue(symbols.get("stand").toString().contains("symbols.asm:2"));
        assertTrue(symbols.get("VALUE").toString().contains("unmapped"));
    }

    @Test
    public void operandBytesUseCapturedInstructionStartAndDataMappingsStayExact() throws Exception {
        String text = "lxi h, 1234h\ndb 00h, 00h\n";
        Path source = source("operands.asm", text);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 4, 1, 0);
        annotate(memory, source, 7, 2, 0);
        Compiler compiler = compiler(text, token(text, "lxi", Token.RESERVED), token(text, "db", Token.PREPROCESSOR));
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.disassemble(4)).thenReturn(new DisassembledInstruction(4, "lxi h, 1234h", "21 34 12"));
        when(disassembler.getNextInstructionPosition(4)).thenReturn(7);
        SourceAddressMap map = capture(compiler, memory, disassembler, source);

        assertEquals(4, map.sourceAt(5, 0).get().address);
        assertEquals(4, map.sourceAt(6, 0).get().address);
        assertEquals(1, map.sourceAt(6, 0).get().position.line);
        assertEquals(7, map.sourceAt(7, 0).get().address);
        assertFalse(map.sourceAt(8, 0).isPresent());
        verify(disassembler, never()).disassemble(5);
        verify(disassembler, never()).disassemble(6);
        verify(disassembler, never()).disassemble(7);
        verify(disassembler, never()).getNextInstructionPosition(5);
        verify(disassembler, never()).getNextInstructionPosition(6);
    }

    @Test
    public void instructionSpanStopsAtNextMappingEvenWhenThatMappingIsAmbiguous() throws Exception {
        String text = "lxi h, 1234h\nnop\nnop\n";
        Path source = source("overlap.asm", text);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 4, 1, 0);
        annotate(memory, source, 6, 2, 0);
        annotate(memory, source, 6, 3, 0);
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.disassemble(4)).thenReturn(new DisassembledInstruction(4, "lxi h, 1234h", "21 34 12"));
        when(disassembler.getNextInstructionPosition(4)).thenReturn(7);
        SourceAddressMap map = capture(compiler(text, token(text, "lxi", Token.RESERVED)), memory, disassembler, source);

        assertEquals(4, map.sourceAt(5, 0).get().address);
        assertFalse(map.sourceAt(6, 0).isPresent());
    }

    @Test
    public void decodingFailureAndUnsupportedSemanticAnalysisKeepOnlyExactMappings() throws Exception {
        String text = "nop\n";
        Path source = source("unknown.asm", text);
        DebugMemory memory = new DebugMemory(32, 1, 0);
        annotate(memory, source, 4, 1, 0);
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.disassemble(4)).thenThrow(new InvalidInstructionException());
        SourceAddressMap map = capture(compiler(text, token(text, "nop", Token.RESERVED)), memory, disassembler, source);
        assertTrue(map.sourceAt(4, 0).isPresent());
        assertFalse(map.sourceAt(5, 0).isPresent());

        Compiler unsupported = mock(Compiler.class);
        LexicalAnalyzer lexer = mock(LexicalAnalyzer.class);
        when(unsupported.createLexer()).thenReturn(lexer);
        when(lexer.analyze(text)).thenThrow(new UnsupportedOperationException());
        SourceAddressMap exact = capture(unsupported, memory, disassembler, source);
        assertTrue(exact.addressOf(at(source, 1, 0)).isPresent());
        assertTrue(exact.symbols().isEmpty());
    }

    @Test
    public void resizedMemoryRejectsTheOldAddressMap() throws Exception {
        Path source = source("resize.asm", "nop\n");
        MemoryContext<?> memory = mock(MemoryContext.class);
        Annotations annotations = new Annotations();
        annotations.add(4, new SourceCodeAnnotation(7, at(source, 1, 0)));
        when(memory.annotations()).thenReturn(annotations);
        when(memory.getSize()).thenReturn(32);
        when(memory.getBanksCount()).thenReturn(1);
        SourceAddressMap map = capture(null, memory, null, source);
        assertTrue(map.sourceAt(4, 0).isPresent());
        when(memory.getSize()).thenReturn(64);
        assertFalse(map.sourceAt(4, 0).isPresent());
        assertFalse(map.addressOf(at(source, 1, 0)).isPresent());
        assertFalse(SourceAddressMap.empty().sourceAt(4, 0).isPresent());
    }

    private Path source(String name, String text) throws Exception {
        Path path = files.getRoot().toPath().resolve(name);
        Files.writeString(path, text);
        return path;
    }

    private SourceCodePosition at(Path file, int line, int column) {
        return SourceCodePosition.of(line, column, file.toString());
    }

    private void annotate(DebugMemory memory, Path file, int address, int line, int column) {
        memory.annotations.add(address, new SourceCodeAnnotation(7, at(file, line, column)));
    }

    private Compiler compiler(String text, Token... tokens) {
        return compiler(Map.of(text, List.of(tokens)));
    }

    private Compiler compiler(Map<String, List<Token>> sources) {
        Compiler compiler = mock(Compiler.class);
        when(compiler.createLexer()).thenAnswer(call -> {
            LexicalAnalyzer lexer = mock(LexicalAnalyzer.class);
            when(lexer.analyze(anyString())).thenAnswer(analysis -> sources.getOrDefault(analysis.getArgument(0), List.of()));
            return lexer;
        });
        return compiler;
    }

    private SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory, Disassembler disassembler,
                                     Path... sources) throws Exception {
        Map<Path, String> inputs = new LinkedHashMap<>();
        for (Path source : sources) inputs.putAll(SourceAddressMap.captureInputs(compiler, source));
        return SourceAddressMap.capture(compiler, memory, disassembler, inputs);
    }

    private Token include(String text, String file) {
        return new SourceToken(Token.INCLUDE_PATH, text.indexOf('"'), '"' + file + '"', null, "", null, file);
    }

    private Token definition(String text, String value) {
        return new SourceToken(Token.SYMBOL_DEFINITION, text.indexOf(value), value,
                value.endsWith(":") ? value.substring(0, value.length() - 1) : value, "", "");
    }

    private Token token(String text, String value, int type) {
        return new SourceToken(type, text.indexOf(value), value, null, null, null);
    }
}
