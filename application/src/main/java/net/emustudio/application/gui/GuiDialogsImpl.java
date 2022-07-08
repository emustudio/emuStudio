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

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GuiDialogsImpl implements ExtendedDialogs {
    private final RadixUtils radixUtils = RadixUtils.getInstance();
    private Component parent;

    @Override
    public boolean isGui() {
        return true;
    }

    @Override
    public void setParent(Component parent) {
        this.parent = parent;
    }

    @Override
    public void showError(String message) {
        showError(message, "Error");
    }

    @Override
    public void showError(String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String message) {
        showInfo(message, "Information");
    }

    @Override
    public void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public Optional<Integer> readInteger(String message) {
        return readInteger(message, NoGuiDialogsImpl.INPUT_MESSAGE);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title) {
        return readInteger(message, title, 0);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title, int initial) {
        Object inputValue = JOptionPane.showInputDialog(
            parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial
        );
        return Optional.ofNullable(inputValue).map(String::valueOf).map(radixUtils::parseRadix);
    }

    @Override
    public Optional<String> readString(String message) {
        return readString(message, NoGuiDialogsImpl.INPUT_MESSAGE);
    }

    @Override
    public Optional<String> readString(String message, String title) {
        return readString(message, title, "");
    }

    @Override
    public Optional<String> readString(String message, String title, String initial) {
        Object inputValue = JOptionPane.showInputDialog(
            parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial
        );
        return Optional.ofNullable(inputValue).map(String::valueOf);
    }

    @Override
    public Optional<Double> readDouble(String message) {
        return readDouble(message, NoGuiDialogsImpl.INPUT_MESSAGE);
    }

    @Override
    public Optional<Double> readDouble(String message, String title) {
        return readDouble(message, title, 0);
    }

    @Override
    public Optional<Double> readDouble(String message, String title, double initial) {
        Object inputValue = JOptionPane.showInputDialog(
            parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial
        );
        return Optional.ofNullable(inputValue).map(String::valueOf).map(Double::parseDouble);
    }

    @Override
    public DialogAnswer ask(String message) {
        return ask(message, "Confirmation");
    }

    @Override
    public DialogAnswer ask(String message, String title) {
        int answer = JOptionPane.showConfirmDialog(
            parent, message, title, JOptionPane.YES_NO_CANCEL_OPTION
        );

        switch (answer) {
            case JOptionPane.YES_OPTION:
                return DialogAnswer.ANSWER_YES;
            case JOptionPane.NO_OPTION:
                return DialogAnswer.ANSWER_NO;
        }
        return DialogAnswer.ANSWER_CANCEL;
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, boolean appendMissingExtension,
                                     FileExtensionsFilter... filters) {
        return chooseFile(title, approveButtonText, Path.of(System.getProperty("user.dir")), appendMissingExtension, filters);
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonTest, boolean appendMissingExtension,
                                     List<FileExtensionsFilter> filters) {
        return chooseFile(title, approveButtonTest, appendMissingExtension, filters.toArray(FileExtensionsFilter[]::new));
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, Path baseDirectory,
                                     boolean appendMissingExtension, FileExtensionsFilter... filters) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setApproveButtonText(approveButtonText);
        fileChooser.setCurrentDirectory(baseDirectory.toFile());

        FileNameExtensionFilter firstFilter = null;
        for (FileExtensionsFilter filter : filters) {
            String formattedExtensions = filter.getExtensions().stream()
                .map(e -> "*." + e)
                .collect(Collectors.joining(", ", " (", ")"));

            FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter(
                filter.getDescription() + formattedExtensions,
                filter.getExtensions().toArray(new String[0])
            );
            if (firstFilter == null) {
                firstFilter = extensionFilter;
            }
            fileChooser.addChoosableFileFilter(extensionFilter);
        }
        fileChooser.setAcceptAllFileFilterUsed(true);
        if (firstFilter != null) {
            fileChooser.setFileFilter(firstFilter);
        }

        int result = appendMissingExtension ? fileChooser.showSaveDialog(parent) : fileChooser.showOpenDialog(parent);
        fileChooser.setVisible(true);
        if (result == JFileChooser.APPROVE_OPTION) {
            List<String> allExtensions = new ArrayList<>();
            FileFilter selectedFilter = fileChooser.getFileFilter();
            if (selectedFilter instanceof FileNameExtensionFilter) {
                allExtensions.addAll(List.of(((FileNameExtensionFilter)selectedFilter).getExtensions()));
            }

            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile != null) {
                String extension = "";
                if (appendMissingExtension && selectedFilter != null) {
                    if (!allExtensions.isEmpty()) {
                        extension = "." + allExtensions.get(0); // get the first one
                    }
                }
                if (!extension.isEmpty() && !selectedFile.getName().endsWith(extension)) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + extension);
                }
                return Optional.of(selectedFile.toPath());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, Path baseDirectory,
                                     boolean appendMissingExtension, List<FileExtensionsFilter> filters) {
        return chooseFile(
            title, approveButtonText, baseDirectory, appendMissingExtension, filters.toArray(FileExtensionsFilter[]::new)
        );
    }
}
