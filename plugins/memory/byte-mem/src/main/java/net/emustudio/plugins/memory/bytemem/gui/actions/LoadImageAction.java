/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.ui.StandardIcons;
import net.emustudio.emulib.runtime.ExecutionState;
import java.util.concurrent.CompletionException;
import net.emustudio.emulib.runtime.io.FileLoader;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import net.emustudio.plugins.memory.bytemem.gui.SelectBankAddressDialog;
import net.emustudio.plugins.memory.bytemem.loaders.MemoryImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadImageAction.class);
    private final static String ICON_FILE = StandardIcons.ICON_OPEN_FILE;
    private final Dialogs dialogs;
    private final ByteMemoryContext context;
    private final JDialog parent;
    private final Runnable repaint;
    private final GUI gui;
    private final ExecutionState execution;
    private Path recentOpenPath;

    public LoadImageAction(Dialogs dialogs, ByteMemoryContext context, JDialog parent, Runnable repaint, GUI gui,
                           ExecutionState execution) {
        super("Load image file...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.parent = Objects.requireNonNull(parent);
        this.repaint = Objects.requireNonNull(repaint);
        this.gui = gui;
        this.execution = Objects.requireNonNull(execution);

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
            FileLoader loader = MemoryImageLoader.createLoader(path);
            Optional<MemoryImageLoader.MemoryBank> bank = askForMemoryBank(MemoryImageLoader.requiresLoadAddress(loader));
            if (bank.isPresent()) {
                setEnabled(false);
                execution.mutate(() -> {
                    try { MemoryImageLoader.load(path, context, bank.get()); }
                    catch (Exception failure) { throw new CompletionException(failure); }
                }).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                    if (failure == null) repaint.run();
                    else {
                        dialogs.showError("Could not load selected image file: " + failure.getMessage(), "Load image file");
                        LOGGER.error("Could not load image file '{}'", path, failure);
                    }
                    setEnabled(true);
                }));
            }
        });
    }

    private Optional<MemoryImageLoader.MemoryBank> askForMemoryBank(boolean canSelectAddress) {
        boolean hasMultipleBanks = context.getBanksCount() > 1;
        MemoryImageLoader.MemoryBank bank = MemoryImageLoader.MemoryBank.of(0, 0);

        if (hasMultipleBanks || canSelectAddress) {
            SelectBankAddressDialog dialog = new SelectBankAddressDialog(
                    parent, hasMultipleBanks, canSelectAddress, dialogs, gui);
            dialog.setVisible(true);

            if (dialog.isOk()) {
                return Optional.of(MemoryImageLoader.MemoryBank.of(dialog.getBank(), dialog.getAddress()));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(bank);
    }
}
