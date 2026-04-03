/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import net.emustudio.plugins.memory.bytemem.gui.SelectBankAddressDialog;
import net.emustudio.plugins.memory.bytemem.loaders.Loader;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;
import static net.emustudio.plugins.memory.bytemem.gui.Constants.IMAGE_EXTENSION_FILTER;

public class LoadImageAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/document-open.png";
    private final Dialogs dialogs;
    private final ByteMemoryContext context;
    private final JDialog parent;
    private final Runnable repaint;
    private final GUI gui;
    private Path recentOpenPath;

    public LoadImageAction(Dialogs dialogs, ByteMemoryContext context, JDialog parent, Runnable repaint, GUI gui) {
        super("Load image file...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.parent = Objects.requireNonNull(parent);
        this.repaint = Objects.requireNonNull(repaint);
        this.gui = gui;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Load image file...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Path currentDirectory = Objects.requireNonNullElse(recentOpenPath, new File(System.getProperty("user.dir")).toPath());
        Optional<Path> imagePath = dialogs.chooseFile(
                "Load image file", "Load", currentDirectory, false, IMAGE_EXTENSION_FILTER);
        imagePath.ifPresent(path -> {
            recentOpenPath = path;
            Loader loader = Loader.createLoader(path);
            Optional<Loader.MemoryBank> bank = askForMemoryBank(!loader.isMemoryAddressAware());
            if (bank.isPresent()) {
                try {
                    loader.load(path, context, bank.get());
                    repaint.run();
                } catch (Exception ex) {
                    dialogs.showError("Could not load selected image file: " + ex.getMessage(), "Load image file");

                    ex.printStackTrace();
                }
            }
        });
    }

    private Optional<Loader.MemoryBank> askForMemoryBank(boolean canSelectAddress) {
        boolean hasMultipleBanks = context.getBanksCount() > 1;
        Loader.MemoryBank bank = Loader.MemoryBank.of(0, 0);

        if (hasMultipleBanks || canSelectAddress) {
            SelectBankAddressDialog dialog = new SelectBankAddressDialog(
                    parent, hasMultipleBanks, canSelectAddress, dialogs, gui);
            dialog.setVisible(true);

            if (dialog.isOk()) {
                return Optional.of(Loader.MemoryBank.of(dialog.getBank(), dialog.getAddress()));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(bank);
    }
}
