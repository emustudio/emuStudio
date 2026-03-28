/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class InputDialogTest extends AbstractSwingTest {

    @Test
    public void okButtonStoresInputAndClosesDialog() throws Exception {
        JFrame parent = showFrame(onEdt(JFrame::new));
        InputDialog dialog = createDialog(parent);

        showDialog(dialog);

        JTextField inputField = findComponent(dialog, JTextField.class, field -> true);
        setText(inputField, "updated");
        triggerButton(findButton(dialog, "OK"));

        assertEquals("updated", getResult(dialog));
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void cancelButtonLeavesResultUnset() throws Exception {
        JFrame parent = showFrame(onEdt(JFrame::new));
        InputDialog dialog = createDialog(parent);

        showDialog(dialog);
        triggerButton(findButton(dialog, "Cancel"));

        assertNull(getResult(dialog));
        assertFalse(onEdt(dialog::isDisplayable));
    }

    private InputDialog createDialog(Component parent) throws Exception {
        Constructor<InputDialog> constructor = InputDialog.class.getDeclaredConstructor(
                Component.class, String.class, String.class, Object.class, net.emustudio.emulib.runtime.ui.GUI.class
        );
        constructor.setAccessible(true);

        InputDialog dialog = onEdt(() -> constructor.newInstance(
                parent, "Enter text", "Input", "initial", new GUIImpl()
        ));
        runOnEdt(() -> dialog.setModal(false));
        return dialog;
    }

    private String getResult(InputDialog dialog) throws Exception {
        Field resultField = InputDialog.class.getDeclaredField("result");
        resultField.setAccessible(true);
        return (String) resultField.get(dialog);
    }
}
