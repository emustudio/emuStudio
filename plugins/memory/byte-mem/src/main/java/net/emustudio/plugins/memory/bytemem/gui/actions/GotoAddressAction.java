/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class GotoAddressAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/format-indent-more.png";
    private final Dialogs dialogs;
    private final ByteMemoryContext context;
    private final Consumer<Integer> setPageFromAddress;

    public GotoAddressAction(Dialogs dialogs, ByteMemoryContext context, Consumer<Integer> setPageFromAddress) {
        super("Go to address...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.setPageFromAddress = Objects.requireNonNull(setPageFromAddress);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Go to address...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_G);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            dialogs
                    .readInteger("Enter memory address:", "Go to address")
                    .ifPresent(address -> {
                        if (address < 0 || address >= context.getSize()) {
                            dialogs.showError(
                                    "Address out of bounds (min=0, max=" + (context.getSize() - 1) + ")", "Go to address"
                            );
                        } else {
                            setPageFromAddress.accept(address);
                        }
                    });
        } catch (NumberFormatException ex) {
            dialogs.showError("Invalid number format", "Go to address");
        }
    }
}
