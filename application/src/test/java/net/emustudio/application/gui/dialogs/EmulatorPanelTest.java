/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EmulatorPanelTest extends AbstractSwingTest {

    @Test
    public void deviceSelectionEnablesDeviceButtonsAndDoubleClickShowsGui() {
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        Memory memory = mock(Memory.class);
        Device keyboard = mock(Device.class);
        Device printer = mock(Device.class);
        MemoryContext<?> memoryContext = mock(MemoryContext.class);

        when(cpu.getStatusPanel()).thenReturn(new JPanel());
        when(cpu.isBreakpointSupported()).thenReturn(false);
        when(memory.isShowSettingsSupported()).thenReturn(false);
        when(keyboard.getTitle()).thenReturn("Keyboard");
        when(keyboard.isShowSettingsSupported()).thenReturn(false);
        when(printer.getTitle()).thenReturn("Printer");
        when(printer.isShowSettingsSupported()).thenReturn(true);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getMemory()).thenReturn(Optional.of(memory));
        when(computer.getDevices()).thenReturn(List.of(keyboard, printer));

        JFrame parent = showFrame(onEdt(JFrame::new));
        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(
                parent,
                computer,
                createDebugTableModel(),
                dialogs,
                mock(EmulationController.class),
                memoryContext,
                new GUIImpl()
        ));

        showInFrame(panel);

        JList<?> deviceList = findComponent(panel, JList.class, list -> true);
        JButton settingsButton = findButton(panel, "Show settings...");
        JButton guiButton = findButton(panel, "Show device...");

        assertFalse(onEdt(settingsButton::isEnabled));
        assertFalse(onEdt(guiButton::isEnabled));

        runOnEdt(() -> deviceList.setSelectedIndex(0));
        assertFalse(onEdt(settingsButton::isEnabled));
        assertTrue(onEdt(guiButton::isEnabled));

        runOnEdt(() -> deviceList.setSelectedIndex(1));
        assertTrue(onEdt(settingsButton::isEnabled));
        assertTrue(onEdt(guiButton::isEnabled));

        runOnEdt(() -> deviceList.dispatchEvent(new MouseEvent(
                deviceList, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 2, false
        )));

        verify(printer).showGUI(parent);
    }

    @Test
    public void cpuStateChangesToggleDebugControlsAndMemoryListenerRegistration() {
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        MemoryContext<?> memoryContext = mock(MemoryContext.class);
        EmulationController emulationController = mock(EmulationController.class);

        when(cpu.getStatusPanel()).thenReturn(new JPanel());
        when(cpu.isBreakpointSupported()).thenReturn(false);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(emulationController.isTimedRunning()).thenReturn(false);

        JFrame parent = showFrame(onEdt(JFrame::new));
        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(
                parent,
                computer,
                createDebugTableModel(),
                dialogs,
                emulationController,
                memoryContext,
                new GUIImpl()
        ));

        showInFrame(panel);

        ArgumentCaptor<CPU.CPUListener> listenerCaptor = ArgumentCaptor.forClass(CPU.CPUListener.class);
        verify(cpu).addCPUListener(listenerCaptor.capture());
        verify(memoryContext).addMemoryListener(any());

        JButton runButton = findButtonByTooltip(panel, "Run emulation");
        JButton pauseButton = findButtonByTooltip(panel, "Pause emulation");
        JButton stepButton = findButtonByTooltip(panel, "Step forward");
        JTable debugTable = findComponent(panel, JTable.class, table -> true);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, panel.getRunState());
        assertTrue(onEdt(runButton::isEnabled));
        assertFalse(onEdt(pauseButton::isEnabled));
        assertTrue(onEdt(stepButton::isEnabled));
        assertTrue(onEdt(debugTable::isEnabled));
        assertTrue(onEdt(debugTable::isVisible));

        runOnEdt(() -> listenerCaptor.getValue().runStateChanged(CPU.RunState.STATE_RUNNING));

        verify(memoryContext).removeMemoryListener(any());
        assertEquals(CPU.RunState.STATE_RUNNING, panel.getRunState());
        assertFalse(onEdt(runButton::isEnabled));
        assertTrue(onEdt(pauseButton::isEnabled));
        assertFalse(onEdt(stepButton::isEnabled));
        assertFalse(onEdt(debugTable::isEnabled));
        assertFalse(onEdt(debugTable::isVisible));

        runOnEdt(() -> listenerCaptor.getValue().runStateChanged(CPU.RunState.STATE_STOPPED_BREAK));

        verify(memoryContext, times(2)).addMemoryListener(any());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, panel.getRunState());
        assertTrue(onEdt(runButton::isEnabled));
        assertFalse(onEdt(pauseButton::isEnabled));
        assertTrue(onEdt(stepButton::isEnabled));
        assertTrue(onEdt(debugTable::isEnabled));
        assertTrue(onEdt(debugTable::isVisible));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private DebugTableModel createDebugTableModel() {
        DebugTableModel model = mock(DebugTableModel.class);
        DebuggerColumn<String> column = mock(DebuggerColumn.class);

        when(column.getClassType()).thenReturn(String.class);
        when(column.getDefaultWidth()).thenReturn(-1);
        when(model.getColumnCount()).thenReturn(1);
        when(model.getColumnName(0)).thenReturn("Address");
        when(model.getColumnClass(0)).thenReturn((Class) String.class);
        when(model.getRowCount()).thenReturn(4);
        when(model.getValueAt(anyInt(), eq(0))).thenReturn("00");
        when(model.getColumnAt(0)).thenReturn((DebuggerColumn) column);

        return model;
    }
}
