/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.TestCpu;
import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.FlowAnalysis;
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

    @After public void closeSessions() { sessions.forEach(session -> { session.execution().close(); session.close(); }); }

    private DebuggerSession session(int size, int pc) throws Exception {
        CPU cpu = TestCpu.mockCpu(withSettings().extraInterfaces(Debugging.class));
        when(cpu.getAddressSpaceSize()).thenReturn(size);
        when(TestCpu.context(cpu).getInstructionLocation()).thenReturn(pc);
        when(((Debugging) cpu).observe(any(), any(Debugging.Capture.class))).thenReturn(() -> {});
        when(cpu.getDecoder()).thenReturn(address -> DecodedInstruction.of(address, 1, "NOP", "00"));
        DebuggerSession session = new DebuggerSession(net.emustudio.application.TestCpu.execution(cpu), cpu, null, null);
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
        assertEquals(4, model.getColumnCount());
        assertEquals("label", model.getColumnName(0));
        assertEquals("address", model.getColumnName(1));
        assertEquals("instruction", model.getColumnName(2));
        assertEquals("opcode", model.getColumnName(3));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals("", model.getValueAt(0, 0));
        assertFalse(model.isCellEditable(0, 0));
        DebuggerColumn<String> custom = DebuggerColumn.text("Label", at -> "label-" + at);
        when(session.cpu.getDebuggerColumns(any(Decoder.class))).thenReturn(List.of(custom));
        model.setCPU(session.cpu, TestCpu.context(session.cpu));
        assertSame(custom, model.getColumnAt(1));
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
        session.runMutation(() -> {});
        model.memoryChanged(1, 1);
        assertEquals(0, model.getLocationAt(0));
        assertEquals(3, model.getLocationAt(1));
        assertEquals(100, model.getLocationAt(2));
        assertEquals(100, model.guessPreviousInstructionLocation());
        session.analyzeFlow(0);
        assertNotNull(session.flowAnalysis().instructionAt(100));
    }

    @Test public void steppingRetainsHistoryWithoutRequiringInspectionAnalysis() throws Exception {
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
        assertNull(session.flowAnalysis());
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

    @Test public void inspectionRootDoesNotAffectListingOrTriggerDecode() throws Exception {
        DebuggerSession session = session(16, 4);
        for (int at = 0; at < 8; at++) executed(session, at, 1, at + 1);
        FlowAnalysis inspection = session.analyzeFlow(0);
        AtomicInteger calls = new AtomicInteger();
        when(session.cpu.getDecoder()).thenReturn(at -> {
            calls.incrementAndGet();
            return DecodedInstruction.of(at, 1, "NOP", "00");
        });
        DebugTableModel model = model(session, 5);
        assertEquals(2, model.getLocationAt(0));
        assertSame(inspection, session.flowAnalysis());
        int initial = calls.get();
        session.analyzeFlow(6);
        assertEquals(2, model.getLocationAt(0));
        assertEquals(initial, calls.get());
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
        session.analyzeFlow(0);
        assertEquals(2, session.flowAnalysis().nextAddress(0));
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
        session.runMutation(() -> {});
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
