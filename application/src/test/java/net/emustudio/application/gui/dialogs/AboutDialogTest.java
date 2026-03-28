/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AboutDialogTest extends AbstractSwingTest {

    @Test
    public void dialogBuildsLogoAndVersionLabels() {
        AboutDialog dialog = onEdt(() -> {
            AboutDialog result = new AboutDialog(new JFrame(), new GUIImpl());
            result.setModal(false);
            return result;
        });

        showDialog(dialog);

        assertEquals("About emuStudio", onEdt(dialog::getTitle));
        assertNotNull(findLabel(dialog, "emuStudio"));
        assertNotNull(findLabel(dialog, "Version: "));
    }
}
