/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerTestMachine;
import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.UserAnnotations;
import net.emustudio.application.emulation.debugger.FlowAnalysis;
import net.emustudio.application.emulation.debugger.InstructionRecording;
import net.emustudio.application.emulation.debugger.SourceAddressMap;

import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.gui.dialogs.RecordingDialog;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ViewRow;
import net.emustudio.application.gui.LogCapture;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
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
import java.util.OptionalInt;
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
    public void annotationsAndBreakpointNavigationUseCpuAddressesRegardlessOfMemoryCellType() throws Exception {
        for (Class<?> cellType : List.of(Integer.class, Byte.class)) {
            try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {
                doReturn(cellType).when(session.memory).getCellTypeClass();
                when(session.memory.getSize()).thenReturn(0);
                session.config.annotations().add(new UserAnnotations.Entry(1, "entry", "CODE"));
                when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(1);
                session.setBreakpoint(1, true).join();
                clearInvocations(session.memory);
            })) {
                SwingUtilities.invokeAndWait(() -> {
                    assertTrue(button(workspace.panel, "Annotate inspected address").isEnabled());
                    JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                    JList<?> list = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                    assertEquals(1, list.getModel().getSize());
                    assertEquals("entry", ((MemoryAnnotations.Entry) list.getModel().getElementAt(0)).text);
                });
                workspace.machine.state(CPU.RunState.STATE_RUNNING);
                SwingUtilities.invokeAndWait(() -> {
                    assertTrue(workspace.session.beforeStep());
                    workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
                });
                SwingUtilities.invokeAndWait(() -> {
                    assertTrue(workspace.panel.isInstructionView());
                    assertEquals(1, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                });
                verify(workspace.machine.memory, never()).getCellTypeClass();
                verify(workspace.machine.memory, never()).getSize();
            }
        }
    }

    @Test
    public void flowRefreshKeepsInspectionToolbarAndListingEnabled() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8,
                session -> observed(session, 0, OptionalInt.of(1)))) {
            SwingUtilities.invokeAndWait(() -> {
                AbstractButton follow = button(workspace.panel, "Show instructions from PC");
                follow.doClick(0);
                assertTrue(follow.isEnabled());
                assertTrue(button(workspace.panel, "Fold identical analysis repeats").isEnabled());
                assertTrue(component(workspace.panel, JTextField.class, "Search annotations").isEnabled());
                assertTrue(recordingControl(workspace).isEnabled());
                assertTrue(workspace.table.isEnabled());
            });
        }
    }

    @Test
    public void stepDoesNotRenderCellsForSizingOrResetUserColumnWidths() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation())
                    .thenAnswer(call -> pc.get());
            when(session.cpu.step()).thenAnswer(call -> {
                pc.incrementAndGet();
                return CPU.RunState.STATE_STOPPED_BREAK;
            });
        })) {
            AtomicInteger rendered = new AtomicInteger();
            AtomicInteger updates = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                flushUiEvents();
                workspace.table.instructionModel().addTableModelListener(event -> updates.incrementAndGet());
                workspace.table.setDefaultRenderer(String.class, new javax.swing.table.DefaultTableCellRenderer() {
                    @Override public Component getTableCellRendererComponent(JTable table, Object value,
                            boolean selected, boolean focus, int row, int column) {
                        rendered.incrementAndGet();
                        return super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                    }
                });
                workspace.table.getColumnModel().getColumn(2).setPreferredWidth(350);
            });

            workspace.machine.execution.step().join();

            SwingUtilities.invokeAndWait(() -> {
                flushUiEvents();
                assertEquals("Steps must not measure table cells", 0, rendered.get());
                assertEquals("One table update per completed step", 1, updates.get());
                assertEquals(350, workspace.table.getColumnModel().getColumn(2).getPreferredWidth());
                assertTrue(workspace.table.instructionModel().isRowAtCurrentInstruction(1));
                assertEquals(-1, workspace.table.getSelectedRow());
            });
        }
    }

    @Test
    public void annotationSearchUsesEngineEntriesAndPreservesSelectionAfterReplacement() throws Exception {
        MemoryAnnotations.Entry initial = new MemoryAnnotations.Entry(UserAnnotations.OWNER, 100, 1, 4,
                "CODE", "original", null);
        MemoryAnnotations.Entry replacement = new MemoryAnnotations.Entry(UserAnnotations.OWNER, 100, 1, 4,
                "CODE", "updated", null);
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {
            session.annotationEngine.replaceOwner(UserAnnotations.OWNER, List.of(initial));
            session.annotationEngine.replaceOwner(42, List.of(new MemoryAnnotations.Entry(42, 1, 1, 4,
                    "label", "compiler label", null)));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                JList<?> list = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                assertEquals(1, list.getModel().getSize());
                assertSame(initial, list.getModel().getElementAt(0));
                list.setSelectedIndex(0);
                workspace.session.annotationEngine.replaceOwner(UserAnnotations.OWNER, List.of(replacement));
                workspace.panel.stepCompleted();
                flushUiEvents();
                assertSame(replacement, list.getSelectedValue());
                search.setText("compiler");
                assertEquals(0, list.getModel().getSize());
                search.setText("updated");
                assertEquals(1, list.getModel().getSize());
                assertSame(replacement, list.getModel().getElementAt(0));
            });
        }
    }

    @Test
    public void refreshingUnchangedAnnotationsDoesNotRebuildListOrClearSelection() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session ->
                session.config.annotations().add(new UserAnnotations.Entry(0, "start", "CODE")))) {
            AtomicInteger changes = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                JList<?> list = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                list.setSelectedIndex(0);
                list.getModel().addListDataListener(new javax.swing.event.ListDataListener() {
                    public void intervalAdded(javax.swing.event.ListDataEvent event) { changes.incrementAndGet(); }
                    public void intervalRemoved(javax.swing.event.ListDataEvent event) { changes.incrementAndGet(); }
                    public void contentsChanged(javax.swing.event.ListDataEvent event) { changes.incrementAndGet(); }
                });
                workspace.panel.stepCompleted();
                flushUiEvents();
                assertEquals(0, changes.get());
                assertEquals(0, list.getSelectedIndex());
                workspace.session.config.annotations().add(new UserAnnotations.Entry(1, "next", "CODE"));
                flushUiEvents();
                assertEquals(2, list.getModel().getSize());
                assertTrue(changes.get() > 0);
            });
        }
    }

    @Test
    public void annotationEventsUpdateSearchOnSwingAndIgnoreQueuedChangesAfterClose() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session ->
                session.config.annotations().add(new UserAnnotations.Entry(0, "start", "CODE")))) {
            AtomicInteger changes = new AtomicInteger();
            java.util.concurrent.atomic.AtomicReference<JList<?>> rows = new java.util.concurrent.atomic.AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                JList<?> list = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                rows.set(list);
                list.setSelectedIndex(0);
                list.getModel().addListDataListener(new javax.swing.event.ListDataListener() {
                    private void changed() {
                        assertTrue(SwingUtilities.isEventDispatchThread());
                        changes.incrementAndGet();
                    }
                    public void intervalAdded(javax.swing.event.ListDataEvent event) { changed(); }
                    public void intervalRemoved(javax.swing.event.ListDataEvent event) { changed(); }
                    public void contentsChanged(javax.swing.event.ListDataEvent event) { changed(); }
                });
            });
            // Model updates may arrive from a compiler or another worker, without a CPU step.
            workspace.session.config.annotations().add(new UserAnnotations.Entry(1, "next", "CODE"));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(2, rows.get().getModel().getSize());
                assertEquals(0, rows.get().getSelectedIndex());
                JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                search.setText("next");
                assertEquals(1, rows.get().getModel().getSize());
                assertEquals("next", ((MemoryAnnotations.Entry) rows.get().getModel().getElementAt(0)).text);
                workspace.session.config.annotations().add(new UserAnnotations.Entry(2, "next again", "CODE"));
                workspace.panel.close();
                changes.set(0);
                flushUiEvents();
                assertEquals(0, changes.get());
                assertEquals(1, rows.get().getModel().getSize());
            });
        }
    }

    @Test
    public void defaultInstructionSelectionStaysEmptyThroughTabsRefreshAndStepping() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session ->
                when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation())
                        .thenAnswer(call -> pc.get()))) {
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(-1, workspace.table.getSelectedRow());
                assertTrue(workspace.table.instructionModel().isRowAtCurrentInstruction(1));
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                workspace.panel.showInstructions();
                assertEquals(-1, workspace.table.getSelectedRow());
                workspace.table.instructionModel().setMaxRows(5);
                assertEquals(-1, workspace.table.getSelectedRow());
                pc.set(2);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(-1, workspace.table.getSelectedRow());
                assertTrue(workspace.table.instructionModel().isRowAtCurrentInstruction(2));
                workspace.panel.navigate(3);
                assertEquals(3, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                workspace.table.clearSelection();
                workspace.table.instructionModel().memoryChanged(0, 0);
                assertEquals(-1, workspace.table.getSelectedRow());
                pc.set(4);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(-1, workspace.table.getSelectedRow());
                assertTrue(workspace.table.instructionModel().isRowAtCurrentInstruction(2));
            });
        }
    }

    @Test
    public void recordingFailureIsLoggedWhileRunningAndClearsWhenAFreshCaptureStarts() throws Exception {
        try (LogCapture logs = new LogCapture(InstructionRecording.class);
             Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session ->
                session.startNewRecording(10, InstructionRecording.Mode.LAST, 0, 0))) {
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            workspace.session.failed("Instruction recording failed: disk full");
            assertTrue(logs.contains("Instruction recording failed: disk full"));
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel.watchesPanel(), "Refresh").doClick(0);
                flushUiEvents();
                assertNull(findComponent(workspace.panel, JLabel.class, "Debugger diagnostic"));
                assertEquals(CPU.RunState.STATE_RUNNING, workspace.machine.execution.getRunState());
                SwingUtilities.invokeLater(() -> button(activeRecordingDialog(), "Start recording").doClick(0));
                recordingControl(workspace).doClick(0);
                flushUiEvents();

                assertTrue(workspace.session.recording.isRecording());
                assertTrue(workspace.session.recording.error().isBlank());
                assertEquals(CPU.RunState.STATE_RUNNING, workspace.machine.execution.getRunState());
            });
        }
    }

    @Test
    public void flowDisplayUsesCapturedInstructionsWithoutBackgroundDecoding() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        AtomicBoolean decodedOffEdt = new AtomicBoolean();
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {
            when(session.cpu.getDecoder().decode(anyInt())).thenAnswer(call -> {
                if (!SwingUtilities.isEventDispatchThread()) decodedOffEdt.set(true);
                return instruction(call.getArgument(0));
            });
            observed(session, 0, OptionalInt.of(2));
            observed(session, 2, OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });
            assertTrue("Observed flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            assertFalse(decodedOffEdt.get());
            assertNotNull(workspace.session.flowAnalysis().instructionAt(2));
        }
    }

    @Test
    public void watchEditsKeepInvalidTextInactiveAndRestoreSupportedFormatsWhenCorrected() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            workspace.machine.values.put("A", 65L);
            SwingUtilities.invokeAndWait(() -> {
                Container watches = workspace.panel.watchesPanel();
                JTextField expression = component(watches, JTextField.class, "Expression");
                expression.setText("A ==");
                assertTrue(button(watches, "Add").isEnabled());
                button(watches, "Add").doClick(0);
                flushUiEvents();

                assertEquals(List.of("A =="), workspace.session.config.watches);
                JTable values = component(watches, JTable.class, "Watch expressions and values");
                assertTrue(renderedRowTooltip(values, 0).contains("Inactive"));
                assertTrue(workspace.session.watchErrors().containsKey("A =="));
                values.setRowSelectionInterval(0, 0);
                button(watches, "Edit watch").doClick(0);
                assertTrue(values.isEditing());
                JTextField editor = (JTextField) values.getEditorComponent();
                assertEquals("A ==", editor.getText());
                editor.setText("A");
                editor.postActionEvent();
                flushUiEvents();

                assertEquals(List.of("A"), workspace.session.config.watches);
                assertTrue(workspace.session.watchErrors().isEmpty());
                values.setRowSelectionInterval(0, 0);
                JComboBox<?> format = component(watches, JComboBox.class, "Watch value format");
                format.setSelectedItem("Hexadecimal");
                assertTrue(renderedRowTooltip(values, 0).contains("0x41"));
                assertEquals(0, values.getSelectedRow());
                assertNull(findComponent(watches, AbstractButton.class, "Edit register"));
            });
        }
    }

    @Test
    public void watchCellsEditOnDoubleClickAndEscapeDiscardsDraftAcrossRefresh() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {
            session.config.watches.add("A");
            session.refreshWatches().join();
        })) {
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = new JFrame();
                try {
                    frame.setContentPane(workspace.panel.watchesPanel());
                    frame.setSize(320, 260);
                    frame.setVisible(true);
                    JTable values = component(frame, JTable.class, "Watch expressions and values");
                    assertTrue(values.isCellEditable(0, 0));
                    assertFalse(values.isCellEditable(0, 1));
                    java.awt.Rectangle cell = values.getCellRect(0, 0, true);
                    values.dispatchEvent(new java.awt.event.MouseEvent(values, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, cell.x + 4, cell.y + 4, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertFalse(values.isEditing());
                    java.awt.Rectangle valueCell = values.getCellRect(0, 1, true);
                    values.dispatchEvent(new java.awt.event.MouseEvent(values, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, valueCell.x + 4, valueCell.y + 4, 2, false, java.awt.event.MouseEvent.BUTTON1));
                    assertFalse(values.isEditing());
                    values.dispatchEvent(new java.awt.event.MouseEvent(values, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, cell.x + 4, cell.y + 4, 2, false, java.awt.event.MouseEvent.BUTTON1));
                    assertTrue(values.isEditing());
                    JTextField editor = (JTextField) values.getEditorComponent();
                    assertEquals("A", editor.getSelectedText());
                    editor.setText("mem[0x0001]");
                    workspace.machine.values.put("A", 7L);
                    button(frame, "Refresh").doClick(0);
                    workspace.session.refreshWatches().join();
                    flushUiEvents();
                    assertTrue(values.isEditing());
                    assertEquals("mem[0x0001]", editor.getText());
                    Object cancel = values.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                            .get(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
                    values.getActionMap().get(cancel).actionPerformed(new java.awt.event.ActionEvent(values, 0, "cancel"));
                    flushUiEvents();
                    assertFalse(values.isEditing());
                    assertEquals(List.of("A"), workspace.session.config.watches);
                    assertEquals("A", values.getValueAt(0, 0));
                    assertTrue(values.getValueAt(0, 1).toString(), values.getValueAt(0, 1).toString().startsWith("7"));
                } finally { frame.dispose(); }
            });
        }
    }

    @Test
    public void watchesShowValuesWithoutTabsAndEvaluateMemoryExpressions() throws Exception {
        byte[] memory = {0, 17, 0, 0};
        try (Workspace workspace = workspace(new byte[][] {memory}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                Container watches = workspace.panel.watchesPanel();
                assertNull(findComponent(watches, JTabbedPane.class, null));
                assertNull(findComponent(watches, JList.class, "Memory access watches"));
                JTextField expression = component(watches, JTextField.class, "Expression");
                expression.setText("mem[0x0001]");
                button(watches, "Add").doClick(0);
                flushUiEvents();
                JTable values = component(watches, JTable.class, "Watch expressions and values");
                assertEquals(List.of("mem[0x0001]"), workspace.session.config.watches);
                assertEquals("mem[0x0001] = 17", renderedRowTooltip(values, 0));
                memory[1] = 42;
                button(watches, "Refresh").doClick(0);
                flushUiEvents();
                assertTrue(renderedRowTooltip(values, 0).contains("mem[0x0001] = 42"));
            });
        }
    }

    @Test
    public void recordingTabOwnsCaptureAndAnalysisControls() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                assertEquals(List.of("Debugger", "Inspection", "Recording"),
                        java.util.stream.IntStream.range(0, tabs.getTabCount())
                                .mapToObj(tabs::getTitleAt).collect(java.util.stream.Collectors.toList()));
                int recordingIndex = tabs.indexOfTab("Recording");
                assertNull(tabs.getIconAt(recordingIndex));
                Container recording = (Container) tabs.getComponentAt(recordingIndex);
                assertTrue(SwingUtilities.isDescendingFrom(recordingControl(workspace), recording));
                assertNotNull(component(recording, JTable.class, "Recorded instructions"));
                assertNotNull(component(recording, JTextPane.class, "Recorded instruction state"));
                Container inspection = (Container) tabs.getComponentAt(tabs.indexOfTab("Inspection"));
                assertNull(findComponent(inspection, JTable.class, "Recorded instructions"));
                assertNull(findComponent(workspace.panel, AbstractButton.class, "Analyze recording"));

                selectRecording(workspace);
                assertEquals(recording, tabs.getSelectedComponent());
                assertEquals(0, component(recording, JTable.class, "Recorded instructions").getRowCount());
                assertFalse(button(recording, "Pause recording").isEnabled());
                assertFalse(button(recording, "Stop recording").isEnabled());
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                assertTrue(button(inspection, "Show instructions from PC").isVisible());
            });
        }
    }

    @Test
    public void recordingsHaveStableNumbersAndDefaultNamesAcrossCaptureModes() throws Exception {
        List<InstructionRecording.Event> events = new ArrayList<>();
        for (int i = 0; i < 4; i++) events.add(event(i + 1, i, "NOP", "00", List.of(), true));
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Fold identical repeats").doClick(0);
                JComboBox<?> recordings = component(workspace.panel, JComboBox.class, "Recording");
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                for (InstructionRecording.Mode mode : InstructionRecording.Mode.values()) {
                    workspace.session.startNewRecording(2, mode, 1, 2);
                    events.forEach(event -> capture(workspace.session, event));
                    recordingPanel(workspace).refresh();
                    button(workspace.panel, "Stop recording").doClick(0);
                    flushUiEvents();
                    selectRecording(workspace);
                    assertEquals((mode.ordinal() + 1) + " | Unnamed", recordings.getSelectedItem().toString());
                    assertEquals(mode == InstructionRecording.Mode.UNLIMITED ? 4 : 2, timeline.getRowCount());
                }
                assertEquals("Start recording", recordingControl(workspace).getToolTipText());
                assertTrue(recordingControl(workspace).isEnabled());
            });
        }
    }

    @Test
    public void replacingRunningRecordingKeepsOldCaptureUntilTheDialogSucceeds() throws Exception {
        try (LogCapture logs = new LogCapture(RecordingDialog.class);
             Workspace workspace = workspace(new byte[][] {new byte[4]}, 4,
                     session -> session.startNewRecording(10, InstructionRecording.Mode.FIRST, 0, 0))) {
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> {
                capture(workspace.session, traceEvent(1, "OLD", "00", true));
                SwingUtilities.invokeLater(() -> {
                    RecordingDialog dialog = activeRecordingDialog();
                    component(dialog, JTextField.class, "Instructions").setText("0");
                    button(dialog, "Start recording").doClick(0);
                    assertTrue(workspace.session.recording.isRecording());
                    assertEquals(10, workspace.session.recording.limit());
                    assertEquals(1, workspace.session.recording.events().size());
                    assertTrue(logs.contains("Recording limit must be 1..1000000"));
                    dialog.dispose();
                });
                recordingControl(workspace).doClick(0);
                JComboBox<?> recordings = component(workspace.panel, JComboBox.class, "Recording");
                assertEquals(1, recordings.getItemCount());
                assertTrue(workspace.session.recording.isRecording());

                SwingUtilities.invokeLater(() -> {
                    capture(workspace.session, traceEvent(2, "LAST OLD", "01", true));
                    RecordingDialog dialog = activeRecordingDialog();
                    component(dialog, JTextField.class, "Instructions").setText("5");
                    button(dialog, "Start recording").doClick(0);
                });
                recordingControl(workspace).doClick(0);

                assertEquals(2, recordings.getItemCount());
                assertEquals("2 | Unnamed", recordings.getSelectedItem().toString());
                assertEquals(5, workspace.session.recording.limit());
                assertTrue(workspace.session.recording.events().isEmpty());
                assertEquals(CPU.RunState.STATE_RUNNING, workspace.machine.execution.getRunState());
                recordings.setSelectedIndex(0);
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                assertEquals(2, timeline.getRowCount());
                assertEquals("LAST OLD", timeline.getValueAt(1, 2));
            });
            workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void pauseAndContinueKeepOneRecordingAndStopDisablesBothControls() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4,
                session -> session.startNewRecording(10, InstructionRecording.Mode.LAST, 0, 0))) {
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> {
                capture(workspace.session, traceEvent(1, "FIRST", "00", true));
                AbstractButton pause = button(workspace.panel, "Pause recording");
                Icon pauseIcon = pause.getIcon();
                pause.doClick(0);
                assertTrue(workspace.session.recording.isPaused());
                assertEquals("Continue recording", pause.getToolTipText());
                assertNotEquals(pauseIcon, pause.getIcon());
                capture(workspace.session, traceEvent(2, "SKIP", "01", true));
                pause.doClick(0);
                capture(workspace.session, traceEvent(3, "SECOND", "02", true));
                button(workspace.panel, "Stop recording").doClick(0);

                assertFalse(workspace.session.recording.isRecording());
                assertFalse(pause.isEnabled());
                assertFalse(button(workspace.panel, "Stop recording").isEnabled());
                assertEquals(1, component(workspace.panel, JComboBox.class, "Recording").getItemCount());
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                assertEquals(2, timeline.getRowCount());
                assertEquals("SECOND", timeline.getValueAt(1, 2));
                assertEquals(CPU.RunState.STATE_RUNNING, workspace.machine.execution.getRunState());
            });
            workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
        }
    }

    @Test
    public void recordingRenameAndDeletePreserveOtherCapturesAndNeverReuseNumbers() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                showRecording(workspace, List.of(traceEvent(1, "FIRST", "00", true)));
                showRecording(workspace, List.of(traceEvent(2, "SECOND", "01", true)));
                JComboBox<?> recordings = component(workspace.panel, JComboBox.class, "Recording");
                SwingUtilities.invokeLater(() -> {
                    for (Window window : Window.getWindows()) {
                        if (window instanceof JDialog && window.isShowing()
                                && ((JDialog) window).getTitle().equals("Rename recording")) {
                            JOptionPane input = component((Container) window, JOptionPane.class, null);
                            input.setInputValue("Loop <A&B>");
                            input.setValue(JOptionPane.OK_OPTION);
                            return;
                        }
                    }
                    throw new AssertionError("Missing recording rename dialog");
                });
                button(workspace.panel, "Rename recording").doClick(0);
                assertEquals("2 | Loop <A&B>", recordings.getSelectedItem().toString());
                button(workspace.panel, "Delete recording").doClick(0);
                assertEquals(1, recordings.getItemCount());
                assertEquals("1 | Unnamed", recordings.getSelectedItem().toString());
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                assertEquals("FIRST", timeline.getValueAt(0, 2));
                button(workspace.panel, "Delete recording").doClick(0);
                assertEquals(0, timeline.getRowCount());
                assertFalse(button(workspace.panel, "Rename recording").isEnabled());
                assertFalse(button(workspace.panel, "Delete recording").isEnabled());
                showRecording(workspace, List.of(traceEvent(3, "THIRD", "02", true)));
                assertEquals("3 | Unnamed", recordings.getSelectedItem().toString());
            });
        }
    }

    @Test
    public void recordedStateUsesBoldMonospacedSectionsAndMemoryNavigationFindsTheEarlierWriter() throws Exception {
        ViewRow write = new ViewRow("Memory", "Write @0010", "", "1", true,
                List.of("memory/write/16"), List.of());
        ViewRow read = new ViewRow("Memory", "Read @0010", "", "1", false, List.of(),
                List.of(new NavLink("Find earlier write", "memory/write/16")));
        ViewRow unchanged = new ViewRow("State", "A", "0", "0", false, List.of(), List.of());
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                showRecording(workspace, List.of(
                        event(1, 0, "WRITE", "01", List.of(write), true),
                        event(2, 1, "READ", "02", List.of(unchanged, read), true)));
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                timeline.setRowSelectionInterval(1, 1);
                JTextPane detail = component(workspace.panel, JTextPane.class, "Recorded instruction state");
                JComboBox<?> memory = component(workspace.panel, JComboBox.class, "Memory navigation");
                assertEquals("Read 0x0010", memory.getSelectedItem().toString());
                assertEquals(java.awt.Font.MONOSPACED, detail.getFont().getFamily());
                assertEquals(13, timeline.getFont().getSize());
                assertFalse(detail.getText().contains("A    0"));
                button(workspace.panel, "Show unchanged values").doClick(0);
                assertTrue(detail.getText().contains("A    0"));
                for (String section : List.of("State", "Memory")) {
                    int offset = detail.getText().indexOf("\n" + section + "\n") + 1;
                    assertTrue(offset > 0);
                    assertTrue(javax.swing.text.StyleConstants.isBold(
                            detail.getStyledDocument().getCharacterElement(offset).getAttributes()));
                }
                button(workspace.panel, "Find earlier write").doClick(0);
                assertEquals(0, timeline.getSelectedRow());
                assertEquals(0, memory.getItemCount());
                assertFalse(button(workspace.panel, "Find earlier write").isEnabled());
            });
            verify(workspace.session.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test
    public void toolbarStartsOnlyAfterDialogStartAndEscCancels() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                Icon recordIcon = recordingControl(workspace).getIcon();
                SwingUtilities.invokeLater(() -> {
                    RecordingDialog dialog = activeRecordingDialog();
                    assertFalse(workspace.session.recording.isRecording());
                    assertFalse(dialog.isResizable());
                    component(dialog, JTextField.class, "Instructions").setText("10h");
                    button(dialog, "Start recording").doClick(0);
                    flushUiEvents();
                });
                recordingControl(workspace).doClick(0);
                flushUiEvents();

                assertTrue(workspace.session.recording.isRecording());
                assertTrue(recordingControl(workspace).isEnabled());
                assertEquals(recordIcon, recordingControl(workspace).getIcon());
                assertTrue(button(workspace.panel, "Stop recording").isEnabled());
                capture(workspace.session, traceEvent(1, "NOP", "00", true));
                button(workspace.panel, "Stop recording").doClick(0);
                flushUiEvents();
                SwingUtilities.invokeLater(() -> {
                    RecordingDialog dialog = activeRecordingDialog();
                    JRootPane root = dialog.getRootPane();
                    Object key = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .get(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
                    root.getActionMap().get(key).actionPerformed(
                            new java.awt.event.ActionEvent(root, 0, "escape"));
                });
                recordingControl(workspace).doClick(0);
                flushUiEvents();

                assertFalse(workspace.session.recording.isRecording());
                assertTrue(recordingControl(workspace).isEnabled());
                assertEquals(1, workspace.session.recording.events().size());
            });
        }
    }

    private static RecordingDialog activeRecordingDialog() {
        for (Window window : Window.getWindows())
            if (window instanceof RecordingDialog && window.isShowing()) return (RecordingDialog) window;
        throw new AssertionError("Recording dialog missing");
    }

    @Test
    public void dialogEnablesOnlyRelevantFieldsAndRejectsInvalidInput() throws Exception {
        try (LogCapture logs = new LogCapture(RecordingDialog.class);
             Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                RecordingDialog dialog = new RecordingDialog(null, new EmuStudioGui(),
                        workspace.session, previous -> {});
                try {
                    assertEquals("Start recording", dialog.getTitle());
                    JRadioButton keep = component(dialog, JRadioButton.class, "Keep");
                    JRadioButton range = component(dialog, JRadioButton.class, "Specific address range:");
                    JRadioButton unlimited = component(dialog, JRadioButton.class, "Unlimited");
                    JComboBox<?> order = component(dialog, JComboBox.class, "Instruction order");
                    JTextField count = component(dialog, JTextField.class, "Instructions");
                    JTextField from = component(dialog, JTextField.class, "From");
                    JTextField to = component(dialog, JTextField.class, "To");
                    assertTrue(keep.isSelected());
                    assertEquals("1000", count.getText());
                    assertEquals("first", order.getSelectedItem());
                    assertEquals("0x0000", from.getText());
                    JRadioButton[] choices = {keep, range, unlimited, keep};
                    for (JRadioButton choice : choices) {
                        choice.doClick(0);
                        assertEquals(choice == keep, keep.isSelected());
                        assertEquals(choice == range, range.isSelected());
                        assertEquals(choice == unlimited, unlimited.isSelected());
                        assertEquals(choice == keep, count.isEnabled());
                        assertEquals(choice == keep, order.isEnabled());
                        assertEquals(choice == range, from.isEnabled());
                        assertEquals(choice == range, to.isEnabled());
                    }
                    count.setText("0");
                    button(dialog, "Start recording").doClick(0);
                    flushUiEvents();
                    assertFalse(workspace.session.recording.isRecording());
                    assertTrue(button(dialog, "Start recording").isEnabled());
                    assertTrue(logs.contains("Recording limit must be 1..1000000"));
                    range.doClick(0);
                    from.setText("3");
                    to.setText("1");
                    button(dialog, "Start recording").doClick(0);
                    flushUiEvents();
                    assertFalse(workspace.session.recording.isRecording());
                    from.setText("1h");
                    to.setText("10b");
                    button(dialog, "Start recording").doClick(0);
                    flushUiEvents();

                    assertTrue(workspace.session.recording.isRecording());
                    assertEquals(InstructionRecording.Mode.ADDRESS_RANGE, workspace.session.recording.mode());
                    assertEquals(1, workspace.session.recording.from());
                    assertEquals(2, workspace.session.recording.through());
                } finally {
                    dialog.dispose();
                }
            });
        }
    }

    @Test
    public void recordingFoldsNopsAcrossChangingAddressesAndRegistersAndUnfoldsEveryEvent() throws Exception {
        List<InstructionRecording.Event> events = new ArrayList<>();
        for (int i = 0; i < 3; i++) events.add(event(3617234 + i, 0x2FBA + i, "nop", "00", List.of(
                changed("PC", 0x2FBA + i, 0x2FBB + i), changed("R", 0x5A + i, 0x5B + i)), true));
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                showRecording(workspace, events);
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");

                assertEquals(1, timeline.getRowCount());
                assertEquals("3617234–3617236 (3)", timeline.getValueAt(0, 0));
                assertEquals("2FBA → 2FBC", timeline.getValueAt(0, 1));
                assertEquals("nop × 3", timeline.getValueAt(0, 2));
                assertEquals("PC=2FBA→2FBD R=5A→5D", timeline.getValueAt(0, 3));
                timeline.setRowSelectionInterval(0, 0);
                String detail = component(workspace.panel, JTextPane.class, "Recorded instruction state").getText();
                assertTrue(detail.contains("PC    2FBA → 2FBD"));
                assertTrue(detail.contains("R    5A → 5D"));
                button(workspace.panel, "Fold identical repeats").doClick(0);

                assertEquals(3, timeline.getRowCount());
                assertEquals("2FBB", timeline.getValueAt(1, 1));
                timeline.setRowSelectionInterval(1, 1);
                assertTrue(component(workspace.panel, JTextPane.class, "Recorded instruction state")
                        .getText().contains("Sequence 3617235"));
                assertTrue(workspace.session.recording.events().isEmpty());
            });
        }
    }

    @Test
    public void recordingFoldingKeepsEncodingChangesSequenceGapsAndFailuresSeparate() throws Exception {
        List<InstructionRecording.Event> events = List.of(
                traceEvent(1, "NOP", "00", true), traceEvent(2, "NOP", "00", true),
                traceEvent(3, "NOP", "DD00", true), traceEvent(4, "INC A", "DD00", true),
                traceEvent(6, "INC A", "DD00", true), traceEvent(7, "INC A", "DD00", false),
                traceEvent(8, "INC A", "DD00", false), traceEvent(9, "INC A", "DD00", true));
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                showRecording(workspace, events);
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");

                assertEquals(7, timeline.getRowCount());
                assertEquals("NOP × 2", timeline.getValueAt(0, 2));
                assertEquals("NOP", timeline.getValueAt(1, 2));
                assertEquals("6", timeline.getValueAt(3, 0));
                assertEquals("INC A (failed)", timeline.getValueAt(4, 2));
                assertEquals("INC A (failed)", timeline.getValueAt(5, 2));
            });
        }
    }

    @Test
    public void singletonRecordingPreservesReadAndWriteRowsAtTheSameAddress() throws Exception {
        InstructionRecording.Event increment = event(1, 0, "INC (HL)", "34",
                List.of(access(false, "03", "03"), access(true, "03", "04")), true);
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                showRecording(workspace, List.of(increment));
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                timeline.setRowSelectionInterval(0, 0);
                String detail = component(workspace.panel, JTextPane.class, "Recorded instruction state").getText();

                assertTrue(detail.contains("READ 0003    03"));
                assertTrue(detail.contains("WRITE 0003    03 → 04"));
                assertEquals(2, component(workspace.panel, JComboBox.class, "Memory navigation").getItemCount());
            });
        }
    }

    @Test
    public void foldedRecordingPreservesAllMemoryAccessesAndEarlierWriteSearchWithinRun() throws Exception {
        List<InstructionRecording.Event> events = List.of(
                event(1, 0, "INC (HL)", "34", List.of(changed("A", 0, 1), access(false, "00", "00"), access(true, "00", "01")), true),
                event(2, 1, "INC (HL)", "34", List.of(changed("A", 1, 0), access(false, "01", "01"), access(true, "01", "02")), true));
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                showRecording(workspace, events);
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                timeline.setRowSelectionInterval(0, 0);
                JTextPane detail = component(workspace.panel, JTextPane.class, "Recorded instruction state");
                JComboBox<?> access = component(workspace.panel, JComboBox.class, "Memory navigation");

                assertEquals("A=0→0 WRITE 0003=00→01 WRITE 0003=01→02", timeline.getValueAt(0, 3));
                assertTrue(detail.getText().contains("0 → 0 (changed within run)"));
                assertTrue(detail.getText().contains("#1 · WRITE 0003 = 00 → 01"));
                assertTrue(detail.getText().contains("#2 · WRITE 0003 = 01 → 02"));
                assertEquals(4, access.getItemCount());
                assertTrue(detail.getText().contains("#1 · READ 0003 = 00"));
                assertTrue(detail.getText().contains("#2 · READ 0003 = 01"));
                access.setSelectedIndex(2);
                button(workspace.panel, "Find earlier write").doClick(0);

                assertEquals(2, timeline.getRowCount());
                assertEquals(0, timeline.getSelectedRow());
                assertTrue(detail.getText().contains("Sequence 1 ·"));
            });
            verify(workspace.session.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    private static InstructionRecording.Event traceEvent(long sequence, String mnemonic, String opcode, boolean completed) {
        return event(sequence, 0, mnemonic, opcode, List.of(), completed);
    }

    private static InstructionRecording.Event event(long sequence, int address, String summary, String foldKey,
                                                     List<ViewRow> rows, boolean completed) {
        return new InstructionRecording.Event(sequence, completed,
                new StepView(summary, summary + "/" + foldKey, OptionalInt.of(address), rows));
    }

    private static ViewRow changed(String name, int before, int after) {
        return new ViewRow("Machine state", name, Integer.toHexString(before).toUpperCase(),
                Integer.toHexString(after).toUpperCase(), before != after, List.of(), List.of());
    }

    private static ViewRow access(boolean write, String before, String after) {
        return new ViewRow("Memory accesses", (write ? "WRITE" : "READ") + " 0003", before, after, write,
                write ? List.of("test/write/3") : List.of(),
                List.of(new NavLink("Find earlier write", "test/write/3")));
    }

    private static void capture(DebuggerSession session, InstructionRecording.Event event) {
        session.afterStep(event.completed, Optional.empty(), OptionalInt.empty(), Optional.of(event.view));
    }

    private static void showRecording(Workspace workspace, List<InstructionRecording.Event> events) {
        workspace.panel.addRecording(events);
        selectRecording(workspace);
    }

    private static void selectRecording(Workspace workspace) {
        JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
        tabs.setSelectedIndex(tabs.indexOfTab("Recording"));
    }

    private static JButton recordingControl(Workspace workspace) {
        return component(workspace.panel, JButton.class, "recordInstructions");
    }

    private static RecordingPanel recordingPanel(Workspace workspace) {
        JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
        return (RecordingPanel) tabs.getComponentAt(tabs.indexOfTab("Recording"));
    }

    private static void flushUiEvents() {
        java.awt.SecondaryLoop loop = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        SwingUtilities.invokeLater(loop::exit);
        Timer timeout = new Timer(5000, event -> loop.exit());
        timeout.setRepeats(false);
        timeout.start();
        try { loop.enter(); }
        finally { timeout.stop(); }
    }

    @Test
    public void pcAnalysisFoldsRepeatsAndRevealsIndividualNavigationTargets() throws Exception {
        assertAnalysisFolding("Show instructions from PC");
    }

    @Test
    public void selectionAnalysisFoldsRepeatsAndRevealsIndividualNavigationTargets() throws Exception {
        assertAnalysisFolding("Refresh instruction flow");
    }

    private void assertAnalysisFolding(String origin) throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[9]}, 9, session -> {
            for (int at = 0; at < 8; at++) observed(session, at, OptionalInt.of(at + 1));
            observed(session, DecodedInstruction.of(8, 1, "STOP", "76"), OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, origin).doClick(0);
            });
            assertTrue("Analysis should finish", shown.await(5, TimeUnit.SECONDS));

            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                AbstractButton fold = button(workspace.panel, "Fold identical analysis repeats");
                assertTrue(fold.isSelected());
                int foldedHeight = graph.getPreferredSize().height;
                assertEquals("0001–0007 · NOP × 7", graphTooltip(graph, 40, 130));
                fold.doClick(0);
                int unfoldedHeight = graph.getPreferredSize().height;
                assertTrue(foldedHeight < unfoldedHeight);
                fold.doClick(0);

                assertTrue(graph.getPreferredSize().height < unfoldedHeight);
                assertEquals("0001–0007 · NOP × 7", graphTooltip(graph, 40, 130));
                workspace.panel.navigate(4);
                assertEquals("0004 · NOP", graphTooltip(graph, 40, 220));
                assertEquals(4, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                fold.doClick(0);
                assertEquals("0001 · NOP", graphTooltip(graph, 40, 130));
                assertEquals(unfoldedHeight, graph.getPreferredSize().height);
            });
            verify(workspace.session.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    private static String graphTooltip(JPanel graph, int x, int y) {
        return graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                0, 0, x, y, 0, false));
    }

    @Test
    public void manualInspectionStaysFrozenUntilRefresh() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[6]}, 6, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
            for (int at = 0; at < 4; at++) observed(session, at, OptionalInt.of(at + 1));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                workspace.panel.navigate(0);
                button(workspace.panel, "Refresh instruction flow").doClick(0);
            });
            assertTrue("Analysis should finish", shown.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                        0, 0, 1, 1, 1, false, java.awt.event.MouseEvent.BUTTON1));
                assertFalse(button(workspace.panel, "Go to instruction").isEnabled());
                button(workspace.panel, "Fold identical analysis repeats").doClick(0);
                assertFalse("Relayout must preserve deselection", button(workspace.panel, "Go to instruction").isEnabled());
                button(workspace.panel, "Fold identical analysis repeats").doClick(0);
                pc.set(2);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
                workspace.panel.stepCompleted();
                flushUiEvents();
                flushUiEvents();
                assertFalse("PC following must preserve deselection", button(workspace.panel, "Go to instruction").isEnabled());
            });
            CountDownLatch refreshed = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.setInspectionReadyListener(refreshed::countDown);
                observed(workspace.session, 4, OptionalInt.of(5));
                workspace.panel.stepCompleted();
                flushUiEvents();
                assertFalse(button(workspace.panel, "Go to instruction").isEnabled());
                button(workspace.panel, "Refresh instruction flow").doClick(0);
            });
            assertTrue("Flow refresh should finish", refreshed.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertTrue("Explicit refresh selects its requested root", button(workspace.panel, "Go to instruction").isEnabled());
                workspace.panel.navigate(2);
                assertTrue("Explicit navigation still selects", button(workspace.panel, "Go to instruction").isEnabled());
            });
        }
    }

    @Test
    public void pcFollowToggleDisablesRefreshAndTurningItOffFreezesSelection() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
            observed(session, 0, OptionalInt.of(2));
            observed(session, 2, OptionalInt.of(4));
            observed(session, 4, OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                JToggleButton follow = component(workspace.panel, JToggleButton.class, "Show instructions from PC");
                AbstractButton refresh = button(workspace.panel, "Refresh instruction flow");
                assertFalse(follow.isSelected());
                assertTrue(refresh.isEnabled());
                assertNotNull(follow.getIcon());
                assertNotNull(follow.getDisabledIcon());
                assertNotNull(refresh.getIcon());
                assertNotNull(refresh.getDisabledIcon());
                workspace.panel.setInspectionReadyListener(shown::countDown);
                follow.doClick(0);
                assertTrue(follow.isSelected());
                assertFalse(refresh.isEnabled());
            });
            assertTrue(shown.await(5, TimeUnit.SECONDS));
            CountDownLatch refreshed = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                JToggleButton follow = component(workspace.panel, JToggleButton.class, "Show instructions from PC");
                follow.doClick(0);
                assertFalse(follow.isSelected());
                assertTrue(button(workspace.panel, "Refresh instruction flow").isEnabled());
                String selected = graph.getAccessibleContext().getAccessibleDescription();
                observed(workspace.session, 2, OptionalInt.of(4));
                pc.set(4);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
                workspace.panel.stepCompleted();
                flushUiEvents();
                flushUiEvents();
                assertEquals(selected, graph.getAccessibleContext().getAccessibleDescription());
                workspace.panel.navigate(2);
                workspace.panel.setInspectionReadyListener(refreshed::countDown);
                button(workspace.panel, "Refresh instruction flow").doClick(0);
            });
            assertTrue(refreshed.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertNull(workspace.session.flowAnalysis().instructionAt(0));
                assertNotNull(workspace.session.flowAnalysis().instructionAt(2));
                assertNotNull(workspace.session.flowAnalysis().instructionAt(4));
                assertTrue(button(workspace.panel, "Refresh instruction flow").isEnabled());
            });
        }
    }

    @Test
    public void pcOriginInspectionSelectsCurrentPcAfterStepsAndBackwardJump() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
            observed(session, 0, OptionalInt.of(2));
            observed(session, 2, OptionalInt.of(5));
            observed(session, 5, OptionalInt.of(0));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });
            assertTrue(shown.await(5, TimeUnit.SECONDS));
            for (int[] step : new int[][] {{0, 2}, {2, 5}, {5, 0}}) {
                CountDownLatch refreshed = new CountDownLatch(1);
                SwingUtilities.invokeAndWait(() -> {
                    workspace.panel.setInspectionReadyListener(refreshed::countDown);
                    JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                    graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, 1, 1, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    observed(workspace.session, step[0], OptionalInt.of(step[1]));
                    pc.set(step[1]);
                    workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
                    workspace.panel.stepCompleted();
                });
                assertTrue("Step should refresh Inspection", refreshed.await(5, TimeUnit.SECONDS));
                SwingUtilities.invokeAndWait(() -> {
                    JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                    assertTrue(graph.getAccessibleContext().getAccessibleDescription(),
                            graph.getAccessibleContext().getAccessibleDescription().startsWith(String.format("%04X ·", step[1])));
                    assertEquals(-1, workspace.table.getSelectedRow());
                });
            }
        }
    }

    @Test
    public void pcOriginInspectionFollowsPcWithoutNewObservations() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        try (Workspace workspace = workspace(new byte[][] {new byte[4]}, 4, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
            observed(session, 0, OptionalInt.of(1));
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Show instructions from PC").doClick(0);
                pc.set(1);
                workspace.panel.stepCompleted();
            });
            SwingUtilities.invokeAndWait(() -> {
                FlowGraphPanel graph = component(workspace.panel, FlowGraphPanel.class, "Instruction flow graph");
                assertEquals(1, graph.selectedNode());
                assertTrue(graph.getAccessibleContext().getAccessibleDescription().startsWith("0001 ·"));
                assertNull(workspace.session.flowAnalysis().instructionAt(1));
                assertTrue(workspace.session.flowAnalysis().unknown().containsKey(1));
            });
        }
    }

    @Test
    public void pcOriginInspectionFollowsTimedExecutionWithoutStepCompletedCallback() throws Exception {
        AtomicInteger pc = new AtomicInteger();
        AtomicInteger disabled = new AtomicInteger();
        CountDownLatch shown = new CountDownLatch(1), followed = new CountDownLatch(2);
        java.util.Set<String> selections = java.util.concurrent.ConcurrentHashMap.newKeySet();
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
            observed(session, 0, OptionalInt.of(1));
            when(session.cpu.step()).thenAnswer(call -> {
                int address = pc.getAndIncrement();
                observed(session, address, OptionalInt.of(address + 1));
                CPU.RunState state = address == 1 ? CPU.RunState.STATE_STOPPED_NORMAL : CPU.RunState.STATE_STOPPED_BREAK;
                net.emustudio.application.TestCpu.publish(session.cpu, state);
                return state;
            });
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });
            assertTrue(shown.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> workspace.panel.setInspectionReadyListener(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                String selected = graph.getAccessibleContext().getAccessibleDescription();
                if (selected != null && selections.add(selected.substring(0, 4))) followed.countDown();
            }));
            SwingUtilities.invokeAndWait(() -> {
                for (String name : List.of("Show instructions from PC", "Fold identical analysis repeats")) {
                    button(workspace.panel, name).addPropertyChangeListener("enabled", event -> {
                        if (Boolean.FALSE.equals(event.getNewValue())) disabled.incrementAndGet();
                    });
                }
            });
            workspace.machine.execution.step(500, TimeUnit.MILLISECONDS).get(2, TimeUnit.SECONDS);
            assertTrue("Both timed steps should update graph selection", followed.await(5, TimeUnit.SECONDS));
            assertEquals(java.util.Set.of("0001", "0002"), selections);
            assertNull(workspace.session.flowAnalysis().instructionAt(2));
            assertTrue(workspace.session.flowAnalysis().unknown().containsKey(2));
            assertEquals("Timed steps must keep inspection controls enabled", 0, disabled.get());
            SwingUtilities.invokeAndWait(() -> assertEquals(-1, workspace.table.getSelectedRow()));
        }
    }

    @Test
    public void manualFlowKeepsLabelsAndLayoutWhenPcMoves() throws Exception {
        AtomicInteger pc = new AtomicInteger(4);
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[11]}, 11, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
            for (int at = 0; at < 10; at++) observed(session, at, OptionalInt.of(at + 1));
            observed(session, DecodedInstruction.of(10, 1, "STOP", "76"), OptionalInt.empty());
        })) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(6, "checkpoint", "CODE"));
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(0);
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Refresh instruction flow").doClick(0);
            });
            assertTrue("Analysis should finish", shown.await(5, TimeUnit.SECONDS));

            SwingUtilities.invokeAndWait(() -> {
                assertTrue(button(workspace.panel, "Fold identical analysis repeats").isSelected());
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");

                assertEquals("0001–0003 · NOP × 3", graphTooltip(graph, 150, 130));
                assertEquals("0004 · NOP", graphTooltip(graph, 150, 220));
                assertEquals("checkpoint · 0006 · NOP", graphTooltip(graph, 150, 400));
                pc.set(2);
                workspace.panel.stepCompleted();
            });
            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                assertEquals("0004 · NOP", graphTooltip(graph, 150, 220));
                assertEquals("checkpoint · 0006 · NOP", graphTooltip(graph, 150, 400));
            });
        }
    }

    @Test
    public void recordingTabBrowsesIndependentRecordingsWithoutChangingLiveMachine() throws Exception {
        InstructionRecording.Event first =
                event(1, 0, "INC A", "3C", List.of(changed("A", 1, 2)), true);
        InstructionRecording.Event second =
                event(2, 1, "NOP", "00", List.of(), true);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                selectRecording(workspace);
                assertEquals(0, component(workspace.panel, JTable.class, "Recorded instructions").getRowCount());
                workspace.session.startNewRecording(1024, InstructionRecording.Mode.LAST, 0, 0);
                capture(workspace.session, first);
                recordingPanel(workspace).refresh();
                button(workspace.panel, "Stop recording").doClick(0);
                flushUiEvents();
                JTable timeline = component(workspace.panel, JTable.class, "Recorded instructions");
                timeline.setRowSelectionInterval(0, 0);
                assertTrue(component(workspace.panel, JTextPane.class, "Recorded instruction state")
                        .getText().contains("A    1 → 2"));

                workspace.session.startNewRecording(1024, InstructionRecording.Mode.LAST, 0, 0);
                capture(workspace.session, second);
                recordingPanel(workspace).refresh();
                button(workspace.panel, "Stop recording").doClick(0);
                flushUiEvents();
                JComboBox<?> recordings = component(workspace.panel, JComboBox.class, "Recording");
                assertEquals(2, recordings.getItemCount());
                assertEquals(1, timeline.getRowCount());
                assertEquals("NOP", timeline.getValueAt(0, 2));
                recordings.setSelectedIndex(0);

                assertEquals(1, timeline.getRowCount());
                assertEquals("INC A", timeline.getValueAt(0, 2));
                assertNull(findComponent(workspace.panel, AbstractButton.class, "Open recording…"));
                assertNull(findComponent(workspace.panel, AbstractButton.class, "History filters"));
            });
            verify(workspace.session.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test
    public void instructionPopupTogglesBreakpointAtRightClickedAddress() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.table.setRowSelectionInterval(0, 0);
                int address = workspace.table.instructionModel().getLocationAt(2);
                java.awt.Rectangle cell = workspace.table.getCellRect(2, 0, true);
                java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_PRESSED, 0, 0, cell.x + 1, cell.y + 1,
                        1, false, java.awt.event.MouseEvent.BUTTON3);

                for (java.awt.event.MouseListener listener : workspace.table.getMouseListeners())
                    listener.mousePressed(click);
                JPopupMenu popup = workspace.table.getComponentPopupMenu();
                for (javax.swing.event.PopupMenuListener listener : popup.getPopupMenuListeners())
                    listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));
                JMenuItem toggle = component(popup, JMenuItem.class, "Set breakpoint");
                toggle.doClick(0);
                flushUiEvents();

                assertTrue(workspace.session.isBreakpointSet(address));
                assertFalse(workspace.session.isBreakpointSet(0));
                assertEquals(1, workspace.breakpointChanges.get());
                assertNull(findComponent(popup, JMenuItem.class, "Pin inspection"));
                assertTrue(workspace.panel.isInstructionView());

                for (javax.swing.event.PopupMenuListener listener : popup.getPopupMenuListeners())
                    listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));
                assertEquals("Unset breakpoint", toggle.getText());
                toggle.doClick(0);
                flushUiEvents();

                assertFalse(workspace.session.isBreakpointSet(address));
                assertEquals(2, workspace.breakpointChanges.get());
                for (javax.swing.event.PopupMenuListener listener : popup.getPopupMenuListeners())
                    listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));
                assertEquals("Set breakpoint", toggle.getText());
            });
        }
    }

    @Test
    public void instructionPopupDisablesBreakpointWithoutSelectionOrWhileRunning() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.table.clearSelection();
                JPopupMenu popup = workspace.table.getComponentPopupMenu();

                for (javax.swing.event.PopupMenuListener listener : popup.getPopupMenuListeners())
                    listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));

                JMenuItem toggle = component(popup, JMenuItem.class, "Set breakpoint");
                assertFalse(toggle.isEnabled());

                workspace.table.setRowSelectionInterval(0, 0);
            });
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> {
                JPopupMenu popup = workspace.table.getComponentPopupMenu();
                JMenuItem toggle = component(popup, JMenuItem.class, "Set breakpoint");
                for (javax.swing.event.PopupMenuListener listener : popup.getPopupMenuListeners())
                    listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));

                assertFalse(toggle.isEnabled());
                toggle.doClick(0);
                flushUiEvents();
                assertEquals(0, workspace.breakpointChanges.get());
            });
        }
    }

    @Test
    public void instructionViewUsesInstructionModelDirectlyForCustomColumns() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                DebugTableModel model = workspace.table.instructionModel();
                model.setDebuggerColumns(List.of(net.emustudio.emulib.application.ui.debugger.DebuggerColumn
                        .text("renamed", address -> "custom")));
                assertSame(model, workspace.table.getModel());
                assertEquals("label", workspace.table.getColumnName(0));
                assertEquals("renamed", workspace.table.getColumnName(1));
            });
        }
    }

    @Test
    public void newAnnotationUsesDialogInsteadOfInlineEditor() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
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
                                JLabel type = component((Container) window, JLabel.class, "Type");
                                JComboBox<?> kind = (JComboBox<?>) type.getLabelFor();
                                assertEquals(2, kind.getItemCount());
                                assertEquals("Code", kind.getItemAt(0));
                                assertEquals("Data", kind.getItemAt(1));
                                assertEquals("Code", kind.getSelectedItem());
                            JLabel address = component((Container) window, JLabel.class, "From:");
                            ((JTextField) address.getLabelFor()).setText("0x0001");
                            JLabel end = component((Container) window, JLabel.class, "To:");
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
            assertEquals("entry", workspace.session.config.annotations().get(1).label);
            assertEquals("CODE", workspace.session.config.annotations().get(1).kind);
        }
    }

    @Test
    public void inspectionToolbarShowsAllAnnotationsWhenEmptySearchIsClicked() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[16]}, 16, session -> {})) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 3, 4, "first", "CODE"));
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 8, 9, "second", "DATA"));
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                Container inspection = (Container) tabs.getSelectedComponent();
                JTextField search = component(inspection, JTextField.class, "Search annotations");
                JToolBar toolbar = (JToolBar) SwingUtilities.getAncestorOfClass(JToolBar.class, search);
                assertNotSame(toolbar, SwingUtilities.getAncestorOfClass(JToolBar.class, button(inspection, "Show instructions from PC")));
                assertSame(toolbar, SwingUtilities.getAncestorOfClass(JToolBar.class, button(inspection, "Export annotations")));
                assertNull(findComponent(inspection, JLabel.class, "Search annotations"));
                JFrame frame = new JFrame();
                try {
                    frame.setContentPane(workspace.panel);
                    frame.setSize(900, 600);
                    frame.setVisible(true);
                    search.dispatchEvent(new java.awt.event.MouseEvent(search, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    JPopupMenu popup = search.getComponentPopupMenu();
                    assertTrue(popup.isVisible());
                    JList<?> results = component(popup, JList.class, "Annotations");
                    assertEquals(2, results.getModel().getSize());
                    assertEquals("first", ((MemoryAnnotations.Entry) results.getModel().getElementAt(0)).text);
                    assertEquals("second", ((MemoryAnnotations.Entry) results.getModel().getElementAt(1)).text);
                    results.setSelectedIndex(1);
                    java.awt.Rectangle selected = results.getCellBounds(1, 1);
                    results.dispatchEvent(new java.awt.event.MouseEvent(results, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, selected.x + 2, selected.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertFalse(popup.isVisible());
                    assertTrue(component(workspace.panel, JPanel.class, "Instruction flow graph")
                            .getAccessibleContext().getAccessibleDescription().contains("second"));
                } finally { frame.dispose(); }
            });
        }
    }

    @Test
    public void annotationSearchKeyboardPreviewsThenActivatesTheChosenResult() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[16]}, 16, session -> {})) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 3, 4, "first", "CODE"));
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 8, 9, "second", "DATA"));
            List<Integer> visits = new ArrayList<>();
            JFrame frame = new JFrame();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    frame.setContentPane(workspace.panel);
                    frame.setSize(560, 420);
                    frame.setVisible(true);
                    JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                    tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                    workspace.panel.setAddressSelectionListener((address, visit) -> visits.add(address));
                    JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                    JPopupMenu popup = search.getComponentPopupMenu();
                    JList<?> results = component(popup, JList.class, "Annotations");

                    pressKey(search, "DOWN");
                    pressKey(search, "DOWN");

                    assertTrue(popup.isVisible());
                    assertEquals(1, results.getSelectedIndex());
                    assertTrue(visits.isEmpty());

                    search.postActionEvent();

                    assertFalse(popup.isVisible());
                    assertEquals(List.of(8), visits);
                    assertTrue(component(workspace.panel, JPanel.class, "Instruction flow graph")
                            .getAccessibleContext().getAccessibleDescription().contains("second"));

                    pressKey(search, "UP");
                    assertTrue(popup.isVisible());
                    assertEquals(0, results.getSelectedIndex());
                    pressKey(search, "ESCAPE");
                    assertFalse(popup.isVisible());
                    assertEquals(List.of(8), visits);
                    assertEquals("", search.getText());

                    pressKey(search, "DOWN");
                    search.postActionEvent();
                    pressKey(search, "DOWN");
                    pressKey(search, "UP");
                    search.postActionEvent();
                    assertFalse(popup.isVisible());
                    assertEquals(List.of(8, 8, 3), visits);
                });
            } finally { SwingUtilities.invokeAndWait(frame::dispose); }
        }
    }

    @Test
    public void annotationSearchHandlesEmptyResultsAndClosesWithTheWorkspace() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[16]}, 16, session -> {})) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 3, 4, "first", "CODE"));
            List<Integer> visits = new ArrayList<>();
            JFrame frame = new JFrame();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    frame.setContentPane(workspace.panel);
                    frame.setSize(560, 420);
                    frame.setVisible(true);
                    JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                    tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                    workspace.panel.setAddressSelectionListener((address, visit) -> visits.add(address));
                    JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                    JPopupMenu popup = search.getComponentPopupMenu();

                    search.setText("missing");
                    pressKey(search, "DOWN");
                    pressKey(search, "UP");
                    search.postActionEvent();

                    assertEquals(0, component(popup, JList.class, "Annotations").getModel().getSize());
                    assertTrue(visits.isEmpty());
                    pressKey(search, "ESCAPE");
                    assertEquals("missing", search.getText());

                    search.setText("");
                    pressKey(search, "DOWN");
                    assertTrue(popup.isVisible());
                    workspace.panel.close();

                    assertFalse(popup.isVisible());
                    search.postActionEvent();
                    search.setText("first");
                    search.dispatchEvent(new java.awt.event.MouseEvent(search, java.awt.event.MouseEvent.MOUSE_CLICKED,
                            0, 0, 5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1));
                    assertFalse(popup.isVisible());
                    assertTrue(visits.isEmpty());
                });
            } finally { SwingUtilities.invokeAndWait(frame::dispose); }
        }
    }

    private static void pressKey(JComponent component, String key) {
        Object binding = component.getInputMap().get(KeyStroke.getKeyStroke(key));
        assertNotNull("Missing key binding: " + key, binding);
        Action action = component.getActionMap().get(binding);
        assertNotNull("Missing key action: " + key, action);
        action.actionPerformed(new java.awt.event.ActionEvent(component, java.awt.event.ActionEvent.ACTION_PERFORMED, key));
    }

    @Test
    public void searchingSelectsAnAnnotationOutsideTheAnalyzedFlow() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[16]}, 16, session -> {})) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 12, 14, "buffer", "DATA"));
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                assertEquals(-1, tabs.indexOfTab("Annotations"));
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                Container panel = (Container) tabs.getSelectedComponent();
                AbstractButton edit = button(panel, "Edit selected annotation");
                AbstractButton delete = button(panel, "Delete selected annotation");
                assertFalse(edit.isEnabled());
                assertFalse(delete.isEnabled());
                for (String name : List.of("Annotate inspected address", "Edit selected annotation",
                        "Delete selected annotation", "Import annotations", "Export annotations")) {
                    AbstractButton action = button(panel, name);
                    assertTrue(action.getText() == null || action.getText().isEmpty());
                    assertNotNull(action.getIcon());
                }

                JTextField search = component(panel, JTextField.class, "Search annotations");
                search.setText("buffer");
                DebugTableModel listing = workspace.table.instructionModel();
                List<Integer> previousRows = java.util.stream.IntStream.range(0, listing.getRowCount())
                        .mapToObj(listing::getLocationAt).collect(java.util.stream.Collectors.toList());
                int previousSelection = workspace.table.getSelectedRow();
                search.postActionEvent();
                assertEquals(previousRows, java.util.stream.IntStream.range(0, listing.getRowCount())
                        .mapToObj(listing::getLocationAt).collect(java.util.stream.Collectors.toList()));
                assertEquals(previousSelection, workspace.table.getSelectedRow());
                assertEquals("Inspection", tabs.getTitleAt(tabs.getSelectedIndex()));

                JPanel graph = component(panel, JPanel.class, "Instruction flow graph");
                assertTrue(graph.getAccessibleContext().getAccessibleDescription().contains("buffer"));
                assertTrue(edit.isEnabled());
                assertTrue(delete.isEnabled());
                assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                        0, 0, 10, 40, 0, false)).contains("buffer"));
                doubleClickLaneAndCancel(graph, 10, 40, "buffer");
                graph.dispatchEvent(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_CLICKED,
                        0, 0, 300, 300, 1, false, java.awt.event.MouseEvent.BUTTON1));
                assertFalse(edit.isEnabled());
                assertFalse(delete.isEnabled());
            });
        }
    }

    @Test
    public void annotationsCanBeCreatedAndEditedWhileCpuRuns() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8]}, 8, session -> {})) {
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                assertTrue(button(workspace.panel, "Annotate inspected address").isEnabled());
                editAnnotationDialog(workspace.panel, "New annotation", "Annotate inspected address", dialog -> {
                    annotationField(dialog, "From:").setText("0x0002");
                    annotationField(dialog, "To:").setText("0x0003");
                    annotationField(dialog, "Label").setText("live");
                });
            });
            SwingUtilities.invokeAndWait(() -> {
                JTextField search = component(workspace.panel, JTextField.class, "Search annotations");
                assertTrue(search.isEnabled());
                JList<?> results = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                assertEquals(1, results.getModel().getSize());
                results.setSelectedIndex(0);
                assertTrue(button(workspace.panel, "Edit selected annotation").isEnabled());
                assertTrue(button(workspace.panel, "Delete selected annotation").isEnabled());
                editAnnotationDialog(workspace.panel, "Edit annotation", "Edit selected annotation", dialog ->
                        annotationField(dialog, "Label").setText("updated live"));
            });
            assertEquals("updated live", workspace.session.config.annotations().get(2).label);
            assertEquals(4, workspace.session.config.annotations().get(2).end);
            assertEquals(CPU.RunState.STATE_RUNNING, workspace.machine.execution.getRunState());
        }
    }

    @Test
    public void annotationToolbarExportsAndImportsUserAnnotations() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("annotations.properties");
        try (Workspace workspace = workspace(new byte[][] {new byte[16]}, 16, session -> {})) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 3, 5, "buffer", "DATA"));
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> chooseAnnotationFile(workspace.panel, "Export annotations", file));
            assertTrue(java.nio.file.Files.isRegularFile(file));
            workspace.session.config.annotations().replace(List.of());

            SwingUtilities.invokeAndWait(() -> chooseAnnotationFile(workspace.panel, "Import annotations", file));
            SwingUtilities.invokeAndWait(() -> chooseAnnotationFile(workspace.panel, "Import annotations", file));

            assertEquals("buffer", workspace.session.config.annotations().get(3).label);
            assertEquals(1, workspace.session.config.annotations().entries().size());
        }
    }

    private static void chooseAnnotationFile(Container panel, String action, Path file) {
        AtomicBoolean chosen = new AtomicBoolean();
        Timer timer = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog) || !window.isShowing()) continue;
                JFileChooser chooser = findComponent((Container) window, JFileChooser.class, null);
                if (chooser == null) continue;
                ((Timer) event.getSource()).stop();
                chooser.setSelectedFile(file.toFile());
                chosen.set(true);
                chooser.approveSelection();
            }
        });
        timer.start();
        try { button(panel, action).doClick(0); }
        finally { timer.stop(); }
        assertTrue("Expected file chooser", chosen.get());
    }

    @Test
    public void annotationNavigationReplacesUnsavedDraftAndEscapeClosesEditor() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {})) {
            workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 1, 2, "saved", "CODE"));
            List<Integer> visits = new ArrayList<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.setAddressSelectionListener((address, visit) -> visits.add(address));
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                Container annotations = (Container) tabs.getSelectedComponent();
                Timer timer = new Timer(100, event -> {
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (!(window instanceof JDialog) || !window.isShowing()
                                || !"New annotation".equals(((JDialog) window).getTitle())) continue;
                        ((Timer) event.getSource()).stop();
                        try {
                            JLabel label = component((Container) window, JLabel.class, "Label");
                            ((JTextField) label.getLabelFor()).setText("discarded");
                            JTextField search = component(annotations, JTextField.class, "Search annotations");
                            search.setText("saved");
                            JList<?> results = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                            results.setSelectedIndex(0);
                            search.postActionEvent();

                            assertEquals(0, results.getSelectedIndex());
                            assertTrue(button(annotations, "Edit selected annotation").isEnabled());
                            assertTrue(button(annotations, "Delete selected annotation").isEnabled());
                            assertEquals(List.of(1), visits);
                            assertEquals("saved", ((JTextField) label.getLabelFor()).getText());

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
            });
            if (failure.get() != null) throw new AssertionError(failure.get());
            assertNull(workspace.session.config.annotations().get(0));
        }
    }

    @Test
    public void annotationEditorCreatesAndEditsOverlappingRangesIndependently() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {new byte[8], new byte[8]}, 8, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                Container panel = (Container) tabs.getSelectedComponent();
                editAnnotationDialog(panel, "New annotation", "Annotate inspected address", dialog -> {
                    annotationField(dialog, "From:").setText("0x0001");
                    annotationField(dialog, "To:").setText("0x0004");
                    annotationField(dialog, "Label").setText("first");
                });
                editAnnotationDialog(panel, "New annotation", "Annotate inspected address", dialog -> {
                    annotationField(dialog, "From:").setText("0x0002");
                    annotationField(dialog, "To:").setText("0x0005");
                    annotationField(dialog, "Label").setText("second");
                });
                JTextField search = component(panel, JTextField.class, "Search annotations");
                @SuppressWarnings("unchecked") JList<MemoryAnnotations.Entry> list = component(search.getComponentPopupMenu(), JList.class, "Annotations");
                assertEquals(2, list.getModel().getSize());
                int firstRow = list.getModel().getElementAt(0).text.equals("first") ? 0 : 1;
                list.setSelectedIndex(firstRow);
                editAnnotationDialog(panel, "Edit annotation", "Edit selected annotation", dialog -> {
                    annotationField(dialog, "To:").setText("0x0006");
                    annotationField(dialog, "Label").setText("updated");
                });
            });

            List<UserAnnotations.Entry> entries = workspace.session.config.annotations().entries();
            assertEquals(2, entries.size());
            UserAnnotations.Entry updated = entries.stream().filter(entry -> entry.label.equals("updated")).findFirst().orElseThrow();
            UserAnnotations.Entry second = entries.stream().filter(entry -> entry.label.equals("second")).findFirst().orElseThrow();
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
                assertEquals(-1, tabs.indexOfTab("Annotations"));
            });
        }
    }

    @Test
    public void rapidExecutionSnapshotsKeepThePublishedWatchRowsStable() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {
            session.config.watches.add("A");
            session.refreshWatches().join();
        })) {
            AtomicInteger changes = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                JTable values = component(workspace.panel.watchesPanel(), JTable.class, "Watch expressions and values");
                values.getModel().addTableModelListener(event -> changes.incrementAndGet());
                for (int i = 0; i < 100; i++) workspace.machine.execution.step().join();
            });
            SwingUtilities.invokeAndWait(() -> {});
            SwingUtilities.invokeAndWait(() -> {
                JTable values = component(workspace.panel.watchesPanel(), JTable.class, "Watch expressions and values");
                assertEquals(1, values.getRowCount());
                assertEquals("A = 0", renderedRowTooltip(values, 0));
                assertEquals(0, changes.get());
            });
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
    public void executionDisablesTableWithoutHidingInstructionListing() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            Icon[] marker = new Icon[1];
            SwingUtilities.invokeAndWait(() -> {
                DebugTableModel model = workspace.table.instructionModel();
                int pcRow = java.util.stream.IntStream.range(0, model.getRowCount())
                        .filter(model::isRowAtCurrentInstruction).findFirst().orElseThrow();
                int addressColumn = workspace.table.getColumnModel().getColumnIndex("address");
                JLabel paused = (JLabel) workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(pcRow, addressColumn), pcRow, addressColumn);
                marker[0] = paused.getIcon();
                java.awt.Color currentBackground = new java.awt.Color(255, 179, 179);
                assertEquals(currentBackground, paused.getBackground());
                assertEquals("Current instruction (PC)", paused.getToolTipText());
            });
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> {
                DebugTableModel model = workspace.table.instructionModel();
                int pcRow = java.util.stream.IntStream.range(0, model.getRowCount())
                        .filter(row -> model.getLocationAt(row) == 0).findFirst().orElseThrow();
                int addressColumn = workspace.table.getColumnModel().getColumnIndex("address");
                java.awt.Color currentBackground = new java.awt.Color(255, 179, 179);
                assertFalse(model.isRowAtCurrentInstruction(pcRow));
                JLabel running = (JLabel) workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(pcRow, addressColumn), pcRow, addressColumn);
                assertNotSame(marker[0], running.getIcon());
                assertNotEquals(currentBackground, running.getBackground());
                assertNull(running.getToolTipText());
                assertFalse(workspace.table.isEnabled());
                assertTrue(workspace.table.isVisible());
                for (String name : List.of("Show instructions from PC", "Refresh instruction flow",
                        "Go to instruction", "Edit selected annotation", "Delete selected annotation"))
                    assertFalse(name, button(workspace.panel, name).isEnabled());
                for (String name : List.of("Annotate inspected address", "Import annotations", "Export annotations"))
                    assertTrue(name, button(workspace.panel, name).isEnabled());
                assertTrue(component(workspace.panel, JTextField.class, "Search annotations").isEnabled());
                assertTrue(component(workspace.panel, JComboBox.class, "Graph zoom").isEnabled());
            });
            workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            SwingUtilities.invokeAndWait(() -> {
                DebugTableModel model = workspace.table.instructionModel();
                int pcRow = java.util.stream.IntStream.range(0, model.getRowCount())
                        .filter(row -> model.getLocationAt(row) == 0).findFirst().orElseThrow();
                int addressColumn = workspace.table.getColumnModel().getColumnIndex("address");
                java.awt.Color currentBackground = new java.awt.Color(255, 179, 179);
                assertTrue(model.isRowAtCurrentInstruction(pcRow));
                JLabel resumed = (JLabel) workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(pcRow, addressColumn), pcRow, addressColumn);
                assertSame(marker[0], resumed.getIcon());
                assertEquals(currentBackground, resumed.getBackground());
                assertEquals("Current instruction (PC)", resumed.getToolTipText());
                workspace.panel.showInstructions();
                assertTrue(workspace.table.isVisible());
                assertTrue(workspace.table.isEnabled());
                assertTrue(button(workspace.panel, "Show instructions from PC").isEnabled());
                assertTrue(button(workspace.panel, "Refresh instruction flow").isEnabled());
                assertFalse(button(workspace.panel, "Edit selected annotation").isEnabled());
                assertFalse(button(workspace.panel, "Delete selected annotation").isEnabled());
                assertFalse(button(workspace.panel, "Go to instruction").isEnabled());
            });
        }
    }

    @Test
    public void debuggerTabsExcludeBreakpointManagerAndRetainExistingBreakpoints() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3, session -> {
            session.config.breakpoints.add(1);
        })) {
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                assertEquals(List.of("Debugger", "Inspection", "Recording"),
                        java.util.stream.IntStream.range(0, tabs.getTabCount())
                                .mapToObj(tabs::getTitleAt).collect(java.util.stream.Collectors.toList()));
                assertNull(findComponent(workspace.panel, JList.class, "Breakpoints"));
                assertEquals(List.of(1), List.copyOf(workspace.session.config.breakpoints));
            });
        }
    }

    @Test
    public void executionControlsUseCpuVisibleAddresses() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0}, {0, 0}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 0)));
                flushUiEvents();
                assertTrue(workspace.session.isBreakpointSet(0));

            });
        }
    }

    @Test
    public void instructionRowsShowLabelFirstAndBreakpointsAsBoldText() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {
            try {
                session.config.annotations().add(new UserAnnotations.Entry(0, "start", "CODE"));
            } catch (Exception e) { throw new RuntimeException(e); }
        })) {
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(workspace.table.getColumnCount(), workspace.table.getModel().getColumnCount());
                assertEquals(List.of("label", "address", "instruction", "opcode"),
                        java.util.stream.IntStream.range(0, workspace.table.getColumnCount())
                                .mapToObj(workspace.table::getColumnName).collect(java.util.stream.Collectors.toList()));
                assertEquals("", workspace.table.getValueAt(0, 0));
                for (int col = 0; col < workspace.table.getColumnCount(); col++) {
                    String name = workspace.table.getColumnName(col);
                    assertFalse(List.of("bp", "Label", "Comment").contains(name));
                    assertTrue(workspace.table.getColumnModel().getColumn(col).getMinWidth()
                            < workspace.table.getColumnModel().getColumn(col).getPreferredWidth());
                }
                assertEquals(JTable.AUTO_RESIZE_OFF, workspace.table.getAutoResizeMode());
                java.awt.Rectangle cell = workspace.table.getCellRect(1, 0, true);
                java.awt.event.MouseEvent hover = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, cell.x + 1, cell.y + 1, 0, false);
                assertEquals("start", workspace.table.getToolTipText(hover));
                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 0)));
                flushUiEvents();
                JComponent rendered = (JComponent) workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(1, 0), 1, 0);
                assertTrue(rendered.getFont().isBold());
            });
        }
    }

    @Test
    public void instructionRowsAndTooltipsCoverAnnotationRangeAndUseLaneColors() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3, session -> {
            try {
                session.config.annotations().add(new UserAnnotations.Entry(-1, 0, 2, "range", "CODE"));
                session.config.annotations().add(new UserAnnotations.Entry(-1, 1, 2, "inner", "CODE"));
            } catch (Exception e) { throw new RuntimeException(e); }
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.table.instructionModel().setMaxRows(7);
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                java.awt.Rectangle first = workspace.table.getCellRect(3, 0, true);
                java.awt.event.MouseEvent hoverFirst = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, first.x + 1, first.y + 1, 0, false);
                assertEquals("range", workspace.table.getToolTipText(hoverFirst));
                java.awt.Color lane = java.awt.Color.getHSBColor(Math.floorMod((0 * 31 + 2) * 0x9E3779B9, 360) / 360f, .55f, .78f);
                java.awt.Color base = graph.getBackground();
                java.awt.Color expected = new java.awt.Color((lane.getRed() * 28 + base.getRed() * 227 + 127) / 255,
                        (lane.getGreen() * 28 + base.getGreen() * 227 + 127) / 255,
                        (lane.getBlue() * 28 + base.getBlue() * 227 + 127) / 255);
                assertEquals(expected, workspace.table.createToolTip().getBackground());

                java.awt.Rectangle second = workspace.table.getCellRect(4, 0, true);
                java.awt.event.MouseEvent hoverSecond = new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, second.x + 1, second.y + 1, 0, false);
                assertEquals("range | inner", workspace.table.getToolTipText(hoverSecond));
                assertNotEquals(expected, workspace.table.createToolTip().getBackground());
                java.awt.Color overlap = workspace.table.createToolTip().getBackground();
                workspace.table.clearSelection();
                for (int col = 0; col < workspace.table.getColumnCount(); col++) {
                    assertEquals(overlap, workspace.table.prepareRenderer(
                            workspace.table.getCellRenderer(4, col), 4, col).getBackground());
                    assertEquals(workspace.table.getBackground(), workspace.table.prepareRenderer(
                            workspace.table.getCellRenderer(5, col), 5, col).getBackground());
                }

                workspace.table.setRowSelectionInterval(4, 4);
                assertEquals(workspace.table.getSelectionBackground(), workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(4, 0), 4, 0).getBackground());
                assertEquals(new java.awt.Color(255, 179, 179), workspace.table.prepareRenderer(
                        workspace.table.getCellRenderer(3, 0), 3, 0).getBackground());

                java.awt.Rectangle third = workspace.table.getCellRect(5, 0, true);
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
                flushUiEvents();
                assertTrue(workspace.session.isBreakpointSet(1));
                assertTrue(workspace.panel.isInstructionView());
                assertEquals(1, workspace.breakpointChanges.get());

                assertTrue(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.UNSET, 1)));
                flushUiEvents();
                assertFalse(workspace.session.isBreakpointSet(1));
                assertEquals(2, workspace.breakpointChanges.get());
            });
        }
    }

    @Test
    public void invalidBreakpointRequestLogsErrorWithoutChangingRules() throws Exception {
        try (LogCapture logs = new LogCapture(DebuggerPanel.class);
             Workspace workspace = workspace(new byte[][] {{1, 2}}, 2, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(workspace.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 2)));
                assertTrue(workspace.session.config.breakpoints.isEmpty());
                assertEquals(0, workspace.breakpointChanges.get());
            });
            assertTrue(logs.contains("Address outside CPU"));
        }
    }

    @Test
    public void closingPanelDetachesItsListenerWhileSessionRemainsUsable() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0}}, 1, session -> {})) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.close();
                workspace.panel.close();
            });
            clearInvocations(workspace.machine.debug);
            workspace.machine.execution.step().join();
            SwingUtilities.invokeAndWait(() -> {});
            verify(workspace.machine.debug, never()).compile(anyString());
            workspace.session.requirePaused();
        }
    }

    @Test
    public void breakpointHitsFollowTheNextPc() throws Exception {
        AtomicInteger pc = new AtomicInteger(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0, 0}}, 4,
                session -> {
                    when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenAnswer(call -> pc.get());
                    session.setBreakpoint(1, true).join();
                })) {
            workspace.machine.state(CPU.RunState.STATE_RUNNING);
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.session.beforeStep());
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(workspace.panel.isInstructionView());
                assertEquals(1, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
            });

            SwingUtilities.invokeAndWait(() -> {
                pc.set(2);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(2, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
            });

            SwingUtilities.invokeAndWait(() -> {
                pc.set(3);
                workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            });
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(3, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
            });
            SwingUtilities.invokeAndWait(() -> workspace.machine.state(CPU.RunState.STATE_STOPPED_BREAK));
            SwingUtilities.invokeAndWait(() -> assertEquals(3, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow())));
        }
    }

    @Test
    public void analyzeReferencesStartsAtPcInsteadOfInspectedAddress() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0, 0, 0}}, 5, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(3);
            observed(session, 3, OptionalInt.of(1));
            observed(session, 1, OptionalInt.of(4));
            observed(session, 3, OptionalInt.of(4));
            observed(session, 4, OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(0);
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });

            assertTrue("Flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            FlowAnalysis snapshot = workspace.session.flowAnalysis();
            assertNull(snapshot.instructionAt(0));
            assertNotNull(snapshot.instructionAt(1));
            assertNull(snapshot.instructionAt(2));
            assertNotNull(snapshot.instructionAt(3));
            assertNotNull(snapshot.instructionAt(4));
            SwingUtilities.invokeAndWait(() -> {
                JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                        0, 0, 40, 40, 0, false)).contains("0001"));
                assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                        0, 0, 40, 130, 0, false)).contains("0003"));
                assertTrue(graph.getToolTipText(new java.awt.event.MouseEvent(graph, java.awt.event.MouseEvent.MOUSE_MOVED,
                        0, 0, 40, 220, 0, false)).contains("0004"));
                button(workspace.panel, "Go to instruction").doClick(0);
                assertEquals(3, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
            });
        }
    }

    @Test
    public void observedDestinationDoesNotInventAnExecutedInstruction() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2,
                session -> observed(session, 0, OptionalInt.of(1)))) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });
            assertTrue("Observed flow should be displayed", shown.await(5, TimeUnit.SECONDS));

            assertNotNull(workspace.session.flowAnalysis().instructionAt(0));
            assertNull(workspace.session.flowAnalysis().instructionAt(1));
            assertTrue(workspace.session.flowAnalysis().unknown().containsKey(1));
        }
    }

    @Test
    public void selectingGraphInstructionUpdatesDebuggerAndAnalysisStart() throws Exception {
        CountDownLatch shown = new CountDownLatch(1), reanalyzed = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {
            observed(session, 0, OptionalInt.of(1));
            observed(session, 1, OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) {
                        shown.countDown();
                    }
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
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
                button(workspace.panel, "Show instructions from PC").doClick(0);
                workspace.panel.setInspectionReadyListener(reanalyzed::countDown);
                button(workspace.panel, "Refresh instruction flow").doClick(0);
            });
            assertTrue("Analysis should start at the selected graph instruction", reanalyzed.await(5, TimeUnit.SECONDS));
            assertNull(workspace.session.flowAnalysis().instructionAt(0));
            assertNotNull(workspace.session.flowAnalysis().instructionAt(1));
        }
    }

    @Test
    public void steppingRefreshesObservedBranchesWithoutAdvancingExplicitRoot() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[256]}, 256, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(100);
            observed(session, DecodedInstruction.of(100, 3, "BRANCH", "00 00 00"), OptionalInt.of(103));
            observed(session, DecodedInstruction.of(103, 1, "A", "01"), OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });
            assertTrue(shown.await(5, TimeUnit.SECONDS));
            observed(workspace.session, DecodedInstruction.of(100, 3, "BRANCH", "00 00 00"), OptionalInt.of(200));
            observed(workspace.session, DecodedInstruction.of(200, 1, "B", "02"), OptionalInt.empty());
            workspace.session.config.annotations().add(new UserAnnotations.Entry(100, "entry", "CODE"));
            workspace.machine.memoryChanged(100);
            when(net.emustudio.application.TestCpu.context(workspace.session.cpu).getInstructionLocation()).thenReturn(200);
            SwingUtilities.invokeAndWait(workspace.panel::stepCompleted);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while ((workspace.session.flowAnalysis() == null
                    || workspace.session.flowAnalysis().instructionAt(103) == null
                    || workspace.session.flowAnalysis().instructionAt(200) == null)
                    && System.nanoTime() < deadline) Thread.sleep(10);

            FlowAnalysis refreshed = workspace.session.flowAnalysis();
            assertNotNull(refreshed);
            assertNotNull(refreshed.instructionAt(100));
            assertNotNull(refreshed.instructionAt(103));
            assertNotNull(refreshed.instructionAt(200));
            java.util.Set<String> edges = new java.util.HashSet<>();
            refreshed.forEachEdge((from, to) -> edges.add(from + "->" + to));
            assertEquals(java.util.Set.of("100->103", "100->200"), edges);
        }
    }

    @Test
    public void inspectionCanFilterObservedFlowFromSelectedInstructionInsteadOfPc() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0, 0}}, 4, session -> {
            when(net.emustudio.application.TestCpu.context(session.cpu).getInstructionLocation()).thenReturn(3);
            observed(session, 1, OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(1);
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                button(workspace.panel, "Refresh instruction flow").doClick(0);
            });
            assertTrue("Selected observed instruction should be displayed", shown.await(5, TimeUnit.SECONDS));
            assertNotNull(workspace.session.flowAnalysis().instructionAt(1));
            assertNull(workspace.session.flowAnalysis().instructionAt(3));
            SwingUtilities.invokeAndWait(workspace.panel::stepCompleted);
            assertNotNull(workspace.session.flowAnalysis().instructionAt(1));
        }
    }

    @Test
    public void inspectPopupAnalyzesRightClickedInstruction() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {new byte[16]}, 16, session -> {
            observed(session, 5, OptionalInt.empty());
        })) {
            SwingUtilities.invokeAndWait(() -> {
                workspace.table.getSelectionModel().addListSelectionListener(event -> {
                    int selected = workspace.table.getSelectedRow();
                    if (!event.getValueIsAdjusting() && selected >= 0 && workspace.panel.isInstructionView())
                        workspace.panel.followAddress(workspace.table.instructionModel().getLocationAt(selected));
                });
                workspace.panel.navigate(4);
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                workspace.table.setSize(workspace.table.getPreferredSize());
                java.awt.Rectangle row = workspace.table.getCellRect(2, 1, true);
                assertEquals(5, workspace.table.instructionModel().getLocationAt(2));
                workspace.table.dispatchEvent(new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_PRESSED, 0, java.awt.event.InputEvent.BUTTON3_DOWN_MASK,
                        row.x + 2, row.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON3));
                workspace.table.dispatchEvent(new java.awt.event.MouseEvent(workspace.table,
                        java.awt.event.MouseEvent.MOUSE_RELEASED, 1, 0,
                        row.x + 2, row.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON3));
                assertEquals(5, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
                ((JMenuItem) workspace.table.getComponentPopupMenu().getComponent(0)).doClick(0);
            });

            assertTrue("Flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            assertNotNull(workspace.session.flowAnalysis().instructionAt(5));
            assertNull(workspace.session.flowAnalysis().instructionAt(4));
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = component(workspace.panel, JTabbedPane.class, "debuggerViews");
                assertEquals("Inspection", tabs.getTitleAt(tabs.getSelectedIndex()));
                assertEquals(5, workspace.table.instructionModel().getLocationAt(workspace.table.getSelectedRow()));
            });
            verify(workspace.session.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test
    public void goToSourcePopupUsesSelectedInstruction() throws Exception {
        try (Workspace workspace = workspace(new byte[][] {{0, 0, 0}}, 3, session -> {})) {
            net.emustudio.emulib.plugin.compiler.SourceCodePosition position =
                    mock(net.emustudio.emulib.plugin.compiler.SourceCodePosition.class);
            workspace.session.annotationEngine.put(new net.emustudio.emulib.application.annotations.MemoryAnnotations.Entry(
                    1, 0, 1, 2, "source", "", position));

            SwingUtilities.invokeAndWait(() -> {
                workspace.panel.navigate(1);
                ((JMenuItem) workspace.table.getComponentPopupMenu().getComponent(1)).doClick(0);
            });

            assertSame(position, workspace.sourcePosition.get());
        }
    }

    @Test
    public void reachableObservedFlowGraphNavigatesOnDoubleClick() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}}, 2, session -> {
            observed(session, 0, OptionalInt.of(0));
            observed(session, 0, OptionalInt.of(1));
            observed(session, 1, OptionalInt.empty());
        })) {
                workspace.session.config.annotations().add(new UserAnnotations.Entry(0, "start", "CODE"));
                workspace.session.config.annotations().add(new UserAnnotations.Entry(-1, 0, 2, "region", "CODE"));
                SwingUtilities.invokeAndWait(() -> {
                    AbstractButton goTo = button(workspace.panel, "Go to instruction");
                    assertFalse(goTo.isEnabled());
                    goTo.addPropertyChangeListener("enabled", event -> {
                        if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                    });
                    button(workspace.panel, "Show instructions from PC").doClick(0);
                });
                assertTrue("Observed instructions should reach UI", shown.await(5, TimeUnit.SECONDS));
                UserAnnotations annotationIndex = workspace.session.config.annotations();
                SwingUtilities.invokeAndWait(() -> {
                    assertNull(findComponent(workspace.panel, JList.class, "Static references"));
                    JPanel graph = component(workspace.panel, JPanel.class, "Instruction flow graph");
                    JScrollPane graphScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, graph);
                    assertEquals(30, graphScroll.getVerticalScrollBar().getUnitIncrement());
                    AtomicInteger symbolReads = new AtomicInteger();
                    workspace.panel.setSymbols(() -> { symbolReads.incrementAndGet(); return List.of(); });
                    workspace.panel.setSourceMappingsCurrent(true);
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
                        assertEquals("Selection changes should keep cached labels", 2, symbolReads.get());
                    } finally { graphics.dispose(); }
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
                    for (String name : List.of("Show instructions from PC", "Refresh instruction flow", "Go to instruction")) {
                        AbstractButton action = button(workspace.panel, name);
                        assertNotNull(action.getIcon());
                    }
                    for (String name : List.of("Show instructions from PC", "Refresh instruction flow"))
                        assertTrue(button(workspace.panel, name).getHideActionText());
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
                        assertEquals("0x0000", annotationField(dialog, "From:").getText());
                        assertEquals("0x0001", annotationField(dialog, "To:").getText());
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
                            entry.label.isEmpty() && entry.address == 0 && entry.end == 2 && entry.kind.equals("CODE")));
                });
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
    public void observedInstructionCanNavigateBackToLiveListing() throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        try (Workspace workspace = workspace(new byte[][] {{0, 0}, {0, 0}}, 2,
                session -> observed(session, 0, OptionalInt.empty()))) {
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) shown.countDown();
                });
                workspace.panel.navigate(0);
                button(workspace.panel, "Show instructions from PC").doClick(0);
            });
            assertTrue("Observed flow should be displayed", shown.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                button(workspace.panel, "Go to instruction").doClick(0);
                assertTrue(workspace.panel.isInstructionView());
            });
        }
    }


    private Workspace workspace(byte[][] banks, int common, Configuration configure) throws Exception {
        Path settings = temporary.newFolder().toPath().resolve("debug.properties");
        Workspace workspace = new Workspace();
        workspace.machine = new DebuggerTestMachine(banks, common);
        Decoder decoder = mock(Decoder.class);
        when(decoder.decode(anyInt())).thenAnswer(call ->
                instruction(call.getArgument(0)));
        when(workspace.machine.cpu.getDecoder()).thenReturn(decoder);
        workspace.session = workspace.machine.session(settings);
        configure.accept(workspace.session);
        CountDownLatch ready = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                DebugTableModel model = new DebugTableModel();
                model.setCPU(workspace.machine.cpu, net.emustudio.application.TestCpu.context(workspace.machine.cpu));
                model.setMaxRows(3);
                workspace.table = new DebugTableImpl(model);
                JScrollPane scroll = new JScrollPane(workspace.table);
                JPanel instructions = new JPanel(new BorderLayout());
                instructions.add(scroll);
                workspace.panel = new DebuggerPanel(workspace.session, workspace.machine.execution, workspace.table, instructions,
                        workspace.sourcePosition::set, workspace.breakpointChanges::incrementAndGet,
                        new EmuStudioGui());
                workspace.panel.setInspectionReadyListener(ready::countDown);
            });
            assertTrue("Initial annotation loading should finish", ready.await(5, TimeUnit.SECONDS));
            return workspace;
        } catch (Exception | AssertionError failure) {
            workspace.close();
            throw failure;
        }
    }

    @FunctionalInterface
    private interface Configuration {
        void accept(DebuggerSession session) throws Exception;
    }

    private static DecodedInstruction instruction(int address) {
        return DecodedInstruction.of(address, 1, "NOP", "00");
    }

    private static void observed(DebuggerSession session, int address, OptionalInt next) {
        observed(session, instruction(address), next);
    }

    private static void observed(DebuggerSession session, DecodedInstruction instruction, OptionalInt next) {
        session.afterStep(true, Optional.of(instruction), next, Optional.empty());
    }

    private static AbstractButton button(Container root, String name) {
        return component(root, AbstractButton.class, name);
    }

    private static <T> String renderedRowTooltip(JList<T> list, int row) {
        return ((JComponent) list.getCellRenderer().getListCellRendererComponent(
                list, list.getModel().getElementAt(row), row, false, false)).getToolTipText();
    }

    private static String renderedRowTooltip(JTable table, int row) {
        return ((JComponent) table.prepareRenderer(table.getCellRenderer(row, 0), row, 0)).getToolTipText();
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
        DebuggerSession session;
        DebuggerPanel panel;
        DebugTableImpl table;
        final AtomicInteger breakpointChanges = new AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<net.emustudio.emulib.plugin.compiler.SourceCodePosition> sourcePosition = new java.util.concurrent.atomic.AtomicReference<>();

        @Override public void close() throws Exception {
            if (panel != null) SwingUtilities.invokeAndWait(panel::close);
            if (session != null) { machine.execution.close(); session.close(); }
        }
    }
}
