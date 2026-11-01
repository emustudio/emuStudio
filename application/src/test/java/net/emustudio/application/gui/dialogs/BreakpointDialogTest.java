/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Test;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BreakpointDialogTest extends AbstractSwingTest {
    @Test
    public void setButtonSubmitsParsedAddressBankAndKeepInspection() {
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(mock(Dialogs.class), true, request -> { submitted.set(request); return true; });
        showDialog(dialog);
        JComboBox<?> bank = findComponent(dialog, JComboBox.class,
                field -> "Bank".equals(field.getAccessibleContext().getAccessibleName()));
        assertEquals(1, onEdt(bank::getSelectedItem));
        runOnEdt(() -> bank.setSelectedIndex(0));
        setText(findComponent(dialog, JTextField.class, field -> true), "0x2A");
        triggerButton(findComponent(dialog, JCheckBox.class, box -> true));
        triggerButton(findButton(dialog, "Set"));

        assertEquals(42, submitted.get().address);
        assertEquals(0, submitted.get().bank);
        assertEquals(BreakpointDialog.Operation.SET, submitted.get().operation);
        assertTrue(submitted.get().keepInspection);
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void unsetButtonDefaultsToBreakpointOnly() {
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(mock(Dialogs.class), true, request -> { submitted.set(request); return true; });
        showDialog(dialog);
        setText(findComponent(dialog, JTextField.class, field -> true), "21");
        triggerButton(findButton(dialog, "Unset"));

        assertEquals(21, submitted.get().address);
        assertEquals(1, submitted.get().bank);
        assertEquals(BreakpointDialog.Operation.UNSET, submitted.get().operation);
        assertFalse(submitted.get().keepInspection);
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void inspectOnlyWorksWithoutBreakpointSupport() {
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(mock(Dialogs.class), false, request -> { submitted.set(request); return true; });
        showDialog(dialog);
        assertFalse(onEdt(findButton(dialog, "Set")::isEnabled));
        assertFalse(onEdt(findButton(dialog, "Unset")::isEnabled));
        assertEquals("Inspect only", onEdt(() -> dialog.getRootPane().getDefaultButton().getText()));
        triggerButton(findButton(dialog, "Inspect only"));

        assertEquals(BreakpointDialog.Operation.INSPECT_ONLY, submitted.get().operation);
        assertEquals(7, submitted.get().address);
        assertEquals(1, submitted.get().bank);
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void singleBankIsFixedToZero() {
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = onEdt(() -> new BreakpointDialog(new JFrame(), mock(Dialogs.class),
                new EmuStudioGui(), 7, 0, 1, 256, 128, true,
                request -> { submitted.set(request); return true; }));
        showDialog(dialog);
        JComboBox<?> bank = findComponent(dialog, JComboBox.class, field -> true);
        assertFalse(onEdt(bank::isEnabled));
        assertEquals(0, onEdt(bank::getSelectedItem));
        triggerButton(findButton(dialog, "Set"));
        assertEquals(0, submitted.get().bank);
    }

    @Test
    public void invalidOrOutOfRangeAddressKeepsDraftAndDoesNotSubmit() {
        Dialogs dialogs = mock(Dialogs.class);
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(dialogs, true, request -> { submitted.set(request); return true; });
        showDialog(dialog);
        JTextField address = findComponent(dialog, JTextField.class, field -> true);
        for (String text : new String[]{"invalid", "-1", "0x100"}) {
            setText(address, text);
            triggerButton(findButton(dialog, "Set"));
            assertEquals(text, onEdt(address::getText));
            assertNull(submitted.get());
            assertTrue(onEdt(dialog::isDisplayable));
        }
        verify(dialogs, times(3)).showError("Invalid address. Enter a value from 0 to 127.");
        setText(address, "0x80");
        triggerButton(findButton(dialog, "Inspect only"));
        assertNull(submitted.get());
        verify(dialogs, times(4)).showError("Invalid address. Enter a value from 0 to 127.");
    }

    @Test
    public void rejectedRequestKeepsDialogOpen() {
        BreakpointDialog dialog = createDialog(mock(Dialogs.class), true, request -> false);
        showDialog(dialog);
        triggerButton(findButton(dialog, "Set"));
        assertTrue(onEdt(dialog::isDisplayable));
        assertEquals("0x0007", onEdt(findComponent(dialog, JTextField.class, field -> true)::getText));
    }

    private BreakpointDialog createDialog(Dialogs dialogs, boolean supported, Predicate<BreakpointDialog.Request> submit) {
        return onEdt(() -> {
            BreakpointDialog result = new BreakpointDialog(new JFrame(), dialogs, new EmuStudioGui(), 7, 1, 2, 256, 128, supported, submit);
            result.setModal(false);
            return result;
        });
    }
}
