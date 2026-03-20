/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.application.gui.dialogs.InputDialog;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;

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

public class GuiDialogsImpl implements Dialogs {
    private final RadixUtils radixUtils = RadixUtils.getInstance();
    private Component parent;

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
        return InputDialog.showInputDialog(parent, message, title, initial)
                .map(radixUtils::parseRadix);
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
        return InputDialog.showInputDialog(parent, message, title, initial);
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
        return InputDialog.showInputDialog(parent, message, title, initial)
                .map(Double::parseDouble);
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

        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
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
                allExtensions.addAll(List.of(((FileNameExtensionFilter) selectedFilter).getExtensions()));
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

    @Override
    public Optional<Path> chooseDirectory(String title, String approveButtonText) {
        return chooseDirectory(title, approveButtonText, Path.of(System.getProperty("user.dir")));
    }

    @Override
    public Optional<Path> chooseDirectory(String title, String approveButtonText, Path baseDirectory) {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setApproveButtonText(approveButtonText);
        fileChooser.setCurrentDirectory(baseDirectory.toFile());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(parent);
        fileChooser.setVisible(true);
        if (result == JFileChooser.APPROVE_OPTION) {
            return Optional.ofNullable(fileChooser.getSelectedFile()).map(File::toPath);
        }
        return Optional.empty();
    }
}
