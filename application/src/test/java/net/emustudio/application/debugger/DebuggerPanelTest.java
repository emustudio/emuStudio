/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DebuggerPanelTest {
    @Test
    public void instructionViewAcceptsColumnsRenamedByWrapper() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                javax.swing.table.TableModel base = workspace.table.instructionModel();
                javax.swing.table.TableModel wrapper = new javax.swing.table.AbstractTableModel() {
                    @Override public int getRowCount() { return base.getRowCount(); }
                    @Override public int getColumnCount() { return base.getColumnCount(); }
                    @Override public String getColumnName(int column) {
                        return column == 0 ? "renamed" : base.getColumnName(column);
                    }
                    @Override public Object getValueAt(int row, int column) { return base.getValueAt(row, column); }
                };
                workspace.table.showInstructions(wrapper);
                assertEquals("renamed", workspace.table.getColumnName(0));
            });
        }
    }

    @Test
    public void newAnnotationUsesDialogInsteadOfInlineEditor() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Annotations"));
                Container annotations = (Container) tabs.getSelectedComponent();
                assertNull(findComponent(annotations, JButton.class, "Save annotation"));
                assertFalse(button(annotations, "Edit selected annotation").isEnabled());
                Timer timer = new Timer(100, event -> {
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (!(window instanceof JDialog) || !window.isShowing()
                                || !"New annotation".equals(((JDialog) window).getTitle())) continue;
                        ((Timer) event.getSource()).stop();
                        try {
                            JLabel label = component((Container) window, JLabel.class, "Label");
                            ((JTextField) label.getLabelFor()).setText("entry");
                            JLabel address = component((Container) window, JLabel.class, "Address");
                            ((JTextField) address.getLabelFor()).setText("0x0001");
                            JLabel end = component((Container) window, JLabel.class, "End address");
                            ((JTextField) end.getLabelFor()).setText("0x0001");
                            assertNull(findComponent((Container) window, JButton.class, "Cancel"));
                            button((Container) window, "Save annotation").doClick(0);
                        } catch (Throwable error) {
                            failure.set(error);
                            window.dispose();
                        }
                    }
                });
                timer.start();
                button(annotations, "Annotate inspected address").doClick(0);
                timer.stop();
            });
            if (failure.get() != null) throw new AssertionError(failure.get());
            assertEquals("entry", workspace.session.annotations().get(0, 1).label);
        }
    }

    @Test
    public void escapeDiscardsAnnotationDraftWithoutConfirmation() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Annotations"));
                Container annotations = (Container) tabs.getSelectedComponent();
                Timer timer = new Timer(100, event -> {
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (!(window instanceof JDialog) || !window.isShowing()
                                || !"New annotation".equals(((JDialog) window).getTitle())) continue;
                        ((Timer) event.getSource()).stop();
                        try {
                            JLabel label = component((Container) window, JLabel.class, "Label");
                            ((JTextField) label.getLabelFor()).setText("discarded");
                            JRootPane root = ((JDialog) window).getRootPane();
                            KeyStroke escape = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
                            Object action = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(escape);
                            root.getActionMap().get(action).actionPerformed(new java.awt.event.ActionEvent(root, 0, "escape"));
                        } catch (Throwable error) {
                            failure.set(error);
                            window.dispose();
                        }
                    }
                });
                timer.start();
                button(annotations, "Annotate inspected address").doClick(0);
                timer.stop();
                assertTrue(workspace.panel.confirmSaveAnnotations());
            });
            if (failure.get() != null) throw new AssertionError(failure.get());
            assertNull(workspace.session.annotations().get(0, 0));
        }
    }

    @Test
    public void annotationEditorCreatesAndEditsOverlappingRangesIndependently() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8], new byte[8]}, 8, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Annotations"));
                Container panel = (Container) tabs.getSelectedComponent();
                editAnnotationDialog(panel, "New annotation", "Annotate inspected address", dialog -> {
                    annotationField(dialog, "Address").setText("0x0001");
                    annotationField(dialog, "End address").setText("0x0004");
                    annotationField(dialog, "Label").setText("first");
                    ((JSpinner) component(dialog, JLabel.class, "Bank").getLabelFor()).setValue(1);
                });
                editAnnotationDialog(panel, "New annotation", "Annotate inspected address", dialog -> {
                    annotationField(dialog, "Address").setText("0x0002");
                    annotationField(dialog, "End address").setText("0x0005");
                    annotationField(dialog, "Label").setText("second");
                    ((JSpinner) component(dialog, JLabel.class, "Bank").getLabelFor()).setValue(1);
                });
                @SuppressWarnings("unchecked") JList<AnnotationIndex.Entry> list = component(panel, JList.class, "Annotations");
                assertEquals(2, list.getModel().getSize());
                int firstRow = list.getModel().getElementAt(0).label.equals("first") ? 0 : 1;
                list.setSelectedIndex(firstRow);
                editAnnotationDialog(panel, "Edit annotation", "Edit selected annotation", dialog -> {
                    annotationField(dialog, "End address").setText("0x0006");
                    annotationField(dialog, "Label").setText("updated");
                });
            });

            List<AnnotationIndex.Entry> entries = workspace.session.annotations().entries();
            assertEquals(2, entries.size());
            AnnotationIndex.Entry updated = entries.stream().filter(entry -> entry.label.equals("updated")).findFirst().orElseThrow();
            AnnotationIndex.Entry second = entries.stream().filter(entry -> entry.label.equals("second")).findFirst().orElseThrow();
            assertEquals(1, updated.bank);
            assertEquals(1, updated.address);
            assertEquals(7, updated.end);
            assertEquals(1, second.bank);
            assertEquals(2, second.address);
            assertEquals(6, second.end);
        }
    }

    private static JTextField annotationField(Container dialog, String name) {
        return (JTextField) component(dialog, JLabel.class, name).getLabelFor();
    }

    private static void editAnnotationDialog(Container panel, String title, String buttonName, Consumer<Container> edit) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        AtomicBoolean opened = new AtomicBoolean();
        Timer timer = new Timer(100, event -> {
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                if (!(window instanceof JDialog) || !window.isShowing() || !title.equals(((JDialog) window).getTitle())) continue;
                ((Timer) event.getSource()).stop();
                opened.set(true);
                try {
                    edit.accept((Container) window);
                    button((Container) window, "Save annotation").doClick(0);
                    if (window.isShowing()) throw new AssertionError("Annotation dialog did not close after save");
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    window.dispose();
                }
            }
        });
        timer.start();
        try { button(panel, buttonName).doClick(0); }
        finally { timer.stop(); }
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertTrue("Expected annotation dialog", opened.get());
    }

    @Test
    public void obsoleteMemoryControlsAreAbsent() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                for (String name : List.of("Take snapshot", "Capture baseline", "Compare baseline", "Compare binary",
                        "Export memory and metadata", "Rescan", "Compare / strings", "Live memory",
                        "Previous", "Next", "Show all addresses"))
                    assertNull(name, findComponent(workspace.panel, AbstractButton.class, name));
                assertNotNull(findComponent(workspace.panel, AbstractButton.class, "Find strings"));
                assertNull(findComponent(workspace.panel, JLabel.class, "All addresses"));
            });
        }
    }

    @Test
    public void memoryInspectionReadsLiveBankAndCommonCellsWithoutCapture() throws Exception {
        byte[][] cells = {{1, 2, 3, 4}, {9, 8, 7, 6}};
        try (Workspace workspace = workspace(cells, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.showMemory();
                workspace.panel.navigate(1, 1);
                assertEquals("08", workspace.table.getValueAt(1, 1));
                cells[1][1] = 0x41; // Direct read must see the edit even before a UI notification.
                assertEquals("41", workspace.table.getValueAt(1, 1));
                cells[0][3] = 0x55;
                assertEquals("55", workspace.table.getValueAt(3, 1));
                assertEquals(0, workspace.machine.memory.getSelectedBank());
            });
        }
    }

    @Test
    public void liveStringResultsAreDiscardedAfterMemoryChanges() throws Exception {
        byte[][] cells = {{65, 66, 67, 68, 0}};
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(cells, cells[0].length, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.showMemory();
                workspace.panel.setInspectionReadyListener(shown::countDown);
                button(workspace.panel, "Find strings").doClick(0);
            });
            assertTrue("Live string scan should finish", shown.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(1, workspace.table.getRowCount());
                assertEquals("ABCD", workspace.table.getValueAt(0, 4));
                cells[0][0] = 90;
                workspace.machine.memoryChanged(0);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(5, workspace.table.getRowCount());
                assertEquals("", workspace.table.getValueAt(0, 4));
                assertEquals("5A", workspace.table.getValueAt(0, 1));
            });
        }
    }

    @Test
    public void repeatedCpuCallbacksQueueOnePausedRefresh() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> session.watches.add("A"))) {
            SwingUtilities.invokeAndWait(() -> {
                clearInvocations(workspace.machine.debug);
                for (int i = 0; i < 100; i++)
                    for (net.emustudio.emulib.plugins.cpu.CPU.CPUListener listener : workspace.machine.cpuListeners)
                        listener.internalStateChanged();
            });
            SwingUtilities.invokeAndWait(() -> {});
            verify(workspace.machine.debug, times(1)).readRegister("A");
        }
    }

    @Test
    public void closingSessionDuringLiveScanDoesNotPublishOrNotifyInspectionReady() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), finished = new CountDownLatch(1);
        AtomicInteger readyCalls = new AtomicInteger();
        try (Workspace workspace = workspace(new byte[][] {{65, 66, 67, 68, 0}}, 5, session -> {})) {
            doAnswer(call -> { finished.countDown(); return null; })
                    .when(workspace.dialogs).showError(anyString(), eq("Debugger"));
            SwingUtilities.invokeAndWait(() -> {
                when(workspace.machine.memory.readBank(anyInt(), anyInt())).thenAnswer(call -> {
                    if ("debugger-analysis".equals(Thread.currentThread().getName())) {
                        entered.countDown();
                        assertTrue("Test should release live read", release.await(5, TimeUnit.SECONDS));
                    }
                    return (byte) 65;
                });
                workspace.panel.showMemory();
                workspace.panel.setInspectionReadyListener(readyCalls::incrementAndGet);
                button(workspace.panel, "Find strings").doClick(0);
            });
            try {
                assertTrue("Live read should start", entered.await(5, TimeUnit.SECONDS));
                workspace.session.close();
                workspace.session = null;
                release.countDown();
                assertTrue("Closed session should reject scan", finished.await(5, TimeUnit.SECONDS));
                SwingUtilities.invokeAndWait(() -> {
                    assertEquals(0, readyCalls.get());
                    assertEquals(5, workspace.table.getRowCount());
                });
            } finally { release.countDown(); }
        }
    }

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void browsingAnotherBankChangesInspectionWithoutMovingCpu() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2, 3}, {4, 5, 3}}, 2, session -> {})) {
            List<String> selections = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.setAddressSelectionListener((address, bank, visit) ->
                        selections.add(address + ":" + bank + ":" + visit));
                workspace.panel.navigate(1, 1);

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
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(-1, 0));
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(2, 0));
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(0, 1));
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
        DebugSession.Rule read = new DebugSession.AccessRule("read buffer", "READ", 0, -1, "", 1);
        DebugSession.Rule execute = new DebugSession.Rule("loop", "EXEC", 1, "", 1);
        DebugSession.Rule write = new DebugSession.AccessRule("write buffer", "WRITE", 2, -1, "", 1);
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
        DebugSession.Rule execute = new DebugSession.Rule("loop", "EXEC", 0, "", 1);
        DebugSession.Rule read = new DebugSession.AccessRule("read buffer", "READ", 1, -1, "", 1);
        DebugSession.Rule write = new DebugSession.AccessRule("write buffer", "WRITE", 2, -1, "", 1);
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
    public void executionControlsHaveNoBankSelectorWhileMemoryWatchesKeepTheirBank() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}, {0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.showBreakpoints();
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                Container breakpoints = (Container) tabs.getComponentAt(tabs.indexOfTab("Breakpoints"));
                assertNull("Execution breakpoint form must not select a bank",
                        findComponent(breakpoints, JComboBox.class, null));
                button(breakpoints, "Add breakpoint").doClick(0);
                JList<?> rows = component(breakpoints, JList.class, "Breakpoints");
                assertFalse(rows.getModel().getElementAt(0).toString().contains("bank"));

                Container watches = workspace.panel.watchesPanel();
                JComboBox<?> bank = component(watches, JComboBox.class, "Memory watch bank");
                bank.setSelectedIndex(2); // All banks, Bank 0, Bank 1.
                button(watches, "Add memory watch").doClick(0);
                DebugSession.AccessRule watch = (DebugSession.AccessRule) workspace.session.rules.stream()
                        .filter(rule -> rule instanceof DebugSession.AccessRule).findFirst().orElseThrow();
                assertEquals(1, watch.bank);
                assertEquals("WRITE", watch.kind);
            });
        }
    }

    @Test
    public void memoryControlsInspectAnExplicitBankWithoutChangingCpuOrBreakpoints() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}, {3, 4}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.showMemory();
                JTextField address = component(workspace.panel, JTextField.class, "Memory address");
                JComboBox<?> bank = component(workspace.panel, JComboBox.class, "Memory inspection bank");
                address.setText("0x1");
                bank.setSelectedIndex(1);
                button(workspace.panel, "Inspect address").doClick(0);
                assertFalse(workspace.panel.isInstructionView());
                assertEquals(1, workspace.table.getSelectedRow());
                assertEquals("0x0001", address.getText());
                assertEquals(1, bank.getSelectedItem());
                assertEquals(0, workspace.machine.memory.getSelectedBank());
                assertTrue(workspace.session.rules.isEmpty());
                assertEquals(0, workspace.breakpointChanges.get());

                workspace.panel.navigate(0, 0);
                assertEquals("0x0000", address.getText());
                assertEquals(0, bank.getSelectedItem());
            });
            verify(workspace.machine.cpu, never()).setInstructionLocation(anyInt());
            verify(workspace.machine.cpu, never()).setBreakpoint(anyInt());
        }
    }

    @Test
    public void instructionRowsShowAnnotationsAndBreakpointsWithoutExtraColumns() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {
            try {
                session.annotations().put(new AnnotationIndex.Entry(0, 0, "start", "entry point", "CODE"));
            } catch (Exception e) { throw new RuntimeException(e); }
        })) {
            SwingUtilities.invokeAndWait(() -> {
                for (int col = 0; col < workspace.table.getColumnCount(); col++) {
                    String name = workspace.table.getColumnName(col);
                    assertFalse(List.of("bp", "Label", "Comment").contains(name));
                    assertTrue(workspace.table.getColumnModel().getColumn(col).getMinWidth()
                            >= workspace.table.getColumnModel().getColumn(col).getPreferredWidth());
                }
                assertEquals(JTable.AUTO_RESIZE_OFF, workspace.table.getAutoResizeMode());
                java.awt.Rectangle cell = workspace.table.getCellRect(0, 0, true);
                java.awt.event.MouseEvent hover = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, cell.x + 1, cell.y + 1, 0, false);
                assertEquals("start — entry point", workspace.table.getToolTipText(hover));
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 0)));
                JComponent rendered = (JComponent) workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(0, 0), 0, 0);
                assertTrue(rendered.getFont().isBold());
            });
        }
    }

    @Test
    public void setAndUnsetRequestsOnlyEditTheBreakpoint() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 1)));
                assertTrue(workspace.session.isBreakpointSet(1));
                assertFalse(workspace.panel.isInspectionPinned());
                assertTrue(workspace.panel.isInstructionView());
                assertEquals(1, workspace.breakpointChanges.get());

                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.UNSET, 1)));
                assertFalse(workspace.session.isBreakpointSet(1));
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
                        BreakpointDialog.Operation.SET, 2)));
                assertTrue(workspace.session.rules.isEmpty());
                assertEquals(0, workspace.breakpointChanges.get());
            });
            verify(workspace.dialogs).showError("Address outside CPU", "Debugger");
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

    @Test
    public void breakpointHitsFollowTheNextPcUnlessTheUserManuallyPinsInspection() throws Exception {
        AtomicInteger pc = new AtomicInteger(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0, 0}}, 4,
                session -> {
                    when(session.cpu.getInstructionLocation()).thenAnswer(call -> pc.get());
                    session.setBreakpoint(1, true);
                })) {
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.session.beforeInstruction(1, 0));
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(workspace.panel.isInstructionView());
                assertEquals(1, workspace.table.getSelectedRow());
                assertFalse(workspace.panel.isInspectionPinned());
            });

            SwingUtilities.invokeAndWait(() -> {
                pc.set(2);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(2, workspace.table.getSelectedRow());
                JCheckBoxMenuItem pin = (JCheckBoxMenuItem) workspace.table.getComponentPopupMenu().getComponent(0);
                pin.doClick(0);
                assertTrue(workspace.panel.isInspectionPinned());
            });

            SwingUtilities.invokeAndWait(() -> {
                pc.set(3);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("Only the user's explicit pin should hold inspection", 2, workspace.table.getSelectedRow());
                JCheckBoxMenuItem pin = (JCheckBoxMenuItem) workspace.table.getComponentPopupMenu().getComponent(0);
                pin.doClick(0);
                assertFalse(workspace.panel.isInspectionPinned());
            });
            SwingUtilities.invokeAndWait(() -> workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK));
            SwingUtilities.invokeAndWait(() -> assertEquals(3, workspace.table.getSelectedRow()));
        }
    }

    @Test
    public void reachableFlowDecodesOffEdtAndGraphNavigatesOnDoubleClick() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), shown = new CountDownLatch(1);
        AtomicBoolean decodedOnEdt = new AtomicBoolean();
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(call -> (int) call.getArgument(0) + 1);
        when(disassembler.disassemble(anyInt())).thenAnswer(call ->
                new DisassembledInstruction(call.getArgument(0), "NOP", "00"));
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {
            Disassembler live = session.cpu.getDisassembler();
            when(live.forBank(0)).thenReturn(Optional.of(disassembler));
            when(disassembler.getFlow(0)).thenAnswer(call -> {
                decodedOnEdt.set(SwingUtilities.isEventDispatchThread());
                entered.countDown();
                assertTrue("Test should release decoder", release.await(10, TimeUnit.SECONDS));
                return Optional.of(new InstructionFlow(InstructionFlow.Kind.JUMP, 1, true, List.of(1)));
            });
            when(disassembler.getFlow(1)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.STOP, 2, false, List.of())));
        })) {
            try {
                workspace.session.annotations().put(new AnnotationIndex.Entry(0, 0, "start", "", "CODE"));
                workspace.session.annotations().add(new AnnotationIndex.Entry(-1, 0, 0, 2, "region", "", "UNKNOWN"));
                SwingUtilities.invokeAndWait(() -> {
                    AbstractButton goTo = button(workspace.panel, "Go to instruction");
                    assertFalse(goTo.isEnabled());
                    goTo.addPropertyChangeListener("enabled", event -> {
                        if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                    });
                    button(workspace.panel, "Analyze from address").doClick(0);
                    assertFalse("Analysis should disable inspection before worker starts", workspace.table.isEnabled());
                });
                assertTrue("Background decoder should start", entered.await(5, TimeUnit.SECONDS));
                assertFalse("Decoder must not run on Swing event thread", decodedOnEdt.get());
                CountDownLatch responsive = new CountDownLatch(1);
                SwingUtilities.invokeLater(() -> { workspace.session.requirePaused(); responsive.countDown(); });
                assertTrue("Paused queries on EDT must not wait for live flow decode",
                        responsive.await(5, TimeUnit.SECONDS));
                release.countDown();
                assertTrue("Published instructions should reach UI", shown.await(5, TimeUnit.SECONDS));
                SwingUtilities.invokeAndWait(() -> {
                    assertNull(findComponent(workspace.panel, JList.class, "Static references"));
                    JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                    JScrollPane graphScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, graph);
                    assertEquals(30, graphScroll.getVerticalScrollBar().getUnitIncrement());
                    assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(), 0, 260, 40, 0, false)).contains("Branch"));
                    assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(), 0, 260, 40, 0, false)).contains("start"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 10, 25, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(graph.getAccessibleContext().getAccessibleDescription().contains("start"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 120, 25, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(graph.getAccessibleContext().getAccessibleDescription().contains("region"));
                    JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                    tabs.setSelectedIndex(tabs.indexOfTab("References"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertFalse(button(workspace.panel, "Go to instruction").isEnabled());
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 260, 130, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(button(workspace.panel, "Go to instruction").isEnabled());
                    button(workspace.panel, "Go to instruction").doClick(0);
                    assertTrue(workspace.panel.isInstructionView());
                    assertEquals(1, workspace.table.getSelectedRow());
                    tabs.setSelectedIndex(tabs.indexOfTab("References"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 260, 130, 2, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(workspace.panel.isInstructionView());
                    assertEquals(1, workspace.table.getSelectedRow());
                    assertTrue(workspace.table.isEnabled());
                });
                verify(workspace.session.cpu.getDisassembler(), never()).getFlow(anyInt());
            } finally { release.countDown(); }
        }
    }

    @Test
    public void unsupportedExplicitBankDecoderReportsDiagnosticWithoutDecodingMappedBank() throws Exception {
        CountDownLatch failed = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}, {0, 0}}, 2,
                session -> when(session.cpu.getDisassembler().forBank(anyInt())).thenReturn(Optional.empty()))) {
            doAnswer(call -> { failed.countDown(); return null; }).when(workspace.dialogs).showError(anyString(), eq("Debugger"));
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(0, 1);
                button(workspace.panel, "Analyze from address").doClick(0);
            });
            assertTrue("Unsupported bank decoder should be reported", failed.await(5, TimeUnit.SECONDS));
            org.mockito.ArgumentCaptor<String> message = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(workspace.dialogs).showError(message.capture(), eq("Debugger"));
            assertTrue(message.getValue().toLowerCase(java.util.Locale.ROOT).contains("bank"));
            verify(workspace.session.cpu.getDisassembler(), never()).getFlow(anyInt());
        }
    }


    private Workspace workspace(byte[][] banks, int common, Consumer<DebugSession> configure) throws Exception {
        Path settings = temporary.newFolder().toPath().resolve("debug.properties");
        Workspace workspace = new Workspace();
        workspace.machine = new DebuggerTestMachine(banks, common, false);
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.forBank(anyInt())).thenReturn(Optional.of(disassembler));
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
            assertTrue("Initial annotation loading should finish", ready.await(5, TimeUnit.SECONDS));
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
            if (type.isInstance(child) && (name == null || name.equals(child.getName()) || name.equals(accessible)
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
