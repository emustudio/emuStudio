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
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class DumpMemoryAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(DumpMemoryAction.class);
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/ssem/gui/document-save.png";
    private final Dialogs dialogs;
    private final MemoryContext<Byte> context;

    public DumpMemoryAction(Dialogs dialogs, MemoryContext<Byte> context) {
        super("Dump (save) memory to a file...", new ImageIcon(DumpMemoryAction.class.getResource(ICON_FILE)));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Dump (save) memory to a file...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Path currentDirectory = Path.of(System.getProperty("user.dir"));
        Optional<Path> dumpPath = dialogs.chooseFile(
                "Dump memory content into a file", "Save", currentDirectory, true,
                new FileExtensionsFilter("Human-readable dump", "txt"),
                new FileExtensionsFilter("Binary dump", "bin"));

        dumpPath.ifPresent(path -> {
            try {
                if (path.toString().toLowerCase(Locale.ENGLISH).endsWith(".txt")) {
                    try (BufferedWriter out = new BufferedWriter(new FileWriter(path.toFile()))) {
                        for (int i = 0; i < 32; i++) {
                            Byte[] v = context.read(i * 4, 4);
                            out.write(String.format("0x%02X, 0x%02X, 0x%02X, 0x%02X\n", v[0], v[1], v[2], v[3]));
                        }
                    }
                } else {
                    try (DataOutputStream ds = new DataOutputStream(new FileOutputStream(path.toFile()))) {
                        for (int i = 0; i < context.getSize(); i++) {
                            ds.writeByte(context.read(i) & 0xff);
                        }
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Memory dump could not be created", ex);
                dialogs.showError("Memory dump could not be created: " + ex.getMessage() + ". Please see log file for more details.");
            }
        });
    }
}
