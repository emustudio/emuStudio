/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui.actions;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class DumpMemoryAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(DumpMemoryAction.class);
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/rasp/gui/document-save.png";
    private final Dialogs dialogs;
    private final Supplier<Integer> programLocation;
    private final MemoryContextImpl context;

    public DumpMemoryAction(Dialogs dialogs, MemoryContextImpl context, Supplier<Integer> programLocation) {
        super("Dump (save) memory to a file...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.programLocation = Objects.requireNonNull(programLocation);

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
                new FileExtensionsFilter("Binary dump", "brasp"));

        dumpPath.ifPresent(path -> {
            setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws IOException {
                    dump(path);
                    return null;
                }

                @Override
                protected void done() {
                    setEnabled(true);
                    try {
                        get();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        LOGGER.error("Memory dump could not be created", cause);
                        dialogs.showError("Memory dump could not be created: " + cause.getMessage() + ". Please see log file for more details.");
                    }
                }
            }.execute();
        });
    }

    private void dump(Path path) throws IOException {
        if (path.toString().toLowerCase(Locale.ENGLISH).endsWith(".txt")) {
            try (BufferedWriter out = new BufferedWriter(new FileWriter(path.toFile()))) {
                for (int i = 0; i < context.getSize(); i++) {
                    out.write(String.format("%X:\t%02X\n", i, context.read(i)));
                }
            }
        } else {
            RaspMemoryContext.serialize(path, programLocation.get(), context.getSnapshot());
        }
    }
}
