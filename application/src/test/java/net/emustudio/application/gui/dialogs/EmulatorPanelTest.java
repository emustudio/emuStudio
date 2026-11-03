/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.debugger.DebuggerPanel;
import net.emustudio.application.debugger.DebugSession;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EmulatorPanelTest extends AbstractSwingTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test
    public void unsupportedBreakpointsDisableToolbarAndCannotOpenDialog() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        when(cpu.getTitle()).thenReturn("Unsupported CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            JFrame parent = showFrame(onEdt(JFrame::new));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(parent, computer, createDebugTableModel(),
                    mock(Dialogs.class), mock(EmulationController.class), null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            assertFalse(onEdt(findButtonByTooltip(panel, "Set breakpoint...")::isEnabled));
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            assertFalse(manager.isBreakpointDialogSupported());

            // Fail before entering a modal dialog if the caller's capability guard is removed.
            when(cpu.getAddressSpaceSize()).thenThrow(new AssertionError("Unsupported dialog must not be constructed"));
            try {
                runOnEdt(() -> manager.showBreakpointDialog(parent));
            } finally {
                doReturn(256).when(cpu).getAddressSpaceSize();
            }
            runOnEdt(panel::close);
        }
    }

    @Test
    public void breakpointMouseAndEnterToggleTwoStatesWithoutRepagingOrOpeningSource() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        Set<Integer> addresses = new HashSet<>();
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.isBreakpointSupported()).thenReturn(true);
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(cpu.isBreakpointSet(anyInt())).thenAnswer(c -> addresses.contains(c.getArgument(0)));
        when(cpu.getBreakpoints()).thenAnswer(c -> Set.copyOf(addresses));
        doAnswer(c -> addresses.add(c.getArgument(0))).when(cpu).setBreakpoint(anyInt());
        doAnswer(c -> addresses.remove(c.getArgument(0))).when(cpu).unsetBreakpoint(anyInt());
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        DebugTableModel model = createDebugTableModel();
        when(model.getLocationAt(anyInt())).thenAnswer(c -> 2 + 2 * (Integer) c.getArgument(0));
        AtomicReference<SourceCodePosition> source = new AtomicReference<>();
        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(new JFrame(), computer, model,
                    mock(Dialogs.class), mock(EmulationController.class), null, source::set, new EmuStudioGui()));
            showInFrame(panel);
            JTable table = findComponent(panel, JTable.class, t -> true);
            clearInvocations(model);
            runOnEdt(() -> {
                java.awt.Rectangle cell = table.getCellRect(1, 0, true);
                int x = cell.x + cell.width / 2, y = cell.y + cell.height / 2;
                table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1));
                table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_RELEASED, 2, 0, x, y, 1, false, MouseEvent.BUTTON1));
                table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 3, 0, x, y, 1, false, MouseEvent.BUTTON1));
            });
            runOnEdt(() -> {});
            assertEquals(Set.of(4), addresses);
            assertEquals(Boolean.TRUE, onEdt(() -> table.getValueAt(1, 0)));
            runOnEdt(() -> table.getActionMap().get("showSource").actionPerformed(
                    new java.awt.event.ActionEvent(table, 0, "")));
            runOnEdt(() -> {});
            assertEquals(Boolean.FALSE, onEdt(() -> table.getValueAt(1, 0)));
            assertTrue(addresses.isEmpty());
            runOnEdt(() -> table.getActionMap().get("showSource").actionPerformed(
                    new java.awt.event.ActionEvent(table, 0, "")));
            runOnEdt(() -> {});
            assertEquals(Boolean.TRUE, onEdt(() -> table.getValueAt(1, 0)));
            assertEquals(Set.of(4), addresses);
            verify(model, never()).showAddress(anyInt());
            assertNull(source.get());
            runOnEdt(panel::close);
        }
    }

    @Test
    public void breakpointManagerSharesOrdinaryCpuBreakpoints() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        when(cpu.getTitle()).thenReturn("Test CPU");
        Set<Integer> breakpoints = new HashSet<>(Set.of(42));
        when(cpu.isBreakpointSupported()).thenReturn(true);
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(cpu.getBreakpoints()).thenAnswer(invocation -> Set.copyOf(breakpoints));
        when(cpu.isBreakpointSet(anyInt())).thenAnswer(invocation -> breakpoints.contains(invocation.getArgument(0)));
        doAnswer(invocation -> breakpoints.add(invocation.getArgument(0))).when(cpu).setBreakpoint(anyInt());
        doAnswer(invocation -> breakpoints.remove(invocation.getArgument(0))).when(cpu).unsetBreakpoint(anyInt());
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());

        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            JFrame parent = showFrame(onEdt(JFrame::new));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(parent, computer, createDebugTableModel(),
                    mock(Dialogs.class), mock(EmulationController.class), null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            JButton breakpointButton = findButtonByTooltip(panel, "Set breakpoint...");
            assertTrue(onEdt(breakpointButton::isEnabled));
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            runOnEdt(manager::showBreakpoints);
            JTabbedPane tabs = findComponent(manager, JTabbedPane.class, pane -> true);
            assertEquals(-1, onEdt(() -> tabs.indexOfTab("Watches")).intValue());
            JSplitPane columns = findComponent(panel, JSplitPane.class, split -> "debuggerColumns".equals(split.getName()));
            assertSame(manager, onEdt(columns::getLeftComponent));
            assertTrue(SwingUtilities.isDescendingFrom(manager.watchesPanel(), columns.getRightComponent()));

            assertEquals("Breakpoints", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            JList<?> rows = findComponent(onEdt(() -> (java.awt.Container) tabs.getSelectedComponent()), JList.class, list -> true);
            assertEquals("Enabled breakpoint: 002A", onEdt(() -> rows.getModel().getElementAt(0)));
            assertTrue(onEdt(findButtonByTooltip(manager, "Add breakpoint")::isEnabled));

            runOnEdt(() -> rows.setSelectedIndex(0));
            triggerButton(findButtonByTooltip(onEdt(() -> (java.awt.Container) tabs.getSelectedComponent()), "Remove"));
            verify(cpu).unsetBreakpoint(42);
            assertTrue(breakpoints.isEmpty());

            // A breakpoint set through the main debugger's CPU API appears on reopening the manager.
            runOnEdt(() -> {
                cpu.setBreakpoint(64);
                tabs.setSelectedIndex(tabs.indexOfTab("Memory"));
            });
            runOnEdt(manager::showBreakpoints);
            assertSame(manager, findComponent(panel, DebuggerPanel.class, component -> true));
            assertEquals("Breakpoints", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            assertEquals(1, onEdt(() -> rows.getModel().getSize()).intValue());
            assertEquals("Enabled breakpoint: 0040", onEdt(() -> rows.getModel().getElementAt(0)));
            assertFalse(onEdt(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                    .anyMatch(window -> window instanceof JDialog && window.isShowing())));
            runOnEdt(panel::close);
        }
    }

    @Test
    public void breakpointManagerSupportsPortableRulesWithoutNativeBreakpoints() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        DebugCPU debug = mock(DebugCPU.class);
        when(cpu.getTitle()).thenReturn("Portable CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(cpu.getDebugger()).thenReturn(Optional.of(debug));
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            DebugSession.Rule rule = new DebugSession.Rule("entry", "EXEC", 12, "", 2);
            session.rules.add(rule);
            session.updateProbe();
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), mock(EmulationController.class),
                    null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            JButton breakpointButton = findButtonByTooltip(panel, "Set breakpoint...");
            assertTrue(onEdt(breakpointButton::isEnabled));
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            runOnEdt(manager::showBreakpoints);
            JTabbedPane tabs = findComponent(manager, JTabbedPane.class, pane -> true);
            java.awt.Container controls = onEdt(() -> (java.awt.Container) tabs.getSelectedComponent());
            JList<?> rows = findComponent(controls, JList.class, list -> true);
            assertTrue(onEdt(findButtonByTooltip(manager, "Add breakpoint")::isEnabled));
            runOnEdt(() -> rows.setSelectedIndex(0));
            triggerButton(findButtonByTooltip(controls, "Enable / disable"));
            assertFalse(rule.enabled);
            verify(debug).removeListener(session);
            runOnEdt(() -> rows.setSelectedIndex(0));
            triggerButton(findButtonByTooltip(controls, "Remove"));
            assertTrue(session.rules.isEmpty());
            verify(cpu, never()).unsetBreakpoint(anyInt());
            runOnEdt(panel::close);
        }
    }

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
                position -> {},
                new EmuStudioGui()
        ));

        showInFrame(panel);

        JList<?> deviceList = findComponent(panel, JList.class, list -> "devices".equals(list.getName()));
        JSplitPane columns = findComponent(panel, JSplitPane.class, split -> "debuggerColumns".equals(split.getName()));
        JSplitPane sidebar = findComponent(panel, JSplitPane.class, split -> "debuggerSidebar".equals(split.getName()));
        assertTrue(SwingUtilities.isDescendingFrom(deviceList, sidebar.getBottomComponent()));
        assertTrue(SwingUtilities.isDescendingFrom(sidebar, columns.getRightComponent()));
        assertTrue(onEdt(() -> columns.getLeftComponent().getHeight() > 300));
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
                position -> {},
                new EmuStudioGui()
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

        verify(memoryContext, times(2)).removeMemoryListener(any());
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

    @Test
    public void doubleClickingAnnotatedInstructionNavigatesToSource() {
        VirtualComputer computer = mock(VirtualComputer.class);
        MemoryContext<?> memoryContext = mock(MemoryContext.class);
        DebugTableModel model = createDebugTableModel();
        SourceCodePosition position = SourceCodePosition.of(7, 3, "program.asm");
        Annotations annotations = new Annotations();
        annotations.add(42, new SourceCodeAnnotation(5, position));
        AtomicReference<SourceCodePosition> navigated = new AtomicReference<>();

        when(computer.getCPU()).thenReturn(Optional.empty());
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(memoryContext.annotations()).thenReturn(annotations);
        when(model.getLocationAt(0)).thenReturn(42);

        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(
                new JFrame(), computer, model, mock(Dialogs.class), null, memoryContext,
                navigated::set, new EmuStudioGui()
        ));
        showInFrame(panel);
        JTable debugTable = findComponent(panel, JTable.class, table -> true);

        runOnEdt(() -> debugTable.dispatchEvent(new MouseEvent(
                debugTable, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                5, 5, 2, false, MouseEvent.BUTTON1
        )));

        assertEquals(position, navigated.get());
    }

    @Test
    public void resizingPreservesSidebarDividerAndClosingDetachesListeners() {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        MemoryContext<?> memory = mock(MemoryContext.class);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(new JFrame(), computer, createDebugTableModel(),
                mock(Dialogs.class), mock(EmulationController.class), memory, position -> {}, new EmuStudioGui()));
        showInFrame(panel);
        JSplitPane split = findComponent(panel, JSplitPane.class, item -> "debuggerColumns".equals(item.getName()));
        runOnEdt(() -> {
            panel.resizeComponents(768);
            split.setDividerLocation(240);
            panel.resizeComponents(900);
        });
        assertEquals(240, onEdt(split::getDividerLocation).intValue());
        runOnEdt(panel::close);
        runOnEdt(panel::close);
        verify(cpu, times(1)).removeCPUListener(any());
        // Initial registration removes any previous copy, then close removes the final listener.
        verify(memory, times(2)).removeMemoryListener(any());
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
