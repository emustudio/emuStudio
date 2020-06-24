package net.emustudio.application.gui.dialogs;

import net.emustudio.application.Constants;
import net.emustudio.application.gui.ToolbarButton;
import net.emustudio.application.gui.actions.CompileAction;
import net.emustudio.application.gui.actions.editor.*;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Supplier;

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

    private final JSplitPane splitSource = new JSplitPane();

    public EditorPanel(JFrame parent, Dialogs dialogs, Editor editor, VirtualComputer computer, Runnable updateTitle,
                       Supplier<CPU.RunState> runState) {

        this.editor = Objects.requireNonNull(editor);
        this.dialogs = Objects.requireNonNull(dialogs);

        this.replaceDialog = new ReplaceDialog(parent, editor);
        this.findDialog = new FindDialog(parent, editor);

        JTextArea compilerOutput = new JTextArea();
        compilerOutput.setColumns(20);
        compilerOutput.setEditable(false);
        compilerOutput.setFont(Constants.FONT_MONOSPACED);
        compilerOutput.setLineWrap(true);
        compilerOutput.setRows(3);
        compilerOutput.setWrapStyleWord(true);

        this.saveFileAction = new SaveFileAction(editor, updateTitle);
        this.findAction = new FindAction(findDialog, replaceDialog);
        this.replaceAction = new ReplaceAction(findDialog, replaceDialog);
        this.newFileAction = new NewFileAction(this::confirmSave, editor, compilerOutput, updateTitle);
        this.openFileAction = new OpenFileAction(this::confirmSave, editor, compilerOutput, updateTitle);
        this.compileAction = new CompileAction(
            computer, dialogs, editor, runState, compilerOutput, updateTitle
        );

        RTextScrollPane editorScrollPane = new RTextScrollPane(editor.getView());

        JScrollPane compilerPane = new JScrollPane();
        compilerPane.setViewportView(compilerOutput);

        splitSource.setBorder(null);
        splitSource.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitSource.setOneTouchExpandable(true);
        splitSource.setLeftComponent(editorScrollPane);
        splitSource.setRightComponent(compilerPane);

        JToolBar mainToolBar = setupMainToolbar();
        mainToolBar.setRollover(true);

        GroupLayout panelSourceLayout = new GroupLayout(this);
        setLayout(panelSourceLayout);
        panelSourceLayout.setHorizontalGroup(
            panelSourceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(mainToolBar)
                .addGroup(
                    panelSourceLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(splitSource)
                        .addContainerGap()
                )
        );
        panelSourceLayout.setVerticalGroup(
            panelSourceLayout
                .createSequentialGroup()
                .addComponent(mainToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(splitSource, 10, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addContainerGap()
        );
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
        JToolBar mainToolBar = new JToolBar();

        mainToolBar.setFloatable(false);
        mainToolBar.setBorderPainted(false);
        mainToolBar.setRollover(true);

        mainToolBar.add(new ToolbarButton(newFileAction, "New file"));
        mainToolBar.add(new ToolbarButton(openFileAction, "Open file"));
        mainToolBar.add(new ToolbarButton(saveFileAction, "Save file"));
        mainToolBar.addSeparator();
        mainToolBar.add(new ToolbarButton(
            RTextArea.getAction(RTextArea.UNDO_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-undo.png",
            "Undo"
        ));
        mainToolBar.add(new ToolbarButton(RTextArea.getAction(RTextArea.REDO_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-redo.png",
            "Redo"
        ));
        mainToolBar.addSeparator();
        mainToolBar.add(new ToolbarButton(
            RTextArea.getAction(RTextArea.CUT_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-cut.png",
            "Cut selection"
        ));
        mainToolBar.add(new ToolbarButton(
            RTextArea.getAction(RTextArea.COPY_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-copy.png",
            "Copy selection"
        ));
        mainToolBar.add(new ToolbarButton(
            RTextArea.getAction(RTextArea.PASTE_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-paste.png",
            "Paste from clipboard"
        ));
        mainToolBar.addSeparator();
        mainToolBar.add(new ToolbarButton(findAction, "Find text..."));
        mainToolBar.add(new ToolbarButton(replaceAction, "Find/replace text..."));
        mainToolBar.addSeparator();
        mainToolBar.add(new ToolbarButton(compileAction, "Compile source"));

        return mainToolBar;
    }
}
