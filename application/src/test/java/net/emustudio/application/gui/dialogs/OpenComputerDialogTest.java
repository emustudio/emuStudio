/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Test;

import javax.swing.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class OpenComputerDialogTest extends AbstractSwingTest {

    @Test
    public void openButtonRequiresSelectedComputer() {
        Dialogs dialogs = mock(Dialogs.class);
        Consumer<ComputerConfig> selectComputer = mock(Consumer.class);
        OpenComputerDialog dialog = createDialog(dialogs, selectComputer);

        showDialog(dialog);

        JList<?> computerList = findComponent(dialog, JList.class, list -> true);
        runOnEdt(computerList::clearSelection);
        triggerButton(findButton(dialog, "Open computer"));

        verify(dialogs).showError("A computer has to be selected!", "Open computer");
        verifyNoInteractions(selectComputer);
    }

    @Test
    public void exitButtonClosesDialog() {
        OpenComputerDialog dialog = createDialog(mock(Dialogs.class), mock(Consumer.class));

        showDialog(dialog);
        triggerButton(findButton(dialog, "Exit"));

        assertFalse(onEdt(dialog::isDisplayable));
    }

    private OpenComputerDialog createDialog(Dialogs dialogs, Consumer<ComputerConfig> selectComputer) {
        AppSettings appSettings = new AppSettings(Config.inMemory(), false, false);
        return onEdt(() -> {
            OpenComputerDialog dialog = new OpenComputerDialog(appSettings, dialogs, selectComputer, new GUIImpl());
            dialog.setModal(false);
            return dialog;
        });
    }
}
