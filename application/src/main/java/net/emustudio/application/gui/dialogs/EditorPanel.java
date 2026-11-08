/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.actions.CompileAction;
import net.emustudio.application.gui.actions.editor.*;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.gui.editor.TabbedEditor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.ui.rtextarea.RTextArea;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.util.Objects;
import java.util.function.Supplier;

import static net.emustudio.emulib.runtime.ui.StandardIcons.*;
import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.application.gui.editor.Editor.MIN_FONT_ZOOM;
import static net.emustudio.application.gui.editor.Editor.MAX_FONT_ZOOM;
import static net.emustudio.application.Constants.FONT_CODE;

public class EditorPanel extends JPanel {
    private final static int COMPILER_OUTPUT_HEIGHT = 120;

    private final Editor editor;
    private final Dialogs dialogs;
    private final GUI gui;

    private final FindAction findAction;
    private final ReplaceAction replaceAction;
    private final SaveFileAction saveFileAction;
    private final NewFileAction newFileAction;
    private final OpenFileAction openFileAction;
    private final CompileAction compileAction;

    private final ReplaceDialog replaceDialog;
    private final FindDialog findDialog;

    private final JSplitPane splitSource;
    private boolean dividerInitialized;

    public EditorPanel(JFrame parent, Dialogs dialogs, Editor editor, VirtualComputer computer, Runnable updateTitle,
                       Supplier<CPU.RunState> runState, GUI gui) {

        this.editor = Objects.requireNonNull(editor);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);

        this.replaceDialog = new ReplaceDialog(parent, editor);
        this.findDialog = new FindDialog(parent, editor);

        JTextArea compilerOutput = gui.textAreaReadOnly(3, 20);
        compilerOutput.setFont(FONT_MONOSPACED);

        this.saveFileAction = new SaveFileAction(editor, updateTitle);
        this.findAction = new FindAction(findDialog, replaceDialog) {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (editor instanceof TabbedEditor) ((TabbedEditor) editor).searchBar().showFind();
                else super.actionPerformed(event);
            }
        };
        this.replaceAction = new ReplaceAction(findDialog, replaceDialog) {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (editor instanceof TabbedEditor) ((TabbedEditor) editor).searchBar().showReplace();
                else super.actionPerformed(event);
            }
        };
        Supplier<Boolean> confirmReplace = editor instanceof TabbedEditor ? () -> true : this::confirmSave;
        this.newFileAction = new NewFileAction(confirmReplace, editor, compilerOutput, updateTitle);
        this.openFileAction = new OpenFileAction(confirmReplace, editor, compilerOutput, updateTitle);
        this.compileAction = new CompileAction(
                computer, dialogs, editor, runState, compilerOutput, updateTitle
        );

        JScrollPane compilerPane = gui.scrollPane(compilerOutput);
        splitSource = gui.splitPaneTopToBottom(editor.getView(), compilerPane, 1.0);
        // Resize the syntax editor once on release rather than repainting it throughout the drag.
        splitSource.setContinuousLayout(false);
        splitSource.setOneTouchExpandable(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                resizeComponents(getHeight());
            }
        });

        JToolBar mainToolBar = setupMainToolbar();

        setLayout(new net.miginfocom.swing.MigLayout("insets 0, fill", "[grow]", ""));
        add(mainToolBar, "growx, wrap");
        if (editor instanceof TabbedEditor) add(((TabbedEditor) editor).searchBar(), "growx, wrap, hidemode 3");
        add(splitSource, "grow, push, gaptop 0");
    }

    public void resizeComponents(int height) {
        if (!dividerInitialized && isShowing() && splitSource.getHeight() > 0) {
            splitSource.setDividerLocation(Math.max(0,
                    splitSource.getHeight() - splitSource.getDividerSize() - COMPILER_OUTPUT_HEIGHT));
            dividerInitialized = true;
        }
    }

    public Action getFindAction() {
        return findAction;
    }

    public ReplaceAction getReplaceAction() {
        return replaceAction;
    }

    public SaveFileAction getSaveFileAction() {
        return saveFileAction;
    }

    public NewFileAction getNewFileAction() {
        return newFileAction;
    }

    public OpenFileAction getOpenFileAction() {
        return openFileAction;
    }

    public CompileAction getCompileAction() {
        return compileAction;
    }

    public void dispose() {
        findDialog.dispose();
        replaceDialog.dispose();
    }

    public final boolean confirmSave() {
        if (editor instanceof TabbedEditor) {
            return ((TabbedEditor) editor).confirmSaveAll();
        }
        if (editor.isDirty()) {
            Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
            if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                return editor.saveFile();
            } else return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
        }
        return true;
    }

    private JToolBar setupMainToolbar() {
        JToolBar mainToolBar = gui.toolBar();

        mainToolBar.add(gui.toolbarButton(newFileAction));
        mainToolBar.add(gui.toolbarButton(openFileAction));
        mainToolBar.add(gui.toolbarButton(saveFileAction));
        mainToolBar.addSeparator();
        mainToolBar.add(gui.toolbarButton(
                RTextArea.getAction(RTextArea.UNDO_ACTION),
                ICON_UNDO,
                "Undo"
        ));
        mainToolBar.add(gui.toolbarButton(
                RTextArea.getAction(RTextArea.REDO_ACTION),
                ICON_REDO,
                "Redo"
        ));
        mainToolBar.addSeparator();
        mainToolBar.add(gui.toolbarButton(findAction));
        mainToolBar.addSeparator();
        JButton compileButton = gui.toolbarButton(compileAction);
        compileButton.getAccessibleContext().setAccessibleName("Save & Compile source file");
        mainToolBar.add(compileButton);
        mainToolBar.addSeparator();

        JComboBox<String> fontZoom = new JComboBox<>(new String[]{
                "30%", "50%", "75%", "100%", "125%", "150%", "175%", "200%", "250%", "300%", "350%"
        });
        fontZoom.setEditable(true);
        fontZoom.setSelectedItem("100%");
        fontZoom.setToolTipText("Font zoom (Ctrl + / Ctrl -)");
        fontZoom.getAccessibleContext().setAccessibleName("Font zoom");
        Dimension zoomSize = new Dimension(90, fontZoom.getPreferredSize().height);
        fontZoom.setPreferredSize(zoomSize);
        fontZoom.setMaximumSize(zoomSize);
        Runnable updateZoom = () -> {
            int zoom = Math.round(editor.getFontSize() * 100.0f / FONT_CODE.getSize2D());
            String percentage = zoom + "%";
            if (!percentage.equals(fontZoom.getSelectedItem())) {
                fontZoom.setSelectedItem(percentage);
            }
        };
        updateZoom.run();
        fontZoom.addActionListener(e -> {
            String value = Objects.toString(fontZoom.getSelectedItem(), "").trim();
            if (value.endsWith("%")) value = value.substring(0, value.length() - 1).trim();
            try {
                int zoom = Math.max(MIN_FONT_ZOOM, Math.min(MAX_FONT_ZOOM, Integer.parseInt(value)));
                if (zoom != Math.round(editor.getFontSize() * 100.0f / FONT_CODE.getSize2D())) {
                    editor.setFontSize(FONT_CODE.getSize2D() * zoom / 100.0f);
                }
            } catch (NumberFormatException ignored) {
                // Restore the current zoom when the entered value is not a percentage.
            }
            updateZoom.run();
        });
        editor.addFontSizeChangeListener(updateZoom);
        mainToolBar.add(fontZoom);

        return mainToolBar;
    }
}
