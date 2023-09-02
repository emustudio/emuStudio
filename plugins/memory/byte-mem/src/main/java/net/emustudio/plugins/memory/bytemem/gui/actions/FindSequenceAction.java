package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.bytemem.gui.FindSequenceDialog;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.loadIcon;

public class FindSequenceAction extends AbstractAction {
    private final static String ICON_FILE = "/net/emustudio/plugins/memory/bytemem/gui/edit-find.png";
    private final Dialogs dialogs;
    private final Consumer<Integer> setPageFromAddress;
    private final MemoryTableModel tableModel;
    private final Supplier<Integer> getCurrentAddress;
    private final JDialog parent;

    public FindSequenceAction(Dialogs dialogs, Consumer<Integer> setPageFromAddress, MemoryTableModel tableModel,
                              Supplier<Integer> getCurrentAddress, JDialog parent) {
        super("Find sequence...", loadIcon(ICON_FILE));

        this.dialogs = Objects.requireNonNull(dialogs);
        this.setPageFromAddress = Objects.requireNonNull(setPageFromAddress);
        this.tableModel = Objects.requireNonNull(tableModel);
        this.getCurrentAddress = Objects.requireNonNull(getCurrentAddress);
        this.parent = Objects.requireNonNull(parent);

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        putValue(SHORT_DESCRIPTION, "Find sequence...");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AtomicInteger foundAddress = new AtomicInteger(-1);
        FindSequenceDialog dialog = new FindSequenceDialog(
                dialogs, parent, tableModel, getCurrentAddress.get(), foundAddress::set
        );

        dialog.setVisible(true);

        int address = foundAddress.get();
        if (address != -1) {
            setPageFromAddress.accept(address);
        }
    }
}
