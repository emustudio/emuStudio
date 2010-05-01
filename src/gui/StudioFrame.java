/*
 * StudioFrame.java
 *
 * Created on Nedeľa, 2007, august 5, 13:43
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package gui;


import architecture.ArchHandler;
import architecture.Main;
import gui.utils.DebugTable;
import gui.utils.DebugTableModel;
import gui.utils.NiceButton;
import gui.syntaxHighlighting.DocumentReader;
import gui.utils.EmuTextPane;
import gui.utils.FindText;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import plugins.compiler.IMessageReporter;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext.ICPUListener;
import plugins.device.IDevice;
import plugins.memory.IMemoryContext.IMemListener;
import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class StudioFrame extends javax.swing.JFrame {
    private EmuTextPane txtSource;
    private ArchHandler arch; // current architecture
    private ActionListener undoStateListener;
    private Clipboard systemClipboard;
    private int run_state = ICPU.STATE_STOPPED_BREAK;

    private IMessageReporter reporter;
    private DebugTable tblDebug;
    
    // emulator
    private DebugTableModel debug_model;
    
    public StudioFrame(String fileName) {
    	this();
    	txtSource.openFile(fileName);
    }
    
    /** Creates new form StudioFrame */
    public StudioFrame() {
        // create models and components
        arch = Main.currentArch;
        txtSource = new EmuTextPane();
        debug_model = new DebugTableModel(arch.getCPU(),arch.getMemory());
        tblDebug = new DebugTable(debug_model, arch.getCPU());
        initComponents();
        btnBreakpoint.setEnabled(arch.getCPU().isBreakpointSupported());
        jScrollPane1.setViewportView(txtSource);
        paneDebug.setViewportView(tblDebug);
        
        // set up message reporter for compiler messages
        this.reporter = new IMessageReporter() {
			@Override
            public void report(String message, int type) {
                txtOutput.append(message+"\n");
            }
			@Override
            public void report(int row, int col, String message, int type) {
                txtOutput.append("["+row + ";" + col +"] ");
                txtOutput.append(message+"\n");
            }
        };
        
        // Initialize compiler
        arch.getCompiler().initialize(arch, reporter);
        txtSource.setLexer(arch.getCompiler().getLexer(txtSource.getDocumentReader()));
        setUndoListener();
        setClipboardListener();
        
        //emulator settings
        this.setStatusGUI();
        try {
            arch.getMemory().getContext().addMemoryListener(new IMemListener() {
                public void memChange(EventObject evt, int adr) {
                    if (run_state == ICPU.STATE_RUNNING) return;
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            });

            arch.getCPU().getContext().addCPUListener(new ICPUListener() {
                public void stateUpdated(EventObject evt) {
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
				public void runChanged(EventObject evt, int state) {
					synchronized(this) {
						run_state = state;
	                    if (state == ICPU.STATE_RUNNING) {
	                        btnStop.setEnabled(true); btnBack.setEnabled(false);
	                        btnRun.setEnabled(false); btnStep.setEnabled(false);
	                        btnBeginning.setEnabled(false); btnPause.setEnabled(true);
	                        btnRunTime.setEnabled(false);
	                    } else {
	                        btnPause.setEnabled(false);
	                        if (state == ICPU.STATE_STOPPED_BREAK) {
	                            btnStop.setEnabled(true); btnRunTime.setEnabled(true);
	                            btnRun.setEnabled(true); btnStep.setEnabled(true);
	                        } else {
	                            btnStop.setEnabled(false); btnRunTime.setEnabled(false);
	                            btnRun.setEnabled(false); btnStep.setEnabled(false);
	                        }
	                        btnBack.setEnabled(true); btnBeginning.setEnabled(true);
	                        tblDebug.setEnabled(true);
	                        tblDebug.setVisible(true);
	                        tblDebug.revalidate();
	                        tblDebug.repaint();
	                    }
					}
    			}
            });
        } catch(NullPointerException e) {}
        btnBreakpoint.setEnabled(arch.getCPU().isBreakpointSupported());
        lstDevices.setModel(new AbstractListModel() {
            public int getSize() { return arch.getDevices().length; }
            public Object getElementAt(int index) {
                return arch.getDevices()[index].getTitle();
            }
        });
        new FindText(); // create instance
        this.setLocationRelativeTo(null);
        this.setTitle("emuStudio - " + arch.getArchName());
        txtSource.grabFocus();
    }
        
    // get gui panel from CPU plugin and show in main window
    public void setStatusGUI() {
        JPanel statusPanel = arch.getCPU().getStatusGUI();
        if (statusPanel == null) return;
        GroupLayout layout = new GroupLayout(this.statusWindow);
        this.statusWindow.setLayout(layout);
        layout.setHorizontalGroup(
        		layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		.addComponent(statusPanel)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
            .addComponent(statusPanel)
        );
        pack();
    }
    
    private void setUndoListener() {
        undoStateListener = new ActionListener() {
            public synchronized void actionPerformed(ActionEvent e) {
                if (txtSource.canUndo() == true) {
               		mnuEditUndo.setEnabled(true);
               		btnUndo.setEnabled(true);
                }
                else {
               		mnuEditUndo.setEnabled(false);
               		btnUndo.setEnabled(false);
                }
                if (txtSource.canRedo() == true) {
               		mnuEditRedo.setEnabled(true);
               		btnRedo.setEnabled(true);
                }
                else {
               		mnuEditRedo.setEnabled(false);
               		btnRedo.setEnabled(false);
                }
            }
        };
        txtSource.setUndoStateChangedAction(undoStateListener);
    }
    
    // clipboard operations implementation
    private void setClipboardListener() {
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (systemClipboard.getContents(null) != null) {
            btnPaste.setEnabled(true);
            mnuEditPaste.setEnabled(true);
        }
        systemClipboard.addFlavorListener(new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
                if (systemClipboard.getContents(null) == null) {
                    btnPaste.setEnabled(false);
                    mnuEditPaste.setEnabled(false);
                }
                else {
                    btnPaste.setEnabled(true);
                    mnuEditPaste.setEnabled(true);
                }
            }
        });
        txtSource.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                if (e.getDot() == e.getMark()) {
                    btnCut.setEnabled(false);mnuEditCut.setEnabled(false);
                    btnCopy.setEnabled(false);mnuEditCopy.setEnabled(false);
                } else {
                    btnCut.setEnabled(true);mnuEditCut.setEnabled(true);
                    btnCopy.setEnabled(true);mnuEditCopy.setEnabled(true);
                }
            }
        });
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
        btnFindReplace = new JButton();
        btnUndo = new JButton();
        btnRedo = new JButton();
        JSeparator jSeparator2 = new JSeparator();
        JButton btnCompile = new JButton();
        JSplitPane splitSoure = new JSplitPane();
        jScrollPane1 = new JScrollPane();
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
        JButton btnMemory = new JButton();
        paneDebug = new JScrollPane();
        JButton btnPrevious = new NiceButton();
        JButton btnNext = new NiceButton();
        JButton btnToPC = new NiceButton();
        JPanel peripheralPanel = new JPanel();
        JScrollPane paneDevices = new JScrollPane();
        lstDevices = new JList();
        JButton btnShowGUI = new NiceButton();
        JButton btnShowSettings = new NiceButton();
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
        JMenuItem mnuProjectCompile = new JMenuItem();
        JMenuItem mnuProjectViewConfig = new JMenuItem();
        JMenu mnuHelp = new JMenu();
        JMenuItem mnuHelpAbout = new JMenuItem();
        JSeparator jSeparator7 = new JSeparator();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("emuStudio");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        tabbedPane.setFocusable(false);
        panelSource.setOpaque(false);

        toolStandard.setFloatable(false);
        toolStandard.setRollover(true);

        btnNew.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/document-new.png"))); // NOI18N
        btnNew.setToolTipText("New file");
        btnNew.setFocusable(false);
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });

        btnOpen.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/document-open.png"))); // NOI18N
        btnOpen.setToolTipText("Open file");
        btnOpen.setFocusable(false);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        btnSave.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/document-save.png"))); // NOI18N
        btnSave.setToolTipText("Save file");
        btnSave.setFocusable(false);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(10, 32768));
        jSeparator1.setPreferredSize(new java.awt.Dimension(10, 10));

        btnCut.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-cut.png"))); // NOI18N
        btnCut.setToolTipText("Cut selection");
        btnCut.setEnabled(false);
        btnCut.setFocusable(false);
        btnCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCutActionPerformed(evt);
            }
        });

        btnCopy.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-copy.png"))); // NOI18N
        btnCopy.setToolTipText("Copy selection");
        btnCopy.setEnabled(false);
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyActionPerformed(evt);
            }
        });

        btnPaste.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-paste.png"))); // NOI18N
        btnPaste.setToolTipText("Paste selection");
        btnPaste.setEnabled(false);
        btnPaste.setFocusable(false);
        btnPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasteActionPerformed(evt);
            }
        });

        btnFindReplace.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-find-replace.png"))); // NOI18N
        btnFindReplace.setToolTipText("Find/replace text...");
        btnFindReplace.setFocusable(false);
        btnFindReplace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFindReplaceActionPerformed(evt);
            }
        });
        
        btnUndo.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-undo.png"))); // NOI18N
        btnUndo.setToolTipText("Undo");
        btnUndo.setEnabled(false);
        btnUndo.setFocusable(false);
        btnUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUndoActionPerformed(evt);
            }
        });

        btnRedo.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-redo.png"))); // NOI18N
        btnRedo.setToolTipText("Redo");
        btnRedo.setEnabled(false);
        btnRedo.setFocusable(false);
        btnRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRedoActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(10, 32767));

        jSeparator7.setOrientation(SwingConstants.VERTICAL);
        jSeparator7.setMaximumSize(new java.awt.Dimension(10, 32767));

        btnCompile.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/compile.png"))); // NOI18N
        btnCompile.setToolTipText("Compile source");
        btnCompile.setFocusable(false);
        btnCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompileActionPerformed(evt);
            }
        });

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
        
        splitSoure.setBorder(null);
        splitSoure.setDividerLocation(260);
        splitSoure.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitSoure.setOneTouchExpandable(true);
        splitSoure.setLeftComponent(jScrollPane1);

        txtOutput.setColumns(20);
        txtOutput.setEditable(false);
        txtOutput.setFont(new Font("Monospaced", 0, 12));
        txtOutput.setLineWrap(true);
        txtOutput.setRows(3);
        txtOutput.setWrapStyleWord(true);
        jScrollPane2.setViewportView(txtOutput);

        splitSoure.setRightComponent(jScrollPane2);

        GroupLayout panelSourceLayout = new GroupLayout(panelSource);
        panelSource.setLayout(panelSourceLayout);
        panelSourceLayout.setHorizontalGroup(
            panelSourceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(toolStandard) //, GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
            .addGroup(panelSourceLayout.createSequentialGroup()
            		.addContainerGap()
            		.addComponent(splitSoure) //, GroupLayout.DEFAULT_SIZE, 708, Short.MAX_VALUE)
            		.addContainerGap())
        );
        panelSourceLayout.setVerticalGroup(
            panelSourceLayout.createSequentialGroup()
            .addComponent(toolStandard, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
            .addComponent(splitSoure, 10, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
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
        splitPerDebug.setDividerLocation(330);
        splitPerDebug.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPerDebug.setAutoscrolls(true);
        splitPerDebug.setContinuousLayout(true);

        debuggerPanel.setBorder(BorderFactory.createTitledBorder("Debugger"));

        toolDebug.setFloatable(false);
        toolDebug.setRollover(true);
        toolDebug.setBorder(null);
        toolDebug.setBorderPainted(false);

        btnReset.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/view-refresh.png"))); // NOI18N
        btnReset.setToolTipText("Reset emulation");
        btnReset.setFocusable(false);
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        btnBeginning.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-first.png"))); // NOI18N
        btnBeginning.setToolTipText("Jump to beginning");
        btnBeginning.setFocusable(false);
        btnBeginning.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBeginningActionPerformed(evt);
            }
        });

        btnBack.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-previous.png"))); // NOI18N
        btnBack.setToolTipText("Step back");
        btnBack.setFocusable(false);
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        btnStop.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-stop.png"))); // NOI18N
        btnStop.setToolTipText("Stop emulation");
        btnStop.setFocusable(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        btnPause.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-pause.png"))); // NOI18N
        btnPause.setToolTipText("Pause emulation");
        btnPause.setFocusable(false);
        btnPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseActionPerformed(evt);
            }
        });

        btnRun.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-play.png"))); // NOI18N
        btnRun.setToolTipText("Run emulation");
        btnRun.setFocusable(false);
        btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });

        btnRunTime.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-play-time.png"))); // NOI18N
        btnRunTime.setToolTipText("Run emulation in time slices");
        btnRunTime.setFocusable(false);
        btnRunTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunTimeActionPerformed(evt);
            }
        });
        
        btnStep.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-next.png"))); // NOI18N
        btnStep.setToolTipText("Step forward");
        btnStep.setFocusable(false);
        btnStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStepActionPerformed(evt);
            }
        });

        btnJump.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/go-jump.png"))); // NOI18N
        btnJump.setToolTipText("Jump to address");
        btnJump.setFocusable(false);
        btnJump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJumpActionPerformed(evt);
            }
        });

        btnBreakpoint.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/preferences-desktop.png"))); // NOI18N
        btnBreakpoint.setToolTipText("Set/unset breakpoint to address...");
        btnBreakpoint.setFocusable(false);
        btnBreakpoint.setHorizontalTextPosition(SwingConstants.CENTER);
        btnBreakpoint.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBreakpointActionPerformed(evt);
            }
        });

        btnMemory.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/Memory24.gif"))); // NOI18N
        btnMemory.setToolTipText("Show operating memory");
        btnMemory.setFocusable(false);
        btnMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMemoryActionPerformed(evt);
            }
        });
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

        btnPrevious.setText("< Previous");
        btnPrevious.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviousActionPerformed(evt);
            }
        });

        btnNext.setText("Next >");
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextActionPerformed(evt);
            }
        });

        btnToPC.setText("To PC");
        btnToPC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToPCActionPerformed(evt);
            }
        });

        GroupLayout debuggerPanelLayout = new GroupLayout(debuggerPanel);
        debuggerPanel.setLayout(debuggerPanelLayout);
        debuggerPanelLayout.setHorizontalGroup(
            debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(toolDebug) //, GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
            .addGroup(debuggerPanelLayout.createSequentialGroup()
                .addComponent(btnPrevious)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnToPC)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                .addComponent(btnNext))
            .addComponent(paneDebug, 10, 350, Short.MAX_VALUE)
        );
        debuggerPanelLayout.setVerticalGroup(
            debuggerPanelLayout.createSequentialGroup()
                .addComponent(toolDebug, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                .addComponent(paneDebug, GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPrevious)
                    .addComponent(btnNext)
                    .addComponent(btnToPC))
        );
        splitLeftRight.setDividerLocation(1.0);
        splitPerDebug.setTopComponent(debuggerPanel);

        peripheralPanel.setBorder(BorderFactory.createTitledBorder("Peripheral devices"));

        paneDevices.setViewportView(lstDevices);
        lstDevices.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) 
	                showGUIButtonActionPerformed(new ActionEvent(this,0,""));				
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseReleased(MouseEvent e) {}
        });

        btnShowSettings.setText("Settings");
        btnShowSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSettingsButtonActionPerformed(evt);
            }
        });

        btnShowGUI.setText("Show");
        btnShowGUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showGUIButtonActionPerformed(evt);
            }
        });

        GroupLayout peripheralPanelLayout = new GroupLayout(peripheralPanel);
        peripheralPanel.setLayout(peripheralPanelLayout);
        peripheralPanelLayout.setHorizontalGroup(
            peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(paneDevices)
            .addGroup(GroupLayout.Alignment.TRAILING, peripheralPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnShowSettings)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShowGUI)
                .addContainerGap()));
        peripheralPanelLayout.setVerticalGroup(
            peripheralPanelLayout.createSequentialGroup()
                .addComponent(paneDevices)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                		.addComponent(btnShowSettings)
                        .addComponent(btnShowGUI)));
        splitPerDebug.setRightComponent(peripheralPanel);
        splitLeftRight.setLeftComponent(splitPerDebug);

        GroupLayout panelEmulatorLayout = new GroupLayout(panelEmulator);
        panelEmulator.setLayout(panelEmulatorLayout);
        panelEmulatorLayout.setHorizontalGroup(
            panelEmulatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(splitLeftRight)
        );
        panelEmulatorLayout.setVerticalGroup(
            panelEmulatorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitLeftRight, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addContainerGap()
        );

        tabbedPane.addTab("Emulator", panelEmulator);

        mnuFile.setText("File");

        mnuFileNew.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileNew.setText("New");
        mnuFileNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileNewActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileNew);

        mnuFileOpen.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileOpen.setText("Open...");
        mnuFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileOpenActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileOpen);
        mnuFile.add(jSeparator3);

        mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setText("Save");
        mnuFileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSave);

        mnuFileSaveAs.setText("Save As...");
        mnuFileSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveAsActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSaveAs);
        mnuFile.add(jSeparator4);

        mnuFileExit.setText("Exit");
        mnuFileExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileExitActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileExit);

        jMenuBar2.add(mnuFile);

        mnuEdit.setText("Edit");

        mnuEditUndo.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditUndo.setText("Undo");
        mnuEditUndo.setEnabled(false);
        mnuEditUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditUndoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditUndo);

        mnuEditRedo.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditRedo.setText("Redo");
        mnuEditRedo.setEnabled(false);
        mnuEditRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditRedoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditRedo);
        mnuEdit.add(jSeparator6);

        mnuEditCut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCut.setText("Cut selection");
        mnuEditCut.setEnabled(false);
        mnuEditCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCutActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCut);

        mnuEditCopy.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCopy.setText("Copy selection");
        mnuEditCopy.setEnabled(false);
        mnuEditCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCopyActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCopy);

        mnuEditPaste.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditPaste.setText("Paste selection");
        mnuEditPaste.setEnabled(false);
        mnuEditPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditPasteActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditPaste);
        mnuEdit.add(jSeparator5);

        mnuEditFind.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditFind.setText("Find/replace text...");
        mnuEditFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditFindActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditFind);

        mnuEditFindNext.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        mnuEditFindNext.setText("Find next");
        mnuEditFindNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditFindNextActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditFindNext);

        mnuEditReplaceNext.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        mnuEditReplaceNext.setText("Replace next");
        mnuEditReplaceNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditReplaceNextActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditReplaceNext);

        jMenuBar2.add(mnuEdit);

        mnuProject.setText("Project");

        mnuProjectCompile.setText("Compile source...");
        mnuProjectCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuProjectCompileActionPerformed(evt);
            }
        });
        mnuProject.add(mnuProjectCompile);

        mnuProjectViewConfig.setText("View configuration...");
        mnuProjectViewConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuProjectViewConfigActionPerformed(evt);
            }
        });
        mnuProject.add(mnuProjectViewConfig);

        jMenuBar2.add(mnuProject);

        mnuHelp.setText("Help");

        mnuHelpAbout.setText("About...");
        mnuHelpAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuHelpAboutActionPerformed(evt);
            }
        });
        mnuHelp.add(mnuHelpAbout);

        jMenuBar2.add(mnuHelp);

        setJMenuBar(jMenuBar2);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    private void btnPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseActionPerformed
        arch.getCPU().pause();
    }//GEN-LAST:event_btnPauseActionPerformed

    private void showSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showGUIButtonActionPerformed
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("Device has to be selected!");
                return;
            }
            arch.getDevices()[i].showSettings();
        } catch(Exception e) {
        	e.printStackTrace();
            StaticDialogs.showErrorMessage("Can't show settings of the device:\n " + e.getMessage());
        }
    }//GEN-LAST:event_showGUIButtonActionPerformed
    
    
    private void showGUIButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showGUIButtonActionPerformed
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("Device has to be selected!");
                return;
            }
            arch.getDevices()[i].showGUI();
        } catch(Exception e) {
        	e.printStackTrace();
            StaticDialogs.showErrorMessage("Can't show GUI of the device:\n " + e.getMessage());
        }
    }//GEN-LAST:event_showGUIButtonActionPerformed

    private void btnMemoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMemoryActionPerformed
        arch.getMemory().showGUI();
    }//GEN-LAST:event_btnMemoryActionPerformed

    private void btnStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStepActionPerformed
        arch.getCPU().step();
    }//GEN-LAST:event_btnStepActionPerformed

    private void btnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunActionPerformed
        tblDebug.setVisible(false);
        arch.getCPU().execute();
    }//GEN-LAST:event_btnRunActionPerformed

    private void btnRunTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunActionPerformed
    	String sliceText = JOptionPane.showInputDialog("Enter time slice in milliseconds:","500");
    	try {
    		final int slice = Integer.parseInt(sliceText);
    		new Thread() {
    			public void run() {
    				ICPU cpu = arch.getCPU();
    				while(run_state == ICPU.STATE_STOPPED_BREAK) {
    					cpu.step();
    					try { Thread.sleep(slice); }
    					catch(InterruptedException e) {}
    				}
    			}
    		}.start();
    	} catch(NumberFormatException e) {
    		StaticDialogs.showErrorMessage("Error: the number has to be integer,");
    	}
    }//GEN-LAST:event_btnRunActionPerformed
    
    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        arch.getCPU().stop();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        try {
            int pc = arch.getCPU().getInstrPosition();
            if (pc > 0) {
                arch.getCPU().setInstrPosition(pc-1);
                paneDebug.revalidate();
                if (tblDebug.isVisible()) {
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            }
        } catch(NullPointerException e) {}
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnBeginningActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBeginningActionPerformed
        try {
        	arch.getCPU().setInstrPosition(0);
            paneDebug.revalidate();
            if (tblDebug.isVisible()) {
                tblDebug.revalidate();
                tblDebug.repaint();
            }
        } catch(NullPointerException e) {}
    }//GEN-LAST:event_btnBeginningActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        arch.getCPU().reset(arch.getMemory().getProgramStart()); // first address of an image??
        arch.getMemory().reset();
        IDevice dev[] = arch.getDevices();
        if (dev != null) {
            for (int i = 0; i < dev.length; i++)
                dev[i].reset();
        }
        paneDebug.revalidate();
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnJumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnJumpActionPerformed
        int address = 0;
        try {
            address = Integer.decode(JOptionPane.showInputDialog(this,
                    "Jump to address: ", "Jump",JOptionPane.QUESTION_MESSAGE,
                    null,null,0).toString()).intValue();
        } catch(Exception e) {return;}
        try {
            if (arch.getCPU().setInstrPosition(address) == false) {
                JOptionPane.showMessageDialog(this,
                        "Typed address is incorrect ! (expected range from 0 to "
                        + String.valueOf(arch.getMemory().getSize())+")",
                        "Jump",JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch(NullPointerException e) {}
        paneDebug.revalidate();
        if (tblDebug.isVisible()) {
            tblDebug.revalidate();
            tblDebug.repaint();
        }
    }//GEN-LAST:event_btnJumpActionPerformed

    private void mnuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuHelpAboutActionPerformed
        (new AboutDialog(this,true)).setVisible(true);
    }//GEN-LAST:event_mnuHelpAboutActionPerformed

    private void mnuProjectCompileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuProjectCompileActionPerformed
        btnCompileActionPerformed(evt);
    }//GEN-LAST:event_mnuProjectCompileActionPerformed

    private void btnCompileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompileActionPerformed
        if (txtSource.isFileSaved() == false) {
            StaticDialogs.showErrorMessage("You must first save source file.");
            return;
        }
        if (run_state == ICPU.STATE_RUNNING) {
            StaticDialogs.showErrorMessage("You must first stop running emulation.");
            return;        	
        }
        txtSource.setEditable(false);
        txtOutput.setText("");
        String fn = txtSource.getFileName();
        fn = fn.substring(0,fn.lastIndexOf(".")) + ".hex"; // chyba.

// zatial... neskor sa bude dat nastavit v kompilatore...asi
        int res = JOptionPane.showConfirmDialog(null,
                "Will you want to load compiled file into operating memory ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);
        try {
        	DocumentReader r = new DocumentReader(txtSource.getDocument());
            if (res == JOptionPane.YES_OPTION) {
            	arch.getMemory().reset();
            	arch.getCompiler().compile(fn, r, arch.getMemory().getContext());
            	int programStart = arch.getCompiler().getProgramStartAddress();
            	arch.getMemory().setProgramStart(programStart);
            	arch.getCPU().reset(programStart);
            } else arch.getCompiler().compile(fn,r);
        }
        catch(Exception e) {
            txtOutput.append(e.toString()+"\n");
            txtSource.setEditable(true);
        } catch(Error ex) {}
        txtSource.setEditable(true);

    }//GEN-LAST:event_btnCompileActionPerformed

    private void mnuProjectViewConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuProjectViewConfigActionPerformed
        new ViewArchDialog(this,true).setVisible(true);
    }//GEN-LAST:event_mnuProjectViewConfigActionPerformed

    private void mnuEditPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditPasteActionPerformed
        btnPasteActionPerformed(evt);
    }//GEN-LAST:event_mnuEditPasteActionPerformed

    private void mnuEditCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditCopyActionPerformed
        btnCopyActionPerformed(evt);
    }//GEN-LAST:event_mnuEditCopyActionPerformed

    private void mnuEditCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditCutActionPerformed
        btnCutActionPerformed(evt);
    }//GEN-LAST:event_mnuEditCutActionPerformed

    private void mnuEditRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditRedoActionPerformed
        btnRedoActionPerformed(evt);
    }//GEN-LAST:event_mnuEditRedoActionPerformed

    private void mnuEditUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditUndoActionPerformed
        btnUndoActionPerformed(evt);
    }//GEN-LAST:event_mnuEditUndoActionPerformed

    private void mnuFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileSaveAsActionPerformed
        txtSource.saveFileDialog();
    }//GEN-LAST:event_mnuFileSaveAsActionPerformed

    private void mnuFileSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileSaveActionPerformed
        btnSaveActionPerformed(evt);
    }//GEN-LAST:event_mnuFileSaveActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        txtSource.saveFile();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void mnuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileOpenActionPerformed
        btnOpenActionPerformed(evt);
    }//GEN-LAST:event_mnuFileOpenActionPerformed

    private void mnuFileNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileNewActionPerformed
        btnNewActionPerformed(evt);
    }//GEN-LAST:event_mnuFileNewActionPerformed

    private void mnuFileExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileExitActionPerformed
        this.processWindowEvent(new WindowEvent(this,WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_mnuFileExitActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (txtSource.confirmSave() == true) return;
        arch.destroy();
        dispose();
        System.exit(0); //calling the method is a must
    }//GEN-LAST:event_formWindowClosing

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        txtSource.openFileDialog();
        txtOutput.setText("");
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
        txtSource.newFile();
        txtOutput.setText("");
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnFindReplaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
    	mnuEditFindActionPerformed(evt);
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPasteActionPerformed
        try{ txtSource.paste(); }
        catch (Exception e) {}
    }//GEN-LAST:event_btnPasteActionPerformed

    private void btnCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopyActionPerformed
        try{ txtSource.copy(); }
        catch (Exception e) {}
    }//GEN-LAST:event_btnCopyActionPerformed

    private void btnCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCutActionPerformed
        try{ txtSource.cut(); }
        catch (Exception e) {}
    }//GEN-LAST:event_btnCutActionPerformed

    private void btnRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRedoActionPerformed    	
    	txtSource.redo();
       	//undoStateListener.actionPerformed(new ActionEvent(this,0,""));
    }//GEN-LAST:event_btnRedoActionPerformed

    private void btnUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUndoActionPerformed
      	txtSource.undo();
       	//undoStateListener.actionPerformed(new ActionEvent(this,0,""));
    }//GEN-LAST:event_btnUndoActionPerformed

    private void mnuEditFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditFindActionPerformed
        new FindDialog(this,false,txtSource).setVisible(true);
    }//GEN-LAST:event_mnuEditFindActionPerformed

    private void mnuEditFindNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditFindNextActionPerformed
        try {
            if (FindText.getThis().findNext(txtSource.getText(), 
                    txtSource.getCaretPosition(), 
                    txtSource.getDocument().getEndPosition().getOffset()-1)) {
                txtSource.select(FindText.getThis().getMatchStart(), 
                        FindText.getThis().getMatchEnd());
                txtSource.grabFocus();
            } else StaticDialogs.showMessage("Expression was not found");
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }//GEN-LAST:event_mnuEditFindNextActionPerformed

    private void btnBreakpointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBreakpointActionPerformed
        int address = 0;
        new BreakpointDialog(this,true).setVisible(true);
        address = BreakpointDialog.getAdr();
        if ((address != -1) && arch.getCPU().isBreakpointSupported())
            arch.getCPU().setBreakpoint(address,
                    BreakpointDialog.getSet());
        paneDebug.revalidate();
        if (tblDebug.isVisible()) {
            tblDebug.revalidate();
            tblDebug.repaint();
        }
    }//GEN-LAST:event_btnBreakpointActionPerformed

    private void mnuEditReplaceNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditReplaceNextActionPerformed
        try {
            if (FindText.getThis().replaceNext(txtSource)) {
                txtSource.grabFocus();
            } else StaticDialogs.showMessage("Expression was not found");
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }//GEN-LAST:event_mnuEditReplaceNextActionPerformed

private void btnPreviousActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousActionPerformed
    debug_model.previous();
    tblDebug.revalidate();
    tblDebug.repaint();
}//GEN-LAST:event_btnPreviousActionPerformed

private void btnToPCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnToPCActionPerformed
    debug_model.topc();
    tblDebug.revalidate();
    tblDebug.repaint();
}//GEN-LAST:event_btnToPCActionPerformed

private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextActionPerformed
    debug_model.next();
    tblDebug.revalidate();
    tblDebug.repaint();
}//GEN-LAST:event_btnNextActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JButton btnBack;
    JButton btnBeginning;
    JButton btnBreakpoint;
    JButton btnFindReplace;
    JButton btnCopy;
    JButton btnCut;
    JButton btnPaste;
    JButton btnPause;
    JButton btnRedo;
    JButton btnRun;
    JButton btnRunTime;
    JButton btnStep;
    JButton btnStop;
    JButton btnUndo;
    JScrollPane jScrollPane1;
    JList lstDevices;
    JMenuItem mnuEditCopy;
    JMenuItem mnuEditCut;
    JMenuItem mnuEditPaste;
    JMenuItem mnuEditRedo;
    JMenuItem mnuEditUndo;
    JScrollPane paneDebug;
    JPanel statusWindow;
    JTextArea txtOutput;
    // End of variables declaration//GEN-END:variables
    
}
