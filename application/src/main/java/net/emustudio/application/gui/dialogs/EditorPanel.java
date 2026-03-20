/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.actions.CompileAction;
import net.emustudio.application.gui.actions.editor.*;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.application.gui.GUIProvider;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.ui.rtextarea.RTextArea;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Supplier;

import static net.emustudio.application.gui.framework.EmuStudioUI.*;
import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

public class EditorPanel extends JPanel {
    private final static int MIN_COMPILER_OUTPUT_HEIGHT = 200;
    private final static double GOLDEN_RATIO = 1.6180339887;

    private final Editor editor;
    private final Dialogs dialogs;

    private final FindAction findAction;
    private final ReplaceAction replaceAction;
    private final SaveFileAction saveFileAction;
    private final NewFileAction newFileAction;
    private final OpenFileAction openFileAction;
    private final CompileAction compileAction;

    private final ReplaceDialog replaceDialog;
    private final FindDialog findDialog;

    private final JSplitPane splitSource;

    public EditorPanel(JFrame parent, Dialogs dialogs, Editor editor, VirtualComputer computer, Runnable updateTitle,
                       Supplier<CPU.RunState> runState) {

        this.editor = Objects.requireNonNull(editor);
        this.dialogs = Objects.requireNonNull(dialogs);

        this.replaceDialog = new ReplaceDialog(parent, editor);
        this.findDialog = new FindDialog(parent, editor);

        JTextArea compilerOutput = GUIProvider.getGUI().textAreaReadOnly(3, 20);
        compilerOutput.setFont(FONT_MONOSPACED);

        this.saveFileAction = new SaveFileAction(editor, updateTitle);
        this.findAction = new FindAction(findDialog, replaceDialog);
        this.replaceAction = new ReplaceAction(findDialog, replaceDialog);
        this.newFileAction = new NewFileAction(this::confirmSave, editor, compilerOutput, updateTitle);
        this.openFileAction = new OpenFileAction(this::confirmSave, editor, compilerOutput, updateTitle);
        this.compileAction = new CompileAction(
                computer, dialogs, editor, runState, compilerOutput, updateTitle
        );

        JScrollPane compilerPane = GUIProvider.getGUI().scrollPane(compilerOutput);

        splitSource = GUIProvider.getGUI().splitPaneTopToBottom(editor.getView(), compilerPane, 1.0);
        splitSource.setOneTouchExpandable(true);

        JToolBar mainToolBar = setupMainToolbar();

        setLayout(new net.miginfocom.swing.MigLayout("insets 0, fill", "[grow]", "[][grow]"));
        add(mainToolBar, "growx, wrap");
        add(splitSource, "grow, gaptop 0");
    }

    public void resizeComponents(int height) {
        int newHeight = (int) (((double) height) / GOLDEN_RATIO);

        if (height - newHeight < MIN_COMPILER_OUTPUT_HEIGHT) {
            splitSource.setDividerLocation(height - MIN_COMPILER_OUTPUT_HEIGHT);
        } else {
            splitSource.setDividerLocation(newHeight);
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
        if (editor.isDirty()) {
            Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
            if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                return editor.saveFile();
            } else return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
        }
        return true;
    }

    private JToolBar setupMainToolbar() {
        JToolBar mainToolBar = GUIProvider.getGUI().toolBar();

        mainToolBar.add(GUIProvider.getGUI().toolbarButton(newFileAction));
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(openFileAction));
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(saveFileAction));
        mainToolBar.addSeparator();
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(
                RTextArea.getAction(RTextArea.UNDO_ACTION),
                ICON_UNDO,
                "Undo"
        ));
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(
                RTextArea.getAction(RTextArea.REDO_ACTION),
                ICON_REDO,
                "Redo"
        ));
        mainToolBar.addSeparator();
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(
                RTextArea.getAction(RTextArea.CUT_ACTION),
                ICON_CUT,
                "Cut selection"
        ));
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(
                RTextArea.getAction(RTextArea.COPY_ACTION),
                ICON_COPY,
                "Copy selection"
        ));
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(
                RTextArea.getAction(RTextArea.PASTE_ACTION),
                ICON_PASTE,
                "Paste from clipboard"
        ));
        mainToolBar.addSeparator();
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(findAction));
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(replaceAction));
        mainToolBar.addSeparator();
        mainToolBar.add(GUIProvider.getGUI().toolbarButton(compileAction));

        return mainToolBar;
    }
}
