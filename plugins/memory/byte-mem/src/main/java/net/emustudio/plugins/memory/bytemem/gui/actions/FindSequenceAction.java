package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.bytemem.gui.model.MemoryTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindSequenceAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(FindSequenceAction.class);
    public final static String ERROR_NUMBER_FORMAT = "Cannot parse sequence of bytes. Bytes (in correct radix format) must be separated with spaces.";

    private final Dialogs dialogs;
    private final Runnable dispose;
    private final MemoryTableModel tableModel;

    private final Consumer<Integer> setFoundAddress;

    private final Supplier<Boolean> isCurrentPage;
    private final Supplier<Boolean> isPlainText;
    private final int currentAddress;
    private final JTextComponent txtPosition;
    private final JTextComponent txtFindText;

    private final RadixUtils radixUtils = RadixUtils.getInstance();

    public FindSequenceAction(Dialogs dialogs, Runnable dispose,
                              MemoryTableModel tableModel,
                              Consumer<Integer> setFoundAddress,
                              Supplier<Boolean> isCurrentPage,
                              Supplier<Boolean> isPlainText,
                              int currentAddress,
                              JTextComponent txtPosition,
                              JTextComponent txtFindText) {
        super("Find");

        this.dialogs = Objects.requireNonNull(dialogs);
        this.dispose = Objects.requireNonNull(dispose);
        this.tableModel = Objects.requireNonNull(tableModel);

        this.setFoundAddress = Objects.requireNonNull(setFoundAddress);

        this.currentAddress = currentAddress;
        this.isCurrentPage = Objects.requireNonNull(isCurrentPage);
        this.isPlainText = Objects.requireNonNull(isPlainText);
        this.txtPosition = Objects.requireNonNull(txtPosition);
        this.txtFindText = Objects.requireNonNull(txtFindText);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LastError lastError = new LastError();

        try {
            lastError.component = txtPosition;
            lastError.message = ERROR_NUMBER_FORMAT;
            int from = isCurrentPage.get() ? currentAddress : radixUtils.parseRadix(txtPosition.getText().trim());

            lastError.component = txtFindText;
            lastError.message = "Sequence cannot be empty";
            String text = txtFindText.getText();
            if (text.isEmpty()) {
                throw new Exception();
            }

            final byte[] sequenceToFind;
            if (isPlainText.get()) {
                sequenceToFind = text.getBytes();
            } else {
                lastError.message = ERROR_NUMBER_FORMAT;

                List<Integer> mapped = Stream.of(text.split(" ")).map(radixUtils::parseRadix).collect(Collectors.toList());
                sequenceToFind = new byte[mapped.size()];
                for (int i = 0; i < sequenceToFind.length; i++) {
                    sequenceToFind[i] = mapped.get(i).byteValue(); // NOTE: if integer is > than byte, the rest is cut off.
                }
            }

            tableModel.findSequence(sequenceToFind, from).ifPresent(setFoundAddress);
            dispose.run();
        } catch (Exception ex) {
            LOGGER.debug(lastError.message, ex);
            dialogs.showError(lastError.message, "Find sequence");

            lastError.component.selectAll();
            lastError.component.requestFocus();
        }
    }

    private final static class LastError {
        JTextComponent component;
        String message;
    }
}
