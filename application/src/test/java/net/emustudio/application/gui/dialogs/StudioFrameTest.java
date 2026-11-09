/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.debugger.DebuggerSession;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class StudioFrameTest extends AbstractSwingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void frameBuildsTitleTabsAndMenus() {
        StudioFrame frame = createFrame(null);

        showFrame(frame);

        JTabbedPane tabs = findComponent(frame.getContentPane(), JTabbedPane.class, pane -> true);
        JMenuBar menuBar = onEdt(frame::getJMenuBar);

        assertEquals("emuStudio [Demo computer]", onEdt(frame::getTitle));
        assertEquals("Source code editor", onEdt(() -> tabs.getTitleAt(0)));
        assertEquals("Emulator", onEdt(() -> tabs.getTitleAt(1)));
        assertEquals("File", onEdt(() -> menuBar.getMenu(0).getText()));
        assertEquals("Edit", onEdt(() -> menuBar.getMenu(1).getText()));
        assertEquals("Project", onEdt(() -> menuBar.getMenu(2).getText()));
        assertEquals("Help", onEdt(() -> menuBar.getMenu(3).getText()));
        assertEquals(4, onEdt(menuBar::getMenuCount).intValue());
        List<String> editingItems = onEdt(() -> {
            List<String> labels = new java.util.ArrayList<>();
            for (java.awt.Component item : menuBar.getMenu(1).getMenuComponents()) {
                if (item instanceof JMenuItem) labels.add(((JMenuItem) item).getText());
            }
            return labels;
        });
        org.junit.Assert.assertTrue(editingItems.containsAll(List.of("Text", "Selection", "Navigate", "Editing macros",
                "Editor display", "Refactor", "Split view")));
        org.junit.Assert.assertFalse(editingItems.contains("Find / replace in files…"));
    }

    @Test
    public void openedFileUpdatesWindowTitleAndKeepsWorkspaceTabStable() throws IOException {
        Path sourceFile = temporaryFolder.newFile("program.asm").toPath();
        Files.writeString(sourceFile, "NOP");

        StudioFrame frame = createFrame(sourceFile);

        showFrame(frame);

        JTabbedPane tabs = findComponent(frame.getContentPane(), JTabbedPane.class, pane -> true);
        assertEquals("Source code editor", onEdt(() -> tabs.getTitleAt(0)));
        assertEquals("emuStudio [Demo computer] — program.asm", onEdt(frame::getTitle));
    }

    @Test
    public void doubleClickingDebugRowOpensAnnotatedSourcePosition() throws Exception {
        Path sourceFile = temporaryFolder.newFile("linked.asm").toPath();
        Files.writeString(sourceFile, "NOP\nHLT\n");
        VirtualComputer computer = mock(VirtualComputer.class);
        ComputerConfig computerConfig = mock(ComputerConfig.class);
        MemoryContext<?> memoryContext = mock(MemoryContext.class);
        DebugTableModel model = createDebugTableModel();
        MemoryAnnotations annotations = new MemoryAnnotations();
        annotations.put(new MemoryAnnotations.Entry(4, 1, 12, 13, "source", "",
                SourceCodePosition.of(2, 0, sourceFile.toString())));
        CPU sessionCpu = mock(CPU.class);
        when(sessionCpu.getAddressSpaceSize()).thenReturn(256);
        when(memoryContext.getSize()).thenReturn(256);
        DebuggerSession session = new DebuggerSession(sessionCpu, memoryContext, null, annotations);

        when(computerConfig.getName()).thenReturn("Demo computer");
        when(computer.getComputerConfig()).thenReturn(computerConfig);
        when(computer.getCompiler()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.empty());
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(computer.getDebugger()).thenReturn(Optional.of(session));
        when(model.getLocationAt(0)).thenReturn(12);

        StudioFrame frame = onEdt(() -> new StudioFrame(
                computer, new AppSettings(com.electronwill.nightconfig.core.Config.inMemory(), false, false), mock(Dialogs.class), model, memoryContext, null, new EmuStudioGui()
        ));
        showFrame(frame);
        JTabbedPane tabs = findComponent(frame.getContentPane(), JTabbedPane.class, pane -> true);
        JTable debugTable = findComponent(frame.getContentPane(), JTable.class, table -> true);
        TextEditorPane textEditor = findComponent(frame.getContentPane(), TextEditorPane.class, editor -> true);

        runOnEdt(() -> {
            tabs.setSelectedIndex(1);
            debugTable.dispatchEvent(new MouseEvent(
                    debugTable, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    5, 5, 2, false, MouseEvent.BUTTON1
            ));
        });

        assertEquals(0, onEdt(tabs::getSelectedIndex).intValue());
        assertEquals("Source code editor", onEdt(() -> tabs.getTitleAt(0)));
        assertEquals("emuStudio [Demo computer] — linked.asm", onEdt(frame::getTitle));
        assertEquals(onEdt(() -> textEditor.getLineStartOffset(1)), onEdt(textEditor::getCaretPosition));
        session.close();
    }

    private StudioFrame createFrame(Path fileName) {
        return createFrame(fileName, null);
    }

    @Test
    public void enteringEmulatorSelectsDebuggerTab() throws Exception {
        CPU cpu = mock(CPU.class);
        try (DebuggerSession session = new DebuggerSession(cpu, null, null)) {
            StudioFrame frame = createFrame(null, session);
            showFrame(frame);
            JTabbedPane workspace = findComponent(frame.getContentPane(), JTabbedPane.class, pane -> true);
            JTabbedPane views = findComponent(frame.getContentPane(), JTabbedPane.class,
                    pane -> "debuggerViews".equals(pane.getName()));
            runOnEdt(() -> {
                views.setSelectedIndex(views.indexOfTab("Inspection"));
                workspace.setSelectedIndex(1);
            });
            assertEquals("Debugger", onEdt(() -> views.getTitleAt(views.getSelectedIndex())));
            runOnEdt(() -> {
                views.setSelectedIndex(views.indexOfTab("Inspection"));
                workspace.setSelectedIndex(0);
                workspace.setSelectedIndex(1);
            });
            assertEquals("Debugger", onEdt(() -> views.getTitleAt(views.getSelectedIndex())));
        }
    }

    private StudioFrame createFrame(Path fileName, DebuggerSession session) {
        VirtualComputer computer = mock(VirtualComputer.class);
        ComputerConfig computerConfig = mock(ComputerConfig.class);

        when(computerConfig.getName()).thenReturn("Demo computer");
        when(computer.getComputerConfig()).thenReturn(computerConfig);
        when(computer.getCompiler()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.empty());
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(computer.getDebugger()).thenReturn(Optional.ofNullable(session));

        return onEdt(() -> new StudioFrame(
                computer,
                new AppSettings(com.electronwill.nightconfig.core.Config.inMemory(), false, false),
                mock(Dialogs.class),
                createDebugTableModel(),
                null,
                fileName,
                new EmuStudioGui()
        ));
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
        when(model.getRowCount()).thenReturn(1);
        when(model.getValueAt(anyInt(), eq(0))).thenReturn("00");
        when(model.getColumnAt(0)).thenReturn((DebuggerColumn) column);

        return model;
    }
}
