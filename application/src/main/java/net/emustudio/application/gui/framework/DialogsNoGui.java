/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.emulib.io.FileType;
import net.emustudio.emulib.application.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class DialogsNoGui implements Dialogs {
    private final static Logger LOGGER = LoggerFactory.getLogger(DialogsNoGui.class);

    public static String formatMessage(String title, String message) {
        return "[" + title + "] " + message;
    }

    @Override
    public void showError(String message, String title) {
        LOGGER.error(formatMessage(title, message));
    }

    @Override
    public void showInfo(String message, String title) {
        LOGGER.info(formatMessage(title, message));
    }

    @Override
    public Answer ask(String message, String title) {
        LOGGER.warn("Cannot ask without GUI, assuming cancel: {}", formatMessage(title, message));
        return Answer.CANCEL;
    }

    @Override
    public Optional<String> readString(String message, String title, String initial) {
        return cannotRead(message, title);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title, Integer initial) {
        return cannotRead(message, title);
    }

    @Override
    public Optional<Double> readDouble(String message, String title, Double initial) {
        return cannotRead(message, title);
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, Path baseDirectory,
                                     boolean appendMissingExtension, List<FileType> fileTypes) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseDirectory(String title, String approveButtonText, Path baseDirectory) {
        return Optional.empty();
    }

    private static <T> Optional<T> cannotRead(String message, String title) {
        LOGGER.warn("Cannot read value without GUI: {}", formatMessage(title, message));
        return Optional.empty();
    }
}
