/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.debugger.DebuggerPanel;
import net.emustudio.application.debugger.DebugSession;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.debugger.Debugging;
import net.emustudio.emulib.plugins.debugger.ValueView;
import net.emustudio.emulib.runtime.ExecutionState;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
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
                    mock(Dialogs.class), mockExecution(), null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            assertFalse(onEdt(findButtonByTooltip(panel, "Set breakpoint...")::isEnabled));
            assertFalse(onEdt(findToggleButtonByTooltip(panel, "Record instructions")::isEnabled));
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
    public void recordingToggleAfterResetStartsAndStopsRecordingWithoutButtonText() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Recording CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), mockExecution(),
                    null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            JButton reset = findButtonByTooltip(panel, "Reset emulation");
            JToggleButton recording = findToggleButtonByTooltip(panel, "Record instructions");

            runOnEdt(() -> {
                SwingUtilities.invokeLater(() -> {
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (window instanceof JDialog && window.isShowing()
                                && "Record instructions".equals(((JDialog) window).getTitle())) {
                            findButton((JDialog) window, "Start").doClick(0);
                            return;
                        }
                    }
                    throw new AssertionError("Recording dialog missing");
                });
                recording.doClick(0);
            });
            session.update(() -> {}).join();
            runOnEdt(() -> {});

            assertTrue(session.isRecording());
            runOnEdt(() -> {
                JToolBar toolbar = (JToolBar) reset.getParent();
                assertSame(toolbar, recording.getParent());
                assertEquals(toolbar.getComponentIndex(reset) + 1, toolbar.getComponentIndex(recording));
                assertFalse(SwingUtilities.isDescendingFrom(recording, manager));
                assertTrue(recording.getText() == null || recording.getText().isEmpty());
                assertNotNull(recording.getIcon());
                assertTrue(recording.isSelected());
                assertEquals("Stop recording", recording.getToolTipText());
                assertEquals("Stop recording", recording.getAccessibleContext().getAccessibleName());
            });

            runOnEdt(() -> manager.setExecutionRunning(true));

            assertTrue(onEdt(recording::isEnabled));
            assertTrue(session.isRecording());

            runOnEdt(() -> manager.setExecutionRunning(false));
            triggerButton(recording);
            session.update(() -> {}).join();
            runOnEdt(() -> {});

            assertFalse(session.isRecording());
            runOnEdt(() -> {
                assertFalse(recording.isSelected());
                assertTrue(recording.getText() == null || recording.getText().isEmpty());
                assertEquals("Record instructions", recording.getToolTipText());
                assertEquals("Record instructions", recording.getAccessibleContext().getAccessibleName());
                panel.close();
            });
        }
    }

    @Test
    public void instructionSelectionDoesNotToggleBreakpointsOrOpenSource() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        DebugTableModel model = createDebugTableModel();
        when(model.getLocationAt(anyInt())).thenAnswer(c -> 2 + 2 * (Integer) c.getArgument(0));
        AtomicReference<SourceCodePosition> source = new AtomicReference<>();
        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(new JFrame(), computer, model,
                    mock(Dialogs.class), mockExecution(), null, source::set, new EmuStudioGui()));
            showInFrame(panel);
            JTable table = findComponent(panel, DebugTableImpl.class, t -> true);
            clearInvocations(model);
            runOnEdt(() -> {
                java.awt.Rectangle cell = table.getCellRect(1, 0, true);
                int x = cell.x + cell.width / 2, y = cell.y + cell.height / 2;
                table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 1, 0, x, y, 1, false, MouseEvent.BUTTON1));
                table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_RELEASED, 2, 0, x, y, 1, false, MouseEvent.BUTTON1));
                table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 3, 0, x, y, 1, false, MouseEvent.BUTTON1));
            });
            runOnEdt(() -> {});
            assertTrue(session.breakpoints.isEmpty());
            runOnEdt(() -> table.getActionMap().get("showSource").actionPerformed(
                    new java.awt.event.ActionEvent(table, 0, "")));
            runOnEdt(() -> {});
            assertTrue(session.breakpoints.isEmpty());
            verify(model, never()).showAddress(anyInt());
            assertNull(source.get());
            runOnEdt(panel::close);
        }
    }

    @Test
    public void applicationBreakpointControlRemainsAvailableWithoutBreakpointsTab() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());

        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            session.setBreakpoint(42, true).join();
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            JFrame parent = showFrame(onEdt(JFrame::new));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(parent, computer, createDebugTableModel(),
                    mock(Dialogs.class), mockExecution(), null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            JButton breakpointButton = findButtonByTooltip(panel, "Set breakpoint...");
            assertTrue(onEdt(breakpointButton::isEnabled));
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            JTabbedPane tabs = findComponent(manager, JTabbedPane.class, pane -> true);
            assertEquals(-1, onEdt(() -> tabs.indexOfTab("Breakpoints")).intValue());
            assertEquals(-1, onEdt(() -> tabs.indexOfTab("Watches")).intValue());
            JSplitPane columns = findComponent(panel, JSplitPane.class, split -> "debuggerColumns".equals(split.getName()));
            assertSame(manager, onEdt(columns::getLeftComponent));
            assertTrue(SwingUtilities.isDescendingFrom(manager.watchesPanel(), columns.getRightComponent()));

            assertEquals("Debugger", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            assertTrue(session.isBreakpointSet(42));
            assertFalse(onEdt(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                    .anyMatch(window -> window instanceof JDialog && window.isShowing())));
            runOnEdt(panel::close);
        }
    }

    @Test
    public void portableBreakpointControlRemainsAvailableWithoutBreakpointsTab() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Portable CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        try (DebugSession session = new DebugSession(cpu, null, files.newFolder().toPath().resolve("computer.debugger"))) {
            DebugSession.Breakpoint breakpoint = new DebugSession.Breakpoint(12);
            session.breakpoints.add(breakpoint);
            session.updateProbe();
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), mockExecution(),
                    null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            JButton breakpointButton = findButtonByTooltip(panel, "Set breakpoint...");
            assertTrue(onEdt(breakpointButton::isEnabled));
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            JTabbedPane tabs = findComponent(manager, JTabbedPane.class, pane -> true);
            assertEquals(-1, onEdt(() -> tabs.indexOfTab("Breakpoints")).intValue());
            assertEquals(List.of(breakpoint), session.breakpoints);
            assertTrue(breakpoint.enabled);
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
                mockExecution(),
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
        ExecutionFixture fixture = new ExecutionFixture();
        EmulationController emulationController = fixture.controller;

        when(cpu.getStatusPanel()).thenReturn(new JPanel());
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());

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

        verify(fixture.state).subscribe(any());
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

        runOnEdt(() -> fixture.publish(CPU.RunState.STATE_RUNNING));

        verify(memoryContext, times(2)).removeMemoryListener(any());
        assertEquals(CPU.RunState.STATE_RUNNING, panel.getRunState());
        assertFalse(onEdt(runButton::isEnabled));
        assertTrue(onEdt(pauseButton::isEnabled));
        assertFalse(onEdt(stepButton::isEnabled));
        assertFalse(onEdt(debugTable::isEnabled));
        assertFalse(onEdt(debugTable::isVisible));

        runOnEdt(() -> fixture.publish(CPU.RunState.STATE_STOPPED_BREAK));

        verify(memoryContext, times(2)).addMemoryListener(any());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, panel.getRunState());
        assertTrue(onEdt(runButton::isEnabled));
        assertFalse(onEdt(pauseButton::isEnabled));
        assertTrue(onEdt(stepButton::isEnabled));
        assertTrue(onEdt(debugTable::isEnabled));
        assertTrue(onEdt(debugTable::isVisible));
    }

    @Test
    public void doubleClickingAnnotatedInstructionNavigatesToSource() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        MemoryContext<?> memoryContext = mock(MemoryContext.class);
        DebugTableModel model = createDebugTableModel();
        SourceCodePosition position = SourceCodePosition.of(7, 3, "program.asm");
        MemoryAnnotations annotations = new MemoryAnnotations();
        annotations.put(new MemoryAnnotations.Entry(5, 1, 42, 43, "source", "", position));
        CPU sessionCpu = mock(CPU.class);
        when(sessionCpu.getAddressSpaceSize()).thenReturn(256);
        when(memoryContext.getSize()).thenReturn(256);
        DebugSession session = new DebugSession(sessionCpu, memoryContext, null, annotations);
        AtomicReference<SourceCodePosition> navigated = new AtomicReference<>();

        when(computer.getCPU()).thenReturn(Optional.empty());
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(computer.getDebugger()).thenReturn(Optional.of(session));
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
        runOnEdt(panel::close);
        session.close();
    }

    @Test
    public void resizingPreservesSidebarDividerAndClosingDetachesListeners() {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mock(CPU.class);
        MemoryContext<?> memory = mock(MemoryContext.class);
        ExecutionFixture fixture = new ExecutionFixture();
        EmulationController execution = fixture.controller;
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(new JFrame(), computer, createDebugTableModel(),
                mock(Dialogs.class), execution, memory, position -> {}, new EmuStudioGui()));
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
        verify(fixture.subscription, times(1)).close();
        // Initial registration removes any previous copy, then close removes the final listener.
        verify(memory, times(2)).removeMemoryListener(any());
    }

    private static CPU debuggingCpu() {
        CPU cpu = mock(CPU.class, withSettings().extraInterfaces(Debugging.class));
        Debugging debugging = (Debugging) cpu;
        when(debugging.observe(any(), anyBoolean())).thenReturn(() -> {});
        when(debugging.compile(anyString())).thenReturn(new Debugging.Expression() {
            @Override public boolean test() { return true; }
            @Override public ValueView display() { return new ValueView("ready", List.of(), false); }
        });
        return cpu;
    }

    private static EmulationController mockExecution() { return new ExecutionFixture().controller; }

    private static final class ExecutionFixture {
        final EmulationController controller = mock(EmulationController.class);
        final ExecutionState state = mock(ExecutionState.class);
        final ExecutionState.Subscription subscription = mock(ExecutionState.Subscription.class);
        final AtomicReference<java.util.function.Consumer<ExecutionState.Snapshot>> listener = new AtomicReference<>();
        private ExecutionState.Snapshot snapshot = snapshot(CPU.RunState.STATE_STOPPED_BREAK);

        ExecutionFixture() {
            when(controller.snapshot()).thenAnswer(call -> snapshot);
            when(controller.executionState()).thenReturn(state);
            when(state.snapshot()).thenAnswer(call -> snapshot);
            when(state.subscribe(any())).thenAnswer(call -> {
                listener.set(call.getArgument(0));
                return subscription;
            });
        }

        void publish(CPU.RunState runState) {
            snapshot = snapshot(runState);
            listener.get().accept(snapshot);
        }

        private static ExecutionState.Snapshot snapshot(CPU.RunState runState) {
            return new ExecutionState.Snapshot(runState, false, false, 0, Float.NaN);
        }
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
