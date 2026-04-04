/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.GuiImpl;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BreakpointDialogTest extends AbstractSwingTest {

    @Test
    public void setButtonParsesAddressAndClosesDialog() {
        BreakpointDialog dialog = createDialog(mock(Dialogs.class));

        showDialog(dialog);
        JTextField addressField = findComponent(dialog, JTextField.class, field -> true);
        JButton setButton = findComponent(dialog, JButton.class, button -> "Set".equals(button.getText()));
        setText(addressField, "0x2A");
        triggerButton(setButton);

        assertEquals(42, dialog.getAddress());
        assertTrue(dialog.isSet());
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void unsetButtonParsesAddressAndClosesDialog() {
        BreakpointDialog dialog = createDialog(mock(Dialogs.class));

        showDialog(dialog);
        JTextField addressField = findComponent(dialog, JTextField.class, field -> true);
        JButton unsetButton = findComponent(dialog, JButton.class, button -> "Unset".equals(button.getText()));
        setText(addressField, "21");
        triggerButton(unsetButton);

        assertEquals(21, dialog.getAddress());
        assertFalse(dialog.isSet());
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void invalidAddressShowsErrorAndKeepsDialogOpen() {
        Dialogs dialogs = mock(Dialogs.class);
        BreakpointDialog dialog = createDialog(dialogs);

        showDialog(dialog);
        JTextField addressField = findComponent(dialog, JTextField.class, field -> true);
        JButton setButton = findComponent(dialog, JButton.class, button -> "Set".equals(button.getText()));
        setText(addressField, "invalid");
        triggerButton(setButton);

        verify(dialogs).showError("Invalid address, try again !");
        assertEquals(-1, dialog.getAddress());
        assertFalse(dialog.isSet());
        assertTrue(onEdt(dialog::isDisplayable));
    }

    private BreakpointDialog createDialog(Dialogs dialogs) {
        return onEdt(() -> {
            BreakpointDialog result = new BreakpointDialog(new JFrame(), dialogs, new GuiImpl());
            result.setModal(false);
            return result;
        });
    }
}
