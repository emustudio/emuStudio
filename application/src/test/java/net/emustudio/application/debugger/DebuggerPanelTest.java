/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
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
            assertEquals("entry", workspace.session.annotations().get(1).label);
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
            assertNull(workspace.session.annotations().get(0));
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
                });
                editAnnotationDialog(panel, "New annotation", "Annotate inspected address", dialog -> {
                    annotationField(dialog, "Address").setText("0x0002");
                    annotationField(dialog, "End address").setText("0x0005");
                    annotationField(dialog, "Label").setText("second");
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
            assertEquals(1, updated.address);
            assertEquals(7, updated.end);
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
    public void memoryInspectionTabAndControlsAreAbsent() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                assertEquals(-1, tabs.indexOfTab("Memory"));
                for (String name : List.of("Inspect address", "Find strings", "Show source"))
                    assertNull(name, findComponent(workspace.panel, AbstractButton.class, name));
                assertNotNull(workspace.panel.watchesPanel());
                assertTrue(tabs.indexOfTab("Annotations") >= 0);
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

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void browsingAnAddressChangesInspectionWithoutMovingCpu() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2, 3}, {4, 5, 3}}, 2, session -> {})) {
            List<String> selections = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.setAddressSelectionListener((address, visit) ->
                        selections.add(address + ":" + visit));
                workspace.panel.navigate(1);

                assertTrue(workspace.panel.isInstructionView());
                assertEquals("Instruction listing", workspace.table.getAccessibleContext().getAccessibleName());
            });

            assertEquals(List.of("1:true"), selections);
            verify(workspace.machine.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test
    public void navigationRejectsAddressesOutsideMemory() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(-1));
                assertThrows(IllegalArgumentException.class, () -> workspace.panel.navigate(2));
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

                workspace.panel.setExecutionRunning(false);
                workspace.panel.showInstructions();
                assertTrue(workspace.table.isVisible());
                assertTrue(workspace.table.isEnabled());
            });
        }
    }

    @Test
    public void breakpointListActionsTargetVisibleExecutionRuleAmongMemoryWatches() throws Exception {
        DebugSession.Rule read = new DebugSession.AccessRule("read buffer", "READ", 0, "", 1);
        DebugSession.Rule execute = new DebugSession.Rule("loop", "EXEC", 1, "", 1);
        DebugSession.Rule write = new DebugSession.AccessRule("write buffer", "WRITE", 2, "", 1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3,
                session -> session.rules.addAll(List.of(read, execute, write)))) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                Container breakpoints = (Container) tabs.getComponentAt(tabs.indexOfTab("Breakpoints"));
                assertNull(findComponent(breakpoints, JToggleButton.class, "Advanced breakpoint options"));
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
        DebugSession.Rule read = new DebugSession.AccessRule("read buffer", "READ", 1, "", 1);
        DebugSession.Rule write = new DebugSession.AccessRule("write buffer", "WRITE", 2, "", 1);
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
    public void executionControlsAndMemoryWatchesUseCpuVisibleAddresses() throws Exception {
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
                assertNull(findComponent(watches, JComboBox.class, "Memory watch bank"));
                button(watches, "Add memory watch").doClick(0);
                DebugSession.AccessRule watch = (DebugSession.AccessRule) workspace.session.rules.stream()
                        .filter(rule -> rule instanceof DebugSession.AccessRule).findFirst().orElseThrow();
                assertEquals(0, watch.address);
                assertEquals("WRITE", watch.kind);
            });
        }
    }

    @Test
    public void instructionRowsShowAnnotationsAndBreakpointsWithoutExtraColumns() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {
            try {
                session.annotations().put(new AnnotationIndex.Entry(0, "start", "CODE"));
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
                assertEquals("start", workspace.table.getToolTipText(hover));
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 0)));
                JComponent rendered = (JComponent) workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(0, 0), 0, 0);
                assertTrue(rendered.getFont().isBold());
            });
        }
    }

    @Test
    public void instructionTooltipCoversAnnotationRangeAndUsesLaneColors() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3, session -> {
            try {
                session.annotations().add(new AnnotationIndex.Entry(-1, 0, 2, "range", "UNKNOWN"));
                session.annotations().add(new AnnotationIndex.Entry(-1, 1, 2, "inner", "UNKNOWN"));
            } catch (Exception e) { throw new RuntimeException(e); }
        })) {
            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                java.awt.Rectangle first = workspace.table.getCellRect(0, 0, true);
                java.awt.event.MouseEvent hoverFirst = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, first.x + 1, first.y + 1, 0, false);
                assertEquals("range", workspace.table.getToolTipText(hoverFirst));
                java.awt.Color lane = java.awt.Color.getHSBColor(0f, .55f, .78f);
                java.awt.Color base = graph.getBackground();
                java.awt.Color expected = new java.awt.Color((lane.getRed() * 28 + base.getRed() * 227 + 127) / 255,
                        (lane.getGreen() * 28 + base.getGreen() * 227 + 127) / 255,
                        (lane.getBlue() * 28 + base.getBlue() * 227 + 127) / 255);
                assertEquals(expected, workspace.table.createToolTip().getBackground());

                java.awt.Rectangle second = workspace.table.getCellRect(1, 0, true);
                java.awt.event.MouseEvent hoverSecond = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, second.x + 1, second.y + 1, 0, false);
                assertEquals("range | inner", workspace.table.getToolTipText(hoverSecond));
                assertNotEquals(expected, workspace.table.createToolTip().getBackground());

                java.awt.Rectangle third = workspace.table.getCellRect(2, 0, true);
                java.awt.event.MouseEvent hoverThird = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, third.x + 1, third.y + 1, 0, false);
                assertNull(workspace.table.getToolTipText(hoverThird));
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
                assertTrue(workspace.session.beforeInstruction(1));
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.panel.isInstructionView());
                assertEquals(1, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                assertFalse(workspace.panel.isInspectionPinned());
            });

            SwingUtilities.invokeAndWait(() -> {
                pc.set(2);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(2, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                JCheckBoxMenuItem pin = (JCheckBoxMenuItem) workspace.table.getComponentPopupMenu().getComponent(3);
                pin.doClick(0);
                assertTrue(workspace.panel.isInspectionPinned());
            });

            SwingUtilities.invokeAndWait(() -> {
                pc.set(3);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("Only the user's explicit pin should hold inspection", 2, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                JCheckBoxMenuItem pin = (JCheckBoxMenuItem) workspace.table.getComponentPopupMenu().getComponent(3);
                pin.doClick(0);
                assertFalse(workspace.panel.isInspectionPinned());
            });
            SwingUtilities.invokeAndWait(() -> workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK));
            SwingUtilities.invokeAndWait(() -> assertEquals(3, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow())));
        }
    }

    @Test
    public void analyzeReferencesStartsAtPcInsteadOfInspectedAddress() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0, 0, 0}}, 5, session -> {
            when(session.cpu.getInstructionLocation()).thenReturn(3);
            Disassembler decoder = session.cpu.getDisassembler();
            when(decoder.getFlow(3)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.JUMP, 4, true, List.of(1))));
            when(decoder.getFlow(1)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.RETURN, 2, false, List.of())));
            when(decoder.getFlow(4)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.STOP, 5, false, List.of())));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(0);
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Analyze from PC").doClick(0);
            });

            assertTrue("Flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            ProgramIndex.Snapshot snapshot = workspace.session.programIndex().snapshot();
            assertNull(snapshot.instructionAt(0));
            assertNotNull(snapshot.instructionAt(1));
            assertNull(snapshot.instructionAt(2));
            assertNotNull(snapshot.instructionAt(3));
            assertNotNull(snapshot.instructionAt(4));
            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                        0, 0, 40, 40, 0, false)).contains("0003"));
                assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                        0, 0, 40, 130, 0, false)).contains("0001"));
                button(workspace.panel, "Go to instruction").doClick(0);
                assertEquals(3, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
            });
        }
    }

    @Test
    public void selectingGraphInstructionUpdatesDebuggerAndAnalysisStart() throws Exception {
        CountDownLatch shown = new CountDownLatch(1), reanalyzed = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {
            Disassembler decoder = session.cpu.getDisassembler();
            when(decoder.getFlow(0)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.JUMP, 1, true, List.of(1))));
            when(decoder.getFlow(1)).thenAnswer(call -> {
                if (reads.incrementAndGet() == 2) reanalyzed.countDown();
                return Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 2, false, List.of()));
            });
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Analyze from PC").doClick(0);
            });
            assertTrue(shown.await(5, TimeUnit.SECONDS));

            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                        0, 0, 40, 130, 1, false, java.awt.event.MouseEvent.BUTTON1));

                assertEquals(1, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                assertEquals("Inspection", tabs.getTitleAt(tabs.getSelectedIndex()));
                button(workspace.panel, "Analyze from selected instruction").doClick(0);
            });
            assertTrue("Analysis should start at the selected graph instruction", reanalyzed.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void inspectionCanAnalyzeSelectedInstructionInsteadOfPc() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0, 0}}, 4, session -> {
            when(session.cpu.getInstructionLocation()).thenReturn(3);
            when(session.cpu.getDisassembler().getFlow(1)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.STOP, 2, false, List.of())));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(1);
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Analyze from selected instruction").doClick(0);
            });

            assertTrue("Flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            assertNotNull(workspace.session.programIndex().snapshot().instructionAt(1));
            assertNull(workspace.session.programIndex().snapshot().instructionAt(3));
        }
    }

    @Test
    public void inspectPopupAnalyzesRightClickedInstruction() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3, session -> {
            when(session.cpu.getDisassembler().getFlow(1)).thenReturn(Optional.of(
                    new InstructionFlow(InstructionFlow.Kind.STOP, 2, false, List.of())));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(0);
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                workspace.table.setSize(workspace.table.getPreferredSize());
                java.awt.Rectangle row = workspace.table.getCellRect(1, 1, true);
                workspace.table.dispatchEvent(new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_PRESSED, 0, java.awt.event.InputEvent.BUTTON3_DOWN_MASK,
                        row.x + 2, row.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON3));
                assertEquals(1, workspace.table.getSelectedRow());
                ((JMenuItem) workspace.table.getComponentPopupMenu().getComponent(0)).doClick(0);
            });

            assertTrue("Flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            assertNotNull(workspace.session.programIndex().snapshot().instructionAt(1));
            assertNull(workspace.session.programIndex().snapshot().instructionAt(0));
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                assertEquals("Inspection", tabs.getTitleAt(tabs.getSelectedIndex()));
            });
        }
    }

    @Test
    public void goToSourcePopupUsesSelectedInstruction() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3, session -> {})) {
            net.emustudio.emulib.plugins.compiler.SourceCodePosition position =
                    mock(net.emustudio.emulib.plugins.compiler.SourceCodePosition.class);
            workspace.session.annotationEngine.put(new net.emustudio.emulib.runtime.annotations.AnnotationEngine.Entry(
                    1, 0, 1, 2, "source", "", position));

            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(1);
                ((JMenuItem) workspace.table.getComponentPopupMenu().getComponent(1)).doClick(0);
            });

            assertSame(position, workspace.sourcePosition.get());
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
            when(session.cpu.getDisassembler()).thenReturn(disassembler);
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
                workspace.session.annotations().put(new AnnotationIndex.Entry(0, "start", "CODE"));
                workspace.session.annotations().add(new AnnotationIndex.Entry(-1, 0, 2, "region", "UNKNOWN"));
                SwingUtilities.invokeAndWait(() -> {
                    AbstractButton goTo = button(workspace.panel, "Go to instruction");
                    assertFalse(goTo.isEnabled());
                    goTo.addPropertyChangeListener("enabled", event -> {
                        if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                    });
                    button(workspace.panel, "Analyze from PC").doClick(0);
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
                AnnotationIndex annotationIndex = workspace.session.annotations();
                SwingUtilities.invokeAndWait(() -> {
                    assertNull(findComponent(workspace.panel, JList.class, "Static references"));
                    JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                    JScrollPane graphScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, graph);
                    assertEquals(30, graphScroll.getVerticalScrollBar().getUnitIncrement());
                    AtomicInteger symbolReads = new AtomicInteger();
                    workspace.panel.setSymbols(() -> { symbolReads.incrementAndGet(); return List.of(); });
                    workspace.panel.setSourceNavigationStatus("Source mapping current", true);
                    graph.setSize(graph.getPreferredSize());
                    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(600, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D graphics = image.createGraphics();
                    try {
                        graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                        graph.paint(graphics);
                        graph.paint(graphics);
                        assertEquals("Selection repaints should reuse compiled labels", 1, symbolReads.get());
                        workspace.panel.setSymbols(() -> { symbolReads.incrementAndGet(); return List.of(); });
                        graph.paint(graphics);
                        assertEquals("Changing symbols should refresh graph labels", 2, symbolReads.get());
                        graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_RELEASED,
                                System.currentTimeMillis(), 0, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                        graph.paint(graphics);
                        assertEquals("Paint after selection should recheck live symbols", 3, symbolReads.get());
                    } finally { graphics.dispose(); }
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
                    doubleClickLaneAndCancel(graph, 120, 25, "region");
                    doubleClickLaneAndCancel(graph, 200, 80, "region");
                    assertTrue(annotationIndex.entries().stream().anyMatch(entry -> entry.label.equals("region")));
                    JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                    tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                    for (String name : List.of("Analyze from PC", "Analyze from selected instruction", "Go to instruction")) {
                        AbstractButton action = button(workspace.panel, name);
                        assertTrue(action.getText() == null || action.getText().isEmpty());
                        assertNotNull(action.getIcon());
                    }
                    JViewport graphViewport = graphScroll.getViewport();
                    graphViewport.setViewSize(graph.getPreferredSize());
                    graphViewport.setExtentSize(new Dimension(200, 100));
                    graphViewport.setViewPosition(new Point(270, 130));
                    Point beforeClick = graphViewport.getViewPosition();
                    for (int count = 1; count <= 2; count++) {
                        graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 280, 140, count, false, java.awt.event.MouseEvent.BUTTON1));
                        graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_RELEASED,
                                System.currentTimeMillis(), 0, 280, 140, count, false, java.awt.event.MouseEvent.BUTTON1));
                        graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                                System.currentTimeMillis(), 0, 280, 140, count, false, java.awt.event.MouseEvent.BUTTON1));
                        if (count == 1) assertEquals(beforeClick, graphViewport.getViewPosition());
                    }
                    assertTrue(workspace.panel.isInstructionView());
                    assertEquals(1, workspace.table.getSelectedRow());
                    tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertFalse(button(workspace.panel, "Go to instruction").isEnabled());
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 260, 130, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(button(workspace.panel, "Go to instruction").isEnabled());
                    button(workspace.panel, "Go to instruction").doClick(0);
                    assertTrue(workspace.panel.isInstructionView());
                    assertEquals(1, workspace.table.getSelectedRow());
                    tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 260, 130, 2, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(workspace.panel.isInstructionView());
                    assertEquals(1, workspace.table.getSelectedRow());
                    assertTrue(workspace.table.isEnabled());
                    tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                    JComboBox<?> zoom = component(workspace.panel, JComboBox.class, "Graph zoom");
                    JViewport viewport = graphScroll.getViewport();
                    viewport.setExtentSize(new Dimension(200, 150));
                    zoom.setSelectedItem("200%");
                    assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(), 0, 520, 80, 0, false)).contains("start"));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 520, 260, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(button(workspace.panel, "Go to instruction").isEnabled());
                    viewport.setViewPosition(new Point(100, 100));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_PRESSED,
                            System.currentTimeMillis(), java.awt.event.InputEvent.BUTTON3_DOWN_MASK, 300, 200, 1, false, java.awt.event.MouseEvent.BUTTON3));
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_DRAGGED,
                            System.currentTimeMillis(), java.awt.event.InputEvent.BUTTON3_DOWN_MASK, 280, 170, 0, false, java.awt.event.MouseEvent.NOBUTTON));
                    assertEquals(new Point(120, 130), viewport.getViewPosition());
                    assertTrue(button(workspace.panel, "Go to instruction").isEnabled());
                    graphScroll.dispatchEvent(new java.awt.event.MouseWheelEvent(graphScroll, java.awt.event.MouseEvent.MOUSE_WHEEL,
                            System.currentTimeMillis(), java.awt.event.InputEvent.CTRL_DOWN_MASK, 50, 50, 0, false,
                            java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                    assertEquals("250%", zoom.getSelectedItem());
                    zoom.setSelectedItem("100%");
                    int before = annotationIndex.entries().size();
                    dragAnnotation(graph, dialog -> {
                        assertEquals("0x0000", annotationField(dialog, "Address").getText());
                        assertEquals("0x0001", annotationField(dialog, "End address").getText());
                        JRootPane root = ((JDialog) dialog).getRootPane();
                        Object action = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                                .get(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
                        root.getActionMap().get(action).actionPerformed(new java.awt.event.ActionEvent(root, 0, "escape"));
                    });
                    assertEquals(before, annotationIndex.entries().size());
                    dragAnnotation(graph, dialog -> {
                        button(dialog, "Save annotation").doClick(0);
                    });
                    assertEquals(before + 1, annotationIndex.entries().size());
                    assertTrue(annotationIndex.entries().stream().anyMatch(entry ->
                            entry.label.isEmpty() && entry.address == 0 && entry.end == 2));
                });
            } finally { release.countDown(); }
        }
    }

    private static void dragAnnotation(JPanel graph, Consumer<Container> action) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        AtomicBoolean opened = new AtomicBoolean();
        Timer timer = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog) || !window.isShowing() || !"New annotation".equals(((JDialog) window).getTitle())) continue;
                ((Timer) event.getSource()).stop();
                opened.set(true);
                try { action.accept((Container) window); }
                catch (Throwable error) { failure.set(error); }
                finally { window.dispose(); }
            }
        });
        timer.start();
        try {
            graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_PRESSED,
                    1, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 245, 20, 1, false, java.awt.event.MouseEvent.BUTTON1));
            graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_DRAGGED,
                    2, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 470, 180, 0, false, java.awt.event.MouseEvent.NOBUTTON));
            graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_RELEASED,
                    3, 0, 470, 180, 1, false, java.awt.event.MouseEvent.BUTTON1));
        } finally { timer.stop(); }
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertTrue("Expected annotation dialog", opened.get());
    }

    private static void doubleClickLaneAndCancel(JPanel graph, int x, int y, String expectedLabel) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        AtomicBoolean opened = new AtomicBoolean();
        Timer timer = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog) || !window.isShowing() || !"Edit annotation".equals(((JDialog) window).getTitle())) continue;
                ((Timer) event.getSource()).stop();
                opened.set(true);
                try {
                    assertEquals(expectedLabel, annotationField((Container) window, "Label").getText());
                    JRootPane root = ((JDialog) window).getRootPane();
                    Object action = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .get(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
                    root.getActionMap().get(action).actionPerformed(new java.awt.event.ActionEvent(root, 0, "escape"));
                } catch (Throwable error) { failure.set(error); }
                finally { window.dispose(); }
            }
        });
        timer.start();
        try {
            graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, x, y, 2, false, java.awt.event.MouseEvent.BUTTON1));
        } finally { timer.stop(); }
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertTrue("Expected annotation editor", opened.get());
    }

    @Test
    public void flowUsesCpuDecoderForInspectedAddress() throws Exception {
        CountDownLatch decoded = new CountDownLatch(1), shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}, {0, 0}}, 2, session -> {
            Disassembler decoder = session.cpu.getDisassembler();
            when(decoder.getFlow(0)).thenAnswer(call -> {
                decoded.countDown();
                return Optional.of(new InstructionFlow(InstructionFlow.Kind.STOP, 1, false, List.of()));
            });
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                workspace.panel.navigate(0);
                button(workspace.panel, "Analyze from PC").doClick(0);
            });
            assertTrue("CPU decoder should run", decoded.await(5, TimeUnit.SECONDS));
            assertTrue("Flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").doClick(0);
                assertTrue(workspace.panel.isInstructionView());
            });
            verify(workspace.dialogs, never()).showError(anyString(), eq("Debugger"));
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
                        workspace.sourcePosition::set, workspace.breakpointChanges::incrementAndGet,
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
        final java.util.concurrent.atomic.AtomicReference<net.emustudio.emulib.plugins.compiler.SourceCodePosition> sourcePosition = new java.util.concurrent.atomic.AtomicReference<>();

        @Override public void close() throws Exception {
            if (panel != null) SwingUtilities.invokeAndWait(panel::close);
            if (session != null) session.close();
        }
    }
}
