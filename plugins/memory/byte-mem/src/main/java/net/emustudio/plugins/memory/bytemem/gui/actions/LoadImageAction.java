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
import net.emustudio.plugins.memory.bytemem.gui.SelectBankAddressDialog;
import net.emustudio.plugins.memory.bytemem.loaders.Loader;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.IMAGE_EXTENSION_FILTER;

public class LoadImageAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/document-open.png";
    private final Dialogs dialogs;
    private final ByteMemoryContext context;
    private final JDialog parent;
    private final Runnable repaint;

    public LoadImageAction(Dialogs dialogs, ByteMemoryContext context, JDialog parent, Runnable repaint) {
        super("Load image file...", new ImageIcon(LoadImageAction.class.getResource(ICON_FILE)));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.parent = Objects.requireNonNull(parent);
        this.repaint = Objects.requireNonNull(repaint);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Load image file...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional<Path> imagePath = dialogs.chooseFile(
                "Load image file", "Load", Path.of(System.getProperty("user.dir")),
                false, IMAGE_EXTENSION_FILTER);
        imagePath.ifPresent(path -> {
            Loader loader = Loader.createLoader(path);
            Loader.MemoryBank bank = askForMemoryBank(!loader.isMemoryAddressAware());
            try {
                loader.load(path, context, bank);
                repaint.run();
            } catch (Exception ex) {
                dialogs.showError("Could not load selected image file: " + ex.getMessage(), "Load image file");

                ex.printStackTrace();
            }
        });
    }

    private Loader.MemoryBank askForMemoryBank(boolean canSelectAddress) {
        boolean hasMultipleBanks = context.getBanksCount() > 1;
        Loader.MemoryBank bank = Loader.MemoryBank.of(0, 0);

        if (hasMultipleBanks || canSelectAddress) {
            SelectBankAddressDialog dialog = new SelectBankAddressDialog(
                    parent, hasMultipleBanks, canSelectAddress, dialogs);
            dialog.setVisible(true);

            if (dialog.isOk()) {
                bank = Loader.MemoryBank.of(dialog.getBank(), dialog.getAddress());
            }
        }
        return bank;
    }
}
