/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DebuggerPanelTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void browsingAnotherBankChangesInspectionWithoutMovingCpu() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2, 3}, {4, 5, 3}}, 2, session -> {})) {
            List<String> selections = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.setAddressSelectionListener((address, bank, visit) ->
                        selections.add(address + ":" + bank + ":" + visit));
                workspace.panel.navigate(1, 1, true);

                assertFalse(workspace.panel.isInstructionView());
                assertEquals("Memory bytes", workspace.table.getAccessibleContext().getAccessibleName());
                assertEquals(1, workspace.table.getSelectedRow());
            });

            assertEquals(List.of("1:1:true"), selections);
            verify(workspace.machine.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test
    public void navigationRejectsAddressesAndBanksOutsideMemory() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(-1, 0, true));
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(2, 0, true));
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(0, 1, true));
                assertTrue(workspace.panel.isInstructionView());
            });
        }
    }

    @Test
    public void executionDisablesTableAndHidesInstructionListingUntilPaused() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.setExecutionRunning(true);
                assertFalse(workspace.table.isEnabled());
                assertFalse(workspace.table.isVisible());

                workspace.panel.showMemory();
                assertTrue("Memory view stays visible during execution", workspace.table.isVisible());
                assertFalse(workspace.table.isEnabled());

                workspace.panel.setExecutionRunning(false);
                workspace.panel.showInstructions();
                assertTrue(workspace.table.isVisible());
                assertTrue(workspace.table.isEnabled());
            });
        }
    }

    @Test
    public void breakpointListActionsTargetVisibleExecutionRuleAmongMemoryWatches() throws Exception {
        DebugSession.Rule read = new DebugSession.Rule("read buffer", "READ", 0, -1, "", 1);
        DebugSession.Rule execute = new DebugSession.Rule("loop", "EXEC", 1, -1, "", 1);
        DebugSession.Rule write = new DebugSession.Rule("write buffer", "WRITE", 2, -1, "", 1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3,
                session -> session.rules.addAll(List.of(read, execute, write)))) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                Container breakpoints = (Container) tabs.getComponentAt(tabs.indexOfTab("Breakpoints"));
                JList<?> list = component(breakpoints, JList.class, "Breakpoints");
                assertEquals(1, list.getModel().getSize());
                list.setSelectedIndex(0);
                button(breakpoints, "Enable / disable").doClick(0);
                assertFalse(execute.enabled);
                assertTrue(read.enabled);
                assertTrue(write.enabled);
                assertEquals(0, list.getSelectedIndex());

                button(breakpoints, "Remove").doClick(0);
                assertEquals(List.of(read, write), workspace.session.rules);
                assertEquals(0, list.getModel().getSize());
            });
        }
    }

    @Test
    public void memoryWatchActionsTargetVisibleWatchAmongExecutionRules() throws Exception {
        DebugSession.Rule execute = new DebugSession.Rule("loop", "EXEC", 0, -1, "", 1);
        DebugSession.Rule read = new DebugSession.Rule("read buffer", "READ", 1, -1, "", 1);
        DebugSession.Rule write = new DebugSession.Rule("write buffer", "WRITE", 2, -1, "", 1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3,
                session -> session.rules.addAll(List.of(execute, read, write)))) {
            SwingUtilities.invokeAndWait(() -> {
                Container watches = workspace.panel.watchesPanel();
                JList<?> list = component(watches, JList.class, "Memory access watches");
                assertEquals(2, list.getModel().getSize());
                list.setSelectedIndex(1);
                button(watches, "Enable / disable memory watch").doClick(0);
                assertFalse(write.enabled);
                assertTrue(read.enabled);
                assertTrue(execute.enabled);

                button(watches, "Remove memory watch").doClick(0);
                assertEquals(List.of(execute, read), workspace.session.rules);
                assertEquals(1, list.getModel().getSize());
            });
        }
    }

    @Test
    public void inspectOnlyRequestBrowsesMemoryWithoutAddingBreakpoint() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}, {3, 4}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.INSPECT_ONLY, 1, 1, false)));
                assertFalse(workspace.panel.isInstructionView());
                assertEquals(1, workspace.table.getSelectedRow());
                assertTrue(workspace.session.rules.isEmpty());
                assertEquals(0, workspace.breakpointChanges.get());
            });
            verify(workspace.machine.cpu, never()).setBreakpoint(anyInt());
        }
    }

    @Test
    public void setAndUnsetRequestsUpdateBreakpointAndInspectionPreference() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 1, 0, true)));
                assertEquals(BreakpointState.KEEP_INSPECTION, workspace.session.breakpointState(1));
                assertTrue(workspace.panel.isInspectionPinned());
                assertEquals(1, workspace.breakpointChanges.get());

                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.UNSET, 1, 0, false)));
                assertEquals(BreakpointState.OFF, workspace.session.breakpointState(1));
                assertFalse(workspace.panel.isInspectionPinned());
                assertEquals(2, workspace.breakpointChanges.get());
            });
        }
    }

    @Test
    public void invalidBreakpointRequestShowsErrorWithoutChangingRules() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 2, 0, false)));
                assertTrue(workspace.session.rules.isEmpty());
                assertEquals(0, workspace.breakpointChanges.get());
            });
            verify(workspace.dialogs).showError("Address outside memory", "Debugger");
        }
    }

    @Test
    public void closingPanelDetachesItsListenerWhileSessionRemainsUsable() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0}}, 1, session -> {})) {
            assertEquals(2, workspace.machine.cpuListeners.size());
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.close();
                workspace.panel.close();
            });
            assertEquals(1, workspace.machine.cpuListeners.size());
            workspace.session.requirePaused();
        }
    }

    private Workspace workspace(byte[][] banks, int common, Consumer<DebugSession> configure) throws Exception {
        Path settings = temporary.newFolder().toPath().resolve("debug.properties");
        Workspace workspace = new Workspace();
        workspace.machine = new DebuggerTestMachine(banks, common, false);
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(call -> (int) call.getArgument(0) + 1);
        when(disassembler.disassemble(anyInt())).thenAnswer(call ->
                new DisassembledInstruction(call.getArgument(0), "NOP", "00"));
        when(workspace.machine.cpu.getDisassembler()).thenReturn(disassembler);
        workspace.session = new DebugSession(workspace.machine.cpu, workspace.machine.memory, settings);
        configure.accept(workspace.session);
        CountDownLatch ready = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                DebugTableModelImpl model = new DebugTableModelImpl();
                model.setCPU(workspace.machine.cpu);
                model.setMaxRows(3);
                workspace.table = new DebugTableImpl(model);
                JScrollPane scroll = new JScrollPane(workspace.table);
                JPanel instructions = new JPanel(new BorderLayout());
                instructions.add(scroll);
                workspace.panel = new DebuggerPanel(workspace.session, workspace.table, scroll, instructions,
                        position -> {}, workspace.breakpointChanges::incrementAndGet,
                        new EmuStudioGui(), workspace.dialogs);
                workspace.panel.setInspectionReadyListener(ready::countDown);
            });
            assertTrue("Initial memory capture should finish", ready.await(5, TimeUnit.SECONDS));
            return workspace;
        } catch (Exception | AssertionError failure) {
            workspace.close();
            throw failure;
        }
    }

    private static AbstractButton button(Container root, String name) {
        return component(root, AbstractButton.class, name);
    }

    private static <T extends Component> T component(Container root, Class<T> type, String name) {
        T found = findComponent(root, type, name);
        if (found == null) throw new AssertionError("Missing " + type.getSimpleName() + ": " + name);
        return found;
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type, String name) {
        for (Component child : root.getComponents()) {
            String accessible = child instanceof JComponent
                    ? ((JComponent) child).getAccessibleContext().getAccessibleName() : null;
            String tooltip = child instanceof JComponent ? ((JComponent) child).getToolTipText() : null;
            if (type.isInstance(child) && (name.equals(child.getName()) || name.equals(accessible)
                    || name.equals(tooltip))) return type.cast(child);
            if (child instanceof Container) {
                T nested = findComponent((Container) child, type, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static final class Workspace implements AutoCloseable {
        DebuggerTestMachine machine;
        DebugSession session;
        DebuggerPanel panel;
        DebugTableImpl table;
        final Dialogs dialogs = mock(Dialogs.class);
        final AtomicInteger breakpointChanges = new AtomicInteger();

        @Override public void close() throws Exception {
            if (panel != null) SwingUtilities.invokeAndWait(panel::close);
            if (session != null) session.close();
        }
    }
}
