/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.ui.StandardIcons;
import org.junit.Test;

import javax.swing.*;
import java.awt.Font;
import java.awt.event.ActionEvent;

import static org.junit.Assert.*;

public class GuiComponentPolishTest {
    @Test
    public void toolbarNamesFollowActionAndTooltipChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (boolean toggle : new boolean[]{false, true}) {
                Action action = action("Run");
                AbstractButton button = toggle ? new ToolbarToggleButton(action) : new ToolbarButton(action);
                assertNull(button.getToolTipText());
                assertEquals("Run", button.getAccessibleContext().getAccessibleName());

                action.putValue(Action.NAME, "Resume");
                assertEquals("Resume", button.getAccessibleContext().getAccessibleName());
                action.putValue(Action.SHORT_DESCRIPTION, "Resume emulation");
                assertEquals("Resume emulation", button.getToolTipText());
                assertEquals("Resume emulation", button.getAccessibleContext().getAccessibleName());

                action.putValue(Action.SHORT_DESCRIPTION, null);
                assertNull(button.getToolTipText());
                assertEquals("Resume", button.getAccessibleContext().getAccessibleName());
                button.getAccessibleContext().setAccessibleName("Custom accessible name");
                button.setToolTipText("A different tooltip");
                assertEquals("Custom accessible name", button.getAccessibleContext().getAccessibleName());
            }
        });
    }

    @Test
    public void iconOnlyConsumerButtonsHaveAccessibleNamesAndKeepExplicitOverrides() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EmuStudioGui gui = new EmuStudioGui();
            for (AbstractButton button : new AbstractButton[]{
                    gui.toolbarButton(event -> {}, StandardIcons.ICON_MEMORY, "Inspect memory"),
                    gui.toolbarToggleButton(event -> {}, StandardIcons.ICON_RECORD, "Record instructions")}) {
                assertEquals(button.getToolTipText(), button.getAccessibleContext().getAccessibleName());
                assertFalse(button.isFocusable());
                button.getAccessibleContext().setAccessibleName("Custom control");
                button.setToolTipText("Updated description");
                assertEquals("Custom control", button.getAccessibleContext().getAccessibleName());
            }
        });
    }

    @Test
    public void tableRowsFitLargeFontsWithoutGrowingOnRepeatedStyling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EmuStudioGui gui = new EmuStudioGui();
            JTable table = new JTable(2, 2);
            table.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
            gui.styleTable(table);
            int height = table.getRowHeight();
            assertTrue(height >= table.getFontMetrics(table.getFont()).getHeight() + 4);
            gui.styleTable(table);
            assertEquals(height, table.getRowHeight());

            table.setRowHeight(height + 20);
            gui.styleTable(table);
            assertEquals(height + 20, table.getRowHeight());
        });
    }

    private Action action(String name) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent event) {
            }
        };
    }
}
