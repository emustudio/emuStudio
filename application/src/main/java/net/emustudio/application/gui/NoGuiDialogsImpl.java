/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class NoGuiDialogsImpl implements ExtendedDialogs {
    public final static String INPUT_MESSAGE = "Please insert a value";
    private final static Logger LOGGER = LoggerFactory.getLogger(NoGuiDialogsImpl.class);

    public static String formatMessage(String title, String message) {
        return "[" + title + "] " + message;
    }

    @Override
    public void showError(String message) {
        showError(message, "Error");
    }

    @Override
    public void showError(String message, String title) {
        LOGGER.error(formatMessage(title, message));
    }

    @Override
    public void showInfo(String message) {
        showInfo(message, "Information");
    }

    @Override
    public void showInfo(String message, String title) {
        LOGGER.info(formatMessage(title, message));
    }

    @Override
    public Optional<Integer> readInteger(String message) {
        return readInteger(message, INPUT_MESSAGE);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title) {
        return readInteger(message, title, 0);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title, int initial) {
        throw new RuntimeException("Cannot read value: implemented just for GUI version");
    }

    @Override
    public Optional<String> readString(String message) {
        return readString(message, INPUT_MESSAGE);
    }

    @Override
    public Optional<String> readString(String message, String title) {
        return readString(message, title, "");
    }

    @Override
    public Optional<String> readString(String message, String title, String initial) {
        throw new RuntimeException("Cannot read value: implemented just for GUI version");
    }

    @Override
    public Optional<Double> readDouble(String message) {
        return readDouble(message, INPUT_MESSAGE);
    }

    @Override
    public Optional<Double> readDouble(String message, String title) {
        return readDouble(message, title, 0);
    }

    @Override
    public Optional<Double> readDouble(String message, String title, double initial) {
        throw new RuntimeException("Cannot read value: implemented just for GUI version");
    }

    @Override
    public DialogAnswer ask(String message) {
        return ask(message, "Confirmation");
    }

    @Override
    public DialogAnswer ask(String message, String title) {
        throw new RuntimeException("Cannot ask for confirmation: implemented just for GUI version");
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, boolean appendMissingExtension,
                                     FileExtensionsFilter... filters) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, boolean appendMissingExtension,
                                     List<FileExtensionsFilter> list) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, Path baseDirectory,
                                     boolean appendMissingExtension, FileExtensionsFilter... filters) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, Path baseDirectory,
                                     boolean appendMissingExtension, List<FileExtensionsFilter> list) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseDirectory(String title, String approveButtonText) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseDirectory(String title, String approveButtonText, Path baseDirectory) {
        return Optional.empty();
    }
}
