/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.ui.StandardIcons;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.NumberUtils;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.io.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static net.emustudio.emulib.application.ui.GUI.loadIcon;

public class LoadImageAction extends AbstractAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadImageAction.class);
    private final static String ICON_FILE = StandardIcons.ICON_OPEN_FILE;
    private final ApplicationApi api;
    private final Dialogs dialogs;
    private final MemoryContext<Byte> context;
    private final Runnable repaint;
    private Path recentOpenPath;

    public LoadImageAction(ApplicationApi api, MemoryContext<Byte> context, Runnable repaint) {
        super("Load image file...", loadIcon(ICON_FILE, LoadImageAction.class));

        this.api = Objects.requireNonNull(api);
        this.dialogs = Objects.requireNonNull(api.getDialogs());
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
                false, List.of(new FileType("Memory image", "bssem")));

        imagePath.ifPresent(path -> {
            recentOpenPath = path;
            setEnabled(false);
            api.getExecutionState().update(() -> {
                try (FileInputStream stream = new FileInputStream(path.toFile())) {
                    ByteBuffer code = ByteBuffer.wrap(stream.readAllBytes());
                    int startLine = code.getInt();
                    byte[] data = new byte[code.remaining()];
                    code.get(data);
                    api.setProgramLocation(startLine * 4);
                    context.write(0, NumberUtils.nativeBytesToBytes(data));
                } catch (Exception failure) { throw new CompletionException(failure); }
            }).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                if (failure == null) repaint.run();
                else {
                    dialogs.showError("Could not load selected image file: " + failure.getMessage(), "Load image file");
                    LOGGER.error("Could not load image file '{}'", path, failure);
                }
                setEnabled(true);
            }));
        });
    }
}
