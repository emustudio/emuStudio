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
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.PagesPanel;
import net.emustudio.application.gui.debugtable.PaginatingDisassembler;
import net.emustudio.application.gui.editor.FindText;
import net.emustudio.application.gui.editor.SourceCodeEditor;
import net.emustudio.application.gui.editor.SourceCodeEditor.UndoActionListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StudioFrame extends JFrame {
    private final static Logger LOGGER = LoggerFactory.getLogger(StudioFrame.class);
    private final static int MIN_COMPILER_OUTPUT_HEIGHT = 200;
    private final static int MIN_PERIPHERAL_PANEL_HEIGHT = 100;
    private final static double GOLDEN_RATIO = 1.6180339887;
    private final static String SOURCE_CODE_EDITOR = "Source code editor";

    private final VirtualComputer computer;
    private final EmulationController emulationController;
    private final ApplicationConfig applicationConfig;

    private final Clipboard systemClipboard;
    private final FindText finder = new FindText();
    private final SourceCodeEditor txtSource;
    private final JTable debugTable;
    private final DebugTableModel debugTableModel;
    private final Dialogs dialogs;

    private final MemoryContext<?> memoryContext;
    private final Memory.MemoryListener memoryListener;

    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    public StudioFrame(VirtualComputer computer, ApplicationConfig applicationConfig, Dialogs dialogs, JTable debugTable,
                       MemoryContext<?> memoryContext, String fileName) {
        this(computer, applicationConfig, dialogs, debugTable, memoryContext);
        txtSource.openFile(fileName);
    }

    public StudioFrame(VirtualComputer computer, ApplicationConfig applicationConfig, Dialogs dialogs, JTable debugTable,
                       MemoryContext<?> memoryContext) {
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.computer = Objects.requireNonNull(computer);
        this.debugTable = Objects.requireNonNull(debugTable);
        this.debugTableModel = Objects.requireNonNull((DebugTableModel) debugTable.getModel());
        this.dialogs = Objects.requireNonNull(dialogs);
        this.memoryContext = memoryContext;

        this.emulationController = new EmulationController(
            computer.getCPU().orElse(null), computer.getMemory().orElse(null), computer.getDevices()
        );

        txtSource = new SourceCodeEditor(computer.getCompiler().orElse(null), dialogs);
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        memoryListener = new Memory.MemoryListener() {
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
        editorScrollPane.setViewportView(txtSource);
        paneDebug.setViewportView(debugTable);
        paneDebug.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        this.setStatusGUI();
        pack();

        setupListeners();

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
        this.setLocationRelativeTo(null);
        this.setTitle("emuStudio [" + computer.getComputerConfig().getName() + "]");
        txtSource.grabFocus();

        resizeComponents();
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
        setupClipboard();
        setupUndoRedo();

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

    private void setupUndoRedo() {
        UndoActionListener undoStateListener = new UndoActionListener() {

            @Override
            public void undoStateChanged(boolean canUndo, String presentationName) {
                mnuEditUndo.setEnabled(canUndo);
                btnUndo.setEnabled(canUndo);
                btnUndo.setToolTipText(presentationName);
            }

            @Override
            public void redoStateChanged(boolean canRedo, String presentationName) {
                mnuEditRedo.setEnabled(canRedo);
                btnRedo.setEnabled(canRedo);
                btnRedo.setToolTipText(presentationName);
            }
        };
        txtSource.setUndoActionListener(undoStateListener);
    }

    private void setupClipboard() {
        if (systemClipboard.getContents(null) != null) {
            btnPaste.setEnabled(true);
            mnuEditPaste.setEnabled(true);
        }
        systemClipboard.addFlavorListener(e -> {
            if (systemClipboard.getContents(null) == null) {
                btnPaste.setEnabled(false);
                mnuEditPaste.setEnabled(false);
            } else {
                btnPaste.setEnabled(true);
                mnuEditPaste.setEnabled(true);
            }
        });
        txtSource.addCaretListener(e -> {
            if (e.getDot() == e.getMark()) {
                btnCut.setEnabled(false);
                mnuEditCut.setEnabled(false);
                btnCopy.setEnabled(false);
                mnuEditCopy.setEnabled(false);
            } else {
                btnCut.setEnabled(true);
                mnuEditCut.setEnabled(true);
                btnCopy.setEnabled(true);
                mnuEditCopy.setEnabled(true);
            }
        });
    }

    private void setupCompiler() {
        Optional<Compiler> compiler = computer.getCompiler();

        if (compiler.isPresent()) {
            compiler.get().addCompilerListener(new CompilerListener() {

                @Override
                public void onStart() {
                    txtOutput.append("Compiling started...\n");
                }

                @Override
                public void onMessage(CompilerMessage message) {
                    txtOutput.append(message.getFormattedMessage() + "\n");
                }

                @Override
                public void onFinish() {
                    txtOutput.append("Compiling has finished.\n");
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
        JToolBar toolStandard = new JToolBar();
        JButton btnNew = new JButton();
        JButton btnOpen = new JButton();
        JButton btnSave = new JButton();
        JSeparator jSeparator1 = new JSeparator();
        btnCut = new JButton();
        btnCopy = new JButton();
        btnPaste = new JButton();
        JButton btnFindReplace = new JButton();
        btnUndo = new JButton();
        btnRedo = new JButton();
        JSeparator jSeparator2 = new JSeparator();
        btnCompile = new JButton();
        splitSource = new JSplitPane();
        editorScrollPane = new JScrollPane();
        JScrollPane compilerPane = new JScrollPane();
        txtOutput = new JTextArea();
        JPanel panelEmulator = new JPanel();
        JSplitPane splitLeftRight = new JSplitPane();
        statusWindow = new JPanel();
        splitPerDebug = new JSplitPane();
        JPanel debuggerPanel = new JPanel();
        toolDebug = new JToolBar();
        JButton btnReset = new JButton();
        btnBeginning = new JButton();
        btnBack = new JButton();
        btnStop = new JButton();
        btnPause = new JButton();
        btnRun = new JButton();
        btnRunTime = new JButton();
        btnStep = new JButton();
        JButton btnJump = new JButton();
        btnBreakpoint = new JButton();
        btnMemory = new JButton();
        paneDebug = new JScrollPane();

        JPanel peripheralPanel = new JPanel();
        JScrollPane paneDevices = new JScrollPane();
        lstDevices = new JList<>();
        JButton btnShowGUI = new ConstantSizeButton();
        final JButton btnShowSettings = new ConstantSizeButton();
        JMenuBar jMenuBar2 = new JMenuBar();
        JMenu mnuFile = new JMenu();
        JMenuItem mnuFileNew = new JMenuItem();
        JMenuItem mnuFileOpen = new JMenuItem();
        JSeparator jSeparator3 = new JSeparator();
        JMenuItem mnuFileSave = new JMenuItem();
        JMenuItem mnuFileSaveAs = new JMenuItem();
        JSeparator jSeparator4 = new JSeparator();
        JMenuItem mnuFileExit = new JMenuItem();
        JMenu mnuEdit = new JMenu();
        mnuEditUndo = new JMenuItem();
        mnuEditRedo = new JMenuItem();
        JSeparator jSeparator6 = new JSeparator();
        mnuEditCut = new JMenuItem();
        mnuEditCopy = new JMenuItem();
        mnuEditPaste = new JMenuItem();
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
        JSeparator jSeparator7 = new JSeparator();
        panelPages = PagesPanel.create(debugTableModel);

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

        toolStandard.setFloatable(false);
        toolStandard.setBorderPainted(false);
        toolStandard.setRollover(true);

        btnNew.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/document-new.png"))); // NOI18N
        btnNew.setToolTipText("New file");
        btnNew.setFocusable(false);
        btnNew.addActionListener(this::btnNewActionPerformed);
        btnNew.setBorderPainted(false);

        btnOpen.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/document-open.png"))); // NOI18N
        btnOpen.setToolTipText("Open file");
        btnOpen.setFocusable(false);
        btnOpen.addActionListener(this::btnOpenActionPerformed);
        btnOpen.setBorderPainted(false);

        btnSave.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/document-save.png"))); // NOI18N
        btnSave.setToolTipText("Save file");
        btnSave.setFocusable(false);
        btnSave.addActionListener(this::btnSaveActionPerformed);
        btnSave.setBorderPainted(false);

        jSeparator1.setOrientation(SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(10, 32768));
        jSeparator1.setPreferredSize(new java.awt.Dimension(10, 10));

        btnCut.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-cut.png"))); // NOI18N
        btnCut.setToolTipText("Cut selection");
        btnCut.setEnabled(false);
        btnCut.setFocusable(false);
        btnCut.addActionListener(this::btnCutActionPerformed);
        btnCut.setBorderPainted(false);

        btnCopy.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-copy.png"))); // NOI18N
        btnCopy.setToolTipText("Copy selection");
        btnCopy.setEnabled(false);
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(this::btnCopyActionPerformed);
        btnCopy.setBorderPainted(false);

        btnPaste.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-paste.png"))); // NOI18N
        btnPaste.setToolTipText("Paste selection");
        btnPaste.setEnabled(false);
        btnPaste.setFocusable(false);
        btnPaste.addActionListener(this::btnPasteActionPerformed);
        btnPaste.setBorderPainted(false);

        btnFindReplace.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-find-replace.png"))); // NOI18N
        btnFindReplace.setToolTipText("Find/replace text...");
        btnFindReplace.setFocusable(false);
        btnFindReplace.addActionListener(this::btnFindReplaceActionPerformed);
        btnFindReplace.setBorderPainted(false);

        btnUndo.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-undo.png"))); // NOI18N
        btnUndo.setToolTipText("Undo");
        btnUndo.setEnabled(false);
        btnUndo.setFocusable(false);
        btnUndo.addActionListener(this::btnUndoActionPerformed);
        btnUndo.setBorderPainted(false);

        btnRedo.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-redo.png"))); // NOI18N
        btnRedo.setToolTipText("Redo");
        btnRedo.setEnabled(false);
        btnRedo.setFocusable(false);
        btnRedo.addActionListener(this::btnRedoActionPerformed);
        btnRedo.setBorderPainted(false);

        jSeparator2.setOrientation(SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(10, 32767));

        jSeparator7.setOrientation(SwingConstants.VERTICAL);
        jSeparator7.setMaximumSize(new java.awt.Dimension(10, 32767));

        btnCompile.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/compile.png"))); // NOI18N
        btnCompile.setToolTipText("Compile source");
        btnCompile.setFocusable(false);
        btnCompile.addActionListener(this::btnCompileActionPerformed);
        btnCompile.setBorderPainted(false);

        toolStandard.add(btnNew);
        toolStandard.add(btnOpen);
        toolStandard.add(btnSave);
        toolStandard.add(jSeparator1);
        toolStandard.add(btnUndo);
        toolStandard.add(btnRedo);
        toolStandard.add(jSeparator2);
        toolStandard.add(btnFindReplace);
        toolStandard.add(btnCut);
        toolStandard.add(btnCopy);
        toolStandard.add(btnPaste);
        toolStandard.add(jSeparator7);
        toolStandard.add(btnCompile);

        splitSource.setBorder(null);
        splitSource.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitSource.setOneTouchExpandable(true);
        splitSource.setLeftComponent(editorScrollPane);

        txtOutput.setColumns(20);
        txtOutput.setEditable(false);
        txtOutput.setFont(Constants.MONOSPACED_PLAIN_12);
        txtOutput.setLineWrap(true);
        txtOutput.setRows(3);
        txtOutput.setWrapStyleWord(true);

        compilerPane.setViewportView(txtOutput);
        splitSource.setRightComponent(compilerPane);

        GroupLayout panelSourceLayout = new GroupLayout(panelSource);
        panelSource.setLayout(panelSourceLayout);
        panelSourceLayout.setHorizontalGroup(
            panelSourceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(toolStandard)
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
                .addComponent(toolStandard, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
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

        toolDebug.setFloatable(false);
        toolDebug.setRollover(true);
        toolDebug.setBorder(null);
        toolDebug.setBorderPainted(false);

        btnReset.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/reset.png"))); // NOI18N
        btnReset.setToolTipText("Reset emulation");
        btnReset.setFocusable(false);
        btnReset.addActionListener(this::btnResetActionPerformed);
        btnReset.setBorderPainted(false);

        btnBeginning.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-first.png"))); // NOI18N
        btnBeginning.setToolTipText("Jump to beginning");
        btnBeginning.setFocusable(false);
        btnBeginning.addActionListener(this::btnBeginningActionPerformed);
        btnBeginning.setBorderPainted(false);

        btnBack.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-previous.png"))); // NOI18N
        btnBack.setToolTipText("Step back");
        btnBack.setFocusable(false);
        btnBack.addActionListener(this::btnBackActionPerformed);
        btnBack.setBorderPainted(false);

        btnStop.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-stop.png"))); // NOI18N
        btnStop.setToolTipText("Stop emulation");
        btnStop.setFocusable(false);
        btnStop.addActionListener(this::btnStopActionPerformed);
        btnStop.setBorderPainted(false);

        btnPause.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-pause.png"))); // NOI18N
        btnPause.setToolTipText("Pause emulation");
        btnPause.setFocusable(false);
        btnPause.addActionListener(this::btnPauseActionPerformed);
        btnPause.setBorderPainted(false);

        btnRun.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-play.png"))); // NOI18N
        btnRun.setToolTipText("Run emulation");
        btnRun.setFocusable(false);
        btnRun.addActionListener(this::btnRunActionPerformed);
        btnRun.setBorderPainted(false);

        btnRunTime.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-play-time.png"))); // NOI18N
        btnRunTime.setToolTipText("Run emulation in time slices");
        btnRunTime.setFocusable(false);
        btnRunTime.addActionListener(this::btnRunTimeActionPerformed);
        btnRunTime.setBorderPainted(false);

        btnStep.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-next.png"))); // NOI18N
        btnStep.setToolTipText("Step forward");
        btnStep.setFocusable(false);
        btnStep.addActionListener(this::btnStepActionPerformed);
        btnStep.setBorderPainted(false);

        btnJump.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-jump.png"))); // NOI18N
        btnJump.setToolTipText("Jump to address");
        btnJump.setFocusable(false);
        btnJump.addActionListener(this::btnJumpActionPerformed);
        btnJump.setBorderPainted(false);

        btnBreakpoint.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/breakpoints.png"))); // NOI18N
        btnBreakpoint.setToolTipText("Set/unset breakpoint to address...");
        btnBreakpoint.setFocusable(false);
        btnBreakpoint.setHorizontalTextPosition(SwingConstants.CENTER);
        btnBreakpoint.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnBreakpoint.addActionListener(this::btnBreakpointActionPerformed);
        btnBreakpoint.setBorderPainted(false);

        btnMemory.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/grid_memory.gif"))); // NOI18N
        btnMemory.setToolTipText("Show operating memory");
        btnMemory.setFocusable(false);
        btnMemory.addActionListener(this::btnMemoryActionPerformed);
        btnMemory.setBorderPainted(false);

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

        jMenuBar2.add(mnuFile);

        mnuEdit.setText("Edit");
        mnuEdit.setFont(mnuEdit.getFont().deriveFont(mnuEdit.getFont().getStyle() & ~java.awt.Font.BOLD));

        mnuEditUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        mnuEditUndo.setText("Undo");
        mnuEditUndo.setFont(mnuEditUndo.getFont().deriveFont(mnuEditUndo.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditUndo.setEnabled(false);
        mnuEditUndo.addActionListener(this::mnuEditUndoActionPerformed);
        mnuEdit.add(mnuEditUndo);

        mnuEditRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        mnuEditRedo.setText("Redo");
        mnuEditRedo.setEnabled(false);
        mnuEditRedo.setFont(mnuEditRedo.getFont().deriveFont(mnuEditRedo.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditRedo.addActionListener(this::mnuEditRedoActionPerformed);
        mnuEdit.add(mnuEditRedo);
        mnuEdit.add(jSeparator6);

        mnuEditCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        mnuEditCut.setText("Cut selection");
        mnuEditCut.setEnabled(false);
        mnuEditCut.setFont(mnuEditCut.getFont().deriveFont(mnuEditCut.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditCut.addActionListener(this::mnuEditCutActionPerformed);
        mnuEdit.add(mnuEditCut);

        mnuEditCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        mnuEditCopy.setText("Copy selection");
        mnuEditCopy.setEnabled(false);
        mnuEditCopy.setFont(mnuEditCopy.getFont().deriveFont(mnuEditCopy.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditCopy.addActionListener(this::mnuEditCopyActionPerformed);
        mnuEdit.add(mnuEditCopy);

        mnuEditPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        mnuEditPaste.setText("Paste selection");
        mnuEditPaste.setEnabled(false);
        mnuEditPaste.setFont(mnuEditPaste.getFont().deriveFont(mnuEditPaste.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditPaste.addActionListener(this::mnuEditPasteActionPerformed);
        mnuEdit.add(mnuEditPaste);
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

        jMenuBar2.add(mnuEdit);

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

        jMenuBar2.add(mnuProject);

        mnuHelp.setText("Help");
        mnuHelp.setFont(mnuHelp.getFont().deriveFont(mnuHelp.getFont().getStyle() & ~java.awt.Font.BOLD));

        mnuHelpAbout.setText("About...");
        mnuHelpAbout.setFont(mnuHelpAbout.getFont().deriveFont(mnuHelpAbout.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuHelpAbout.addActionListener(this::mnuHelpAboutActionPerformed);
        mnuHelp.add(mnuHelpAbout);

        jMenuBar2.add(mnuHelp);

        setJMenuBar(jMenuBar2);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
    }

    private void btnPauseActionPerformed(ActionEvent evt) {
        emulationController.pause();
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
        emulationController.step();
    }

    private void btnRunActionPerformed(ActionEvent evt) {
        debugTable.setEnabled(false);
        emulationController.start();
    }

    private void btnRunTimeActionPerformed(ActionEvent evt) {
        try {
            dialogs
                .readInteger("Enter time slice in milliseconds:", "Timed emulation", 500)
                .ifPresent(sliceMillis -> emulationController.step(sliceMillis, TimeUnit.MILLISECONDS));
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format", "Timed emulation");
        }
    }

    private void btnStopActionPerformed(ActionEvent evt) {
        emulationController.stop();
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
        emulationController.reset();
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
            } else if (txtSource.saveFile(true)) {
                updateTitleOfSourceCodePanel();

                txtSource.getCurrentFile().ifPresent(file -> {
                    txtOutput.setText("");

                    try {
                        computer.getMemory().ifPresent(Memory::reset);
                        compiler.compile(file.getAbsolutePath());
                        int programStart = compiler.getProgramLocation();

                        computer.getMemory().ifPresent(memory -> memory.setProgramLocation(programStart));

                        computer.getCPU().ifPresent(cpu -> cpu.reset(programStart));
                    } catch (Exception e) {
                        txtOutput.append("Could not compile file: " + e.toString() + "\n");
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

    private void mnuEditPasteActionPerformed(ActionEvent evt) {
        btnPasteActionPerformed(evt);
    }

    private void mnuEditCopyActionPerformed(ActionEvent evt) {
        btnCopyActionPerformed(evt);
    }

    private void mnuEditCutActionPerformed(ActionEvent evt) {
        btnCutActionPerformed(evt);
    }

    private void mnuEditRedoActionPerformed(ActionEvent evt) {
        btnRedoActionPerformed(evt);
    }

    private void mnuEditUndoActionPerformed(ActionEvent evt) {
        btnUndoActionPerformed(evt);
    }

    private void mnuFileSaveAsActionPerformed(ActionEvent evt) {
        txtSource.saveFileDialog();
        updateTitleOfSourceCodePanel();
    }

    private void mnuFileSaveActionPerformed(ActionEvent evt) {
        btnSaveActionPerformed(evt);
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        txtSource.saveFile(true);
        updateTitleOfSourceCodePanel();
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
        if (!txtSource.confirmSaveButStillUnsaved()) {
            computer.close();
            dispose();
            System.exit(0); //calling the method is a must
        }
    }

    private void btnOpenActionPerformed(ActionEvent evt) {
        if (txtSource.openFileDialog()) {
            txtOutput.setText("");
        }
        updateTitleOfSourceCodePanel();
    }

    private void btnNewActionPerformed(ActionEvent evt) {
        txtSource.newFile();
        txtOutput.setText("");
        updateTitleOfSourceCodePanel();
    }

    private void btnFindReplaceActionPerformed(ActionEvent evt) {
        mnuEditFindActionPerformed(evt);
    }

    private void btnPasteActionPerformed(ActionEvent evt) {
        txtSource.paste();
    }

    private void btnCopyActionPerformed(ActionEvent evt) {
        txtSource.copy();
    }

    private void btnCutActionPerformed(ActionEvent evt) {
        txtSource.cut();
    }

    private void btnRedoActionPerformed(ActionEvent evt) {
        txtSource.redo();
    }

    private void btnUndoActionPerformed(ActionEvent evt) {
        txtSource.undo();
    }

    private void mnuEditFindActionPerformed(ActionEvent evt) {
        FindTextDialog.create(this, finder, txtSource, dialogs).setVisible(true);
    }

    private void mnuEditFindNextActionPerformed(ActionEvent evt) {
        try {
            if (finder.findNext(txtSource.getText(),
                txtSource.getCaretPosition(),
                txtSource.getDocument().getEndPosition().getOffset() - 1)) {
                txtSource.select(finder.getMatchStart(), finder.getMatchEnd());
                txtSource.grabFocus();
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
            if (finder.replaceNext(txtSource)) {
                txtSource.grabFocus();
            } else {
                dialogs.showError("Text was not found", "Replace next");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }

    private void updateTitleOfSourceCodePanel() {
        txtSource.getCurrentFile().ifPresentOrElse(
            file -> tabbedPane.setTitleAt(0, SOURCE_CODE_EDITOR + " (" + file.getName() + ")"),
            () -> tabbedPane.setTitleAt(0, SOURCE_CODE_EDITOR)
        );
    }

    private JButton btnBack;
    private JButton btnBeginning;
    private JButton btnBreakpoint;
    private JButton btnCopy;
    private JButton btnCut;
    private JButton btnPaste;
    private JButton btnPause;
    private JButton btnRedo;
    private JButton btnRun;
    private JButton btnRunTime;
    private JButton btnStep;
    private JButton btnStop;
    private JButton btnUndo;
    private JScrollPane editorScrollPane;
    private JList<String> lstDevices;
    private JMenuItem mnuEditCopy;
    private JMenuItem mnuEditCut;
    private JMenuItem mnuEditPaste;
    private JMenuItem mnuEditRedo;
    private JMenuItem mnuEditUndo;
    private JScrollPane paneDebug;
    private JPanel statusWindow;
    private JTextArea txtOutput;
    private JMenuItem mnuProjectCompilerSettings;
    private JButton btnMemory;
    private JButton btnCompile;
    private JMenuItem mnuProjectCompile;
    private JSplitPane splitSource;
    private JSplitPane splitPerDebug;
    private JToolBar toolDebug;
    private JPanel panelPages;
    private JTabbedPane tabbedPane;
}
