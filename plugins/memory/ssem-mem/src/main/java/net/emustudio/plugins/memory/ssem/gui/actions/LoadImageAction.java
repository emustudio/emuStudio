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
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class LoadImageAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/ssem/gui/document-open.png";
    private final ApplicationApi api;
    private final Dialogs dialogs;
    private final MemoryContext<Byte> context;
    private final Runnable repaint;
    private Path recentOpenPath;

    public LoadImageAction(ApplicationApi api, MemoryContext<Byte> context, Runnable repaint) {
        super("Load image file...", new ImageIcon(LoadImageAction.class.getResource(ICON_FILE)));

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
                false, new FileExtensionsFilter("Memory image", "bssem"));

        System.out.println(imagePath);
        imagePath.ifPresent(path -> {
            recentOpenPath = path;
            try {
                try (FileInputStream stream = new FileInputStream(path.toFile())) {
                    ByteBuffer code = ByteBuffer.wrap(stream.readAllBytes());
                    int startLine = code.getInt();
                    byte[] data = new byte[code.remaining()];
                    code.get(data);

                    api.setProgramLocation(startLine * 4);
                    context.write(0, NumberUtils.nativeBytesToBytes(data));
                }
                repaint.run();
            } catch (Exception ex) {
                dialogs.showError("Could not load selected image file: " + ex.getMessage(), "Load image file");

                ex.printStackTrace();
            }
        });
    }
}
