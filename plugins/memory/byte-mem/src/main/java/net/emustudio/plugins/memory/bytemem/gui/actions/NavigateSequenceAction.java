/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public final class NavigateSequenceAction extends AbstractAction {
    private static final String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/edit-find.png";
    private final Runnable navigate;

    public NavigateSequenceAction(MemorySearch search, boolean previous) {
        super(previous ? "Find previous" : "Find next", loadIcon(ICON_FILE));
        Objects.requireNonNull(search);
        navigate = previous ? search::previous : search::next;
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_F3, previous ? InputEvent.SHIFT_DOWN_MASK : 0));
        putValue(SHORT_DESCRIPTION, previous ? "Find previous (Shift+F3)" : "Find next (F3)");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        navigate.run();
    }
}
