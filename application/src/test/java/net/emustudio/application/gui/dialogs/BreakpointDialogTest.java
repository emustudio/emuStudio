/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BreakpointDialogTest extends AbstractSwingTest {
    @Test
    public void existingBreakpointFollowsTheAddressAndPreventsMeaninglessUnset() {
        BreakpointDialog dialog = onEdt(() -> new BreakpointDialog(new JFrame(), mock(Dialogs.class),
                new EmuStudioGui(), 7, 256, address -> address == 7, request -> false));
        showDialog(dialog);
        JLabel state = findComponent(dialog, JLabel.class,
                label -> "Existing breakpoint state".equals(label.getAccessibleContext().getAccessibleName()));
        assertTrue(onEdt(findButton(dialog, "Unset")::isEnabled));
        setText(findComponent(dialog, JTextField.class, field -> true), "8");
        assertTrue(onEdt(state::getText).contains("No execution breakpoint"));
        assertFalse(onEdt(findButton(dialog, "Unset")::isEnabled));
    }

    @Test
    public void dialogContainsOnlyBreakpointControls() {
        BreakpointDialog dialog = createDialog(mock(Dialogs.class), request -> false);
        showDialog(dialog);
        runOnEdt(() -> assertNoInspectionControls(dialog));
        assertEquals("Set", onEdt(() -> dialog.getRootPane().getDefaultButton().getText()));
    }

    @Test
    public void setSubmitsCpuAddress() {
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(mock(Dialogs.class),
                request -> { submitted.set(request); return true; });
        showDialog(dialog);
        setText(findComponent(dialog, JTextField.class, field -> true), "0xFF");
        triggerButton(findButton(dialog, "Set"));
        assertEquals(255, submitted.get().address);
        assertEquals(BreakpointDialog.Operation.SET, submitted.get().operation);
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void unsetSubmitsCpuAddress() {
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(mock(Dialogs.class),
                request -> { submitted.set(request); return true; });
        showDialog(dialog);
        setText(findComponent(dialog, JTextField.class, field -> true), "21");
        triggerButton(findButton(dialog, "Unset"));
        assertEquals(21, submitted.get().address);
        assertEquals(BreakpointDialog.Operation.UNSET, submitted.get().operation);
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void invalidAddressKeepsTheDialogAndDoesNotSubmit() {
        Dialogs dialogs = mock(Dialogs.class);
        AtomicReference<BreakpointDialog.Request> submitted = new AtomicReference<>();
        BreakpointDialog dialog = createDialog(dialogs,
                request -> { submitted.set(request); return true; });
        showDialog(dialog);
        JTextField address = findComponent(dialog, JTextField.class, field -> true);
        for (String text : new String[]{"invalid", "-1", "0x100"}) {
            setText(address, text);
            triggerButton(findButton(dialog, "Set"));
            assertEquals(text, onEdt(address::getText));
            assertNull(submitted.get());
            assertTrue(onEdt(dialog::isDisplayable));
        }
        verify(dialogs, times(3)).showError("Invalid address. Enter a value from 0 to 255.");
    }

    @Test
    public void rejectedRequestKeepsDialogOpen() {
        BreakpointDialog dialog = createDialog(mock(Dialogs.class), request -> false);
        showDialog(dialog);
        triggerButton(findButton(dialog, "Set"));
        assertTrue(onEdt(dialog::isDisplayable));
        assertEquals("0x0007", onEdt(findComponent(dialog, JTextField.class, field -> true)::getText));
    }

    private static void assertNoInspectionControls(Component component) {
        assertFalse("Breakpoint dialog must not select memory banks", component instanceof JComboBox);
        if (component instanceof AbstractButton) {
            String text = ((AbstractButton) component).getText();
            assertFalse(text != null && text.contains("Inspect"));
        }
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) assertNoInspectionControls(child);
    }

    private BreakpointDialog createDialog(Dialogs dialogs, Predicate<BreakpointDialog.Request> submit) {
        return onEdt(() -> {
            BreakpointDialog result = new BreakpointDialog(new JFrame(), dialogs, new EmuStudioGui(),
                    7, 256, address -> true, submit);
            result.setModal(false);
            return result;
        });
    }
}
