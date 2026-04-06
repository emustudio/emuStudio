/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ui.debugger.AddressColumn;
import net.emustudio.emulib.runtime.ui.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.ui.debugger.CannotSetDebuggerValueException;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DebugTableModelImplTest {

    @Test
    public void defaultColumnsReflectBreakpointSupport() {
        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(createCpu(true, 0), () -> 32);

        assertEquals(4, model.getColumnCount());
        assertTrue(model.getColumnAt(0) instanceof BreakpointColumn);
        assertEquals(Boolean.class, model.getColumnClass(0));
        assertEquals("address", model.getColumnName(1));

        DebugTableModelImpl noBreakpoints = new DebugTableModelImpl();
        noBreakpoints.setCPU(createCpu(false, 0), () -> 32);

        assertEquals(3, noBreakpoints.getColumnCount());
        assertTrue(noBreakpoints.getColumnAt(0) instanceof AddressColumn);
        assertEquals(String.class, noBreakpoints.getColumnClass(0));
    }

    @Test
    public void setDefaultColumnsWithoutCpuLeavesColumnsUntouched() {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<String> column = new TestColumn<>(String.class, "Label", false);
        model.setDebuggerColumns(List.of(column));

        model.setDefaultColumns();

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
        model.memorySizeChanged(64);

        verify(ida).setInstructionsPerPage(7);
        verify(ida, times(3)).pagePrevious();
        verify(ida, times(4)).pageNext();
        verify(ida).pageFirst();
        verify(ida).pageLast();
        verify(ida).pageCurrent();
        verify(ida).flushCache(4, 10);
        assertTrue(eventCount.get() >= 10);
    }

    @Test
    public void getValueAtUsesEmptyValuesWhenLocationIsUnknown() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<Boolean> breakpoints = new TestColumn<>(Boolean.class, "Breakpoint", true);
        TestColumn<String> mnemo = new TestColumn<>(String.class, "Mnemonic", false);
        model.setDebuggerColumns(Arrays.asList(breakpoints, mnemo));

        assertNull(model.getValueAt(0, 0));

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(true, 12);
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
        CPU cpu = createCpu(false, 7);
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
    public void setValueAtSwallowsCannotSetDebuggerValueException() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<String> column = new TestColumn<>(String.class, "Mnemonic", true);
        column.throwOnSet = cannotSetValueException("bad value");
        model.setDebuggerColumns(List.of(column));

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(false, 9);
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
        CPU cpu = createCpu(true, 0);
        model.setCPU(cpu, () -> 32);

        assertFalse(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(PaginatingDisassembler.INSTR_PER_PAGE / 2, 0));
    }

    @Test
    public void cellEditabilityCurrentRowAndGuessPreviousLocationFollowDisassemblerState() throws Exception {
        DebugTableModelImpl model = new DebugTableModelImpl();
        TestColumn<Boolean> editable = new TestColumn<>(Boolean.class, "Breakpoint", true);
        TestColumn<String> readOnly = new TestColumn<>(String.class, "Mnemonic", false);
        model.setDebuggerColumns(Arrays.asList(editable, readOnly));

        assertFalse(model.isCellEditable(0, 0));
        assertFalse(model.isRowAtCurrentInstruction(0));
        assertEquals(0, model.guessPreviousInstructionLocation());

        PaginatingDisassembler ida = mock(PaginatingDisassembler.class);
        CPU cpu = createCpu(true, 20);
        when(ida.rowToLocation(20, 1)).thenReturn(-1);
        when(ida.rowToLocation(20, 2)).thenReturn(100);
        when(ida.rowToLocation(20, 3)).thenReturn(-1);
        when(ida.isRowAtCurrentInstruction(2)).thenReturn(true);
        when(ida.getCurrentInstructionRow()).thenReturn(4);
        setField(model, "ida", ida);
        setField(model, "cpu", cpu);

        assertFalse(model.isCellEditable(1, 0));
        assertFalse(model.isCellEditable(2, 1));
        assertTrue(model.isCellEditable(2, 0));
        assertTrue(model.isRowAtCurrentInstruction(2));
        assertEquals(19, model.guessPreviousInstructionLocation());

        when(ida.rowToLocation(20, 3)).thenReturn(18);
        assertEquals(18, model.guessPreviousInstructionLocation());
    }

    private CPU createCpu(boolean breakpointSupported, int instructionLocation) {
        Disassembler disassembler = new LinearDisassembler();
        CPU cpu = mock(CPU.class);
        when(cpu.getDisassembler()).thenReturn(disassembler);
        when(cpu.getInstructionLocation()).thenReturn(instructionLocation);
        when(cpu.isBreakpointSupported()).thenReturn(breakpointSupported);
        when(cpu.isBreakpointSet(anyInt())).thenReturn(false);
        return cpu;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = DebugTableModelImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private CannotSetDebuggerValueException cannotSetValueException(String message) throws Exception {
        Constructor<CannotSetDebuggerValueException> constructor =
                CannotSetDebuggerValueException.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(message);
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
        private CannotSetDebuggerValueException throwOnSet;

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
        public void setValue(int location, Object value) throws CannotSetDebuggerValueException {
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
