/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class PaperTapeGui extends DialogBase {
    private static final String ICONS = "/net/emustudio/plugins/device/mits88tap/gui/";
    private static final FileExtensionsFilter TAPE_FILTER = new FileExtensionsFilter("Paper tape", "pt", "bin", "hex");

    private final PaperTapeUnit tape;
    private final Dialogs dialogs;
    private final GUI gui;
    private final JTextField directoryName = new JTextField();
    private final DefaultListModel<Path> tapes = new DefaultListModel<>();
    private final JList<Path> tapeList = new JList<>(tapes);
    private final JTextArea readerFile = new JTextArea();
    private final JTextArea punchFile = new JTextArea();
    private final JLabel readerStatus;
    private final JLabel punchStatus;
    private final JProgressBar progress = new JProgressBar();
    private final JButton load;
    private final JButton rewind;
    private final JButton eject;
    private final JButton closeOutput;
    private final Timer refreshTimer = new Timer(100, event -> refresh());
    private Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

    PaperTapeGui(JFrame parent, PaperTapeUnit tape, Dialogs dialogs, GUI gui) {
        super(parent, "Altair PTR/PTP", false);
        this.tape = tape;
        this.dialogs = dialogs;
        this.gui = gui;
        readerStatus = gui.labelBold("No tape loaded");
        punchStatus = gui.labelBold("No output tape");
        load = button("Load", "load.png", () -> {
            Path selected = tapeList.getSelectedValue();
            if (selected != null) { loadReader(selected); }
        });
        rewind = button("Rewind", "reset.png", () -> { tape.rewindReader(); refresh(); });
        eject = button("Eject", "tape-eject.png", () -> { tape.detachReader(); refresh(); });
        closeOutput = button("Close output", "stop.png", () -> { tape.detachPunch(); refresh(); });
        tapeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tapeList.addListSelectionListener(event -> load.setEnabled(tapeList.getSelectedValue() != null));
        tapeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                Path path = (Path) value;
                setText(path.getFileName().toString());
                setToolTipText(path.toString());
                return this;
            }
        });
        directoryName.setEditable(false);
        progress.setStringPainted(true);
        selectDirectory(directory);
        refresh();
        buildContent();
    }

    private JButton button(String title, String icon, Runnable action) {
        JButton button = gui.button(title);
        button.setIcon(GUI.loadIcon(ICONS + icon));
        button.addActionListener(event -> action.run());
        return button;
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel available = gui.section("Available tapes", "insets 2", "[grow]", "[][grow][]");
        JPanel directories = gui.panel("fillx", "[grow][]", "[]");
        JButton browse = gui.buttonBrowseDirectories(dialogs, "Select tape directory", "Select", this::selectDirectory);
        browse.setIcon(GUI.loadIcon(ICONS + "browse.png"));
        browse.setText("");
        browse.setToolTipText("Select directory");
        JToolBar directoryToolbar = gui.toolBar();
        directoryToolbar.add(browse);
        directoryName.setMinimumSize(new Dimension(0, 0));
        directories.add(directoryName, "growx");
        directories.add(directoryToolbar);
        JToolBar availableToolbar = gui.toolBar();
        availableToolbar.add(button("Refresh", "refresh.png", this::refreshTapes));
        availableToolbar.add(Box.createHorizontalGlue());
        availableToolbar.add(load);
        available.add(directories, "growx, wrap");
        available.add(gui.scrollPane(tapeList), "grow, wrap");
        available.add(availableToolbar, "growx");

        JPanel reader = gui.panel("insets dialog", "[grow]", "[][grow][][]");
        reader.add(tapeInfo(readerFile, readerStatus), "growx, wrap");
        reader.add(gui.label("Guest software reads tape through CPU port 13h."), "growx, wrap");
        reader.add(progress, "growx, wrap");
        JToolBar readerToolbar = gui.toolBar();
        readerToolbar.add(rewind);
        readerToolbar.add(Box.createHorizontalGlue());
        readerToolbar.add(eject);
        reader.add(readerToolbar, "growx");

        JPanel punch = gui.panel("insets dialog", "[grow]", "[][grow][]");
        punch.add(tapeInfo(punchFile, punchStatus), "growx, wrap");
        punch.add(gui.label("<html>Guest software writes tape through CPU port 13h.<br>Opening an output tape replaces its existing contents."), "growx, wrap");
        JToolBar punchToolbar = gui.toolBar();
        punchToolbar.add(button("Save tape", "events-save.png", this::choosePunch));
        punchToolbar.add(Box.createHorizontalGlue());
        punchToolbar.add(closeOutput);
        punch.add(punchToolbar, "growx");

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Reader", reader);
        tabs.addTab("Punch", punch);
        JSplitPane split = gui.splitPaneLeftToRight(available, tabs, 0.3);
        split.setDividerLocation(250);
        JPanel content = gui.panel("insets dialog, fill", "[grow]", "[grow]");
        content.add(split, "push, grow");
        setPreferredSize(new Dimension(800, 400));
        return content;
    }

    private JPanel tapeInfo(JTextArea file, JLabel status) {
        file.setEditable(false);
        file.setLineWrap(true);
        file.setWrapStyleWord(true);
        file.setBackground(UIManager.getColor("Panel.background"));
        file.setMinimumSize(new Dimension(0, 0));
        JPanel info = gui.panel("", "[][grow]", "[][]");
        info.add(gui.label("File name:"), "alignx right");
        info.add(file, "growx, wrap");
        info.add(gui.label("Status:"), "alignx right");
        info.add(status, "growx");
        return info;
    }

    void selectDirectory(Path path) {
        directory = path.toAbsolutePath().normalize();
        directoryName.setText(directory.toString());
        directoryName.setToolTipText(directory.toString());
        refreshTapes();
    }

    private void refreshTapes() {
        Path selected = tapeList.getSelectedValue();
        tapes.clear();
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> paths = files.filter(Files::isRegularFile).filter(Files::isReadable)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return TAPE_FILTER.getExtensions().stream().anyMatch(extension -> name.endsWith("." + extension));
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            paths.forEach(tapes::addElement);
            if (selected != null) { tapeList.setSelectedValue(selected, true); }
        } catch (IOException | UncheckedIOException e) {
            dialogs.showError(e.getMessage(), "Read tape directory");
        }
    }

    private void loadReader(Path path) {
        try {
            tape.attachReader(path);
        } catch (IOException e) {
            dialogs.showError(e.getMessage(), "Load reader tape");
        }
        refresh();
    }

    private void choosePunch() {
        dialogs.chooseFile("Save punch tape", "Save", directory, true, List.of(
                new FileExtensionsFilter("Paper tape", "pt"),
                new FileExtensionsFilter("Binary file", "bin"),
                new FileExtensionsFilter("Intel HEX", "hex"))).ifPresent(path -> {
            try {
                tape.attachPunch(path);
            } catch (IOException e) {
                dialogs.showError(e.getMessage(), "Save punch tape");
            }
            refresh();
        });
    }

    void refresh() {
        synchronized (tape) {
            boolean readerLoaded = tape.getReaderPath().isPresent();
            boolean punchLoaded = tape.getPunchPath().isPresent();
            readerFile.setText(tape.getReaderPath().map(Path::toString).orElse("N/A"));
            punchFile.setText(tape.getPunchPath().map(Path::toString).orElse("N/A"));
            int position = tape.getReaderPosition();
            int length = tape.getReaderLength();
            readerStatus.setText(!readerLoaded ? "No tape loaded" : position < length ? "Ready" : "Read complete");
            punchStatus.setText(punchLoaded ? "Ready" : "No output tape");
            progress.setMaximum(Math.max(1, length));
            progress.setValue(position);
            progress.setString(position + " / " + length + " bytes");
            rewind.setEnabled(readerLoaded);
            eject.setEnabled(readerLoaded);
            closeOutput.setEnabled(punchLoaded);
        }
        load.setEnabled(tapeList.getSelectedValue() != null);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) { refresh(); refreshTimer.start(); }
        else { refreshTimer.stop(); }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
