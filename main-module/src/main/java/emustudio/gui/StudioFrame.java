/*
 * (c) Copyright 2006-2016, Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui;

import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.Compiler.CompilerListener;
import emulib.plugins.compiler.Message;
import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.RunState;
import emulib.plugins.memory.Memory;
import emulib.plugins.memory.Memory.MemoryListener;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.RadixUtils;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.Computer;
import emustudio.architecture.SettingsManagerImpl;
import emustudio.emulation.EmulationController;
import emustudio.gui.debugTable.DebugTableImpl;
import emustudio.gui.debugTable.DebugTableModel;
import emustudio.gui.debugTable.PagesPanel;
import emustudio.gui.editor.EmuTextPane;
import emustudio.gui.editor.EmuTextPane.UndoActionListener;
import emustudio.gui.utils.ConstantSizeButton;
import emustudio.gui.utils.FindText;
import emustudio.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StudioFrame extends JFrame {
    private final static Logger LOGGER = LoggerFactory.getLogger(StudioFrame.class);

    private final Computer computer;
    private final SettingsManagerImpl settings;
    private final EmulationController emulationController;
    private final Clipboard systemClipboard;
    private final FindText finder = new FindText();
    private final EmuTextPane txtSource;
    private final DebugTableImpl debugTable;
    private final DebugTableModel debugTableModel;

    private final MemoryContext memoryContext;
    private final MemoryListener memoryListener;

    private volatile RunState runState = RunState.STATE_STOPPED_BREAK;

    public StudioFrame(ContextPool contextPool, Computer computer, String fileName, SettingsManagerImpl settings,
                       DebugTableImpl debugTable) throws ContextNotFoundException, InvalidContextException {
        this(contextPool, computer, settings, debugTable);
        txtSource.openFile(fileName);
    }

    public StudioFrame(ContextPool contextPool, final Computer computer, SettingsManagerImpl settings,
                       DebugTableImpl debugTable) throws ContextNotFoundException, InvalidContextException {
        this.computer = Objects.requireNonNull(computer);
        this.settings = Objects.requireNonNull(settings);
        this.debugTable = Objects.requireNonNull(debugTable);
        this.debugTableModel = Objects.requireNonNull(debugTable.getModel());

        emulationController = new EmulationController(
            computer.getCPU().get(), computer.getMemory(), computer::deviceIterator
        );

        txtSource = new EmuTextPane(computer.getCompiler());
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        memoryContext = contextPool.getMemoryContext(Main.password.hashCode(), MemoryContext.class);
        memoryListener = new MemoryListener() {
            @Override
            public void memoryChanged(int memoryPosition) {
                debugTableModel.memoryChanged(memoryPosition, memoryPosition + 1);
                debugTable.refresh();
            }

            @Override
            public void memorySizeChanged() {
                debugTableModel.setMemorySize(memoryContext.getSize());
            }
        };

        initComponents();

        btnMemory.setEnabled(computer.getMemory().isPresent() && computer.getMemory().get().isShowSettingsSupported());
        editorScrollPane.setViewportView(txtSource);
        paneDebug.setViewportView(debugTable);
        paneDebug.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        this.setStatusGUI();
        pack();

        setupListeners();

        lstDevices.setModel(new AbstractListModel<String>() {

            @Override
            public int getSize() {
                return computer.getDeviceCount();
            }

            @Override
            public String getElementAt(int index) {
                return computer.getDevice(index).get().getTitle();
            }
        });
        this.setLocationRelativeTo(null);
        this.setTitle("emuStudio [" + computer.getName() + "]");
        txtSource.grabFocus();
    }

    private void setStatusGUI() {
        JPanel statusPanel = computer.getCPU().get().getStatusPanel();
        if (statusPanel == null) {
            return;
        }
        GroupLayout layout = new GroupLayout(this.statusWindow);
        this.statusWindow.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(statusPanel));
        layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(statusPanel));
    }

    private void setupListeners() {
        setupClipboard();
        setupUndoRedo();

        setupCompiler();
        setupCPU();
        setStateNotRunning(runState);
    }

    private void setupCPU() {
        CPU cpu = computer.getCPU().get();
        cpu.addCPUListener(new CPU.CPUListener() {

            @Override
            public void internalStateChanged() {
                debugTable.refresh();
            }

            @Override
            public void runStateChanged(RunState state) {
                runState = state;
                if (state == RunState.STATE_RUNNING) {
                    setStateRunning();
                } else {
                    setStateNotRunning(state);
                }
            }
        });
        btnBreakpoint.setEnabled(cpu.isBreakpointSupported());
    }

    private void setStateNotRunning(RunState state) {
        btnPause.setEnabled(false);
        if (state == RunState.STATE_STOPPED_BREAK) {
            btnStop.setEnabled(true);
            btnRunTime.setEnabled(true);
            btnRun.setEnabled(true);
            btnStep.setEnabled(true);
        } else {
            btnStop.setEnabled(false);
            btnRunTime.setEnabled(false);
            btnRun.setEnabled(false);
            btnStep.setEnabled(false);
        }
        btnBack.setEnabled(true);
        btnBeginning.setEnabled(true);
        paneDebug.setEnabled(true);
        debugTable.setEnabled(true);
        debugTable.setVisible(true);
        debugTableModel.currentPage();
        debugTable.refresh();

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
                public void onMessage(Message message) {
                    txtOutput.append(message.getFormattedMessage() + "\n");
                }

                @Override
                public void onFinish(int errorCode) {
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

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
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
        JSplitPane splitSource = new JSplitPane();
        editorScrollPane = new JScrollPane();
        JScrollPane jScrollPane2 = new JScrollPane();
        txtOutput = new JTextArea();
        JPanel panelEmulator = new JPanel();
        JSplitPane splitLeftRight = new JSplitPane();
        statusWindow = new JPanel();
        JSplitPane splitPerDebug = new JSplitPane();
        JPanel debuggerPanel = new JPanel();
        JToolBar toolDebug = new JToolBar();
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
        JPanel panelPages = PagesPanel.create(debugTable);

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
        panelSource.setOpaque(false);

        toolStandard.setFloatable(false);
        toolStandard.setRollover(true);

        btnNew.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/document-new.png"))); // NOI18N
        btnNew.setToolTipText("New file");
        btnNew.setFocusable(false);
        btnNew.addActionListener(this::btnNewActionPerformed);

        btnOpen.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/document-open.png"))); // NOI18N
        btnOpen.setToolTipText("Open file");
        btnOpen.setFocusable(false);
        btnOpen.addActionListener(this::btnOpenActionPerformed);

        btnSave.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/document-save.png"))); // NOI18N
        btnSave.setToolTipText("Save file");
        btnSave.setFocusable(false);
        btnSave.addActionListener(this::btnSaveActionPerformed);

        jSeparator1.setOrientation(SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(10, 32768));
        jSeparator1.setPreferredSize(new java.awt.Dimension(10, 10));

        btnCut.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-cut.png"))); // NOI18N
        btnCut.setToolTipText("Cut selection");
        btnCut.setEnabled(false);
        btnCut.setFocusable(false);
        btnCut.addActionListener(this::btnCutActionPerformed);

        btnCopy.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-copy.png"))); // NOI18N
        btnCopy.setToolTipText("Copy selection");
        btnCopy.setEnabled(false);
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(this::btnCopyActionPerformed);

        btnPaste.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-paste.png"))); // NOI18N
        btnPaste.setToolTipText("Paste selection");
        btnPaste.setEnabled(false);
        btnPaste.setFocusable(false);
        btnPaste.addActionListener(this::btnPasteActionPerformed);

        btnFindReplace.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-find-replace.png"))); // NOI18N
        btnFindReplace.setToolTipText("Find/replace text...");
        btnFindReplace.setFocusable(false);
        btnFindReplace.addActionListener(this::btnFindReplaceActionPerformed);

        btnUndo.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-undo.png"))); // NOI18N
        btnUndo.setToolTipText("Undo");
        btnUndo.setEnabled(false);
        btnUndo.setFocusable(false);
        btnUndo.addActionListener(this::btnUndoActionPerformed);

        btnRedo.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/edit-redo.png"))); // NOI18N
        btnRedo.setToolTipText("Redo");
        btnRedo.setEnabled(false);
        btnRedo.setFocusable(false);
        btnRedo.addActionListener(this::btnRedoActionPerformed);

        jSeparator2.setOrientation(SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(10, 32767));

        jSeparator7.setOrientation(SwingConstants.VERTICAL);
        jSeparator7.setMaximumSize(new java.awt.Dimension(10, 32767));

        btnCompile.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/compile.png"))); // NOI18N
        btnCompile.setToolTipText("Compile source");
        btnCompile.setFocusable(false);
        btnCompile.addActionListener(this::btnCompileActionPerformed);

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
        splitSource.setDividerLocation(260);
        splitSource.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitSource.setOneTouchExpandable(true);
        splitSource.setLeftComponent(editorScrollPane);

        txtOutput.setColumns(20);
        txtOutput.setEditable(false);
        txtOutput.setFont(new Font("Monospaced", 0, 12));
        txtOutput.setLineWrap(true);
        txtOutput.setRows(3);
        txtOutput.setWrapStyleWord(true);
        jScrollPane2.setViewportView(txtOutput);

        splitSource.setRightComponent(jScrollPane2);

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

        tabbedPane.addTab("Source code editor", panelSource);

        panelEmulator.setOpaque(false);

        splitLeftRight.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitLeftRight.setContinuousLayout(true);
        splitLeftRight.setFocusable(false);

        statusWindow.setBorder(BorderFactory.createTitledBorder("Status"));

        splitLeftRight.setRightComponent(statusWindow);

        splitPerDebug.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitPerDebug.setDividerLocation(365);
        splitPerDebug.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPerDebug.setAutoscrolls(true);
        splitPerDebug.setContinuousLayout(true);

        debuggerPanel.setBorder(BorderFactory.createTitledBorder("Debugger"));

        toolDebug.setFloatable(false);
        toolDebug.setRollover(true);
        toolDebug.setBorder(null);
        toolDebug.setBorderPainted(false);

        btnReset.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/reset.png"))); // NOI18N
        btnReset.setToolTipText("Reset emulation");
        btnReset.setFocusable(false);
        btnReset.addActionListener(this::btnResetActionPerformed);

        btnBeginning.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-first.png"))); // NOI18N
        btnBeginning.setToolTipText("Jump to beginning");
        btnBeginning.setFocusable(false);
        btnBeginning.addActionListener(this::btnBeginningActionPerformed);

        btnBack.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-previous.png"))); // NOI18N
        btnBack.setToolTipText("Step back");
        btnBack.setFocusable(false);
        btnBack.addActionListener(this::btnBackActionPerformed);

        btnStop.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-stop.png"))); // NOI18N
        btnStop.setToolTipText("Stop emulation");
        btnStop.setFocusable(false);
        btnStop.addActionListener(this::btnStopActionPerformed);

        btnPause.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-pause.png"))); // NOI18N
        btnPause.setToolTipText("Pause emulation");
        btnPause.setFocusable(false);
        btnPause.addActionListener(this::btnPauseActionPerformed);

        btnRun.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-play.png"))); // NOI18N
        btnRun.setToolTipText("Run emulation");
        btnRun.setFocusable(false);
        btnRun.addActionListener(this::btnRunActionPerformed);

        btnRunTime.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-play-time.png"))); // NOI18N
        btnRunTime.setToolTipText("Run emulation in time slices");
        btnRunTime.setFocusable(false);
        btnRunTime.addActionListener(this::btnRunTimeActionPerformed);

        btnStep.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-next.png"))); // NOI18N
        btnStep.setToolTipText("Step forward");
        btnStep.setFocusable(false);
        btnStep.addActionListener(this::btnStepActionPerformed);

        btnJump.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/go-jump.png"))); // NOI18N
        btnJump.setToolTipText("Jump to address");
        btnJump.setFocusable(false);
        btnJump.addActionListener(this::btnJumpActionPerformed);

        btnBreakpoint.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/breakpoints.png"))); // NOI18N
        btnBreakpoint.setToolTipText("Set/unset breakpoint to address...");
        btnBreakpoint.setFocusable(false);
        btnBreakpoint.setHorizontalTextPosition(SwingConstants.CENTER);
        btnBreakpoint.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnBreakpoint.addActionListener(this::btnBreakpointActionPerformed);

        btnMemory.setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/grid_memory.gif"))); // NOI18N
        btnMemory.setToolTipText("Show operating memory");
        btnMemory.setFocusable(false);
        btnMemory.addActionListener(this::btnMemoryActionPerformed);
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
                    btnShowSettings.setEnabled(computer.getDevice(i).get().isShowSettingsSupported());
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


        mnuFileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        mnuFileNew.setText("New");
        mnuFile.setFont(mnuFile.getFont().deriveFont(mnuFile.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileNew.setFont(mnuFileNew.getFont().deriveFont(mnuFileNew.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileNew.addActionListener(this::mnuFileNewActionPerformed);
        mnuFile.add(mnuFileNew);

        mnuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        mnuFileOpen.setText("Open...");
        mnuFileOpen.setFont(mnuFileOpen.getFont().deriveFont(mnuFileOpen.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuFileOpen.addActionListener(this::mnuFileOpenActionPerformed);
        mnuFile.add(mnuFileOpen);
        mnuFile.add(jSeparator3);

        mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
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

        mnuEditUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
        mnuEditUndo.setText("Undo");
        mnuEditUndo.setFont(mnuEditUndo.getFont().deriveFont(mnuEditUndo.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditUndo.setEnabled(false);
        mnuEditUndo.addActionListener(this::mnuEditUndoActionPerformed);
        mnuEdit.add(mnuEditUndo);

        mnuEditRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
        mnuEditRedo.setText("Redo");
        mnuEditRedo.setEnabled(false);
        mnuEditRedo.setFont(mnuEditRedo.getFont().deriveFont(mnuEditRedo.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditRedo.addActionListener(this::mnuEditRedoActionPerformed);
        mnuEdit.add(mnuEditRedo);
        mnuEdit.add(jSeparator6);

        mnuEditCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
        mnuEditCut.setText("Cut selection");
        mnuEditCut.setEnabled(false);
        mnuEditCut.setFont(mnuEditCut.getFont().deriveFont(mnuEditCut.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditCut.addActionListener(this::mnuEditCutActionPerformed);
        mnuEdit.add(mnuEditCut);

        mnuEditCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        mnuEditCopy.setText("Copy selection");
        mnuEditCopy.setEnabled(false);
        mnuEditCopy.setFont(mnuEditCopy.getFont().deriveFont(mnuEditCopy.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditCopy.addActionListener(this::mnuEditCopyActionPerformed);
        mnuEdit.add(mnuEditCopy);

        mnuEditPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
        mnuEditPaste.setText("Paste selection");
        mnuEditPaste.setEnabled(false);
        mnuEditPaste.setFont(mnuEditPaste.getFont().deriveFont(mnuEditPaste.getFont().getStyle() & ~java.awt.Font.BOLD));
        mnuEditPaste.addActionListener(this::mnuEditPasteActionPerformed);
        mnuEdit.add(mnuEditPaste);
        mnuEdit.add(jSeparator5);

        mnuEditFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
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
                Main.tryShowErrorMessage("Device has to be selected!");
                return;
            }
            computer.getDevice(i).get().showSettings();
        } catch (Exception e) {
            LOGGER.error("Can't show settings of the device.", e);
            Main.tryShowErrorMessage("Can't show settings of the device:\n " + e.getMessage());
        }
    }

    private void showGUIButtonActionPerformed(ActionEvent evt) {
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                Main.tryShowErrorMessage("Device has to be selected!");
                return;
            }
            computer.getDevice(i).get().showGUI();
        } catch (Exception e) {
            LOGGER.error("Can't show GUI of the device.", e);
            Main.tryShowErrorMessage("Can't show GUI of the device:\n " + e.getMessage());
        }
    }

    private void btnMemoryActionPerformed(ActionEvent evt) {
        Optional<Memory> memory = computer.getMemory();

        if (memory.isPresent() && (memory.get().isShowSettingsSupported())) {
            memory.get().showSettings();
        } else {
            Main.tryShowMessage("The GUI is not supported");
        }
    }

    private void btnStepActionPerformed(ActionEvent evt) {
        emulationController.step();
    }

    private void btnRunActionPerformed(ActionEvent evt) {
        debugTable.setEnabled(false);
        emulationController.start();
    }

    private void btnRunTimeActionPerformed(ActionEvent evt) {
        String sliceText = StaticDialogs.inputStringValue("Enter time slice in milliseconds:",
                "Run timed emulation", "500");
        if (sliceText == null) {
            return;
        }
        try {
            final int sliceMillis = RadixUtils.getInstance().parseRadix(sliceText);
            emulationController.step(sliceMillis, TimeUnit.MILLISECONDS);
        } catch (NumberFormatException e) {
            Main.tryShowErrorMessage("Error: Wrong number format");
        }
    }

    private void btnStopActionPerformed(ActionEvent evt) {
        emulationController.stop();
    }

    private void btnBackActionPerformed(ActionEvent evt) {
        CPU cpu = computer.getCPU().get();
        int pc = cpu.getInstructionPosition();
        if (pc > 0) {
            cpu.setInstructionPosition(debugTableModel.guessPreviousInstructionPosition());
            paneDebug.revalidate();
            debugTable.refresh();
        }
    }

    private void btnBeginningActionPerformed(ActionEvent evt) {
        CPU cpu = computer.getCPU().get();
        cpu.setInstructionPosition(0);
        paneDebug.revalidate();
        debugTable.refresh();
    }

    private void btnResetActionPerformed(ActionEvent evt) {
        emulationController.reset();
    }

    private void btnJumpActionPerformed(ActionEvent evt) {
        int address;
        try {
            String number = StaticDialogs.inputStringValue("Jump to address: ", "Jump", "0");
            if (number == null) {
                return;
            }
            address = RadixUtils.getInstance().parseRadix(number);
        } catch (NumberFormatException e) {
            Main.tryShowErrorMessage("The number entered is in incorrect format", "Jump");
            return;
        }

        CPU cpu = computer.getCPU().get();
        Optional<Memory> memory = computer.getMemory();

        if (!cpu.setInstructionPosition(address)) {
            int memorySize = 0;
            if (memory.isPresent()) {
                memorySize = memory.get().getSize();
            }
            String maxSize = memory.isPresent() ?
                    "\n (expected range from 0 to " + String.valueOf(memorySize - 1) + ")"
                    : "";
            Main.tryShowErrorMessage("Typed address is incorrect !" + maxSize, "Jump");
        }
        debugTable.refresh();
    }

    private void mnuHelpAboutActionPerformed(ActionEvent evt) {
        (new AboutDialog(this)).setVisible(true);
    }

    private void mnuProjectCompileActionPerformed(ActionEvent evt) {
        btnCompileActionPerformed(evt);
    }

    private void btnCompileActionPerformed(ActionEvent evt) {
        if (runState == RunState.STATE_RUNNING) {
            Main.tryShowErrorMessage("You must first stop running emulation.", "Compile");
            return;
        }

        Optional<Compiler> compiler = computer.getCompiler();
        if (!compiler.isPresent()) {
            Main.tryShowErrorMessage("Compiler is not defined.", "Compile");
            return;
        }
        try {
            if (!txtSource.saveFile(true)) {
                return;
            }

            String fn = txtSource.getFileName();
            txtOutput.setText("");

            Optional<Memory> memory = computer.getMemory();

            if (memory.isPresent()) {
                memory.get().reset();
            }
            compiler.get().compile(fn);
            int programStart = compiler.get().getProgramStartAddress();
            if (memory.isPresent()) {
                memory.get().setProgramStart(programStart);
            }
            computer.getCPU().get().reset(programStart);
        } catch (Exception e) {
            LOGGER.error("Could not compile file.", e);
            txtOutput.append("Could not compile file: " + e.toString() + "\n");
        }
    }

    private void mnuProjectViewConfigActionPerformed(ActionEvent evt) {
        new ViewComputerDialog(this, computer, settings).setVisible(true);
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
    }

    private void mnuFileSaveActionPerformed(ActionEvent evt) {
        btnSaveActionPerformed(evt);
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        txtSource.saveFile(true);
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
        if (!txtSource.confirmSave()) {
            computer.destroy();
            dispose();
            System.exit(0); //calling the method is a must
        }
    }

    private void btnOpenActionPerformed(ActionEvent evt) {
        if (txtSource.openFileDialog()) {
            txtOutput.setText("");
        }
    }

    private void btnNewActionPerformed(ActionEvent evt) {
        txtSource.newFile();
        txtOutput.setText("");
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
        FindDialog.create(this, finder, false, txtSource).setVisible(true);
    }

    private void mnuEditFindNextActionPerformed(ActionEvent evt) {
        try {
            if (finder.findNext(txtSource.getText(),
                    txtSource.getCaretPosition(),
                    txtSource.getDocument().getEndPosition().getOffset() - 1)) {
                txtSource.select(finder.getMatchStart(), finder.getMatchEnd());
                txtSource.grabFocus();
            } else {
                Main.tryShowMessage("Expression was not found", "Find");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }

    private void btnBreakpointActionPerformed(ActionEvent evt) {
        BreakpointDialog bDialog = new BreakpointDialog(this, true);
        bDialog.setVisible(true);
        int address = bDialog.getAddress();

        CPU cpu = computer.getCPU().get();
        if ((address != -1) && cpu.isBreakpointSupported()) {
            if (bDialog.isSet()) {
                cpu.setBreakpoint(address);
            } else {
                cpu.unsetBreakpoint(address);
            }
        }
        paneDebug.revalidate();
        debugTable.refresh();
    }

    private void mnuEditReplaceNextActionPerformed(ActionEvent evt) {
        try {
            if (finder.replaceNext(txtSource)) {
                txtSource.grabFocus();
            } else {
                Main.tryShowMessage("Expression was not found", "Replace next");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
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
}
