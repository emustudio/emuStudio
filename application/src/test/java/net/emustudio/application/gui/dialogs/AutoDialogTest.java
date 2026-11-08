/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.application.emulation.EmulationController;
import org.junit.Test;

import javax.swing.*;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AutoDialogTest extends AbstractSwingTest {

    @Test
    public void setActionUpdatesStatusAndStopButtonStopsCpu() {
        VirtualComputer computer = mock(VirtualComputer.class);
        EmulationController execution = mock(EmulationController.class);
        when(computer.getExecution()).thenReturn(Optional.of(execution));

        AutoDialog dialog = onEdt(() -> new AutoDialog(computer, new EmuStudioGui()));

        showDialog(dialog);
        JButton stopButton = findComponent(dialog, JButton.class, button -> "Stop".equals(button.getText()));
        assertNotNull(stopButton);
        assertFalse(onEdt(stopButton::isEnabled));

        runOnEdt(() -> dialog.setAction("Running diagnostics", true));

        assertNotNull(findLabel(dialog, "Running diagnostics"));
        assertTrue(onEdt(stopButton::isEnabled));
        triggerButton(stopButton);

        verify(execution).stop();
    }
}
