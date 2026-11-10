/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import com.formdev.flatlaf.util.ScaledImageIcon;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.gui.dialogs.RecordingDialog;
import net.emustudio.emulib.plugin.cpu.*;
import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.ValueView;
import net.emustudio.emulib.plugin.debugger.ViewRow;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.application.ui.GUI;
import net.emustudio.emulib.application.ui.Dialogs;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import static net.emustudio.emulib.ui.StandardIcons.*;

/** Unified debugger workspace over CPU-visible addresses; browsing never changes PC. */
public final class DebuggerPanel extends JPanel implements AutoCloseable {
    @FunctionalInterface public interface AddressSelectionListener {
        void selected(int address, boolean visit);
    }

    private AddressSelectionListener addressSelectionListener = (address, visit) -> {};
    private Runnable inspectionReadyListener = () -> {};
    private java.util.function.Supplier<List<SourceAddressMap.Symbol>> compiledSymbols = List::of;
    private boolean sourceMappingsCurrent;
    private final DebuggerSession session;
    private final EmuStudioGui gui;
    private final Consumer<SourceCodePosition> sourceNavigator;
    private final Runnable breakpointsChanged;
    private final AnalysisTasks tasks;
    private final Map<Component, Boolean> enabledControls = new IdentityHashMap<>();
    private boolean disposed;
    private final AtomicBoolean refreshQueued = new AtomicBoolean();
    private boolean initializationPending;
    private JToggleButton recordingControl;
    private boolean recordingChangePending, updatingWatchFormats;
    private final Dialogs dialogs;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel debuggerDiagnostic = new JLabel();
    private String viewError = "";
    private final JPanel watchPanel;
    private AnnotationForm annotationForm;
    private final JPanel annotationList = new JPanel(new CardLayout());
    private final JLabel annotationEmpty = new JLabel("", SwingConstants.CENTER);
    private final JTextField annotationFilter;
    private JButton editAnnotation, deleteAnnotation;
    private final JPopupMenu annotationResults = new JPopupMenu();
    private List<InstructionRecording.Event> displayedHistory = List.of();
    private final List<int[]> historyGroups = new ArrayList<>();
    private final HistoryModel historyModel = new HistoryModel();
    private final JTable history = new JTable(historyModel);
    private final JComboBox<String> historyNavigation = new JComboBox<>();
    private List<NavLink> selectedHistoryLinks = List.of();
    private List<Long> selectedHistorySequences = List.of();
    private final JCheckBox foldHistory = new JCheckBox("Fold identical repeats", true);
    private final JCheckBox foldFlow = new JCheckBox("Fold identical repeats", true);
    private final JComboBox<String> historyOrigin = new JComboBox<>(new String[] {"Current recording"});
    private final RecordingArchive recordings = new RecordingArchive();
    private int recordingsCreated;
    private boolean updatingHistoryOrigin;
    private final JPanel inspectionBody = new JPanel(new CardLayout());
    private final JPanel historyBody = new JPanel(new CardLayout());
    private final JLabel historyEmptyTitle = new JLabel(), historyEmptyHint = new JLabel();
    private JButton historyEmptyRecord;
    private boolean browsingRecording;
    private int inspectedAddress;
    private final JComboBox<String> watchFormat = new JComboBox<>();
    private final JCheckBox showUnchangedValues = new JCheckBox("Show unchanged values");
    private final WatchTableModel watchRows = new WatchTableModel();
    private final DefaultListModel<UserAnnotations.Entry> annotationRows = new DefaultListModel<>();
    private final JTable watches = new JTable(watchRows);
    private final JList<UserAnnotations.Entry> annotations = new JList<>(annotationRows);
    private final JTextArea historicalState;
    private final DebugTableImpl table;
    private final DebugTableModel instructionModel;
    private final boolean byteMemory;
    private final InstructionAnnotationsModel instructionRows;
    private final JComponent instructionView;
    private boolean running;
    private int lastInstructionAddress;
    private final GraphPanel graph = new GraphPanel();
    private JButton goToFlowInstruction;
    private JToolBar flowAnnotations;
    private int flowAnalysisLimit = 1000;
    private final List<JButton> inspectionButtons = new ArrayList<>();
    private boolean flowFromPc;
    private UserAnnotations index;
    private boolean navigating;
    private final MemoryContext.MemoryListener memoryListener = new MemoryContext.MemoryListener() {
        @Override public void memoryContentChanged(int from, int to) { requestRefresh(); }
        @Override public void memorySizeChanged() { requestRefresh(); }
    };
    private final CPUContext.CPUListener stateListener = new CPUContext.CPUListener() {
        @Override public void runStateChanged(CPU.RunState state) { refreshExecutionState(); }
        @Override public void internalStateChanged() { refreshExecutionState(); }
    };

    private void refreshExecutionState() {
        CPUContext context = session.execution().cpuContext();
        executionChanged(context.getRunState(), context.getInstructionLocation());
    }
    private final AtomicBoolean executionRefreshQueued = new AtomicBoolean();
    private volatile CPU.RunState pendingExecutionState;
    private volatile int pendingInstructionLocation;
    private CPU.RunState appliedExecutionState;
    private int appliedInstructionLocation;
    private boolean pendingPcFollow;
    private int pendingPcAddress = -1;

    private void executionChanged(CPU.RunState state, int instructionLocation) {
        pendingExecutionState = state;
        pendingInstructionLocation = instructionLocation;
        if (!executionRefreshQueued.compareAndSet(false, true)) return;
        SwingUtilities.invokeLater(() -> {
            executionRefreshQueued.set(false);
            if (disposed) return;
            CPU.RunState nextState = pendingExecutionState;
            int nextLocation = pendingInstructionLocation;
            if (appliedExecutionState != nextState || appliedInstructionLocation != nextLocation) {
                pendingPcFollow = true;
                pendingPcAddress = nextLocation;
            }
            appliedExecutionState = nextState;
            appliedInstructionLocation = nextLocation;
            setExecutionRunning(nextState == CPU.RunState.STATE_RUNNING);
            refreshDiagnostic();
            refreshWatches();
            if (running) {
                tasks.cancel();
                updateRecordingControls(false);
                return;
            }
            try { session.requirePaused(); } catch (IllegalStateException ignored) { return; }
            if (tasks.isBusy()) {
                if (index == null) initializationPending = true;
                return;
            }
            if (byteMemory && index == null) { initializeAnnotations(); return; }
            if (inspectBreakpointHit()) { pendingPcFollow = false; refresh(); return; }
            followPendingPc();
            requestRefresh();
        });
    }

    private void followPendingPc() {
        if (!pendingPcFollow || disposed || running || tasks.isBusy()) return;
        pendingPcFollow = false;
        instructionModel.currentPage();
        lastInstructionAddress = pendingPcAddress;
        if (lastInstructionAddress >= 0)
            guard(() -> navigate(lastInstructionAddress, false, NavigationTarget.LISTING));
    }

    public DebuggerPanel(
            DebuggerSession session,
            DebugTableImpl table,
            JComponent instructionView,
            Consumer<SourceCodePosition> sourceNavigator,
            Runnable breakpointsChanged,
            EmuStudioGui gui, Dialogs dialogs) {
        this.gui = Objects.requireNonNull(gui);
        this.table = Objects.requireNonNull(table);
        this.instructionView = Objects.requireNonNull(instructionView);
        this.instructionModel = table.instructionModel();
        this.byteMemory = session.memory != null && session.memory.getCellTypeClass() == Byte.class
                && session.memory.getSize() > 0;
        this.instructionRows = new InstructionAnnotationsModel();
        instructionModel.addTableModelListener(instructionRows);
        this.dialogs = Objects.requireNonNull(dialogs);
        annotationFilter = gui.textField("", 14);
        lastInstructionAddress = session.execution().cpuContext().getInstructionLocation();
        inspectedAddress = lastInstructionAddress;
        historicalState = gui.textAreaReadOnly(50, 10);
        annotations.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.session = session;
        if (instructionModel instanceof DebugTableModelImpl)
            ((DebugTableModelImpl) instructionModel).setDebuggerSession(session);
        this.sourceNavigator = sourceNavigator;
        this.breakpointsChanged = Objects.requireNonNull(breakpointsChanged);
        tasks = new AnalysisTasks(session, this::setBusy, this::showRuntimeFailure);
        table.setInstructionRowPresentation(row -> {
            if (table.getModel() != instructionRows || disposed || running || tasks.isBusy()) return false;
            int address = instructionModel.getLocationAt(row);
            return address >= 0 && session.isBreakpointSet(address);
        }, row -> {
            if (table.getModel() != instructionRows || disposed || running || tasks.isBusy()) return null;
            return annotationTooltip(instructionModel.getLocationAt(row));
        });
        table.setRowBackground(row -> {
            if (table.getModel() != instructionRows || disposed || running || tasks.isBusy()) return null;
            return annotationBackground(instructionModel.getLocationAt(row));
        });
        setLayout(new BorderLayout(6, 6));
        tabs.setName("debuggerViews");
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Debugger", instructionView);
        watchPanel = watchPanel();
        tabs.addTab("Inspection", flowPanel());
        add(tabs, BorderLayout.CENTER);
        debuggerDiagnostic.getAccessibleContext().setAccessibleName("Debugger diagnostic");
        debuggerDiagnostic.putClientProperty("html.disable", true);
        debuggerDiagnostic.setVisible(false);
        add(debuggerDiagnostic, BorderLayout.SOUTH);
        tabs.addChangeListener(event -> activateView());
        activateView();
        session.execution().cpuContext().addCPUListener(stateListener);
        executionChanged(session.execution().getRunState(), session.execution().getInstructionLocation());
        if (session.memory != null) session.memory.addMemoryListener(memoryListener);
        setMinimumSize(new Dimension(300, 160));
        JPopupMenu selectionMenu = new JPopupMenu();
        JMenuItem inspect = new JMenuItem("Inspect");
        inspect.addActionListener(event -> guard(() -> {
            int address = selectedInstructionAddress();
            navigate(address);
            tabs.setSelectedIndex(tabs.indexOfTab("Inspection"));
            analyzeFlow(address);
        }));
        JMenuItem goToSource = new JMenuItem("Go to source");
        goToSource.addActionListener(event -> guard(() -> showSource(selectedInstructionAddress())));
        selectionMenu.add(inspect);
        selectionMenu.add(goToSource);
        selectionMenu.addSeparator();
        JMenuItem toggleBreakpoint = new JMenuItem("Set breakpoint");
        toggleBreakpoint.addActionListener(event -> guard(() -> {
            int address = selectedInstructionAddress();
            applyBreakpointRequest(new BreakpointDialog.Request(
                    session.isBreakpointSet(address) ? BreakpointDialog.Operation.UNSET : BreakpointDialog.Operation.SET,
                    address));
        }));
        selectionMenu.add(toggleBreakpoint);
        selectionMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) {
                boolean enabled = isInstructionView() && table.getSelectedRow() >= 0 && !running && !tasks.isBusy();
                inspect.setEnabled(enabled);
                goToSource.setEnabled(enabled);
                boolean validAddress = enabled && instructionModel.getLocationAt(
                        table.convertRowIndexToModel(table.getSelectedRow())) >= 0;
                toggleBreakpoint.setEnabled(validAddress && isBreakpointDialogSupported());
                toggleBreakpoint.setText(validAddress && session.isBreakpointSet(selectedInstructionAddress())
                        ? "Unset breakpoint" : "Set breakpoint");
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event) {}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event) {}
        });
        table.addMouseListener(new MouseAdapter() {
            private void selectPopupRow(MouseEvent event) {
                if (!event.isPopupTrigger() && !SwingUtilities.isRightMouseButton(event)) return;
                int row = table.rowAtPoint(event.getPoint());
                if (isInstructionView()) {
                    if (row >= 0) table.setRowSelectionInterval(row, row);
                    else table.clearSelection();
                }
            }
            @Override public void mousePressed(MouseEvent event) { selectPopupRow(event); }
            @Override public void mouseReleased(MouseEvent event) {
                // Right-button press already selected the instruction and may have recentered the listing.
                if (!SwingUtilities.isRightMouseButton(event)) selectPopupRow(event);
            }
        });
        table.setComponentPopupMenu(selectionMenu);
        refresh();
        if (session.memory != null && session.memory.getCellTypeClass() == Byte.class && session.memory.getSize() > 0)
            initializeAnnotations();
        List<String> loadWarnings = session.takeLoadWarnings();
        if (!loadWarnings.isEmpty())
            SwingUtilities.invokeLater(() -> dialogs.showError(String.join("\n", loadWarnings), "Debugger settings"));
    }

    private void initializeAnnotations() {
        if (session.memory == null || session.memory.getCellTypeClass() != Byte.class || session.memory.getSize() == 0) return;
        guard(() -> tasks.submit(session::annotations, this::showAnnotations));
    }

    public JComponent watchesPanel() {
        return watchPanel;
    }

    public JToggleButton recordingControl() {
        return recordingControl;
    }

    public void setAddressSelectionListener(AddressSelectionListener listener) {
        addressSelectionListener = Objects.requireNonNull(listener);
    }

    public void setInspectionReadyListener(Runnable listener) {
        inspectionReadyListener = Objects.requireNonNull(listener);
    }

    public void setSourceMappingsCurrent(boolean current) {
        sourceMappingsCurrent = current;
        graph.invalidateLabels();
        configureInstructionColumns();
        table.repaint();
        graph.repaint();
    }

    private void notifyAddressSelection(boolean visit) {
        graph.repaint();
        if (!disposed && !running && !tasks.isBusy())
            addressSelectionListener.selected(inspectedAddress, visit);
    }

    /** Automatic source following keeps the active analysis tab. */
    public void synchronizeSourceAddress(SourceAddressMap.Location location) {
        if (!location.instruction) return;
        alignInstructionAddress(location.address);
        navigate(location.address, true, location.instruction ? NavigationTarget.SOURCE_INSTRUCTION : NavigationTarget.SOURCE_DATA);
    }

    public void alignInstructionAddress(int address) {
        if (lastInstructionAddress == address) return;
        lastInstructionAddress = address;
        instructionModel.showAddress(address);
    }

    private void selectInstructionAddress() {
        if (table.getModel() != instructionRows || running || disposed) return;
        boolean wasNavigating = navigating;
        navigating = true;
        try {
            for (int row = 0; row < instructionModel.getRowCount(); row++) {
                if (instructionModel.getLocationAt(row) == lastInstructionAddress) {
                    table.setRowSelectionInterval(row, row);
                    break;
                }
            }
        } finally { navigating = wasNavigating; }
    }

    public void setSymbols(java.util.function.Supplier<List<SourceAddressMap.Symbol>> values) {
        compiledSymbols = Objects.requireNonNull(values);
        graph.invalidateLabels();
        table.repaint();
        graph.repaint();
    }

    @Override
    public void close() {
        if (disposed) return;
        disposed = true;
        annotationResults.setVisible(false);
        annotationFilter.setComponentPopupMenu(null);
        tasks.close();
        session.execution().cpuContext().removeCPUListener(stateListener);
        if (session.memory != null) session.memory.removeMemoryListener(memoryListener);
        instructionModel.removeTableModelListener(instructionRows);
    }

    private void showAnnotations(UserAnnotations loaded) {
        viewError = "";
        refreshDiagnostic();
        index = loaded;
        if (annotationForm != null) annotationForm.load(inspectedAddress);
        refreshAnnotations(annotationFilter.getText());
        table.repaint();
        requestRefresh();
    }

    private void showFailure(Throwable failure) {
        if (failure.getCause() instanceof java.util.concurrent.CancellationException) {
            return;
        }
        dialogs.showError(failure.getMessage() == null ? failure.toString() : failure.getMessage(), "Debugger");
    }

    private void showRuntimeFailure(Throwable failure) {
        while ((failure instanceof java.util.concurrent.CompletionException
                || failure instanceof java.util.concurrent.ExecutionException) && failure.getCause() != null)
            failure = failure.getCause();
        if (failure instanceof java.util.concurrent.CancellationException
                || failure.getCause() instanceof java.util.concurrent.CancellationException) return;
        viewError = failure.getMessage() == null ? failure.toString() : failure.getMessage();
        org.slf4j.LoggerFactory.getLogger(DebuggerPanel.class).warn(viewError, failure);
        refreshDiagnostic();
    }

    private void refreshDiagnostic() {
        String text = session.recordingError();
        if (text.isBlank()) text = viewError.isBlank() ? session.status : viewError;
        debuggerDiagnostic.setText(text);
        debuggerDiagnostic.setToolTipText(text.isBlank() ? null : text);
        debuggerDiagnostic.setVisible(!text.isBlank());
    }

    private void setBusy(boolean busy) {
        if (busy) {
            annotationResults.setVisible(false);
            rememberAndDisable(this);
        }
        else {
            enabledControls.forEach(Component::setEnabled);
            enabledControls.clear();
            updateFlowNavigation();
            updateExecutionState();
            boolean paused = true;
            try { session.requirePaused(); } catch (IllegalStateException ignored) { paused = false; }
            if (recordingControl != null) updateRecordingControls(paused);
            if (initializationPending && index == null) {
                initializationPending = false;
                if (!disposed && paused) initializeAnnotations();
            }
            if (paused && index != null && !tasks.isBusy()) {
                if (inspectBreakpointHit()) pendingPcFollow = false;
                else followPendingPc();
            }
            if (paused && !disposed && index != null && !tasks.isBusy() && !running) {
                try { session.applyPaused(session.pausedGeneration(), inspectionReadyListener); }
                catch (IllegalStateException unavailable) { /* A concurrent change discards the notification. */ }
            }
        }
    }

    private void rememberAndDisable(Component component) {
        enabledControls.put(component, component.isEnabled());
        component.setEnabled(false);
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) rememberAndDisable(child);
    }

    private JPanel borderPanel(int gap) {
        JPanel panel = gui.panel("", "", "");
        panel.setLayout(new BorderLayout(gap, gap));
        return panel;
    }

    private JPanel row() {
        return gui.panel("insets 4, fillx, hidemode 3", "[grow]", "");
    }

    private JLabel label(String text, JComponent field) {
        JLabel label = gui.label(text);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(text);
        return label;
    }

    private JToolBar line(JPanel panel, Component... components) {
        JToolBar group = toolBar();
        group.setLayout(new net.miginfocom.swing.MigLayout("insets 0, hidemode 3, gapx 2", "", ""));
        for (Component component : components) group.add(component);
        panel.add(group, "span, growx, wrap");
        return group;
    }

    private JToolBar toolBar() {
        JToolBar toolbar = gui.toolBar();
        toolbar.putClientProperty("FlatLaf.style", "focusableButtons: true");
        return toolbar;
    }

    private JScrollPane scrollTools(JComponent tools) {
        JScrollPane scroll = gui.scrollPane(tools);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.setMinimumSize(new Dimension(0, 0));
        return scroll;
    }

    private JButton button(String icon, String tooltip, Operation operation) {
        JButton button = gui.toolbarButton(event -> guard(operation), icon, tooltip);
        button.setFocusable(true);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private JButton liveButton(String icon, String tooltip, Operation operation) {
        JButton button = gui.toolbarButton(event -> {
            if (disposed) return;
            try { operation.run(); } catch (Exception failure) { showFailure(failure); }
        }, icon, tooltip);
        button.setFocusable(true);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private void submitUpdate(Operation operation) {
        session.update(() -> {
            try { operation.run(); }
            catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            if (failure != null) showRuntimeFailure(failure);
            else viewError = "";
            refreshDiagnostic();
            refreshWatches();
            refreshHistory();
        }));
    }

    private JButton inspectionButton(String icon, String tooltip, Operation operation) {
        Action action = new AbstractAction(tooltip) {
            @Override public void actionPerformed(ActionEvent event) { guard(operation); }
        };
        action.putValue(Action.SHORT_DESCRIPTION, tooltip);
        JButton button = gui.toolbarButton(action);
        button.setFocusable(true);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.putClientProperty("inspectionIcon", icon);
        button.setIcon(loadInspectionIcon(icon, button, false));
        button.setDisabledIcon(loadInspectionIcon(icon, button, true));
        inspectionButtons.add(button);
        return button;
    }

    private static Icon loadInspectionIcon(String name, JComponent component, boolean disabled) {
        ImageIcon image = GUI.loadIcon(name.startsWith("/") ? name
                : "/net/emustudio/application/gui/icons/inspection/" + name + ".png",
                DebuggerPanel.class);
        if (disabled) image = (ImageIcon) UIManager.getLookAndFeel().getDisabledIcon(component, image);
        return new ScaledImageIcon(image);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (inspectionButtons == null) return;
        for (JButton button : inspectionButtons) {
            String icon = (String) button.getClientProperty("inspectionIcon");
            button.setIcon(loadInspectionIcon(icon, button, false));
            button.setDisabledIcon(loadInspectionIcon(icon, button, true));
        }
        if (annotationFilter != null)
            annotationFilter.putClientProperty("JTextField.leadingIcon", loadInspectionIcon("search-annotations", annotationFilter, false));
    }

    private JToggleButton toggleButton(String icon, String tooltip) {
        JToggleButton button = gui.toolbarToggleButton(event -> {}, icon, tooltip);
        button.setFocusable(true);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private void guard(Operation operation) {
        if (disposed || tasks.isBusy() || session.execution().isBusy()) return;
        try {
            session.requirePaused();
            operation.run();
        } catch (Exception e) {
            showFailure(e);
        }
    }

    public void showInstructions() {
        tabs.setSelectedComponent(instructionView);
    }

    public boolean isInstructionView() {
        return tabs.getSelectedComponent() == instructionView;
    }

    public void showSelectedSource() {
        guard(() -> showSource(inspectedAddress));
    }

    private int selectedInstructionAddress() {
        int row = table.getSelectedRow();
        if (!isInstructionView() || row < 0) throw new IllegalStateException("Select an instruction first");
        int address = instructionModel.getLocationAt(table.convertRowIndexToModel(row));
        if (address < 0) throw new IllegalStateException("Select an instruction first");
        return address;
    }

    public void setExecutionRunning(boolean running) {
        this.running = running;
        refreshDiagnostic();
        updateExecutionState();
    }

    private void updateExecutionState() {
        boolean paused = !running && !tasks.isBusy();
        table.setEnabled(paused);
        if (recordingControl != null) updateRecordingControls(paused);
        inspectionButtons.stream()
                .filter(button -> button != goToFlowInstruction && button != editAnnotation && button != deleteAnnotation)
                .forEach(button -> button.setEnabled(paused));
        annotationFilter.setEnabled(paused);
        foldFlow.setEnabled(paused);
        if (!paused) annotationResults.setVisible(false);
        updateAnnotationActions();
        updateFlowNavigation();
        graph.repaint();
        if (annotationForm != null) annotationForm.updateState();
    }

    private void activateView() {
        boolean wasNavigating = navigating;
        navigating = true;
        try {
            if (isInstructionView()) {
                table.showInstructions(instructionRows);
                configureInstructionColumns();
                inspectedAddress = lastInstructionAddress;
                selectInstructionAddress();
                table.getAccessibleContext().setAccessibleName("Instruction listing");
            }
            updateExecutionState();
            revalidate();
            repaint();
        } finally { navigating = wasNavigating; }
        if (!navigating) notifyAddressSelection(false);
    }

    private void configureInstructionColumns() {
        if (table.getModel() != instructionRows) return;
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int width = Math.max(column.getPreferredWidth(),
                    table.getTableHeader().getFontMetrics(table.getTableHeader().getFont())
                            .stringWidth(column.getHeaderValue().toString()) + 16);
            for (int row = 0; row < table.getRowCount(); row++) {
                width = Math.max(width, table.prepareRenderer(table.getCellRenderer(row, col), row, col)
                        .getPreferredSize().width + 8);
            }
            column.setMinWidth(width);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    public boolean isBreakpointDialogSupported() {
        return session.debug != null;
    }

    public void showBreakpointDialog(JFrame parent) {
        if (!isBreakpointDialogSupported()) return;
        BreakpointDialog dialog = new BreakpointDialog(parent, dialogs, gui, inspectedAddress,
                session.cpu.getAddressSpaceSize(), session::isBreakpointSet,
                this::applyBreakpointRequest);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    boolean applyBreakpointRequest(BreakpointDialog.Request request) {
        try {
            if (tasks.isBusy()) throw new IllegalStateException("Wait for memory analysis to finish.");
            if (request.address < 0 || request.address >= session.cpu.getAddressSpaceSize())
                throw new IllegalArgumentException("Address outside CPU");
            session.setBreakpoint(request.address, request.operation == BreakpointDialog.Operation.SET)
                    .whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                        if (disposed) return;
                        if (failure != null) showRuntimeFailure(failure);
                        else {
                            try { breakpointListChanged(); }
                            catch (IOException saving) { showRuntimeFailure(saving); }
                        }
                    }));
            return true;
        } catch (Exception e) {
            showFailure(e);
            return false;
        }
    }

    private boolean inspectBreakpointHit() {
        if (!byteMemory || index == null || tasks.isBusy()) return false;
        DebuggerSession.BreakpointHit hit = session.takeBreakpointHit();
        if (hit == null) return false;
        instructionModel.currentPage();
        lastInstructionAddress = session.execution().cpuContext().getInstructionLocation();
        showInstructions();
        navigate(hit.address);
        return true;
    }

    public void followAddress(int at) {
        if (navigating || tasks.isBusy()) return;
        navigate(at);
    }

    public void navigate(int at) {
        navigate(at, true, NavigationTarget.LISTING);
    }

    private enum NavigationTarget { LISTING, SOURCE_INSTRUCTION, SOURCE_DATA }

    private void navigate(int at, boolean visit, NavigationTarget target) {
        if (tasks.isBusy()) return;
        session.requirePaused();
        if (session.memory == null) return;
        if (at < 0 || at >= session.cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside memory");
        navigating = true;
        try {
            inspectedAddress = at;
            if ((visit || graph.selectedNode >= 0)
                    && graph.representatives.containsKey(at) && graph.selectedNode != at) graph.selectNode(at);
            if (target != NavigationTarget.SOURCE_DATA) {
                if (visit && target == NavigationTarget.LISTING) showInstructions();
                lastInstructionAddress = at;
                inspectedAddress = at;
                instructionModel.showAddress(at);
                selectInstructionAddress();
            }
        } finally {
            navigating = false;
        }
        notifyAddressSelection(visit);
    }

    private void annotationTools(JToolBar tools) {
        annotations.setCellRenderer(new DetailRenderer<>(entry -> {
            String kind = entry.kind.equals("CODE") ? "Code" : "Data";
            return new ListDetail(String.format("%04X–%04X", entry.address, entry.end - 1),
                    entry.label.isBlank() ? "Unnamed annotation" : entry.label,
                    kind, entry.toString());
        }));
        annotations.getAccessibleContext().setAccessibleName("Annotations");
        annotations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        annotationList.add(gui.scrollPane(annotations), "list");
        annotationList.add(annotationEmpty, "empty");
        annotationResults.setLayout(new BorderLayout());
        annotationResults.add(annotationList, BorderLayout.CENTER);
        annotationResults.setPreferredSize(new Dimension(320, 200));
        annotationResults.setFocusable(false);
        annotationFilter.setComponentPopupMenu(annotationResults);
        annotationFilter.setToolTipText("Search annotations");
        annotationFilter.getAccessibleContext().setAccessibleName("Search annotations");
        annotationFilter.putClientProperty("JTextField.placeholderText", "Search annotations");
        annotationFilter.putClientProperty("JTextField.leadingIcon", loadInspectionIcon("search-annotations", annotationFilter, false));
        annotationFilter.putClientProperty("FlatLaf.style", "placeholderForeground: $TextField.foreground");
        annotationForm = new AnnotationForm();
        editAnnotation = inspectionButton("edit-annotation", "Edit selected annotation", () -> {
            UserAnnotations.Entry entry = annotations.getSelectedValue();
            if (entry != null) showAnnotationEditor(entry.address, "Edit annotation", entry);
        });
        editAnnotation.setEnabled(false);
        annotations.addListSelectionListener(e -> updateAnnotationActions());
        annotations.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int row = annotations.locationToIndex(event.getPoint());
                if (SwingUtilities.isLeftMouseButton(event) && row >= 0
                        && annotations.getCellBounds(row, row).contains(event.getPoint()))
                    guard(DebuggerPanel.this::activateAnnotationResult);
            }
        });
        tools.add(annotationFilter, "growx, pushx, wmin 160");
        tools.addSeparator();
        JButton newAnnotation = inspectionButton("add-annotation", "Annotate inspected address", () -> {
            if (!confirmSaveAnnotations()) return;
            showAnnotationEditor(inspectedAddress, "New annotation", null);
        });
        tools.add(newAnnotation);
        tools.add(editAnnotation);
        deleteAnnotation = inspectionButton("remove-annotation", "Delete selected annotation", () -> {
            UserAnnotations.Entry entry = annotations.getSelectedValue();
            if (entry == null || !confirmSaveAnnotations()) return;
            if (!confirm(String.format("Delete annotation at %04X?", entry.address), "Delete annotation")) return;
            changeAnnotations(() -> index.remove(entry));
            annotationForm.load(entry.address);
        });
        deleteAnnotation.setEnabled(false);
        tools.add(deleteAnnotation);
        tools.addSeparator();
        tools.add(inspectionButton("import-annotations", "Import annotations", () -> {
            if (!confirmSaveAnnotations()) return;
            Path path = choose(false, false);
            if (path == null) return;
            int[] skipped = {0};
            changeAnnotations(() -> skipped[0] = index.load(path));
            annotationForm.load(annotationForm.address);
            if (skipped[0] > 0)
                dialogs.showError("Skipped " + skipped[0] + " invalid annotation(s).", "Import annotations");
        }));
        tools.add(inspectionButton("export-annotations", "Export annotations", () -> {
            Path path = choose(true, false);
            if (path != null && (!Files.exists(path) || confirm("Replace existing file?", "Export annotations")))
                index.save(path);
        }));
        onTextChange(() -> {
            if (disposed || running || tasks.isBusy()) return;
            refreshAnnotations(annotationFilter.getText());
            if (annotationFilter.isShowing() && annotationFilter.hasFocus())
                annotationResults.show(annotationFilter, 0, annotationFilter.getHeight());
        }, annotationFilter);
        annotationFilter.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (!disposed && !running && !tasks.isBusy()
                        && SwingUtilities.isLeftMouseButton(event) && annotationFilter.isShowing()) {
                    refreshAnnotations(annotationFilter.getText());
                    annotationResults.show(annotationFilter, 0, annotationFilter.getHeight());
                }
            }
        });
        annotationFilter.addActionListener(event -> guard(this::activateAnnotationResult));
        bindAnnotationSearchKey(annotationFilter, "DOWN", () -> moveAnnotationResult(1));
        bindAnnotationSearchKey(annotationFilter, "UP", () -> moveAnnotationResult(-1));
        bindAnnotationSearchKey(annotationFilter, "ESCAPE", () -> annotationResults.setVisible(false));
        bindAnnotationSearchKey(annotations, "ENTER", this::activateAnnotationResult);
        bindAnnotationSearchKey(annotations, "ESCAPE", () -> {
            annotationResults.setVisible(false);
            annotationFilter.requestFocusInWindow();
        });
    }

    private void bindAnnotationSearchKey(JComponent component, String key, Operation operation) {
        String action = "annotationSearch" + key;
        component.getInputMap().put(KeyStroke.getKeyStroke(key), action);
        component.getActionMap().put(action, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { guard(operation); }
        });
    }

    private void moveAnnotationResult(int direction) {
        if (annotationFilter.isShowing() && !annotationResults.isVisible()) {
            refreshAnnotations(annotationFilter.getText());
            annotationResults.show(annotationFilter, 0, annotationFilter.getHeight());
        }
        if (annotationRows.isEmpty()) return;
        int selected = annotations.getSelectedIndex();
        int next = selected < 0 ? (direction > 0 ? 0 : annotationRows.size() - 1)
                : Math.max(0, Math.min(annotationRows.size() - 1, selected + direction));
        annotations.setSelectedIndex(next);
        annotations.ensureIndexIsVisible(next);
    }

    private void activateAnnotationResult() {
        if (annotations.getSelectedIndex() < 0 && !annotationRows.isEmpty()) annotations.setSelectedIndex(0);
        UserAnnotations.Entry entry = annotations.getSelectedValue();
        if (entry == null) return;
        if (!confirmSaveAnnotations()) {
            selectAnnotationTarget();
            return;
        }
        annotationForm.loadEntry(entry);
        graph.showAnnotation(entry);
        annotationResults.setVisible(false);
        navigate(entry.address, true, NavigationTarget.SOURCE_DATA);
    }

    private void updateAnnotationActions() {
        boolean selected = !running && !tasks.isBusy() && annotations.getSelectedValue() != null;
        if (editAnnotation != null) editAnnotation.setEnabled(selected);
        if (deleteAnnotation != null) deleteAnnotation.setEnabled(selected);
    }

    private void showAnnotationEditor(int address, String title, UserAnnotations.Entry entry) {
        if (entry == null) annotationForm.loadNew(address);
        else annotationForm.loadEntry(entry);
        Window owner = SwingUtilities.getWindowAncestor(this);
        net.emustudio.emulib.ui.components.DialogBase dialog =
                new net.emustudio.emulib.ui.components.DialogBase(owner instanceof Frame ? (Frame) owner : null, title, true) {
                    { setResizable(false); buildContent(); }
                    @Override protected JComponent initializeComponents() { return annotationForm; }
                };
        annotationForm.onSaved = dialog::dispose;
        dialog.getRootPane().setDefaultButton(annotationForm.save);
        SwingUtilities.invokeLater(annotationForm.addressField::requestFocusInWindow);
        dialog.setVisible(true);
        annotationForm.onSaved = () -> {};
        annotationForm.load(address);
    }

    private boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private static void onTextChange(Runnable action, javax.swing.text.JTextComponent... fields) {
        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { action.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { action.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { action.run(); }
        };
        for (javax.swing.text.JTextComponent field : fields) field.getDocument().addDocumentListener(listener);
    }

    private void changeAnnotations(Operation change) throws Exception {
        List<UserAnnotations.Entry> previous = index.entries();
        try {
            change.run();
            session.save();
        } catch (Exception failure) {
            index.replace(previous);
            throw failure;
        }
        refreshAnnotations(annotationFilter.getText());
        table.repaint();
        if (graph.flow != null) analyzeFlow(graph.start, flowFromPc);
        else {
            graph.lanes.clear();
            graph.selectedLane = -1;
            UserAnnotations.Entry selected = annotations.getSelectedValue();
            if (selected != null) graph.showAnnotation(selected);
            graph.updateGraphSize();
            graph.repaint();
        }
    }

    public boolean confirmSaveAnnotations() {
        try {
            return annotationForm == null || annotationForm.confirmReplacement();
        } catch (Exception failure) {
            showFailure(failure);
            return false;
        }
    }

    private void selectAnnotationTarget() {
        annotations.clearSelection();
        annotations.setSelectedIndex(rowOf(annotationRows, annotationForm::matches));
    }

    private final class AnnotationForm extends JPanel {
        private final List<String> kinds = List.of("CODE", "DATA");
        private final JTextField addressField = gui.textField("", 16), labelField = gui.textField("", 12);
        private final JTextField endField = gui.textField("", 16);
        private final JComboBox<String> kind = new JComboBox<>(new String[]{"Code", "Data"});
        private final JButton save;
        private int address;
        private UserAnnotations.Entry original;
        private boolean loading, confirming;
        private Runnable onSaved = () -> {};

        AnnotationForm() {
            setLayout(new net.miginfocom.swing.MigLayout("insets 6, fillx, hidemode 3", "[grow]", ""));
            JPanel fields = gui.panel("insets 0, fillx", "[][grow,fill]", "");
            addressField.getAccessibleContext().setAccessibleName("Address");
            addressField.setToolTipText("Decimal or hexadecimal address, for example 42 or 0x2A");
            fields.add(label("Address", addressField));
            fields.add(addressField, "growx, wrap");
            endField.setToolTipText("Last address included in the annotation");
            fields.add(label("End address", endField));
            fields.add(endField, "growx, wrap");
            fields.add(label("Label", labelField));
            fields.add(labelField, "growx, wrap");
            add(fields, "growx, wrap");
            save = button(ICON_SAVE, "Save annotation", () -> { saveDraft(); onSaved.run(); });
            save.setName("saveAnnotation");
            save.setText("Save");
            JPanel options = gui.panel("insets 0, fillx", "[][][grow][]", "");
            options.add(label("Type", kind));
            options.add(kind);
            options.add(save, "skip 1");
            add(options, "growx");
            onTextChange(this::updateState, labelField, addressField, endField);
            kind.addActionListener(event -> updateState());
            labelField.addActionListener(event -> save.doClick());
            updateState();
        }

        boolean matches(UserAnnotations.Entry entry) {
            return original != null && original.id >= 0 && entry.id == original.id;
        }

        void load(int at) {
            UserAnnotations.Entry entry = index == null ? null : index.get(at);
            if (entry == null) loadNew(at);
            else loadEntry(entry);
        }

        void loadNew(int at) {
            loadEntry(new UserAnnotations.Entry(at, "", "CODE"));
        }

        void loadEntry(UserAnnotations.Entry entry) {
            loading = true;
            try {
                address = entry.address;
                addressField.setText(String.format("0x%04X", address));
                endField.setText(String.format("0x%04X", entry.end - 1));
                original = entry;
                labelField.setText(entry.label);
                kind.setSelectedIndex(kinds.indexOf(entry.kind));
            } finally {
                loading = false;
            }
            updateState();
        }

        boolean dirty() {
            return original != null && (!original.label.equals(labelField.getText())
                    || !original.kind.equals(kinds.get(kind.getSelectedIndex()))
                    || !addressField.getText().equalsIgnoreCase(String.format("0x%04X", original.address))
                    || !endField.getText().equalsIgnoreCase(String.format("0x%04X", original.end - 1)));
        }

        void updateState() {
            if (loading) return;
            boolean dirty = dirty();
            save.setToolTipText("Save annotation");
            save.getAccessibleContext().setAccessibleName("Save annotation");
            save.setEnabled(original != null && (dirty || original.id < 0) && !running && !tasks.isBusy());
        }

        void saveDraft() throws Exception {
            if (original == null || (original.id >= 0 && !dirty())) return;
            int target, end;
            try {
                target = net.emustudio.emulib.Numbers.parseRadix(addressField.getText());
                end = net.emustudio.emulib.Numbers.parseRadix(endField.getText());
                if (target < 0 || end < target || end >= session.addressSpaceSize()) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid address. Enter a value from 0 to "
                        + (session.addressSpaceSize() - 1) + ".");
            }
            address = target;
            UserAnnotations.Entry draft = new UserAnnotations.Entry(original.id, address, end + 1, labelField.getText(), kinds.get(kind.getSelectedIndex()));
            UserAnnotations.Entry[] saved = {draft};
            changeAnnotations(() -> {
                if (draft.id < 0) saved[0] = index.add(draft);
                else index.update(draft);
            });
            loadEntry(saved[0]);
        }

        boolean confirmReplacement() throws Exception {
            if (confirming) return false;
            if (!dirty()) return true;
            confirming = true;
            try {
                int choice = JOptionPane.showOptionDialog(DebuggerPanel.this,
                        String.format("Save changes to annotation at %04X?", address),
                        "Unsaved annotation", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, new Object[]{"Save", "Discard", "Cancel"}, "Save");
                if (choice == 0) saveDraft();
                return choice == 0 || choice == 1;
            } finally {
                confirming = false;
            }
        }
    }

    private JPanel watchPanel() {
        JPanel panel = borderPanel(4);
        watches.setTableHeader(null);
        watches.setFont(new Font(Font.MONOSPACED, Font.PLAIN, watches.getFont().getSize()));
        watches.getAccessibleContext().setAccessibleName("Watch expressions and values");
        watches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watches.putClientProperty("terminateEditOnFocusLost", true);
        watches.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2
                        && watches.columnAtPoint(event.getPoint()) == 0)
                    editWatch(watches.rowAtPoint(event.getPoint()));
            }
        });
        watches.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                     boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                Map.Entry<String, String> entry = watchRows.rows.get(table.convertRowIndexToModel(row));
                setToolTipText(entry.getKey() + " = " + entry.getValue());
                return this;
            }
        });
        watches.getDefaultEditor(String.class).addCellEditorListener(new javax.swing.event.CellEditorListener() {
            @Override public void editingStopped(javax.swing.event.ChangeEvent event) {}
            @Override public void editingCanceled(javax.swing.event.ChangeEvent event) {
                SwingUtilities.invokeLater(DebuggerPanel.this::refreshWatches);
            }
        });
        watches.getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (watches.isEditing()) watches.getCellEditor().cancelCellEditing();
            }
        });
        panel.add(gui.scrollPane(watches));
        JPanel tools = row();
        JTextField expression = gui.textField("", 16);
        expression.setToolTipText("Machine expression, e.g. A or mem[0x1234]; invalid text is saved and shown inactive until corrected.");
        JButton add = liveButton(ICON_ADD, "Add", () -> {
            String text = expression.getText();
            submitUpdate(() -> { session.watches.add(text); session.save(); });
            expression.setText("");
        });
        expression.addActionListener(e -> add.doClick());
        watchFormat.addActionListener(e -> { if (!updatingWatchFormats) refreshWatches(); });
        watchFormat.setToolTipText("Watch value format");
        watchFormat.getAccessibleContext().setAccessibleName("Watch value format");
        JPanel entry = gui.panel("insets 4, fillx", "[][grow][]", "");
        entry.add(label("Expression", expression));
        entry.add(expression, "growx");
        JToolBar entryActions = toolBar();
        entryActions.add(add);
        entry.add(entryActions);
        tools.add(entry, "span, growx, wrap");
        JToolBar watchTools = line(tools, liveButton(ICON_REMOVE, "Remove", () -> {
            String selected = selectedWatch();
            if (selected != null) submitUpdate(() -> { session.watches.remove(selected); session.save(); });
        }), liveButton(ICON_RENAME, "Edit watch", () -> {
            editWatch(watches.getSelectedRow());
        }), liveButton(ICON_REFRESH, "Refresh", () -> submitUpdate(() -> {})));
        watchTools.addSeparator();
        watchTools.add(watchFormat);
        panel.add(scrollTools(tools), BorderLayout.NORTH);
        panel.setPreferredSize(new Dimension(280, 230));
        return panel;
    }

    private void editWatch(int row) {
        if (row >= 0 && watches.editCellAt(row, 0)) {
            JTextField editor = (JTextField) watches.getEditorComponent();
            editor.requestFocusInWindow();
            editor.selectAll();
        }
    }

    private String selectedWatch() {
        int row = watches.getSelectedRow();
        return row < 0 ? null : watchRows.rows.get(watches.convertRowIndexToModel(row)).getKey();
    }

    private final class WatchTableModel extends AbstractTableModel {
        private List<Map.Entry<String, String>> rows = List.of();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int column) { return column == 0 ? "Expression" : "Value"; }
        @Override public Class<?> getColumnClass(int column) { return String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        @Override public Object getValueAt(int row, int column) {
            return column == 0 ? rows.get(row).getKey() : rows.get(row).getValue();
        }
        @Override public void setValueAt(Object value, int row, int column) {
            if (column != 0) return;
            String original = rows.get(row).getKey();
            String expression = value.toString();
            if (original.equals(expression)) return;
            rows = new ArrayList<>(rows);
            rows.set(row, Map.entry(expression, "Pending"));
            fireTableRowsUpdated(row, row);
            submitUpdate(() -> {
                if (row < session.watches.size() && session.watches.get(row).equals(original))
                    session.watches.set(row, expression);
                session.save();
            });
        }
    }

    private static <T> int rowOf(ListModel<T> model, Predicate<T> matches) {
        for (int row = 0; row < model.getSize(); row++)
            if (matches.test(model.getElementAt(row))) return row;
        return -1;
    }

    private void breakpointListChanged() throws IOException {
        breakpointsChanged.run();
        session.save();
        refresh();
    }

    private JPanel flowPanel() {
        JPanel panel = borderPanel(0);
        JPanel tools = new InspectionTools();
        JToolBar actions = line(tools);
        JButton analyzePc = inspectionButton(ICON_CPU, "Show observed flow from PC", this::analyzeFlow);
        actions.add(analyzePc);
        JButton analyzeSelection = inspectionButton(ICON_PAGE_CURRENT, "Show observed flow from selected instruction", () -> analyzeFlow(lastInstructionAddress));
        actions.add(analyzeSelection);
        JButton analyzeRecording = inspectionButton(ICON_SNAPSHOT, "Analyze recording", this::analyzeRecording);
        actions.add(analyzeRecording);
        actions.addSeparator();
        goToFlowInstruction = inspectionButton("go-to-instruction", "Go to instruction", () -> graph.followNode(graph.selectedNode));
        updateFlowNavigation();
        actions.add(goToFlowInstruction);
        actions.add(Box.createHorizontalGlue(), "pushx, growx");
        foldFlow.getAccessibleContext().setAccessibleName("Fold identical analysis repeats");
        foldFlow.setToolTipText("Group identical consecutive instructions; keep entry points, labels and PC separate");
        foldFlow.addActionListener(event -> guard(graph::relayout));
        actions.add(foldFlow);
        JComboBox<String> zoom = new JComboBox<>(new String[]{"50%", "75%", "100%", "125%", "150%", "200%", "250%", "300%"});
        zoom.setSelectedItem("100%");
        zoom.setName("Graph zoom");
        zoom.addActionListener(event -> graph.setZoom(zoom.getSelectedIndex(), null));
        graph.zoomControl = zoom;
        zoom.getAccessibleContext().setAccessibleName("Graph zoom");
        zoom.setToolTipText("Graph zoom (Ctrl+mouse wheel)");
        actions.add(zoom);
        if (byteMemory) {
            flowAnnotations = line(tools);
            annotationTools(flowAnnotations);
        }
        graph.getAccessibleContext().setAccessibleDescription(
                "Select an instruction with the mouse or arrow keys; press Enter or double-click to navigate. Indirect targets may be unresolved.");
        panel.add(scrollTools(tools), BorderLayout.NORTH);
        JScrollPane graphScroll = gui.scrollPane(graph);
        graphScroll.getVerticalScrollBar().setUnitIncrement(30);
        graphScroll.getHorizontalScrollBar().setUnitIncrement(30);
        MouseWheelListener[] wheelListeners = graphScroll.getMouseWheelListeners();
        for (MouseWheelListener listener : wheelListeners) graphScroll.removeMouseWheelListener(listener);
        graphScroll.addMouseWheelListener(event -> {
            if (event.isControlDown() && event.getWheelRotation() != 0) {
                Point anchor = SwingUtilities.convertPoint(graphScroll, event.getPoint(), graphScroll.getViewport());
                graph.setZoom(graph.zoomIndex - event.getWheelRotation(), anchor);
                event.consume();
            } else {
                for (MouseWheelListener listener : wheelListeners) listener.mouseWheelMoved(event);
            }
        });
        inspectionBody.add(graphScroll, "flow");
        inspectionBody.add(historyPanel(), "recording");
        panel.add(inspectionBody, BorderLayout.CENTER);
        return panel;
    }

    private void updateFlowNavigation() {
        if (goToFlowInstruction != null)
            goToFlowInstruction.setEnabled(!running && !tasks.isBusy() && !browsingRecording && graph.boxes.containsKey(graph.selectedNode));
    }

    private void analyzeFlow() {
        analyzeFlow(session.execution().cpuContext().getInstructionLocation(), true);
    }

    private void analyzeFlow(int address) {
        analyzeFlow(address, false);
    }

    public void stepCompleted() {
        if (!disposed) requestRefresh();
    }

    public void setFlowAnalysisLimit(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Flow analysis limit must be positive");
        flowAnalysisLimit = limit;
    }

    private void analyzeFlow(int address, boolean fromPc) {
        analyzeFlow(address, fromPc, address);
    }

    private void analyzeFlow(int address, boolean fromPc, int selection) {
        int limit = flowAnalysisLimit;
        tasks.submit(() -> session.analyzeFlow(address, limit), flow -> {
            viewError = "";
            refreshDiagnostic();
            if (!flow.isCurrent(session))
                throw new IllegalStateException("Flow analysis discarded: program changed");
            browsingRecording = false;
            ((CardLayout) inspectionBody.getLayout()).show(inspectionBody, "flow");
            graph.zoomControl.setVisible(true);
            foldFlow.setVisible(true);
            goToFlowInstruction.setVisible(true);
            if (flowAnnotations != null) flowAnnotations.setVisible(true);
            flowFromPc = fromPc;
            graph.start = address;
            graph.setFlow(flow, selection);
        });
    }

    /** Shrink flexible fields before resorting to horizontal toolbar scrolling. */
    private static final class InspectionTools extends JPanel implements Scrollable {
        InspectionTools() {
            super(new net.miginfocom.swing.MigLayout("insets 4, fillx, hidemode 3", "[grow]", ""));
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() != null && getParent().getWidth() >= getMinimumSize().width;
        }
        @Override public boolean getScrollableTracksViewportHeight() { return true; }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return com.formdev.flatlaf.util.UIScale.scale(16);
        }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(getScrollableUnitIncrement(visible, orientation, direction), visible.width / 2);
        }
    }

    private JPanel historyPanel() {
        JPanel p = borderPanel(0);
        history.getAccessibleContext().setAccessibleName("Recorded instructions");
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        gui.styleTable(history);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, history.getFont().getSize()));
        int[] widths = {170, 120, 250, 300};
        for (int column = 0; column < widths.length; column++)
            history.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        JPanel tools = gui.panel("insets 4, fillx, hidemode 3", "[grow,fill]", "");
        JPanel capture = gui.panel("insets 0, fillx, hidemode 3", "[][grow,fill]", "");
        historyOrigin.getAccessibleContext().setAccessibleName("Recording");
        historyOrigin.addActionListener(event -> {
            if (!disposed && !updatingHistoryOrigin) { history.clearSelection(); refreshHistory(); }
        });
        capture.add(label("Recording", historyOrigin));
        capture.add(historyOrigin);
        recordingControl = toggleButton(ICON_RECORD, "Record instructions");
        recordingControl.addActionListener(event -> {
            if (!disposed && !recordingChangePending) toggleRecentRecording();
            updateRecordingControls(!running && !tasks.isBusy());
        });
        tools.add(capture, "growx, wrap");
        tools.add(foldHistory, "alignx right, wrap");
        foldHistory.addActionListener(event -> { if (!disposed) refreshHistory(); });
        showUnchangedValues.addActionListener(event -> showHistoricalState(history.getSelectedRow()));
        historicalState.setEditable(false);
        historicalState.setFont(new Font(Font.MONOSPACED, Font.PLAIN, historicalState.getFont().getSize()));
        historicalState.getAccessibleContext().setAccessibleName("Recorded instruction state");
        history.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showHistoricalState(history.getSelectedRow());
        });
        p.add(scrollTools(tools), BorderLayout.NORTH);
        JPanel detail = gui.section("Recorded state", "", "", "");
        detail.setLayout(new BorderLayout(4, 4));
        JPanel detailTools = gui.panel("insets 4, fillx", "[grow,fill][]", "");
        detailTools.add(showUnchangedValues, "cell 1 0");
        detail.add(detailTools, BorderLayout.NORTH);
        detail.add(gui.scrollPane(historicalState));
        JButton earlier = historyButton("Follow link", ICON_FIND, this::followRecordedLink);
        earlier.setToolTipText("Follow the selected recorded navigation link");
        JPanel memoryActions = gui.panel("insets 4, fillx", "[][grow,fill][]", "");
        memoryActions.add(label("Navigation", historyNavigation));
        memoryActions.add(historyNavigation);
        memoryActions.add(earlier);
        detail.add(memoryActions, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gui.scrollPane(history), detail);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(.6);
        historyBody.add(split, "results");
        JPanel empty = gui.panel("insets 24, fillx, aligny center, hidemode 3", "[grow,center]", "");
        historyEmptyTitle.setFont(historyEmptyTitle.getFont().deriveFont(Font.BOLD));
        historyEmptyTitle.getAccessibleContext().setAccessibleName("History empty state");
        empty.add(historyEmptyTitle, "wrap");
        empty.add(historyEmptyHint, "wrap");
        historyEmptyRecord = historyButton("Record instructions", ICON_RECORD, this::toggleRecentRecording);
        empty.add(historyEmptyRecord, "wrap");
        historyBody.add(empty, "empty");
        p.add(historyBody);
        updateRecordingControls(true);
        return p;
    }

    private JButton historyButton(String text, String icon, Operation action) {
        JButton button = liveButton(icon, text, action);
        button.setText(text);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        return button;
    }

    private void analyzeRecording() {
        browsingRecording = true;
        flowFromPc = false;
        refreshHistory();
        ((CardLayout) inspectionBody.getLayout()).show(inspectionBody, "recording");
        graph.zoomControl.setVisible(false);
        foldFlow.setVisible(false);
        goToFlowInstruction.setVisible(false);
        if (flowAnnotations != null) flowAnnotations.setVisible(false);
        annotationResults.setVisible(false);
        updateFlowNavigation();
    }

    private void toggleRecentRecording() {
        if (session.isRecording()) {
            DebuggerSession.RecordingMode mode = session.recordingMode();
            String scope;
            if (mode == DebuggerSession.RecordingMode.ADDRESS_RANGE) {
                scope = String.format("From 0x%04X to 0x%04X", session.recordingFrom(), session.recordingThrough());
            } else if (mode == DebuggerSession.RecordingMode.UNLIMITED) {
                scope = "";
            } else {
                String position = mode == DebuggerSession.RecordingMode.FIRST ? "First " : "Last ";
                scope = position + session.recordingLimit() + " instructions";
            }
            recordingChangePending = true;
            session.update(() -> session.record(0)).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                recordingChangePending = false;
                if (disposed) return;
                if (failure != null) showRuntimeFailure(failure);
                else {
                    List<InstructionRecording.Event> captured = session.history();
                    if (!captured.isEmpty()) addRecording(captured, scope);
                }
                refreshHistory();
            }));
        } else {
            RecordingDialog dialog = new RecordingDialog(
                    (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this), gui, dialogs, session,
                    () -> { historyOrigin.setSelectedIndex(0); refreshHistory(); });
            dialog.setVisible(true);
        }
        updateRecordingControls(!running && !tasks.isBusy());
    }

    void addRecording(List<InstructionRecording.Event> captured, String scope) {
        if (captured.isEmpty()) return;
        updatingHistoryOrigin = true;
        try {
            recordings.add(captured);
            historyOrigin.addItem("Recording " + (++recordingsCreated) + (scope.isEmpty() ? "" : " · " + scope));
            historyOrigin.setSelectedIndex(recordings.size());
        } finally { updatingHistoryOrigin = false; }
        refreshHistory();
    }

    /** Completed captures are kept until the panel is disposed. */
    static final class RecordingArchive {
        private final List<List<InstructionRecording.Event>> captures = new ArrayList<>();

        void add(List<InstructionRecording.Event> captured) {
            if (captured.isEmpty()) throw new IllegalArgumentException("No recorded steps");
            captures.add(List.copyOf(captured));
        }

        int size() { return captures.size(); }
        List<InstructionRecording.Event> get(int index) { return captures.get(index); }
    }

    private void updateRecordingControls(boolean paused) {
        if (recordingControl == null) return;
        recordingControl.setSelected(session.isRecording());
        String action = session.isRecording() ? "Stop recording" : "Record instructions";
        recordingControl.setIcon(EmuStudioGui.loadIcon(session.isRecording() ? ICON_STOP : ICON_RECORD, DebuggerPanel.class));
        recordingControl.setToolTipText(action);
        recordingControl.getAccessibleContext().setAccessibleName(action);
        recordingControl.setEnabled(session.debug != null && !tasks.isBusy() && !recordingChangePending);
        historyEmptyRecord.setEnabled(session.debug != null && !tasks.isBusy() && !recordingChangePending);
        historyOrigin.setEnabled(paused);
        history.setEnabled(paused);
    }

    private void followRecordedLink() {
        int selected = historyNavigation.getSelectedIndex();
        if (selected < 0 || selected >= selectedHistoryLinks.size())
            throw new IllegalArgumentException("Select a recorded navigation link first");
        NavLink link = selectedHistoryLinks.get(selected);
        OptionalLong target = InstructionRecording.resolveEvent(displayedHistory,
                selectedHistorySequences.get(selected), link);
        if (target.isPresent()) {
            foldHistory.setSelected(false);
            refreshHistory();
            selectHistorySequence(target.getAsLong());
        } else dialogs.showInfo("No matching event retained in this recording.", "Recorded navigation");
    }

    private void selectHistorySequence(long sequence) {
        for (int row = 0; row < historyGroups.size(); row++)
            if (displayedHistory.get(historyGroups.get(row)[0]).sequence == sequence) {
                history.setRowSelectionInterval(row, row);
                history.scrollRectToVisible(history.getCellRect(row, 0, true));
                break;
            }
    }

    private void refreshHistory() {
        refreshDiagnostic();
        int selected = history.getSelectedRow();
        InstructionRecording.Event selectedEvent = selected >= 0 && selected < historyGroups.size()
                ? displayedHistory.get(historyGroups.get(selected)[0]) : null;
        int source = historyOrigin.getSelectedIndex();
        displayedHistory = source <= 0 ? session.history() : recordings.get(source - 1);
        historyGroups.clear();
        for (int i = 0; i < displayedHistory.size(); i++) {
            int last = i;
            while (foldHistory.isSelected() && last + 1 < displayedHistory.size()
                    && sameRecordedStep(displayedHistory.get(last), displayedHistory.get(last + 1))) last++;
            historyGroups.add(new int[] {i, last});
            i = last;
        }
        historyModel.fireTableDataChanged();
        if (selectedEvent != null)
            for (int row = 0; row < historyGroups.size(); row++) {
                int[] group = historyGroups.get(row);
                if (displayedHistory.subList(group[0], group[1] + 1).contains(selectedEvent)) {
                    history.setRowSelectionInterval(row, row);
                    break;
                }
            }
        if (displayedHistory.isEmpty()) {
            selectedHistoryLinks = List.of();
            historyNavigation.removeAllItems();
            historyEmptyTitle.setText(session.debug == null ? "Recording unavailable for this CPU"
                    : session.isRecording() ? "Ready to capture instructions" : "No instructions recorded");
            historyEmptyHint.setText(session.isRecording()
                    ? "Step or run your program, then pause to inspect its instructions."
                    : "Start recording, then step or run your program.");
            historyEmptyRecord.setVisible(session.debug != null && !session.isRecording());
            historicalState.setText("");
            historicalState.setCaretPosition(0);
        } else if (history.getSelectedRow() < 0) {
            historicalState.setText("Select a step to inspect its captured details.");
            historicalState.setCaretPosition(0);
        }
        ((CardLayout) historyBody.getLayout()).show(historyBody, displayedHistory.isEmpty() ? "empty" : "results");
        updateRecordingControls(!running && !tasks.isBusy());
    }

    private static boolean sameRecordedStep(InstructionRecording.Event first, InstructionRecording.Event second) {
        return second.sequence == first.sequence + 1 && first.completed && second.completed
                && !first.view.foldKey.isBlank() && first.view.foldKey.equals(second.view.foldKey);
    }

    private List<ViewRow> combinedRows(int[] group) {
        if (group[0] == group[1]) return displayedHistory.get(group[0]).view.rows;
        List<ViewRow> rows = new ArrayList<>();
        Map<List<String>, Integer> stateRows = new LinkedHashMap<>();
        for (int i = group[0]; i <= group[1]; i++) {
            for (ViewRow row : displayedHistory.get(i).view.rows) {
                if (!row.tags.isEmpty() || !row.links.isEmpty()) {
                    rows.add(row);
                    continue;
                }
                List<String> key = List.of(row.group, row.label);
                Integer previousIndex = stateRows.get(key);
                if (previousIndex == null) {
                    stateRows.put(key, rows.size());
                    rows.add(row);
                } else {
                    ViewRow previous = rows.get(previousIndex);
                    rows.set(previousIndex, new ViewRow(row.group, row.label,
                            previous.before, row.after, previous.changed || row.changed, List.of(), List.of()));
                }
            }
        }
        return rows;
    }

    private String recordedAddress(InstructionRecording.Event first, InstructionRecording.Event last) {
        if (first.view.programLocation.isEmpty()) return "";
        int from = first.view.programLocation.getAsInt();
        return last.view.programLocation.isPresent() && from != last.view.programLocation.getAsInt()
                ? String.format("%04X → %04X", from, last.view.programLocation.getAsInt())
                : String.format("%04X", from);
    }

    private final class HistoryModel extends AbstractTableModel {
        private final String[] columns = {"Sequence", "Location", "Step", "Changes"};
        @Override public int getRowCount() { return historyGroups.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            int[] group = historyGroups.get(row);
            InstructionRecording.Event event = displayedHistory.get(group[0]), last = displayedHistory.get(group[1]);
            switch (column) {
                case 0: return group[0] == group[1] ? Long.toString(event.sequence)
                        : event.sequence + "–" + last.sequence + " (" + (group[1] - group[0] + 1) + ")";
                case 1: return recordedAddress(event, last);
                case 2: return event.view.summary + (event.completed ? "" : " (failed)")
                        + (group[0] == group[1] ? "" : " × " + (group[1] - group[0] + 1));
                default: {
                    List<String> values = new ArrayList<>();
                    for (ViewRow value : combinedRows(group))
                        if (value.changed) values.add(value.label + "=" + value.before + "→" + value.after);
                    return String.join(" ", values);
                }
            }
        }
    }

    private void showHistoricalState(int selected) {
        selectedHistoryLinks = new ArrayList<>();
        selectedHistorySequences = new ArrayList<>();
        historyNavigation.removeAllItems();
        historyNavigation.setEnabled(false);
        if (selected < 0 || selected >= historyGroups.size()) return;
        int[] group = historyGroups.get(selected);
        InstructionRecording.Event event = displayedHistory.get(group[0]), last = displayedHistory.get(group[1]);
        boolean folded = group[0] != group[1];
        StringBuilder text = new StringBuilder("Sequence " + event.sequence
                + (folded ? "–" + last.sequence : "") + " · " + recordedAddress(event, last) + " · "
                + event.view.summary + (event.completed ? "" : " (failed)") + "\n");
        if (folded) text.append(group[1] - group[0] + 1)
                .append(" matching steps; unfold to inspect each event.\n");
        String section = null;
        for (ViewRow row : combinedRows(group)) {
            if (folded && (!row.tags.isEmpty() || !row.links.isEmpty())) continue;
            if (!row.changed && row.links.isEmpty() && row.tags.isEmpty() && !showUnchangedValues.isSelected()) continue;
            if (!row.group.equals(section)) {
                section = row.group;
                text.append("\n").append(section).append("\n");
            }
            text.append(row.label).append("    ").append(row.before);
            if (row.changed || !row.before.equals(row.after)) text.append(" → ").append(row.after);
            if (folded && row.changed && row.before.equals(row.after)) text.append(" (changed within run)");
            text.append("\n");
        }
        // Keep every row's links tied to its own original ordinal, including within folded groups.
        for (int i = group[0]; i <= group[1]; i++) {
            InstructionRecording.Event instruction = displayedHistory.get(i);
            for (ViewRow row : instruction.view.rows) {
                if (folded && (!row.tags.isEmpty() || !row.links.isEmpty())) {
                    if (!row.group.equals(section)) {
                        section = row.group;
                        text.append("\n").append(section).append("\n");
                    }
                    text.append("#").append(instruction.sequence).append(" · ")
                            .append(row.label).append(" = ");
                    if (!row.before.isEmpty() && !row.before.equals(row.after)) text.append(row.before).append(" → ");
                    text.append(row.after).append("\n");
                }
                for (NavLink link : row.links) {
                    selectedHistoryLinks.add(link);
                    selectedHistorySequences.add(instruction.sequence);
                    historyNavigation.addItem((folded ? "#" + instruction.sequence + " · " : "")
                            + row.label + " · " + link.label);
                }
            }
        }
        historyNavigation.setEnabled(!selectedHistoryLinks.isEmpty());
        historicalState.setText(text.toString());
        historicalState.setCaretPosition(0);
    }

    private void showSource(int at) {
        session.sourceAt(at)
                .ifPresentOrElse(sourceNavigator, () -> {
                    throw new IllegalStateException("No current source mapping at this address");
                });
    }

    private void refreshAnnotations(String filter) {
        UserAnnotations.Entry selected = annotations.getSelectedValue();
        List<UserAnnotations.Entry> entries = index == null ? List.of() : index.search(filter);
        updateRows(annotationRows, entries);
        annotations.clearSelection();
        if (selected != null) annotations.setSelectedIndex(rowOf(annotationRows, entry -> entry.id == selected.id));
        updateAnnotationActions();
        annotationEmpty.setText(index == null || index.entries().isEmpty()
                ? "<html>No annotations yet.<br>Select an address and create an annotation.</html>"
                : "<html>No matching annotations.<br>Change Search annotations.</html>");
        ((CardLayout) annotationList.getLayout()).show(annotationList, entries.isEmpty() ? "empty" : "list");
    }

    private List<UserAnnotations.Entry> annotationsCovering(int address) {
        if (index == null) return List.of();
        int size = session.addressSpaceSize();
        if (address < 0 || address >= size)
            return List.of();
        List<UserAnnotations.Entry> result = new ArrayList<>();
        for (UserAnnotations.Entry entry : index.entries())
            if (entry.address <= address && address < entry.end) result.add(entry);
        return result;
    }

    private String annotationTooltip(int address) {
        List<String> descriptions = new ArrayList<>();
        for (UserAnnotations.Entry entry : annotationsCovering(address)) {
            String description = entry.label.trim();
            descriptions.add(description.isEmpty() ? "Annotation" : description);
        }
        return descriptions.isEmpty() ? null : String.join(" | ", descriptions);
    }

    /** Derived from the annotated range only, so an annotation keeps its color across sessions. */
    private static Color laneFill(UserAnnotations.Entry annotation) {
        int hue = Math.floorMod((annotation.address * 31 + annotation.end) * 0x9E3779B9, 360);
        return Color.getHSBColor(hue / 360f, .55f, .78f);
    }

    private Color annotationBackground(int address) {
        List<UserAnnotations.Entry> entries = annotationsCovering(address);
        if (entries.isEmpty()) return null;
        Color background = graph.getBackground();
        if (background == null) background = table.getBackground();
        for (UserAnnotations.Entry entry : entries) {
            Color lane = laneFill(entry);
            background = new Color((lane.getRed() * 28 + background.getRed() * 227 + 127) / 255,
                    (lane.getGreen() * 28 + background.getGreen() * 227 + 127) / 255,
                    (lane.getBlue() * 28 + background.getBlue() * 227 + 127) / 255);
        }
        return background;
    }

    private String sourceLabelsAt(int address) {
        if (!sourceMappingsCurrent || address < 0) return "";
        return session.annotationEngine.at(address).stream()
                .filter(entry -> entry.start == address)
                .filter(entry -> entry.kind.equals("label"))
                .map(entry -> entry.text.trim()).filter(label -> !label.isEmpty()).distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static <T> void updateRows(DefaultListModel<T> model, List<T> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i >= model.size()) model.addElement(values.get(i));
            else if (!Objects.equals(model.get(i), values.get(i))) model.set(i, values.get(i));
        }
        while (model.size() > values.size()) model.remove(model.size() - 1);
    }

    private static final class ListDetail {
        final String location, title, detail, tooltip;

        ListDetail(String location, String title, String detail, String tooltip) {
            this.location = location;
            this.title = title;
            this.detail = detail;
            this.tooltip = tooltip;
        }
    }

    /** Keep list navigation while giving addresses and descriptions stable, scannable columns. */
    private static final class DetailRenderer<T> extends JPanel implements ListCellRenderer<T> {
        private final DefaultListCellRenderer standard = new DefaultListCellRenderer();
        private final JLabel location = new JLabel(), title = new JLabel(), detail = new JLabel();
        private final JPanel text = new JPanel(new BorderLayout(0, 2));
        private final Function<T, ListDetail> describe;

        DetailRenderer(Function<T, ListDetail> describe) {
            super(new BorderLayout(10, 0));
            this.describe = describe;
            location.setPreferredSize(new Dimension(122, 20));
            location.setVerticalAlignment(SwingConstants.TOP);
            for (JLabel label : List.of(location, title, detail)) label.putClientProperty("html.disable", true);
            text.setOpaque(false);
            text.add(title, BorderLayout.NORTH);
            text.add(detail, BorderLayout.SOUTH);
            add(location, BorderLayout.WEST);
            add(text);
        }

        @Override public Component getListCellRendererComponent(JList<? extends T> list, T value,
                int index, boolean selected, boolean focus) {
            standard.getListCellRendererComponent(list, value, index, selected, focus);
            ListDetail row = describe.apply(value);
            setBackground(standard.getBackground());
            setEnabled(list.isEnabled());
            setBorder(BorderFactory.createCompoundBorder(standard.getBorder(), BorderFactory.createEmptyBorder(3, 4, 3, 4)));
            for (JLabel label : List.of(location, title, detail)) {
                label.setForeground(standard.getForeground());
                label.setFont(list.getFont());
                label.setEnabled(list.isEnabled());
            }
            location.setFont(new Font(Font.MONOSPACED, Font.PLAIN, list.getFont().getSize()));
            location.setText(row.location);
            title.setText(row.title);
            detail.setText(row.detail);
            detail.setVisible(!row.detail.isBlank());
            setToolTipText(row.tooltip);
            getAccessibleContext().setAccessibleName(row.location + ", " + row.title + ", " + row.detail);
            return this;
        }
    }

    private void requestRefresh() {
        if (!refreshQueued.compareAndSet(false, true)) return;
        SwingUtilities.invokeLater(() -> {
            refreshQueued.set(false);
            if (disposed) return;
            if (index == null && tasks.isBusy()) initializationPending = true;
            refresh();
        });
    }

    private void refresh() {
        if (disposed) return;
        refreshDiagnostic();
        refreshWatches();
        if (running || session.execution().isBusy() || tasks.isBusy()) return;
        try { session.requirePaused(); } catch (IllegalStateException e) { return; }
        if (index != null && index != session.annotations()) {
            index = null;
            initializeAnnotations();
            return;
        }
        updateRecordingControls(true);
        try { refreshHistory(); } catch (IllegalArgumentException failure) {
            showFailure(failure);
        }
        table.repaint();
        refreshAnnotations(annotationFilter.getText());
        if (graph.flow != null && !graph.flow.isCurrent(session))
            guard(() -> analyzeFlow(graph.start, flowFromPc, graph.selectedNode));
        else if (graph.flow != null && foldFlow.isSelected()
                && graph.layoutPc != session.execution().cpuContext().getInstructionLocation()) guard(graph::relayout);
    }

    private void refreshWatches() {
        if (watches.isEditing()) return;
        String selectedExpression = selectedWatch();
        Map<String, ValueView> values = session.watchValues();
        Map<String, String> errors = session.watchErrors();
        String selectedFormat = (String) watchFormat.getSelectedItem();
        Set<String> formats = new LinkedHashSet<>();
        formats.add("Default");
        values.values().forEach(value -> value.formats.forEach(format -> formats.add(format.label)));
        List<String> displayedFormats = new ArrayList<>();
        for (int i = 0; i < watchFormat.getItemCount(); i++) displayedFormats.add(watchFormat.getItemAt(i));
        if (!displayedFormats.equals(new ArrayList<>(formats))) {
            updatingWatchFormats = true;
            try {
                watchFormat.removeAllItems();
                formats.forEach(watchFormat::addItem);
                if (formats.contains(selectedFormat)) watchFormat.setSelectedItem(selectedFormat);
            } finally { updatingWatchFormats = false; }
        }
        List<Map.Entry<String, String>> watchText = new ArrayList<>();
        for (String expression : session.watches) {
            ValueView view = values.get(expression);
            if (errors.containsKey(expression)) watchText.add(Map.entry(expression, "Inactive: " + errors.get(expression)));
            else if (view == null) watchText.add(Map.entry(expression, "Pending"));
            else {
                String formatted = view.text;
                for (ValueView.Format format : view.formats)
                    if (format.label.equals(selectedFormat)) formatted = format.text;
                watchText.add(Map.entry(expression, formatted + (view.changed ? "  * changed" : "")));
            }
        }
        if (watchRows.rows.equals(watchText)) return;
        watchRows.rows = watchText;
        watchRows.fireTableDataChanged();
        if (selectedExpression != null)
            for (int row = 0; row < watchText.size(); row++)
                if (watchText.get(row).getKey().equals(selectedExpression)) {
                    watches.setRowSelectionInterval(row, row);
                    break;
                }
    }

    private Path choose(boolean save, boolean directory) {
        JFileChooser chooser = new JFileChooser();
        if (directory) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    @FunctionalInterface
    private interface Operation {
        void run() throws Exception;
    }

    private final class InstructionAnnotationsModel extends AbstractTableModel implements TableModelListener {
        private int baseColumn(int col) { return col - 1; }
        @Override public int getRowCount() { return instructionModel.getRowCount(); }
        @Override public int getColumnCount() { return instructionModel.getColumnCount() + 1; }
        @Override public String getColumnName(int col) {
            if (col == 0) return "label";
            return instructionModel.getColumnName(baseColumn(col));
        }
        @Override public Class<?> getColumnClass(int col) {
            if (col == 0) return String.class;
            return instructionModel.getColumnClass(baseColumn(col));
        }
        @Override public Object getValueAt(int row, int col) {
            if (disposed || tasks.isBusy() || running) return null;
            if (col == 0) return sourceLabelsAt(instructionModel.getLocationAt(row));
            return instructionModel.getValueAt(row, baseColumn(col));
        }
        @Override public boolean isCellEditable(int row, int col) {
            if (col == 0 || disposed || tasks.isBusy() || running) return false;
            return instructionModel.isCellEditable(row, baseColumn(col));
        }
        @Override public void setValueAt(Object value, int row, int col) {
            if (col == 0) return;
            instructionModel.setValueAt(value, row, baseColumn(col));
        }
        @Override public void tableChanged(TableModelEvent event) {
            if (event.getFirstRow() == TableModelEvent.HEADER_ROW) {
                fireTableStructureChanged();
                SwingUtilities.invokeLater(() -> { if (!disposed) configureInstructionColumns(); });
            }
            else {
                fireTableDataChanged();
                SwingUtilities.invokeLater(() -> { if (!disposed) configureInstructionColumns(); });
            }
            selectInstructionAddress();
        }
    }

    private final class GraphPanel extends JPanel {
        FlowAnalysis flow;
        private int layoutPc = -1;
        int start;
        int selectedNode = -1;
        final Map<Integer, Rectangle> boxes = new LinkedHashMap<>();
        final Map<Integer, Integer> representatives = new HashMap<>();
        final Map<Integer, List<Integer>> nodeMembers = new HashMap<>();
        final Map<Integer, Integer> instructionEnds = new HashMap<>();
        private Map<Integer, String> nodeLabelCache;
        private DebuggerSession labelSession;
        private long labelContentRevision, labelMemoryRevision;
        private List<FlowAnalysis.Edge> selectedEdges = List.of();
        private final List<AnnotationLane> lanes = new ArrayList<>();
        private int selectedLane = -1;
        private final int[] zoomLevels = {50, 75, 100, 125, 150, 200, 250, 300};
        private int zoomIndex = 2;
        private JComboBox<String> zoomControl;
        private Point dragStart, dragView;
        private Point selectionStart;
        private Rectangle selectionArea, candidateLane;
        private boolean draggedSelection;

        private final class AnnotationLane {
            final UserAnnotations.Entry annotation;
            final Rectangle bounds;
            final Rectangle header;

            AnnotationLane(UserAnnotations.Entry annotation, Rectangle bounds, Rectangle header) {
                this.annotation = annotation;
                this.bounds = bounds;
                this.header = header;
            }
        }

        GraphPanel() {
            super(new BorderLayout());
            setFocusable(true);
            setToolTipText("");
            getAccessibleContext().setAccessibleName("Instruction flow graph");
            for (int code : new int[]{KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN}) {
                int delta = code == KeyEvent.VK_LEFT || code == KeyEvent.VK_UP ? -1 : 1;
                String action = "flow" + code;
                getInputMap().put(KeyStroke.getKeyStroke(code, 0), action);
                getActionMap().put(action, new AbstractAction() {
                    public void actionPerformed(ActionEvent event) {
                        List<Integer> addresses = new ArrayList<>(boxes.keySet());
                        if (addresses.isEmpty()) return;
                        int current = addresses.indexOf(selectedNode);
                        selectInstruction(addresses.get(current < 0 ? 0 : Math.floorMod(current + delta, addresses.size())), true);
                    }
                });
            }
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "followFlow");
            getActionMap().put("followFlow", new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    if (boxes.containsKey(selectedNode)) followNode(selectedNode);
                }
            });
            for (int direction : new int[]{-1, 1}) {
                String action = direction < 0 ? "previousAnnotationLane" : "nextAnnotationLane";
                getInputMap().put(KeyStroke.getKeyStroke(direction < 0 ? KeyEvent.VK_UP : KeyEvent.VK_DOWN,
                        InputEvent.ALT_DOWN_MASK), action);
                getActionMap().put(action, new AbstractAction() {
                    public void actionPerformed(ActionEvent event) {
                        if (!lanes.isEmpty()) selectLane(selectedLane < 0 ? 0 : Math.floorMod(selectedLane + direction, lanes.size()));
                    }
                });
            }
            MouseAdapter mouse = new MouseAdapter() {
                        @Override public void mousePressed(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e) && current()) {
                                selectionStart = e.getPoint();
                                invalidateLabels();
                                draggedSelection = false;
                                return;
                            }
                            if (!SwingUtilities.isRightMouseButton(e)) return;
                            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, GraphPanel.this);
                            if (viewport != null) {
                                dragStart = SwingUtilities.convertPoint(GraphPanel.this, e.getPoint(), viewport);
                                dragView = viewport.getViewPosition();
                            }
                        }
                        @Override public void mouseReleased(MouseEvent e) {
                            dragStart = dragView = null;
                            if (!SwingUtilities.isLeftMouseButton(e) || selectionStart == null) return;
                            selectionStart = null;
                            if (!draggedSelection) return;
                            List<Integer> selected = new ArrayList<>();
                            boxes.forEach((address, box) -> { if (selectionArea != null && selectionArea.intersects(box)) selected.add(address); });
                            selectionArea = null;
                            if (!selected.isEmpty()) {
                                int first = Collections.min(selected);
                                int end = selected.stream().mapToInt(instructionEnds::get).max().orElse(first + 1);
                                guard(() -> {
                                    requireCurrent();
                                    if (confirmSaveAnnotations())
                                        showAnnotationEditor(first, "New annotation",
                                                new UserAnnotations.Entry(-1, first, end, "", "CODE"));
                                });
                            }
                            candidateLane = null;
                            repaint();
                        }
                        @Override public void mouseDragged(MouseEvent e) {
                            if (selectionStart != null && (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                                if (!draggedSelection && selectionStart.distance(e.getPoint()) < 4) return;
                                draggedSelection = true;
                                Point a = modelPoint(selectionStart), b = modelPoint(e.getPoint());
                                selectionArea = new Rectangle(Math.min(a.x, b.x), Math.min(a.y, b.y),
                                        Math.max(1, Math.abs(a.x - b.x)), Math.max(1, Math.abs(a.y - b.y)));
                                candidateLane = null;
                                for (Rectangle box : boxes.values()) if (selectionArea.intersects(box))
                                    candidateLane = candidateLane == null ? new Rectangle(box) : candidateLane.union(box);
                                if (candidateLane != null) candidateLane.grow(8, 8);
                                repaint();
                                return;
                            }
                            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, GraphPanel.this);
                            if (viewport == null || dragStart == null) return;
                            Point point = SwingUtilities.convertPoint(GraphPanel.this, e.getPoint(), viewport);
                            setViewPosition(viewport, dragView.x + dragStart.x - point.x, dragView.y + dragStart.y - point.y);
                        }
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (!SwingUtilities.isLeftMouseButton(e)) return;
                            if (draggedSelection) { draggedSelection = false; return; }
                            Point point = modelPoint(e.getPoint());
                            for (Map.Entry<Integer, Rectangle> entry : boxes.entrySet()) {
                                if (entry.getValue().contains(point)) {
                                    requestFocusInWindow();
                                    selectInstruction(entry.getKey(), false);
                                    if (e.getClickCount() == 2) followNode(entry.getKey());
                                    return;
                                }
                            }
                            for (int i = 0; i < lanes.size(); i++) {
                                Rectangle bounds = lanes.get(i).bounds;
                                Rectangle inner = new Rectangle(bounds);
                                inner.grow(-5, -5);
                                if (lanes.get(i).header.contains(point)
                                        || (bounds.contains(point) && !inner.contains(point))) {
                                    requestFocusInWindow();
                                    selectLane(i);
                                    if (e.getClickCount() == 2) editLane(i);
                                    return;
                                }
                            }
                            if (e.getClickCount() == 2) {
                                for (int i = lanes.size() - 1; i >= 0; i--) {
                                    if (lanes.get(i).bounds.contains(point)) {
                                        selectLane(i);
                                        editLane(i);
                                        return;
                                    }
                                }
                            }
                            selectedLane = -1;
                            annotations.clearSelection();
                            selectNode(-1);
                        }
                    };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private Point modelPoint(Point point) {
            double scale = zoomLevels[zoomIndex] / 100.0;
            return new Point((int) (point.x / scale), (int) (point.y / scale));
        }

        private Rectangle viewRect(Rectangle rect) {
            double scale = zoomLevels[zoomIndex] / 100.0;
            return new Rectangle((int) (rect.x * scale), (int) (rect.y * scale),
                    (int) Math.ceil(rect.width * scale), (int) Math.ceil(rect.height * scale));
        }

        private void setViewPosition(JViewport viewport, int x, int y) {
            Dimension view = viewport.getViewSize(), extent = viewport.getExtentSize();
            viewport.setViewPosition(new Point(Math.max(0, Math.min(x, Math.max(0, view.width - extent.width))),
                    Math.max(0, Math.min(y, Math.max(0, view.height - extent.height)))));
        }

        private void setZoom(int index, Point anchor) {
            index = Math.max(0, Math.min(index, zoomLevels.length - 1));
            if (index == zoomIndex) return;
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
            if (viewport != null && anchor == null)
                anchor = new Point(viewport.getWidth() / 2, viewport.getHeight() / 2);
            Point previous = viewport == null ? null : viewport.getViewPosition();
            double ratio = (double) zoomLevels[index] / zoomLevels[zoomIndex];
            zoomIndex = index;
            if (zoomControl != null && zoomControl.getSelectedIndex() != index) zoomControl.setSelectedIndex(index);
            updateGraphSize();
            if (viewport != null) {
                viewport.setViewSize(getPreferredSize());
                setViewPosition(viewport, (int) Math.round((previous.x + anchor.x) * ratio - anchor.x),
                        (int) Math.round((previous.y + anchor.y) * ratio - anchor.y));
            }
            repaint();
        }

        private void selectLane(int lane) {
            selectedLane = lane;
            AnnotationLane selected = lanes.get(lane);
            int row = rowOf(annotationRows, entry -> entry.id == selected.annotation.id);
            if (row < 0 && !annotationFilter.getText().isEmpty()) {
                annotationFilter.setText("");
                row = rowOf(annotationRows, entry -> entry.id == selected.annotation.id);
            }
            if (row >= 0) {
                annotations.setSelectedIndex(row);
                annotationForm.loadEntry(selected.annotation);
            }
            getAccessibleContext().setAccessibleDescription("Selected annotation: " + laneTitle(selected.annotation)
                    + ". Alt Up and Alt Down select annotation lanes.");
            scrollRectToVisible(viewRect(selected.header));
            repaint();
        }

        private void editLane(int lane) {
            UserAnnotations.Entry annotation = lanes.get(lane).annotation;
            guard(() -> {
                if (confirmSaveAnnotations())
                    showAnnotationEditor(annotation.address, "Edit annotation", annotation);
            });
        }

        private void showAnnotation(UserAnnotations.Entry annotation) {
            for (int i = 0; i < lanes.size(); i++) {
                if (lanes.get(i).annotation.id == annotation.id) {
                    selectLane(i);
                    return;
                }
            }
            int top = boxes.values().stream().mapToInt(box -> box.y + box.height).max().orElse(10) + 25;
            for (AnnotationLane lane : lanes) top = Math.max(top, lane.bounds.y + lane.bounds.height + 12);
            Rectangle bounds = new Rectangle(5, top, 220, 45);
            lanes.add(new AnnotationLane(annotation, bounds, new Rectangle(8, top + 3, 100, 24)));
            updateGraphSize();
            selectLane(lanes.size() - 1);
        }

        private String laneTitle(UserAnnotations.Entry annotation) {
            return annotation.label.isBlank() ? String.format("%04X", annotation.address) : annotation.label;
        }

        private void selectNode(int address) {
            selectNode(address, true);
        }

        private void selectInstruction(int address, boolean scroll) {
            if (!current()) return;
            selectNode(address, scroll);
            guard(() -> navigate(address, false, NavigationTarget.LISTING));
        }

        private void selectNode(int address, boolean scroll) {
            if (!boxes.containsKey(address) && representatives.containsKey(address) && current())
                setFlow(flow, address);
            selectedNode = address;
            if (boxes.containsKey(address))
                getAccessibleContext().setAccessibleDescription(nodeDescription(address)
                        + ". Turn off Fold identical repeats to inspect individual instructions.");
            updateFlowNavigation();
            Rectangle selected = boxes.get(address);
            updateSelectedEdges();
            updateGraphSize();
            repaint();
            if (scroll && selected != null) scrollRectToVisible(viewRect(selected));
        }

        private void followNode(int address) {
            guard(() -> {
                requireCurrent();
                navigate(address, true, NavigationTarget.LISTING);
            });
        }

        boolean current() { return flow != null && flow.isCurrent(session); }

        void requireCurrent() {
            if (!current()) throw new IllegalStateException("Flow analysis is stale. Analyze again.");
        }

        @Override public String getToolTipText(MouseEvent event) {
            if (flow != null && !current()) return "Flow analysis is stale. Analyze again.";
            if (selectionStart == null && candidateLane == null) invalidateLabels();
            Point point = modelPoint(event.getPoint());
            for (Map.Entry<Integer, Rectangle> entry : boxes.entrySet())
                if (entry.getValue().contains(point))
                    return nodeDescription(entry.getKey());
            for (AnnotationLane lane : lanes)
                if (lane.header.contains(point))
                    return String.format("%s · %04X–%04X", laneTitle(lane.annotation),
                            lane.annotation.address, lane.annotation.end - 1);
            return null;
        }

        void setFlow(FlowAnalysis flow) {
            setFlow(flow, start);
        }

        private void relayout() {
            if (current()) setFlow(flow, selectedNode);
        }

        private String nodeAddress(int address) {
            return nodeMembers.get(address).size() > 1
                    ? String.format("%04X–%04X", address, instructionEnds.get(address) - 1)
                    : String.format("%04X", address);
        }

        private String nodeMnemonic(int address) {
            int count = nodeMembers.get(address).size();
            return flow.mnemonic(address) + (count > 1 ? " × " + count : "");
        }

        private String nodeDescription(int address) {
            String labels = nodeLabels(address);
            String unknown = flow.unknown().get(address);
            return (labels.isEmpty() ? "" : labels + " · ") + nodeAddress(address) + " · " + nodeMnemonic(address)
                    + (flow.isData(address) ? " · Data region" : "")
                    + (unknown == null ? "" : " · " + unknown);
        }

        private void setFlow(FlowAnalysis flow, int selection) {
            this.flow = flow;
            invalidateLabels();
            boxes.clear();
            representatives.clear();
            nodeMembers.clear();
            instructionEnds.clear();
            lanes.clear();
            selectedLane = -1;
            selectedNode = selection;
            Map<Integer, FlowGraph.Instruction> instructions = new HashMap<>();
            flow.forEachInstruction(instruction -> instructions.put(instruction.cells().start, instruction));
            Map<Integer, List<Integer>> successors = new HashMap<>();
            flow.forEachEdge((from, to) ->
                    successors.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to));
            Deque<Integer> pending = new ArrayDeque<>();
            Set<Integer> visited = new HashSet<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                int at = pending.removeFirst();
                if (!visited.add(at)) continue;
                FlowGraph.Instruction instruction = instructions.get(at);
                if (instruction == null) {
                    if (!flow.unknown().containsKey(at)) visited.remove(at);
                    continue;
                }
                pending.addAll(successors.getOrDefault(at, List.of()));
            }
            layoutPc = session.execution().cpuContext().getInstructionLocation();
            Set<Integer> boundaries = new HashSet<>(List.of(start, layoutPc, selection));
            refreshNodeLabels();
            boundaries.addAll(nodeLabelCache.keySet());
            if (index != null) for (UserAnnotations.Entry annotation : index.entries()) {
                instructions.forEach((at, instruction) -> {
                    if ((at <= annotation.address && annotation.address < instruction.cells().end)
                            || (at < annotation.end && annotation.end <= instruction.cells().end))
                        boundaries.add(at);
                });
            }
            List<List<Integer>> runs = foldFlow.isSelected() ? flow.instructionRuns(visited, boundaries)
                    : new TreeSet<>(visited).stream().map(List::of).collect(java.util.stream.Collectors.toList());
            for (List<Integer> run : runs) {
                int at = run.get(0);
                FlowGraph.Instruction instruction = instructions.get(at);
                int i = boxes.size();
                boxes.put(at, new Rectangle(30, 25 + i * 90, 210, 60));
                nodeMembers.put(at, run);
                run.forEach(member -> representatives.put(member, at));
                FlowGraph.Instruction last = instructions.get(run.get(run.size() - 1));
                instructionEnds.put(at, last == null ? at + 1 : last.cells().end);
            }
            if (index != null) {
                List<UserAnnotations.Entry> visible = new ArrayList<>();
                for (UserAnnotations.Entry annotation : index.entries()) {
                    if (boxes.keySet().stream().anyMatch(at -> coversInstruction(annotation, at))) visible.add(annotation);
                }
                int nodeX = 30 + visible.size() * 110;
                boxes.values().forEach(box -> box.x = nodeX);
                for (int i = 0; i < visible.size(); i++) {
                    UserAnnotations.Entry annotation = visible.get(i);
                    List<Rectangle> covered = new ArrayList<>();
                    boxes.forEach((at, box) -> {
                        if (coversInstruction(annotation, at)) covered.add(box);
                    });
                    int top = covered.get(0).y - 8, bottom = covered.get(covered.size() - 1).y + 68;
                    int x = 5 + i * 110;
                    lanes.add(new AnnotationLane(annotation, new Rectangle(x, top, nodeX + 220 + i * 4 - x, bottom - top),
                            new Rectangle(x + 3, top + 3, 100, 24)));
                }
            }
            if (selectedNode >= 0 && !boxes.containsKey(selectedNode) && !boxes.isEmpty())
                selectedNode = boxes.keySet().iterator().next();
            UserAnnotations.Entry selectedAnnotation = annotations.getSelectedValue();
            if (selectedAnnotation != null) showAnnotation(selectedAnnotation);
            updateFlowNavigation();
            updateSelectedEdges();
            updateGraphSize();
            revalidate();
            repaint();
        }

        private String nodeLabels(int address) {
            refreshNodeLabels();
            return nodeLabelCache.getOrDefault(address, "");
        }

        private void invalidateLabels() { nodeLabelCache = null; }

        private void refreshNodeLabels() {
            DebuggerSession currentSession = index == null ? null : session;
            long revision = session.programRevision();
            long content = revision;
            long memory = session.memoryRevision();
            if (nodeLabelCache != null && labelSession == currentSession
                    && labelContentRevision == content && labelMemoryRevision == memory) return;
            Map<Integer, LinkedHashSet<String>> annotations = new HashMap<>(), symbols = new HashMap<>();
            if (index != null) for (UserAnnotations.Entry entry : index.entries()) {
                String label = entry.label.trim();
                if (!label.isEmpty()) annotations.computeIfAbsent(entry.address, ignored -> new LinkedHashSet<>()).add(label);
            }
            if (sourceMappingsCurrent) for (SourceAddressMap.Symbol symbol : compiledSymbols.get())
                symbol.location.ifPresent(location -> symbols.computeIfAbsent(location.address,
                        ignored -> new LinkedHashSet<>()).add(symbol.name));
            Map<Integer, String> labels = new HashMap<>();
            Set<Integer> keys = new HashSet<>(annotations.keySet());
            keys.addAll(symbols.keySet());
            for (int key : keys) {
                String annotation = String.join(", ", annotations.getOrDefault(key, new LinkedHashSet<>()));
                String source = String.join(", ", symbols.getOrDefault(key, new LinkedHashSet<>()));
                labels.put(key, annotation.isEmpty() ? source
                        : source.isEmpty() || source.equals(annotation) ? annotation : annotation + ", " + source);
            }
            nodeLabelCache = labels;
            labelSession = currentSession;
            labelContentRevision = content;
            labelMemoryRevision = memory;
        }

        private boolean coversInstruction(UserAnnotations.Entry annotation, int at) {
            int overlap = Math.max(at, annotation.address);
            return overlap < Math.min(instructionEnds.get(at), annotation.end);
        }

        private void updateSelectedEdges() {
            List<FlowAnalysis.Edge> edges = new ArrayList<>();
            if (flow != null) flow.forEachEdge((from, to) -> {
                Integer source = representatives.get(from), target = representatives.get(to);
                if (source == null || target == null) return;
                if (source.equals(target) && from != to) return;
                if (source == selectedNode || target == selectedNode)
                    edges.add(new FlowAnalysis.Edge(source, target));
            });
            selectedEdges = edges;
        }

        private void updateGraphSize() {
            double scale = zoomLevels[zoomIndex] / 100.0;
            int bottom = lanes.stream().mapToInt(lane -> lane.bounds.y + lane.bounds.height + 20).max().orElse(0);
            setPreferredSize(new Dimension((int) Math.ceil(Math.max(460, 290 + lanes.size() * 114 + selectedEdges.size() * 20) * scale),
                    (int) Math.ceil(Math.max(Math.max(400, boxes.size() * 90 + 50), bottom) * scale)));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (flow == null && lanes.isEmpty()) {
                return;
            }
            if (selectionStart == null && candidateLane == null) invalidateLabels();
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = zoomLevels[zoomIndex] / 100.0;
            g.scale(scale, scale);
            Rectangle clip = g.getClipBounds();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (candidateLane != null) {
                g.setColor(new Color(55, 120, 200, 40));
                g.fillRoundRect(candidateLane.x, candidateLane.y, candidateLane.width, candidateLane.height, 14, 14);
                g.setColor(new Color(55, 120, 200));
                g.drawRoundRect(candidateLane.x, candidateLane.y, candidateLane.width, candidateLane.height, 14, 14);
            }
            for (int i = 0; i < lanes.size(); i++) {
                Rectangle bounds = lanes.get(i).bounds;
                Color color = laneFill(lanes.get(i).annotation);
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
                g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);
            }
            for (int i = 0; i < lanes.size(); i++) {
                AnnotationLane lane = lanes.get(i);
                Color color = Color.getHSBColor((i * .19f) % 1f, .65f, .65f);
                g.setColor(color);
                g.setStroke(new BasicStroke(i == selectedLane ? 3f : 1.5f));
                g.drawRoundRect(lane.bounds.x, lane.bounds.y, lane.bounds.width, lane.bounds.height, 14, 14);
                g.fillRoundRect(lane.header.x, lane.header.y, lane.header.width, lane.header.height, 8, 8);
                g.setColor(Color.WHITE);
                String title = laneTitle(lane.annotation);
                g.drawString(title.length() > 12 ? title.substring(0, 12) : title, lane.header.x + 5, lane.header.y + 16);
            }
            List<FlowAnalysis.Edge> edges = selectedEdges;
            for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
                FlowAnalysis.Edge edge = edges.get(edgeIndex);
                int from = edge.from, to = edge.to;
                Rectangle a = boxes.get(from), b = boxes.get(to);
                if (a == null || b == null) continue;
                Rectangle bounds = a.union(b);
                bounds.grow(getPreferredSize().width, 20);
                if (clip != null && !clip.intersects(bounds)) continue;
                g.setColor(getForeground());
                boolean fallThrough = flow.nextAddress(from) == to && b.y == a.y + 90;
                int lane = a.x + a.width + 35 + edgeIndex * 20;
                int x = fallThrough ? a.x + a.width / 2 : a.x + a.width;
                int offset = Math.floorMod(edgeIndex, 9) * 5 - 20;
                int y = fallThrough ? a.y + a.height : a.y + (from == to ? a.height / 3 : a.height / 2 + offset);
                int tx = fallThrough ? b.x + b.width / 2 : b.x + b.width;
                int ty = fallThrough ? b.y : b.y + (from == to ? 2 * b.height / 3 : b.height / 2 + offset);
                if (fallThrough) g.drawLine(x, y, tx, ty);
                else {
                    g.drawLine(x, y, lane, y);
                    g.drawLine(lane, y, lane, ty);
                    g.drawLine(lane, ty, tx, ty);
                }
                double angle = fallThrough ? Math.atan2(ty - y, tx - x) : Math.PI;
                for (double delta : new double[] {-.5, .5})
                    g.drawLine(
                            tx,
                            ty,
                            tx - (int) (10 * Math.cos(angle + delta)),
                            ty - (int) (10 * Math.sin(angle + delta)));
            }
            Font nodeFont = new Font(Font.MONOSPACED, Font.PLAIN, g.getFont().getSize());
            Font labelFont = nodeFont.deriveFont(Font.BOLD);
            for (Map.Entry<Integer, Rectangle> entry : boxes.entrySet()) {
                Rectangle box = entry.getValue();
                if (clip != null && !clip.intersects(box)) continue;
                boolean selected = entry.getKey() == selectedNode;
                boolean pc = !running && !tasks.isBusy()
                        && entry.getKey() == session.execution().cpuContext().getInstructionLocation();
                Color selectionBackground = UIManager.getColor("List.selectionBackground");
                Color selectionForeground = UIManager.getColor("List.selectionForeground");
                Color surface = UIManager.getColor("Table.background");
                Color foreground = UIManager.getColor("Table.foreground");
                g.setColor(selected && selectionBackground != null ? selectionBackground : surface != null ? surface : getBackground());
                g.fillRoundRect(box.x, box.y, box.width, box.height, 10, 10);
                g.setColor(selected && selectionForeground != null ? selectionForeground : foreground != null ? foreground : getForeground());
                float width = selected ? 2f : 1f;
                boolean marked = flow.isData(entry.getKey()) || flow.unknown().containsKey(entry.getKey());
                g.setStroke(marked ? new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 4f}, 0f)
                        : new BasicStroke(width));
                g.drawRoundRect(box.x, box.y, box.width, box.height, 10, 10);
                String labels = nodeLabels(entry.getKey());
                int addressY = labels.isEmpty() ? 20 : 34;
                if (!labels.isEmpty()) {
                    g.setFont(labelFont);
                    g.drawString(labels.length() > 28 ? labels.substring(0, 28) : labels, box.x + 8, box.y + 16);
                }
                g.setFont(nodeFont);
                g.drawString(nodeAddress(entry.getKey()) + (pc ? " · PC" : "")
                        + (flow.isData(entry.getKey()) ? " · DATA" : ""), box.x + 8, box.y + addressY);
                String text = nodeMnemonic(entry.getKey());
                g.drawString(
                        text.length() > 28 ? text.substring(0, 28) : text, box.x + 8, box.y + addressY + 20);
            }
            if (selectionArea != null) {
                g.setColor(new Color(55, 120, 200, 35));
                g.fill(selectionArea);
                g.setColor(new Color(55, 120, 200));
                g.draw(selectionArea);
            }
            g.dispose();
        }
    }
}
