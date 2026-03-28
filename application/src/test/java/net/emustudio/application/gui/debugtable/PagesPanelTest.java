/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PagesPanelTest {

    @Test
    public void navigationButtonsDelegateToDebugTableModel() {
        DebugTableModel model = mock(DebugTableModel.class);
        Dialogs dialogs = mock(Dialogs.class);
        GUI gui = mock(GUI.class);
        stubGui(gui);

        PagesPanel panel = PagesPanel.create(model, dialogs, gui);
        List<JButton> buttons = collectButtons(panel);

        buttons.get(0).doClick();
        buttons.get(2).doClick();
        buttons.get(3).doClick();
        buttons.get(4).doClick();

        verify(model).firstPage();
        verify(model).previousPage();
        verify(model).currentPage();
        verify(model).nextPage();
    }

    @Test
    public void seekButtonsUseDialogsAndRememberLastValue() {
        DebugTableModel model = mock(DebugTableModel.class);
        Dialogs dialogs = mock(Dialogs.class);
        GUI gui = mock(GUI.class);
        stubGui(gui);
        when(dialogs.readInteger("Please enter number of pages to backward", "Seek", 10)).thenReturn(Optional.of(3));
        when(dialogs.readInteger("Please enter number of pages to forward", "Seek", 3)).thenReturn(Optional.of(5));
        when(dialogs.readInteger("Please enter number of pages to backward", "Seek", 5)).thenReturn(Optional.empty());

        PagesPanel panel = PagesPanel.create(model, dialogs, gui);
        List<JButton> buttons = collectButtons(panel);

        buttons.get(1).doClick();
        buttons.get(5).doClick();
        buttons.get(1).doClick();

        verify(model).seekBackwardPage(3);
        verify(model).seekForwardPage(5);
        verify(dialogs).readInteger("Please enter number of pages to backward", "Seek", 10);
        verify(dialogs).readInteger("Please enter number of pages to forward", "Seek", 3);
        verify(dialogs).readInteger("Please enter number of pages to backward", "Seek", 5);
    }

    private void stubGui(GUI gui) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<ActionEvent> action = invocation.getArgument(0);
            String icon = invocation.getArgument(1);
            String tooltip = invocation.getArgument(2);
            JButton button = new JButton();
            button.setToolTipText(tooltip);
            button.putClientProperty("icon", icon);
            button.addActionListener(event -> action.accept(event));
            return button;
        }).when(gui).toolbarButton(org.mockito.ArgumentMatchers.<Consumer<ActionEvent>>any(), anyString(), anyString());
        when(gui.panel(anyString(), anyString(), anyString())).thenReturn(new JPanel());
    }

    private List<JButton> collectButtons(Container container) {
        List<JButton> buttons = new ArrayList<>();
        collectButtons(container, buttons);
        if (buttons.size() != 6) {
            fail("Expected 6 toolbar buttons but found " + buttons.size());
        }
        return buttons;
    }

    private void collectButtons(Container container, List<JButton> buttons) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                buttons.add((JButton) component);
            }
            if (component instanceof Container) {
                collectButtons((Container) component, buttons);
            }
        }
    }
}
