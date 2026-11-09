/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.application.gui.dialogs.InputDialog;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.io.FileType;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.application.ui.GUI;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DialogsGui implements Dialogs {
    private final GUI gui;
    private Component parent;

    public DialogsGui(GUI gui) {
        this.gui = java.util.Objects.requireNonNull(gui);
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    @Override
    public void showError(String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title, Integer initial) {
        return readString(message, title, initial == null ? null : String.valueOf(initial))
                .flatMap(value -> parse(value, v -> Numbers.parseRadix(v.trim())));
    }

    @Override
    public Optional<String> readString(String message, String title, String initial) {
        return InputDialog.showInputDialog(parent, message, title, initial == null ? "" : initial, gui);
    }

    @Override
    public Optional<Double> readDouble(String message, String title, Double initial) {
        return readString(message, title, initial == null ? null : String.valueOf(initial))
                .flatMap(value -> parse(value, Double::parseDouble));
    }

    @Override
    public Answer ask(String message, String title) {
        int answer = JOptionPane.showConfirmDialog(
                parent, message, title, JOptionPane.YES_NO_CANCEL_OPTION
        );

        switch (answer) {
            case JOptionPane.YES_OPTION:
                return Answer.YES;
            case JOptionPane.NO_OPTION:
                return Answer.NO;
        }
        return Answer.CANCEL;
    }

    @Override
    public Optional<Path> chooseFile(String title, String approveButtonText, Path baseDirectory,
                                     boolean appendMissingExtension, List<FileType> fileTypes) {

        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setApproveButtonText(approveButtonText);
        fileChooser.setCurrentDirectory(baseDirectoryOrCurrent(baseDirectory).toFile());

        FileNameExtensionFilter firstFilter = null;
        for (FileType fileType : fileTypes) {
            String formattedExtensions = fileType.getExtensions().stream()
                    .map(e -> "*." + e)
                    .collect(Collectors.joining(", ", " (", ")"));

            FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter(
                    fileType.getDescription() + formattedExtensions,
                    fileType.getExtensions().toArray(new String[0])
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
    public Optional<Path> chooseDirectory(String title, String approveButtonText, Path baseDirectory) {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setApproveButtonText(approveButtonText);
        fileChooser.setCurrentDirectory(baseDirectoryOrCurrent(baseDirectory).toFile());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(parent);
        fileChooser.setVisible(true);
        if (result == JFileChooser.APPROVE_OPTION) {
            return Optional.ofNullable(fileChooser.getSelectedFile()).map(File::toPath);
        }
        return Optional.empty();
    }

    private static Path baseDirectoryOrCurrent(Path baseDirectory) {
        return baseDirectory == null ? Path.of(System.getProperty("user.dir")) : baseDirectory;
    }

    private static <T> Optional<T> parse(String value, Function<String, T> parser) {
        try {
            return Optional.of(parser.apply(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
