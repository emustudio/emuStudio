/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

package net.emustudio.application.gui;

import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class NoGuiDialogsImpl implements ExtendedDialogs {
    private final static Logger LOGGER = LoggerFactory.getLogger(NoGuiDialogsImpl.class);
    public final static String INPUT_MESSAGE = "Please insert a value";

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
        return readInteger(message, title,0);
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

    public static String formatMessage(String title, String message) {
        return "[" + title + "] " + message;
    }
}
