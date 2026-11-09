/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.emulib.application.ui.Dialogs;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NoDialogsGuiTest {

    @Test
    public void formatMessageAndChooserFallbacksUseExpectedDefaults() {
        DialogsNoGui dialogs = new DialogsNoGui();

        assertEquals("[Title] Message", DialogsNoGui.formatMessage("Title", "Message"));
        assertFalse(dialogs.chooseFile("Open", "Open", null, true, Collections.emptyList()).isPresent());
        assertFalse(dialogs.chooseFile("Open", "Open", Path.of("."), true, Collections.emptyList()).isPresent());
        assertFalse(dialogs.chooseDirectory("Dir", "Choose", null).isPresent());
        assertFalse(dialogs.chooseDirectory("Dir", "Choose", Path.of(".")).isPresent());
    }

    @Test
    public void interactiveMethodsAreCancelledInNoGuiMode() {
        DialogsNoGui dialogs = new DialogsNoGui();

        assertFalse(dialogs.readInteger("value", "Title", null).isPresent());
        assertFalse(dialogs.readString("value", "Title", "initial").isPresent());
        assertFalse(dialogs.readDouble("value", "Title", 1.5).isPresent());
        assertEquals(Dialogs.Answer.CANCEL, dialogs.ask("confirm", "Title"));
    }
}
