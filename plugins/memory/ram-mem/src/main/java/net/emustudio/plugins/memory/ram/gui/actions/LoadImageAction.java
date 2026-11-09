/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui.actions;

import net.emustudio.emulib.runtime.ui.StandardIcons;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.io.FileType;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class LoadImageAction extends AbstractAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadImageAction.class);
    private final static String ICON_FILE = StandardIcons.ICON_OPEN_FILE;
    private final Dialogs dialogs;
    private final MemoryContextImpl context;
    private final Runnable repaint;
    private Path recentOpenPath;

    public LoadImageAction(Dialogs dialogs, MemoryContextImpl context, Runnable repaint) {
        super("Load image file...", loadIcon(ICON_FILE, LoadImageAction.class));

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
                false, List.of(new FileType("Memory image", "bram")));
        imagePath.ifPresent(path -> {
            recentOpenPath = path;
            setEnabled(false);
            try {
                context.deserialize(path.toString());
                repaint.run();
            } catch (Exception failure) {
                dialogs.showError("Could not load selected image file: " + failure.getMessage(), "Load image file");
                LOGGER.error("Could not load image file '{}'", path, failure);
            } finally {
                setEnabled(true);
            }
        });
    }
}
