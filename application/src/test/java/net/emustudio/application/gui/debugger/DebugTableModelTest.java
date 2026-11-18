/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.TestCpu;
import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
import net.emustudio.emulib.plugin.cpu.*;
import net.emustudio.emulib.plugin.debugger.Debugging;
import org.junit.After;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DebugTableModelTest {
    private final List<DebuggerSession> sessions = new ArrayList<>();

    @After public void closeSessions() { sessions.forEach(session -> { session.close(); }); }

    private DebuggerSession session(int size, int pc) throws Exception {
        CPU cpu = TestCpu.mockCpu(withSettings().extraInterfaces(Debugging.class));
        when(cpu.getAddressSpaceSize()).thenReturn(size);
        when(TestCpu.context(cpu).getInstructionLocation()).thenReturn(pc);
        when(((Debugging) cpu).observe(any(), any(Debugging.Capture.class))).thenReturn(() -> {});
        when(cpu.getDecoder()).thenReturn(address -> DecodedInstruction.of(address, 1, "NOP", "00"));
        DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null);
        sessions.add(session);
        return session;
    }

    private DebugTableModel model(DebuggerSession session, int rows) {
        DebugTableModel model = new DebugTableModel();
        model.setCPU(session.cpu, TestCpu.context(session.cpu));
        model.setDebuggerSession(session);
        model.setMaxRows(rows);
        return model;
    }

    private void executed(DebuggerSession session, int at, int length, int next) {
        session.afterStep(true, Optional.of(DecodedInstruction.of(at, length, "EXEC", "00")),
                OptionalInt.of(next), Optional.empty());
    }

    @Test public void columnMetadataUsesCpuPresentationAndEmptyRowsStayEmpty() throws Exception {
        DebugTableModel empty = new DebugTableModel();
        assertEquals(0, empty.getRowCount());
        assertNull(empty.getValueAt(0, 0));
        DebuggerSession session = session(8, 0);
        DebugTableModel model = model(session, 5);
        assertEquals(5, model.getColumnCount());
        assertEquals("label", model.getColumnName(0));
        assertEquals("address", model.getColumnName(1));
        assertEquals("instruction", model.getColumnName(2));
        assertEquals("opcode", model.getColumnName(3));
        assertEquals("flow", model.getColumnName(4));
        assertFalse(model.isCellEditable(2, 4));
        assertEquals("", model.getValueAt(2, 4));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals("", model.getValueAt(0, 0));
        assertFalse(model.isCellEditable(0, 0));
        DebuggerColumn<String> custom = DebuggerColumn.text("Label", at -> "label-" + at);
        when(session.cpu.getDebuggerColumns(any(Decoder.class))).thenReturn(List.of(custom));
        model.setCPU(session.cpu, TestCpu.context(session.cpu));
        assertSame(custom, model.getColumnAt(1));
        assertEquals("flow", model.getColumnName(2));
        assertEquals("label-0", model.getValueAt(2, 1));
    }

    @Test public void pcStaysCenteredAtStartAndEndAndAfterResize() throws Exception {
        DebuggerSession session = session(8, 0);
        DebugTableModel model = model(session, 5);
        assertEquals(-1, model.getLocationAt(0));
        assertEquals(-1, model.getLocationAt(1));
        assertEquals(0, model.getLocationAt(2));
        assertTrue(model.isRowAtCurrentInstruction(2));
        when(TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(7);
        model.currentPage();
        assertEquals(7, model.getLocationAt(2));
        assertEquals(-1, model.getLocationAt(3));
        assertEquals(-1, model.getLocationAt(4));
        model.setMaxRows(9);
        assertEquals(7, model.getLocationAt(4));
        assertTrue(model.isRowAtCurrentInstruction(4));
        model.setMaxRows(0);
        assertEquals(1, model.getRowCount());
        assertEquals(7, model.getLocationAt(0));
    }

    @Test public void sharedObservedInstructionsSurviveWritesAndShowBehindPc() throws Exception {
        DebuggerSession session = session(256, 104);
        executed(session, 0, 3, 3);
        executed(session, 3, 2, 100);
        executed(session, 100, 4, 104);
        DebugTableModel model = model(session, 7);
        assertEquals(List.of(0, 3, 100), List.of(model.getLocationAt(0), model.getLocationAt(1), model.getLocationAt(2)));
        assertEquals(104, model.getLocationAt(3));
        assertTrue(model.isRowAtCurrentInstruction(3));
        assertEquals(105, model.getLocationAt(4));
        session.runMutation(() -> {}, java.util.concurrent.CompletableFuture.completedFuture(null));
        model.memoryChanged(1, 1);
        assertEquals(0, model.getLocationAt(0));
        assertEquals(3, model.getLocationAt(1));
        assertEquals(100, model.getLocationAt(2));
        assertEquals(100, model.guessPreviousInstructionLocation());
    }

    @Test public void steppingRetainsObservedHistory() throws Exception {
        DebuggerSession session = session(32, 0);
        DebugTableModel model = model(session, 5);
        assertEquals(0, model.getLocationAt(2));
        executed(session, 0, 3, 3);
        when(TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(3);
        model.currentPage();
        assertEquals(0, model.getLocationAt(1));
        assertEquals(3, model.getLocationAt(2));
        executed(session, 3, 2, 5);
        when(TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(5);
        model.currentPage();
        assertEquals(0, model.getLocationAt(0));
        assertEquals(3, model.getLocationAt(1));
        assertEquals(5, model.getLocationAt(2));
    }

    @Test public void historyHasNoViewportOrAnalysisSizeLimit() throws Exception {
        DebuggerSession session = session(10000, 5000);
        for (int at = 0; at < 5000; at++) executed(session, at, 1, at + 1);
        DebugTableModel model = model(session, 5);
        assertEquals(4998, model.getLocationAt(0));
        model.firstPage();
        assertEquals(0, model.getLocationAt(2));
        model.showAddress(2500);
        assertEquals(2498, model.getLocationAt(0));
        model.currentPage();
        assertEquals(5000, model.getLocationAt(2));
        assertEquals(5000, session.observedInstructionsBefore(5000, Integer.MAX_VALUE).size());
    }

    @Test public void incompleteInstructionsAreNotAddedAndResetClearsSharedHistory() throws Exception {
        DebuggerSession session = session(16, 4);
        executed(session, 0, 2, 2);
        session.afterStep(false, Optional.of(DecodedInstruction.of(2, 2, "FAILED", "00")),
                OptionalInt.of(4), Optional.empty());
        DebugTableModel model = model(session, 5);
        assertEquals(-1, model.getLocationAt(0));
        assertEquals(0, model.getLocationAt(1));
        session.cpuReset();
        model.currentPage();
        assertEquals(-1, model.getLocationAt(1));
        assertEquals(4, model.getLocationAt(2));
    }

    @Test public void browsingPagingAndResizeLeaveCpuAndMarkerIndependent() throws Exception {
        DebuggerSession session = session(64, 20);
        for (int at = 0; at < 20; at++) executed(session, at, 1, at + 1);
        DebugTableModel model = model(session, 5);
        model.previousPage();
        assertEquals(16, model.getLocationAt(2));
        assertFalse(model.isRowAtCurrentInstruction(2));
        model.nextPage();
        assertEquals(20, model.getLocationAt(2));
        model.showAddress(30);
        model.setMaxRows(9);
        assertEquals(30, model.getLocationAt(4));
        model.currentPage();
        assertEquals(20, model.getLocationAt(4));
        verify(session.cpu, never()).setInstructionLocation(anyInt());
        assertThrows(IllegalArgumentException.class, () -> model.showAddress(-1));
        assertThrows(IllegalArgumentException.class, () -> model.showAddress(64));
    }

    private List<Integer> locations(DebugTableModel model) {
        List<Integer> result = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) result.add(model.getLocationAt(row));
        return result;
    }

    @Test public void foldsObservedRepeatsAndRevealsExactNavigationTargets() throws Exception {
        DebuggerSession session = session(32, 12);
        for (int at = 0; at < 12; at++) executed(session, at, 1, at + 1);
        DebugTableModel model = model(session, 7);

        model.setFoldRepeats(true);
        assertEquals(List.of(-1, -1, 0, 12, 13, 14, 15), locations(model));
        assertEquals("0000–000B", model.getValueAt(2, 1));
        assertEquals("NOP × 12", model.getValueAt(2, 2));
        assertEquals("00", model.getValueAt(2, 3));
        assertEquals(2, model.rowForAddress(8));
        assertTrue(model.isRowAtCurrentInstruction(3));

        model.showAddress(7);
        assertEquals(7, model.getLocationAt(3));
        assertFalse(model.isFolded(3));
        assertEquals("NOP × 7", model.getValueAt(2, 2));
        assertEquals("NOP × 4", model.getValueAt(4, 2));
        model.setFoldRepeats(false);
        assertEquals(List.of(4, 5, 6, 7, 8, 9, 10), locations(model));
        verify(session.cpu, never()).setInstructionLocation(anyInt());
    }

    @Test public void individualRunsExpandAndCollapseWhileOtherRunsStayFolded() throws Exception {
        DebuggerSession session = session(32, 0);
        for (int at = 0; at < 20; at++) executed(session, at, 1, at + 1);
        when(session.cpu.getDecoder()).thenReturn(at -> DecodedInstruction.of(at, 1, at == 8 ? "OTHER" : "NOP", "00"));
        DebugTableModel model = model(session, 7);
        model.setFoldRepeats(true);
        assertEquals("NOP × 7", model.getValueAt(4, 2));
        assertEquals("NOP × 11", model.getValueAt(6, 2));

        model.toggleFoldAt(4);
        assertEquals(List.of(-1, -1, -1, 0, 1, 2, 3), locations(model));
        assertTrue(model.hasFoldControl(4));
        assertFalse(model.isFolded(4));
        model.toggleFoldAt(4);
        assertEquals("NOP × 7", model.getValueAt(4, 2));
        assertEquals("NOP × 11", model.getValueAt(6, 2));
    }

    @Test public void foldBoundariesKeepPcBreakpointsLabelsSymbolsAndAnnotationEdgesVisible() throws Exception {
        DebuggerSession session = session(40, 30);
        for (int at = 0; at < 30; at++) executed(session, at, 1, at + 1);
        session.setBreakpoint(12, true).join();
        session.config.annotations().add(new net.emustudio.application.emulation.debugger.UserAnnotations.Entry(
                -1, 18, 22, "region", "CODE"));
        DebugTableModel model = model(session, 41);
        model.setPresentation(at -> at == 5 ? "label" : "", () -> true, () -> true);
        model.setFoldBoundaries(() -> Set.of(25));
        model.showAddress(0);
        model.setFoldRepeats(true);
        for (int at : List.of(0, 5, 12, 18, 22, 25, 30)) {
            int row = model.rowForAddress(at);
            assertTrue("Missing protected address " + at, row >= 0);
            assertFalse("Folded protected address " + at, model.isFolded(row));
        }
        assertTrue(model.isFolded(model.rowForAddress(2)));
        assertTrue(model.isRowAtCurrentInstruction(model.rowForAddress(30)));
    }

    @Test public void foldedPagesCountDisplayedRunsAndRoundTripAcrossBothEnds() throws Exception {
        DebuggerSession session = session(25, 20);
        for (int at = 0; at < 20; at++) executed(session, at, 1, at + 1);
        when(session.cpu.getDecoder()).thenReturn(at -> DecodedInstruction.of(at, 1, at % 4 == 3 ? "OTHER" : "NOP", "00"));
        DebugTableModel model = model(session, 5);
        model.setFoldRepeats(true);
        List<Integer> current = locations(model);
        assertEquals(List.of(16, 19, 20, 21, 22), current);
        model.previousPage();
        List<Integer> previous = locations(model);
        assertEquals(List.of(8, 11, 12, 15, 16), previous);
        model.nextPage();
        assertEquals(current, locations(model));
        model.firstPage();
        assertEquals(List.of(0, 3, 4, 7, 8), locations(model));
        model.nextPage();
        assertEquals(previous, locations(model));
        model.nextPage();
        model.nextPage();
        assertEquals(List.of(22, 23, 24, -1, -1), locations(model));
        model.nextPage();
        assertEquals(List.of(22, 23, 24, -1, -1), locations(model));
        model.previousPage();
        assertEquals(current, locations(model));
    }

    @Test public void expandingLongRunBeforePcKeepsItsStartVisibleAndPagingReversible() throws Exception {
        DebuggerSession session = session(128, 100);
        for (int at = 0; at < 100; at++) executed(session, at, 1, at + 1);
        DebugTableModel model = model(session, 7);
        model.setFoldRepeats(true);
        int row = model.rowForAddress(0);
        assertEquals(2, row);
        model.toggleFoldAt(row);
        assertEquals(0, model.getLocationAt(row));
        assertEquals(1, model.getLocationAt(row + 1));
        List<Integer> expanded = locations(model);
        model.nextPage();
        model.previousPage();
        assertEquals(expanded, locations(model));
        model.toggleFoldAt(row);
        assertEquals(0, model.getLocationAt(row));
        assertTrue(model.isFolded(row));
        assertTrue(model.isRowAtCurrentInstruction(row + 1));
    }

    @Test public void foldingRechecksLiveEncodingAndLengthAfterWritesAndClearsAfterReset() throws Exception {
        DebuggerSession session = session(32, 0);
        for (int at = 0; at < 16; at++) executed(session, at, 1, at + 1);
        AtomicInteger changed = new AtomicInteger(-1);
        when(session.cpu.getDecoder()).thenReturn(at -> DecodedInstruction.of(at,
                at == changed.get() ? 2 : 1, "NOP", at == changed.get() ? "00 01" : "00"));
        DebugTableModel model = model(session, 7);
        model.setFoldRepeats(true);
        assertEquals("NOP × 15", model.getValueAt(4, 2));
        changed.set(5);
        model.memoryChanged(5, 7);
        assertEquals("NOP × 4", model.getValueAt(4, 2));
        assertEquals(5, model.getLocationAt(5));
        assertFalse(model.isFolded(5));
        assertEquals(7, model.getLocationAt(6));
        assertEquals("NOP × 9", model.getValueAt(6, 2));

        session.cpuReset();
        model.currentPage();
        assertEquals(List.of(-1, -1, -1, 0, 1, 2, 3), locations(model));
        assertFalse(model.isFolded(4));
    }

    @Test public void unobservedInstructionsStaySeparateAndReadsReusePreparedFolds() throws Exception {
        DebuggerSession session = session(32, 0);
        AtomicInteger calls = new AtomicInteger(), boundaries = new AtomicInteger();
        when(session.cpu.getDecoder()).thenReturn(at -> {
            calls.incrementAndGet();
            return DecodedInstruction.of(at, 1, "NOP", "00");
        });
        DebugTableModel model = model(session, 5);
        model.setFoldBoundaries(() -> { boundaries.incrementAndGet(); return Set.of(); });
        model.setFoldRepeats(true);
        assertEquals(List.of(-1, -1, 0, 1, 2), locations(model));
        for (int at = 0; at < 10; at++) executed(session, at, 1, at + 1);
        model.currentPage();
        assertEquals("NOP × 9", model.getValueAt(3, 2));
        int decoded = calls.get(), prepared = boundaries.get();
        for (int i = 0; i < 20; i++) {
            locations(model);
            model.getValueAt(3, 2);
        }
        assertEquals(decoded, calls.get());
        assertEquals(prepared, boundaries.get());
    }

    @Test public void sourceLineMappingsDoNotPreventFoldingAndNewBranchesRefreshIt() throws Exception {
        DebuggerSession session = session(16, 0);
        for (int at = 0; at < 8; at++) {
            executed(session, at, 1, at + 1);
            session.annotationEngine.put(new net.emustudio.emulib.application.annotations.MemoryAnnotations.Entry(
                    10, at, at, at + 1, "source", "", null));
        }
        DebugTableModel model = model(session, 7);
        model.setFoldRepeats(true);
        assertEquals("NOP × 7", model.getValueAt(4, 2));
        executed(session, 4, 1, 12);
        assertEquals("NOP × 4", model.getValueAt(4, 2));
        assertEquals("NOP × 3", model.getValueAt(5, 2));
    }

    @Test public void foldedSummaryCannotEditAnyMemberUntilExpanded() throws Exception {
        DebuggerSession session = session(16, 0);
        for (int at = 0; at < 8; at++) executed(session, at, 1, at + 1);
        AtomicInteger edited = new AtomicInteger(-1);
        DebuggerColumn<String> editable = new DebuggerColumn<>() {
            public String getTitle() { return "Editable"; }
            public Class<String> getClassType() { return String.class; }
            public boolean isEditable() { return true; }
            public String getValue(int address) { return "value-" + address; }
            public void setValue(int address, Object value) { edited.set(address); }
        };
        DebugTableModel model = model(session, 5);
        model.setDebuggerColumns(List.of(editable));
        model.setFoldRepeats(true);
        assertTrue(model.isFolded(3));
        assertFalse(model.isCellEditable(3, 1));
        model.setValueAt("blocked", 3, 1);
        assertEquals(-1, edited.get());
        model.toggleFoldAt(3);
        assertTrue(model.isCellEditable(3, 1));
        model.setValueAt("edit", 3, 1);
        assertEquals(1, edited.get());
    }

    @Test public void hiddenValuesMakePagingAndFoldingSafeWithoutDecoding() throws Exception {
        DebuggerSession session = session(32, 0);
        AtomicInteger decodes = new AtomicInteger();
        when(session.cpu.getDecoder()).thenReturn(at -> {
            decodes.incrementAndGet();
            return DecodedInstruction.of(at, 1, "NOP", "00");
        });
        DebugTableModel model = model(session, 5);
        model.setFoldRepeats(true);
        AtomicBoolean visible = new AtomicBoolean(true);
        model.setPresentation(at -> "", visible::get, visible::get);
        visible.set(false);
        int prepared = decodes.get();
        model.nextPage();
        model.previousPage();
        model.seekForwardPage(3);
        model.seekBackwardPage(3);
        model.firstPage();
        model.currentPage();
        model.showAddress(10);
        model.setMaxRows(7);
        model.toggleFoldAt(3);
        model.setFoldRepeats(false);
        assertNull(model.getValueAt(3, 2));
        assertEquals(prepared, decodes.get());
        assertTrue(model.isFoldRepeats());
    }

    @Test public void forwardPagesReplaceBothHalvesAndPreviousReturnsTheExactPage() throws Exception {
        DebuggerSession session = session(64, 20);
        for (int at = 0; at < 20; at++) executed(session, at, 1, at + 1);
        DebugTableModel model = model(session, 5);
        assertEquals(List.of(18, 19, 20, 21, 22), locations(model));
        model.nextPage();
        assertEquals(List.of(22, 23, 24, 25, 26), locations(model));
        model.nextPage();
        assertEquals(List.of(26, 27, 28, 29, 30), locations(model));
        model.previousPage();
        assertEquals(List.of(22, 23, 24, 25, 26), locations(model));
        model.previousPage();
        assertEquals(List.of(18, 19, 20, 21, 22), locations(model));
        assertTrue(model.isRowAtCurrentInstruction(2));
    }

    @Test public void unexecutedVariableLengthPagesRoundTripWithoutHistory() throws Exception {
        DebuggerSession session = session(32, 0);
        when(session.cpu.getDecoder()).thenReturn(at -> DecodedInstruction.of(at, 2, "TWO", "00 00"));
        DebugTableModel model = model(session, 5);
        List<Integer> current = locations(model);
        assertEquals(List.of(-1, -1, 0, 2, 4), current);
        model.seekForwardPage(2);
        assertEquals(List.of(12, 14, 16, 18, 20), locations(model));
        model.previousPage();
        assertEquals(List.of(4, 6, 8, 10, 12), locations(model));
        model.previousPage();
        assertEquals(current, locations(model));
        model.showAddress(24);
        model.previousPage();
        assertEquals(List.of(12, 14, 16, 18, 20), locations(model));
        model.nextPage();
        assertEquals(List.of(20, 22, 24, 26, 28), locations(model));
    }

    @Test public void pagingAcrossSparseObservedInstructionsUsesTheSameOrderedStartsBothWays() throws Exception {
        DebuggerSession session = session(256, 104);
        executed(session, 0, 3, 3);
        executed(session, 3, 2, 100);
        executed(session, 100, 4, 104);
        DebugTableModel model = model(session, 3);
        assertEquals(List.of(100, 104, 105), locations(model));
        model.previousPage();
        assertEquals(List.of(0, 3, 100), locations(model));
        model.nextPage();
        assertEquals(List.of(100, 104, 105), locations(model));
        model.nextPage();
        assertEquals(List.of(105, 106, 107), locations(model));
        model.previousPage();
        assertEquals(List.of(100, 104, 105), locations(model));
    }

    @Test public void partialPagesAtBothEndsRemainReversible() throws Exception {
        DebuggerSession session = session(8, 3);
        for (int at = 0; at < 3; at++) executed(session, at, 1, at + 1);
        DebugTableModel model = model(session, 5);
        List<Integer> current = locations(model);
        assertEquals(List.of(1, 2, 3, 4, 5), current);
        model.nextPage();
        assertEquals(List.of(5, 6, 7, -1, -1), locations(model));
        model.nextPage();
        assertEquals(List.of(5, 6, 7, -1, -1), locations(model));
        model.previousPage();
        assertEquals(current, locations(model));
        model.previousPage();
        assertEquals(List.of(-1, -1, -1, 0, 1), locations(model));
        model.previousPage();
        assertEquals(List.of(-1, -1, -1, 0, 1), locations(model));
        model.nextPage();
        assertEquals(current, locations(model));
        model.firstPage();
        assertEquals(List.of(-1, -1, -1, 0, 1), locations(model));
    }

    @Test public void liveDecodeRemainsIndependentOfHistoricalInstructionLength() throws Exception {
        DebuggerSession session = session(16, 0);
        executed(session, 0, 2, 2);
        DebugTableModel model = model(session, 3);
        assertEquals(0, model.getLocationAt(1));
        assertEquals(1, model.getLocationAt(2));
    }

    @Test public void columnsReuseUnboundedDecodedCacheUntilMemoryChanges() throws Exception {
        DebuggerSession session = session(16, 0);
        AtomicInteger calls = new AtomicInteger(), length = new AtomicInteger(2);
        when(session.cpu.getDecoder()).thenReturn(at -> {
            calls.incrementAndGet();
            return DecodedInstruction.of(at, at == 0 ? length.get() : 1, "LIVE " + length.get(), "ENC");
        });
        DebugTableModel model = model(session, 3);
        assertEquals(2, model.getLocationAt(2));
        assertEquals("LIVE 2", model.getValueAt(1, 2));
        int initial = calls.get();
        for (int i = 0; i < 20; i++) {
            model.getValueAt(1, 3);
            model.showAddress(0);
            model.getValueAt(1, 2);
        }
        assertEquals(initial, calls.get());
        length.set(3);
        session.runMutation(() -> {}, java.util.concurrent.CompletableFuture.completedFuture(null));
        assertEquals(3, model.getLocationAt(2));
        assertEquals("LIVE 3", model.getValueAt(1, 2));
        assertTrue(calls.get() > initial);
    }

    @Test public void invalidCellsStayBrowsableAndDoNotDecodeOutsideAddressSpace() throws Exception {
        DebuggerSession session = session(2, 1);
        Decoder decoder = mock(Decoder.class);
        when(decoder.decode(1)).thenThrow(mock(InvalidInstructionException.class));
        when(session.cpu.getDecoder()).thenReturn(decoder);
        DebugTableModel model = model(session, 5);
        assertEquals(1, model.getLocationAt(2));
        assertEquals(-1, model.getLocationAt(3));
        verify(decoder, never()).decode(2);
    }

    @Test public void editableColumnsUseResolvedAddressAndIgnoreWrongTypes() throws Exception {
        DebuggerSession session = session(8, 2);
        AtomicInteger edited = new AtomicInteger(-1);
        DebuggerColumn<String> column = new DebuggerColumn<>() {
            public String getTitle() { return "Editable"; }
            public Class<String> getClassType() { return String.class; }
            public boolean isEditable() { return true; }
            public String getValue(int address) { return Integer.toString(address); }
            public void setValue(int address, Object value) { edited.set(address); }
        };
        DebugTableModel model = model(session, 3);
        model.setDebuggerColumns(List.of(column));
        assertFalse(model.isCellEditable(0, 1));
        assertTrue(model.isCellEditable(1, 1));
        model.setValueAt("edit", 1, 1);
        assertEquals(2, edited.get());
        model.setValueAt(42, 2, 1);
        model.setValueAt("ignored", 0, 1);
        assertEquals(2, edited.get());

        AtomicBoolean visible = new AtomicBoolean(true), editing = new AtomicBoolean(true);
        model.setPresentation(address -> "label-" + address, visible::get, editing::get);
        assertEquals("label-2", model.getValueAt(1, 0));
        assertFalse(model.isCellEditable(1, 0));
        model.setValueAt("read-only label", 2, 0);
        assertEquals(2, edited.get());
        editing.set(false);
        assertFalse(model.isCellEditable(2, 1));
        model.setValueAt("blocked", 2, 1);
        assertEquals(2, edited.get());
        visible.set(false);
        assertNull(model.getValueAt(1, 0));
        assertNull(model.getValueAt(1, 1));
    }
}
