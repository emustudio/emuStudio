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

import net.emustudio.application.Constants;
import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.ConstantSizeButton;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.PagesPanel;
import net.emustudio.application.gui.debugtable.PaginatingDisassembler;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.gui.editor.FindText;
import net.emustudio.application.gui.editor.REditor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StudioFrame extends JFrame implements SearchListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(StudioFrame.class);
    private final static int MIN_COMPILER_OUTPUT_HEIGHT = 200;
    private final static int MIN_PERIPHERAL_PANEL_HEIGHT = 100;
    private final static double GOLDEN_RATIO = 1.6180339887;
    private final static String SOURCE_CODE_EDITOR = "Source code editor";

    private final VirtualComputer computer;
    private final EmulationController emulationController;
    private final ApplicationConfig applicationConfig;

    private final FindText finder = new FindText();
    private final Editor editor;
    private final JTable debugTable;
    private final DebugTableModel debugTableModel;
    private final Dialogs dialogs;

    private final MemoryContext<?> memoryContext;
    private final Memory.MemoryListener memoryListener;

    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    public StudioFrame(VirtualComputer computer, ApplicationConfig applicationConfig, Dialogs dialogs,
                       DebugTableModel debugTableModel, MemoryContext<?> memoryContext, String fileName) {
        this(computer, applicationConfig, dialogs, debugTableModel, memoryContext);
        editor.openFile(fileName);
    }

    public StudioFrame(VirtualComputer computer, ApplicationConfig applicationConfig, Dialogs dialogs,
                       DebugTableModel debugTableModel, MemoryContext<?> memoryContext) {
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.computer = Objects.requireNonNull(computer);
        this.debugTable = new DebugTableImpl(Objects.requireNonNull(debugTableModel));
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.memoryContext = memoryContext;

        this.emulationController = computer.getCPU().map(cpu -> new EmulationController(
            cpu, computer.getMemory().orElse(null), computer.getDevices()
        )).orElse(null);

        this.editor = computer.getCompiler()
            .map(compiler -> new REditor(dialogs, compiler))
            .orElse(new REditor(dialogs));

        this.memoryListener = new Memory.MemoryListener() {
            @Override
            public void memoryChanged(int memoryPosition) {
                debugTableModel.memoryChanged(memoryPosition, memoryPosition + 1);
                refreshDebugTable();
            }

            @Override
            public void memorySizeChanged() {
                if (memoryContext == null) {
                    debugTableModel.setMemorySize(0);
                } else {
                    debugTableModel.setMemorySize(memoryContext.getSize());
                }
            }
        };

        initComponents();

        btnMemory.setEnabled(computer.getMemory().filter(Memory::isShowSettingsSupported).isPresent());
        editorScrollPane.setViewportView(editor.getView());
        paneDebug.setViewportView(debugTable);
        paneDebug.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        setStatusGUI();
        lstDevices.setModel(new AbstractListModel<>() {
            private final List<Device> devices = computer.getDevices();

            @Override
            public int getSize() {
                return devices.size();
            }

            @Override
            public String getElementAt(int index) {
                return devices.get(index).getTitle();
            }
        });
        this.setTitle("emuStudio [" + computer.getComputerConfig().getName() + "]");

        pack();
        setupListeners();
        this.setLocationRelativeTo(null);
        editor.grabFocus();
        resizeComponents();
    }

    @Override
    public void searchEvent(SearchEvent e) {
        SearchEvent.Type type = e.getType();
        SearchContext context = e.getSearchContext();
        SearchResult result;
        TextEditorPane pane = editor.getView();

        switch (type) {
            default:
            case MARK_ALL:
                SearchEngine.markAll(pane, context);
                break;
            case FIND:
                result = SearchEngine.find(pane, context);
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(pane);
                }
                break;
            case REPLACE:
                result = SearchEngine.replace(pane, context);
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(pane);
                }
                break;
            case REPLACE_ALL:
                result = SearchEngine.replaceAll(pane, context);
                dialogs.showInfo(result.getCount() + " occurrences replaced.", "Replace all");
                break;
        }
    }

    @Override
    public String getSelectedText() {
        return editor.getView().getSelectedText();
    }

    private void setStatusGUI() {
        computer.getCPU().ifPresent(cpu -> {
            JPanel statusPanel = cpu.getStatusPanel();
            if (statusPanel == null) {
                return;
            }
            GroupLayout layout = new GroupLayout(this.statusWindow);
            this.statusWindow.setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(statusPanel));
            layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(statusPanel));
        });
    }

    private void setupListeners() {
        setupCompiler();
        setupCPU();
        setStateNotRunning(runState);

        setupWindowResized();
    }

    private void setupWindowResized() {
        splitSource.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponents();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
    }

    private void resizeComponents() {
        int height = getHeight();
        int newHeight = (int) (((double) height) / GOLDEN_RATIO);

        if (height - newHeight < MIN_COMPILER_OUTPUT_HEIGHT) {
            splitSource.setDividerLocation(height - MIN_COMPILER_OUTPUT_HEIGHT);
        } else {
            splitSource.setDividerLocation(newHeight);
        }

        double rowHeight = debugTable.getRowHeight();
        double additionalHeight = toolDebug.getHeight() + panelPages.getHeight() + 140;
        double heightTogether = additionalHeight + rowHeight * PaginatingDisassembler.INSTR_PER_PAGE;

        if (heightTogether + MIN_PERIPHERAL_PANEL_HEIGHT > height) {
            heightTogether = Math.max(0, height - MIN_PERIPHERAL_PANEL_HEIGHT);
        }

        double dividerLocation = Math.min(1.0, heightTogether / (double) height);

        splitPerDebug.setDividerLocation(dividerLocation);
    }

    private void setupCPU() {
        computer.getCPU().ifPresent(cpu -> {
            cpu.addCPUListener(new CPU.CPUListener() {

                @Override
                public void internalStateChanged() {
                    refreshDebugTable();
                }

                @Override
                public void runStateChanged(CPU.RunState state) {
                    runState = state;
                    if (state == CPU.RunState.STATE_RUNNING) {
                        setStateRunning();
                    } else {
                        setStateNotRunning(state);
                    }
                }
            });
            btnBreakpoint.setEnabled(cpu.isBreakpointSupported());
        });
    }

    private void setStateNotRunning(CPU.RunState state) {
        btnPause.setEnabled(false);
        if (state == CPU.RunState.STATE_STOPPED_BREAK) {
            btnStop.setEnabled(true);
            btnRunTime.setEnabled(true);
            btnRun.setEnabled(true);
            btnStep.setEnabled(true);
        } else {
            btnStop.setEnabled(false);
            btnRunTime.setEnabled(false);
            btnRun.setEnabled(false);
            btnStep.setEnabled(false);
            debugTableModel.currentPage();
        }
        btnBack.setEnabled(true);
        btnBeginning.setEnabled(true);
        paneDebug.setEnabled(true);
        debugTable.setEnabled(true);
        debugTable.setVisible(true);
        refreshDebugTable();

        memoryContext.addMemoryListener(memoryListener);
    }

    private void setStateRunning() {
        btnStop.setEnabled(true);
        btnBack.setEnabled(false);
        btnRun.setEnabled(false);
        btnStep.setEnabled(false);
        btnBeginning.setEnabled(false);
        btnPause.setEnabled(true);
        btnRunTime.setEnabled(false);
        debugTable.setEnabled(false);
        debugTable.setVisible(false);
        paneDebug.setEnabled(false);

        memoryContext.removeMemoryListener(memoryListener);
    }

    private void setupCompiler() {
        Optional<Compiler> compiler = computer.getCompiler();

        if (compiler.isPresent()) {
            compiler.get().addCompilerListener(new CompilerListener() {

                @Override
                public void onStart() {
                    compilerOutput.append("Compiling started...\n");
                }

                @Override
                public void onMessage(CompilerMessage message) {
                    compilerOutput.append(message.getFormattedMessage() + "\n");
                }

                @Override
                public void onFinish() {
                    compilerOutput.append("Compiling has finished.\n");
                }
            });
            if (!compiler.get().isShowSettingsSupported()) {
                mnuProjectCompilerSettings.setEnabled(false);
            }
        } else {
            btnCompile.setEnabled(false);
            mnuProjectCompile.setEnabled(false);
            mnuProjectCompilerSettings.setEnabled(false);
        }
    }

    private void refreshDebugTable() {
        if (debugTable.isEnabled()) {
            debugTable.revalidate();
            debugTable.repaint();
        }
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        JPanel panelSource = new JPanel();
        splitSource = new JSplitPane();
        editorScrollPane = new RTextScrollPane(editor.getView());
        JScrollPane compilerPane = new JScrollPane();
        compilerOutput = new JTextArea();
        JPanel panelEmulator = new JPanel();
        JSplitPane splitLeftRight = new JSplitPane();
        statusWindow = new JPanel();
        splitPerDebug = new JSplitPane();
        JPanel debuggerPanel = new JPanel();

        paneDebug = new JScrollPane();

        JPanel peripheralPanel = new JPanel();
        JScrollPane paneDevices = new JScrollPane();
        lstDevices = new JList<>();
        JButton btnShowGUI = new ConstantSizeButton();
        final JButton btnShowSettings = new ConstantSizeButton();

        panelPages = PagesPanel.create(debugTableModel, dialogs);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("emuStudio");
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                formWindowClosing();
            }
        });

        tabbedPane.setFocusable(false);
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(tabbedPane.getFont().getStyle() & ~java.awt.Font.BOLD));
        panelSource.setOpaque(true);

        JToolBar mainToolBar = setupMainToolbar();

        splitSource.setBorder(null);
        splitSource.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitSource.setOneTouchExpandable(true);
        splitSource.setLeftComponent(editorScrollPane);

        compilerOutput.setColumns(20);
        compilerOutput.setEditable(false);
        compilerOutput.setFont(Constants.MONOSPACED_PLAIN_12);
        compilerOutput.setLineWrap(true);
        compilerOutput.setRows(3);
        compilerOutput.setWrapStyleWord(true);

        compilerPane.setViewportView(compilerOutput);
        splitSource.setRightComponent(compilerPane);

        GroupLayout panelSourceLayout = new GroupLayout(panelSource);
        panelSource.setLayout(panelSourceLayout);
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
                .addComponent(mainToolBar, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
                .addComponent(splitSource, 10, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addContainerGap()
        );

        tabbedPane.addTab(SOURCE_CODE_EDITOR, panelSource);

        panelEmulator.setOpaque(true);

        splitLeftRight.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitLeftRight.setContinuousLayout(true);
        splitLeftRight.setFocusable(false);

        TitledBorder statusBorder = BorderFactory.createTitledBorder("Status");
        statusBorder.setTitleFont(statusBorder.getTitleFont().deriveFont(statusBorder.getTitleFont().getStyle() & ~Font.BOLD));
        statusWindow.setBorder(statusBorder);

        splitLeftRight.setRightComponent(statusWindow);

        splitPerDebug.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitPerDebug.setDividerLocation(500);
        splitPerDebug.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPerDebug.setAutoscrolls(true);
        splitPerDebug.setContinuousLayout(true);

        TitledBorder debuggerBorder = BorderFactory.createTitledBorder("Debugger");
        debuggerBorder.setTitleFont(debuggerBorder.getTitleFont().deriveFont(debuggerBorder.getTitleFont().getStyle() & ~Font.BOLD));
        debuggerPanel.setBorder(debuggerBorder);

        setupDebugToolbar();

        GroupLayout debuggerPanelLayout = new GroupLayout(debuggerPanel);
        debuggerPanel.setLayout(debuggerPanelLayout);
        debuggerPanelLayout.setHorizontalGroup(
            debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(toolDebug) //, GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                .addGroup(debuggerPanelLayout.createSequentialGroup().addComponent(panelPages))
                .addComponent(paneDebug, 10, 350, Short.MAX_VALUE));
        debuggerPanelLayout.setVerticalGroup(
            debuggerPanelLayout.createSequentialGroup()
                .addComponent(toolDebug, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                .addComponent(paneDebug, GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(panelPages)));
        splitLeftRight.setDividerLocation(1.0);
        splitPerDebug.setTopComponent(debuggerPanel);

        peripheralPanel.setBorder(BorderFactory.createTitledBorder("Peripheral devices"));

        paneDevices.setViewportView(lstDevices);
        lstDevices.setFont(lstDevices.getFont().deriveFont(lstDevices.getFont().getStyle() & ~java.awt.Font.BOLD));
        lstDevices.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int i = lstDevices.getSelectedIndex();
                if (i >= 0) {
                    btnShowSettings.setEnabled(computer.getDevices().get(i).isShowSettingsSupported());
                }

                if (e.getClickCount() == 2) {
                    showGUIButtonActionPerformed(new ActionEvent(this, 0, ""));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });

        btnShowSettings.setText("Settings");
        btnShowSettings.setFont(btnShowSettings.getFont().deriveFont(btnShowSettings.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnShowSettings.addActionListener(this::showSettingsButtonActionPerformed);

        btnShowGUI.setText("Show");
        btnShowGUI.setFont(btnShowGUI.getFont().deriveFont(btnShowGUI.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnShowGUI.addActionListener(this::showGUIButtonActionPerformed);

        GroupLayout peripheralPanelLayout = new GroupLayout(peripheralPanel);
        peripheralPanel.setLayout(peripheralPanelLayout);
        peripheralPanelLayout.setHorizontalGroup(
            peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(paneDevices).addGroup(GroupLayout.Alignment.TRAILING, peripheralPanelLayout.createSequentialGroup().addContainerGap().addComponent(btnShowSettings).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnShowGUI).addContainerGap()));
        peripheralPanelLayout.setVerticalGroup(
            peripheralPanelLayout.createSequentialGroup().addComponent(paneDevices).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(btnShowSettings).addComponent(btnShowGUI)));
        splitPerDebug.setRightComponent(peripheralPanel);
        splitLeftRight.setLeftComponent(splitPerDebug);

        GroupLayout panelEmulatorLayout = new GroupLayout(panelEmulator);
        panelEmulator.setLayout(panelEmulatorLayout);
        panelEmulatorLayout.setHorizontalGroup(
            panelEmulatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(splitLeftRight));
        panelEmulatorLayout.setVerticalGroup(
            panelEmulatorLayout.createSequentialGroup().addContainerGap().addComponent(splitLeftRight, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addContainerGap());

        tabbedPane.addTab("Emulator", panelEmulator);

        setJMenuBar(setupMainMenu());

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
        JMenuItem mnuFileNew = new JMenuItem();
        JMenuItem mnuFileOpen = new JMenuItem();
        JSeparator jSeparator3 = new JSeparator();
        JMenuItem mnuFileSave = new JMenuItem();
        JMenuItem mnuFileSaveAs = new JMenuItem();
        JSeparator jSeparator4 = new JSeparator();
        JMenuItem mnuFileExit = new JMenuItem();
        JMenu mnuEdit = new JMenu();
        JSeparator jSeparator6 = new JSeparator();
        JSeparator jSeparator5 = new JSeparator();
        JMenuItem mnuEditFind = new JMenuItem();
        JMenuItem mnuEditFindNext = new JMenuItem();
        JMenuItem mnuEditReplaceNext = new JMenuItem();
        JMenu mnuProject = new JMenu();
        mnuProjectCompile = new JMenuItem();
        JMenuItem mnuProjectViewConfig = new JMenuItem();
        mnuProjectCompilerSettings = new JMenuItem();
        JMenu mnuHelp = new JMenu();
        JMenuItem mnuHelpAbout = new JMenuItem();

        mnuFile.setText("File");
        mnuFile.setFont(mnuFile.getFont().deriveFont(mnuFile.getFont().getStyle() & ~java.awt.Font.BOLD));


        mnuFileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        mnuFileNew.setText("New");
        mnuFile.setFont(mnuFile.getFont().deriveFont(mnuFile.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileNew.setFont(mnuFileNew.getFont().deriveFont(mnuFileNew.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileNew.addActionListener(this::mnuFileNewActionPerformed);
        mnuFile.add(mnuFileNew);

        mnuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        mnuFileOpen.setText("Open...");
        mnuFileOpen.setFont(mnuFileOpen.getFont().deriveFont(mnuFileOpen.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileOpen.addActionListener(this::mnuFileOpenActionPerformed);
        mnuFile.add(mnuFileOpen);
        mnuFile.add(jSeparator3);

        mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        mnuFileSave.setText("Save");
        mnuFileSave.setFont(mnuFileSave.getFont().deriveFont(mnuFileSave.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileSave.addActionListener(this::mnuFileSaveActionPerformed);
        mnuFile.add(mnuFileSave);

        mnuFileSaveAs.setText("Save As...");
        mnuFileSaveAs.setFont(mnuFileSaveAs.getFont().deriveFont(mnuFileSaveAs.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileSaveAs.addActionListener(this::mnuFileSaveAsActionPerformed);
        mnuFile.add(mnuFileSaveAs);
        mnuFile.add(jSeparator4);

        mnuFileExit.setText("Exit");
        mnuFileExit.setFont(mnuFileExit.getFont().deriveFont(mnuFileExit.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileExit.addActionListener(this::mnuFileExitActionPerformed);
        mnuFile.add(mnuFileExit);

        mainMenuBar.add(mnuFile);

        mnuEdit.setText("Edit");
        mnuEdit.setFont(mnuEdit.getFont().deriveFont(mnuEdit.getFont().getStyle() & ~java.awt.Font.BOLD));

        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        mnuEdit.add(jSeparator6);
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        mnuEdit.add(createMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        mnuEdit.add(jSeparator5);

        mnuEditFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        mnuEditFind.setText("Find/replace text...");
        mnuEditFind.setFont(mnuEditFind.getFont().deriveFont(mnuEditFind.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditFind.addActionListener(this::mnuEditFindActionPerformed);
        mnuEdit.add(mnuEditFind);

        mnuEditFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        mnuEditFindNext.setText("Find next");
        mnuEditFindNext.setFont(mnuEditFindNext.getFont().deriveFont(mnuEditFindNext.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditFindNext.addActionListener(this::mnuEditFindNextActionPerformed);
        mnuEdit.add(mnuEditFindNext);

        mnuEditReplaceNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        mnuEditReplaceNext.setText("Replace next");
        mnuEditReplaceNext.setFont(mnuEditReplaceNext.getFont().deriveFont(mnuEditReplaceNext.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditReplaceNext.addActionListener(this::mnuEditReplaceNextActionPerformed);
        mnuEdit.add(mnuEditReplaceNext);

        mainMenuBar.add(mnuEdit);

        mnuProject.setText("Project");
        mnuProject.setFont(mnuProject.getFont().deriveFont(mnuProject.getFont().getStyle() & ~java.awt.Font.BOLD));

        mnuProjectCompile.setText("Compile source...");
        mnuProjectCompile.setFont(mnuProjectCompile.getFont().deriveFont(mnuProjectCompile.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuProjectCompile.addActionListener(this::mnuProjectCompileActionPerformed);
        mnuProject.add(mnuProjectCompile);

        mnuProjectViewConfig.setText("View computer...");
        mnuProjectViewConfig.setFont(mnuProjectViewConfig.getFont().deriveFont(mnuProjectViewConfig.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuProjectViewConfig.addActionListener(this::mnuProjectViewConfigActionPerformed);
        mnuProject.add(mnuProjectViewConfig);

        mnuProjectCompilerSettings.setText("Compiler settings...");
        mnuProjectCompilerSettings.setFont(mnuProjectCompilerSettings.getFont().deriveFont(mnuProjectCompilerSettings.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuProjectCompilerSettings.addActionListener(this::mnuProjectCompilerSettingsActionPerformed);
        mnuProject.add(mnuProjectCompilerSettings);

        mainMenuBar.add(mnuProject);

        mnuHelp.setText("Help");
        mnuHelp.setFont(mnuHelp.getFont().deriveFont(mnuHelp.getFont().getStyle() & ~java.awt.Font.BOLD));

        mnuHelpAbout.setText("About...");
        mnuHelpAbout.setFont(mnuHelpAbout.getFont().deriveFont(mnuHelpAbout.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuHelpAbout.addActionListener(this::mnuHelpAboutActionPerformed);
        mnuHelp.add(mnuHelpAbout);

        mainMenuBar.add(mnuHelp);
        return mainMenuBar;
    }

    private JToolBar setupMainToolbar() {
        JToolBar mainToolBar = new JToolBar();

        JSeparator separator1 = new JSeparator();
        JSeparator separator2 = new JSeparator();
        JSeparator separator3 = new JSeparator();

        ToolbarButton btnNew = new ToolbarButton(
            this::btnNewActionPerformed,
            "/net/emustudio/application/gui/dialogs/document-new.png",
            "New file"
        );
        ToolbarButton btnOpen = new ToolbarButton(
            this::btnOpenActionPerformed,
            "/net/emustudio/application/gui/dialogs/document-open.png",
            "Open file"
        );
        ToolbarButton btnSave = new ToolbarButton(
            this::btnSaveActionPerformed,
            "/net/emustudio/application/gui/dialogs/document-save.png",
            "Save file"
        );

        ToolbarButton btnCut = new ToolbarButton(
            RTextArea.getAction(RTextArea.CUT_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-cut.png",
            "Cut selection"
        );
        ToolbarButton btnCopy = new ToolbarButton(
            RTextArea.getAction(RTextArea.COPY_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-copy.png",
            "Copy selection"
        );
        ToolbarButton btnPaste = new ToolbarButton(
            RTextArea.getAction(RTextArea.PASTE_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-paste.png",
            "Paste from clipboard"
        );

        ToolbarButton btnFindReplace = new ToolbarButton(
            this::btnFindReplaceActionPerformed,
            "/net/emustudio/application/gui/dialogs/edit-find-replace.png",
            "Find/replace text..."
        );

        ToolbarButton btnUndo = new ToolbarButton(
            RTextArea.getAction(RTextArea.UNDO_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-undo.png",
            "Undo"
        );
        ToolbarButton btnRedo = new ToolbarButton(RTextArea.getAction(RTextArea.REDO_ACTION),
            "/net/emustudio/application/gui/dialogs/edit-redo.png",
            "Redo"
        );

        btnCompile = new ToolbarButton(
            this::btnCompileActionPerformed,
            "/net/emustudio/application/gui/dialogs/compile.png",
            "Compile source"
        );

        mainToolBar.setFloatable(false);
        mainToolBar.setBorderPainted(false);
        mainToolBar.setRollover(true);

        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMaximumSize(new java.awt.Dimension(10, 32768));
        separator1.setPreferredSize(new java.awt.Dimension(10, 10));

        separator2.setOrientation(SwingConstants.VERTICAL);
        separator2.setMaximumSize(new java.awt.Dimension(10, 32767));

        separator3.setOrientation(SwingConstants.VERTICAL);
        separator3.setMaximumSize(new java.awt.Dimension(10, 32767));

        mainToolBar.add(btnNew);
        mainToolBar.add(btnOpen);
        mainToolBar.add(btnSave);
        mainToolBar.add(separator1);
        mainToolBar.add(btnUndo);
        mainToolBar.add(btnRedo);
        mainToolBar.add(separator2);
        mainToolBar.add(btnFindReplace);
        mainToolBar.add(btnCut);
        mainToolBar.add(btnCopy);
        mainToolBar.add(btnPaste);
        mainToolBar.add(separator3);
        mainToolBar.add(btnCompile);

        return mainToolBar;
    }


    private void setupDebugToolbar() {
        toolDebug = new JToolBar();
        ToolbarButton btnReset = new ToolbarButton(
            this::btnResetActionPerformed,
            "/net/emustudio/application/gui/dialogs/reset.png",
            "Reset emulation"
        );
        btnBeginning = new ToolbarButton(
            this::btnBeginningActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-first.png",
            "Jump to beginning"
        );
        btnBack = new ToolbarButton(
            this::btnBackActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-previous.png",
            "Step back"
        );
        btnStop = new ToolbarButton(
            this::btnStopActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-stop.png",
            "Stop emulation"
        );
        btnPause = new ToolbarButton(
            this::btnPauseActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-pause.png",
            "Pause emulation"
        );
        btnRun = new ToolbarButton(
            this::btnRunActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-play.png",
            "Run emulation"
        );
        btnRunTime = new ToolbarButton(
            this::btnRunTimeActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-play-time.png",
            "Run emulation in time slices"
        );
        btnStep = new ToolbarButton(
            this::btnStepActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-next.png",
            "Step forward"
        );
        ToolbarButton btnJump = new ToolbarButton(
            this::btnJumpActionPerformed,
            "/net/emustudio/application/gui/dialogs/go-jump.png",
            "Jump to address"
        );
        btnBreakpoint = new ToolbarButton(
            this::btnBreakpointActionPerformed,
            "/net/emustudio/application/gui/dialogs/breakpoints.png",
            "Set/unset breakpoint to address..."
        );
        btnMemory = new ToolbarButton(
            this::btnMemoryActionPerformed,
            "/net/emustudio/application/gui/dialogs/grid_memory.gif",
            "Show operating memory"
        );

        toolDebug.setFloatable(false);
        toolDebug.setRollover(true);
        toolDebug.setBorder(null);
        toolDebug.setBorderPainted(false);

        toolDebug.add(btnReset);
        toolDebug.add(btnBeginning);
        toolDebug.add(btnBack);
        toolDebug.add(btnStop);
        toolDebug.add(btnPause);
        toolDebug.add(btnRun);
        toolDebug.add(btnRunTime);
        toolDebug.add(btnStep);
        toolDebug.add(btnJump);
        toolDebug.add(btnBreakpoint);
        toolDebug.add(btnMemory);
    }


    private static JMenuItem createMenuItem(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setToolTipText(null); // Swing annoyingly adds tool tip text to the menu item
        return item;
    }

    private void btnPauseActionPerformed(ActionEvent evt) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::pause);
    }

    private void showSettingsButtonActionPerformed(ActionEvent evt) {
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                dialogs.showError("Device has to be selected!", "Show device settings");
            } else {
                computer.getDevices().get(i).showSettings();
            }
        } catch (Exception e) {
            LOGGER.error("Cannot show device settings.", e);
            dialogs.showError("Unexpected error. Please see log file for details", "Show device settings");
        }
    }

    private void showGUIButtonActionPerformed(ActionEvent evt) {
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                dialogs.showError("Device has to be selected!", "Show device");
            } else {
                computer.getDevices().get(i).showGUI();
            }
        } catch (Exception e) {
            LOGGER.error("Cannot show device.", e);
            dialogs.showError("Unexpected error. Please see log file for details", "Show device");
        }
    }

    private void btnMemoryActionPerformed(ActionEvent evt) {
        computer.getMemory()
            .filter(Memory::isShowSettingsSupported)
            .ifPresentOrElse(
                Plugin::showSettings,
                () -> dialogs.showInfo("Memory GUI is not supported", "Show Memory")
            );
    }

    private void btnStepActionPerformed(ActionEvent evt) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::step);
    }

    private void btnRunActionPerformed(ActionEvent evt) {
        Optional.ofNullable(emulationController).ifPresent(c -> {
            debugTable.setEnabled(false);
            c.start();
        });
    }

    private void btnRunTimeActionPerformed(ActionEvent evt) {
        Optional.ofNullable(emulationController).ifPresent(c -> {
            try {
                dialogs
                    .readInteger("Enter time slice in milliseconds:", "Timed emulation", 500)
                    .ifPresent(sliceMillis -> c.step(sliceMillis, TimeUnit.MILLISECONDS));
            } catch (NumberFormatException e) {
                dialogs.showError("Invalid number format", "Timed emulation");
            }
        });
    }

    private void btnStopActionPerformed(ActionEvent evt) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::stop);
    }

    private void btnBackActionPerformed(ActionEvent evt) {
        computer.getCPU().ifPresent(cpu -> {
            int pc = cpu.getInstructionLocation();
            if (pc > 0) {
                cpu.setInstructionLocation(debugTableModel.guessPreviousInstructionLocation());
                paneDebug.revalidate();
                refreshDebugTable();
            }
        });
    }

    private void btnBeginningActionPerformed(ActionEvent evt) {
        computer.getCPU().ifPresent(cpu -> {
            cpu.setInstructionLocation(0);
            paneDebug.revalidate();
            refreshDebugTable();
        });
    }

    private void btnResetActionPerformed(ActionEvent evt) {
        Optional.ofNullable(emulationController).ifPresent(EmulationController::reset);
    }

    private void btnJumpActionPerformed(ActionEvent evt) {
        computer.getCPU().ifPresentOrElse(cpu -> {
            try {
                dialogs
                    .readInteger("Memory address:", "Jump to address", 0)
                    .ifPresent(address -> {
                        if (!cpu.setInstructionLocation(address)) {
                            int memorySize = computer.getMemory().map(Memory::getSize).orElse(0);
                            String maxSizeMessage = (memorySize == 0) ? "" : "(probably accepts range from 0 to " + memorySize + ")";
                            dialogs.showError("Invalid memory address" + maxSizeMessage);
                        } else {
                            refreshDebugTable();
                        }
                    });
            } catch (NumberFormatException e) {
                dialogs.showError("Invalid address format", "Jump to address");
            }
        }, () -> dialogs.showInfo("CPU is not set", "Jump to address"));
    }

    private void mnuHelpAboutActionPerformed(ActionEvent evt) {
        (new AboutDialog(this)).setVisible(true);
    }

    private void mnuProjectCompileActionPerformed(ActionEvent evt) {
        btnCompileActionPerformed(evt);
    }

    private void btnCompileActionPerformed(ActionEvent evt) {
        computer.getCompiler().ifPresentOrElse(compiler -> {
            if (runState == CPU.RunState.STATE_RUNNING) {
                dialogs.showError("Emulation must be stopped first.", "Compile");
            } else if (editor.saveFile()) {
                updateTitleOfSourceCodePanel();

                editor.getCurrentFile().ifPresent(file -> {
                    compilerOutput.setText("");

                    try {
                        computer.getMemory().ifPresent(Memory::reset);
                        compiler.compile(file.getAbsolutePath());
                        int programStart = compiler.getProgramLocation();

                        computer.getMemory().ifPresent(memory -> memory.setProgramLocation(programStart));

                        computer.getCPU().ifPresent(cpu -> cpu.reset(programStart));
                    } catch (Exception e) {
                        compilerOutput.append("Could not compile file: " + e.toString() + "\n");
                    }
                });
            }
        }, () -> dialogs.showError("Compiler is not set", "Compile"));
    }

    private void mnuProjectViewConfigActionPerformed(ActionEvent evt) {
        new ViewComputerDialog(this, computer, applicationConfig, dialogs).setVisible(true);
    }

    private void mnuProjectCompilerSettingsActionPerformed(ActionEvent evt) {
        Optional<Compiler> compiler = computer.getCompiler();
        if (compiler.isPresent() && (compiler.get().isShowSettingsSupported())) {
            compiler.get().showSettings();
        }
    }

    private void mnuFileSaveAsActionPerformed(ActionEvent evt) {
        if (editor.saveFileAs()) {
            updateTitleOfSourceCodePanel();
        }
    }

    private void mnuFileSaveActionPerformed(ActionEvent evt) {
        btnSaveActionPerformed(evt);
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        if (editor.saveFile()) {
            updateTitleOfSourceCodePanel();
        }
    }

    private void mnuFileOpenActionPerformed(ActionEvent evt) {
        btnOpenActionPerformed(evt);
    }

    private void mnuFileNewActionPerformed(ActionEvent evt) {
        btnNewActionPerformed(evt);
    }

    private void mnuFileExitActionPerformed(ActionEvent evt) {
        this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void formWindowClosing() {
        if (confirmSave()) {
            Optional.ofNullable(emulationController).ifPresent(EmulationController::close);
            computer.close();
            dispose();
            System.exit(0); //calling the method is a must
        }
    }

    private void btnOpenActionPerformed(ActionEvent evt) {
        if (confirmSave() && editor.openFile()) {
            compilerOutput.setText("");
            updateTitleOfSourceCodePanel();
        }
    }

    private void btnNewActionPerformed(ActionEvent evt) {
        if (confirmSave()) {
            editor.newFile();
            compilerOutput.setText("");
            updateTitleOfSourceCodePanel();
        }
    }

    private void btnFindReplaceActionPerformed(ActionEvent evt) {
        mnuEditFindActionPerformed(evt);
    }

    private void mnuEditFindActionPerformed(ActionEvent evt) {
        ReplaceDialog replaceDialog = new ReplaceDialog(this, this);
        replaceDialog.setVisible(true);
    }

    private void mnuEditFindNextActionPerformed(ActionEvent evt) {
        try {
            if (finder.findNext(editor.getText(),
                editor.getCaretPosition(),
                editor.getDocument().getEndPosition().getOffset() - 1)) {
                editor.select(finder.getMatchStart(), finder.getMatchEnd());
                editor.grabFocus();
            } else {
                dialogs.showError("Text was not found", "Find next");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }

    private void btnBreakpointActionPerformed(ActionEvent evt) {
        computer.getCPU().ifPresent(cpu -> {
            BreakpointDialog bDialog = new BreakpointDialog(this, dialogs);
            bDialog.setVisible(true);
            int address = bDialog.getAddress();

            if ((address != -1) && cpu.isBreakpointSupported()) {
                if (bDialog.isSet()) {
                    cpu.setBreakpoint(address);
                } else {
                    cpu.unsetBreakpoint(address);
                }
            }
            paneDebug.revalidate();
            refreshDebugTable();
        });
    }

    private void mnuEditReplaceNextActionPerformed(ActionEvent evt) {
        try {
            if (finder.replaceNext(editor)) {
                editor.grabFocus();
            } else {
                dialogs.showError("Text was not found", "Replace next");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }

    private void updateTitleOfSourceCodePanel() {
        editor.getCurrentFile().ifPresentOrElse(
            file -> tabbedPane.setTitleAt(0, SOURCE_CODE_EDITOR + " (" + file.getName() + ")"),
            () -> tabbedPane.setTitleAt(0, SOURCE_CODE_EDITOR)
        );
    }

    private boolean confirmSave() {
        if (editor.isDirty()) {
            Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
            if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                return editor.saveFile();
            } else return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
        }
        return true;
    }

    private ToolbarButton btnBack;
    private ToolbarButton btnBeginning;
    private ToolbarButton btnBreakpoint;
    private ToolbarButton btnPause;
    private ToolbarButton btnRun;
    private ToolbarButton btnRunTime;
    private ToolbarButton btnStep;
    private ToolbarButton btnStop;
    private RTextScrollPane editorScrollPane;
    private JList<String> lstDevices;
    private JScrollPane paneDebug;
    private JPanel statusWindow;
    private JTextArea compilerOutput;
    private JMenuItem mnuProjectCompilerSettings;
    private ToolbarButton btnMemory;
    private ToolbarButton btnCompile;
    private JMenuItem mnuProjectCompile;
    private JSplitPane splitSource;
    private JSplitPane splitPerDebug;
    private JToolBar toolDebug;
    private JPanel panelPages;
    private JTabbedPane tabbedPane;
}
