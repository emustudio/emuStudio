/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import org.junit.Test;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class NoDialogsGuiTest {

    @Test
    public void formatMessageAndChooserFallbacksUseExpectedDefaults() {
        DialogsNoGui dialogs = new DialogsNoGui();

        assertEquals("[Title] Message", DialogsNoGui.formatMessage("Title", "Message"));
        assertFalse(dialogs.chooseFile("Open", "Open", true).isPresent());
        assertFalse(dialogs.chooseFile("Open", "Open", true, Collections.emptyList()).isPresent());
        assertFalse(dialogs.chooseFile("Open", "Open", Path.of("."), true).isPresent());
        assertFalse(dialogs.chooseFile("Open", "Open", Path.of("."), true, Collections.emptyList()).isPresent());
        assertFalse(dialogs.chooseDirectory("Dir", "Choose").isPresent());
        assertFalse(dialogs.chooseDirectory("Dir", "Choose", Path.of(".")).isPresent());
    }

    @Test
    public void interactiveMethodsThrowInNoGuiMode() {
        DialogsNoGui dialogs = new DialogsNoGui();

        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.readInteger("value");
            }
        });
        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.readString("value");
            }
        });
        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.readDouble("value");
            }
        });
        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.ask("confirm");
            }
        });

        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.readInteger("value", "Title", 7);
            }
        });
        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.readString("value", "Title", "initial");
            }
        });
        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.readDouble("value", "Title", 1.5);
            }
        });
        expectRuntimeException(new ThrowingRunnable() {
            @Override
            public void run() {
                dialogs.ask("confirm", "Title");
            }
        });
    }

    private void expectRuntimeException(ThrowingRunnable runnable) {
        try {
            runnable.run();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(true, e.getMessage().contains("implemented just for GUI version"));
        }
    }

    private interface ThrowingRunnable {
        void run();
    }
}
