package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;

public class GotoAddressAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/format-indent-more.png";
    private final Dialogs dialogs;
    private final ByteMemoryContext context;
    private final Consumer<Integer> setPageFromAddress;

    public GotoAddressAction(Dialogs dialogs, ByteMemoryContext context, Consumer<Integer> setPageFromAddress) {
        super("Go to address...", new ImageIcon(GotoAddressAction.class.getResource(ICON_FILE)));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.context = Objects.requireNonNull(context);
        this.setPageFromAddress = Objects.requireNonNull(setPageFromAddress);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Go to address...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_G);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            dialogs
                    .readInteger("Enter memory address:", "Go to address")
                    .ifPresent(address -> {
                        if (address < 0 || address >= context.getSize()) {
                            dialogs.showError(
                                    "Address out of bounds (min=0, max=" + (context.getSize() - 1) + ")", "Go to address"
                            );
                        } else {
                            setPageFromAddress.accept(address);
                        }
                    });
        } catch (NumberFormatException ex) {
            dialogs.showError("Invalid number format", "Go to address");
        }
    }
}
