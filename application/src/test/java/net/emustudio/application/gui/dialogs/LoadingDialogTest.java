/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.GuiImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LoadingDialogTest extends AbstractSwingTest {

    @Test
    public void dialogShowsLoadingInstructions() {
        LoadingDialog dialog = onEdt(() -> new LoadingDialog(new GuiImpl()));

        showDialog(dialog);

        assertEquals("emuStudio", onEdt(dialog::getTitle));
        assertNotNull(findLabel(dialog, "Loading computer, please wait..."));
        assertNotNull(findLabel(dialog, "If you see some errors, please see the log file."));
    }
}
