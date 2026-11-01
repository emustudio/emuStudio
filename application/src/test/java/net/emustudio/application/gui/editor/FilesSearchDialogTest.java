/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class FilesSearchDialogTest extends AbstractSwingTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test(timeout = 10000)
    public void folderOptionsAppearOnlyForFolderSearch() throws Exception {
        TabbedEditor editor = editor("nop\n");
        FilesSearchDialog dialog = onEdt(() -> new FilesSearchDialog(editor, editor.searchBar()));
        showDialog(dialog);
        runOnEdt(() -> dialog.setVisible(true));
        JComboBox<?> scope = findComponent(dialog, JComboBox.class,
                component -> "Scope".equals(component.getAccessibleContext().getAccessibleName()));
        JTextField folder = findComponent(dialog, JTextField.class,
                field -> "Search folder".equals(field.getAccessibleContext().getAccessibleName()));
        assertFalse(onEdt(folder::isShowing));
        runOnEdt(() -> scope.setSelectedItem("Folder"));
        assertTrue(onEdt(folder::isShowing));
        runOnEdt(() -> scope.setSelectedItem("Open files"));
        assertFalse(onEdt(folder::isShowing));
    }

    @Test(timeout = 10000)
    public void previewShowsLineContextWhileApplyingOnlyReviewedMatchRanges() throws Exception {
        String text = "    sta counter\ncounter: db 0\n";
        TabbedEditor editor = editor(text);
        InlineSearchBar search = onEdt(editor::searchBar);
        runOnEdt(() -> { search.find.setText("counter"); search.replace.setText("total"); });
        FilesSearchDialog dialog = onEdt(() -> new FilesSearchDialog(editor, search));
        showDialog(dialog);
        runOnEdt(() -> dialog.setVisible(true));
        triggerButton(findButton(dialog, "Search"));
        JButton find = findButton(dialog, "Search");
        long deadline = System.currentTimeMillis() + 5000;
        while (!onEdt(find::isEnabled) && System.currentTimeMillis() < deadline) Thread.sleep(10);
        assertTrue(onEdt(find::isEnabled));
        JTable table = findComponent(dialog, JTable.class, component -> true);
        assertEquals(2, onEdt(table::getRowCount).intValue());
        assertEquals("    sta counter", onEdt(() -> table.getValueAt(0, 3)));
        assertEquals("    sta total", onEdt(() -> table.getValueAt(0, 4)));
        assertTrue(onEdt(() -> table.getValueAt(0, 1).toString()).startsWith("program.asm — "));
        String fileTip = onEdt(() -> ((JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1)).getToolTipText());
        assertEquals(onEdt(() -> editor.getCurrentFile().orElseThrow().getAbsolutePath()), fileTip);
        triggerButton(findButton(dialog, "Apply checked replacements"));
        assertEquals("    sta total\ntotal: db 0\n", onEdt(() -> editor.activeEditor().textPane().getText()));
        assertEquals(text, Files.readString(onEdt(() -> editor.getCurrentFile().orElseThrow().toPath())));
        runOnEdt(() -> editor.activeEditor().textPane().undoLastAction());
        assertEquals(text, onEdt(() -> editor.activeEditor().textPane().getText()));
    }

    private TabbedEditor editor(String source) throws Exception {
        Path path = Files.writeString(files.newFile("program.asm").toPath(), source);
        return onEdt(() -> new TabbedEditor(mock(Dialogs.class), null, List.of(path), paths -> {}));
    }
}
