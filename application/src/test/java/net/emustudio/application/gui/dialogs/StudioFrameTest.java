/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
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
        StudioFrame frame = createFrame(Optional.empty());

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
    }

    @Test
    public void constructorUpdatesEditorTabTitleFromOpenedFile() throws IOException {
        Path sourceFile = temporaryFolder.newFile("program.asm").toPath();
        Files.writeString(sourceFile, "NOP");

        StudioFrame frame = createFrame(Optional.of(sourceFile));

        showFrame(frame);

        JTabbedPane tabs = findComponent(frame.getContentPane(), JTabbedPane.class, pane -> true);
        assertEquals("program.asm", onEdt(() -> tabs.getTitleAt(0)));
    }

    private StudioFrame createFrame(Optional<Path> fileName) {
        VirtualComputer computer = mock(VirtualComputer.class);
        ComputerConfig computerConfig = mock(ComputerConfig.class);

        when(computerConfig.getName()).thenReturn("Demo computer");
        when(computer.getComputerConfig()).thenReturn(computerConfig);
        when(computer.getCompiler()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.empty());
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of());

        return onEdt(() -> new StudioFrame(
                computer,
                mock(AppSettings.class),
                mock(Dialogs.class),
                createDebugTableModel(),
                null,
                fileName,
                new GUIImpl()
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
