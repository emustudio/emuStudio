/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui.actions;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class LoadImageAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/ram/gui/document-open.png";
    private final Dialogs dialogs;
    private final MemoryContextImpl context;
    private final Runnable repaint;
    private Path recentOpenPath;

    public LoadImageAction(Dialogs dialogs, MemoryContextImpl context, Runnable repaint) {
        super("Load image file...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.repaint = Objects.requireNonNull(repaint);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Load image file...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Path currentDirectory = Objects.requireNonNullElse(recentOpenPath, new File(System.getProperty("user.dir")).toPath());
        Optional<Path> imagePath = dialogs.chooseFile(
                "Load image file", "Load", currentDirectory,
                false, new FileExtensionsFilter("Memory image", "bram"));
        imagePath.ifPresent(path -> {
            recentOpenPath = path;
            try {
                context.deserialize(path.toString());
                repaint.run();
            } catch (Exception ex) {
                dialogs.showError("Could not load selected image file: " + ex.getMessage(), "Load image file");

                ex.printStackTrace();
            }
        });
    }
}
