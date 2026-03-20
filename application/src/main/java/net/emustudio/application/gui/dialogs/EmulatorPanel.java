/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.actions.emulator.*;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.PagesPanel;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.application.gui.GUIProvider;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmulatorPanel extends JPanel {
    private final static int MIN_PERIPHERAL_PANEL_HEIGHT = 100;

    private final JPanel statusWindow = new JPanel();
    private final GroupLayout statusWindowLayout = new GroupLayout(statusWindow);

    private final JToolBar toolDebug = GUIProvider.getGUI().toolBar();
    private final JPanel panelPages;
    private final JScrollPane paneDebug;

    private final JList<String> lstDevices = new JList<>();
    private final JSplitPane splitPerDebug;

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
    private final MemoryContext.MemoryListener memoryListener;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    public EmulatorPanel(JFrame parent, VirtualComputer computer, DebugTableModel debugTableModel, Dialogs dialogs,
                         EmulationController emulationController, MemoryContext<?> memoryContext) {
        this.memoryContext = memoryContext;
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.debugTable = new DebugTableImpl(debugTableModel);

        paneDebug = GUIProvider.getGUI().scrollPane(debugTable);
        paneDebug.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        debugTable.setFillsViewportHeight(true);

        GUIProvider.getGUI().styleTable(debugTable);

        paneDebug.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                debugTable.dispatchEvent(e); // Debug table is not shrinking, just expanding...
            }
        });

        statusWindow.setBorder(BorderFactory.createTitledBorder("Status"));
        statusWindow.setLayout(statusWindowLayout);

        computer.getCPU().flatMap(cpu -> Optional.ofNullable(cpu.getStatusPanel())).ifPresent(this::setStatusPanel);

        this.stepBackAction = new StepBackAction(computer, debugTableModel, this::refreshDebugTable);
        this.resetAction = new ResetAction(emulationController);
        this.jumpToBeginningAction = new JumpToBeginningAction(computer, this::refreshDebugTable);
        this.stopAction = new StopAction(emulationController);
        this.pauseAction = new PauseAction(emulationController, () -> this.setStateNotRunning(CPU.RunState.STATE_STOPPED_BREAK, false));
        this.runAction = new RunAction(emulationController, debugTable);
        this.runTimedAction = new RunTimedAction(emulationController, dialogs);
        this.stepAction = new StepAction(emulationController);
        this.jumpAction = new JumpAction(computer, dialogs, this::refreshDebugTable);
        this.breakpointAction = new BreakpointAction(parent, computer, dialogs, this::refreshDebugTable);
        this.showMemoryAction = new ShowMemoryAction(parent, computer, dialogs);

        showMemoryAction.setEnabled(computer.getMemory().filter(Memory::isShowSettingsSupported).isPresent());
        breakpointAction.setEnabled(computer.getCPU().filter(CPU::isBreakpointSupported).isPresent());

        setupDebugToolbar();

        panelPages = PagesPanel.create(debugTableModel, dialogs);

        JPanel debuggerPanel = GUIProvider.getGUI().section("Debugger", "insets dialog", "[grow]", "[][grow][]");
        debuggerPanel.add(toolDebug, "growx, wrap");
        debuggerPanel.add(paneDebug, "grow, wrap");
        debuggerPanel.add(panelPages, "growx");

        this.showDeviceSettingsAction = new ShowDeviceSettingsAction(parent, computer, dialogs, lstDevices::getSelectedIndex);
        this.showDeviceGuiAction = new ShowDeviceGuiAction(parent, computer, dialogs, lstDevices::getSelectedIndex);

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
                if (e.getClickCount() == 2) {
                    showDeviceGuiAction.actionPerformed(new ActionEvent(this, 0, ""));
                }
            }
        });

        JPanel peripheralPanel = GUIProvider.getGUI().section("Peripheral devices", "insets dialog", "[grow]", "[grow][]");
        JScrollPane paneDevices = GUIProvider.getGUI().scrollPane(lstDevices);
        GUIProvider.getGUI().styleList(lstDevices);

        JButton btnShowSettings = new JButton(showDeviceSettingsAction);
        JButton btnShowGUI = new JButton(showDeviceGuiAction);

        peripheralPanel.add(paneDevices, "grow, wrap");
        peripheralPanel.add(btnShowSettings, "split 2, sizegroup btns, tag ok");
        peripheralPanel.add(btnShowGUI, "sizegroup btns, tag cancel");

        splitPerDebug = GUIProvider.getGUI().splitPaneTopToBottom(debuggerPanel, peripheralPanel, 1.0);
        splitPerDebug.setDividerLocation(500);
        splitPerDebug.setAutoscrolls(true);

        JSplitPane splitLeftRight = GUIProvider.getGUI().splitPaneLeftToRight(splitPerDebug, statusWindow, 1.0);
        splitLeftRight.setFocusable(false);
        splitLeftRight.setDividerLocation(1.0);

        setLayout(new net.miginfocom.swing.MigLayout("insets dialog, fill", "[grow]", "[grow]"));
        add(splitLeftRight, "grow");

        this.memoryListener = new MemoryContext.MemoryListener() {
            @Override
            public void memoryContentChanged(int fromLocatiom, int toLocation) {
                debugTableModel.memoryChanged(fromLocatiom, toLocation + 1);
                refreshDebugTable();
            }

            @Override
            public void memorySizeChanged() {
                debugTableModel.memorySizeChanged(memoryContext == null ? 0 : memoryContext.getSize());
            }
        };

        computer.getCPU().ifPresent(cpu -> cpu.addCPUListener(new CPU.CPUListener() {

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
                    setStateNotRunning(state, Optional.ofNullable(emulationController).filter(EmulationController::isTimedRunning).isPresent());
                }
            }
        }));

        // initial state
        setStateNotRunning(runState, false);
    }

    public void resizeComponents(int height) {
        double rowHeight = debugTable.getRowHeight();
        double additionalHeight = toolDebug.getHeight() + panelPages.getHeight() + 140;
        double heightTogether = additionalHeight + rowHeight * debugTableModel.getRowCount();

        if (heightTogether + MIN_PERIPHERAL_PANEL_HEIGHT > height) {
            heightTogether = Math.max(0, height - MIN_PERIPHERAL_PANEL_HEIGHT);
        }

        double dividerLocation = Math.min(1.0, heightTogether / (double) height);
        splitPerDebug.setDividerLocation(dividerLocation);
    }

    public CPU.RunState getRunState() {
        return runState;
    }


    private void setStatusPanel(JPanel statusPanel) {
        statusWindowLayout.setHorizontalGroup(statusWindowLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(statusPanel));
        statusWindowLayout.setVerticalGroup(statusWindowLayout.createSequentialGroup().addComponent(statusPanel));
    }

    private void setupDebugToolbar() {
        toolDebug.add(GUIProvider.getGUI().toolbarButton(resetAction));
        toolDebug.addSeparator();
        toolDebug.add(GUIProvider.getGUI().toolbarButton(jumpToBeginningAction));
        toolDebug.add(GUIProvider.getGUI().toolbarButton(stepBackAction));
        toolDebug.add(GUIProvider.getGUI().toolbarButton(stopAction));
        toolDebug.add(GUIProvider.getGUI().toolbarButton(pauseAction));
        toolDebug.add(GUIProvider.getGUI().toolbarButton(runAction));
        toolDebug.add(GUIProvider.getGUI().toolbarButton(runTimedAction));
        toolDebug.add(GUIProvider.getGUI().toolbarButton(stepAction));
        toolDebug.addSeparator();
        toolDebug.add(GUIProvider.getGUI().toolbarButton(jumpAction));
        toolDebug.addSeparator();
        toolDebug.add(GUIProvider.getGUI().toolbarButton(breakpointAction));
        toolDebug.addSeparator();
        toolDebug.add(GUIProvider.getGUI().toolbarButton(showMemoryAction));
    }

    private void refreshDebugTable() {
        if (debugTable.isEnabled()) {
            debugTable.repaint();
        }
    }


    private void setStateNotRunning(CPU.RunState state, boolean timedRunning) {
        pauseAction.setEnabled(false);
        stepBackAction.setEnabled(true);
        jumpToBeginningAction.setEnabled(true);
        paneDebug.setEnabled(true);
        debugTable.setEnabled(true);
        debugTable.setVisible(true);
        panelPages.setVisible(true);

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

        Optional.ofNullable(memoryContext).ifPresent(m -> m.addMemoryListener(memoryListener));
    }

    private void setStateRunning() {
        stopAction.setEnabled(true);
        stepBackAction.setEnabled(false);
        runAction.setEnabled(false);
        stepAction.setEnabled(false);
        jumpToBeginningAction.setEnabled(false);
        pauseAction.setEnabled(true);
        runTimedAction.setEnabled(false);
        debugTable.setEnabled(false);
        debugTable.setVisible(false);
        paneDebug.setEnabled(false);
        panelPages.setVisible(false);

        Optional.ofNullable(memoryContext).ifPresent(m -> m.removeMemoryListener(memoryListener));
    }
}
