package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.ToolbarButton;
import net.emustudio.application.gui.actions.emulator.*;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.PagesPanel;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmulatorPanel extends JPanel {
    private final static int MIN_PERIPHERAL_PANEL_HEIGHT = 100;

    private final JPanel statusWindow = new JPanel();
    private final GroupLayout statusWindowLayout = new GroupLayout(statusWindow);

    private final JToolBar toolDebug = new JToolBar();
    private final JPanel panelPages;
    private final JScrollPane paneDebug = new JScrollPane();

    private final JList<String> lstDevices = new JList<>();
    private final JSplitPane splitPerDebug = new JSplitPane();

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
    private final Memory.MemoryListener memoryListener;
    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_BREAK;

    public EmulatorPanel(JFrame parent, VirtualComputer computer, DebugTableModel debugTableModel, Dialogs dialogs,
                         EmulationController emulationController, MemoryContext<?> memoryContext) {
        this.memoryContext = memoryContext;
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        if (memoryContext != null) {
            this.debugTableModel.setMemorySize(memoryContext.getSize());
        }

        this.debugTable = new DebugTableImpl(debugTableModel);

        paneDebug.setViewportView(debugTable);
        paneDebug.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        debugTable.setFillsViewportHeight(true);

        paneDebug.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                debugTable.dispatchEvent(e); // Debug table is not shrinking, just expanding...
            }
        });

        statusWindow.setBorder(BorderFactory.createTitledBorder("Status"));
        statusWindow.setLayout(statusWindowLayout);

        computer.getCPU()
            .flatMap(cpu -> Optional.ofNullable(cpu.getStatusPanel()))
            .ifPresent(this::setStatusPanel);

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

        JPanel debuggerPanel = new JPanel();
        debuggerPanel.setBorder(BorderFactory.createTitledBorder("Debugger"));
        GroupLayout debuggerPanelLayout = new GroupLayout(debuggerPanel);

        debuggerPanelLayout.setAutoCreateGaps(true);
        debuggerPanelLayout.setAutoCreateContainerGaps(true);

        debuggerPanel.setLayout(debuggerPanelLayout);
        debuggerPanelLayout.setHorizontalGroup(
            debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(toolDebug)
                .addComponent(paneDebug)
                .addComponent(panelPages));
        debuggerPanelLayout.setVerticalGroup(
            debuggerPanelLayout.createSequentialGroup()
                .addComponent(toolDebug, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(paneDebug, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelPages, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));

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

        JPanel peripheralPanel = new JPanel();
        peripheralPanel.setBorder(BorderFactory.createTitledBorder("Peripheral devices"));
        JScrollPane paneDevices = new JScrollPane();
        paneDevices.setViewportView(lstDevices);

        JButton btnShowSettings = new JButton(showDeviceSettingsAction);
        JButton btnShowGUI = new JButton(showDeviceGuiAction);

        GroupLayout peripheralPanelLayout = new GroupLayout(peripheralPanel);
        peripheralPanel.setLayout(peripheralPanelLayout);
        peripheralPanelLayout.setHorizontalGroup(
            peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(paneDevices)
                .addGroup(GroupLayout.Alignment.TRAILING,
                    peripheralPanelLayout.createSequentialGroup()
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

        splitPerDebug.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitPerDebug.setDividerLocation(500);
        splitPerDebug.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPerDebug.setAutoscrolls(true);
        splitPerDebug.setContinuousLayout(true);
        splitPerDebug.setTopComponent(debuggerPanel);
        splitPerDebug.setRightComponent(peripheralPanel);

        JSplitPane splitLeftRight = new JSplitPane();
        splitLeftRight.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitLeftRight.setContinuousLayout(true);
        splitLeftRight.setFocusable(false);
        splitLeftRight.setDividerLocation(1.0);
        splitLeftRight.setRightComponent(statusWindow);
        splitLeftRight.setLeftComponent(splitPerDebug);

        GroupLayout panelEmulatorLayout = new GroupLayout(this);
        setLayout(panelEmulatorLayout);
        panelEmulatorLayout.setHorizontalGroup(
            panelEmulatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(splitLeftRight));
        panelEmulatorLayout.setVerticalGroup(
            panelEmulatorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitLeftRight, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addContainerGap());

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
        statusWindowLayout.setHorizontalGroup(
            statusWindowLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(statusPanel));
        statusWindowLayout.setVerticalGroup(
            statusWindowLayout.createSequentialGroup()
                .addComponent(statusPanel));
    }

    private void setupDebugToolbar() {
        toolDebug.setFloatable(false);
        toolDebug.setRollover(true);
        toolDebug.setBorderPainted(false);

        toolDebug.add(new ToolbarButton(resetAction, "Reset emulation"));
        toolDebug.addSeparator();
        toolDebug.add(new ToolbarButton(jumpToBeginningAction, "Jump to beginning"));
        toolDebug.add(new ToolbarButton(stepBackAction,"Step back"));
        toolDebug.add(new ToolbarButton(stopAction, "Stop emulation"));
        toolDebug.add(new ToolbarButton(pauseAction, "Pause emulation"));
        toolDebug.add(new ToolbarButton(runAction, "Run emulation"));
        toolDebug.add(new ToolbarButton(runTimedAction, "Run \"timed\" emulation"));
        toolDebug.add(new ToolbarButton(stepAction, "Step forward"));
        toolDebug.addSeparator();
        toolDebug.add(new ToolbarButton(jumpAction, "Jump to address"));
        toolDebug.addSeparator();
        toolDebug.add(new ToolbarButton(breakpointAction, "Set/unset breakpoint to address..."));
        toolDebug.addSeparator();
        toolDebug.add(new ToolbarButton(showMemoryAction, "Show operating memory"));
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
