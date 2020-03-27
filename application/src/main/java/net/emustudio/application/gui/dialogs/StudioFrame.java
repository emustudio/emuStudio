/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.actions.AboutAction;
import net.emustudio.application.gui.actions.CompilerSettingsAction;
import net.emustudio.application.gui.actions.ExitAction;
import net.emustudio.application.gui.actions.ViewComputerAction;
import net.emustudio.application.gui.actions.editor.FindNextAction;
import net.emustudio.application.gui.actions.editor.FindPreviousAction;
import net.emustudio.application.gui.actions.editor.SaveFileAsAction;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.gui.editor.REditor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.fife.ui.rtextarea.RTextArea;

import javax.swing.*;
import java.awt.event.*;
import java.util.Objects;

public class StudioFrame extends JFrame {
    private final static String SOURCE_CODE_EDITOR = "Source code editor";

    private final Editor editor;

    private final EditorPanel editorPanel;
    private final EmulatorPanel emulatorPanel;

    private final SaveFileAsAction saveFileAsAction;
    private final FindNextAction findNextAction;
    private final FindPreviousAction findPreviousAction;
    private final ExitAction exitAction;

    private final ViewComputerAction viewComputerAction;
    private final CompilerSettingsAction compilerSettingsAction;
    private final AboutAction aboutAction;

    private final JTabbedPane tabbedPane = new JTabbedPane();


    public StudioFrame(VirtualComputer computer, ApplicationConfig applicationConfig, Dialogs dialogs,
                       DebugTableModel debugTableModel, MemoryContext<?> memoryContext, String fileName) {
        this(computer, applicationConfig, dialogs, debugTableModel, memoryContext);
        editor.openFile(fileName);
    }

    public StudioFrame(VirtualComputer computer, ApplicationConfig applicationConfig, Dialogs dialogs,
                       DebugTableModel debugTableModel, MemoryContext<?> memoryContext) {
        Objects.requireNonNull(computer);

        this.editor = computer.getCompiler()
            .map(compiler -> new REditor(dialogs, compiler))
            .orElse(new REditor(dialogs));

        EmulationController emulationController = computer.getCPU().map(cpu -> new EmulationController(
            cpu, computer.getMemory().orElse(null), computer.getDevices()
        )).orElse(null);

        this.emulatorPanel = new EmulatorPanel(
            this, computer, debugTableModel, dialogs, emulationController, memoryContext
        );
        this.editorPanel = new EditorPanel(
            this, dialogs, editor, computer, this::updateTitleOfSourceCodePanel, emulatorPanel::getRunState
        );

        this.saveFileAsAction = new SaveFileAsAction(editor, this::updateTitleOfSourceCodePanel);
        this.findNextAction = new FindNextAction(editor, dialogs, editorPanel.getFindAction());
        this.findPreviousAction = new FindPreviousAction(editor, dialogs, editorPanel.getFindAction());
        this.exitAction = new ExitAction(editorPanel::confirmSave, emulationController, computer, this::formWindowClosing);

        this.viewComputerAction = new ViewComputerAction(this, computer, dialogs, applicationConfig);
        this.compilerSettingsAction = new CompilerSettingsAction(this, computer);
        this.aboutAction = new AboutAction(this);

        initComponents();

        this.setTitle("emuStudio [" + computer.getComputerConfig().getName() + "]");

        pack();
        setLocationRelativeTo(null);
        resizeComponents();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponents();
            }
        });
        editor.grabFocus();
    }


    private void resizeComponents() {
        int height = getHeight();
        editorPanel.resizeComponents(height);
        emulatorPanel.resizeComponents(height);
    }

    private void initComponents() {
        setIconImage(new ImageIcon(getClass().getResource("/net/emustudio/application/gui/favicon16.png")).getImage());

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        getRootPane().registerKeyboardAction(
            e ->editor.clearMarkedOccurences(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                exitAction.actionPerformed(new ActionEvent(evt.getSource(), evt.getID(), evt.paramString()));
            }
        });

        tabbedPane.setFocusable(false);
        tabbedPane.addTab(SOURCE_CODE_EDITOR, editorPanel);
        tabbedPane.addTab("Emulator", emulatorPanel);

        JMenuBar mainMenu = setupMainMenu();
        setJMenuBar(mainMenu);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
    }

    private JMenuBar setupMainMenu() {
        JMenuBar mainMenuBar = new JMenuBar();
        JMenu mnuFile = new JMenu();
        JSeparator separator1 = new JSeparator();
        JSeparator separator2 = new JSeparator();
        JMenu mnuEdit = new JMenu();
        JSeparator separator3 = new JSeparator();
        JSeparator separator4 = new JSeparator();
        JMenu mnuProject = new JMenu();
        JMenu mnuHelp = new JMenu();

        mnuFile.setText("File");
        mnuFile.add(createMenuItem(editorPanel.getNewFileAction()));
        mnuFile.add(createMenuItem(editorPanel.getOpenFileAction()));
        mnuFile.add(separator1);
        mnuFile.add(createMenuItem(editorPanel.getSaveFileAction()));
        mnuFile.add(saveFileAsAction);
        mnuFile.add(separator2);
        mnuFile.add(createMenuItem(exitAction));
        mainMenuBar.add(mnuFile);

        mnuEdit.setText("Edit");
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        mnuEdit.add(separator3);
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        mnuEdit.add(separator4);
        mnuEdit.add(createMenuItem(editorPanel.getFindAction()));
        mnuEdit.add(createMenuItem(editorPanel.getReplaceAction()));
        mnuEdit.add(createMenuItem(findNextAction));
        mnuEdit.add(createMenuItem(findPreviousAction));
        mainMenuBar.add(mnuEdit);

        mnuProject.setText("Project");
        mnuProject.add(createMenuItem(editorPanel.getCompileAction()));
        mnuProject.add(createMenuItem(viewComputerAction));
        mnuProject.add(createMenuItem(compilerSettingsAction));
        mainMenuBar.add(mnuProject);

        mnuHelp.setText("Help");
        mnuHelp.add(createMenuItem(aboutAction));
        mainMenuBar.add(mnuHelp);
        return mainMenuBar;
    }


    private static JMenuItem createMenuItem(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setToolTipText(null); // Swing annoyingly adds tool tip text to the menu item
        return item;
    }

    private void formWindowClosing() {
        editorPanel.dispose();
        dispose();
    }

    private void updateTitleOfSourceCodePanel() {
        editor.getCurrentFile().ifPresentOrElse(
            file -> tabbedPane.setTitleAt(0, file.getName()),
            () -> tabbedPane.setTitleAt(0, SOURCE_CODE_EDITOR)
        );
    }
}
