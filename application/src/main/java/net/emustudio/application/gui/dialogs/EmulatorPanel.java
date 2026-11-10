/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.debugger.DebuggerPanel;
import net.emustudio.application.gui.actions.emulator.*;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.PagesPanel;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.application.gui.framework.EmuStudioGui;

import javax.swing.*;
import java.awt.event.*;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class EmulatorPanel extends JPanel {
    private static final int PERIPHERAL_PANEL_HEIGHT = 200;

    private final EmuStudioGui gui;
    private final JPanel statusWindow;
    private final JToolBar toolDebug;
    private final JPanel panelPages;
    private final JScrollPane paneDebug;

    private final JList<String> lstDevices = new JList<>();
    private final JSplitPane splitLeftRight;
    private final JSplitPane sidebar;
    private final JSplitPane cpuWatches;
    private boolean dividersInitialized;

    private final DebugTableModel debugTableModel;
    private final JTable debugTable;

    private final StepBackAction stepBackAction;
    private final ResetAction resetAction;
    private final JumpToBeginningAction jumpToBeginningAction;
    private final StopAction stopAction;
    private final PauseAction pauseAction;
    private final RunAction runAction;
    private final RunTimedAction runTimedAction;
    private final StepAction stepAction;
    private final JumpAction jumpAction;
    private final BreakpointAction breakpointAction;
    private final ShowMemoryAction showMemoryAction;

    private final ShowDeviceSettingsAction showDeviceSettingsAction;
    private final ShowDeviceGuiAction showDeviceGuiAction;

    private final MemoryContext<?> memoryContext;
    private final net.emustudio.application.debugger.DebuggerSession session;
    private final MemoryContext.MemoryListener memoryListener;
    private final DebuggerPanel inspection;
    private final CPU cpu;
    private final EmulationController execution;
    private CPUContext.CPUListener executionListener;
    private boolean closed;
    private boolean commandBusy;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    public EmulatorPanel(JFrame parent, VirtualComputer computer, DebugTableModel debugTableModel, Dialogs dialogs,
                         EmulationController emulationController, MemoryContext<?> memoryContext,
                         Consumer<SourceCodePosition> sourceNavigator, EmuStudioGui gui) {
        this.gui = Objects.requireNonNull(gui);
        this.execution = emulationController;
        Objects.requireNonNull(sourceNavigator);
        this.memoryContext = memoryContext;
        this.session = computer.getDebugger().orElse(null);
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.statusWindow = gui.section("CPU status / registers", "insets 0, fill", "[grow]", "[grow]");
        this.debugTable = new DebugTableImpl(debugTableModel);
        this.debugTable.getAccessibleContext().setAccessibleName("Instruction listing");
        this.toolDebug = gui.toolBar();
        this.toolDebug.putClientProperty("FlatLaf.style", "focusableButtons: true");

        paneDebug = gui.scrollPane(debugTable);
        paneDebug.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        debugTable.setFillsViewportHeight(true);

        gui.styleTable(debugTable);

        debugTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event) || memoryContext == null
                        || (inspection != null && !inspection.isInstructionView())) {
                    return;
                }
                int viewRow = debugTable.rowAtPoint(event.getPoint());
                if (viewRow < 0) {
                    return;
                }
                navigateToSource(viewRow, sourceNavigator);
            }
        });

        paneDebug.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                debugTable.dispatchEvent(e); // Debug table is not shrinking, just expanding...
            }
        });

        computer.getCPU().flatMap(cpu -> Optional.ofNullable(cpu.getStatusPanel())).ifPresentOrElse(
                this::setStatusPanel,
                () -> statusWindow.add(gui.label("CPU status is unavailable for this virtual computer."), "growx"));

        this.stepBackAction = new StepBackAction(computer, debugTableModel, this::refreshDebugTable);
        this.resetAction = new ResetAction(emulationController);
        this.jumpToBeginningAction = new JumpToBeginningAction(computer, this::refreshDebugTable);
        this.stopAction = new StopAction(emulationController);
        this.pauseAction = new PauseAction(emulationController, () -> this.setStateNotRunning(CPU.RunState.STATE_STOPPED_BREAK, false));
        this.runAction = new RunAction(emulationController, debugTable);
        this.runTimedAction = new RunTimedAction(emulationController, dialogs);
        this.stepAction = new StepAction(emulationController, this::stepCompleted);
        this.jumpAction = new JumpAction(computer, dialogs, this::refreshDebugTable);
        this.cpu = computer.getCPU().orElse(null);
        panelPages = PagesPanel.create(debugTableModel, dialogs, gui);
        JPanel debuggerPanel = gui.panel("insets 0, fill", "[grow]", "[grow][]");
        debuggerPanel.add(paneDebug, "grow, wrap, hmin 0");
        debuggerPanel.add(panelPages, "growx");

        this.inspection = computer.getDebugger().map(session -> new DebuggerPanel(
                session, (DebugTableImpl) debugTable, debuggerPanel,
                sourceNavigator, this::refreshDebugTable, gui, dialogs)).orElse(null);
        this.breakpointAction = new BreakpointAction(() -> {
            if (inspection != null) inspection.showBreakpointDialog(parent);
        });
        this.showMemoryAction = new ShowMemoryAction(parent, computer, dialogs);

        showMemoryAction.setEnabled(computer.getMemory().filter(Memory::isShowSettingsSupported).isPresent());
        breakpointAction.setEnabled(inspection != null && inspection.isBreakpointDialogSupported());

        setupDebugToolbar();
        debugTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && inspection != null && inspection.isInstructionView()
                    && runState != CPU.RunState.STATE_RUNNING && debugTable.getSelectedRow() >= 0) {
                int location = debugTableModel.getLocationAt(debugTable.convertRowIndexToModel(debugTable.getSelectedRow()));
                if (location >= 0) inspection.followAddress(location);
            }
        });
        debugTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "showSource");
        debugTable.getActionMap().put("showSource", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (inspection != null && !inspection.isInstructionView()) {
                    inspection.showSelectedSource();
                    return;
                }
                int row = debugTable.getSelectedRow();
                if (row >= 0) navigateToSource(row, sourceNavigator);
            }
        });

        this.showDeviceSettingsAction = new ShowDeviceSettingsAction(parent, computer, dialogs, lstDevices::getSelectedIndex);
        this.showDeviceGuiAction = new ShowDeviceGuiAction(parent, computer, dialogs, lstDevices::getSelectedIndex);

        lstDevices.setName("devices");
        lstDevices.getAccessibleContext().setAccessibleName("Peripheral devices");
        lstDevices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        lstDevices.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                int i = lstDevices.getSelectedIndex();
                showDeviceSettingsAction.setEnabled(i >= 0 && computer.getDevices().get(i).isShowSettingsSupported());
                showDeviceGuiAction.setEnabled(i >= 0);
            }
        });
        lstDevices.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && lstDevices.getSelectedIndex() >= 0) {
                    showDeviceGuiAction.actionPerformed(new ActionEvent(this, 0, ""));
                }
            }
        });

        lstDevices.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "showDevice");
        lstDevices.getActionMap().put("showDevice", showDeviceGuiAction);

        JPanel peripheralPanel = gui.section("Peripheral devices", "insets dialog", "[grow]", "[grow][]");
        JScrollPane paneDevices = gui.scrollPane(lstDevices);
        lstDevices.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JButton btnShowSettings = new JButton(showDeviceSettingsAction);
        JButton btnShowGUI = new JButton(showDeviceGuiAction);

        if (computer.getDevices().isEmpty()) {
            peripheralPanel.add(gui.label("No peripheral devices configured."), "wrap");
        }
        peripheralPanel.add(paneDevices, "grow, wrap");
        peripheralPanel.add(btnShowSettings, "split 2, sizegroup btns, tag ok");
        peripheralPanel.add(btnShowGUI, "sizegroup btns, tag cancel");

        JScrollPane statusScroll = gui.scrollPane(statusWindow);
        statusScroll.setMinimumSize(new Dimension(280, 140));
        JPanel watches = gui.section("Watches", "insets 0, fill", "[grow]", "[grow]");
        watches.add(inspection != null ? inspection.watchesPanel() : gui.label("Watches need a CPU."), "grow");
        watches.setMinimumSize(new Dimension(280, 170));
        cpuWatches = gui.splitPane(JSplitPane.VERTICAL_SPLIT, statusScroll, watches, 0.55);
        cpuWatches.setName("debuggerCpuWatches");
        peripheralPanel.setMinimumSize(new Dimension(280, 110));
        sidebar = gui.splitPane(JSplitPane.VERTICAL_SPLIT, cpuWatches, peripheralPanel, 1.0);
        sidebar.setName("debuggerSidebar");
        sidebar.setPreferredSize(new Dimension(360, 650));
        sidebar.setMinimumSize(new Dimension(280, 420));

        JComponent workspace = inspection != null ? inspection : debuggerPanel;
        workspace.setMinimumSize(new Dimension(360, 180));
        workspace.setPreferredSize(new Dimension(680, 650));
        splitLeftRight = gui.splitPane(JSplitPane.HORIZONTAL_SPLIT, workspace, sidebar, 0.75);
        splitLeftRight.setContinuousLayout(false);
        splitLeftRight.setName("debuggerColumns");
        splitLeftRight.setDividerLocation(700);
        splitLeftRight.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                if (isShowing()) resizeComponents(getHeight());
            }
        });

        setLayout(new net.miginfocom.swing.MigLayout("insets dialog, fill", "[grow]", "[][grow]"));
        JScrollPane headerScroll = gui.scrollPane(toolDebug);
        headerScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        headerScroll.setMinimumSize(new Dimension(0, 0));
        add(headerScroll, "growx, hmin pref, wrap");
        add(splitLeftRight, "grow");
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                resizeComponents(getHeight());
            }
        });

        this.memoryListener = new MemoryContext.MemoryListener() {
            @Override
            public void memoryContentChanged(int fromLocation, int toLocation) {
                runOnEdt(() -> {
                    if (closed) return;
                    debugTableModel.memoryChanged(fromLocation, toLocation + 1);
                    refreshDebugTable();
                });
            }

            @Override
            public void memorySizeChanged() {
                runOnEdt(EmulatorPanel.this::refreshDebugTable);
            }
        };

        if (cpu != null && execution != null) {
            executionListener = new CPUContext.CPUListener() {
                @Override public void runStateChanged(CPU.RunState state) {
                    runOnEdt(() -> {
                        if (closed) return;
                        runState = state;
                        if (runState == CPU.RunState.STATE_RUNNING) setStateRunning();
                        else setStateNotRunning(runState, execution.isTimedRunning());
                    });
                }
                @Override public void internalStateChanged() {
                    runOnEdt(() -> {
                        if (closed) return;
                        commandBusy = false;
                        updateExecutionControls();
                        refreshDebugTable();
                    });
                }
            };
            execution.cpuContext().addCPUListener(executionListener);
            runState = execution.getRunState();
        }

        // initial state
        setStateNotRunning(runState, false);
    }

    public Optional<DebuggerPanel> getDebuggerPanel() {
        return Optional.ofNullable(inspection);
    }

    public void resizeComponents(int height) {
        // Establish the initial proportions once; resizing must preserve the user's dividers.
        if (!dividersInitialized && height > 0 && splitLeftRight.getWidth() > 0) {
            splitLeftRight.setDividerLocation(Math.max(360, splitLeftRight.getWidth() - 370));
            sidebar.setDividerLocation(Math.max(310, splitLeftRight.getHeight() - PERIPHERAL_PANEL_HEIGHT));
            JScrollPane status = (JScrollPane) cpuWatches.getTopComponent();
            int preferredStatusHeight = status.getViewport().getView().getPreferredSize().height + 12;
            int available = Math.max(140, splitLeftRight.getHeight() - PERIPHERAL_PANEL_HEIGHT - 170);
            cpuWatches.setDividerLocation(Math.min(Math.max(220, preferredStatusHeight), available));
            dividersInitialized = true;
        }
    }

    private void navigateToSource(int viewRow, Consumer<SourceCodePosition> sourceNavigator) {
        if (memoryContext == null) return;
        int address = debugTableModel.getLocationAt(debugTable.convertRowIndexToModel(viewRow));
        if (address < 0) return;
        if (session != null) session.sourceAt(address).ifPresent(sourceNavigator);
    }

    public CPU.RunState getRunState() {
        return runState;
    }

    private void setStatusPanel(JPanel statusPanel) {
        statusWindow.removeAll();
        statusWindow.add(statusPanel, "grow");
        statusWindow.revalidate();
        statusWindow.repaint();
    }

    @Override
    public void removeNotify() {
        close();
        super.removeNotify();
    }

    public void close() {
        if (closed) return;
        closed = true;
        if (inspection != null) inspection.close();
        if (executionListener != null) execution.cpuContext().removeCPUListener(executionListener);
        if (memoryContext != null) memoryContext.removeMemoryListener(memoryListener);
    }

    private void setupDebugToolbar() {
        toolDebug.add(gui.toolbarButton(resetAction));
        if (inspection != null) toolDebug.add(inspection.recordingControl());
        toolDebug.addSeparator();
        toolDebug.add(gui.toolbarButton(jumpToBeginningAction));
        toolDebug.add(gui.toolbarButton(stepBackAction));
        toolDebug.add(gui.toolbarButton(stopAction));
        toolDebug.add(gui.toolbarButton(pauseAction));
        toolDebug.add(gui.toolbarButton(runAction));
        toolDebug.add(gui.toolbarButton(runTimedAction));
        toolDebug.add(gui.toolbarButton(stepAction));
        toolDebug.addSeparator();
        toolDebug.add(gui.toolbarButton(jumpAction));
        toolDebug.addSeparator();
        toolDebug.add(gui.toolbarButton(breakpointAction));
        toolDebug.addSeparator();
        toolDebug.add(gui.toolbarButton(showMemoryAction));
        for (java.awt.Component component : toolDebug.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                button.setFocusable(true);
                button.getAccessibleContext().setAccessibleName(button.getToolTipText());
            }
        }
    }

    private void refreshDebugTable() {
        if (!closed && debugTable.isEnabled()) {
            debugTable.repaint();
        }
    }

    private void stepCompleted() {
        if (!closed && inspection != null) inspection.stepCompleted();
    }

    private void setStateNotRunning(CPU.RunState state, boolean timedRunning) {
        resetAction.setEnabled(true);
        jumpAction.setEnabled(true);
        breakpointAction.setEnabled(inspection != null && inspection.isBreakpointDialogSupported());
        pauseAction.setEnabled(false);
        stepBackAction.setEnabled(true);
        jumpToBeginningAction.setEnabled(true);
        paneDebug.setEnabled(true);
        debugTable.setEnabled(true);
        debugTable.setVisible(true);
        paneDebug.setVisible(true);
        panelPages.setVisible(true);
        panelPages.setEnabled(true);
        if (inspection != null) inspection.setExecutionRunning(false);

        if (state == CPU.RunState.STATE_STOPPED_BREAK && !timedRunning) {
            stopAction.setEnabled(true);
            runTimedAction.setEnabled(true);
            runAction.setEnabled(true);
            stepAction.setEnabled(true);
        } else if (state == CPU.RunState.STATE_STOPPED_BREAK) {
            stopAction.setEnabled(true);
            runTimedAction.setEnabled(false);
            pauseAction.setEnabled(true);
            runAction.setEnabled(false);
            stepAction.setEnabled(false);
            stepBackAction.setEnabled(false);
            jumpToBeginningAction.setEnabled(false);
        } else {
            stopAction.setEnabled(false);
            runTimedAction.setEnabled(false);
            runAction.setEnabled(false);
            stepAction.setEnabled(false);
            debugTableModel.currentPage();
        }
        refreshDebugTable();

        Optional.ofNullable(memoryContext).ifPresent(m -> {
            m.removeMemoryListener(memoryListener);
            m.addMemoryListener(memoryListener);
        });
    }

    private void setStateRunning() {
        resetAction.setEnabled(true);
        jumpAction.setEnabled(session != null && session.debug != null);
        breakpointAction.setEnabled(inspection != null && inspection.isBreakpointDialogSupported());
        stopAction.setEnabled(true);
        stepBackAction.setEnabled(false);
        runAction.setEnabled(false);
        stepAction.setEnabled(false);
        jumpToBeginningAction.setEnabled(session != null && session.debug != null);
        pauseAction.setEnabled(true);
        runTimedAction.setEnabled(false);
        debugTable.setEnabled(false);
        paneDebug.setEnabled(false);
        if (inspection != null) inspection.setExecutionRunning(true);

        Optional.ofNullable(memoryContext).ifPresent(m -> m.removeMemoryListener(memoryListener));
    }

    private void updateExecutionControls() {
        if (closed || execution == null) return;
        boolean busy = execution.isBusy();
        if (!busy && commandBusy) {
            if (runState == CPU.RunState.STATE_RUNNING) setStateRunning();
            else setStateNotRunning(runState, execution.isTimedRunning());
        }
        commandBusy = busy;
        if (busy) {
            resetAction.setEnabled(false);
            stopAction.setEnabled(false);
            pauseAction.setEnabled(false);
            runAction.setEnabled(false);
            runTimedAction.setEnabled(false);
            stepAction.setEnabled(false);
            stepBackAction.setEnabled(false);
            jumpAction.setEnabled(false);
            jumpToBeginningAction.setEnabled(false);
            breakpointAction.setEnabled(false);
            debugTable.setEnabled(false);
            paneDebug.setEnabled(false);
            panelPages.setEnabled(false);
            if (inspection != null) inspection.setExecutionRunning(true);
        }
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
