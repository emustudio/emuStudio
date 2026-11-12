/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import static net.emustudio.application.TestCpu.mockCpu;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.debugger.DebuggerPanel;
import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.debugger.DebugTableModel;
import net.emustudio.application.gui.debugger.DebugTableImpl;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.ValueView;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
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
        CPU cpu = mockCpu();
        when(cpu.getTitle()).thenReturn("Unsupported CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, files.newFolder().toPath().resolve("computer.debugger"));
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            JFrame parent = showFrame(onEdt(JFrame::new));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(parent, computer, createDebugTableModel(),
                    mock(Dialogs.class), mockExecution(), null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            assertFalse(onEdt(findButtonByTooltip(panel, "Set breakpoint...")::isEnabled));
            assertFalse(onEdt(findButtonByTooltip(panel, "Start recording")::isEnabled));
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
    public void recordingTabStartsAndStopsRecordingWithoutButtonText() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Recording CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, files.newFolder().toPath().resolve("computer.debugger"));
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), mockExecution(),
                    null, position -> {}, new EmuStudioGui()));
            showInFrame(panel);
            DebuggerPanel manager = findComponent(panel, DebuggerPanel.class, component -> true);
            JButton reset = findButtonByTooltip(panel, "Reset emulation");
            JButton recording = findButtonByTooltip(panel, "Start recording");
            JButton stop = findButtonByTooltip(panel, "Stop recording");
            JToggleButton pause = findToggleButtonByTooltip(panel, "Pause recording");
            JTabbedPane tabs = findComponent(manager, JTabbedPane.class,
                    component -> "debuggerViews".equals(component.getName()));

            runOnEdt(() -> {
                tabs.setSelectedIndex(tabs.indexOfTab("Recording"));
                assertFalse(stop.isEnabled());
                assertFalse(pause.isEnabled());
                SwingUtilities.invokeLater(() -> {
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (window instanceof JDialog && window.isShowing()
                                && "Start recording".equals(((JDialog) window).getTitle())) {
                            findButton((JDialog) window, "Start recording").doClick(0);
                            return;
                        }
                    }
                    throw new AssertionError("Recording dialog missing");
                });
                recording.doClick(0);
            });
            session.update(() -> {}).join();
            runOnEdt(() -> {});

            assertTrue(session.recording.isRecording());
            runOnEdt(() -> {
                JToolBar toolbar = (JToolBar) reset.getParent();
                assertFalse(SwingUtilities.isDescendingFrom(recording, toolbar));
                assertTrue(SwingUtilities.isDescendingFrom(recording, tabs.getSelectedComponent()));
                assertTrue(recording.getText() == null || recording.getText().isEmpty());
                assertNotNull(recording.getIcon());
                assertTrue(stop.getText() == null || stop.getText().isEmpty());
                assertTrue(pause.getText() == null || pause.getText().isEmpty());
                assertTrue(stop.isEnabled());
                assertTrue(pause.isEnabled());
                assertEquals("Start recording", recording.getToolTipText());
                assertEquals("Start recording", recording.getAccessibleContext().getAccessibleName());
            });

            assertTrue(onEdt(recording::isEnabled));
            assertTrue(session.recording.isRecording());

            triggerButton(stop);
            session.update(() -> {}).join();
            runOnEdt(() -> {});

            assertFalse(session.recording.isRecording());
            runOnEdt(() -> {
                assertFalse(stop.isEnabled());
                assertFalse(pause.isEnabled());
                assertTrue(recording.getText() == null || recording.getText().isEmpty());
                assertEquals("Start recording", recording.getToolTipText());
                assertEquals("Start recording", recording.getAccessibleContext().getAccessibleName());
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
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, files.newFolder().toPath().resolve("computer.debugger"));
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
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
            assertTrue(session.config.breakpoints.isEmpty());
            runOnEdt(() -> table.getActionMap().get("showSource").actionPerformed(
                    new java.awt.event.ActionEvent(table, 0, "")));
            runOnEdt(() -> {});
            assertTrue(session.config.breakpoints.isEmpty());
            verify(model).showAddress(4);
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

        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, files.newFolder().toPath().resolve("computer.debugger"));
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
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
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, files.newFolder().toPath().resolve("computer.debugger"));
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            session.config.breakpoints.add(12);
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
            assertTrue(session.isBreakpointSet(12));
            runOnEdt(panel::close);
        }
    }

    @Test
    public void deviceSelectionEnablesDeviceButtonsAndDoubleClickShowsGui() {
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mockCpu();
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
    public void stepKeepsListingVisibleUntilCompletion() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Stepping CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        ExecutionFixture fixture = new ExecutionFixture();
        java.util.concurrent.atomic.AtomicBoolean busy = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.CompletableFuture<CPU.RunState> completed = new java.util.concurrent.CompletableFuture<>();
        when(fixture.controller.isBusy()).thenAnswer(call -> busy.get());
        when(fixture.controller.isStepping()).thenAnswer(call -> busy.get());
        when(fixture.controller.step()).thenAnswer(call -> {
            busy.set(true);
            fixture.publish(CPU.RunState.STATE_RUNNING);
            return completed;
        });
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), fixture.controller,
                    null, position -> {}, new EmuStudioGui()));
            try {
                JFrame frame = showInFrame(panel);
                runOnEdt(() -> frame.setVisible(true));
                JTable table = findComponent(panel, DebugTableImpl.class, component -> true);
                JScrollPane scroll = onEdt(() -> (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table));
                JButton step = findButtonByTooltip(panel, "Step forward");
                runOnEdt(() -> {
                    assertTrue("Listing must start visible", table.isShowing());
                    step.doClick(0);
                    assertTrue(table.isShowing());
                    assertTrue(scroll.isShowing());
                    assertTrue(table.isEnabled());
                    assertTrue(step.isEnabled());
                    busy.set(false);
                    fixture.publish(CPU.RunState.STATE_STOPPED_BREAK);
                    completed.complete(CPU.RunState.STATE_STOPPED_BREAK);
                });
                runOnEdt(() -> {
                    assertTrue(table.isShowing());
                    assertTrue(scroll.isShowing());
                    assertTrue(table.isEnabled());
                    assertTrue(step.isEnabled());
                });
            } finally { runOnEdt(panel::close); }
        }
    }

    @Test
    public void singleStepDoesNotDisableToolbarsWhileInstructionIsExecuting() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
        when(cpu.step()).thenAnswer(call -> {
            net.emustudio.application.TestCpu.publish(cpu, CPU.RunState.STATE_RUNNING);
            entered.countDown();
            if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Step not released");
            net.emustudio.application.TestCpu.publish(cpu, CPU.RunState.STATE_STOPPED_BREAK);
            return CPU.RunState.STATE_STOPPED_BREAK;
        });
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), machineExecution,
                    null, position -> {}, new EmuStudioGui()));
            try {
                JButton step = findButtonByTooltip(panel, "Step forward");
                JButton run = findButtonByTooltip(panel, "Run emulation");
                JToggleButton follow = findToggleButtonByTooltip(panel, "Show instructions from PC");
                java.util.concurrent.atomic.AtomicInteger disabled = new java.util.concurrent.atomic.AtomicInteger();
                machineExecution.addExecutionListener(state -> {
                    if (state == CPU.RunState.STATE_STOPPED_BREAK) completed.countDown();
                });
                runOnEdt(() -> {
                    for (AbstractButton button : List.of(step, run, follow)) {
                        button.addPropertyChangeListener("enabled", event -> {
                            if (Boolean.FALSE.equals(event.getNewValue())) disabled.incrementAndGet();
                        });
                    }
                    step.doClick(0);
                });
                assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
                assertTrue(machineExecution.isStepping());
                runOnEdt(() -> {
                    assertTrue(step.isEnabled());
                    assertTrue(run.isEnabled());
                    assertTrue(follow.isEnabled());
                });
                release.countDown();
                assertTrue(completed.await(2, java.util.concurrent.TimeUnit.SECONDS));
                assertFalse(machineExecution.isStepping());
                runOnEdt(() -> {});
                assertEquals(0, disabled.get());
            } finally {
                release.countDown();
                runOnEdt(panel::close);
            }
        }
    }

    @Test
    public void completedFastStepNeverHidesListingWhenEdtProcessesQueuedStateChanges() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = debuggingCpu();
        when(cpu.getTitle()).thenReturn("Fast stepping CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(256);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDevices()).thenReturn(List.of());
        when(cpu.step()).thenAnswer(call -> {
            net.emustudio.application.TestCpu.publish(cpu, CPU.RunState.STATE_RUNNING);
            net.emustudio.application.TestCpu.publish(cpu, CPU.RunState.STATE_STOPPED_BREAK);
            return CPU.RunState.STATE_STOPPED_BREAK;
        });
        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), null, null);
             net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session)) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            EmulatorPanel panel = onEdt(() -> new EmulatorPanel(showFrame(new JFrame()), computer,
                    createDebugTableModel(), mock(Dialogs.class), machineExecution,
                    null, position -> {}, new EmuStudioGui()));
            try {
                JFrame frame = showInFrame(panel);
                runOnEdt(() -> frame.setVisible(true));
                DebugTableImpl table = findComponent(panel, DebugTableImpl.class, component -> true);
                java.util.concurrent.atomic.AtomicInteger hidden = new java.util.concurrent.atomic.AtomicInteger();
                runOnEdt(() -> {
                    assertTrue(table.isShowing());
                    table.addComponentListener(new java.awt.event.ComponentAdapter() {
                        @Override public void componentHidden(java.awt.event.ComponentEvent event) { hidden.incrementAndGet(); }
                    });
                    try { machineExecution.step().get(2, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception failure) { throw new RuntimeException(failure); }
                    assertFalse(machineExecution.isBusy());
                });
                runOnEdt(() -> {});
                runOnEdt(() -> {
                    assertEquals("Queued RUNNING event must not hide a completed step", 0, hidden.get());
                    assertTrue(table.isShowing());
                });
            } finally { runOnEdt(panel::close); }
        }
    }

    @Test
    public void cpuStateChangesToggleDebugControlsAndMemoryListenerRegistration() {
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mockCpu();
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

        verify(fixture.context).addCPUListener(any());
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
        assertTrue(onEdt(debugTable::isVisible));

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
        CPU sessionCpu = mockCpu();
        when(sessionCpu.getAddressSpaceSize()).thenReturn(256);
        when(memoryContext.getSize()).thenReturn(256);
        DebuggerSession session = new DebuggerSession(sessionCpu, net.emustudio.application.TestCpu.context(sessionCpu), memoryContext, null, annotations);
        net.emustudio.application.emulation.EmulationController machineExecution = net.emustudio.application.TestCpu.execution(session);
        AtomicReference<SourceCodePosition> navigated = new AtomicReference<>();

        when(computer.getCPU()).thenReturn(Optional.empty());
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(computer.getDebugger()).thenReturn(Optional.of(session));
        when(model.getLocationAt(0)).thenReturn(42);

        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(
                new JFrame(), computer, model, mock(Dialogs.class), machineExecution, memoryContext,
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
        machineExecution.close();
        session.close();
    }

    @Test
    public void resizingPreservesSidebarDividerAndClosingDetachesListeners() {
        VirtualComputer computer = mock(VirtualComputer.class);
        CPU cpu = mockCpu();
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
        verify(fixture.context, times(1)).removeCPUListener(fixture.listener.get());
        // Initial registration removes any previous copy, then close removes the final listener.
        verify(memory, times(2)).removeMemoryListener(any());
    }

    private static CPU debuggingCpu() {
        CPU cpu = mockCpu( withSettings().extraInterfaces(Debugging.class));
        Debugging debugging = (Debugging) cpu;
        when(debugging.observe(any(), any(Debugging.Capture.class))).thenReturn(() -> {});
        when(debugging.compile(anyString())).thenReturn(new Debugging.Expression() {
            @Override public boolean test() { return true; }
            @Override public ValueView display() { return new ValueView("ready", List.of(), false); }
        });
        return cpu;
    }

    private static EmulationController mockExecution() { return new ExecutionFixture().controller; }

    private static final class ExecutionFixture {
        final EmulationController controller = mock(EmulationController.class);
        final CPUContext context = mock(CPUContext.class);
        final AtomicReference<CPUContext.CPUListener> listener = new AtomicReference<>();
        private CPU.RunState state = CPU.RunState.STATE_STOPPED_BREAK;

        ExecutionFixture() {
            when(controller.getRunState()).thenAnswer(call -> state);
            when(controller.cpuContext()).thenReturn(context);
            doAnswer(call -> {
                listener.set(call.getArgument(0));
                return null;
            }).when(context).addCPUListener(any());
        }

        void publish(CPU.RunState runState) {
            state = runState;
            listener.get().runStateChanged(runState);
            listener.get().internalStateChanged();
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
