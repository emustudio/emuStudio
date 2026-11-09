/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.application.debugger.InstructionIndex;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DebugTableModelImplTest {

    @Test
    public void defaultColumnsContainOnlyCpuPresentation() {
        DebugTableModelImpl model = new DebugTableModelImpl();

        model.setCPU(createCpu(0));

        assertEquals(3, model.getColumnCount());
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals("address", model.getColumnName(0));
        assertEquals("instruction", model.getColumnName(1));
        assertEquals("opcode", model.getColumnName(2));
    }

    @Test
    public void cpuProvidedColumnsReplaceDefaultColumns() {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<String> column = new TestColumn<>(String.class, "Label", false);
        CPU cpu = createCpu(0);
        when(cpu.getDebuggerColumns()).thenReturn(List.of(column));

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
    public void breakpointColumnIsNotEditableForRowsWithoutInstructionLocation() {
        DebugTableModelImpl model = new DebugTableModelImpl();
        CPU cpu = createCpu(0);
        when(cpu.getAddressSpaceSize()).thenReturn(1);
        model.setCPU(cpu);

        model.setDebuggerColumns(List.of(new TestColumn<>(Boolean.class, "Breakpoint", true)));

        assertTrue(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(1, 0));
    }

    @Test
    public void debuggerHonorsCpuAddressSpaceBound() {
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
    public void browsingAndPagingLeaveCpuLocationAndCurrentInstructionMarkerIndependent() {
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
    public void resizingBrowsedPageRetainsItsAnchorAndRebuildsAdjacentPages() {
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
    public void previousInstructionAtBoundaryDoesNotGuessAnOperandAfterStepping() {
        CPU cpu = createCpu(0);
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(call ->
                (Integer) call.getArgument(0) == 0 ? 2 : (Integer) call.getArgument(0) + 1);
        when(cpu.getDisassembler()).thenReturn(disassembler);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(cpu);
        model.setMaxRows(7);
        assertEquals(0, model.getLocationAt(0));
        when(cpu.getInstructionLocation()).thenReturn(2);
        model.currentPage();
        assertEquals(0, model.guessPreviousInstructionLocation());
    }

    @Test
    public void resizingUnpaintedBrowsedPageRetainsItsCenter() {
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(createCpu(0));
        model.setMaxRows(9);
        model.showAddress(0);
        model.nextPage();
        model.setMaxRows(5);
        assertEquals(12, model.getLocationAt(2));
    }

    @Test
    public void repeatedColumnsAndSelectionReuseMaterializedPageWithoutDecodingBoundaries() {
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
    public void currentIndexedInstructionCoverageSuppliesLengthsWithoutLiveDecode() {
        ProgramIndex index = ProgramIndex.create(16, new MemoryAnnotations());
        try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(false);
            for (int at = 0; at < 16; at += 2)
                edit.putInstruction(new ProgramIndex.Range(at, at + 2), ProgramIndex.InstructionKind.LINEAR);
            edit.commit();
        }
        AtomicInteger calls = new AtomicInteger();
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> { throw new AssertionError("Unexpected live decode"); }, 4, 16));
        model.setMaxRows(3);
        model.setInstructionIndex(new InstructionIndex(index::snapshot));
        assertEquals(2, model.getLocationAt(0));
        assertEquals(4, model.getLocationAt(1));
        assertEquals(6, model.getLocationAt(2));
        assertEquals(0, calls.get());

        model.memoryChanged(2, 2); // A table-only cache reset retains the same authoritative view.
        assertEquals(2, model.getLocationAt(0));
        assertEquals(4, model.getLocationAt(1));
        assertEquals(6, model.getLocationAt(2));
        assertEquals(0, calls.get());
    }

    @Test
    public void cpuAddressesBeyondSmallerMemoryIndexRetainDecoderFallback() {
        ProgramIndex index = ProgramIndex.create(8, new MemoryAnnotations());
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(new AtomicInteger(), at -> at + 1, 12, 16));
        model.setMaxRows(3);
        model.setInstructionIndex(new InstructionIndex(index::snapshot));
        model.showAddress(0);
        model.showAddress(12);
        assertEquals(12, model.getLocationAt(1));
        assertEquals(13, model.getLocationAt(2));
    }

    @Test
    public void revisionChangeInvalidatesOperandCellAndFallbackWithoutUiNotification() {
        ProgramIndex index = ProgramIndex.create(16, new MemoryAnnotations());
        try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(false);
            edit.putInstruction(new ProgramIndex.Range(0, 2), ProgramIndex.InstructionKind.LINEAR);
            for (int at = 2; at < 16; at++)
                edit.putInstruction(new ProgramIndex.Range(at, at + 1), ProgramIndex.InstructionKind.LINEAR);
            edit.commit();
        }
        AtomicInteger calls = new AtomicInteger(), length = new AtomicInteger(2);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at == 0 ? length.get() : at + 1, 0, 16));
        model.setMaxRows(3);
        model.setInstructionIndex(new InstructionIndex(index::snapshot));
        assertEquals(2, model.getLocationAt(1));
        int firstCalls = calls.get();
        length.set(3); // Encoding changed in cell1 of instruction [0,2), not at its start.
        index.memoryChanged(); // UI memory notifications are disabled; root revision is the signal.
        assertEquals(3, model.getLocationAt(1));
        assertTrue(calls.get() > firstCalls);
    }

    @Test
    public void previousInstructionRefreshesRevisionBeforeAnyTableCellIsRead() {
        ProgramIndex index = ProgramIndex.create(16, new MemoryAnnotations());
        AtomicInteger calls = new AtomicInteger(), length = new AtomicInteger(2);
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at == 0 ? length.get() : at == 2 ? 4 : at + 1, 4, 16));
        model.setMaxRows(3);
        model.setInstructionIndex(new InstructionIndex(index::snapshot));
        model.showAddress(0);
        model.currentPage();
        assertEquals(2, model.guessPreviousInstructionLocation());
        int previousCalls = calls.get();
        length.set(3);
        index.memoryChanged();
        assertEquals(3, model.guessPreviousInstructionLocation());
        assertTrue(calls.get() > previousCalls);
    }

    @Test
    public void instructionViewChangesLengthsAtTheSamePcAfterPublication() {
        ProgramIndex index = ProgramIndex.create(16, new MemoryAnnotations());
        try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(false);
            for (int at = 0; at < 16; at += 2)
                edit.putInstruction(new ProgramIndex.Range(at, at + 2), ProgramIndex.InstructionKind.LINEAR);
            edit.commit();
        }
        AtomicInteger calls = new AtomicInteger();
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> { throw new AssertionError("Unexpected live decode"); }, 0, 16));
        model.setMaxRows(3);
        model.setInstructionIndex(new InstructionIndex(index::snapshot));
        assertEquals(2, model.getLocationAt(1));
        try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.replaceFlow(false);
            edit.putInstruction(new ProgramIndex.Range(0, 3), ProgramIndex.InstructionKind.LINEAR);
            edit.putInstruction(new ProgramIndex.Range(3, 6), ProgramIndex.InstructionKind.LINEAR);
            edit.putInstruction(new ProgramIndex.Range(6, 8), ProgramIndex.InstructionKind.LINEAR);
            for (int at = 8; at < 16; at += 2)
                edit.putInstruction(new ProgramIndex.Range(at, at + 2), ProgramIndex.InstructionKind.LINEAR);
            edit.commit();
        }
        assertEquals(3, model.getLocationAt(1));
        model.showAddress(8);
        assertEquals(8, model.getLocationAt(1));
        assertEquals(10, model.getLocationAt(2));
        model.showAddress(12);
        assertEquals(14, model.getLocationAt(2));
        assertEquals(0, calls.get());
    }

    @Test
    public void breakpointOnlyPublicationKeepsMaterializedInstructionPage() {
        ProgramIndex index = ProgramIndex.create(16, new MemoryAnnotations());
        AtomicInteger calls = new AtomicInteger();
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(countedCpu(calls, at -> at + 1, 4, 16));
        model.setMaxRows(3);
        model.setInstructionIndex(new InstructionIndex(index::snapshot));
        assertEquals(4, model.getLocationAt(1));
        int initialCalls = calls.get();
        try (ProgramIndex.BreakpointUpdate edit = index.updateBreakpoints(index.snapshot())) {
            edit.add(4);
            edit.commit();
        }
        assertEquals(4, model.getLocationAt(1));
        assertEquals(initialCalls, calls.get());
    }

    private CPU countedCpu(AtomicInteger calls, java.util.function.IntUnaryOperator next, int pc, int size) {
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(call -> {
            calls.incrementAndGet();
            int at = call.getArgument(0);
            if (at < 0 || at >= size) throw new IndexOutOfBoundsException();
            return next.applyAsInt(at);
        });
        CPU cpu = mock(CPU.class);
        when(cpu.getDisassembler()).thenReturn(disassembler);
        when(cpu.getInstructionLocation()).thenReturn(pc);
        when(cpu.getAddressSpaceSize()).thenReturn(size);
        return cpu;
    }

    private CPU createCpu(int instructionLocation) {
        Disassembler disassembler = new LinearDisassembler();
        CPU cpu = mock(CPU.class);
        when(cpu.getDisassembler()).thenReturn(disassembler);
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

    private static final class LinearDisassembler implements Disassembler {
        @Override
        public DisassembledInstruction disassemble(int memoryPosition) {
            return new DisassembledInstruction(memoryPosition, "nop", "00");
        }

        @Override
        public int getNextInstructionPosition(int memoryPosition) {
            return memoryPosition + 1;
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
