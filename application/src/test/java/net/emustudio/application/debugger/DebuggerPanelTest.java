/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import java.awt.BorderLayout;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import org.mockito.ArgumentCaptor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

public class DebuggerPanelTest extends AbstractSwingTest {
    @org.junit.Rule public org.junit.rules.TemporaryFolder temporary = new org.junit.rules.TemporaryFolder();
    @Test(timeout = 10000)
    @SuppressWarnings("unchecked")
    public void initialCaptureLeavesEdtResponsiveAndRestoresDisabledControls() throws Exception {
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.empty());
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(8);
        when(memory.getBanksCount()).thenReturn(1);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean onEdt = new AtomicBoolean();
        when(memory.readBank(anyInt(), anyInt())).thenAnswer(invocation -> {
            if (release.getCount() > 0) {
                onEdt.set(SwingUtilities.isEventDispatchThread());
                started.countDown();
                release.await();
            }
            return (byte) 0;
        });
        try (DebugSession session = new DebugSession(cpu, memory, null)) {
            DebuggerPanel dialog = null;
            try {
                dialog = onEdt(() -> createPanel(session, mock(Dialogs.class)));
                showInFrame(dialog);
                runOnEdt(dialog::showMemory);
                assertTrue(started.await(1, TimeUnit.SECONDS));
                CountDownLatch edtAvailable = new CountDownLatch(1), restored = new CountDownLatch(1);
                SwingUtilities.invokeLater(edtAvailable::countDown);
                assertTrue(edtAvailable.await(1, TimeUnit.SECONDS));
                assertFalse(onEdt.get());
                JButton search = findButtonByTooltip(dialog, "Find");
                JButton unsupported = findButtonByTooltip(dialog, "Add breakpoint");
                JButton inspect = findButtonByTooltip(dialog.navigationToolbar(), "Back");
                assertFalse(onEdt(search::isEnabled));
                assertFalse(onEdt(unsupported::isEnabled));
                assertFalse(onEdt(inspect::isEnabled));
                runOnEdt(() -> search.addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) restored.countDown();
                }));
                release.countDown();
                assertTrue(restored.await(2, TimeUnit.SECONDS));
                assertTrue(onEdt(search::isEnabled));
                assertFalse(onEdt(unsupported::isEnabled));
                assertTrue(onEdt(inspect::isEnabled));
                JTable table = findComponent(dialog, JTable.class, ignored -> true);
                assertEquals(8, onEdt(table::getRowCount).intValue());
            } finally {
                release.countDown();
                if (dialog != null) {
                    DebuggerPanel closing = dialog;
                    runOnEdt(closing::close);
                }
            }
        }
    }
    @Test(timeout = 10000)
    public void comparisonBaselineKeepsAnnotationsAndDisplaysCapturedValues() throws Exception {
        try (Fixture f = new Fixture()) {
            AnnotationIndex annotations = f.session.annotations();
            annotations.put(new AnnotationIndex.Entry(0, 2, "counter", "Keep this label", true, "DATA"));
            triggerButton(findButtonByTooltip(f.panel, "Capture baseline"));
            awaitReady(f.panel);
            assertSame(annotations, f.session.annotations());
            assertEquals("counter", annotations.get(0, 2).label);
            f.byteValue.set(7);
            triggerButton(findButtonByTooltip(f.panel, "Compare baseline"));
            awaitReady(f.panel);
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertEquals("07", onEdt(() -> table.getValueAt(0, 1)));
            f.byteValue.set(9);
            assertEquals("07", onEdt(() -> table.getValueAt(0, 1)));
            assertEquals(4, table.getColumnCount());
            for (int col = 0; col < table.getColumnCount(); col++)
                assertFalse(table.getColumnName(col).toLowerCase().contains("decode"));
            triggerButton(findButtonByTooltip(f.panel, "Live memory"));
            assertEquals("09", onEdt(() -> table.getValueAt(0, 1)));
        }
    }

    @Test(timeout = 10000)
    public void invalidBreakpointKeepsDraftAndRefreshPreservesBreakpointIdentity() throws Exception {
        try (Fixture f = new Fixture()) {
            JTextField at = findComponent(f.panel, JTextField.class,
                    field -> "Address".equals(field.getAccessibleContext().getAccessibleName()));
            setText(at, "invalid");
            triggerButton(findButtonByTooltip(f.panel, "Add breakpoint"));
            assertEquals("invalid", onEdt(at::getText));
            verify(f.dialogs).showError(anyString(), eq("Debugger"));
            assertTrue(f.session.rules.isEmpty());
            setText(at, "0x0003");
            triggerButton(findButtonByTooltip(f.panel, "Add breakpoint"));
            assertEquals(1, f.session.rules.size());
            JList<?> rules = findComponent(f.panel, JList.class,
                    list -> "Breakpoints".equals(list.getAccessibleContext().getAccessibleName()));
            assertEquals(0, onEdt(rules::getSelectedIndex).intValue());
            f.session.rules.get(0).hits = 99;
            f.refresh();
            assertEquals(0, onEdt(rules::getSelectedIndex).intValue());
            assertTrue(onEdt(() -> rules.getSelectedValue().toString()).contains("hits=99"));
        }
    }

    @Test(timeout = 10000)
    public void enableDisableTogglesOrdinaryBreakpointWithoutLosingRowOrInspectionPolicy() throws Exception {
        try (Fixture f = new Fixture()) {
            java.util.Set<Integer> breakpoints = new java.util.HashSet<>();
            when(f.cpu.isBreakpointSupported()).thenReturn(true);
            when(f.cpu.getBreakpoints()).thenAnswer(call -> java.util.Set.copyOf(breakpoints));
            when(f.cpu.isBreakpointSet(anyInt())).thenAnswer(call -> breakpoints.contains(call.getArgument(0)));
            doAnswer(call -> breakpoints.add(call.getArgument(0))).when(f.cpu).setBreakpoint(anyInt());
            doAnswer(call -> breakpoints.remove(call.getArgument(0))).when(f.cpu).unsetBreakpoint(anyInt());
            f.session.rules.add(new DebugSession.Rule("Advanced", "EXEC", 1, -1, "", 1));
            f.session.setBreakpointState(3, BreakpointState.KEEP_INSPECTION);
            f.refresh();
            JList<?> rules = findComponent(f.panel, JList.class,
                    list -> "Breakpoints".equals(list.getAccessibleContext().getAccessibleName()));
            JButton toggle = findButtonByTooltip(f.panel, "Enable / disable");
            runOnEdt(() -> rules.setSelectedIndex(1));
            triggerButton(toggle);
            assertFalse(f.cpu.isBreakpointSet(3));
            assertEquals(2, onEdt(() -> rules.getModel().getSize()).intValue());
            assertEquals(1, onEdt(rules::getSelectedIndex).intValue());
            assertTrue(onEdt(() -> rules.getSelectedValue().toString()).startsWith("Disabled"));
            assertTrue(onEdt(() -> rules.getSelectedValue().toString()).contains("keep inspection"));
            assertTrue(f.session.rules.get(0).enabled);
            triggerButton(toggle);
            assertEquals(BreakpointState.KEEP_INSPECTION, f.session.breakpointState(3));
            assertTrue(onEdt(() -> rules.getSelectedValue().toString()).startsWith("Enabled"));
            runOnEdt(() -> rules.setSelectedIndex(0));
            triggerButton(toggle);
            assertFalse(f.session.rules.get(0).enabled);
            assertTrue(f.cpu.isBreakpointSet(3));
            runOnEdt(() -> rules.setSelectedIndex(1));
            triggerButton(toggle);
            triggerButton(findButtonByTooltip(f.panel, "Remove"));
            assertEquals(List.of(), f.session.ordinaryBreakpoints());
            assertFalse(f.cpu.isBreakpointSet(3));
            f.session.setBreakpointState(3, BreakpointState.ON);
            assertEquals(BreakpointState.ON, f.session.breakpointState(3));
            verifyNoInteractions(f.dialogs);
        }
    }

    @Test(timeout = 10000)
    public void annotationFilterAndWatchSelectionSurviveRefresh() throws Exception {
        try (Fixture f = new Fixture()) {
            AnnotationIndex annotations = f.session.annotations();
            annotations.put(new AnnotationIndex.Entry(0, 1, "counter", "", false, "DATA"));
            annotations.put(new AnnotationIndex.Entry(0, 2, "other", "", false, "DATA"));
            JTextField filter = findComponent(f.panel, JTextField.class,
                    field -> "Search annotations".equals(field.getAccessibleContext().getAccessibleName()));
            setText(filter, "counter");
            JList<?> rows = findComponent(f.panel, JList.class,
                    list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> rows.setSelectedIndex(0));
            f.session.watches.add("A");
            f.refresh();
            JList<?> watches = findComponent(f.panel.watchesPanel(), JList.class, ignored -> true);
            runOnEdt(() -> watches.setSelectedIndex(0));
            f.refresh();
            assertEquals("counter", onEdt(filter::getText));
            assertEquals(1, onEdt(() -> rows.getModel().getSize()).intValue());
            assertEquals(0, onEdt(rows::getSelectedIndex).intValue());
            assertEquals(0, onEdt(watches::getSelectedIndex).intValue());
        }
    }


    @Test(timeout = 10000)
    public void selectingAnnotationEditsItsAddress() throws Exception {
        try (Fixture f = new Fixture()) {
            AnnotationIndex annotations = f.session.annotations();
            annotations.put(new AnnotationIndex.Entry(0, 3, "counter", "", false, "DATA"));
            f.refresh();
            JList<?> rows = findComponent(f.panel, JList.class,
                    list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> rows.setSelectedIndex(0));
            JTextField label = findComponent(f.panel, JTextField.class,
                    field -> "Label".equals(field.getAccessibleContext().getAccessibleName()));
            setText(label, "renamed");
            // Stepping or browsing must not retarget an annotation draft.
            runOnEdt(() -> f.panel.navigate(0, 0, false));
            triggerButton(annotationSave(f.panel));
            assertEquals("renamed", annotations.get(0, 3).label);
            assertNull(annotations.get(0, 0));
        }
    }

    @Test(timeout = 10000)
    public void annotationOptionsStayHiddenUntilNeededAndSaveWithTheDraft() throws Exception {
        try (Fixture f = new Fixture()) {
            JTabbedPane tabs = findComponent(f.panel, JTabbedPane.class,
                    pane -> "debuggerViews".equals(pane.getName()));
            runOnEdt(() -> {
                tabs.setSelectedIndex(tabs.indexOfTab("Annotations"));
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(f.panel);
                frame.setSize(960, 600);
                frame.setVisible(true);
            });
            JButton save = annotationSave(f.panel);
            JToggleButton more = findToggleButtonByTooltip(f.panel, "Annotation options");
            assertThrows(AssertionError.class, () -> findButtonByTooltip(f.panel, "Export annotations"));
            setText(findComponent(f.panel, JTextField.class,
                    field -> "Label".equals(field.getAccessibleContext().getAccessibleName())), "entry");
            triggerButton(more);
            JComboBox<?> type = findComponent(f.panel, JComboBox.class,
                    combo -> "Type".equals(combo.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(type::isShowing));
            assertEquals("Unspecified", onEdt(type::getSelectedItem));
            runOnEdt(() -> type.setSelectedItem("Code"));
            triggerButton(findComponent(f.panel, JCheckBox.class, check -> "Bookmark".equals(check.getText())));
            triggerButton(more);
            triggerButton(save);
            AnnotationIndex.Entry entry = f.session.annotations().get(0, 0);
            assertEquals("entry", entry.label);
            assertEquals("CODE", entry.kind);
            assertTrue(entry.bookmark);
            assertFalse(onEdt(type::isShowing));
            assertEquals("Saved at 0000", onEdt(save::getText));
            assertFalse(onEdt(save::isEnabled));
        }
    }

    private JButton annotationSave(DebuggerPanel panel) {
        return findComponent(panel, JButton.class, button -> "saveAnnotation".equals(button.getName()));
    }

    private JTextField annotationField(DebuggerPanel panel, String name) {
        return findComponent(panel, JTextField.class,
                field -> name.equals(field.getAccessibleContext().getAccessibleName()));
    }

    private void answerDraftPrompt(Runnable action, String answer) throws Exception {
        java.util.concurrent.CompletableFuture<Void> pending = java.util.concurrent.CompletableFuture.runAsync(
                () -> runOnEdt(action));
        JDialog prompt = waitForWindow(JDialog.class, dialog -> "Unsaved annotation".equals(dialog.getTitle()));
        triggerButton(findButton(prompt, answer));
        pending.get(3, TimeUnit.SECONDS);
    }

    @Test(timeout = 20000)
    public void selectingAnotherAnnotationOffersSaveDiscardAndCancel() throws Exception {
        for (String answer : List.of("Save", "Discard", "Cancel")) {
            try (Fixture f = new Fixture(temporary.newFolder(answer).toPath().resolve("debugger"))) {
                f.session.annotations().put(new AnnotationIndex.Entry(0, 1, "first", "", false, "DATA"));
                f.session.annotations().put(new AnnotationIndex.Entry(0, 2, "second", "", false, "DATA"));
                f.refresh();
                JList<?> rows = findComponent(f.panel, JList.class,
                        list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
                runOnEdt(() -> rows.setSelectedIndex(0));
                JTextField label = annotationField(f.panel, "Label");
                setText(label, "draft");
                answerDraftPrompt(() -> rows.setSelectedIndex(1), answer);
                boolean cancelled = answer.equals("Cancel");
                assertEquals(cancelled ? "draft" : "second", onEdt(label::getText));
                assertEquals(cancelled ? 0 : 1, onEdt(rows::getSelectedIndex).intValue());
                assertEquals(answer.equals("Save") ? "draft" : "first", f.session.annotations().get(0, 1).label);
                if (answer.equals("Save")) {
                    AnnotationIndex persisted = new AnnotationIndex(f.session.captureMemory());
                    String identity = persisted.properties().getProperty("image.identity");
                    persisted.load(temporary.getRoot().toPath().resolve(answer)
                            .resolve("debugger." + identity + ".annotations"));
                    assertEquals("draft", persisted.get(0, 1).label);
                }
            }
        }
    }

    @Test(timeout = 10000)
    public void refreshAndFilteringKeepTheDraftAndItsOriginalTarget() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.annotations().put(new AnnotationIndex.Entry(0, 3, "counter", "", false, "DATA"));
            f.refresh();
            JList<?> rows = findComponent(f.panel, JList.class,
                    list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> rows.setSelectedIndex(0));
            setText(annotationField(f.panel, "Label"), "draft");
            setText(annotationField(f.panel, "Comment"), "Keep this note");
            f.refresh();
            setText(annotationField(f.panel, "Search annotations"), "nothing matches");
            f.refresh();
            runOnEdt(() -> f.panel.navigate(5, 1, false));
            assertEquals("draft", onEdt(annotationField(f.panel, "Label")::getText));
            assertEquals("Keep this note", onEdt(annotationField(f.panel, "Comment")::getText));
            JButton save = annotationSave(f.panel);
            assertEquals("Save at 0003", onEdt(save::getText));
            assertTrue(onEdt(save::getToolTipText).contains("bank 0"));
            triggerButton(save);
            assertEquals("draft", f.session.annotations().get(0, 3).label);
            assertEquals("Keep this note", f.session.annotations().get(0, 3).comment);
            assertNull(f.session.annotations().get(1, 5));
        }
    }

    @Test(timeout = 10000)
    public void newSnapshotAndExitPreflightRespectCancelledDrafts() throws Exception {
        try (Fixture f = new Fixture()) {
            setText(annotationField(f.panel, "Label"), "draft");
            AnnotationIndex original = f.session.annotations();
            runOnEdt(() -> f.panel.navigate(4, 1, false));
            answerDraftPrompt(() -> findButton(f.panel, "New").doClick(0), "Cancel");
            answerDraftPrompt(() -> findButtonByTooltip(f.panel.navigationToolbar(), "Take snapshot").doClick(0), "Cancel");
            AtomicBoolean closeAllowed = new AtomicBoolean(true);
            answerDraftPrompt(() -> closeAllowed.set(f.panel.confirmSaveAnnotations()), "Cancel");
            assertFalse(closeAllowed.get());
            assertSame(original, f.session.annotations());
            assertEquals("draft", onEdt(annotationField(f.panel, "Label")::getText));
            assertEquals("Save at 0000", onEdt(annotationSave(f.panel)::getText));
            answerDraftPrompt(() -> findButton(f.panel, "New").doClick(0), "Discard");
            assertEquals("", onEdt(annotationField(f.panel, "Label")::getText));
            assertEquals("Save at 0004", onEdt(annotationSave(f.panel)::getText));
            assertNull(f.session.annotations().get(0, 0));
            assertTrue(onEdt(f.panel::confirmSaveAnnotations));
        }
    }

    @Test(timeout = 10000)
    public void failedAnnotationSaveKeepsTheDraftAndPreviousStoredEntry() throws Exception {
        java.nio.file.Path directory = temporary.newFolder("save-failure").toPath();
        try (Fixture f = new Fixture(directory.resolve("debugger"))) {
            String identity = f.session.annotations().properties().getProperty("image.identity");
            java.nio.file.Path destination = java.nio.file.Files.createDirectory(
                    directory.resolve("debugger." + identity + ".annotations"));
            java.nio.file.Files.writeString(destination.resolve("block"), "Cannot replace this directory");
            f.session.annotations().put(new AnnotationIndex.Entry(0, 0, "original", "", false, "DATA"));
            f.refresh();
            JList<?> rows = findComponent(f.panel, JList.class,
                    list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> rows.setSelectedIndex(0));
            setText(annotationField(f.panel, "Label"), "draft");
            triggerButton(annotationSave(f.panel));
            assertEquals("original", f.session.annotations().get(0, 0).label);
            assertEquals("draft", onEdt(annotationField(f.panel, "Label")::getText));
            assertTrue(onEdt(annotationSave(f.panel)::isEnabled));
            verify(f.dialogs).showError(anyString(), eq("Debugger"));
            AtomicBoolean closeAllowed = new AtomicBoolean(true);
            answerDraftPrompt(() -> closeAllowed.set(f.panel.confirmSaveAnnotations()), "Save");
            assertFalse(closeAllowed.get());
            assertEquals("draft", onEdt(annotationField(f.panel, "Label")::getText));
            assertEquals("original", f.session.annotations().get(0, 0).label);
        }
    }

    @Test(timeout = 10000)
    public void snapshotSaveRetainsMetadataAndAlignsTheNewEditingTarget() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.annotations().put(new AnnotationIndex.Entry(0, 3, "entry", "", false, "CODE"));
            f.refresh();
            JList<?> rows = findComponent(f.panel, JList.class,
                    list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> rows.setSelectedIndex(0));
            setText(annotationField(f.panel, "Label"), "saved before snapshot");
            runOnEdt(() -> f.panel.navigate(5, 0, false));
            answerDraftPrompt(() -> findButtonByTooltip(f.panel.navigationToolbar(), "Take snapshot").doClick(0), "Save");
            awaitReady(f.panel);
            assertEquals("saved before snapshot", f.session.annotations().get(0, 3).label);
            assertEquals("Save at 0005", onEdt(annotationSave(f.panel)::getText));
            assertEquals(-1, onEdt(rows::getSelectedIndex).intValue());
        }
    }

    @Test(timeout = 10000)
    public void annotationManagementConfirmsDeletionAndExplainsEmptyResults() throws Exception {
        try (Fixture f = new Fixture()) {
            JTabbedPane tabs = findComponent(f.panel, JTabbedPane.class,
                    pane -> "debuggerViews".equals(pane.getName()));
            runOnEdt(() -> {
                tabs.setSelectedIndex(tabs.indexOfTab("Annotations"));
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(f.panel);
                frame.setSize(960, 600);
                frame.setVisible(true);
            });
            JLabel empty = findComponent(f.panel, JLabel.class,
                    label -> label.getText() != null && label.getText().contains("No annotations yet"));
            assertTrue(onEdt(empty::isShowing));
            JButton bundle = findButton(f.panel, "Memory bundle…");
            assertTrue(onEdt(() -> SwingUtilities.isDescendingFrom(bundle,
                    tabs.getComponentAt(tabs.indexOfTab("Memory")))));
            f.session.annotations().put(new AnnotationIndex.Entry(0, 1, "entry", "", false, "DATA"));
            f.refresh();
            assertFalse(onEdt(empty::isShowing));
            JList<?> rows = findComponent(f.panel, JList.class,
                    list -> "Annotations".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> rows.setSelectedIndex(0));
            assertEquals("Annotations", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            for (String answer : List.of("Cancel", "OK")) {
                triggerButton(findButton(f.panel, "Manage…"));
                JPopupMenu menu = onEdt(() -> {
                    for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath())
                        if (element instanceof JPopupMenu) return (JPopupMenu) element;
                    throw new AssertionError("Annotation management menu is not open");
                });
                JMenuItem delete = findComponent(menu, JMenuItem.class,
                        item -> "Delete selected annotation…".equals(item.getText()));
                java.util.concurrent.CompletableFuture<Void> pending = java.util.concurrent.CompletableFuture.runAsync(
                        () -> triggerButton(delete));
                JDialog prompt = waitForWindow(JDialog.class, dialog -> "Delete annotation".equals(dialog.getTitle()));
                triggerButton(findButton(prompt, answer));
                pending.get(3, TimeUnit.SECONDS);
                assertEquals(answer.equals("OK"), f.session.annotations().entries().isEmpty());
            }
            assertTrue(onEdt(empty::isShowing));
        }
    }

    @Test(timeout = 10000)
    public void automaticPcFollowPreservesSearchAndBankChangeClearsIt() throws Exception {
        try (Fixture f = new Fixture()) {
            f.byteValue.set(7);
            f.selectiveValue.set(true);
            JTextField value = findComponent(f.panel, JTextField.class,
                    field -> "Search value".equals(field.getAccessibleContext().getAccessibleName()) && field.getColumns() == 12);
            setText(value, "07");
            triggerButton(findButtonByTooltip(f.panel, "Find"));
            awaitReady(f.panel);
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertEquals(1, onEdt(table::getRowCount).intValue());
            f.stopped();
            assertEquals(1, onEdt(table::getRowCount).intValue());
            assertEquals("0003", onEdt(() -> table.getValueAt(0, 0)));
            doReturn((byte) 9).when(f.session.memory).readBank(3, 1);
            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                    BreakpointDialog.Operation.INSPECT_ONLY, 3, 1, false))));
            assertEquals(8, onEdt(table::getRowCount).intValue());
            assertEquals("09", onEdt(() -> table.getValueAt(3, 1)));
            findLabel(f.panel, "All addresses");
            assertEquals(0, f.session.memory.getSelectedBank());
            assertThrows(AssertionError.class, () -> findComponent(f.panel.navigationToolbar(), JComboBox.class,
                    field -> "Bank".equals(field.getAccessibleContext().getAccessibleName())));
        }
    }

    @Test(timeout = 10000)
    public void currentProgramRebindsAnnotationImageWithoutReplacingComparisonBaseline() throws Exception {
        java.nio.file.Path settings = temporary.getRoot().toPath().resolve("debugger");
        try (Fixture f = new Fixture(settings)) {
            MemoryAnalysis originalImage = f.session.image();
            AnnotationIndex original = f.session.annotations();
            original.put(new AnnotationIndex.Entry(0, 2, "old program", "", false, "CODE"));
            f.byteValue.set(12);
            triggerButton(findButtonByTooltip(f.panel.navigationToolbar(), "Take snapshot"));
            awaitReady(f.panel);
            assertEquals(12, f.session.image().value(0, 0));
            assertNotSame(original, f.session.annotations());
            assertTrue(f.session.annotations().entries().isEmpty());
            assertEquals("old program", original.get(0, 2).label);
            AnnotationIndex saved = new AnnotationIndex(originalImage);
            saved.load(settings.resolveSibling("debugger." + originalImage.identity() + ".annotations"));
            assertEquals("old program", saved.get(0, 2).label);
            triggerButton(findButtonByTooltip(f.panel, "Compare baseline"));
            awaitReady(f.panel);
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertEquals(8, onEdt(table::getRowCount).intValue());
            findLabel(f.panel, "8 changed bytes");
        }
    }

    @Test(timeout = 10000)
    @SuppressWarnings("unchecked")
    public void interruptedInitialCaptureRecoversAfterPause() throws Exception {
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.empty());
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(8);
        when(memory.getBanksCount()).thenReturn(1);
        CountDownLatch reading = new CountDownLatch(1), release = new CountDownLatch(1);
        when(memory.readBank(anyInt(), anyInt())).thenAnswer(invocation -> {
            if (release.getCount() > 0) { reading.countDown(); release.await(); }
            return (byte) 3;
        });
        try (DebugSession session = new DebugSession(cpu, memory, null)) {
            DebuggerPanel panel = onEdt(() -> createPanel(session, mock(Dialogs.class)));
            try {
                assertTrue(reading.await(1, TimeUnit.SECONDS));
                ArgumentCaptor<CPU.CPUListener> listeners = ArgumentCaptor.forClass(CPU.CPUListener.class);
                verify(cpu, atLeastOnce()).addCPUListener(listeners.capture());
                runOnEdt(() -> listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_RUNNING)));
                release.countDown();
                runOnEdt(() -> listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK)));
                awaitReady(panel);
                runOnEdt(panel::showMemory);
                JTable table = findComponent(panel, JTable.class, ignored -> true);
                assertEquals(8, onEdt(table::getRowCount).intValue());
                assertEquals("03", onEdt(() -> table.getValueAt(0, 1)));
            } finally {
                release.countDown();
                runOnEdt(panel::close);
            }
        }
    }

    @Test(timeout = 10000)
    public void recordingControlMatchesSessionAcrossRunAndPause() throws Exception {
        try (Fixture f = new Fixture()) {
            JToggleButton recording = findComponent(f.panel, JToggleButton.class,
                    control -> "Record recent instructions".equals(control.getToolTipText()));
            ArgumentCaptor<CPU.CPUListener> listeners = ArgumentCaptor.forClass(CPU.CPUListener.class);
            verify(f.cpu, atLeastOnce()).addCPUListener(listeners.capture());
            listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_RUNNING));
            runOnEdt(() -> {});
            assertFalse(onEdt(recording::isEnabled));
            assertFalse(onEdt(recording::isSelected));
            assertFalse(f.session.isRecording());
            f.stopped();
            assertTrue(onEdt(recording::isEnabled));
            triggerButton(recording);
            assertTrue(f.session.isRecording());
            listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_RUNNING));
            runOnEdt(() -> {});
            assertFalse(onEdt(recording::isEnabled));
            assertTrue(onEdt(recording::isSelected));
            f.stopped();
            assertTrue(onEdt(recording::isEnabled));
            assertTrue(onEdt(recording::isSelected));
        }
    }

    @Test(timeout = 10000)
    public void wordMemoryRecordingControlsAndHistoryRecoverAfterRunAndPause() throws Exception {
        CPU cpu = mock(CPU.class);
        DebugCPU debug = mock(DebugCPU.class);
        MemoryContext<Integer> memory = mock(MemoryContext.class);
        doReturn(Integer.class).when(memory).getCellTypeClass();
        when(memory.getSize()).thenReturn(16);
        when(memory.getBanksCount()).thenReturn(1);
        doReturn(memory).when(debug).memory();
        when(debug.processor()).thenReturn("word CPU");
        when(debug.registers()).thenReturn(List.of(new DebugCPU.Register("PC", 32)));
        when(cpu.getDebugger()).thenReturn(Optional.of(debug));
        when(cpu.getAddressSpaceSize()).thenReturn(16);
        try (DebugSession session = new DebugSession(cpu, memory, null)) {
            DebuggerPanel panel = onEdt(() -> createPanel(session, mock(Dialogs.class)));
            try {
                JToggleButton recording = findToggleButtonByTooltip(panel, "Record recent instructions");
                triggerButton(recording);
                java.nio.file.Path file = temporary.getRoot().toPath().resolve("word.emutrace");
                session.recordTo(file);
                ArgumentCaptor<CPU.CPUListener> listeners = ArgumentCaptor.forClass(CPU.CPUListener.class);
                verify(cpu, atLeastOnce()).addCPUListener(listeners.capture());
                listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_RUNNING));
                runOnEdt(() -> {});
                assertFalse(onEdt(recording::isEnabled));
                session.afterInstruction(historyEvent(1, 0, 42, 4));
                listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK));
                runOnEdt(() -> {});
                assertTrue(onEdt(recording::isEnabled));
                assertEquals(1, onEdt(historyTable(panel)::getRowCount).intValue());
                JButton fileRecording = findButtonByTooltip(panel, "Stop file recording");
                assertTrue(onEdt(fileRecording::isEnabled));
                triggerButton(fileRecording);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (session.recordingPath() != null && System.nanoTime() < deadline) Thread.sleep(10);
                runOnEdt(() -> {});
                assertNull(session.recordingPath());
            } finally { runOnEdt(panel::close); }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private DebuggerPanel createPanel(DebugSession session, Dialogs dialogs) {
        EmuStudioGui gui = new EmuStudioGui();
        DebugTableModel original = mock(DebugTableModel.class);
        DebuggerColumn column = mock(DebuggerColumn.class);
        when(column.getClassType()).thenReturn(String.class);
        when(column.getDefaultWidth()).thenReturn(-1);
        when(original.getColumnCount()).thenReturn(1);
        when(original.getColumnName(0)).thenReturn("Instruction");
        when(original.getColumnClass(0)).thenReturn((Class) String.class);
        when(original.getColumnAt(0)).thenReturn(column);
        when(original.getRowCount()).thenReturn(1);
        when(original.getLocationAt(0)).thenReturn(0);
        when(original.getValueAt(0, 0)).thenReturn("nop");
        DebugTableImpl table = new DebugTableImpl(original);
        JScrollPane scroll = gui.scrollPane(table);
        JPanel view = new JPanel(new BorderLayout());
        view.add(scroll);
        return new DebuggerPanel(session, table, scroll, view, ignored -> {}, () -> {}, gui, dialogs);
    }

    private void awaitReady(DebuggerPanel panel) throws Exception {
        JButton find = findButtonByTooltip(panel, "Find");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!onEdt(find::isEnabled) && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue("Analysis should finish", onEdt(find::isEnabled));
    }

    @Test(timeout = 10000)
    public void oneTableRetainsCapturedBytesSelectionAndInstructionAnnotationsAcrossModes() throws Exception {
        try (Fixture f = new Fixture()) {
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            javax.swing.table.TableModel bytes = onEdt(table::getModel);
            f.session.annotations().put(new AnnotationIndex.Entry(0, 0, "entry", "Start here", false, "CODE"));
            f.byteValue.set(7);
            triggerButton(findButtonByTooltip(f.panel, "Capture baseline"));
            awaitReady(f.panel);
            f.byteValue.set(9);
            triggerButton(findButtonByTooltip(f.panel, "Compare baseline"));
            awaitReady(f.panel);
            runOnEdt(() -> table.setRowSelectionInterval(3, 3));
            f.byteValue.set(11);

            runOnEdt(f.panel::showInstructions);
            assertSame(table, findComponent(f.panel, JTable.class, ignored -> true));
            assertEquals(2, countTables(f.panel)); // Shared instruction/byte table plus independent History table.
            assertEquals("bp", onEdt(() -> table.getColumnName(0)));
            assertEquals("Instruction", onEdt(() -> table.getColumnName(1)));
            assertEquals("nop", onEdt(() -> table.getValueAt(0, 1)));
            assertEquals("entry", onEdt(() -> table.getValueAt(0, 2)));
            assertEquals("Start here", onEdt(() -> table.getValueAt(0, 3)));
            runOnEdt(() -> f.panel.navigate(3, 0, true));
            verify(f.cpu, never()).setInstructionLocation(anyInt());

            runOnEdt(f.panel::showMemory);
            assertSame(table, findComponent(f.panel, JTable.class, ignored -> true));
            assertSame(bytes, onEdt(table::getModel));
            assertEquals(3, onEdt(table::getSelectedRow).intValue());
            assertEquals("09", onEdt(() -> table.getValueAt(3, 1)));
            assertEquals(2, countTables(f.panel)); // Shared instruction/byte table plus independent History table.
        }
    }

    @Test(timeout = 10000)
    public void inspectOnlyReadsLiveMemoryAndNeverChangesBreakpoints() throws Exception {
        java.nio.file.Path settings = temporary.getRoot().toPath().resolve("inspect-only.debugger");
        try (Fixture f = new Fixture(settings)) {
            DebugSession.Rule existing = new DebugSession.Rule("Keep", "WRITE", 2, 0, "", 1);
            f.session.rules.add(existing);
            f.session.save();
            byte[] saved = java.nio.file.Files.readAllBytes(settings);
            f.byteValue.set(7);
            triggerButton(findButtonByTooltip(f.panel, "Capture baseline"));
            awaitReady(f.panel);
            f.byteValue.set(9);
            triggerButton(findButtonByTooltip(f.panel, "Compare baseline"));
            awaitReady(f.panel);
            f.byteValue.set(11);
            when(f.session.memory.getSize()).thenReturn(16);

            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                    BreakpointDialog.Operation.INSPECT_ONLY, 12, 0, false))));
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertFalse(onEdt(f.panel::isInstructionView));
            assertEquals(16, onEdt(table::getRowCount).intValue());
            assertEquals(12, onEdt(table::getSelectedRow).intValue());
            assertEquals("0B", onEdt(() -> table.getValueAt(12, 1)));
            findLabel(f.panel, "All addresses");
            assertEquals(List.of(existing), f.session.rules);
            assertArrayEquals(saved, java.nio.file.Files.readAllBytes(settings));
            verify(f.cpu, never()).setBreakpoint(anyInt());
            verify(f.cpu, never()).unsetBreakpoint(anyInt());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void breakpointButtonCyclesInPlaceAndPortableKeepPolicyPersists() throws Exception {
        try (Fixture f = new Fixture()) {
            runOnEdt(f.panel::showInstructions);
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertEquals(BreakpointState.OFF, onEdt(() -> table.getValueAt(0, 0)));
            for (BreakpointState expected : new BreakpointState[]{BreakpointState.ON, BreakpointState.KEEP_INSPECTION, BreakpointState.OFF}) {
                runOnEdt(() -> {
                    assertTrue(table.editCellAt(0, 0));
                    ((AbstractButton) table.getEditorComponent()).doClick(0);
                });
                runOnEdt(() -> {});
                assertTrue(onEdt(f.panel::isInstructionView));
                assertEquals(expected, onEdt(() -> table.getValueAt(0, 0)));
                if (expected == BreakpointState.KEEP_INSPECTION) {
                    try (DebugSession loaded = new DebugSession(f.cpu, f.session.memory,
                            temporary.getRoot().toPath().resolve("debugger"))) {
                        assertEquals(BreakpointState.KEEP_INSPECTION, loaded.breakpointState(0));
                    }
                }
            }
            assertTrue(f.session.rules.isEmpty());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void everyHitReplacesCapturedResultsAndOverridesPreviousHeldInspection() throws Exception {
        try (Fixture f = new Fixture()) {
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            f.byteValue.set(7);
            triggerButton(findButtonByTooltip(f.panel, "Capture baseline"));
            awaitReady(f.panel);
            f.byteValue.set(9);
            triggerButton(findButtonByTooltip(f.panel, "Compare baseline"));
            awaitReady(f.panel);
            f.byteValue.set(11);
            f.session.setBreakpointState(3, BreakpointState.KEEP_INSPECTION);
            doReturn((byte) 13).when(f.session.memory).readBank(3, 1);
            f.session.setBreakpointState(4, BreakpointState.ON);
            runOnEdt(f.panel::showInstructions);
            assertTrue(f.session.beforeInstruction(3, 1));
            when(f.cpu.getInstructionLocation()).thenReturn(2);
            f.stopped();
            assertFalse(onEdt(f.panel::isInstructionView));
            assertEquals(3, onEdt(table::getSelectedRow).intValue());
            assertEquals("0D", onEdt(() -> table.getValueAt(3, 1)));
            findLabel(f.panel, "All addresses");
            runOnEdt(f.panel::showInstructions);
            verify(((DebugTableImpl) table).instructionModel()).currentPage();
            runOnEdt(f.panel::showMemory);
            assertEquals(3, onEdt(table::getSelectedRow).intValue());
            assertEquals("0D", onEdt(() -> table.getValueAt(3, 1)));

            when(f.cpu.getInstructionLocation()).thenReturn(5);
            f.stopped();
            assertEquals(3, onEdt(table::getSelectedRow).intValue()); // Held while stepping.
            assertTrue(f.session.beforeInstruction(4, 0));
            f.stopped();
            assertEquals(4, onEdt(table::getSelectedRow).intValue()); // New hit always wins.
            f.stopped();
            assertEquals(5, onEdt(table::getSelectedRow).intValue()); // Ordinary BP resumes following PC.
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void resumeBeforeQueuedStopDoesNotInspectStaleHit() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.setBreakpointState(3, BreakpointState.ON);
            runOnEdt(f.panel::showInstructions);
            ArgumentCaptor<CPU.CPUListener> listeners = ArgumentCaptor.forClass(CPU.CPUListener.class);
            verify(f.cpu, atLeastOnce()).addCPUListener(listeners.capture());
            runOnEdt(() -> {
                assertTrue(f.session.beforeInstruction(3, 0));
                listeners.getAllValues().forEach(l -> l.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK));
                listeners.getAllValues().forEach(l -> l.runStateChanged(CPU.RunState.STATE_RUNNING));
            });
            runOnEdt(() -> {});
            assertTrue(onEdt(f.panel::isInstructionView));
            verifyNoInteractions(f.dialogs);
            listeners.getAllValues().forEach(l -> l.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK));
            runOnEdt(() -> {});
            assertTrue(onEdt(f.panel::isInstructionView));
        }
    }

    @Test(timeout = 10000)
    public void nativeBreakpointCanInspectImmediatelyAndUnsetWithoutChangingView() throws Exception {
        try (Fixture f = new Fixture()) {
            java.util.Set<Integer> breakpoints = new java.util.HashSet<>();
            when(f.cpu.isBreakpointSupported()).thenReturn(true);
            when(f.cpu.getBreakpoints()).thenAnswer(call -> java.util.Set.copyOf(breakpoints));
            when(f.cpu.isBreakpointSet(anyInt())).thenAnswer(call -> breakpoints.contains(call.getArgument(0)));
            doAnswer(call -> breakpoints.add(call.getArgument(0))).when(f.cpu).setBreakpoint(anyInt());
            doAnswer(call -> breakpoints.remove(call.getArgument(0))).when(f.cpu).unsetBreakpoint(anyInt());
            doReturn((byte) 42).when(f.session.memory).readBank(3, 1);
            runOnEdt(f.panel::showInstructions);
            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                    BreakpointDialog.Operation.SET, 3, 1, true))));
            assertEquals(java.util.Set.of(3), breakpoints);
            assertFalse(onEdt(f.panel::isInstructionView));
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertEquals(3, onEdt(table::getSelectedRow).intValue());
            assertEquals("2A", onEdt(() -> table.getValueAt(3, 1)));

            runOnEdt(f.panel::showInstructions);
            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                    BreakpointDialog.Operation.UNSET, 3, 1, false))));
            assertTrue(breakpoints.isEmpty());
            assertTrue(onEdt(f.panel::isInstructionView));
            assertTrue(f.session.rules.isEmpty());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void portableSimpleBreakpointsAreIdempotentAndKeepAdvancedRules() throws Exception {
        try (Fixture f = new Fixture()) {
            DebugSession.Rule counted = new DebugSession.Rule("Counted", "EXEC", 3, -1, "", 2);
            f.session.rules.add(counted);
            BreakpointDialog.Request set = new BreakpointDialog.Request(BreakpointDialog.Operation.SET, 3, 0, false);
            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(set)));
            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(set)));
            assertEquals(2, f.session.rules.size());
            assertTrue(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                    BreakpointDialog.Operation.UNSET, 3, 0, false))));
            assertEquals(List.of(counted), f.session.rules);
            verify(f.cpu, never()).setBreakpoint(anyInt());
            verify(f.cpu, never()).unsetBreakpoint(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void invalidInspectionCannotPartiallySetBreakpoint() throws Exception {
        try (Fixture f = new Fixture()) {
            when(f.cpu.isBreakpointSupported()).thenReturn(true);
            when(f.cpu.getAddressSpaceSize()).thenReturn(16);
            assertFalse(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                    BreakpointDialog.Operation.SET, 8, 0, true))));
            verify(f.dialogs).showError("Address outside memory", "Debugger");
            verify(f.cpu, never()).setBreakpoint(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void invalidBankCannotPartiallySetBreakpoint() throws Exception {
        try (Fixture f = new Fixture()) {
            when(f.cpu.isBreakpointSupported()).thenReturn(true);
            for (int invalid : new int[]{-1, 2}) {
                assertFalse(onEdt(() -> f.panel.applyBreakpointRequest(new BreakpointDialog.Request(
                        BreakpointDialog.Operation.SET, 3, invalid, false))));
            }
            verify(f.dialogs, times(2)).showError("Bank outside memory", "Debugger");
            verify(f.cpu, never()).setBreakpoint(anyInt());
        }
    }

    private int countTables(java.awt.Container root) {
        int count = root instanceof JTable ? 1 : 0;
        for (java.awt.Component child : root.getComponents())
            if (child instanceof java.awt.Container) count += countTables((java.awt.Container) child);
        return count;
    }

    @Test(timeout = 10000)
    public void navigationHistoryRestoresExactByteModeAfterFollowingAnInstructionReference() throws Exception {
        try (Fixture f = new Fixture()) {
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            runOnEdt(() -> {
                f.panel.navigate(1, 0, true);
                f.panel.showBreakpoints();
                f.panel.navigate(5, 0, true);
            });
            assertTrue(onEdt(f.panel::isInstructionView));
            triggerButton(findButtonByTooltip(f.panel.navigationToolbar(), "Back"));
            assertFalse(onEdt(f.panel::isInstructionView));
            assertEquals("Byte", onEdt(() -> table.getColumnName(1)));
            assertEquals(1, onEdt(table::getSelectedRow).intValue());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void optionalStringColumnPreservesReorderedCommentWidthAndFindStringsExposesResults() throws Exception {
        try (Fixture f = new Fixture()) {
            JTable table = findComponent(f.panel, JTable.class, ignored -> true);
            assertEquals(4, onEdt(table::getColumnCount).intValue());
            runOnEdt(() -> table.moveColumn(3, 0));
            JCheckBox strings = findComponent(f.panel, JCheckBox.class, box -> "String column".equals(box.getText()));
            triggerButton(findComponent(f.panel, JToggleButton.class,
                    button -> "Compare / strings".equals(button.getToolTipText())));
            triggerButton(strings);
            assertEquals(5, onEdt(table::getColumnCount).intValue());
            assertEquals("Comment", onEdt(() -> table.getColumnName(0)));
            assertTrue(onEdt(() -> table.getColumnModel().getColumn(0).getMaxWidth()) > 360);
            triggerButton(strings);
            assertEquals(4, onEdt(table::getColumnCount).intValue());
            f.byteValue.set(65);
            triggerButton(findButtonByTooltip(f.panel, "Find strings"));
            awaitReady(f.panel);
            assertTrue(onEdt(strings::isSelected));
            assertEquals(5, onEdt(table::getColumnCount).intValue());
            assertEquals("AAAAAAAA", onEdt(() -> table.getModel().getValueAt(0, 4)));
        }
    }

    @Test(timeout = 10000)
    public void historyEmphasizesChangesAndRevealsUnchangedRegistersOnDemand() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.record(1024);
            f.session.afterInstruction(new DebugCPU.Instruction(1, 0, 0, "inc a", "3C",
                    java.util.Map.of("PC", 0L, "SP", 5L, "A", 1L),
                    java.util.Map.of("PC", 1L, "SP", 5L, "A", 2L), List.of(), true));
            f.refresh();
            JTable history = historyTable(f.panel);
            runOnEdt(() -> history.setRowSelectionInterval(0, 0));
            JTextArea state = findComponent(f.panel, JTextArea.class,
                    area -> "Recorded instruction state".equals(area.getAccessibleContext().getAccessibleName()));
            String changes = onEdt(state::getText);
            assertTrue(changes.contains("Changed registers"));
            assertTrue(changes.indexOf("A ") < changes.indexOf("PC "));
            assertFalse(changes.contains("SP "));
            triggerButton(findComponent(f.panel, JCheckBox.class,
                    box -> "Show unchanged registers".equals(box.getText())));
            assertTrue(onEdt(state::getText).contains("SP "));
            assertEquals(0, onEdt(history::getSelectedRow).intValue());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    private JTable historyTable(DebuggerPanel panel) {
        return findComponent(panel, JTable.class,
                table -> "Recorded instructions".equals(table.getAccessibleContext().getAccessibleName()));
    }

    private JTextField historyField(DebuggerPanel panel, String name) {
        return findComponent(panel, JTextField.class, field -> name.equals(field.getAccessibleContext().getAccessibleName()));
    }

    private DebugCPU.Instruction historyEvent(long sequence, int pc, int value, int writeAddress) {
        return new DebugCPU.Instruction(sequence, pc, 1, "ld a, " + value, "3E " + String.format("%02X", value),
                java.util.Map.of("PC", (long) pc, "A", 0L),
                java.util.Map.of("PC", (long) pc + 2, "A", (long) value),
                List.of(new DebugCPU.Access(writeAddress, 1, value, true)), true);
    }

    @Test(timeout = 10000)
    public void historyFiltersKeepSequenceNumbersAndPreviousWriteRevealsEarlierEvent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.record(10);
            f.session.afterInstruction(historyEvent(10, 0, 1, 3));
            f.session.afterInstruction(historyEvent(11, 2, 2, 4));
            f.session.afterInstruction(historyEvent(12, 4, 3, 3));
            f.refresh();
            JTable history = historyTable(f.panel);
            setText(historyField(f.panel, "PC from"), "0x2");
            setText(historyField(f.panel, "Changed register"), "a");
            setText(historyField(f.panel, "Memory address"), "3");
            setText(historyField(f.panel, "Memory bank"), "1");
            triggerButton(findButton(f.panel, "Apply filters"));
            assertEquals(1, onEdt(history::getRowCount).intValue());
            assertEquals("12", onEdt(() -> history.getValueAt(0, 0)));
            assertEquals("ld a, 3", onEdt(() -> history.getValueAt(0, 2)));
            runOnEdt(() -> history.setRowSelectionInterval(0, 0));
            triggerButton(findButton(f.panel, "Find earlier write"));
            assertEquals(2, onEdt(history::getRowCount).intValue());
            assertEquals("10", onEdt(() -> history.getValueAt(history.getSelectedRow(), 0)));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void historyFoldingPreservesRecordedEventsAndCanBeExpanded() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.record(10);
            for (int i = 1; i <= 3; i++)
                f.session.afterInstruction(new DebugCPU.Instruction(i, 0, 0, "jp 0", "C3 00 00",
                        java.util.Map.of("PC", 0L), java.util.Map.of("PC", 0L), List.of(), true));
            f.refresh();
            JTable history = historyTable(f.panel);
            JCheckBox fold = findComponent(f.panel, JCheckBox.class, box -> "Fold identical repeats".equals(box.getText()));
            triggerButton(fold);
            assertEquals(1, onEdt(history::getRowCount).intValue());
            assertEquals("1–3 (3)", onEdt(() -> history.getValueAt(0, 0)));
            assertEquals(3, f.session.history().size());
            triggerButton(fold);
            assertEquals(3, onEdt(history::getRowCount).intValue());
        }
    }

    @Test(timeout = 10000)
    public void recentHistoryLimitIsConfigurableAndPreservesVisibleSequence() throws Exception {
        try (Fixture f = new Fixture()) {
            JSpinner limit = findComponent(f.panel, JSpinner.class,
                    spinner -> "Keep last".equals(spinner.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> limit.setValue(2));
            triggerButton(findToggleButtonByTooltip(f.panel, "Record recent instructions"));
            for (int i = 1; i <= 3; i++) f.session.afterInstruction(historyEvent(i, 0, i, 3));
            f.refresh();
            JTable history = historyTable(f.panel);
            assertEquals(2, onEdt(history::getRowCount).intValue());
            assertEquals("2", onEdt(() -> history.getValueAt(0, 0)));
        }
    }

    @Test(timeout = 10000)
    public void offlineHistoryPagesUseCapturedProcessorAndValuesWithoutLiveAnnotations() throws Exception {
        try (Fixture f = new Fixture()) {
            java.nio.file.Path file = temporary.getRoot().toPath().resolve("offline.emutrace");
            InstructionRecording.Header header = new InstructionRecording.Header("Z80 capture", List.of(new DebugCPU.Register("A", 8)));
            java.util.ArrayList<DebugCPU.Instruction> events = new java.util.ArrayList<>();
            for (int i = 1; i <= 2049; i++) events.add(historyEvent(i, 0x8000, 42, 0x9000));
            InstructionRecording.write(file, header, events);
            f.session.annotations().put(new AnnotationIndex.Entry(0, 0, "unrelated-live-label", "", false, "CODE"));
            f.byteValue.set(99);
            runOnEdt(() -> f.panel.openRecording(file));
            awaitReady(f.panel);
            JTable history = historyTable(f.panel);
            assertEquals(1024, onEdt(history::getRowCount).intValue());
            assertEquals("1:8000", onEdt(() -> history.getValueAt(0, 1)));
            assertEquals("ld a, 42", onEdt(() -> history.getValueAt(0, 2)));
            JLabel status = findComponent(f.panel, JLabel.class,
                    label -> "Recording status".equals(label.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(status::getText).contains("Z80 capture"));
            triggerButton(findButtonByTooltip(f.panel, "Next page"));
            awaitReady(f.panel);
            assertEquals(1024, onEdt(history::getRowCount).intValue());
            assertEquals("1025", onEdt(() -> history.getValueAt(0, 0)));
            triggerButton(findButton(f.panel, "Next page"));
            awaitReady(f.panel);
            assertEquals(1, onEdt(history::getRowCount).intValue());
            assertEquals("2049", onEdt(() -> history.getValueAt(0, 0)));
            assertFalse(onEdt(findButton(f.panel, "Next page")::isEnabled));
            triggerButton(findButton(f.panel, "Previous page"));
            awaitReady(f.panel);
            assertEquals("1025", onEdt(() -> history.getValueAt(0, 0)));
            runOnEdt(() -> history.setRowSelectionInterval(0, 0));
            JTextArea state = findComponent(f.panel, JTextArea.class,
                    area -> "Recorded instruction state".equals(area.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(state::getText).contains("WRITE bank 1:9000 = 2A"));
            assertFalse(onEdt(state::getText).contains("unrelated-live-label"));
            triggerButton(findButtonByTooltip(f.panel, "First page"));
            awaitReady(f.panel);
            assertEquals("1", onEdt(() -> history.getValueAt(0, 0)));
            assertFalse(onEdt(findButton(f.panel, "First page")::isEnabled));
            assertFalse(onEdt(findButton(f.panel, "Previous page")::isEnabled));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void historyGuidesFirstCaptureAndKeepsSecondaryControlsCollapsed() throws Exception {
        try (Fixture f = new Fixture()) {
            JTabbedPane tabs = findComponent(f.panel, JTabbedPane.class, pane -> "debuggerViews".equals(pane.getName()));
            runOnEdt(() -> {
                tabs.setSelectedIndex(tabs.indexOfTab("History"));
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(f.panel);
                frame.setSize(1000, 720);
                frame.setVisible(true);
            });
            JLabel empty = findComponent(f.panel, JLabel.class,
                    label -> "History empty state".equals(label.getAccessibleContext().getAccessibleName()));
            assertEquals("No instructions recorded", onEdt(empty::getText));
            assertTrue(onEdt(empty::isShowing));
            assertFalse(onEdt(historyField(f.panel, "PC from")::isShowing));
            assertFalse(onEdt(findButton(f.panel, "Save recent execution…")::isShowing));
            assertFalse(onEdt(findButton(f.panel, "Next page")::isShowing));
            triggerButton(findButton(f.panel, "Start recording"));
            assertTrue(f.session.isRecording());
            assertEquals("Stop recording", onEdt(() -> findToggleButtonByTooltip(f.panel, "Record recent instructions")
                    .getAccessibleContext().getAccessibleName()));
            assertEquals("Ready to capture instructions", onEdt(empty::getText));
            f.session.afterInstruction(historyEvent(1, 0, 1, 3));
            f.refresh();
            assertFalse(onEdt(empty::isShowing));
            assertTrue(onEdt(historyTable(f.panel)::isShowing));
            triggerButton(findToggleButtonByTooltip(f.panel, "History options"));
            assertTrue(onEdt(findButton(f.panel, "Save recent execution…")::isShowing));
            triggerButton(findToggleButtonByTooltip(f.panel, "History filters"));
            assertTrue(onEdt(historyField(f.panel, "PC from")::isShowing));
            assertFalse(onEdt(findButton(f.panel, "Save recent execution…")::isShowing));
            setText(historyField(f.panel, "Memory address"), "99");
            triggerButton(findButton(f.panel, "Apply filters"));
            assertTrue(onEdt(empty::isShowing));
            assertEquals("No instructions match these filters", onEdt(empty::getText));
            triggerButton(findToggleButtonByTooltip(f.panel, "History filters"));
            JLabel summary = findComponent(f.panel, JLabel.class,
                    label -> "Active history filters".equals(label.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(summary::isShowing));
            assertTrue(onEdt(summary::getText).contains("Memory 0063"));
            triggerButton(findButton(f.panel, "Clear filters"));
            assertEquals(1, onEdt(historyTable(f.panel)::getRowCount).intValue());
            assertFalse(onEdt(empty::isShowing));
            verifyNoInteractions(f.dialogs);
        }
    }

    @Test(timeout = 10000)
    public void savedHistoryFiltersAndEarlierWritesSearchBeyondStoragePages() throws Exception {
        try (Fixture f = new Fixture()) {
            java.nio.file.Path file = temporary.getRoot().toPath().resolve("long-history.emutrace");
            java.util.ArrayList<DebugCPU.Instruction> events = new java.util.ArrayList<>();
            for (int i = 1; i <= 2051; i++) events.add(historyEvent(i, i == 2050 ? 0x9000 : 0x8000, i, 0x9000));
            InstructionRecording.write(file, new InstructionRecording.Header("Captured CPU", List.of()), events);
            runOnEdt(() -> f.panel.openRecording(file));
            awaitReady(f.panel);
            setText(historyField(f.panel, "PC from"), "0x9000");
            runOnEdt(() -> historyField(f.panel, "PC from").postActionEvent());
            awaitReady(f.panel);
            JTable history = historyTable(f.panel);
            assertEquals(1, onEdt(history::getRowCount).intValue());
            assertEquals("2050", onEdt(() -> history.getValueAt(0, 0)));
            runOnEdt(() -> history.setRowSelectionInterval(0, 0));
            triggerButton(findButton(f.panel, "Find earlier write"));
            awaitReady(f.panel);
            assertEquals("2049", onEdt(() -> history.getValueAt(history.getSelectedRow(), 0)));
            triggerButton(findButton(f.panel, "Find earlier write"));
            awaitReady(f.panel);
            assertEquals("2048", onEdt(() -> history.getValueAt(history.getSelectedRow(), 0)));
            JLabel summary = findComponent(f.panel, JLabel.class,
                    label -> "Active history filters".equals(label.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(summary::getText).contains("whole recording"));
            assertEquals("", onEdt(historyField(f.panel, "PC from")::getText));
            setText(historyField(f.panel, "Memory address"), "0xFFFF");
            triggerButton(findButton(f.panel, "Apply filters"));
            awaitReady(f.panel);
            assertEquals(0, onEdt(history::getRowCount).intValue());
            findLabel(f.panel, "Searched whole recording. Clear or change the filters.");
            triggerButton(findButton(f.panel, "Clear filters"));
            awaitReady(f.panel);
            assertEquals("1", onEdt(() -> history.getValueAt(0, 0)));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
            verifyNoInteractions(f.dialogs);
        }
    }

    @Test(timeout = 10000)
    public void cancellingRecordingLoadKeepsPreviousSourceAndResults() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.record(10);
            f.session.afterInstruction(historyEvent(11, 0, 1, 3));
            f.refresh();
            java.nio.file.Path file = temporary.getRoot().toPath().resolve("cancelled.emutrace");
            InstructionRecording.write(file, new InstructionRecording.Header("Other CPU", List.of()),
                    List.of(historyEvent(22, 0x8000, 42, 0x9000)));
            JButton cancel = findButton(f.panel, "Cancel");
            runOnEdt(() -> {
                f.panel.openRecording(file);
                assertTrue(cancel.isEnabled());
                assertTrue(cancel.isVisible());
                assertFalse(findButton(f.panel, "Open recording…").isEnabled());
                cancel.doClick(0);
            });
            awaitReady(f.panel);
            JComboBox<?> source = findComponent(f.panel, JComboBox.class,
                    combo -> "History source".equals(combo.getAccessibleContext().getAccessibleName()));
            assertEquals("Recent execution", onEdt(source::getSelectedItem));
            assertEquals("11", onEdt(() -> historyTable(f.panel).getValueAt(0, 0)));
            verifyNoInteractions(f.dialogs);
        }
    }

    @Test(timeout = 10000)
    public void savedHistoryExportsCapturedSourceAndCannotSaveDifferentLiveBuffer() throws Exception {
        try (Fixture f = new Fixture()) {
            f.session.record(10);
            f.session.afterInstruction(historyEvent(1, 0, 99, 3));
            f.refresh();
            java.nio.file.Path recent = temporary.getRoot().toPath().resolve("recent.emutrace");
            runOnEdt(() -> f.panel.saveHistory(recent, false));
            awaitReady(f.panel);
            try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recent)) {
                assertEquals("Test CPU", reader.header().processor);
                assertEquals(1, reader.nextPage(10).get(0).sequence);
            }
            java.nio.file.Path captured = temporary.getRoot().toPath().resolve("captured.emutrace");
            InstructionRecording.write(captured, new InstructionRecording.Header("Captured CPU", List.of()),
                    List.of(historyEvent(50, 0x8000, 42, 0x9000), historyEvent(51, 0x8002, 43, 0x9000)));
            runOnEdt(() -> f.panel.openRecording(captured));
            awaitReady(f.panel);
            assertFalse(onEdt(findButton(f.panel, "Save recent execution…")::isVisible));
            java.nio.file.Path wrong = temporary.getRoot().toPath().resolve("wrong-source.emutrace");
            assertThrows(IllegalStateException.class, () -> runOnEdt(() -> f.panel.saveHistory(wrong, false)));
            assertFalse(java.nio.file.Files.exists(wrong));
            setText(historyField(f.panel, "PC from"), "0x8002");
            triggerButton(findButton(f.panel, "Apply filters"));
            awaitReady(f.panel);
            java.nio.file.Path text = temporary.getRoot().toPath().resolve("shown.txt");
            runOnEdt(() -> f.panel.saveHistory(text, true));
            awaitReady(f.panel);
            String exported = java.nio.file.Files.readString(text);
            assertTrue(exported.contains("Captured CPU"));
            assertTrue(exported.contains("#51 |"));
            assertFalse(exported.contains("#50 |"));
            assertFalse(exported.contains("Test CPU"));
            assertFalse(exported.contains("#1 |"));
            verifyNoInteractions(f.dialogs);
        }
    }

    @Test(timeout = 10000)
    public void offlineHistoryWorksWithoutCpuDebugSupport() throws Exception {
        java.nio.file.Path file = temporary.getRoot().toPath().resolve("unsupported.emutrace");
        InstructionRecording.write(file,
                new InstructionRecording.Header("Recorded Z80", List.of(new DebugCPU.Register("A", 8))),
                List.of(historyEvent(7, 0x8000, 42, 0x9000)));
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.empty());
        try (DebugSession session = new DebugSession(cpu, null, null)) {
            DebuggerPanel panel = onEdt(() -> createPanel(session, mock(Dialogs.class)));
            try {
                runOnEdt(() -> panel.openRecording(file));
                JButton open = findButton(panel, "Open recording…");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (!onEdt(open::isEnabled) && System.nanoTime() < deadline) Thread.sleep(10);
                assertTrue(onEdt(open::isEnabled));
                JTable history = historyTable(panel);
                assertEquals(1, onEdt(history::getRowCount).intValue());
                assertEquals("7", onEdt(() -> history.getValueAt(0, 0)));
                assertFalse(onEdt(findToggleButtonByTooltip(panel, "Record recent instructions")::isEnabled));
                verify(cpu, never()).setInstructionLocation(anyInt());
            } finally { runOnEdt(panel::close); }
        }
    }

    private final class Fixture implements AutoCloseable {
        final CPU cpu = mock(CPU.class);
        final DebugCPU debug = mock(DebugCPU.class);
        final AtomicInteger byteValue = new AtomicInteger();
        final AtomicBoolean selectiveValue = new AtomicBoolean();
        final DebugSession session;
        final Dialogs dialogs = mock(Dialogs.class);
        final DebuggerPanel panel;

        Fixture() throws Exception { this(temporary.getRoot().toPath().resolve("debugger")); }

        @SuppressWarnings("unchecked")
        Fixture(java.nio.file.Path settings) throws Exception {
            MemoryContext<Byte> memory = mock(MemoryContext.class);
            when(memory.getCellTypeClass()).thenReturn(Byte.class);
            when(memory.getSize()).thenReturn(8);
            when(memory.getBanksCount()).thenReturn(2);
            when(memory.getCommonBoundary()).thenReturn(8);
            when(memory.readBank(anyInt(), anyInt())).thenAnswer(invocation ->
                    (byte) (selectiveValue.get() && (Integer) invocation.getArgument(0) != 3 ? 0 : byteValue.get()));
            doReturn(memory).when(debug).memory();
            when(debug.processor()).thenReturn("Test CPU");
            when(debug.registers()).thenReturn(List.of(new DebugCPU.Register("A", 8)));
            when(cpu.getDebugger()).thenReturn(Optional.of(debug));
            when(cpu.getAddressSpaceSize()).thenReturn(8);
            when(cpu.getTitle()).thenReturn("Test CPU");
            session = new DebugSession(cpu, memory, settings);
            panel = onEdt(() -> createPanel(session, dialogs));
            showInFrame(panel);
            awaitReady(panel);
            runOnEdt(panel::showMemory);
        }

        void refresh() {
            ArgumentCaptor<CPU.CPUListener> listeners = ArgumentCaptor.forClass(CPU.CPUListener.class);
            verify(cpu, atLeastOnce()).addCPUListener(listeners.capture());
            listeners.getAllValues().forEach(CPU.CPUListener::internalStateChanged);
            runOnEdt(() -> {});
        }

        void stopped() {
            ArgumentCaptor<CPU.CPUListener> listeners = ArgumentCaptor.forClass(CPU.CPUListener.class);
            verify(cpu, atLeastOnce()).addCPUListener(listeners.capture());
            listeners.getAllValues().forEach(listener -> listener.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK));
            runOnEdt(() -> {});
        }

        public void close() {
            runOnEdt(panel::close);
            session.close();
        }
    }

}
