/*
 * StudioFrame.java
 *
 * Created on Nedeľa, 2007, august 5, 13:43
 */

package gui;


import architecture.ArchHandler;
import architecture.Main;
import gui.utils.DebugTable;
import gui.utils.DebugTableModel;
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
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.AbstractListModel;
import javax.swing.GroupLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import plugins.compiler.ILexer;
import plugins.compiler.IMessageReporter;
import plugins.cpu.ICPUContext.stateEnum;
import plugins.cpu.ICPUContext.ICPUListener;
import plugins.device.IDevice;
import plugins.memory.IMemoryContext.IMemListener;
import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
public class StudioFrame extends javax.swing.JFrame {
    private EmuTextPane txtSource;
    private ArchHandler arch; // current architecture
    private ActionListener undoStateListener;
    private Clipboard systemClipboard;
    
    /* for compiling, not for syntax highlighting */
    private ILexer syntaxLexer;
    private IMessageReporter reporter;
    private DebugTable tblDebug;
    private boolean cpuPermanentRunning;
    
    // emulator
    private DebugTableModel debug_model;
    
    /** Creates new form StudioFrame */
    public StudioFrame() {
        // create models and components
        arch = Main.currentArch;
        txtSource = new EmuTextPane();
        cpuPermanentRunning = false;
        debug_model = new DebugTableModel(arch.getCPU(),arch.getCompiler(),
                arch.getMemory());
        tblDebug = new DebugTable(debug_model, arch.getCPU());
        
        // create other components
        initComponents();
        btnBreakpoint.setEnabled(arch.getCPU().isBreakpointSupported());
        jScrollPane1.setViewportView(txtSource);
        paneDebug.setViewportView(tblDebug);
        
        // set up message reporter for compiler messages
        this.reporter = new IMessageReporter() {
            public void report(String message) {
                txtOutput.append(message+"\n");
            }
            public void report(String location, String message) {
                Font f = txtOutput.getFont().deriveFont(Font.BOLD);
                Font old = txtOutput.getFont();
                txtOutput.setFont(f);
                txtOutput.append("["+location+"] ");
                txtOutput.setFont(old);
                txtOutput.append(message+"\n");
            }
        };
        txtSource.setLexer(arch.getCompiler().getLexer(txtSource.getDocumentReader(),
                reporter));
        syntaxLexer = arch.getCompiler().getLexer(new DocumentReader(txtSource.getDocument()),
                reporter);
        setUndoListener();
        setClipboardListener();
        
        //emulator settings
        this.setStatusGUI();
        try {
            arch.getMemory().getContext().addMemoryListener(new IMemListener() {
                public void memChange(EventObject evt, int adr) {
                    if (cpuPermanentRunning == true) return;
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            });

            arch.getCPU().getContext().addCPUListener(new ICPUListener() {
                public void runChanged(EventObject evt, stateEnum state) {
                    if (state == stateEnum.runned) {
                        btnStop.setEnabled(true); btnBack.setEnabled(false);
                        btnRun.setEnabled(false); btnStep.setEnabled(false);
                        btnBeginning.setEnabled(false); btnPause.setEnabled(true);
                    } else {
                        btnPause.setEnabled(false);
                        cpuPermanentRunning = false;
                        if (state == stateEnum.stoppedBreak) {
                            btnStop.setEnabled(true);
                            btnRun.setEnabled(true); btnStep.setEnabled(true);
                        } else {
                            btnStop.setEnabled(false);
                            btnRun.setEnabled(false); btnStep.setEnabled(false);
                        }
                        btnBack.setEnabled(true); btnBeginning.setEnabled(true);
                        tblDebug.setEnabled(true);
                        tblDebug.setVisible(true);
                        tblDebug.revalidate();
                        tblDebug.repaint();
                    }
                }
                public void stateUpdated(EventObject evt) {
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            });
        } catch(NullPointerException e) {}
        btnBreakpoint.setEnabled(arch.getCPU().isBreakpointSupported());
        lstDevices.setModel(new AbstractListModel() {
            public int getSize() { return arch.getDevices().length; }
            public Object getElementAt(int index) {
                return arch.getDevices()[index].getName();
            }
        });
        new FindText(); // create instance
        this.setLocationRelativeTo(null);
        this.setTitle("emu8 Studio - " + arch.getArchName());
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
            .addGroup(layout.createSequentialGroup()
//                .addContainerGap()
                .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
  //              .addContainerGap()
                )
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(statusPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                //.addContainerGap()
                )
        );
        pack();
    }
    
    // undo/redo implementation
    private void setUndoListener() {
        undoStateListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (txtSource.canUndo() == true) {
                    btnUndo.setEnabled(true);
                    mnuEditUndo.setEnabled(true);
                }
                else {
                    btnUndo.setEnabled(false);
                    mnuEditUndo.setEnabled(false);
                }
                if (txtSource.canRedo() == true) {
                    btnRedo.setEnabled(true);
                    mnuEditRedo.setEnabled(true);
                }
                else {
                    btnRedo.setEnabled(false);
                    mnuEditRedo.setEnabled(false);
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
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JTabbedPane jTabbedPane1 = new javax.swing.JTabbedPane();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JToolBar jToolBar1 = new javax.swing.JToolBar();
        javax.swing.JButton btnNew = new javax.swing.JButton();
        javax.swing.JButton btnOpen = new javax.swing.JButton();
        javax.swing.JButton btnSave = new javax.swing.JButton();
        javax.swing.JSeparator jSeparator1 = new javax.swing.JSeparator();
        btnCut = new javax.swing.JButton();
        btnCopy = new javax.swing.JButton();
        btnPaste = new javax.swing.JButton();
        btnUndo = new javax.swing.JButton();
        btnRedo = new javax.swing.JButton();
        javax.swing.JSeparator jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JButton btnCompile = new javax.swing.JButton();
        javax.swing.JSplitPane jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        txtOutput = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JSplitPane splitLeftRight = new javax.swing.JSplitPane();
        statusWindow = new javax.swing.JPanel();
        javax.swing.JSplitPane splitPerDebug = new javax.swing.JSplitPane();
        javax.swing.JPanel debuggerPanel = new javax.swing.JPanel();
        javax.swing.JToolBar jToolBar2 = new javax.swing.JToolBar();
        javax.swing.JButton btnReset = new javax.swing.JButton();
        btnBeginning = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnPause = new javax.swing.JButton();
        btnRun = new javax.swing.JButton();
        btnStep = new javax.swing.JButton();
        javax.swing.JButton btnJump = new javax.swing.JButton();
        btnBreakpoint = new javax.swing.JButton();
        javax.swing.JButton btnMemory = new javax.swing.JButton();
        paneDebug = new javax.swing.JScrollPane();
        javax.swing.JPanel peripheralPanel = new javax.swing.JPanel();
        javax.swing.JScrollPane paneDevices = new javax.swing.JScrollPane();
        lstDevices = new javax.swing.JList();
        javax.swing.JButton showGUIButton = new javax.swing.JButton();
        javax.swing.JMenuBar jMenuBar2 = new javax.swing.JMenuBar();
        javax.swing.JMenu mnuFile = new javax.swing.JMenu();
        javax.swing.JMenuItem mnuFileNew = new javax.swing.JMenuItem();
        javax.swing.JMenuItem mnuFileOpen = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator3 = new javax.swing.JSeparator();
        javax.swing.JMenuItem mnuFileSave = new javax.swing.JMenuItem();
        javax.swing.JMenuItem mnuFileSaveAs = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator4 = new javax.swing.JSeparator();
        javax.swing.JMenuItem mnuFileExit = new javax.swing.JMenuItem();
        javax.swing.JMenu mnuEdit = new javax.swing.JMenu();
        mnuEditUndo = new javax.swing.JMenuItem();
        mnuEditRedo = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator6 = new javax.swing.JSeparator();
        mnuEditCut = new javax.swing.JMenuItem();
        mnuEditCopy = new javax.swing.JMenuItem();
        mnuEditPaste = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator5 = new javax.swing.JSeparator();
        javax.swing.JMenuItem mnuEditFind = new javax.swing.JMenuItem();
        javax.swing.JMenuItem mnuEditFindNext = new javax.swing.JMenuItem();
        javax.swing.JMenuItem mnuEditReplaceNext = new javax.swing.JMenuItem();
        javax.swing.JMenu mnuProject = new javax.swing.JMenu();
        javax.swing.JMenuItem mnuProjectCompile = new javax.swing.JMenuItem();
        javax.swing.JMenuItem mnuProjectViewConfig = new javax.swing.JMenuItem();
        javax.swing.JMenu mnuHelp = new javax.swing.JMenu();
        javax.swing.JMenuItem mnuHelpAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("emu8 Studio");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTabbedPane1.setFocusable(false);

        jPanel1.setOpaque(false);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/New24.gif"))); // NOI18N
        btnNew.setToolTipText("New file");
        btnNew.setFocusable(false);
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNew);

        btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Open24.gif"))); // NOI18N
        btnOpen.setToolTipText("Open file");
        btnOpen.setFocusable(false);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpen);

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Save24.gif"))); // NOI18N
        btnSave.setToolTipText("Save file");
        btnSave.setFocusable(false);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jToolBar1.add(btnSave);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(10, 32768));
        jSeparator1.setPreferredSize(new java.awt.Dimension(10, 10));
        jToolBar1.add(jSeparator1);

        btnCut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Cut24.gif"))); // NOI18N
        btnCut.setToolTipText("Cut selection");
        btnCut.setEnabled(false);
        btnCut.setFocusable(false);
        btnCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCutActionPerformed(evt);
            }
        });
        jToolBar1.add(btnCut);

        btnCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Copy24.gif"))); // NOI18N
        btnCopy.setToolTipText("Copy selection");
        btnCopy.setEnabled(false);
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyActionPerformed(evt);
            }
        });
        jToolBar1.add(btnCopy);

        btnPaste.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Paste24.gif"))); // NOI18N
        btnPaste.setToolTipText("Paste selection");
        btnPaste.setEnabled(false);
        btnPaste.setFocusable(false);
        btnPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasteActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPaste);

        btnUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Undo24.gif"))); // NOI18N
        btnUndo.setToolTipText("Undo");
        btnUndo.setEnabled(false);
        btnUndo.setFocusable(false);
        btnUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUndoActionPerformed(evt);
            }
        });
        jToolBar1.add(btnUndo);

        btnRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Redo24.gif"))); // NOI18N
        btnRedo.setToolTipText("Redo");
        btnRedo.setEnabled(false);
        btnRedo.setFocusable(false);
        btnRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRedoActionPerformed(evt);
            }
        });
        jToolBar1.add(btnRedo);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(10, 32767));
        jToolBar1.add(jSeparator2);

        btnCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/patch.gif"))); // NOI18N
        btnCompile.setToolTipText("Compile source...");
        btnCompile.setFocusable(false);
        btnCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompileActionPerformed(evt);
            }
        });
        jToolBar1.add(btnCompile);

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(260);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane1.setLeftComponent(jScrollPane1);

        txtOutput.setColumns(20);
        txtOutput.setEditable(false);
        txtOutput.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtOutput.setLineWrap(true);
        txtOutput.setRows(5);
        txtOutput.setWrapStyleWord(true);
        jScrollPane2.setViewportView(txtOutput);

        jSplitPane1.setRightComponent(jScrollPane2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 708, Short.MAX_VALUE)
                .addGap(10, 10, 10))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Source code editor", jPanel1);

        jPanel2.setOpaque(false);

        splitLeftRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitLeftRight.setDividerLocation(400);
        splitLeftRight.setContinuousLayout(true);
        splitLeftRight.setFocusable(false);

        statusWindow.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));

        javax.swing.GroupLayout statusWindowLayout = new javax.swing.GroupLayout(statusWindow);
        statusWindow.setLayout(statusWindowLayout);
        statusWindowLayout.setHorizontalGroup(
            statusWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 309, Short.MAX_VALUE)
        );
        statusWindowLayout.setVerticalGroup(
            statusWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 441, Short.MAX_VALUE)
        );

        splitLeftRight.setRightComponent(statusWindow);

        splitPerDebug.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitPerDebug.setDividerLocation(330);
        splitPerDebug.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPerDebug.setAutoscrolls(true);
        splitPerDebug.setContinuousLayout(true);

        debuggerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Debugger"));

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        btnReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Refresh24.gif"))); // NOI18N
        btnReset.setToolTipText("Reset emulation");
        btnReset.setFocusable(false);
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });
        jToolBar2.add(btnReset);

        btnBeginning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/StepBack24.gif"))); // NOI18N
        btnBeginning.setToolTipText("Jump to beginning");
        btnBeginning.setFocusable(false);
        btnBeginning.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBeginningActionPerformed(evt);
            }
        });
        jToolBar2.add(btnBeginning);

        btnBack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Back24.gif"))); // NOI18N
        btnBack.setToolTipText("Step back");
        btnBack.setFocusable(false);
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        jToolBar2.add(btnBack);

        btnStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Stop24.gif"))); // NOI18N
        btnStop.setToolTipText("Stop emulation");
        btnStop.setFocusable(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        jToolBar2.add(btnStop);

        btnPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Pause24.gif"))); // NOI18N
        btnPause.setFocusable(false);
        btnPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseActionPerformed(evt);
            }
        });
        jToolBar2.add(btnPause);

        btnRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Run24.gif"))); // NOI18N
        btnRun.setToolTipText("Run emulation");
        btnRun.setFocusable(false);
        btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });
        jToolBar2.add(btnRun);

        btnStep.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Play24.gif"))); // NOI18N
        btnStep.setToolTipText("Step forward");
        btnStep.setFocusable(false);
        btnStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStepActionPerformed(evt);
            }
        });
        jToolBar2.add(btnStep);

        btnJump.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/jump24.gif"))); // NOI18N
        btnJump.setToolTipText("Jump to address");
        btnJump.setFocusable(false);
        btnJump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJumpActionPerformed(evt);
            }
        });
        jToolBar2.add(btnJump);

        btnBreakpoint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Break24.gif"))); // NOI18N
        btnBreakpoint.setToolTipText("Set/unset breakpoint to address...");
        btnBreakpoint.setFocusable(false);
        btnBreakpoint.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBreakpoint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBreakpoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBreakpointActionPerformed(evt);
            }
        });
        jToolBar2.add(btnBreakpoint);

        btnMemory.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/emu8/Memory24.gif"))); // NOI18N
        btnMemory.setToolTipText("Show operating memory");
        btnMemory.setFocusable(false);
        btnMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMemoryActionPerformed(evt);
            }
        });
        jToolBar2.add(btnMemory);

        javax.swing.GroupLayout debuggerPanelLayout = new javax.swing.GroupLayout(debuggerPanel);
        debuggerPanel.setLayout(debuggerPanelLayout);
        debuggerPanelLayout.setHorizontalGroup(
            debuggerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debuggerPanelLayout.createSequentialGroup()
                .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(paneDebug, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
        );
        debuggerPanelLayout.setVerticalGroup(
            debuggerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debuggerPanelLayout.createSequentialGroup()
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paneDebug, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                .addContainerGap())
        );

        splitPerDebug.setTopComponent(debuggerPanel);

        peripheralPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peripheral devices"));

        paneDevices.setViewportView(lstDevices);

        showGUIButton.setText("Show");
        showGUIButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showGUIButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout peripheralPanelLayout = new javax.swing.GroupLayout(peripheralPanel);
        peripheralPanel.setLayout(peripheralPanelLayout);
        peripheralPanelLayout.setHorizontalGroup(
            peripheralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peripheralPanelLayout.createSequentialGroup()
                .addContainerGap(323, Short.MAX_VALUE)
                .addComponent(showGUIButton)
                .addContainerGap())
            .addComponent(paneDevices, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
        );
        peripheralPanelLayout.setVerticalGroup(
            peripheralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peripheralPanelLayout.createSequentialGroup()
                .addComponent(paneDevices, javax.swing.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showGUIButton))
        );

        splitPerDebug.setRightComponent(peripheralPanel);

        splitLeftRight.setLeftComponent(splitPerDebug);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitLeftRight, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitLeftRight, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Emulator", jPanel2);

        mnuFile.setText("File");

        mnuFileNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileNew.setText("New");
        mnuFileNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileNewActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileNew);

        mnuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileOpen.setText("Open...");
        mnuFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileOpenActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileOpen);
        mnuFile.add(jSeparator3);

        mnuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
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

        mnuEditUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditUndo.setText("Undo");
        mnuEditUndo.setEnabled(false);
        mnuEditUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditUndoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditUndo);

        mnuEditRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditRedo.setText("Redo");
        mnuEditRedo.setEnabled(false);
        mnuEditRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditRedoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditRedo);
        mnuEdit.add(jSeparator6);

        mnuEditCut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCut.setText("Cut selection");
        mnuEditCut.setEnabled(false);
        mnuEditCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCutActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCut);

        mnuEditCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCopy.setText("Copy selection");
        mnuEditCopy.setEnabled(false);
        mnuEditCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCopyActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCopy);

        mnuEditPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditPaste.setText("Paste selection");
        mnuEditPaste.setEnabled(false);
        mnuEditPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditPasteActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditPaste);
        mnuEdit.add(jSeparator5);

        mnuEditFind.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditFind.setText("Find/replace text...");
        mnuEditFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditFindActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditFind);

        mnuEditFindNext.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        mnuEditFindNext.setText("Find next");
        mnuEditFindNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditFindNextActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditFindNext);

        mnuEditReplaceNext.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseActionPerformed
        arch.getCPU().pause();
    }//GEN-LAST:event_btnPauseActionPerformed

    private void showGUIButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showGUIButtonActionPerformed
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("Device has to be selected!");
                return;
            }
            arch.getDevices()[i].showGUI();
        } catch(Exception e) {
            StaticDialogs.showErrorMessage("Can't show the device: " + e.getMessage());
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
        cpuPermanentRunning = true;
        arch.getCPU().execute();
    }//GEN-LAST:event_btnRunActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        arch.getCPU().stop();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        try {
            int pc = arch.getCPU().getContext().getInstrPosition();
            if (pc > 0) {
                arch.getCPU().getContext().setInstrPosition(pc-1);
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
            arch.getCPU().getContext().setInstrPosition(0);
            paneDebug.revalidate();
            if (tblDebug.isVisible()) {
                tblDebug.revalidate();
                tblDebug.repaint();
            }
        } catch(NullPointerException e) {}
    }//GEN-LAST:event_btnBeginningActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        arch.getCPU().reset(); // first address of an image??
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
            if (arch.getCPU().getContext().setInstrPosition(address) == false) {
                JOptionPane.showMessageDialog(this,
                        "Typed address is incorrect ! (expected range from 0 to "
                        + String.valueOf(arch.getMemory().getContext().getSize())+")",
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
        txtSource.setEditable(false);
        txtOutput.setText("");
        String fn = txtSource.getFileName();
        fn = fn.substring(0,fn.lastIndexOf(".")) + ".hex";

// zatial... neskor sa bude dat nastavit v kompilatore...asi
        int res = JOptionPane.showConfirmDialog(null,
                "Will you want to load compiled file into operating memory ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);
        try {
            syntaxLexer.reset(new DocumentReader(txtSource.getDocument()),0,
                    0, 0);
            arch.getCompiler().compile(fn, arch.getMemory().getContext(),
                    (res==JOptionPane.YES_OPTION));
            arch.getMemory().setProgramStart(arch.getCompiler()
                    .getProgramStartAddress());
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
        // destroy all devices
        try {
            for (int i = 0; i < arch.getDevices().length; i++)
                arch.getDevices()[i].destroy();
            arch.getCPU().destroy();
            arch.getMemory().destroy();
        } catch (Exception e) {}
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
        undoStateListener.actionPerformed(new ActionEvent(this,0,""));
    }//GEN-LAST:event_btnRedoActionPerformed

    private void btnUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUndoActionPerformed
        txtSource.undo();
        undoStateListener.actionPerformed(new ActionEvent(this,0,""));
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
            if (FindText.getThis().replaceNext(txtSource.getText(), 
                    txtSource.getCaretPosition(),
                    txtSource.getDocument().getEndPosition().getOffset()-1)) {
                txtSource.setText(FindText.getThis().getReplacedString());
                txtSource.grabFocus();
            } else StaticDialogs.showMessage("Expression was not found");
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }//GEN-LAST:event_mnuEditReplaceNextActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JButton btnBack;
    javax.swing.JButton btnBeginning;
    javax.swing.JButton btnBreakpoint;
    javax.swing.JButton btnCopy;
    javax.swing.JButton btnCut;
    javax.swing.JButton btnPaste;
    javax.swing.JButton btnPause;
    javax.swing.JButton btnRedo;
    javax.swing.JButton btnRun;
    javax.swing.JButton btnStep;
    javax.swing.JButton btnStop;
    javax.swing.JButton btnUndo;
    javax.swing.JScrollPane jScrollPane1;
    javax.swing.JList lstDevices;
    javax.swing.JMenuItem mnuEditCopy;
    javax.swing.JMenuItem mnuEditCut;
    javax.swing.JMenuItem mnuEditPaste;
    javax.swing.JMenuItem mnuEditRedo;
    javax.swing.JMenuItem mnuEditUndo;
    javax.swing.JScrollPane paneDebug;
    javax.swing.JPanel statusWindow;
    javax.swing.JTextArea txtOutput;
    // End of variables declaration//GEN-END:variables
    
}
