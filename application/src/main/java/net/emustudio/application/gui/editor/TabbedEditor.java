/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.rsta.ui.search.SearchEvent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TabbedEditor implements Editor {
    private static final String UNTITLED = "Untitled";

    private final Dialogs dialogs;
    private final Compiler compiler;
    private final Consumer<List<Path>> openFilesChanged;
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<REditor> editors = new ArrayList<>();
    private Runnable activeEditorChanged = () -> {
    };

    public TabbedEditor(Dialogs dialogs, Compiler compiler, Collection<Path> openFiles,
                        Consumer<List<Path>> openFilesChanged) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.compiler = compiler;
        this.openFilesChanged = Objects.requireNonNull(openFilesChanged);

        Optional.ofNullable(openFiles).orElse(Collections.emptyList()).stream()
                .filter(Files::isRegularFile)
                .forEach(this::openFile);
        if (editors.isEmpty()) {
            addEditor(createEditor());
        }
        tabs.addChangeListener(e -> activeEditorChanged.run());
        notifyOpenFilesChanged();
    }

    public void setActiveEditorChanged(Runnable activeEditorChanged) {
        this.activeEditorChanged = Objects.requireNonNull(activeEditorChanged);
    }

    public boolean closeCurrent() {
        REditor editor = activeEditor();
        if (!confirmSave(editor)) {
            return false;
        }

        int index = tabs.getSelectedIndex();
        tabs.removeTabAt(index);
        editors.remove(index);
        if (editors.isEmpty()) {
            addEditor(createEditor());
        }
        notifyOpenFilesChanged();
        activeEditorChanged.run();
        return true;
    }

    public boolean confirmSaveAll() {
        for (int i = 0; i < editors.size(); i++) {
            REditor editor = editors.get(i);
            if (editor.isDirty()) {
                tabs.setSelectedIndex(i);
                if (!confirmSave(editor)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void newFile() {
        REditor editor = createEditor();
        addEditor(editor);
        tabs.setSelectedIndex(editors.size() - 1);
        activeEditorChanged.run();
    }

    @Override
    public boolean openFile() {
        return activeEditor().chooseFileToOpen().map(this::openFile).orElse(false);
    }

    @Override
    public boolean openFile(Path fileName) {
        Path normalized = fileName.toAbsolutePath().normalize();
        for (int i = 0; i < editors.size(); i++) {
            if (editors.get(i).getCurrentFile().map(File::toPath).map(Path::toAbsolutePath)
                    .map(Path::normalize).filter(normalized::equals).isPresent()) {
                tabs.setSelectedIndex(i);
                activeEditorChanged.run();
                return true;
            }
        }

        REditor editor = reusableUntitledEditor().orElseGet(this::createEditor);
        if (!editor.openFile(normalized)) {
            return false;
        }
        if (!editors.contains(editor)) {
            addEditor(editor);
        }
        tabs.setSelectedIndex(editors.indexOf(editor));
        updateTitle(editor);
        notifyOpenFilesChanged();
        activeEditorChanged.run();
        return true;
    }

    @Override
    public boolean saveFile() {
        REditor editor = activeEditor();
        boolean saved = editor.saveFile();
        if (saved) {
            updateTitle(editor);
            notifyOpenFilesChanged();
        }
        return saved;
    }

    @Override
    public boolean saveFileAs() {
        REditor editor = activeEditor();
        boolean saved = editor.saveFileAs();
        if (saved) {
            updateTitle(editor);
            notifyOpenFilesChanged();
        }
        return saved;
    }

    @Override
    public boolean isDirty() {
        return activeEditor().isDirty();
    }

    @Override
    public Optional<Boolean> findNext() {
        return activeEditor().findNext();
    }

    @Override
    public Optional<Boolean> findPrevious() {
        return activeEditor().findPrevious();
    }

    @Override
    public void clearMarkedOccurences() {
        activeEditor().clearMarkedOccurences();
    }

    @Override
    public Component getView() {
        return tabs;
    }

    @Override
    public void grabFocus() {
        activeEditor().grabFocus();
    }

    @Override
    public void setPosition(SourceCodePosition position) {
        activeEditor().setPosition(position);
    }

    @Override
    public void setSourceCodePositions(Collection<SourceCodePosition> positions) {
        activeEditor().setSourceCodePositions(positions);
    }

    @Override
    public Optional<File> getCurrentFile() {
        return activeEditor().getCurrentFile();
    }

    @Override
    public void searchEvent(SearchEvent event) {
        activeEditor().searchEvent(event);
    }

    @Override
    public String getSelectedText() {
        return activeEditor().getSelectedText();
    }

    private REditor createEditor() {
        REditor editor = new REditor(dialogs, compiler);
        editor.setOpenFileHandler(this::openFile);
        editor.addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            if (editors.contains(editor)) {
                updateTitle(editor);
                if (editor == activeEditor()) {
                    activeEditorChanged.run();
                }
            }
        }));
        return editor;
    }

    private void addEditor(REditor editor) {
        editors.add(editor);
        tabs.addTab(title(editor), editor.getView());
    }

    private REditor activeEditor() {
        return editors.get(Math.max(0, tabs.getSelectedIndex()));
    }

    private Optional<REditor> reusableUntitledEditor() {
        if (editors.size() == 1) {
            REditor editor = editors.get(0);
            if (!editor.isDirty() && editor.getCurrentFile().isEmpty()) {
                return Optional.of(editor);
            }
        }
        return Optional.empty();
    }

    private boolean confirmSave(REditor editor) {
        if (!editor.isDirty()) {
            return true;
        }
        Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
        if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
            boolean saved = editor.saveFile();
            if (saved) {
                updateTitle(editor);
                notifyOpenFilesChanged();
            }
            return saved;
        }
        return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
    }

    private void updateTitle(REditor editor) {
        int index = editors.indexOf(editor);
        if (index >= 0) {
            tabs.setTitleAt(index, title(editor));
        }
    }

    private String title(REditor editor) {
        String title = editor.getCurrentFile().map(File::getName).orElse(UNTITLED);
        return editor.isDirty() ? title + "*" : title;
    }

    private void notifyOpenFilesChanged() {
        openFilesChanged.accept(editors.stream()
                .map(REditor::getCurrentFile)
                .flatMap(Optional::stream)
                .map(File::toPath)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toList()));
    }
}
