/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import static net.emustudio.application.TestCpu.mockCpu;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.debugger.DebugTableModel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.application.ui.Dialogs;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
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
    public void firstPaintRelayoutsRootPaneAfterNativeGeometryChanges() {
        StudioFrame frame = createFrame(null);
        showFrame(frame);

        runOnEdt(() -> {
            Insets insets = frame.getInsets();
            Rectangle clientBounds = new Rectangle(insets.left, insets.top,
                    frame.getWidth() - insets.left - insets.right,
                    frame.getHeight() - insets.top - insets.bottom);
            frame.getRootPane().setBounds(clientBounds.x + 5, clientBounds.y + 25,
                    clientBounds.width + 10, clientBounds.height + 30);
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();

            try {
                frame.paint(graphics);
            } finally {
                graphics.dispose();
            }

            assertEquals(clientBounds, frame.getRootPane().getBounds());
        });
    }

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
        MemoryAnnotations annotations = new MemoryAnnotations();
        annotations.put(new MemoryAnnotations.Entry(4, 1, 12, 13, "source", "",
                SourceCodePosition.of(2, 0, sourceFile.toString())));
        CPU sessionCpu = mockCpu();
        when(sessionCpu.getAddressSpaceSize()).thenReturn(256);
        when(sessionCpu.getDecoder()).thenReturn(address ->
                net.emustudio.emulib.plugin.cpu.DecodedInstruction.of(address, 1, "NOP", "00"));
        when(net.emustudio.application.TestCpu.context(sessionCpu).getInstructionLocation()).thenReturn(12);
        when(memoryContext.getSize()).thenReturn(256);
        DebuggerSession session = new DebuggerSession(net.emustudio.application.TestCpu.execution(sessionCpu), sessionCpu, memoryContext, null, annotations);

        when(computerConfig.getName()).thenReturn("Demo computer");
        when(computer.getComputerConfig()).thenReturn(computerConfig);
        when(computer.getCompiler()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.of(sessionCpu));
        when(computer.getExecution()).thenReturn(Optional.of(session.execution()));
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());
        when(computer.getDebugger()).thenReturn(Optional.of(session));

        StudioFrame frame = onEdt(() -> new StudioFrame(
                computer, new AppSettings(com.electronwill.nightconfig.core.Config.inMemory(), false, false), mock(Dialogs.class), memoryContext, null, new EmuStudioGui()
        ));
        showFrame(frame);
        JTabbedPane tabs = findComponent(frame.getContentPane(), JTabbedPane.class, pane -> true);
        JTable debugTable = findComponent(frame.getContentPane(), JTable.class, table -> true);
        TextEditorPane textEditor = findComponent(frame.getContentPane(), TextEditorPane.class, editor -> true);

        runOnEdt(() -> {
            tabs.setSelectedIndex(1);
            DebugTableModel model = (DebugTableModel) debugTable.getModel();
            int row = java.util.stream.IntStream.range(0, model.getRowCount())
                    .filter(index -> model.getLocationAt(index) == 12).findFirst().orElseThrow();
            Rectangle cell = debugTable.getCellRect(row, 0, true);
            debugTable.dispatchEvent(new MouseEvent(
                    debugTable, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    cell.x + 5, cell.y + 5, 2, false, MouseEvent.BUTTON1
            ));
        });

        assertEquals(0, onEdt(tabs::getSelectedIndex).intValue());
        assertEquals("Source code editor", onEdt(() -> tabs.getTitleAt(0)));
        assertEquals("emuStudio [Demo computer] — linked.asm", onEdt(frame::getTitle));
        assertEquals(onEdt(() -> textEditor.getLineStartOffset(1)), onEdt(textEditor::getCaretPosition));
        session.execution().close();
        session.close();
    }

    private StudioFrame createFrame(Path fileName) {
        return createFrame(fileName, null);
    }

    @Test
    public void enteringEmulatorSelectsDebuggerTab() throws Exception {
        CPU cpu = mockCpu();
        try (DebuggerSession session = new DebuggerSession(net.emustudio.application.TestCpu.execution(cpu), cpu, null, null);
             net.emustudio.application.emulation.EmulationController machineExecution = session.execution()) {
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
        if (session != null) {
            when(computer.getCPU()).thenReturn(Optional.of(session.cpu));
            when(computer.getExecution()).thenReturn(Optional.of(session.execution()));
        }

        return onEdt(() -> new StudioFrame(
                computer,
                new AppSettings(com.electronwill.nightconfig.core.Config.inMemory(), false, false),
                mock(Dialogs.class),
                null,
                fileName,
                new EmuStudioGui()
        ));
    }

}
