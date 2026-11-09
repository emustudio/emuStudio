/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.application.debugger.DebuggerSession;
import org.junit.After;
import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class DebugTableModelImplTest {

    @Test
    public void defaultColumnsContainOnlyCpuPresentation() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();

        model.setCPU(createCpu(0));

        assertEquals(3, model.getColumnCount());
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals("address", model.getColumnName(0));
        assertEquals("instruction", model.getColumnName(1));
        assertEquals("opcode", model.getColumnName(2));
    }

    @Test
    public void cpuProvidedColumnsReplaceDefaultColumns() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<String> column = new TestColumn<>(String.class, "Label", false);
        CPU cpu = createCpu(0);
        when(cpu.getDebuggerColumns(any(Decoder.class))).thenReturn(List.of(column));

        model.setCPU(cpu);

        assertEquals(1, model.getColumnCount());
        assertEquals(column, model.getColumnAt(0));
    }

    @Test
    public void rowCountAndColumnMetadataFollowInjectedState() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<Boolean> breakpoints = new TestColumn<>(Boolean.class, "Breakpoint", true);
        TestColumn<String> mnemo = new TestColumn<>(String.class, "Mnemonic", false);
        model.setDebuggerColumns(Arrays.asList(breakpoints, mnemo));

        assertEquals(0, model.getRowCount());
        assertEquals(2, model.getColumnCount());
        assertEquals(breakpoints, model.getColumnAt(0));
        assertEquals("Mnemonic", model.getColumnName(1));
        assertEquals(String.class, model.getColumnClass(1));

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        when(ida.getInstructionsPerPage()).thenReturn(11);
        setField(model, "ida", ida);

        assertEquals(11, model.getRowCount());
    }

    @Test
    public void pagingAndMemoryOperationsDelegateToDisassemblerAndFireEvents() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        setField(model, "ida", ida);
        AtomicInteger eventCount = new AtomicInteger();
        model.addTableModelListener(event -> eventCount.incrementAndGet());

        model.setMaxRows(7);
        model.previousPage();
        model.seekBackwardPage(2);
        model.firstPage();
        model.nextPage();
        model.seekForwardPage(3);
        model.lastPage();
        model.currentPage();
        model.memoryChanged(4, 9);

        verify(ida).setInstructionsPerPage(7);
        verify(ida, times(3)).pagePrevious();
        verify(ida, times(4)).pageNext();
        verify(ida).pageFirst();
        verify(ida).pageLast();
        verify(ida).pageCurrent();
        verify(ida).flushCache(4, 10);
        assertTrue(eventCount.get() >= 9);
    }

    @Test
    public void getValueAtUsesEmptyValuesWhenLocationIsUnknown() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<Boolean> breakpoints = new TestColumn<>(Boolean.class, "Breakpoint", true);
        TestColumn<String> mnemo = new TestColumn<>(String.class, "Mnemonic", false);
        model.setDebuggerColumns(Arrays.asList(breakpoints, mnemo));

        assertNull(model.getValueAt(0, 0));

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(12);
        when(ida.rowToLocation(12, 0)).thenReturn(-1);
        setField(model, "ida", ida);
        setField(model, "cpu", cpu);

        assertEquals(Boolean.FALSE, model.getValueAt(0, 0));
        assertNull(model.getValueAt(0, 1));
    }

    @Test
    public void getValueAtAndSetValueAtUseResolvedLocation() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<String> mnemo = new TestColumn<>(String.class, "Mnemonic", true);
        mnemo.putValue(33, "nop");
        model.setDebuggerColumns(List.of(mnemo));

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(7);
        when(ida.rowToLocation(7, 1)).thenReturn(33);
        when(ida.rowToLocation(7, 2)).thenReturn(-1);
        setField(model, "ida", ida);
        setField(model, "cpu", cpu);

        assertEquals("nop", model.getValueAt(1, 0));

        model.setValueAt("ldi", 1, 0);
        assertEquals(33, mnemo.lastSetLocation);
        assertEquals("ldi", mnemo.lastSetValue);

        model.setValueAt(123, 1, 0);
        assertEquals(33, mnemo.lastSetLocation);
        assertEquals("ldi", mnemo.lastSetValue);

        model.setValueAt("ignored", 2, 0);
        assertEquals(33, mnemo.lastSetLocation);
    }

    @Test
    public void setValueAtSwallowsIllegalArgumentException() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<String> column = new TestColumn<>(String.class, "Mnemonic", true);
        column.throwOnSet = new IllegalArgumentException("bad value");
        model.setDebuggerColumns(List.of(column));

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(9);
        when(ida.rowToLocation(9, 0)).thenReturn(40);
        setField(model, "ida", ida);
        setField(model, "cpu", cpu);

        model.setValueAt("bad", 0, 0);

        assertEquals(40, column.lastSetLocation);
        assertEquals("bad", column.lastSetValue);
    }

    @Test
    public void breakpointColumnIsNotEditableForRowsWithoutInstructionLocation() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        CPU cpu = createCpu(0);
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        model.setCPU(cpu);

        model.setDebuggerColumns(List.of(new TestColumn<>(Boolean.class, "Breakpoint", true)));

        assertTrue(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(1, 0));
    }

    @Test
    public void debuggerHonorsCpuAddressSpaceBound() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(createCpu(255));

        assertNotNull(model.getValueAt(PaginatingDisassembler.INSTR_PER_PAGE / 2, 0));
        assertNull(model.getValueAt(PaginatingDisassembler.INSTR_PER_PAGE / 2 + 1, 0));
    }

    @Test
    public void cellEditabilityCurrentRowAndGuessPreviousLocationFollowDisassemblerState() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<Boolean> editable = new TestColumn<>(Boolean.class, "Breakpoint", true);
        TestColumn<String> readOnly = new TestColumn<>(String.class, "Mnemonic", false);
        model.setDebuggerColumns(Arrays.asList(editable, readOnly));

        assertFalse(model.isCellEditable(0, 0));
        assertFalse(model.isRowAtCurrentInstruction(0));
        assertEquals(-1, model.getLocationAt(0));
        assertEquals(0, model.guessPreviousInstructionLocation());

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(20);
        when(ida.rowToLocation(20, 1)).thenReturn(-1);
        when(ida.rowToLocation(20, 2)).thenReturn(100);
        when(ida.rowToLocation(20, 3)).thenReturn(-1);
        when(ida.getCurrentInstructionRow()).thenReturn(4);
        setField(model, "ida", ida);
        setField(model, "cpu", cpu);

        assertFalse(model.isCellEditable(1, 0));
        assertFalse(model.isCellEditable(2, 1));
        assertTrue(model.isCellEditable(2, 0));
        assertFalse(model.isRowAtCurrentInstruction(2));
        assertEquals(100, model.getLocationAt(2));
        assertEquals(19, model.guessPreviousInstructionLocation());

        when(ida.rowToLocation(20, 3)).thenReturn(18);
        assertEquals(18, model.guessPreviousInstructionLocation());
    }

    @Test
    public void browsingAndPagingLeaveCpuLocationAndCurrentInstructionMarkerIndependent() throws Exception {
        CPU cpu = createCpu(20);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(cpu);
        model.setMaxRows(5);

        model.showAddress(100);
        assertEquals(100, model.getLocationAt(2));
        assertFalse(model.isRowAtCurrentInstruction(2));
        model.nextPage();
        assertTrue(model.getLocationAt(2) > 100);

        model.currentPage();
        assertEquals(20, model.getLocationAt(2));
        assertTrue(model.isRowAtCurrentInstruction(2));
        assertEquals(20, cpu.getInstructionLocation());
        verify(cpu, org.mockito.Mockito.never()).setInstructionLocation(anyInt());
        assertThrows(IllegalArgumentException.class, () -> model.showAddress(-1));
        assertThrows(IllegalArgumentException.class, () -> model.showAddress(256));
        model.setMaxRows(0);
        model.showAddress(255);
        assertEquals(1, model.getRowCount());
        assertEquals(255, model.getLocationAt(0));
    }

    @Test
    public void resizingBrowsedPageRetainsItsAnchorAndRebuildsAdjacentPages() throws Exception {
        CPU cpu = createCpu(0);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(cpu);
        model.setMaxRows(9);
        model.showAddress(0);
        assertEquals(8, model.getLocationAt(8));
        model.nextPage();
        assertEquals(8, model.getLocationAt(0));
        int anchor = model.getLocationAt(4);
        model.setMaxRows(9); // A width-only resize must not move the browsed page.
        assertEquals(anchor, model.getLocationAt(4));
        model.setMaxRows(5);
        assertEquals(anchor, model.getLocationAt(2));
        int first = model.getLocationAt(0);
        model.previousPage();
        assertEquals(first, model.getLocationAt(4));
        model.nextPage();
        assertEquals(first, model.getLocationAt(0));
        assertEquals(0, cpu.getInstructionLocation());
        verify(cpu, org.mockito.Mockito.never()).setInstructionLocation(anyInt());
    }

    @Test
    public void previousInstructionAtBoundaryDoesNotGuessAnOperandAfterStepping() throws Exception {
        CPU cpu = createCpu(0);
        Decoder disassembler = mock(Decoder.class);
        when(disassembler.decode(anyInt())).thenAnswer(call ->
                MockHelper.instruction(call.getArgument(0), (Integer) call.getArgument(0) == 0 ? 2 : 1));
        when(cpu.getDecoder()).thenReturn(disassembler);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(cpu);
        model.setMaxRows(7);
        assertEquals(0, model.getLocationAt(0));
        when(cpu.getInstructionLocation()).thenReturn(2);
        model.currentPage();
        assertEquals(0, model.guessPreviousInstructionLocation());
    }

    @Test
    public void resizingUnpaintedBrowsedPageRetainsItsCenter() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(createCpu(0));
        model.setMaxRows(9);
        model.showAddress(0);
        model.nextPage();
        model.setMaxRows(5);
        assertEquals(12, model.getLocationAt(2));
    }

    @Test
    public void repeatedColumnsAndSelectionReuseMaterializedPageWithoutDecodingBoundaries() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CPU cpu = countedCpu(calls, at -> at + 1, 128, 256);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(cpu);
        model.setMaxRows(21);
        model.setDebuggerColumns(List.of(DebuggerColumn.address("%04X"), DebuggerColumn.address("%04X")));
        model.showAddress(0);
        model.showAddress(128);
        int firstCalls = calls.get();
        for (int repeat = 0; repeat < 20; repeat++) for (int row = 0; row < 21; row++) {
            assertNotNull("row " + row, model.getValueAt(row, 0));
            assertNotNull("row " + row, model.getValueAt(row, 1));
            assertTrue(model.getLocationAt(row) >= 0);
            model.isRowAtCurrentInstruction(row);
        }
        model.setMaxRows(21); // Width-only resize/selection must retain the same address page.
        model.getLocationAt(20);
        assertEquals(firstCalls, calls.get());
    }

    @Test
    public void historicalObservedInstructionLengthsNeverReplaceLiveListingDecode() throws Exception {
        DebuggerSession session = session(16);
        analyze(session, at -> at + 2);
        AtomicInteger calls = new AtomicInteger();
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at + 1, 0, 16));
        model.setMaxRows(3);
        model.setDebuggerSession(session);

        assertEquals(1, model.getLocationAt(1));
        assertTrue(calls.get() > 0);
        assertEquals(2, session.flowAnalysis().nextAddress(0));
    }

    @Test
    public void cpuAddressesBeyondSmallerMemoryIndexRetainDecoderFallback() throws Exception {
        DebuggerSession session = session(8);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(new AtomicInteger(), at -> at + 1, 12, 16));
        model.setMaxRows(3);
        model.setDebuggerSession(session);
        model.showAddress(0);
        model.showAddress(12);
        assertEquals(12, model.getLocationAt(1));
        assertEquals(13, model.getLocationAt(2));
    }

    @Test
    public void revisionChangeInvalidatesOperandCellAndFallbackWithoutUiNotification() throws Exception {
        DebuggerSession session = session(16);
        analyze(session, at -> at == 0 ? 2 : at + 1);
        AtomicInteger calls = new AtomicInteger(), length = new AtomicInteger(2);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at == 0 ? length.get() : at + 1, 0, 16));
        model.setMaxRows(3);
        model.setDebuggerSession(session);
        assertEquals(2, model.getLocationAt(1));
        int firstCalls = calls.get();
        length.set(3); // Encoding changed in cell1 of instruction [0,2), not at its start.
        session.runMutation(() -> {}); // UI memory notifications are disabled; root revision is the signal.
        assertEquals(3, model.getLocationAt(1));
        assertTrue(calls.get() > firstCalls);
    }

    @Test
    public void previousInstructionRefreshesRevisionBeforeAnyTableCellIsRead() throws Exception {
        DebuggerSession session = session(16);
        AtomicInteger calls = new AtomicInteger(), length = new AtomicInteger(2);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at == 0 ? length.get() : at == 2 ? 4 : at + 1, 4, 16));
        model.setMaxRows(3);
        model.setDebuggerSession(session);
        model.showAddress(0);
        model.currentPage();
        assertEquals(2, model.guessPreviousInstructionLocation());
        int previousCalls = calls.get();
        length.set(3);
        session.runMutation(() -> {});
        assertEquals(3, model.guessPreviousInstructionLocation());
        assertTrue(calls.get() > previousCalls);
    }

    @Test
    public void selectingObservedGraphViewsDoesNotInvalidateLiveListingPage() throws Exception {
        DebuggerSession session = session(16);
        analyze(session, at -> at + 2);
        AtomicInteger calls = new AtomicInteger();
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at + 1, 0, 16));
        model.setMaxRows(3);
        model.setDebuggerSession(session);
        assertEquals(1, model.getLocationAt(1));
        int previous = calls.get();

        session.analyzeFlow(4, 10);

        assertEquals(1, model.getLocationAt(1));
        assertEquals(previous, calls.get());
    }

    @Test
    public void breakpointOnlyPublicationKeepsMaterializedInstructionPage() throws Exception {
        DebuggerSession session = session(16);
        AtomicInteger calls = new AtomicInteger();
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at + 1, 4, 16));
        model.setMaxRows(3);
        model.setDebuggerSession(session);
        assertEquals(4, model.getLocationAt(1));
        int initialCalls = calls.get();
        session.setBreakpoint(4, true).join();
        assertEquals(4, model.getLocationAt(1));
        assertEquals(initialCalls, calls.get());
    }

    @Test
    public void defaultAndCustomColumnsShareCapturedRowsAndRefreshOnMemoryViewChanges() throws Exception {
        for (boolean custom : new boolean[] {false, true}) {
            AtomicInteger bank = new AtomicInteger();
            Map<Integer, Integer> reads = new HashMap<>();
            Decoder decoder = address -> {
                reads.merge(address, 1, Integer::sum);
                return DecodedInstruction.of(address, 1, "BANK " + bank.get(), "ENC " + bank.get());
            };
            CPU cpu = createCpu(0);
            when(cpu.getDecoder()).thenReturn(decoder);
            when(cpu.getDebuggerColumns(any(Decoder.class))).thenAnswer(call -> {
                Decoder captured = call.getArgument(0);
                return custom ? List.of(DebuggerColumn.text("label", address -> "LABEL"),
                        DebuggerColumn.mnemo(captured), DebuggerColumn.opcode(captured)) : List.of();
            });
            DebuggerSession session = session(16);
            DebugTableModelImpl model = new DebugTableModelImpl();
            model.setCPU(cpu);
            model.setMaxRows(3);
            model.setDebuggerSession(session);

            for (int row = 0; row < 3; row++) {
                int address = model.getLocationAt(row);
                assertEquals("BANK 0", model.getValueAt(row, 1));
                assertEquals("ENC 0", model.getValueAt(row, 2));
                assertEquals(Integer.valueOf(1), reads.get(address));
            }
            bank.set(1);
            assertEquals("ENC 0", model.getValueAt(1, 2));
            session.runMutation(() -> {});
            assertEquals("BANK 1", model.getValueAt(1, 1));
            assertEquals("ENC 1", model.getValueAt(1, 2));
            assertEquals(Integer.valueOf(2), reads.get(1));
            bank.set(2);
            model.memoryChanged(0, 15);
            assertEquals("BANK 2", model.getValueAt(1, 1));
            assertEquals("ENC 2", model.getValueAt(1, 2));
            assertEquals(Integer.valueOf(3), reads.get(1));
        }
    }

    private final java.util.List<DebuggerSession> sessions = new java.util.ArrayList<>();

    @After public void closeSessions() throws Exception { sessions.forEach(DebuggerSession::close); }

    private DebuggerSession session(int size) {
        CPU cpu = mock(CPU.class, withSettings().extraInterfaces(net.emustudio.emulib.plugins.debugger.Debugging.class));
        when(((net.emustudio.emulib.plugins.debugger.Debugging) cpu).observe(any(), any(net.emustudio.emulib.plugins.debugger.Debugging.Capture.class)))
                .thenReturn(() -> {});
        when(cpu.getAddressSpaceSize()).thenReturn(size);
        try {
            DebuggerSession session = new DebuggerSession(cpu, null, null);
            sessions.add(session);
            return session;
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }

    private void analyze(DebuggerSession session, java.util.function.IntUnaryOperator next) {
        for (int address = 0; address < session.addressSpaceSize(); ) {
            int destination = next.applyAsInt(address);
            session.afterStep(true, java.util.Optional.of(MockHelper.instruction(address, destination - address)),
                    java.util.OptionalInt.of(destination), java.util.Optional.empty());
            address = destination;
        }
        session.analyzeFlow(0, session.addressSpaceSize());
    }

    private CPU countedCpu(AtomicInteger calls, java.util.function.IntUnaryOperator next, int pc, int size) throws Exception {
        Decoder disassembler = mock(Decoder.class);
        when(disassembler.decode(anyInt())).thenAnswer(call -> {
            calls.incrementAndGet();
            int at = call.getArgument(0);
            if (at < 0 || at >= size) throw new IndexOutOfBoundsException();
            return MockHelper.instruction(at, next.applyAsInt(at) - at);
        });
        CPU cpu = mock(CPU.class);
        when(cpu.getDecoder()).thenReturn(disassembler);
        when(cpu.getInstructionLocation()).thenReturn(pc);
        when(cpu.getAddressSpaceSize()).thenReturn(size);
        return cpu;
    }

    private CPU createCpu(int instructionLocation) {
        Decoder disassembler = new LinearDisassembler();
        CPU cpu = mock(CPU.class);
        when(cpu.getDecoder()).thenReturn(disassembler);
        when(cpu.getInstructionLocation()).thenReturn(instructionLocation);
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        return cpu;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        if (fieldName.equals("ida") && ((PaginatingDisassembler) value).getInstructionsPerPage() == 0)
            when(((PaginatingDisassembler) value).getInstructionsPerPage()).thenReturn(PaginatingDisassembler.INSTR_PER_PAGE);
        Field field = DebugTableModelImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class LinearDisassembler implements Decoder {
        @Override
        public DecodedInstruction decode(int address) {
            return MockHelper.instruction(address, 1);
        }
    }

    private static final class TestColumn<T> implements DebuggerColumn<T> {
        private final Class<T> classType;
        private final String title;
        private final boolean editable;
        private final Map<Integer, T> values = new HashMap<>();
        private int lastSetLocation = -1;
        private Object lastSetValue;
        private IllegalArgumentException throwOnSet;

        private TestColumn(Class<T> classType, String title, boolean editable) {
            this.classType = classType;
            this.title = title;
            this.editable = editable;
        }

        private void putValue(int location, T value) {
            values.put(location, value);
        }

        @Override
        public Class<T> getClassType() {
            return classType;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public boolean isEditable() {
            return editable;
        }

        @Override
        public void setValue(int location, Object value) {
            lastSetLocation = location;
            lastSetValue = value;
            if (throwOnSet != null) {
                throw throwOnSet;
            }
            values.put(location, classType.cast(value));
        }

        @Override
        public T getValue(int location) {
            return values.get(location);
        }
    }
}
