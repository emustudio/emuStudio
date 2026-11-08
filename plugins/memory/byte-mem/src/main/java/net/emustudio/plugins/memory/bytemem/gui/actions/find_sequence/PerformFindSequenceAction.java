/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformFindSequenceAction extends AbstractAction {
    public enum Format { TEXT, BYTES, INT16, INT32, INT64, FLOAT32, FLOAT64 }
    public final static String ERROR_NUMBER_FORMAT = "Cannot parse sequence of bytes. Bytes (in correct radix format) must be separated with spaces.";
    private final static Logger LOGGER = LoggerFactory.getLogger(PerformFindSequenceAction.class);
    private final Dialogs dialogs;
    private final BiConsumer<byte[], Integer> startSearch;

    private final Supplier<Boolean> isCurrentPage;
    private final Supplier<Format> format;
    private final Supplier<ByteOrder> byteOrder;
    private final int currentAddress;
    private final JTextComponent txtPosition;
    private final JTextComponent txtFindText;


    public PerformFindSequenceAction(Dialogs dialogs, BiConsumer<byte[], Integer> startSearch,
                                     Supplier<Boolean> isCurrentPage,
                                     Supplier<Format> format, Supplier<ByteOrder> byteOrder,
                                     int currentAddress,
                                     JTextComponent txtPosition,
                                     JTextComponent txtFindText) {

        this.dialogs = Objects.requireNonNull(dialogs);
        this.startSearch = Objects.requireNonNull(startSearch);

        this.currentAddress = currentAddress;
        this.isCurrentPage = Objects.requireNonNull(isCurrentPage);
        this.format = Objects.requireNonNull(format);
        this.byteOrder = Objects.requireNonNull(byteOrder);
        this.txtPosition = Objects.requireNonNull(txtPosition);
        this.txtFindText = Objects.requireNonNull(txtFindText);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LastError lastError = new LastError();

        try {
            lastError.component = txtPosition;
            lastError.message = ERROR_NUMBER_FORMAT;
            int from = isCurrentPage.get() ? currentAddress : RadixUtils.parseRadix(txtPosition.getText().trim());

            lastError.component = txtFindText;
            lastError.message = "Sequence cannot be empty";
            String text = txtFindText.getText();
            if (text.isEmpty()) {
                throw new Exception();
            }

            final byte[] sequenceToFind;
            Format selectedFormat = format.get();
            if (selectedFormat == Format.TEXT) {
                sequenceToFind = text.getBytes();
            } else if (selectedFormat == Format.BYTES) {
                lastError.message = ERROR_NUMBER_FORMAT;

                List<Integer> mapped = Stream.of(text.split(" ")).map(RadixUtils::parseRadix).collect(Collectors.toList());
                sequenceToFind = new byte[mapped.size()];
                for (int i = 0; i < sequenceToFind.length; i++) {
                    sequenceToFind[i] = mapped.get(i).byteValue(); // NOTE: if integer is > than byte, the rest is cut off.
                }
            } else {
                lastError.message = "Invalid number for selected format";
                String value = text.trim();
                ByteBuffer buffer = ByteBuffer.allocate(8).order(byteOrder.get());
                switch (selectedFormat) {
                    case INT16: buffer.putShort(Short.parseShort(value)); break;
                    case INT32: buffer.putInt(Integer.parseInt(value)); break;
                    case INT64: buffer.putLong(Long.parseLong(value)); break;
                    case FLOAT32: buffer.putFloat(Float.parseFloat(value)); break;
                    case FLOAT64: buffer.putDouble(Double.parseDouble(value)); break;
                    default: throw new IllegalStateException();
                }
                sequenceToFind = java.util.Arrays.copyOf(buffer.array(), buffer.position());
            }

            startSearch.accept(sequenceToFind, from);
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
