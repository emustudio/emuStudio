/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import org.junit.Test;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AutoDialogTest extends AbstractSwingTest {

    @Test
    public void setActionUpdatesStatusAndStopButtonCancelsSession() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AutoDialog dialog = onEdt(() -> new AutoDialog(() -> cancelled.set(true), new EmuStudioGui()));

        showDialog(dialog);
        JButton stopButton = findComponent(dialog, JButton.class, button -> "Stop".equals(button.getText()));
        assertNotNull(stopButton);
        assertFalse(onEdt(stopButton::isEnabled));

        runOnEdt(() -> dialog.setAction("Running diagnostics", true));

        assertNotNull(findLabel(dialog, "Running diagnostics"));
        assertTrue(onEdt(stopButton::isEnabled));
        triggerButton(stopButton);

        assertTrue(cancelled.get());
        assertFalse(onEdt(stopButton::isEnabled));
        assertNotNull(findLabel(dialog, "Stopping emulation..."));
    }
}
