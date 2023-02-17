package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
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
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/document-save.png";
    private final Dialogs dialogs;
    private final ByteMemoryContext context;

    public DumpMemoryAction(Dialogs dialogs, ByteMemoryContext context) {
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
                        for (int i = 0; i < context.getSize(); i++) {
                            out.write(String.format("%X:\t%02X\n", i, context.read(i)));
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
