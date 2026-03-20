/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.MaxItemsCache;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;

import javax.swing.*;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * BrowseButton - a button with "Browse..." text, opening a dialog for selecting files or directories.
 */
public class BrowseButton extends JButton {
    private final MaxItemsCache<Path> pathCache = new MaxItemsCache<>(10);

    public BrowseButton(Dialogs dialogs, String dialogTitle, String approveButtonText, Consumer<Path> onApprove) {
        super("Browse...");
        this.addActionListener(e -> dialogs
                .chooseDirectory(dialogTitle, approveButtonText, getCurrentDirectory())
                .ifPresent(path -> {
                    pathCache.put(path);
                    onApprove.accept(path);
                }));
    }

    public BrowseButton(Dialogs dialogs, String dialogTitle, String approveButtonText,
                        boolean appendMissingExtensions,
                        Consumer<Path> onApprove, FileExtensionsFilter... filters) {
        super("Browse...");
        addActionListener(e -> dialogs
                .chooseFile(dialogTitle, approveButtonText, getCurrentDirectory(), appendMissingExtensions, filters)
                .ifPresent(path -> {
                    pathCache.put(path);
                    onApprove.accept(path);
                }));
    }

    private Path getCurrentDirectory() {
        return pathCache.first().orElse(Path.of(System.getProperty("user.dir")));
    }
}

