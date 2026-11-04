/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.SourceToken;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.ArrayList;
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

        SourceAddressMap map = capture(compiler(), memory,
                Map.of(source, "NOP\nHALT\n"));

        SourceAddressMap.Location first = map.addressOf(SourceCodePosition.of(1, 0, source.toString())).orElseThrow();
        assertEquals(1, first.address);
        assertEquals(1, first.bank);
        assertTrue(map.sourceAt(1, 0).isEmpty());
        assertEquals(1, map.sourceAt(1, 1).orElseThrow().position.line);
        assertEquals(0, map.sourceAt(6, 1).orElseThrow().bank);
        assertEquals(2, map.sourceAt(6, 0).orElseThrow().position.line);
        assertTrue(map.sourceAt(2, 1).isEmpty());
        when(memory.getSelectedBank()).thenReturn(0);
        assertTrue(map.sourceAt(1, 1).isPresent());
    }

    @Test
    public void pointCompilerMetadataDoesNotInventInstructionCoverage() throws Exception {
        Path source = source("program.asm", "LD A,1\nDB 2\n");
        Compiler compiler = compiler("LD A,1\nDB 2\n", List.of(
                new SourceToken(Token.RESERVED, 0, "LD", null, null, null)));
        MemoryContext<Byte> memory = memory(8, 1, 8, 0,
                Map.of(0, Set.of(annotation(source, 1, 0)), 2, Set.of(annotation(source, 2, 0))));

        SourceAddressMap map = capture(compiler, memory, Map.of(source, "LD A,1\nDB 2\n"));

        assertTrue(map.sourceAt(0, 0).orElseThrow().instruction);
        assertTrue(map.sourceAt(1, 0).isEmpty());
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
        SourceAddressMap map = capture(compiler(), memory, Map.of(source, "NOP\n"));
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
        SourceAddressMap map = capture(compiler(), memory, Map.of(source, "NOP\n"));

        when(memory.getSize()).thenReturn(8);
        when(memory.getAddressSpaceSize()).thenReturn(8);

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
    }

    @Test
    public void sparseSourceCaptureUsesAddressCapacityInsteadOfLoadedOccupancy() throws Exception {
        Path source = source("sparse.asm", "HALT\n");
        int address = Integer.MAX_VALUE - 1;
        MemoryContext<Byte> memory = memory(Integer.MAX_VALUE, 1, 0, 0,
                Map.of(address, Set.of(annotation(source, 1, 0))));
        when(memory.getSize()).thenReturn(0);

        SourceAddressMap map = capture(compiler(), memory, Map.of(source, "HALT\n"));

        assertEquals(Integer.MAX_VALUE, map.programIndex().snapshot().layout().addressSpaceSize());
        assertEquals(address, map.sourceAt(address, 0).orElseThrow().address);
        when(memory.getSize()).thenReturn(1);
        assertEquals(address, map.sourceAt(address, 0).orElseThrow().address);
        verify(memory, never()).readBank(anyInt(), anyInt());
    }

    @Test
    public void multipleAddressesOnOneSourceLineHaveNoUniqueForwardMapping() throws Exception {
        Path source = source("program.asm", "DB 1,2\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0,
                Map.of(0, Set.of(annotation(source, 1, 3)), 1, Set.of(annotation(source, 1, 5))));

        SourceAddressMap map = capture(compiler(), memory, Map.of(source, "DB 1,2\n"));

        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
        assertEquals(3, map.sourceAt(0, 0).orElseThrow().position.column);
        assertEquals(5, map.sourceAt(1, 0).orElseThrow().position.column);
    }

    @Test
    public void multipleAnnotationsAtOneAddressRemainIndexedButNavigationRequiresUniqueSource() throws Exception {
        Path source = source("program.asm", "NOP\nHALT\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0,
                Map.of(0, Set.of(annotation(source, 1, 0), annotation(source, 2, 0))));

        SourceAddressMap map = capture(compiler(), memory,
                Map.of(source, "NOP\nHALT\n"));

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertEquals(0, map.addressOf(SourceCodePosition.of(1, 0, source.toString())).orElseThrow().address);
        assertEquals(0, map.addressOf(SourceCodePosition.of(2, 0, source.toString())).orElseThrow().address);
        List<ProgramIndex.Mapping> mappings = new ArrayList<>();
        map.programIndex().snapshot().forEachMappingAt(0, 0, mappings::add);
        assertEquals(2, mappings.size());
    }

    @Test
    public void capturedSourceOffsetsUseUtf16AcrossCrLfUnicodeAndTabs() throws Exception {
        String text = ";😀\r\n\t\"😀\" NOP\r\n";
        Path source = source("unicode.asm", text);
        int opcodeOffset = text.indexOf("NOP");
        Compiler compiler = compiler(text, List.of(
                new SourceToken(Token.RESERVED, opcodeOffset, "NOP", null, null, null)));
        // ANTLR compiler column counts code points; the supplementary character occupies one column.
        MemoryContext<Byte> memory = memory(8, 1, 8, 0,
                Map.of(0, Set.of(annotation(source, 2, 5))));

        SourceAddressMap map = capture(compiler, memory, Map.of(source, text));

        assertEquals(6, map.sourceAt(0, 0).orElseThrow().position.column);
        List<ProgramIndex.Mapping> mappings = new ArrayList<>();
        map.programIndex().snapshot().forEachMappingAt(0, 0, mappings::add);
        ProgramIndex.SourceSpan span = mappings.get(0).source();
        assertEquals(source.toUri().toString(), span.fileId);
        assertEquals(opcodeOffset, span.startOffset);
        assertEquals(opcodeOffset + 3, span.endOffset);
        assertTrue(map.diagnostics().isEmpty());
    }

    @Test
    public void explicitUtf16AndCodePointConventionsResolveDifferentSupplementaryOffsets() throws Exception {
        String text = "😀AB\n";
        Path source = source("column-units.asm", text);
        List<Token> tokens = List.of(new SourceToken(Token.RESERVED, 2, "A", null, null, null),
                new SourceToken(Token.RESERVED, 3, "B", null, null, null));
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 2))));
        Compiler utf16 = compiler(text, tokens);
        when(utf16.getSourcePositionColumnUnit()).thenReturn(SourceCodePosition.ColumnUnit.UTF16);

        SourceAddressMap utf16Map = capture(utf16, memory, Map.of(source, text));
        SourceAddressMap codePointMap = capture(compiler(text, tokens), memory, Map.of(source, text));

        assertEquals(2, utf16Map.sourceAt(0, 0).orElseThrow().position.column);
        assertEquals(3, codePointMap.sourceAt(0, 0).orElseThrow().position.column);
        assertTrue(utf16Map.diagnostics().isEmpty());
        assertTrue(codePointMap.diagnostics().isEmpty());
    }

    @Test
    public void unknownConventionExcludesBindingsButPreservesSourceOnlySymbolsAndUserMetadata() throws Exception {
        String text = "value EQU 1\n";
        Path source = source("unknown-units.asm", text);
        Compiler compiler = compiler(text, List.of(
                new SourceToken(Token.SYMBOL_DEFINITION, 0, "value", "value", null, null)));
        when(compiler.getSourcePositionColumnUnit()).thenReturn(SourceCodePosition.ColumnUnit.UNKNOWN);
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 6))));
        ProgramIndex index = ProgramIndex.create("unknown-units", 4, 1, 4);
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.putAnnotation(99, 1, new ProgramIndex.Range(0, 2, 3), ProgramIndex.Kinds.LABEL, "user");
            update.commit();
        }

        SourceAddressMap map = capture(compiler, memory, Map.of(source, text), index);

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertTrue(map.addressOf(SourceCodePosition.of(1, 6, source.toString())).isEmpty());
        assertEquals(1, map.diagnostics().size());
        assertTrue(map.diagnostics().get(0).contains("does not declare a source column convention"));
        assertEquals("value", map.symbols().get(0).name);
        assertTrue(map.symbols().get(0).sourceOnly);
        List<ProgramIndex.Annotation> labels = new ArrayList<>();
        index.snapshot().forEachAnnotation(a -> {
            if (a.kind().equals(ProgramIndex.Kinds.LABEL) && a.text().equals("user")) labels.add(a);
        });
        assertEquals(1, labels.size());
        SourceAddressMap noCompiler = capture(null, memory, Map.of(source, text), index);
        assertTrue(noCompiler.sourceAt(0, 0).isEmpty());
        assertTrue(noCompiler.diagnostics().get(0).contains("does not declare"));
    }

    @Test
    public void unsupportedCoordinatesAndEofHaveDiagnosticsInsteadOfClampedBindings() throws Exception {
        String text = "NOP\r\n";
        Path source = source("invalid.asm", text);
        MemoryContext<Byte> memory = memory(8, 1, 8, 0, Map.of(
                0, Set.of(annotation(source, 1, 0)),
                1, Set.of(annotation(source, 1, 3)),
                2, Set.of(annotation(source, 2, 0)),
                3, Set.of(annotation(source, 0, 0)),
                4, Set.of(annotation(source, 1, Integer.MAX_VALUE))));

        SourceAddressMap map = capture(compiler(), memory, Map.of(source, text));

        List<ProgramIndex.Mapping> mappings = new ArrayList<>();
        for (int address = 0; address < 5; address++)
            map.programIndex().snapshot().forEachMappingAt(0, address, mappings::add);
        assertEquals(1, mappings.size());
        assertEquals(0, mappings.get(0).source().startOffset);
        assertEquals(1, mappings.get(0).source().endOffset);
        assertEquals(4, map.diagnostics().size());
        assertTrue(map.diagnostics().get(0).contains("EOF has no extent"));
    }

    @Test
    public void memoryRevisionMakesBindingsHistoricalWithoutErasingProvenance() throws Exception {
        Path source = source("history.asm", "NOP\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 0))));
        ProgramIndex index = ProgramIndex.create("image", 4, 1, 4);
        SourceAddressMap map = capture(compiler(), memory, Map.of(source, "NOP\n"), index);
        ProgramIndex.Snapshot old = index.snapshot();

        index.memoryChanged();

        assertTrue(map.sourceAt(0, 0).isEmpty());
        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, source.toString())).isEmpty());
        List<ProgramIndex.Mapping> mappings = new ArrayList<>();
        old.forEachMappingAt(0, 0, mappings::add);
        assertEquals(1, mappings.size());
        assertEquals(old.memoryRevision(), mappings.get(0).memoryRevision());
        assertNotEquals(index.snapshot().memoryRevision(), mappings.get(0).memoryRevision());
    }

    @Test
    public void refreshingSourceOwnerPreservesOtherOwnersAndRejectsOldSourceRevision() throws Exception {
        Path source = source("recompile.asm", "NOP\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 0))));
        ProgramIndex index = ProgramIndex.create("image", 4, 1, 4);
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.putMapping(99, 1, new ProgramIndex.Range(0, 3, 4),
                    new ProgramIndex.SourceSpan("other:source", 0, 0, 1));
            update.putAnnotation(99, 1, new ProgramIndex.Range(0, 2, 3), ProgramIndex.Kinds.LABEL, "user");
            assertEquals(ProgramIndex.CommitResult.PUBLISHED, update.commit());
        }
        SourceAddressMap first = capture(compiler(), memory, Map.of(source, "NOP\n"), index);
        List<ProgramIndex.Mapping> previous = new ArrayList<>();
        index.snapshot().forEachMappingAt(0, 0, previous::add);

        SourceAddressMap second = capture(compiler(), memory, Map.of(source, "NOP\n"), index);

        assertTrue(first.sourceAt(0, 0).isEmpty());
        assertTrue(second.sourceAt(0, 0).isPresent());
        List<ProgramIndex.Mapping> stale = new ArrayList<>();
        index.snapshot().forEachMappingOverlappingSource(previous.get(0).source(), stale::add);
        assertTrue(stale.isEmpty());
        List<ProgramIndex.Mapping> other = new ArrayList<>();
        index.snapshot().forEachMappingAt(0, 3, other::add);
        assertEquals(99, other.get(0).ownerId());
        List<ProgramIndex.Annotation> annotations = new ArrayList<>();
        index.snapshot().forEachAnnotation(a -> {
            if (a.kind().equals(ProgramIndex.Kinds.LABEL) && a.text().equals("user")) annotations.add(a);
        });
        assertEquals(1, annotations.size());
    }

    @Test
    public void sameBasenameInDifferentDirectoriesAndRepeatedSourceSpanStayDistinct() throws Exception {
        Path left = temporary.newFolder("left").toPath().resolve("same.asm");
        Path right = temporary.newFolder("right").toPath().resolve("same.asm");
        Files.writeString(left, "NOP\n");
        Files.writeString(right, "NOP\n");
        MemoryContext<Byte> memory = memory(8, 1, 8, 0, Map.of(
                0, Set.of(annotation(left, 1, 0)), 1, Set.of(annotation(left, 1, 0)),
                2, Set.of(annotation(right, 1, 0))));
        SourceAddressMap map = capture(compiler(), memory, Map.of(left, "NOP\n", right, "NOP\n"));

        assertTrue(map.addressOf(SourceCodePosition.of(1, 0, left.toString())).isEmpty());
        assertEquals(2, map.addressOf(SourceCodePosition.of(1, 0, right.toString())).orElseThrow().address);
        List<ProgramIndex.Mapping> atLeft = new ArrayList<>();
        map.programIndex().snapshot().forEachMappingAt(0, 0, atLeft::add);
        List<ProgramIndex.Mapping> repeated = new ArrayList<>();
        map.programIndex().snapshot().forEachMappingOverlappingSource(atLeft.get(0).source(), repeated::add);
        assertEquals(2, repeated.size());
    }

    @Test
    public void memoryChangeDuringCaptureRejectsPublication() throws Exception {
        Path source = source("race.asm", "NOP\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 0))));
        ProgramIndex index = ProgramIndex.create("image", 4, 1, 4);
        when(memory.annotations().getAll(SourceCodeAnnotation.class)).thenAnswer(call -> {
            index.memoryChanged();
            return Map.of(0, Set.of(annotation(source, 1, 0)));
        });

        IOException failure = assertThrows(IOException.class,
                () -> capture(compiler(), memory, Map.of(source, "NOP\n"), index));

        assertTrue(failure.getMessage().contains("changed during capture"));
        List<ProgramIndex.Mapping> mappings = new ArrayList<>();
        for (int address = 0; address < 4; address++)
            index.snapshot().forEachMappingAt(0, address, mappings::add);
        assertTrue(mappings.isEmpty());
    }

    @Test
    public void explicitCompilerBankSurvivesResetAndDoesNotRequireByteCells() throws Exception {
        Path source = source("words.asm", "LOAD 1\n");
        @SuppressWarnings("unchecked") MemoryContext<Integer> memory = mock(MemoryContext.class);
        MemoryContextAnnotations annotations = mock(MemoryContextAnnotations.class);
        SourceCodeAnnotation annotation = annotation(source, 1, 0);
        when(memory.getSize()).thenReturn(8);
        when(memory.getAddressSpaceSize()).thenReturn(8);
        when(memory.getBanksCount()).thenReturn(2);
        when(memory.getCommonBoundary()).thenReturn(4);
        when(memory.getCellTypeClass()).thenReturn(Integer.class);
        when(memory.getSelectedBank()).thenReturn(0);
        when(memory.annotations()).thenReturn(annotations);
        when(annotations.getAll(SourceCodeAnnotation.class)).thenReturn(Map.of(1, Set.of(annotation)));
        when(annotations.get(1, SourceCodeAnnotation.class)).thenReturn(Set.of(annotation));
        ProgramIndex index = ProgramIndex.create("words", 8, 2, 4);

        SourceAddressMap map = SourceAddressMap.capture(memory, SourceInputs.of(null, Map.of(source, "LOAD 1\n")), index, 1,
                SourceCodePosition.ColumnUnit.CODE_POINT);

        assertTrue(map.sourceAt(1, 0).isEmpty());
        assertEquals(1, map.sourceAt(1, 1).orElseThrow().bank);
        verify(memory, never()).read(anyInt());
        verify(memory, never()).readBank(anyInt(), anyInt());
    }

    @Test
    public void staleCompilerInputIsRejectedDuringCapture() throws Exception {
        Path source = source("program.asm", "HALT\n");

        IOException failure = assertThrows(IOException.class,
                () -> capture(null, null, Map.of(source, "NOP\n")));

        assertTrue(failure.getMessage().contains("Source changed during compilation"));
    }

    @Test
    public void annotationFromUncapturedInputIsRejected() throws Exception {
        Path source = source("program.asm", "NOP\n");
        Path included = source("include.asm", "HALT\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0,
                Map.of(0, Set.of(annotation(included, 1, 0))));

        IOException failure = assertThrows(IOException.class,
                () -> capture(null, memory, Map.of(source, "NOP\n")));

        assertTrue(failure.getMessage().contains("Source was not captured before compilation"));
    }

    @Test
    public void editorLineEndingNormalizationDoesNotHideExternalSourceChanges() throws Exception {
        Path source = source("program.asm", "NOP\r\nHALT\r\n");
        SourceAddressMap map = capture(null, null,
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

        SourceInputs inputs = SourceInputs.capture(compiler, root);
        SourceAddressMap.capture(compiler, null, inputs, null, 0);

        assertEquals(List.of(root, child), List.copyOf(inputs.files().keySet()));
        assertEquals("include child.asm\n", inputs.files().get(root).text);
        assertEquals("include root.asm\n", inputs.files().get(child).text);
        assertThrows(UnsupportedOperationException.class, () -> inputs.files().remove(root));
        verify(lexer, times(1)).analyze("include child.asm\n");
        verify(lexer, times(1)).analyze("include root.asm\n");
    }

    @Test
    public void memoryWithoutProgramIndexIsRejected() throws Exception {
        Path source = source("program.asm", "NOP\n");
        MemoryContext<Byte> memory = memory(4, 1, 4, 0, Map.of(0, Set.of(annotation(source, 1, 0))));

        assertThrows(IllegalArgumentException.class, () -> SourceAddressMap.capture(compiler(), memory,
                SourceInputs.of(null, Map.of(source, "NOP\n")), null, 0));
    }

    @Test
    public void touchedButIdenticalInputStaysUnchangedUntilContentDiffers() throws Exception {
        Path source = source("program.asm", "NOP\n");
        SourceAddressMap map = capture(null, null, Map.of(source, "NOP\n"));
        FileTime later = FileTime.fromMillis(Files.getLastModifiedTime(source).toMillis() + 10_000);

        Files.setLastModifiedTime(source, later);
        assertTrue(map.inputsUnchanged());

        Files.writeString(source, "RET\n");
        Files.setLastModifiedTime(source, FileTime.fromMillis(later.toMillis() + 10_000));
        assertFalse(map.inputsUnchanged());
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

        SourceAddressMap map = capture(compiler, memory, Map.of(source, text));

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
        List<ProgramIndex.Annotation> labels = new ArrayList<>();
        map.programIndex().snapshot().forEachAnnotation(a -> {
            if (a.kind().equals(ProgramIndex.Kinds.LABEL) && a.text().equals("entry")) labels.add(a);
        });
        assertEquals(1, labels.size());
        assertEquals(0, labels.get(0).range().start);
    }

    private static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory, Map<Path, String> texts)
            throws IOException {
        ProgramIndex index = memory == null ? null : ProgramIndex.create("test", memory.getAddressSpaceSize(),
                memory.getBanksCount(), memory.getCommonBoundary());
        return capture(compiler, memory, texts, index);
    }

    private static SourceAddressMap capture(Compiler compiler, MemoryContext<?> memory, Map<Path, String> texts,
                                            ProgramIndex index) throws IOException {
        return SourceAddressMap.capture(compiler, memory, SourceInputs.of(compiler, texts), index,
                memory == null ? 0 : memory.getSelectedBank());
    }

    private Path source(String name, String text) throws IOException {
        Path file = temporary.newFile(name).toPath().toAbsolutePath().normalize();
        Files.writeString(file, text);
        return file;
    }

    private static SourceCodeAnnotation annotation(Path file, int line, int column) {
        return new SourceCodeAnnotation(1, SourceCodePosition.of(line, column, file.toString()));
    }

    private static Compiler compiler() {
        Compiler compiler = mock(Compiler.class);
        when(compiler.getSourcePositionColumnUnit()).thenReturn(SourceCodePosition.ColumnUnit.CODE_POINT);
        return compiler;
    }

    private static Compiler compiler(String text, List<Token> tokens) {
        Compiler compiler = compiler();
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
        when(memory.getAddressSpaceSize()).thenReturn(size);
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
