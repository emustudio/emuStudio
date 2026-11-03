/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import net.emustudio.emulib.runtime.ui.debugger.BreakpointColumn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import static net.emustudio.emulib.runtime.ui.StandardIcons.*;

/** Unified debugger workspace. Instruction and byte modes share one table; browsing never changes PC. */
public final class DebuggerPanel extends JPanel implements AutoCloseable {
    @FunctionalInterface public interface AddressSelectionListener {
        void selected(int address, int bank, boolean visit);
    }

    private AddressSelectionListener addressSelectionListener = (address, bank, visit) -> {};
    private Runnable inspectionReadyListener = () -> {};
    private boolean pinnedSelection, changingView, updatingSymbols;
    private final JCheckBoxMenuItem pinInspection = new JCheckBoxMenuItem("Pin inspection");
    private final DefaultListModel<SourceAddressMap.Symbol> symbolRows = new DefaultListModel<>();
    private final JList<SourceAddressMap.Symbol> symbols = new JList<>(symbolRows);
    private final JLabel symbolContext = new JLabel("Compile sources to browse symbols.");
    private final JTextField symbolFilter = new JTextField(18);
    private List<SourceAddressMap.Symbol> compiledSymbols = List.of();
    private final JLabel sourceContext = new JLabel();
    private boolean sourceMappingsCurrent;
    private Consumer<SourceAddressMap.Symbol> symbolSelected = symbol -> {};
    private final DebugSession session;
    private final GUI gui;
    private final Consumer<SourceCodePosition> sourceNavigator;
    private final Runnable breakpointsChanged;
    private final AnalysisTasks tasks;
    private final Map<Component, Boolean> enabledControls = new IdentityHashMap<>();
    private boolean disposed;
    private final AtomicBoolean refreshQueued = new AtomicBoolean();
    private boolean initializationPending;
    private JToggleButton recordingControl;
    private final Dialogs dialogs;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel breakpointPanel;
    private final JPanel watchPanel;
    private final JPanel navigationToolbar;
    private AnnotationForm annotationForm;
    private boolean updatingAnnotations;
    private final JPanel annotationList = new JPanel(new CardLayout());
    private final JLabel annotationEmpty = new JLabel("", SwingConstants.CENTER);
    private final JLabel baselineContext;
    private final JLabel resultContext;
    private final JTextField annotationFilter;
    private final JCheckBox bookmarksOnly = new JCheckBox("Bookmarks only");
    private final Set<String> changedWatches = new HashSet<>();
    private List<String> displayedWatches = List.of();
    private List<DebugSession.Rule> displayedRules = List.of();
    private List<DebugSession.AccessRule> displayedMemoryWatches = List.of();
    private List<DebugCPU.Instruction> displayedHistory = List.of();
    private List<DebugCPU.Instruction> historySource = List.of();
    private final List<int[]> historyGroups = new ArrayList<>();
    private final HistoryModel historyModel = new HistoryModel();
    private final JTable history = new JTable(historyModel);
    private final JSpinner recentLimit = new JSpinner(new SpinnerNumberModel(1024, 1, 1_000_000, 1024));
    private final JTextField historyFrom = new JTextField(6), historyThrough = new JTextField(6),
            historyRegister = new JTextField(5), historyMemory = new JTextField(6), historyMemoryBank = new JTextField(3);
    private final JComboBox<String> historyAccess = new JComboBox<>(new String[] {"Any access", "Read", "Write"});
    private final JComboBox<String> historyWriteTarget = new JComboBox<>();
    private List<DebugCPU.Access> selectedHistoryAccesses = List.of();
    private final JCheckBox foldHistory = new JCheckBox("Fold identical repeats");
    private final JLabel historyContext = new JLabel();
    private final JLabel historyCaptureStatus = new JLabel(), historyFilterSummary = new JLabel();
    private final JComboBox<String> historyOrigin = new JComboBox<>(new String[] {"Recent execution"});
    private final JPanel historyBody = new JPanel(new CardLayout());
    private final JLabel historyEmptyTitle = new JLabel(), historyEmptyHint = new JLabel();
    private JButton fileRecordingControl, startFileRecording, nextHistoryPage, previousHistoryPage,
            firstHistoryPage, exportRecordingControl, saveRecentControl, exportResultsControl,
            historyEmptyRecord, historyEmptyClear, historyClearControl;
    private JPanel historyPageControls;
    private JPanel historyWorkControls;
    private final JProgressBar historyProgress = new JProgressBar();
    private JButton cancelHistoryWork;
    private boolean historyAnalysis, historyCanCancel, historyCancelRequested;
    private Path availableRecording;
    private boolean updatingHistoryOrigin;
    private HistoryFilter appliedHistoryFilter = HistoryFilter.empty();
    private final List<InstructionRecording.Reader.Cursor> historyPageStarts = new ArrayList<>();
    private Path openedRecording;
    private InstructionRecording.Header openedHeader;
    private InstructionRecording.Reader.Cursor nextHistoryCursor;
    private long historyPage = 1;
    private boolean openedFinished;
    private String openedStatus = "";
    private List<Integer> ordinaryBreakpoints = List.of();
    private final int breakpointColumn;
    private final boolean addedBreakpointColumn;
    private final JTextField pattern;
    private int inspectedAddress;
    private int inspectionBank;
    private final JTextField memoryAddress = new JTextField(8);
    private final JComboBox<Integer> memoryBank = new JComboBox<>();
    private final JComboBox<String> width = new JComboBox<>(new String[] {"1", "2", "4", "8"});
    private final JComboBox<String> watchFormat = new JComboBox<>(new String[] {"Hex + decimal", "Binary", "ASCII"});
    private final JCheckBox bigEndian = new JCheckBox("Big endian");
    private final JCheckBox capturedStrings = new JCheckBox("String column");
    private final JCheckBox showUnchangedRegisters = new JCheckBox("Show unchanged registers");
    private final DefaultListModel<String> watchRows = new DefaultListModel<>();
    private final DefaultListModel<String> ruleRows = new DefaultListModel<>();
    private final DefaultListModel<String> memoryWatchRows = new DefaultListModel<>();
    private final DefaultListModel<AnnotationIndex.Entry> annotationRows = new DefaultListModel<>();
    private final DefaultListModel<FlowAnalysis.Edge> referenceRows = new DefaultListModel<>();
    private FlowAnalysis referenceFlow;
    private boolean referencesFromSweep;
    private final JList<String> watches = new JList<>(watchRows),
            rules = new JList<>(ruleRows), memoryWatches = new JList<>(memoryWatchRows);
    private final JList<AnnotationIndex.Entry> annotations = new JList<>(annotationRows);
    private final JList<FlowAnalysis.Edge> references = new JList<>(referenceRows);
    private final JTextArea historicalState;
    private final AddressModel model = new AddressModel();
    private final DebugTableImpl table;
    private final DebugTableModel instructionModel;
    private final boolean byteMemory;
    private final InstructionAnnotationsModel instructionRows;
    private final JScrollPane tableScroll;
    private final JComponent instructionView;
    private final Container instructionTableHolder;
    private JPanel memoryView;
    private TableCellRenderer memoryRenderer;
    private boolean running;
    private int lastMemoryAddress = -1;
    private int lastInstructionAddress;
    private final GraphPanel graph = new GraphPanel();
    private final Deque<int[]> backward = new ArrayDeque<>(), forward = new ArrayDeque<>();
    private final Map<String, Long> watchValues = new HashMap<>();
    private final Set<Integer> differences = new HashSet<>();
    private Map<Integer, String> discoveredStrings = Map.of();
    private int differenceBank = -1, stringBank = -1;
    private MemoryAnalysis image, baseline, viewCapture, searchCapture;
    private AnnotationIndex index;
    private List<Integer> results = List.of();
    private int searchBank, searchWidth = 1;
    private boolean searchLittle = true, navigating;
    private final CPU.CPUListener stateListener =
            new CPU.CPUListener() {
                @Override
                public void runStateChanged(CPU.RunState state) {
                    SwingUtilities.invokeLater(
                            () -> {
                                if (disposed) return;
                                if (state == CPU.RunState.STATE_RUNNING) {
                                    tasks.cancel();
                                    updateRecordingControls(false);
                                    return;
                                }
                                try { session.requirePaused(); } catch (IllegalStateException ignored) { return; }
                                if (tasks.isBusy()) {
                                    if (image == null) initializationPending = true;
                                    return;
                                }
                                if (byteMemory && image == null) { initializeImage(); return; }
                                changedWatches.clear();
                                if (inspectBreakpointHit()) { refresh(); return; }
                                if (!pinnedSelection) {
                                    instructionModel.currentPage();
                                    lastInstructionAddress = session.cpu.getInstructionLocation();
                                }
                                if (!isInspectionPinned())
                                    guard(() -> navigate(session.cpu.getInstructionLocation(), instructionBank(), false));
                                requestRefresh();
                            });
                }

                @Override
                public void internalStateChanged() {
                    requestRefresh();
                }
            };

    public DebuggerPanel(
            DebugSession session,
            DebugTableImpl table,
            JScrollPane tableScroll,
            JComponent instructionView,
            Consumer<SourceCodePosition> sourceNavigator,
            Runnable breakpointsChanged,
            GUI gui, Dialogs dialogs) {
        this.gui = Objects.requireNonNull(gui);
        this.table = Objects.requireNonNull(table);
        this.tableScroll = Objects.requireNonNull(tableScroll);
        this.instructionView = Objects.requireNonNull(instructionView);
        this.instructionTableHolder = Objects.requireNonNull(tableScroll.getParent());
        this.instructionModel = table.instructionModel();
        int bp = -1;
        for (int c = 0; c < instructionModel.getColumnCount(); c++)
            if (instructionModel.getColumnAt(c) instanceof BreakpointColumn) { bp = c; break; }
        addedBreakpointColumn = bp < 0 && (session.cpu.isBreakpointSupported() || session.debug != null);
        breakpointColumn = addedBreakpointColumn ? 0 : bp;
        this.byteMemory = session.memory != null && session.memory.getCellTypeClass() == Byte.class
                && session.memory.getSize() > 0;
        this.instructionRows = new InstructionAnnotationsModel();
        instructionModel.addTableModelListener(instructionRows);
        this.dialogs = Objects.requireNonNull(dialogs);
        baselineContext = gui.label("Baseline: initial capture");
        resultContext = gui.label("All addresses");
        annotationFilter = gui.textField("", 14);
        lastInstructionAddress = session.cpu.getInstructionLocation();
        inspectedAddress = lastInstructionAddress;
        pattern = gui.textField("00 ??", 12);
        historicalState = gui.textAreaReadOnly(50, 10);
        for (JList<?> list : List.of(watches, rules, memoryWatches, annotations, references)) gui.styleList(list);
        this.session = session;
        if (session.memory != null && instructionModel instanceof DebugTableModelImpl)
            ((DebugTableModelImpl) instructionModel).setInstructionIndex(new InstructionIndex(
                    () -> session.programIndex().snapshot(), session.memory::getSelectedBank));
        this.sourceNavigator = sourceNavigator;
        this.breakpointsChanged = Objects.requireNonNull(breakpointsChanged);
        tasks = new AnalysisTasks(session, this::setBusy, this::showFailure);
        setLayout(new BorderLayout(6, 6));
        navigationToolbar = row();
        JToolBar navigationActions = line(navigationToolbar, button(ICON_UNDO, "Back", () -> travel(backward, forward)),
                button(ICON_REDO, "Forward", () -> travel(forward, backward)),
                new JToolBar.Separator());
        navigationActions.getAccessibleContext().setAccessibleName("Inspected address history and memory snapshot");
        ((JComponent) navigationActions.getComponent(0)).getAccessibleContext()
                .setAccessibleDescription("Previous inspected address; does not reverse CPU execution");
        ((JComponent) navigationActions.getComponent(1)).getAccessibleContext()
                .setAccessibleDescription("Next inspected address; does not advance CPU execution");
        tabs.setName("debuggerViews");
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Debugger", instructionView);
        tabs.addTab("Symbols", symbolsPanel());
        if (byteMemory) {
            inspectionBank = session.memory.getSelectedBank();
            memoryView = memoryPanel();
            tabs.addTab("Memory", memoryView);
            tabs.addTab("Annotations", annotationPanel());
        } else {
            tabs.addTab(
                    "Memory", gui.label("Byte-memory inspection unavailable for this computer"));
        }
        watchPanel = watchPanel();
        breakpointPanel = rulePanel();
        tabs.addTab("Breakpoints", breakpointPanel);
        tabs.addTab("References", flowPanel());
        tabs.addTab("History", historyPanel());
        add(tabs, BorderLayout.CENTER);
        JPanel context = gui.panel("insets 2 6, fillx, hidemode 3", "[grow]", "");
        sourceContext.getAccessibleContext().setAccessibleName("Source synchronization status");
        context.add(sourceContext, "growx");
        add(context, BorderLayout.NORTH);
        JButton currentProgram = button(ICON_SNAPSHOT, "Take snapshot", () -> {
            if (!confirmSaveAnnotations()) return;
            if (index != null) session.saveAnnotations();
            runAnalysis(session::captureImage, this::showImage);
        });
        currentProgram.setEnabled(session.memory != null && session.memory.getCellTypeClass() == Byte.class);
        currentProgram.getAccessibleContext().setAccessibleDescription(
                "Save this image's annotations, then capture current memory and load matching annotations");
        navigationActions.add(currentProgram);
        tabs.addChangeListener(event -> activateView());
        activateView();
        session.cpu.addCPUListener(stateListener);
        setMinimumSize(new Dimension(300, 160));
        JPopupMenu selectionMenu = new JPopupMenu();
        selectionMenu.add(pinInspection);
        pinInspection.addActionListener(event -> {
            pinnedSelection = pinInspection.isSelected();
            graph.repaint();
        });
        selectionMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) {
                pinInspection.setSelected(isInspectionPinned());
                pinInspection.setEnabled(!running && !tasks.isBusy());
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event) {}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event) {}
        });
        table.setComponentPopupMenu(selectionMenu);
        table
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {
                            if (!navigating && !changingView && table.getModel() == model
                                    && !e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                                int at =
                                        model.addressAt(
                                                table.convertRowIndexToModel(
                                                        table.getSelectedRow()));
                                if (at == inspectedAddress) return;
                                rememberAddress(at, selectedBank());
                                lastMemoryAddress = at;
                                inspectedAddress = at;
                                notifyAddressSelection(true);
                            }
                        });
        table.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (table.getModel() == model && e.getClickCount() == 2)
                            guard(() -> showSource(inspectedAddress));
                    }
                });
        refresh();
        if (session.memory != null && session.memory.getCellTypeClass() == Byte.class && session.memory.getSize() > 0)
            initializeImage();
    }

    private void initializeImage() {
        if (session.memory == null || session.memory.getCellTypeClass() != Byte.class || session.memory.getSize() == 0) return;
        guard(() -> runAnalysis(session::imageCapture, this::showImage));
    }

    public JComponent watchesPanel() {
        return watchPanel;
    }

    public JComponent navigationToolbar() {
        return navigationToolbar;
    }

    public void setAddressSelectionListener(AddressSelectionListener listener) {
        addressSelectionListener = Objects.requireNonNull(listener);
    }

    public void setInspectionReadyListener(Runnable listener) {
        inspectionReadyListener = Objects.requireNonNull(listener);
    }

    public boolean isInspectionPinned() {
        return pinnedSelection;
    }

    public void setSourceNavigationStatus(String message, boolean current) {
        sourceContext.setText(message);
        sourceContext.setToolTipText(message);
        sourceContext.getAccessibleContext().setAccessibleDescription(message);
        sourceContext.setVisible(!message.isEmpty() && !"Source mapping current".equals(message));
        sourceMappingsCurrent = current;
        symbols.setEnabled(current && !running && !tasks.isBusy());
        updateSymbolCount();
    }

    private void notifyAddressSelection(boolean visit) {
        if (byteMemory) {
            memoryAddress.setText(String.format("0x%04X", inspectedAddress));
            memoryBank.setSelectedItem(selectedBank());
        }
        graph.repaint();
        if (!disposed && !running && !tasks.isBusy())
            addressSelectionListener.selected(inspectedAddress, selectedBank(), visit);
    }

    /** Automatic source following keeps the active analysis tab and respects a pinned inspection. */
    public void synchronizeSourceAddress(SourceAddressMap.Location location) {
        if (isInspectionPinned()) return;
        if (!location.instruction && memoryView == null) return;
        if (location.instruction && location.bank == instructionBank())
            alignInstructionAddress(location.address);
        navigate(location.address, location.bank, true,
                location.instruction ? NavigationTarget.SOURCE_INSTRUCTION : NavigationTarget.SOURCE_DATA);
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

    public void setSymbols(List<SourceAddressMap.Symbol> values, Consumer<SourceAddressMap.Symbol> selection) {
        updatingSymbols = true;
        try {
            symbolSelected = Objects.requireNonNull(selection);
            compiledSymbols = List.copyOf(values);
            filterSymbols();
        } finally { updatingSymbols = false; }
    }

    public void selectSymbol(SourceCodePosition position, int address, int bank) {
        SourceAddressMap.Symbol selected = null;
        for (SourceAddressMap.Symbol symbol : compiledSymbols) {
            if (!symbol.position.fileName.equals(position.fileName)) continue;
            if (symbol.position.line == position.line) { selected = symbol; break; }
            if (symbol.location.filter(location -> location.bank == bank && location.address <= address).isPresent()
                    && (selected == null || symbol.position.line > selected.position.line)) selected = symbol;
        }
        updatingSymbols = true;
        try { symbols.setSelectedValue(selected, true); if (selected == null) symbols.clearSelection(); }
        finally { updatingSymbols = false; }
    }

    private JPanel symbolsPanel() {
        JPanel panel = borderPanel(4);
        symbols.getAccessibleContext().setAccessibleName("Compiled symbols");
        symbols.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        symbols.setToolTipText("Definitions from the last successful compilation. Recompile changed sources to synchronize addresses.");
        gui.styleList(symbols);
        symbols.setCellRenderer(new DetailRenderer<>((symbol, row) -> new ListDetail(
                symbol.location.map(at -> String.format("%04X · Bank %d", at.address, at.bank))
                        .orElse(symbol.sourceOnly ? "Source only" : "Address unavailable"),
                symbol.name, Path.of(symbol.position.fileName).getFileName() + ":" + symbol.position.line,
                symbol.position.fileName + ":" + symbol.position.line)));
        JPanel filter = gui.panel("insets 4, fillx", "[][grow]", "");
        filter.add(label("Filter symbols", symbolFilter));
        filter.add(symbolFilter, "growx");
        symbolFilter.setToolTipText("Filter by symbol name, source file or address");
        symbolFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { filterSymbols(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { filterSymbols(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { filterSymbols(); }
        });
        panel.add(filter, BorderLayout.NORTH);
        symbols.addListSelectionListener(event -> {
            if (!updatingSymbols && !event.getValueIsAdjusting() && symbols.getSelectedValue() != null)
                symbolSelected.accept(symbols.getSelectedValue());
        });
        panel.add(gui.scrollPane(symbols));
        panel.add(symbolContext, BorderLayout.SOUTH);
        return panel;
    }

    private void filterSymbols() {
        SourceAddressMap.Symbol selected = symbols.getSelectedValue();
        String query = symbolFilter.getText().strip().toLowerCase(Locale.ROOT);
        boolean wasUpdating = updatingSymbols;
        updatingSymbols = true;
        try {
            updateRows(symbolRows, compiledSymbols.stream()
                    .filter(symbol -> symbol.toString().toLowerCase(Locale.ROOT).contains(query))
                    .collect(java.util.stream.Collectors.toList()));
            if (selected != null && symbolRows.contains(selected)) symbols.setSelectedValue(selected, false);
            else symbols.clearSelection();
            updateSymbolCount();
        } finally { updatingSymbols = wasUpdating; }
    }

    private void updateSymbolCount() {
        symbolContext.setVisible(!compiledSymbols.isEmpty());
        symbolContext.setText(symbolRows.size() == compiledSymbols.size() ? compiledSymbols.size() + " symbols"
                : symbolRows.size() + " of " + compiledSymbols.size() + " symbols");
        symbolContext.setToolTipText("Source-only declarations have no memory address. Names and addresses come from the last successful compilation.");
    }

    @Override
    public void close() {
        if (disposed) return;
        disposed = true;
        tasks.close();
        session.cpu.removeCPUListener(stateListener);
        instructionModel.removeTableModelListener(instructionRows);
    }

    private void showImage(DebugSession.ImageCapture captured) {
        boolean initial = image == null;
        index = session.installImage(captured);
        image = captured.image;
        if (initial) baseline = image;
        if (annotationForm != null) annotationForm.load(selectedBank(), inspectedAddress);
        refreshAnnotations(annotationFilter.getText());
        if (annotationForm != null) selectAnnotationTarget();
        if (initial) model.showAll();
        else model.refreshData();
    }

    private void showFailure(Throwable failure) {
        if (failure.getCause() instanceof java.util.concurrent.CancellationException) {
            if (historyCancelRequested) historyContext.setText("Recording operation cancelled. Any output file may be partial.");
            return;
        }
        dialogs.showError(failure.getMessage() == null ? failure.toString() : failure.getMessage(), "Debugger");
    }

    private <T> void runAnalysis(Callable<T> operation, Consumer<T> apply) {
        tasks.submit(operation, apply);
    }

    private void setBusy(boolean busy) {
        if (busy) {
            rememberAndDisable(this);
            if (watchPanel != null) rememberAndDisable(watchPanel);
            rememberAndDisable(navigationToolbar);
            if (historyAnalysis && historyWorkControls != null) {
                historyWorkControls.setVisible(true);
                historyWorkControls.setEnabled(true);
                historyProgress.setEnabled(true);
                cancelHistoryWork.setVisible(historyCanCancel);
                cancelHistoryWork.setEnabled(historyCanCancel);
            }
        }
        else {
            enabledControls.forEach(Component::setEnabled);
            enabledControls.clear();
            historyAnalysis = historyCancelRequested = false;
            if (historyWorkControls != null) historyWorkControls.setVisible(false);
            updateExecutionState();
            if (recordingControl != null) {
                boolean paused = true;
                try { session.requirePaused(); } catch (IllegalStateException ignored) { paused = false; }
                updateRecordingControls(paused);
            }
            if (initializationPending && image == null) {
                initializationPending = false;
                if (!disposed) initializeImage();
            }
            if (image != null && !tasks.isBusy()) guard(this::inspectBreakpointHit);
            if (!disposed && image != null && !tasks.isBusy() && !running) inspectionReadyListener.run();
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

    private JToggleButton toggleButton(String icon, String tooltip) {
        JToggleButton button = gui.toolbarToggleButton(event -> {}, icon, tooltip);
        button.setFocusable(true);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private void guard(Operation operation) {
        if (disposed || tasks.isBusy()) return;
        try {
            session.requirePaused();
            operation.run();
        } catch (Exception e) {
            showFailure(e);
        }
    }

    private int selectedBank() {
        return inspectionBank;
    }

    public void showMemory() {
        tabs.setSelectedIndex(tabs.indexOfTab("Memory"));
    }

    public void showInstructions() {
        tabs.setSelectedComponent(instructionView);
    }

    public boolean isInstructionView() {
        return tabs.getSelectedComponent() == instructionView;
    }

    public boolean isBreakpointCellSelected() {
        int column = table.getSelectedColumn();
        return table.getModel() == instructionRows && column >= 0
                && table.convertColumnIndexToModel(column) == breakpointColumn;
    }

    public void showSelectedSource() {
        guard(() -> showSource(inspectedAddress));
    }

    public void setExecutionRunning(boolean running) {
        this.running = running;
        updateExecutionState();
    }

    private void updateExecutionState() {
        table.setEnabled(!running && !tasks.isBusy());
        symbols.setEnabled(sourceMappingsCurrent && !running && !tasks.isBusy());
        boolean visible = !running || !isInstructionView();
        table.setVisible(visible);
        tableScroll.setVisible(visible);
        graph.repaint();
        if (annotationForm != null) annotationForm.updateState();
    }

    private int instructionBank() {
        return session.memory == null ? 0 : session.memory.getSelectedBank();
    }

    private void activateView() {
        boolean wasNavigating = navigating;
        navigating = true;
        changingView = true;
        try {
            if (isInstructionView()) {
                if (selectedBank() != instructionBank()) {
                    inspectionBank = instructionBank();
                    resetMemoryView();
                }
                instructionTableHolder.add(tableScroll);
                table.showInstructions(instructionRows);
                if (breakpointColumn >= 0) {
                    TableColumn breakpoint = table.getColumnModel().getColumn(breakpointColumn);
                    breakpoint.setMinWidth(32);
                    breakpoint.setMaxWidth(32);
                    breakpoint.setPreferredWidth(32);
                }
                if (byteMemory) {
                    table.getColumnModel().getColumn(instructionRows.baseCount()).setPreferredWidth(110);
                    table.getColumnModel().getColumn(instructionRows.baseCount() + 1).setPreferredWidth(200);
                }
                inspectedAddress = lastInstructionAddress;
                selectInstructionAddress();
                table.getAccessibleContext().setAccessibleName("Instruction listing");
                tableScroll.getViewport().setViewPosition(new Point());
                tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            } else if (memoryView != null && tabs.getSelectedComponent() == memoryView) {
                memoryView.add(tableScroll, BorderLayout.CENTER);
                table.setModel(model);
                table.setDefaultRenderer(Object.class, memoryRenderer);
                table.setDefaultRenderer(String.class, memoryRenderer);
                configureMemoryColumns();
                table.getAccessibleContext().setAccessibleName("Memory bytes");
                tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                int target = lastMemoryAddress < 0 ? lastInstructionAddress : lastMemoryAddress;
                if (target >= 0 && target < session.memory.getSize() && model.rowAt(target) < 0) {
                    resetMemoryView();
                    lastMemoryAddress = target;
                }
                inspectedAddress = target;
                int row = model.rowAt(target);
                if (target >= 0 && row >= 0) {
                    table.setRowSelectionInterval(row, row);
                    SwingUtilities.invokeLater(() -> {
                        if (!disposed && table.getModel() == model && table.getSelectedRow() >= 0)
                            table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), 0, true));
                    });
                }
            }
            updateExecutionState();
            revalidate();
            repaint();
        } finally { changingView = false; navigating = wasNavigating; }
        if (!navigating) notifyAddressSelection(false);
    }

    public void showBreakpoints() {
        tabs.setSelectedComponent(breakpointPanel);
        refresh();
    }

    public boolean isBreakpointDialogSupported() {
        return session.cpu.isBreakpointSupported() || session.debug != null;
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
            session.requirePaused();
            if (request.address < 0 || request.address >= session.cpu.getAddressSpaceSize())
                throw new IllegalArgumentException("Address outside CPU");
            session.setBreakpoint(request.address, request.operation == BreakpointDialog.Operation.SET);
            breakpointsChanged.run();
            session.save();
            refresh();
            return true;
        } catch (Exception e) {
            showFailure(e);
            return false;
        }
    }

    private void inspectMemoryAddress() {
        if (tasks.isBusy()) throw new IllegalStateException("Wait for memory analysis to finish.");
        session.requirePaused();
        int address = net.emustudio.emulib.runtime.helpers.RadixUtils.getInstance().parseRadix(memoryAddress.getText());
        int bank = (Integer) memoryBank.getSelectedItem();
        if (address < 0 || address >= session.memory.getSize())
            throw new IllegalArgumentException("Address outside memory");
        if (bank < 0 || bank >= session.memory.getBanksCount())
            throw new IllegalArgumentException("Bank outside memory");
        inspectLive(address, bank);
    }

    private void inspectLive(int address, int selected) {
        resetMemoryView();
        showMemory();
        navigate(address, selected, true);
    }

    private boolean inspectBreakpointHit() {
        if (!byteMemory || image == null || tasks.isBusy()) return false;
        DebugSession.BreakpointHit hit = session.takeBreakpointHit();
        if (hit == null) return false;
        instructionModel.currentPage();
        lastInstructionAddress = session.cpu.getInstructionLocation();
        inspectLive(hit.address, hit.bank);
        return true;
    }

    public void followAddress(int at, int selected) {
        if (navigating || tasks.isBusy()) return;
        navigate(at, selected, true);
    }

    public void navigate(int at, int selected, boolean remember) {
        navigate(at, selected, remember, NavigationTarget.LISTING);
    }

    private void rememberAddress(int at, int selected) {
        if (inspectedAddress == at && selectedBank() == selected) return;
        backward.addLast(new int[] {inspectedAddress, selectedBank(), table.getModel() == model ? 1 : 0});
        while (backward.size() > 100) backward.removeFirst();
        forward.clear();
    }

    private enum NavigationTarget { LISTING, SOURCE_INSTRUCTION, SOURCE_DATA }

    private void navigate(int at, int selected, boolean remember, NavigationTarget target) {
        if (tasks.isBusy()) return;
        session.requirePaused();
        if (session.memory == null) return;
        if (at < 0
                || at >= session.memory.getSize()
                || selected < 0
                || selected >= (byteMemory ? session.memory.getBanksCount() : 1))
            throw new IllegalArgumentException("Address/bank outside memory");
        if (table.getModel() == model && model.rowAt(at) < 0 && !remember) return;
        if (remember) { rememberAddress(at, selected); lastMemoryAddress = at; }
        navigating = true;
        try {
            if (selectedBank() != selected) resetMemoryView();
            inspectionBank = selected;
            if (memoryView != null && (isInstructionView() || target == NavigationTarget.LISTING)
                    && (selected != instructionBank() || target == NavigationTarget.SOURCE_DATA)) showMemory();
            inspectedAddress = at;
            if (table.getModel() == model && tabs.getSelectedComponent() == memoryView) {
                int row = model.rowAt(at);
                if (row < 0 && remember) {
                    resetMemoryView();
                    row = at;
                }
                if (row >= 0) {
                    lastMemoryAddress = at;
                    table.setRowSelectionInterval(row, row);
                    table.scrollRectToVisible(table.getCellRect(row, 0, true));
                }
            } else if (target != NavigationTarget.SOURCE_DATA && selected == instructionBank()) {
                if (remember && target == NavigationTarget.LISTING) showInstructions();
                lastInstructionAddress = at;
                inspectedAddress = at;
                instructionModel.showAddress(at);
                selectInstructionAddress();
            }
        } finally {
            navigating = false;
        }
        notifyAddressSelection(remember);
    }

    private void travel(Deque<int[]> from, Deque<int[]> to) {
        if (from.isEmpty()) return;
        to.addLast(new int[] {inspectedAddress, selectedBank(), table.getModel() == model ? 1 : 0});
        int[] place = from.removeLast();
        if (place[2] == 1) showMemory(); else showInstructions();
        navigate(place[0], place[1], false);
    }

    private void resetMemoryView() {
        lastMemoryAddress = -1;
        viewCapture = null;
        searchCapture = null;
        results = List.of();
        differences.clear();
        discoveredStrings = Map.of();
        stringBank = -1;
        resultContext.setText("All addresses");
        model.showAll();
    }

    private void configureMemoryColumns() {
        if (table.getModel() != model) return;
        javax.swing.table.TableColumn strings = null;
        for (int c = 0; c < table.getColumnCount(); c++)
            if (table.getColumnModel().getColumn(c).getModelIndex() == 4)
                strings = table.getColumnModel().getColumn(c);
        if (!capturedStrings.isSelected() && strings != null) table.removeColumn(strings);
        else if (capturedStrings.isSelected() && strings == null) {
            strings = new javax.swing.table.TableColumn(4);
            strings.setHeaderValue("Captured string");
            strings.setPreferredWidth(180);
            table.addColumn(strings);
        }
        for (int c = 0; c < table.getColumnCount(); c++) {
            javax.swing.table.TableColumn column = table.getColumnModel().getColumn(c);
            switch (column.getModelIndex()) {
                case 0: column.setMaxWidth(110); column.setPreferredWidth(90); break;
                case 1: column.setMaxWidth(75); column.setPreferredWidth(60); break;
                case 2: column.setPreferredWidth(160); break;
                case 3: column.setPreferredWidth(360); break;
                default: break;
            }
        }
    }

    private JPanel memoryPanel() {
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, table.getFont().getSize()));
        memoryRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                if (!selected) {
                    boolean changed = selectedBank() == differenceBank
                            && differences.contains(model.addressAt(table.convertRowIndexToModel(row)));
                    Color accent = UIManager.getColor("Table.selectionBackground");
                    Color base = table.getBackground();
                    cell.setBackground(changed && accent != null
                            ? new Color((base.getRed() * 3 + accent.getRed()) / 4,
                                    (base.getGreen() * 3 + accent.getGreen()) / 4,
                                    (base.getBlue() * 3 + accent.getBlue()) / 4) : base);
                }
                setToolTipText(value == null ? null : value.toString());
                return cell;
            }
        };
        JPanel panel = borderPanel(4);
        JPanel controls = row();
        memoryAddress.setText(String.format("0x%04X", inspectedAddress));
        memoryAddress.setToolTipText("Decimal or hexadecimal memory address, for example 42 or 0x2A");
        for (int bank = 0; bank < session.memory.getBanksCount(); bank++) memoryBank.addItem(bank);
        memoryBank.setSelectedItem(selectedBank());
        memoryBank.setEnabled(session.memory.getBanksCount() > 1);
        JButton inspect = button(ICON_JUMP, "Inspect address", this::inspectMemoryAddress);
        memoryAddress.addActionListener(event -> inspect.doClick());
        line(controls, label("Address", memoryAddress), memoryAddress,
                label("Bank", memoryBank), memoryBank, inspect);
        memoryAddress.getAccessibleContext().setAccessibleName("Memory address");
        memoryBank.getAccessibleContext().setAccessibleName("Memory inspection bank");
        JComboBox<String> findKind = new JComboBox<>(new String[] {"Bytes", "Integer", "Text"});
        JLabel widthLabel = label("Width (bytes)", width);
        findKind.addActionListener(e -> {
            boolean integer = findKind.getSelectedIndex() == 1;
            widthLabel.setVisible(integer);
            width.setVisible(integer);
            bigEndian.setVisible(integer);
            if (integer) pattern.setToolTipText("Integer, e.g. 42 or 0x2A");
            else if (findKind.getSelectedIndex() == 2) pattern.setToolTipText("Exact UTF-8 text");
            else pattern.setToolTipText("Hex bytes, e.g. 3E ?? 32");
            controls.revalidate();
        });
        widthLabel.setVisible(false);
        width.setVisible(false);
        bigEndian.setVisible(false);
        pattern.setToolTipText("Hex bytes, e.g. 3E ?? 32");
        JButton find = button(ICON_FIND, "Find", () -> {
            if (findKind.getSelectedIndex() == 1) {
                int bytesWide = Integer.parseInt((String) width.getSelectedItem());
                byte[] bytes = MemoryAnalysis.integer(Long.decode(pattern.getText()), bytesWide, !bigEndian.isSelected());
                byte[] mask = new byte[bytesWide];
                Arrays.fill(mask, (byte) 255);
                startSearch(selectedBank(), bytesWide, !bigEndian.isSelected(), bytes, mask);
            } else if (findKind.getSelectedIndex() == 2) {
                String text = pattern.getText();
                if (text.isEmpty())
                    throw new IllegalArgumentException("Enter text to find");
                byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] mask = new byte[bytes.length];
                Arrays.fill(mask, (byte) 255);
                startSearch(selectedBank(), 1, true, bytes, mask);
            } else {
                byte[][] parsed = MemoryAnalysis.pattern(pattern.getText());
                startSearch(selectedBank(), 1, true, parsed[0], parsed[1]);
            }
        });
        pattern.addActionListener(e -> find.doClick());
        findKind.getAccessibleContext().setAccessibleName("Search type");
        line(controls, label("Find", pattern), findKind, pattern,
                widthLabel, width, bigEndian, find);
        pattern.getAccessibleContext().setAccessibleName("Search value");
        JComboBox<String> rescan = new JComboBox<>(new String[] {"Changed", "Equal", "Increased", "Decreased"});
        JToolBar resultTools = line(controls, resultContext, button(ICON_MOVE_UP, "Previous", () -> selectResult(-1)),
                button(ICON_MOVE_DOWN, "Next", () -> selectResult(1)), rescan,
                button(ICON_REFRESH, "Rescan", () -> {
                    if (searchCapture == null) throw new IllegalStateException("Find a value before rescanning");
                    if (selectedBank() != searchBank) throw new IllegalStateException("Select bank " + searchBank + " to rescan these results");
                    MemoryAnalysis before = searchCapture;
                    int selected = searchBank, bytesWide = searchWidth;
                    boolean little = searchLittle;
                    List<Integer> candidates = results;
                    String mode = (String) rescan.getSelectedItem();
                    runAnalysis(() -> {
                        MemoryAnalysis now = session.captureMemory();
                        return new SearchResult(now, before.rescan(now, selected, candidates, bytesWide, little, mode));
                    }, found -> {
                        searchCapture = found.capture;
                        viewCapture = found.capture;
                        discoveredStrings = Map.of();
                        stringBank = -1;
                        showResults(found.addresses);
                    });
                }));
        JToggleButton more = toggleButton(ICON_BIDIRECTION, "Compare / strings");
        JPanel compareTools = row();
        compareTools.setVisible(false);
        more.addActionListener(e -> {
            compareTools.setVisible(more.isSelected());
            controls.revalidate();
            controls.getParent().revalidate();
        });
        resultTools.addSeparator();
        resultTools.add(button(ICON_MEMORY, "Live memory", this::resetMemoryView));
        resultTools.add(more);
        capturedStrings.setToolTipText("Show or hide the captured-string column without changing memory or results");
        capturedStrings.addActionListener(event -> configureMemoryColumns());
        JToolBar compareActions = line(compareTools, button(ICON_ASCII, "Find strings", () -> {
            int selected = selectedBank();
            runAnalysis(() -> {
                MemoryAnalysis capture = session.captureMemory();
                return new StringResult(capture, capture.strings(selected, 4));
            }, found -> {
                viewCapture = found.capture;
                discoveredStrings = found.strings;
                stringBank = selected;
                capturedStrings.setSelected(true);
                configureMemoryColumns();
                differences.clear();
                model.show(new ArrayList<>(discoveredStrings.keySet()));
                resultContext.setText(discoveredStrings.size() + " strings");
            });
        }), button(ICON_COPY, "Capture baseline", () -> runAnalysis(session::captureMemory, captured -> {
            baseline = captured;
            baselineContext.setText("Baseline: captured now");
        })), button(ICON_BIDIRECTION, "Compare baseline", () -> {
            if (baseline == null) throw new IllegalStateException("Capture a baseline first");
            MemoryAnalysis before = baseline;
            int selected = selectedBank();
            runAnalysis(() -> {
                MemoryAnalysis now = session.captureMemory();
                return new SearchResult(now, before.differences(now, selected));
            }, found -> {
                viewCapture = found.capture;
                        discoveredStrings = Map.of();
                        stringBank = -1;
                showDifferences(found.addresses, selected);
            });
        }));
        compareActions.add(button(ICON_OPEN_FILE, "Compare binary", () -> {
            Path file = choose(false, false);
            if (file == null) return;
            int selected = selectedBank(), size = image.size();
            runAnalysis(() -> {
                byte[] bytes = MemoryAnalysis.readBinary(file, size);
                MemoryAnalysis now = session.captureMemory();
                return new SearchResult(now, now.differences(bytes, selected));
            }, found -> {
                viewCapture = found.capture;
                        discoveredStrings = Map.of();
                        stringBank = -1;
                showDifferences(found.addresses, selected);
            });
        }));
        compareActions.add(button(ICON_COMPILER, "Show source", () -> showSource(inspectedAddress)));
        compareActions.add(capturedStrings);
        JButton memoryBundle = button(ICON_DISK_MOUNTED, "Export memory and metadata", () -> {
            if (!confirmSaveAnnotations()) return;
            Path path = choose(true, true);
            if (path != null) MetadataExport.export(path, image, index,
                    session.debug == null ? session.cpu.getTitle() : session.debug.processor(), selectedBank(), 0);
        });
        memoryBundle.setText("Memory bundle…");
        compareActions.add(memoryBundle);
        controls.add(compareTools, "span, growx, wrap");
        compareActions.add(baselineContext);
        panel.add(scrollTools(controls), BorderLayout.NORTH);
        return panel;
    }

    private static final class StringResult {
        final MemoryAnalysis capture;
        final Map<Integer, String> strings;
        StringResult(MemoryAnalysis capture, Map<Integer, String> strings) {
            this.capture = capture;
            this.strings = strings;
        }
    }

    private void startSearch(int selected, int bytesWide, boolean little, byte[] bytes, byte[] mask) {
        runAnalysis(
                () -> {
                    MemoryAnalysis capture = session.captureMemory();
                    return new SearchResult(capture, capture.search(selected, bytes, mask));
                },
                found -> {
                    searchCapture = found.capture;
                    viewCapture = found.capture;
                    discoveredStrings = Map.of();
                    stringBank = -1;
                    searchBank = selected;
                    searchWidth = bytesWide;
                    searchLittle = little;
                    differences.clear();
                    showResults(found.addresses);
                });
    }

    private void showResults(List<Integer> found) {
        results = found;
        model.show(results);
        resultContext.setText(results.size() + " matches");
    }

    private void showDifferences(List<Integer> found, int selected) {
        differences.clear();
        differences.addAll(found);
        differenceBank = selected;
        model.show(found);
        resultContext.setText(found.size() + " changed bytes");
    }

    private static final class SearchResult {
        final MemoryAnalysis capture;
        final List<Integer> addresses;

        SearchResult(MemoryAnalysis capture, List<Integer> addresses) {
            this.capture = capture;
            this.addresses = addresses;
        }
    }

    private void selectResult(int delta) {
        int count = table.getRowCount();
        if (count == 0) return;
        int row = Math.floorMod(table.getSelectedRow() + delta, count);
        navigate(model.addressAt(row), selectedBank(), true);
    }

    private JPanel annotationPanel() {
        JPanel panel = borderPanel(4);
        annotations.setCellRenderer(new DetailRenderer<>((entry, row) -> {
            String kind = "Data";
            if (entry.kind.equals("UNKNOWN")) kind = "Unspecified";
            else if (entry.kind.equals("CODE")) kind = "Code";
            return new ListDetail(String.format("%04X · Bank %d", entry.address, entry.bank),
                    entry.label.isBlank() ? "Unnamed annotation" : entry.label,
                    kind + (entry.bookmark ? " · Bookmark" : "")
                            + (entry.comment.isBlank() ? "" : " · " + entry.comment), entry.toString());
        }));
        annotations.getAccessibleContext().setAccessibleName("Annotations");
        annotations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        annotationList.add(gui.scrollPane(annotations), "list");
        annotationList.add(annotationEmpty, "empty");
        panel.add(annotationList, BorderLayout.CENTER);
        annotationForm = new AnnotationForm();
        panel.add(annotationForm, BorderLayout.SOUTH);
        JPanel tools = row();
        annotations.addListSelectionListener(e -> {
            AnnotationIndex.Entry entry = annotations.getSelectedValue();
            if (updatingAnnotations || e.getValueIsAdjusting() || entry == null
                    || annotationForm.matches(entry)) return;
            if (!confirmSaveAnnotations()) {
                selectAnnotationTarget();
                return;
            }
            annotationForm.load(entry.bank, entry.address);
            guard(() -> navigate(entry.address, entry.bank, true, NavigationTarget.SOURCE_DATA));
        });
        JToolBar filterTools = line(tools, label("Search annotations", annotationFilter), annotationFilter, bookmarksOnly);
        JButton newAnnotation = button(ICON_RENAME, "Annotate inspected address", () -> {
            if (!confirmSaveAnnotations()) return;
            annotationForm.load(selectedBank(), inspectedAddress);
            selectAnnotationTarget();
            annotationForm.labelField.requestFocusInWindow();
        });
        newAnnotation.setText("New");
        filterTools.add(newAnnotation);
        JPopupMenu management = new JPopupMenu();
        management.add(annotationMenuItem("Import annotations…", () -> {
            if (!confirmSaveAnnotations()) return;
            Path path = choose(false, false);
            if (path == null) return;
            changeAnnotations(() -> index.load(path));
            annotationForm.load(annotationForm.bank, annotationForm.address);
        }));
        JMenuItem delete = annotationMenuItem("Delete selected annotation…", () -> {
            AnnotationIndex.Entry entry = annotations.getSelectedValue();
            if (entry == null || !confirmSaveAnnotations()) return;
            if (JOptionPane.showConfirmDialog(this,
                    String.format("Delete annotation at %04X (bank %d)?", entry.address, entry.bank),
                    "Delete annotation", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            changeAnnotations(() -> index.remove(entry.bank, entry.address));
            annotationForm.load(entry.bank, entry.address);
        });
        management.add(delete);
        management.addSeparator();
        management.add(annotationMenuItem("Clear all annotations…", () -> {
            if (!confirmSaveAnnotations()) return;
            if (JOptionPane.showConfirmDialog(this, "Remove all annotations from this image?",
                    "Clear annotations", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            changeAnnotations(() -> index.entries().forEach(entry -> index.remove(entry.bank, entry.address)));
            annotationForm.load(selectedBank(), inspectedAddress);
        }));
        JButton manage = new JButton("Manage…");
        manage.getAccessibleContext().setAccessibleName("Manage annotations");
        manage.addActionListener(event -> {
            delete.setEnabled(annotations.getSelectedValue() != null);
            management.show(manage, 0, manage.getHeight());
        });
        filterTools.add(manage);
        annotationFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshAnnotations(annotationFilter.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshAnnotations(annotationFilter.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshAnnotations(annotationFilter.getText()); }
        });
        bookmarksOnly.addActionListener(e -> refreshAnnotations(annotationFilter.getText()));
        panel.add(scrollTools(tools), BorderLayout.NORTH);
        return panel;
    }

    private JMenuItem annotationMenuItem(String text, Operation operation) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(event -> guard(operation));
        return item;
    }

    private void changeAnnotations(Operation change) throws Exception {
        List<AnnotationIndex.Entry> previous = index.entries();
        try {
            change.run();
            session.saveAnnotations();
        } catch (Exception failure) {
            index.entries().forEach(entry -> index.remove(entry.bank, entry.address));
            previous.forEach(index::put);
            throw failure;
        }
        refreshAnnotations(annotationFilter.getText());
        model.refreshData();
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
        updatingAnnotations = true;
        try {
            annotations.clearSelection();
            for (int i = 0; i < annotationRows.size(); i++) {
                if (annotationForm.matches(annotationRows.get(i))) {
                    annotations.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            updatingAnnotations = false;
        }
    }

    private final class AnnotationForm extends JPanel {
        private final List<String> kinds = List.of("UNKNOWN", "CODE", "DATA");
        private final JTextField labelField = gui.textField("", 12), comment = gui.textField("", 20);
        private final JCheckBox bookmark = new JCheckBox("Bookmark");
        private final JComboBox<String> kind = new JComboBox<>(new String[]{"Unspecified", "Code", "Data"});
        private final JButton save;
        private int bank, address;
        private AnnotationIndex.Entry original;
        private boolean loading, confirming, saved;

        AnnotationForm() {
            setLayout(new net.miginfocom.swing.MigLayout("insets 6, fillx, hidemode 3", "[grow]", ""));
            JPanel fields = gui.panel("insets 0, fillx", "[][grow,fill]", "");
            fields.add(label("Label", labelField));
            fields.add(labelField, "growx, wrap");
            fields.add(label("Comment", comment));
            fields.add(comment, "growx");
            add(fields, "growx, wrap");
            save = button(ICON_SAVE, "Save annotation", this::saveDraft);
            save.setName("saveAnnotation");
            JToggleButton options = toggleButton(ICON_BIDIRECTION, "Annotation options");
            options.setIcon(null);
            options.setText("Options");
            line(this, bookmark, save, options);
            JPanel extra = row();
            line(extra, label("Type", kind), kind);
            extra.setVisible(false);
            options.addActionListener(event -> {
                extra.setVisible(options.isSelected());
                revalidate();
            });
            add(extra, "growx, wrap");
            javax.swing.event.DocumentListener changed = new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent event) { updateState(); }
                public void removeUpdate(javax.swing.event.DocumentEvent event) { updateState(); }
                public void changedUpdate(javax.swing.event.DocumentEvent event) { updateState(); }
            };
            labelField.getDocument().addDocumentListener(changed);
            comment.getDocument().addDocumentListener(changed);
            bookmark.addActionListener(event -> updateState());
            kind.addActionListener(event -> updateState());
            labelField.addActionListener(event -> guard(this::saveDraft));
            comment.addActionListener(event -> guard(this::saveDraft));
            updateState();
        }

        boolean matches(AnnotationIndex.Entry entry) {
            return original != null && entry.bank == bank && entry.address == address;
        }

        void load(int selected, int at) {
            loading = true;
            try {
                bank = image == null ? selected : image.physicalBank(at, selected);
                address = at;
                original = index == null ? null : index.get(bank, address);
                if (original == null && index != null)
                    original = new AnnotationIndex.Entry(bank, address, "", "", false, "UNKNOWN");
                labelField.setText(original == null ? "" : original.label);
                comment.setText(original == null ? "" : original.comment);
                bookmark.setSelected(original != null && original.bookmark);
                kind.setSelectedIndex(original == null ? 0 : kinds.indexOf(original.kind));
                saved = false;
            } finally {
                loading = false;
            }
            updateState();
        }

        boolean dirty() {
            return original != null && (!original.label.equals(labelField.getText())
                    || !original.comment.equals(comment.getText()) || original.bookmark != bookmark.isSelected()
                    || !original.kind.equals(kinds.get(kind.getSelectedIndex())));
        }

        void updateState() {
            if (loading) return;
            boolean dirty = dirty();
            if (dirty) saved = false;
            save.setText(String.format(saved ? "Saved at %04X" : "Save at %04X", address));
            String description = String.format("Save annotation at %04X in bank %d%s", address, bank,
                    dirty ? " (unsaved changes)" : saved ? " (saved)" : "");
            save.setToolTipText(description);
            save.getAccessibleContext().setAccessibleName(description);
            save.setEnabled(dirty && !running && !tasks.isBusy());
        }

        void saveDraft() throws Exception {
            if (!dirty()) return;
            changeAnnotations(() -> index.put(new AnnotationIndex.Entry(bank, address,
                    labelField.getText(), comment.getText(), bookmark.isSelected(), kinds.get(kind.getSelectedIndex()))));
            original = index.get(bank, address);
            saved = true;
            updateState();
        }

        boolean confirmReplacement() throws Exception {
            if (confirming) return false;
            if (!dirty()) return true;
            confirming = true;
            try {
                int choice = JOptionPane.showOptionDialog(DebuggerPanel.this,
                        String.format("Save changes to annotation at %04X (bank %d)?", address, bank),
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
        watches.setFont(new Font(Font.MONOSPACED, Font.PLAIN, watches.getFont().getSize()));
        watches.getAccessibleContext().setAccessibleName("Watch expressions and values");
        watches.setToolTipText("Values use the inspected bank. Changed values are marked until the next execution stop.");
        watches.setCellRenderer(new DetailRenderer<>((value, row) -> {
            int separator = value.indexOf(" = ");
            return separator < 0 ? new ListDetail("Error", value, "", value)
                    : new ListDetail(value.substring(0, separator), value.substring(separator + 3), "", value);
        }));
        panel.add(gui.scrollPane(watches));
        JPanel tools = row();
        JTextField expression = gui.textField("", 16);
        expression.setToolTipText("Register or expression, for example A or mem[0x2000]. Values use the inspected bank.");
        JButton add = button(ICON_ADD, "Add", () -> {
            session.evaluate(expression.getText(), selectedBank());
            session.watches.add(expression.getText());
            session.save();
            expression.setText("");
            refresh();
        });
        expression.addActionListener(e -> add.doClick());
        watchFormat.addActionListener(e -> refresh());
        watchFormat.setToolTipText("Watch value format");
        watchFormat.getAccessibleContext().setAccessibleName("Watch value format");

        JPanel entry = gui.panel("insets 4, fillx", "[][grow][]", "");
        entry.add(label("Expression", expression));
        entry.add(expression, "growx");
        JToolBar entryActions = toolBar();
        entryActions.add(add);
        entry.add(entryActions);
        tools.add(entry, "span, growx, wrap");
        JToolBar watchTools = line(tools, button(ICON_REMOVE, "Remove", () -> {
            int selected = watches.getSelectedIndex();
            if (selected >= 0) {
                session.watches.remove(selected);
                session.save();
                refresh();
            }
        }), button(ICON_JUMP, "Follow value", () -> {
            int selected = watches.getSelectedIndex();
            if (selected >= 0) {
                int location = Math.toIntExact(session.evaluate(session.watches.get(selected), selectedBank()));
                showMemory();
                navigate(location, selectedBank(), true);
            }
        }), button(ICON_REFRESH, "Refresh", this::refresh));
        if (session.debug != null) {
            JPanel registerEditor = row();
            registerEditor.setVisible(false);
            JToggleButton editRegister = toggleButton(ICON_RENAME, "Edit register");
            editRegister.addActionListener(e -> {
                registerEditor.setVisible(editRegister.isSelected());
                tools.revalidate();
                tools.getParent().revalidate();
            });
            watchTools.addSeparator();
            watchTools.add(watchFormat);
            watchTools.add(editRegister);
            JComboBox<String> register = new JComboBox<>();
            session.debug.registers().forEach(r -> register.addItem(r.name));
            JTextField value = gui.textField("", 9);
            line(registerEditor, label("Register", register), register, label("Value", value), value, button(ICON_SAVE, "Write register", () -> {
                String name = (String) register.getSelectedItem();
                if (name == null) throw new IllegalStateException("No editable registers exposed by CPU");
                session.debug.writeRegister(name, Long.decode(value.getText()));
                refresh();
            }));
            tools.add(registerEditor, "span, growx, wrap");
        } else {
            watchTools.addSeparator();
            watchTools.add(watchFormat);
            line(tools, gui.label("Register editing unavailable for this CPU"));
        }
        panel.add(scrollTools(tools), BorderLayout.NORTH);
        JTabbedPane views = new JTabbedPane();
        views.getAccessibleContext().setAccessibleName("Watches");
        views.addTab("Values", panel);
        views.addTab("Memory access", memoryWatchPanel());
        JPanel container = borderPanel(0);
        container.add(views);
        container.setPreferredSize(new Dimension(280, 230));
        return container;
    }

    private JPanel memoryWatchPanel() {
        JPanel panel = borderPanel(4);
        memoryWatches.getAccessibleContext().setAccessibleName("Memory access watches");
        memoryWatches.setCellRenderer(new DetailRenderer<>((value, row) -> {
            if (row < 0 || row >= displayedMemoryWatches.size()) return new ListDetail("", value, "", value);
            DebugSession.AccessRule rule = displayedMemoryWatches.get(row);
            return new ListDetail(rule.enabled ? "Enabled" : "Disabled", rule.name,
                    (rule.kind.equals("READ") ? "Pause on reads" : "Pause on writes")
                            + String.format(" · %04X · ", rule.address) + memoryWatchOptions(rule), value);
        }));
        panel.add(gui.scrollPane(memoryWatches));
        JPanel tools = row();
        JTextField name = gui.textField("Memory watch", 10), address = gui.textField("0x0000", 7),
                condition = gui.textField("", 12), hits = gui.textField("1", 3);
        JComboBox<String> bank = new JComboBox<>();
        bank.addItem("All banks");
        for (int i = 0; i < (session.memory == null ? 1 : session.memory.getBanksCount()); i++) bank.addItem("Bank " + i);
        JCheckBox reads = new JCheckBox("Pause on reads"), writes = new JCheckBox("Pause on writes", true);
        JPanel fields = gui.panel("insets 4, fillx", "[][grow,fill]", "");
        fields.add(label("Memory watch name", name)); fields.add(name, "wrap");
        fields.add(label("Memory watch address", address)); fields.add(address, "wrap");
        fields.add(label("Memory watch bank", bank)); fields.add(bank, "wrap");
        fields.add(reads); fields.add(writes, "wrap");
        tools.add(fields, "growx, wrap");
        JPanel advancedFields = gui.panel("insets 4, fillx", "[][grow,fill]", "");
        advancedFields.add(label("Memory watch condition", condition)); advancedFields.add(condition, "wrap");
        advancedFields.add(label("Memory watch stop from hit", hits)); advancedFields.add(hits, "wrap");
        advancedFields.setVisible(false);
        JToggleButton advanced = toggleButton(ICON_SETTINGS, "Memory watch options");
        advanced.addActionListener(event -> { advancedFields.setVisible(advanced.isSelected()); tools.revalidate(); });
        JButton add = button(ICON_ADD, "Add memory watch", () -> {
            if (!reads.isSelected() && !writes.isSelected()) throw new IllegalArgumentException("Select reads or writes to watch");
            int at = Integer.decode(address.getText());
            if (at < 0 || at >= session.cpu.getAddressSpaceSize()) throw new IllegalArgumentException("Address outside CPU");
            if (!condition.getText().isBlank()) session.evaluate(condition.getText(), selectedBank());
            List<DebugSession.AccessRule> added = new ArrayList<>();
            for (String access : List.of("READ", "WRITE")) {
                if (access.equals("READ") ? !reads.isSelected() : !writes.isSelected()) continue;
                DebugSession.AccessRule rule = new DebugSession.AccessRule(name.getText(), access, at, bank.getSelectedIndex() - 1,
                        condition.getText(), Long.parseLong(hits.getText()));
                added.add(rule);
            }
            session.rules.addAll(added);
            session.updateProbe();
            session.save();
            refresh();
            memoryWatches.setSelectedIndex(displayedMemoryWatches.indexOf(added.get(0)));
        });
        line(tools, add, advanced, button(ICON_BREAKPOINT, "Enable / disable memory watch", () -> {
            int selected = memoryWatches.getSelectedIndex();
            if (selected < 0 || selected >= displayedMemoryWatches.size()) return;
            DebugSession.Rule rule = displayedMemoryWatches.get(selected);
            rule.enabled = !rule.enabled;
            session.updateProbe();
            session.save();
            refresh();
        }), button(ICON_REMOVE, "Remove memory watch", () -> {
            int selected = memoryWatches.getSelectedIndex();
            if (selected < 0 || selected >= displayedMemoryWatches.size()) return;
            session.rules.remove(displayedMemoryWatches.get(selected));
            session.updateProbe();
            session.save();
            refresh();
        }), button(ICON_RESET, "Reset memory watch hit counts", () -> {
            displayedMemoryWatches.forEach(rule -> rule.hits = 0);
            refresh();
        }));
        tools.add(advancedFields, "growx, wrap");
        if (session.debug == null) {
            for (JComponent control : List.of(name, address, bank, reads, writes, advanced, add)) control.setEnabled(false);
            tools.add(gui.label("Memory access watches unavailable for this CPU."), "wrap");
        }
        panel.add(scrollTools(tools), BorderLayout.NORTH);
        return panel;
    }

    private static String ruleOptions(DebugSession.Rule rule) {
        return "hits " + rule.hits + " / " + rule.afterHits
                + (rule.condition.isBlank() ? "" : " · if " + rule.condition);
    }

    private static String memoryWatchOptions(DebugSession.AccessRule rule) {
        return (rule.bank < 0 ? "All banks" : "Bank " + rule.bank) + " · " + ruleOptions(rule);
    }

    private JPanel rulePanel() {
        JPanel p = borderPanel(0);
        rules.getAccessibleContext().setAccessibleName("Breakpoints");
        rules.setCellRenderer(new DetailRenderer<>((value, row) -> {
            if (row >= 0 && row < displayedRules.size()) {
                DebugSession.Rule rule = displayedRules.get(row);
                String target = rule.kind.equals("SOURCE")
                        ? Path.of(rule.source).getFileName() + ":" + rule.address : String.format("%04X", rule.address);
                return new ListDetail(rule.enabled ? "Enabled" : "Disabled", rule.name,
                        target + " · " + ruleOptions(rule), value);
            }
            int ordinary = row - displayedRules.size();
            return ordinary >= 0 && ordinary < ordinaryBreakpoints.size()
                    ? new ListDetail(session.isBreakpointSet(ordinaryBreakpoints.get(ordinary))
                            ? "Enabled" : "Disabled", String.format("%04X", ordinaryBreakpoints.get(ordinary)),
                            "Execution breakpoint", value)
                    : new ListDetail("", value, "", value);
        }));
        p.add(gui.scrollPane(rules));
        JPanel tools = row();
        JTextField name = gui.textField("Breakpoint", 10), at = gui.textField("0x0000", 7),
                condition = gui.textField("", 18), hits = gui.textField("1", 3),
                source = gui.textField("", 22), sourceLine = gui.textField("1", 5);
        source.setToolTipText("Optional source file and line to stop before execution; leave blank to use Address.");
        sourceLine.setEnabled(false);
        source.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                boolean sourceTarget = !source.getText().isBlank();
                at.setEnabled(!sourceTarget);
                sourceLine.setEnabled(sourceTarget && session.debug != null);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent event) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { update(); }
        });
        JButton addRule = button(ICON_ADD, "Add breakpoint", () -> {
            boolean sourceTarget = !source.getText().isBlank();
            int location = sourceTarget ? Integer.parseInt(sourceLine.getText()) : Integer.decode(at.getText());
            if (!sourceTarget && (location < 0 || location >= session.cpu.getAddressSpaceSize()))
                throw new IllegalArgumentException("Address outside CPU");
            if (session.debug == null) {
                session.setBreakpoint(location, true);
                breakpointsChanged.run();
                session.save();
                refresh();
                rules.setSelectedIndex(displayedRules.size() + ordinaryBreakpoints.indexOf(location));
                return;
            }
            if (!condition.getText().isBlank()) session.evaluate(condition.getText(),
                    session.memory == null ? 0 : session.memory.getSelectedBank());
            DebugSession.Rule rule = new DebugSession.Rule(name.getText(), sourceTarget ? "SOURCE" : "EXEC", location,
                    condition.getText(), Long.parseLong(hits.getText()));
            if (sourceTarget) {
                if (location < 1)
                    throw new IllegalArgumentException("Enter source file and line >= 1");
                rule.source = Path.of(source.getText()).toAbsolutePath().normalize().toString();
            }
            session.rules.add(rule);
            session.updateProbe();
            refresh();
            rules.setSelectedIndex(displayedRules.indexOf(rule));
            session.save();
        });
        JPanel fields = gui.panel("insets 4, fillx, hidemode 3", "[][grow,fill][]", "");
        fields.add(label("Name", name)); fields.add(name, "growx"); fields.add(addRule, "wrap");
        fields.add(label("Address", at)); fields.add(at);
        JToggleButton advanced = toggleButton(ICON_SETTINGS, "Advanced breakpoint options");
        fields.add(advanced, "wrap");
        tools.add(fields, "span, growx, wrap");
        JPanel advancedFields = gui.panel("insets 4, fillx", "[][grow,fill][][70!,fill]", "");
        advancedFields.add(label("Condition", condition)); advancedFields.add(condition);
        advancedFields.add(label("Stop from hit", hits)); advancedFields.add(hits, "wrap");
        advancedFields.add(label("Source file", source)); advancedFields.add(source);
        advancedFields.add(label("Source line", sourceLine)); advancedFields.add(sourceLine, "wrap");
        advancedFields.setVisible(false);
        advanced.addActionListener(event -> {
            advancedFields.setVisible(advanced.isSelected());
            tools.revalidate();
        });
        tools.add(advancedFields, "span, growx, wrap");
        addRule.setEnabled(session.debug != null || session.cpu.isBreakpointSupported());

        JToolBar actions = line(tools);
        actions.add(
                button(ICON_BREAKPOINT, "Enable / disable",
                        () -> {
                            int selected = rules.getSelectedIndex();
                            if (selected >= 0 && selected < displayedRules.size()) {
                                DebugSession.Rule rule = displayedRules.get(selected);
                                rule.enabled = !rule.enabled;
                                session.updateProbe();
                                session.save();
                                refresh();
                            } else if (selected >= displayedRules.size()
                                    && selected < displayedRules.size() + ordinaryBreakpoints.size()) {
                                session.toggleOrdinaryBreakpoint(ordinaryBreakpoints.get(selected - displayedRules.size()));
                                breakpointsChanged.run();
                                session.save();
                                refresh();
                            }
                        }));
        actions.add(
                button(ICON_REMOVE, "Remove",
                        () -> {
                            int selected = rules.getSelectedIndex();
                            if (selected >= 0 && selected < displayedRules.size()) {
                                session.rules.remove(displayedRules.get(selected));
                                session.updateProbe();
                                session.save();
                                refresh();
                            } else if (selected >= displayedRules.size()
                                    && selected
                                            < displayedRules.size() + ordinaryBreakpoints.size()) {
                                session.setBreakpoint(ordinaryBreakpoints.get(selected - displayedRules.size()), false);
                                breakpointsChanged.run();
                                session.save();
                                refresh();
                            }
                        }));
        actions.add(button(ICON_SAVE, "Save settings", session::save));
        actions.add(
                button(ICON_RESET, "Reset hit counts",
                        () -> {
                            displayedRules.forEach(r -> r.hits = 0);
                            refresh();
                        }));
        actions.add(
                button(ICON_JUMP, "Go to address",
                        () -> {
                            int selected = rules.getSelectedIndex();
                            if (selected >= 0 && selected < displayedRules.size()) {
                                DebugSession.Rule rule = displayedRules.get(selected);
                                if (!rule.kind.equals("SOURCE"))
                                    navigate(
                                            rule.address,
                                            selectedBank(),
                                            true);
                            } else if (selected >= displayedRules.size()
                                    && selected
                                            < displayedRules.size() + ordinaryBreakpoints.size()) {
                                navigate(
                                        ordinaryBreakpoints.get(selected - displayedRules.size()),
                                        selectedBank(),
                                        true);
                            }
                        }));
        if (session.debug == null) {
            name.setEnabled(false);
            condition.setEnabled(false);
            hits.setEnabled(false);
            source.setEnabled(false);
            tools.add(gui.label("Conditions and source locations unavailable for this CPU."), "span, wrap");
        }
        p.add(scrollTools(tools), BorderLayout.NORTH);
        return p;
    }

    private JPanel flowPanel() {
        JComboBox<Integer> nodes = new JComboBox<>();
        nodes.setPrototypeDisplayValue(65535);
        nodes.getAccessibleContext().setAccessibleName("Decoded instruction address");
        JLabel flowHint = gui.label("Select a reference or instruction to inspect. Indirect targets may be unresolved.");
        JPanel p = borderPanel(0);
        JPanel tools = row();
        JToolBar actions = line(tools);
        actions.add(
                button(ICON_CONNECTION, "Analyze from address",
                        () -> analyzeFlow(false, nodes, flowHint)));
        actions.add(
                button(ICON_FIND, "Find incoming references",
                        () -> {
                            if (referencesFromSweep && referenceFlow != null
                                    && referenceFlow.isCurrent(session.programIndex())
                                    && referenceFlow.bank() == selectedBank())
                                showIncomingReferences(referenceFlow, inspectedAddress);
                            else analyzeFlow(true, nodes, flowHint);
                        }));
        actions.add(
                button(ICON_JUMP, "Go to reference",
                        () -> {
                            FlowAnalysis.Edge e = references.getSelectedValue();
                            if (e != null) {
                                requireCurrentReferences();
                                navigate(e.from, e.fromBank, true);
                            }
                        }));
        actions.add(label("Instruction", nodes));
        actions.add(nodes);
        actions.add(button(ICON_STEP, "Go to instruction", () -> {
            if (nodes.getSelectedItem() != null) {
                graph.requireCurrent();
                navigate((Integer) nodes.getSelectedItem(), graph.bank, true);
            }
        }));
        flowHint.setToolTipText("Graph: arrow keys select instructions; Enter follows. Incoming references list explicit branch/call targets; graph also includes fall-through. Analysis is bounded and reports partial results.");
        graph.getAccessibleContext().setAccessibleDescription(flowHint.getToolTipText());
        tools.add(flowHint, "span, wrap");
        references.getAccessibleContext().setAccessibleName("Static references");
        references.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "followReference");
        references.getActionMap().put("followReference", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                guard(() -> {
                    FlowAnalysis.Edge edge = references.getSelectedValue();
                    if (edge != null) {
                        requireCurrentReferences();
                        navigate(edge.from, edge.fromBank, true);
                    }
                });
            }
        });
        p.add(scrollTools(tools), BorderLayout.NORTH);
        JSplitPane split =
                gui.splitPaneLeftToRight(gui.scrollPane(references), gui.scrollPane(graph), .3);
        split.setResizeWeight(.3);
        p.add(split);
        return p;
    }

    private void analyzeFlow(boolean sweep, JComboBox<Integer> nodes, JLabel hint) {
        if (session.memory == null) throw new IllegalStateException("Flow analysis requires memory");
        ProgramIndex captured = session.programIndex();
        long generation = session.pausedGeneration();
        int bank = selectedBank(), address = inspectedAddress;
        tasks.submit(() -> {
            DebugSession.AnalysisCapture capture = session.captureAnalysis(generation, captured, bank);
            FlowAnalysis.Pending pending = sweep
                    ? FlowAnalysis.sweep(capture.decoder, 100000, captured, capture.base, bank)
                    : FlowAnalysis.reachable(capture.decoder, address, 256, captured, capture.base, bank);
            FlowAnalysis[] published = new FlowAnalysis[1];
            session.applyPaused(generation, () -> published[0] = pending.publish());
            return published[0];
        }, flow -> {
            if (!flow.isCurrent(session.programIndex()))
                throw new IllegalStateException("Flow analysis discarded: program changed");
            referenceFlow = flow;
            referencesFromSweep = sweep;
            referenceRows.clear();
            if (sweep) showIncomingReferences(flow, address);
            else {
                flow.forEachEdge((fromBank, from, toBank, to, kind) -> {
                    if (kind != ProgramIndex.EdgeKind.FALL_THROUGH)
                        referenceRows.addElement(new FlowAnalysis.Edge(fromBank, from, toBank, to, kind));
                });
                graph.setFlow(flow);
                nodes.removeAllItems();
                flow.forEachInstruction(instruction -> nodes.addItem(instruction.cells().start));
            }
            hint.setText(flow.truncated()
                    ? "Partial analysis: work bound reached. Known references shown; more may exist."
                    : "Select a reference or instruction to inspect. Indirect targets may be unresolved.");
        }, true);
    }

    private void showIncomingReferences(FlowAnalysis flow, int address) {
        referenceRows.clear();
        for (FlowAnalysis.Edge edge : flow.predecessors(address))
            if (!edge.kind.equals("next")) referenceRows.addElement(edge);
    }

    private void requireCurrentReferences() {
        if (referenceFlow == null || !referenceFlow.isCurrent(session.programIndex()))
            throw new IllegalStateException("Flow analysis is stale. Analyze again.");
    }

    private JPanel historyPanel() {
        JPanel p = borderPanel(0);
        history.getAccessibleContext().setAccessibleName("Recorded instructions");
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        gui.styleTable(history);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, history.getFont().getSize()));
        int[] widths = {110, 80, 250, 300};
        for (int column = 0; column < widths.length; column++)
            history.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        JPanel tools = gui.panel("insets 4, fillx, hidemode 3", "[grow,fill]", "");
        JPanel primary = gui.panel("insets 0, fillx, hidemode 3", "[][grow,fill][][][]", "");
        historyOrigin.getAccessibleContext().setAccessibleName("History source");
        historyOrigin.addActionListener(event -> {
            if (updatingHistoryOrigin) return;
            guard(() -> {
                if (historyOrigin.getSelectedIndex() == 0) {
                    openedRecording = null;
                    openedHeader = null;
                    historyPageStarts.clear();
                    refreshHistory();
                } else if (availableRecording != null) {
                    updateHistoryOrigin();
                    openRecording(availableRecording);
                }
            });
        });
        recordingControl = toggleButton(ICON_RECORD, "Record recent instructions");
        recordingControl.addActionListener(e -> guard(this::toggleRecentRecording));
        recentLimit.addChangeListener(e -> guard(() -> {
            if (session.isRecording()) session.record(((Number) recentLimit.getValue()).intValue());
            refreshHistory();
        }));
        fileRecordingControl = historyButton("Stop file recording", ICON_STOP, () ->
                runHistoryAnalysis(false, () -> { session.stopRecordingFile(); return true; }, ignored -> refreshHistory()));
        startFileRecording = historyButton("Record live execution to file…", ICON_RECORD, () -> {
            Path file = choose(true, false);
            if (file != null) {
                requireNewHistoryDestination(file);
                runHistoryAnalysis(false, () -> { session.recordTo(file); return true; }, ignored -> refreshHistory());
            }
        });
        saveRecentControl = historyButton("Save recent execution…", ICON_SAVE, () -> saveHistory(false));
        exportResultsControl = historyButton("Export shown results as text…", ICON_COPY, () -> saveHistory(true));
        exportRecordingControl = historyButton("Export entire recording as text…", ICON_COPY, () -> {
            if (openedRecording == null) return;
            Path file = choose(true, false);
            if (file == null) return;
            requireNewHistoryDestination(file);
            Path source = openedRecording;
            runHistoryAnalysis(true, () -> { InstructionRecording.exportText(file, source); return true; },
                    ignored -> historyContext.setText("Exported entire recording to " + file.getFileName()));
        });
        firstHistoryPage = historyButton("First page", ICON_UNDO, () -> {
            if (openedRecording != null) loadHistoryPage(openedRecording, null, 1, appliedHistoryFilter);
        });
        previousHistoryPage = historyButton("Previous page", ICON_STEP_BACK, () -> {
            if (openedRecording != null && historyPage > 1)
                loadHistoryPage(openedRecording, historyPageStarts.get((int) historyPage - 2),
                        historyPage - 1, appliedHistoryFilter);
        });
        nextHistoryPage = historyButton("Next page", ICON_STEP, () -> {
            if (openedRecording != null && !openedFinished)
                loadHistoryPage(openedRecording, nextHistoryCursor, historyPage + 1, appliedHistoryFilter);
        });
        JButton open = historyButton("Open recording…", ICON_OPEN_FILE, () -> {
            Path file = choose(false, false);
            if (file != null) openRecording(file);
        });
        JToggleButton filter = toggleButton(ICON_FIND, "History filters");
        filter.setText("Filters");
        JToggleButton options = toggleButton(ICON_SETTINGS, "History options");
        options.setText("Options");
        primary.add(label("Source", historyOrigin));
        historyOrigin.getAccessibleContext().setAccessibleName("History source");
        primary.add(historyOrigin);
        primary.add(recordingControl);
        primary.add(open);
        primary.add(options);
        tools.add(primary, "growx, wrap");
        JPanel status = gui.panel("insets 0, fillx, hidemode 3", "[grow,fill][]", "");
        historyCaptureStatus.getAccessibleContext().setAccessibleName("Capture status");
        status.add(historyCaptureStatus);
        status.add(fileRecordingControl);
        tools.add(status, "growx, wrap");
        historyWorkControls = gui.panel("insets 0, fillx", "[grow,fill][]", "");
        historyProgress.setIndeterminate(true);
        historyProgress.setStringPainted(true);
        historyProgress.setString("Working with recording…");
        historyProgress.getAccessibleContext().setAccessibleName("Recording operation in progress");
        historyWorkControls.add(historyProgress);
        // Cancellation remains available while all ordinary analysis controls are disabled.
        cancelHistoryWork = gui.toolbarButton(event -> {
            historyCancelRequested = true;
            cancelHistoryWork.setEnabled(false);
            tasks.cancel();
        }, ICON_STOP, "Cancel");
        cancelHistoryWork.setText("Cancel");
        cancelHistoryWork.setFocusable(true);
        cancelHistoryWork.getAccessibleContext().setAccessibleName("Cancel recording operation");
        historyWorkControls.add(cancelHistoryWork);
        historyWorkControls.setVisible(false);
        tools.add(historyWorkControls, "growx, wrap");
        JPanel filters = gui.panel("insets 4, fillx", "[][grow,fill][][grow,fill]", "");
        filters.add(label("PC from", historyFrom)); filters.add(historyFrom);
        filters.add(label("PC through", historyThrough)); filters.add(historyThrough, "wrap");
        filters.add(label("Changed register", historyRegister)); filters.add(historyRegister, "wrap");
        filters.add(label("Memory address", historyMemory)); filters.add(historyMemory);
        filters.add(label("Memory bank", historyMemoryBank)); filters.add(historyMemoryBank, "wrap");
        filters.add(label("Memory access", historyAccess)); filters.add(historyAccess);
        filters.add(historyButton("Apply filters", ICON_FIND, this::applyHistoryFilters), "span 2, wrap");
        filters.add(gui.label("Addresses: decimal or 0x-prefixed hexadecimal. Blank fields match any value."), "span, wrap");
        filters.setVisible(false);
        JPanel filterStatus = gui.panel("insets 0, fillx, hidemode 3", "[][grow,fill][]", "");
        filterStatus.add(filter);
        historyFilterSummary.getAccessibleContext().setAccessibleName("Active history filters");
        filterStatus.add(historyFilterSummary);
        historyClearControl = historyButton("Clear filters", ICON_UNDO, () -> { clearHistoryFilters(); applyHistoryFilters(); });
        filterStatus.add(historyClearControl);
        tools.add(filterStatus, "growx, wrap");
        tools.add(filters, "growx, wrap");
        JPanel extra = gui.panel("insets 4, fillx, hidemode 3", "[][110!,fill][grow]", "");
        extra.add(label("Keep last", recentLimit)); extra.add(recentLimit);
        extra.add(gui.label("instructions"), "wrap");
        recentLimit.setToolTipText("Maximum recent instructions retained in memory; file recordings keep every instruction.");
        extra.add(foldHistory, "span, wrap");
        extra.add(startFileRecording, "span, growx, wrap");
        extra.add(saveRecentControl, "span, growx, wrap");
        extra.add(exportResultsControl, "span, growx, wrap");
        extra.add(exportRecordingControl, "span, growx, wrap");
        extra.setVisible(false);
        options.addActionListener(event -> {
            extra.setVisible(options.isSelected());
            if (options.isSelected()) { filter.setSelected(false); filters.setVisible(false); }
            tools.revalidate();
        });
        filter.addActionListener(event -> {
            filters.setVisible(filter.isSelected());
            if (filter.isSelected()) { options.setSelected(false); extra.setVisible(false); }
            tools.revalidate();
        });
        tools.add(extra, "growx, wrap");
        for (JTextField field : List.of(historyFrom, historyThrough, historyRegister, historyMemory, historyMemoryBank)) {
            field.setToolTipText(field == historyRegister ? "Register name; only instructions changing this register"
                    : "Optional address or bank: decimal or 0x-prefixed hexadecimal");
            field.addActionListener(e -> guard(this::applyHistoryFilters));
        }
        historyAccess.getAccessibleContext().setAccessibleName("Memory access kind");
        showUnchangedRegisters.addActionListener(e -> showHistoricalState(history.getSelectedRow()));
        foldHistory.addActionListener(e -> guard(this::refreshHistory));
        historyContext.getAccessibleContext().setAccessibleName("Recording status");
        historyContext.setToolTipText("Saved recordings are searched across the whole file. Recent execution is limited to retained instructions.");
        tools.add(historyContext, "span, growx, wrap");
        historyPageControls = gui.panel("insets 0, fillx, hidemode 3", "[][][]", "");
        historyPageControls.add(firstHistoryPage);
        historyPageControls.add(previousHistoryPage);
        historyPageControls.add(nextHistoryPage);
        tools.add(historyPageControls, "growx, wrap");
        historicalState.setEditable(false);
        historicalState.setFont(new Font(Font.MONOSPACED, Font.PLAIN, historicalState.getFont().getSize()));
        historicalState.getAccessibleContext().setAccessibleName("Recorded instruction state");
        history.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showHistoricalState(history.getSelectedRow());
        });
        p.add(scrollTools(tools), BorderLayout.NORTH);
        JPanel detail = borderPanel(4);
        JPanel detailTools = gui.panel("insets 4, fillx", "[grow,fill][]", "");
        detailTools.add(gui.label("Recorded state · read-only"));
        detailTools.add(showUnchangedRegisters);
        detail.add(detailTools, BorderLayout.NORTH);
        detail.add(gui.scrollPane(historicalState));
        JButton earlier = historyButton("Find earlier write", ICON_FIND, this::previousRecordedWrite);
        earlier.setToolTipText("Find an earlier write to the selected memory access, or the memory address in Filters");
        earlier.getAccessibleContext().setAccessibleName("Find earlier write");
        JPanel memoryActions = gui.panel("insets 4, fillx", "[][grow,fill][]", "");
        memoryActions.add(label("Memory access", historyWriteTarget));
        memoryActions.add(historyWriteTarget);
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
        historyEmptyRecord = historyButton("Start recording", ICON_RECORD, this::toggleRecentRecording);
        empty.add(historyEmptyRecord, "wrap");
        historyEmptyClear = historyButton("Clear filters", ICON_UNDO, () -> { clearHistoryFilters(); applyHistoryFilters(); });
        empty.add(historyEmptyClear, "wrap");
        empty.add(historyButton("Open recording…", ICON_OPEN_FILE, () -> {
            Path file = choose(false, false);
            if (file != null) openRecording(file);
        }), "wrap");
        historyBody.add(empty, "empty");
        p.add(historyBody);
        updateRecordingControls(true);
        return p;
    }

    private JButton historyButton(String text, String icon, Operation action) {
        JButton button = button(icon, text, action);
        button.setText(text);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        return button;
    }

    private <T> void runHistoryAnalysis(boolean cancellable, Callable<T> operation, Consumer<T> apply) {
        historyAnalysis = true;
        historyCanCancel = cancellable;
        historyCancelRequested = false;
        try {
            runAnalysis(operation, result -> {
                if (historyCancelRequested) historyContext.setText("Recording operation cancelled. Any output file may be partial.");
                else apply.accept(result);
            });
        }
        catch (RuntimeException failure) { historyAnalysis = false; throw failure; }
    }

    private void toggleRecentRecording() {
        session.record(session.isRecording() ? 0 : ((Number) recentLimit.getValue()).intValue());
        refreshHistory();
    }

    private void updateHistoryOrigin() {
        updatingHistoryOrigin = true;
        try {
            historyOrigin.removeAllItems();
            historyOrigin.addItem("Recent execution");
            if (availableRecording != null) historyOrigin.addItem("Recording: " + availableRecording.getFileName());
            historyOrigin.setSelectedIndex(openedRecording == null ? 0 : 1);
        } finally { updatingHistoryOrigin = false; }
    }

    private void updateRecordingControls(boolean paused) {
        if (recordingControl == null) return;
        recordingControl.setSelected(session.isRecording());
        recordingControl.setText(session.isRecording() ? "Stop recording" : "Start recording");
        recordingControl.getAccessibleContext().setAccessibleName(recordingControl.getText());
        recordingControl.setVisible(openedRecording == null);
        recordingControl.setEnabled(session.debug != null && paused && openedRecording == null);
        historyEmptyRecord.setEnabled(session.debug != null && paused);
        recentLimit.setEnabled(session.debug != null && paused);
        startFileRecording.setEnabled(session.debug != null && paused && session.recordingPath() == null);
        fileRecordingControl.setEnabled(paused);
        boolean saving = session.recordingPath() != null;
        fileRecordingControl.setVisible(saving);
        String capture = session.debug == null ? "Live recording unavailable for this CPU"
                : session.isRecording() ? "Recording recent execution" : "Recent recording off";
        historyCaptureStatus.setText(capture + " · keeps last " + recentLimit.getValue() + " instructions"
                + (saving ? " · Writing " + session.recordingPath().getFileName() : ""));
        historyCaptureStatus.setToolTipText(saving ? session.recordingPath().toString() : null);
        firstHistoryPage.setEnabled(openedRecording != null && historyPage > 1 && paused);
        previousHistoryPage.setEnabled(openedRecording != null && historyPage > 1 && paused);
        historyPageControls.setVisible(openedRecording != null);
        exportRecordingControl.setEnabled(openedRecording != null && paused);
        exportRecordingControl.setVisible(openedRecording != null);
        saveRecentControl.setEnabled(openedRecording == null && !session.history().isEmpty() && paused);
        saveRecentControl.setVisible(openedRecording == null);
        exportResultsControl.setEnabled(!displayedHistory.isEmpty() && paused);
        exportResultsControl.setText(openedRecording == null ? "Export shown results as text…" : "Export this results page as text…");
        exportResultsControl.setToolTipText(exportResultsControl.getText());
        exportResultsControl.getAccessibleContext().setAccessibleName(exportResultsControl.getText());
        historyOrigin.setEnabled(paused);
        nextHistoryPage.setEnabled(openedRecording != null && !openedFinished && paused);
    }

    private void requireNewHistoryDestination(Path file) {
        Path destination = file.toAbsolutePath().normalize();
        if (Files.exists(destination)) throw new IllegalArgumentException("Choose a new file; recordings and exports do not overwrite existing files");
        if ((openedRecording != null && destination.equals(openedRecording.toAbsolutePath().normalize()))
                || (session.recordingPath() != null
                && destination.equals(session.recordingPath().toAbsolutePath().normalize())))
            throw new IllegalArgumentException("Choose a different file from the recording being read or written");
    }

    private void saveHistory(boolean text) {
        Path file = choose(true, false);
        if (file == null) return;
        saveHistory(file, text);
    }

    void saveHistory(Path file, boolean text) {
        session.requirePaused();
        if (!text && openedRecording != null)
            throw new IllegalStateException("Select Recent execution to save its recording");
        requireNewHistoryDestination(file);
        InstructionRecording.Header header = text && openedHeader != null ? openedHeader : session.recordingHeader();
        List<DebugCPU.Instruction> events = text ? List.copyOf(displayedHistory) : session.history();
        runHistoryAnalysis(false, () -> {
            if (text) InstructionRecording.exportText(file, header, events);
            else InstructionRecording.write(file, header, events);
            return true;
        }, ignored -> historyContext.setText("Saved " + events.size() + " instructions to " + file.getFileName()));
    }

    // Kept package-visible for the same offline path exercised by the Open recording action.
    void openRecording(Path file) {
        loadHistoryPage(file, null, 1, appliedHistoryFilter);
    }

    private void loadHistoryPage(Path file, InstructionRecording.Reader.Cursor cursor, long page, HistoryFilter filter) {
        runHistoryAnalysis(true, () -> {
            try (InstructionRecording.Reader reader = new InstructionRecording.Reader(file)) {
                if (cursor != null) reader.seek(cursor);
                List<DebugCPU.Instruction> events = reader.nextPage(1024, filter);
                return new HistoryPage(file, reader.header(), events, reader.cursor(), page,
                        reader.finished(), reader.status());
            }
        }, loaded -> {
            if (page == 1) historyPageStarts.clear();
            while (historyPageStarts.size() > page - 1) historyPageStarts.remove(historyPageStarts.size() - 1);
            historyPageStarts.add(cursor);
            appliedHistoryFilter = filter;
            showHistoryPage(loaded);
        });
    }

    private void showHistoryPage(HistoryPage loaded) {
        openedRecording = availableRecording = loaded.file;
        openedHeader = loaded.header;
        historySource = loaded.events;
        nextHistoryCursor = loaded.cursor;
        historyPage = loaded.page;
        openedFinished = loaded.finished;
        openedStatus = loaded.status;
        history.clearSelection();
        updateHistoryOrigin();
        refreshHistory();
    }

    private static final class HistoryPage {
        final Path file;
        final InstructionRecording.Header header;
        final List<DebugCPU.Instruction> events;
        final InstructionRecording.Reader.Cursor cursor;
        final long page;
        final boolean finished;
        final String status;
        HistoryPage(Path file, InstructionRecording.Header header, List<DebugCPU.Instruction> events,
                    InstructionRecording.Reader.Cursor cursor, long page, boolean finished, String status) {
            this.file = file;
            this.header = header;
            this.events = events;
            this.cursor = cursor;
            this.page = page;
            this.finished = finished;
            this.status = status;
        }
    }

    private static Integer historyNumber(JTextField field) {
        String value = field.getText().strip();
        if (value.isEmpty()) return null;
        try {
            int number = value.startsWith("0x") || value.startsWith("0X")
                    ? Integer.parseInt(value.substring(2), 16) : Integer.parseInt(value);
            if (number < 0) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Invalid " + field.getAccessibleContext().getAccessibleName()
                    + ": use decimal or 0x-prefixed hexadecimal");
        }
    }

    private void clearHistoryFilters() {
        for (JTextField field : List.of(historyFrom, historyThrough, historyRegister, historyMemory, historyMemoryBank))
            field.setText("");
        historyAccess.setSelectedIndex(0);
    }

    private HistoryFilter historyFilter() {
        return new HistoryFilter(historyNumber(historyFrom), historyNumber(historyThrough),
                historyRegister.getText().strip().toUpperCase(Locale.ROOT), historyNumber(historyMemory),
                historyNumber(historyMemoryBank), historyAccess.getSelectedIndex());
    }

    private void applyHistoryFilters() {
        HistoryFilter filter = historyFilter();
        if (openedRecording != null) loadHistoryPage(openedRecording, null, 1, filter);
        else {
            appliedHistoryFilter = filter;
            refreshHistory();
        }
    }

    private static final class HistoryFilter implements java.util.function.Predicate<DebugCPU.Instruction> {
        final Integer from, through, memory, bank;
        final String register;
        final int access;

        static HistoryFilter empty() { return new HistoryFilter(null, null, "", null, null, 0); }

        HistoryFilter(Integer from, Integer through, String register, Integer memory, Integer bank, int access) {
            if (from != null && through != null && from > through)
                throw new IllegalArgumentException("PC from must not exceed PC through");
            this.from = from;
            this.through = through;
            this.register = register;
            this.memory = memory;
            this.bank = bank;
            this.access = access;
        }

        @Override public boolean test(DebugCPU.Instruction event) {
            if ((from != null && event.address < from) || (through != null && event.address > through)) return false;
            if (!register.isEmpty() && (!event.before.containsKey(register)
                    || Objects.equals(event.before.get(register), event.after.get(register)))) return false;
            return (memory == null && bank == null && access == 0)
                    || event.accesses.stream().anyMatch(a -> (memory == null || a.address == memory)
                    && (bank == null || a.bank == bank) && (access == 0 || a.write == (access == 2)));
        }

        String summary() {
            List<String> parts = new ArrayList<>();
            if (from != null) parts.add(String.format("PC ≥ %04X", from));
            if (through != null) parts.add(String.format("PC ≤ %04X", through));
            if (!register.isEmpty()) parts.add(register + " changed");
            if (memory != null) parts.add(String.format("Memory %04X", memory));
            if (bank != null) parts.add("Bank " + bank);
            if (access != 0) parts.add(access == 1 ? "Read" : "Write");
            return String.join(" · ", parts);
        }
    }

    private void previousRecordedWrite() {
        Integer address = historyNumber(historyMemory), bank = historyNumber(historyMemoryBank);
        int access = historyWriteTarget.getSelectedIndex();
        if (access >= 0 && access < selectedHistoryAccesses.size()) {
            address = selectedHistoryAccesses.get(access).address;
            bank = selectedHistoryAccesses.get(access).bank;
        }
        if (address == null) throw new IllegalArgumentException("Enter a Memory address to find its previous recorded write");
        int selected = history.getSelectedRow();
        long beforeSequence = selected < 0 ? Long.MAX_VALUE : displayedHistory.get(historyGroups.get(selected)[0]).sequence;
        HistoryFilter filter = new HistoryFilter(null, null, "", address, bank, 2);
        if (openedRecording != null) {
            Path file = openedRecording;
            runHistoryAnalysis(true, () -> {
                try (InstructionRecording.Reader reader = new InstructionRecording.Reader(file)) {
                    HistoryPage found = null;
                    long sequence = -1;
                    List<InstructionRecording.Reader.Cursor> starts = new ArrayList<>(), foundStarts = List.of();
                    while (!reader.finished()) {
                        starts.add(reader.cursor());
                        List<DebugCPU.Instruction> events = reader.nextPage(1024, filter);
                        long candidate = sequenceBefore(events, beforeSequence);
                        if (candidate >= 0) {
                            sequence = candidate;
                            found = new HistoryPage(file, reader.header(), events, reader.cursor(), starts.size(),
                                    reader.finished(), reader.status());
                            foundStarts = new ArrayList<>(starts);
                        }
                        if (!events.isEmpty() && events.get(events.size() - 1).sequence >= beforeSequence) break;
                    }
                    return new EarlierWrite(found, foundStarts, sequence);
                }
            }, found -> {
                if (found.page == null) {
                    historyContext.setText("No earlier write in this recording for the selected address and bank.");
                    return;
                }
                prepareEarlierWrite(filter);
                historyPageStarts.clear();
                historyPageStarts.addAll(found.starts);
                showHistoryPage(found.page);
                selectHistorySequence(found.sequence);
            });
            return;
        }
        DebugCPU.Instruction found = null;
        for (int i = historySource.size() - 1; i >= 0; i--) {
            DebugCPU.Instruction event = historySource.get(i);
            if (event.sequence < beforeSequence && filter.test(event)) {
                found = event;
                break;
            }
        }
        if (found == null) {
            historyContext.setText("No earlier write in retained recent execution. Older instructions may have been discarded.");
            return;
        }
        prepareEarlierWrite(filter);
        refreshHistory();
        selectHistorySequence(found.sequence);
    }

    private static long sequenceBefore(List<DebugCPU.Instruction> events, long before) {
        long result = -1;
        for (DebugCPU.Instruction event : events) if (event.sequence < before) result = event.sequence;
        return result;
    }

    private static final class EarlierWrite {
        final HistoryPage page;
        final List<InstructionRecording.Reader.Cursor> starts;
        final long sequence;
        EarlierWrite(HistoryPage page, List<InstructionRecording.Reader.Cursor> starts, long sequence) {
            this.page = page;
            this.starts = starts;
            this.sequence = sequence;
        }
    }

    private void prepareEarlierWrite(HistoryFilter filter) {
        historyFrom.setText("");
        historyThrough.setText("");
        historyRegister.setText("");
        historyMemory.setText(String.format("0x%X", filter.memory));
        historyMemoryBank.setText(filter.bank == null ? "" : Integer.toString(filter.bank));
        foldHistory.setSelected(false);
        historyAccess.setSelectedIndex(2);
        appliedHistoryFilter = filter;
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
        int selected = history.getSelectedRow();
        DebugCPU.Instruction selectedEvent = selected >= 0 && selected < historyGroups.size()
                ? displayedHistory.get(historyGroups.get(selected)[0]) : null;
        if (openedRecording == null) historySource = session.history();
        displayedHistory = new ArrayList<>();
        for (DebugCPU.Instruction event : historySource)
            if (appliedHistoryFilter.test(event)) displayedHistory.add(event);
        historyGroups.clear();
        for (int i = 0; i < displayedHistory.size(); i++) {
            int last = i;
            while (foldHistory.isSelected() && last + 1 < displayedHistory.size()
                    && sameRecordedInstruction(displayedHistory.get(last), displayedHistory.get(last + 1))) last++;
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
        String summary = appliedHistoryFilter.summary();
        String scope = openedRecording == null ? "retained recent execution" : "whole recording";
        historyFilterSummary.setText((summary.isEmpty() ? "No filters" : summary) + " · " + scope);
        historyFilterSummary.setToolTipText(historyFilterSummary.getText());
        historyClearControl.setVisible(!summary.isEmpty());
        historyContext.setText(openedRecording == null
                ? displayedHistory.size() + " / " + historySource.size() + " retained instructions"
                : openedHeader.processor + " · " + displayedHistory.size() + " instructions · results page " + historyPage
                        + " · " + (openedStatus.startsWith("Complete") ? "Complete recording"
                        : openedStatus.startsWith("Incomplete") ? "Incomplete recording" : "Completeness not yet verified"));
        historyContext.setToolTipText(openedRecording == null ? "Only retained recent instructions are available."
                : openedRecording + " · " + openedStatus);
        if (displayedHistory.isEmpty()) {
            selectedHistoryAccesses = List.of();
            historyWriteTarget.removeAllItems();
            boolean filtered = !summary.isEmpty();
            historyEmptyTitle.setText(filtered ? "No instructions match these filters"
                    : openedRecording != null ? "This recording contains no instructions"
                    : session.debug == null ? "Live recording unavailable for this CPU"
                    : session.isRecording() ? "Ready to capture instructions" : "No instructions recorded");
            historyEmptyHint.setText(filtered ? "Searched " + scope + ". Clear or change the filters."
                    : openedRecording != null ? openedStatus
                    : session.debug == null ? "Open a saved recording to inspect captured instructions."
                    : session.isRecording() ? "Step or run your program, then pause to inspect its instructions."
                    : "Start recording, then step or run your program.");
            historyEmptyRecord.setVisible(!filtered && openedRecording == null && session.debug != null && !session.isRecording());
            historyEmptyClear.setVisible(filtered);
            historicalState.setText("");
            historicalState.setCaretPosition(0);
        } else if (history.getSelectedRow() < 0) {
            historicalState.setText("Select an instruction to inspect its captured registers and memory accesses.");
            historicalState.setCaretPosition(0);
        }
        ((CardLayout) historyBody.getLayout()).show(historyBody, displayedHistory.isEmpty() ? "empty" : "results");
        updateRecordingControls(!running && !tasks.isBusy());
        if (!session.recordingError().isBlank()) historyCaptureStatus.setText("Recording error: " + session.recordingError());
    }

    private static boolean sameRecordedInstruction(DebugCPU.Instruction first, DebugCPU.Instruction second) {
        if (second.sequence != first.sequence + 1 || first.address != second.address || first.bank != second.bank
                || first.completed != second.completed || !first.mnemonic.equals(second.mnemonic)
                || !first.opcodes.equals(second.opcodes) || !first.before.equals(second.before)
                || !first.after.equals(second.after) || first.accesses.size() != second.accesses.size()) return false;
        for (int i = 0; i < first.accesses.size(); i++) {
            DebugCPU.Access a = first.accesses.get(i), b = second.accesses.get(i);
            if (a.address != b.address || a.bank != b.bank || a.value != b.value || a.write != b.write) return false;
        }
        return true;
    }

    private final class HistoryModel extends AbstractTableModel {
        private final String[] columns = {"Sequence", "PC", "Instruction", "Changes"};
        @Override public int getRowCount() { return historyGroups.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            int[] group = historyGroups.get(row);
            DebugCPU.Instruction event = displayedHistory.get(group[0]), last = displayedHistory.get(group[1]);
            switch (column) {
                case 0: return group[0] == group[1] ? Long.toString(event.sequence)
                        : event.sequence + "–" + last.sequence + " (" + (group[1] - group[0] + 1) + ")";
                case 1: return String.format("%d:%04X", event.bank, event.address);
                case 2: return event.mnemonic + (event.completed ? "" : " (failed)");
                default: {
                    List<String> values = new ArrayList<>();
                    for (String name : new TreeSet<>(event.before.keySet()))
                        if (!Objects.equals(event.before.get(name), event.after.get(name)))
                            values.add(name + "=" + String.format("%X→%X", event.before.get(name), event.after.getOrDefault(name, 0L)));
                    for (DebugCPU.Access access : event.accesses)
                        if (access.write) values.add(String.format("%d:%04X=%02X", access.bank, access.address, access.value));
                    return String.join(" ", values);
                }
            }
        }
    }

    private void showHistoricalState(int selected) {
        if (selected < 0 || selected >= historyGroups.size()) {
            selectedHistoryAccesses = List.of();
            historyWriteTarget.removeAllItems();
            historyWriteTarget.setEnabled(false);
            return;
        }
        int[] group = historyGroups.get(selected);
        DebugCPU.Instruction event = displayedHistory.get(group[0]);
        selectedHistoryAccesses = event.accesses;
        historyWriteTarget.removeAllItems();
        for (DebugCPU.Access access : selectedHistoryAccesses)
            historyWriteTarget.addItem(String.format("%s bank %d:%04X = %02X", access.write ? "WRITE" : "READ",
                    access.bank, access.address, access.value));
        historyWriteTarget.setEnabled(!selectedHistoryAccesses.isEmpty());
        StringBuilder text = new StringBuilder(String.format("Sequence %d · bank %d:%04X · %s · %s%s\n",
                event.sequence, event.bank, event.address, event.opcodes, event.mnemonic, event.completed ? "" : " (failed)"));
        if (group[0] != group[1]) text.append("Identical recorded executions through sequence ")
                .append(displayedHistory.get(group[1]).sequence).append("; unfold to inspect each event.\n");
        text.append("\nChanged registers\n");
        List<String> names = new ArrayList<>(event.before.keySet());
        names.sort(String::compareTo);
        boolean changed = false;
        for (String name : names) {
            long before = event.before.get(name), after = event.after.getOrDefault(name, before);
            if (before != after) {
                text.append(String.format("%-5s %X → %X\n", name, before, after));
                changed = true;
            }
        }
        if (!changed) text.append("None\n");
        if (showUnchangedRegisters.isSelected()) {
            text.append("\nUnchanged registers\n");
            for (String name : names) {
                long before = event.before.get(name);
                if (before == event.after.getOrDefault(name, before))
                    text.append(String.format("%-5s %X\n", name, before));
            }
        }
        text.append("\nRecorded memory accesses\n");
        if (event.accesses.isEmpty()) text.append("None\n");
        for (DebugCPU.Access access : event.accesses)
            text.append(String.format("%s bank %d:%04X = %02X\n", access.write ? "WRITE" : "READ",
                    access.bank, access.address, access.value));
        historicalState.setText(text.toString());
        historicalState.setCaretPosition(0);
    }

    private void showSource(int at) {
        if (session.memory == null || session.memory.annotations() == null) return;
        if ((at < session.memory.getCommonBoundary() ? selectedBank() : 0)
                != session.memory.getBank(at))
            throw new IllegalStateException(
                    "Source mapping belongs to CPU's currently selected bank");
        Set<SourceCodeAnnotation> source =
                session.memory.annotations().get(at, SourceCodeAnnotation.class);
        if (source.isEmpty())
            throw new IllegalStateException("No current source mapping at this address");
        sourceNavigator.accept(
                source.stream()
                        .min(Comparator.comparingLong(SourceCodeAnnotation::getPluginId))
                        .get()
                        .getPosition());
    }

    private void refreshAnnotations(String filter) {
        AnnotationIndex.Entry selected = annotations.getSelectedValue();
        List<AnnotationIndex.Entry> entries = index == null ? List.of() : index.search(filter);
        if (bookmarksOnly.isSelected()) {
            entries = new ArrayList<>(entries);
            entries.removeIf(entry -> !entry.bookmark);
        }
        boolean wasUpdating = updatingAnnotations;
        updatingAnnotations = true;
        try {
            updateRows(annotationRows, entries);
            annotations.clearSelection();
            if (selected != null) {
                for (int i = 0; i < annotationRows.size(); i++) {
                    AnnotationIndex.Entry entry = annotationRows.get(i);
                    if (entry.bank == selected.bank && entry.address == selected.address) {
                        annotations.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            updatingAnnotations = wasUpdating;
        }
        annotationEmpty.setText(index == null || index.entries().isEmpty()
                ? "<html>No annotations yet.<br>Select an address in Debugger or Memory, choose New, then Save.</html>"
                : "<html>No matching annotations.<br>Change Search annotations or Bookmarks only.</html>");
        ((CardLayout) annotationList.getLayout()).show(annotationList, entries.isEmpty() ? "empty" : "list");
    }

    private AnnotationIndex.Entry annotationAt(int at, int bank) {
        return index != null && image != null && at >= 0 && at < image.size()
                && bank >= 0 && bank < image.bankCount() ? index.get(bank, at) : null;
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
        private final BiFunction<T, Integer, ListDetail> describe;

        DetailRenderer(BiFunction<T, Integer, ListDetail> describe) {
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
            ListDetail row = describe.apply(value, index);
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
            if (image == null && tasks.isBusy()) initializationPending = true;
            refresh();
        });
    }

    private void refresh() {
        if (disposed || tasks.isBusy()) return;
        try { session.requirePaused(); } catch (IllegalStateException e) { return; }
        updateRecordingControls(true);
        refreshWatches();
        refreshMemoryWatches();
        refreshBreakpoints();
        try { refreshHistory(); } catch (IllegalArgumentException failure) {
            historyContext.setText(failure.getMessage());
        }
        navigationToolbar.setToolTipText(session.status.isBlank() ? null : session.status);
        model.refreshData();
        refreshAnnotations(annotationFilter.getText());
    }

    private void refreshWatches() {
        int watchSelection = watches.getSelectedIndex();
        String selectedExpression = watchSelection >= 0 && watchSelection < displayedWatches.size()
                ? displayedWatches.get(watchSelection) : null;
        List<String> watchText = new ArrayList<>();
        displayedWatches = List.copyOf(session.watches);
        for (String expression : displayedWatches) {
            try {
                long value = session.evaluate(expression, selectedBank());
                Long old = watchValues.put(expression, value);
                if (old != null && old != value) changedWatches.add(expression);
                String formatted = formatWatchValue(value);
                watchText.add(expression + " = " + formatted + (changedWatches.contains(expression) ? "  * changed" : ""));
            } catch (RuntimeException e) {
                watchText.add(expression + " — " + e.getMessage());
            }
        }
        updateRows(watchRows, watchText);
        if (selectedExpression != null) watches.setSelectedIndex(displayedWatches.indexOf(selectedExpression));
    }

    private String formatWatchValue(long value) {
        switch (watchFormat.getSelectedIndex()) {
            case 1: return Long.toBinaryString(value);
            case 2: return "'" + (value >= 32 && value <= 126 ? (char) value : '.') + "'";
            default: return String.format("0x%X", value) + " / " + value;
        }
    }

    private void refreshMemoryWatches() {
        int memorySelection = memoryWatches.getSelectedIndex();
        DebugSession.AccessRule selectedMemoryWatch = memorySelection >= 0 && memorySelection < displayedMemoryWatches.size()
                ? displayedMemoryWatches.get(memorySelection) : null;
        displayedMemoryWatches = new ArrayList<>();
        List<String> memoryWatchText = new ArrayList<>();
        for (DebugSession.Rule rule : session.rules) {
            if (!(rule instanceof DebugSession.AccessRule)) continue;
            DebugSession.AccessRule watch = (DebugSession.AccessRule) rule;
            displayedMemoryWatches.add(watch);
            memoryWatchText.add((rule.enabled ? "Enabled " : "Disabled ") + rule.name + " · "
                    + (rule.kind.equals("READ") ? "Pause on reads" : "Pause on writes")
                    + String.format(" · %04X · ", rule.address) + memoryWatchOptions(watch));
        }
        updateRows(memoryWatchRows, memoryWatchText);
        if (selectedMemoryWatch != null) memoryWatches.setSelectedIndex(displayedMemoryWatches.indexOf(selectedMemoryWatch));
    }

    private void refreshBreakpoints() {
        int ruleSelection = rules.getSelectedIndex();
        DebugSession.Rule selectedRule = ruleSelection >= 0 && ruleSelection < displayedRules.size()
                ? displayedRules.get(ruleSelection) : null;
        Integer selectedOrdinary = ruleSelection >= displayedRules.size()
                && ruleSelection < displayedRules.size() + ordinaryBreakpoints.size()
                ? ordinaryBreakpoints.get(ruleSelection - displayedRules.size()) : null;
        displayedRules = new ArrayList<>();
        for (DebugSession.Rule rule : session.rules)
            if (rule.kind.equals("EXEC") || rule.kind.equals("SOURCE")) displayedRules.add(rule);
        List<String> ruleText = new ArrayList<>();
        for (DebugSession.Rule rule : displayedRules)
            ruleText.add((rule.enabled ? "Enabled " : "Disabled ") + rule.name + " · "
                    + (rule.kind.equals("SOURCE") ? rule.source + ":" + rule.address : String.format("%04X", rule.address))
                    + " hits=" + rule.hits
                    + (rule.condition.isBlank() ? "" : " if " + rule.condition));
        ordinaryBreakpoints = session.ordinaryBreakpoints();
        for (int ordinary : ordinaryBreakpoints)
            ruleText.add(String.format("%s breakpoint: %04X",
                    session.isBreakpointSet(ordinary) ? "Enabled" : "Disabled", ordinary));
        updateRows(ruleRows, ruleText);
        if (selectedRule != null) rules.setSelectedIndex(displayedRules.indexOf(selectedRule));
        else if (selectedOrdinary != null) {
            int i = ordinaryBreakpoints.indexOf(selectedOrdinary);
            rules.setSelectedIndex(i < 0 ? -1 : displayedRules.size() + i);
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
        private int baseColumn(int col) { return col - (addedBreakpointColumn ? 1 : 0); }
        private int baseCount() { return instructionModel.getColumnCount() + (addedBreakpointColumn ? 1 : 0); }
        @Override public int getRowCount() { return instructionModel.getRowCount(); }
        @Override public int getColumnCount() { return baseCount() + (byteMemory ? 2 : 0); }
        @Override public String getColumnName(int col) {
            if (col == breakpointColumn) return "bp";
            if (col < baseCount()) return instructionModel.getColumnName(baseColumn(col));
            return col == baseCount() ? "Label" : "Comment";
        }
        @Override public Class<?> getColumnClass(int col) {
            if (col == breakpointColumn) return Boolean.class;
            return col < baseCount() ? instructionModel.getColumnClass(baseColumn(col)) : String.class;
        }
        @Override public Object getValueAt(int row, int col) {
            if (disposed || tasks.isBusy() || running) return null;
            if (col == breakpointColumn) {
                int address = instructionModel.getLocationAt(row);
                return address < 0 ? null : session.isBreakpointSet(address);
            }
            if (col < baseCount()) return instructionModel.getValueAt(row, baseColumn(col));
            int at = instructionModel.getLocationAt(row);
            AnnotationIndex.Entry entry = annotationAt(at, instructionBank());
            if (entry == null) return "";
            return col == baseCount() ? entry.label : entry.comment;
        }
        @Override public boolean isCellEditable(int row, int col) {
            if (disposed || tasks.isBusy() || running) return false;
            if (col == breakpointColumn) return instructionModel.getLocationAt(row) >= 0;
            return col < baseCount() && instructionModel.isCellEditable(row, baseColumn(col));
        }
        @Override public void setValueAt(Object value, int row, int col) {
            if (col == breakpointColumn && value instanceof Boolean) {
                int address = instructionModel.getLocationAt(row);
                // Finish the edit before breakpoint refreshes. Keep the listing visible so
                // consecutive clicks toggle the breakpoint; a hit opens live memory.
                SwingUtilities.invokeLater(() -> {
                    if (!disposed) applyBreakpointRequest(new BreakpointDialog.Request(
                            (Boolean) value ? BreakpointDialog.Operation.SET : BreakpointDialog.Operation.UNSET,
                            address));
                });
            } else if (col < baseCount()) instructionModel.setValueAt(value, row, baseColumn(col));
        }
        @Override public void tableChanged(TableModelEvent event) {
            if (event.getFirstRow() == TableModelEvent.HEADER_ROW) fireTableStructureChanged();
            else fireTableDataChanged();
            selectInstructionAddress();
        }
    }

    private final class AddressModel extends AbstractTableModel {
        List<Integer> rows = List.of();

        void showAll() { show(null); }
        void show(List<Integer> addresses) {
            if (table.getModel() != this) { rows = addresses; return; }
            int row = table.getSelectedRow();
            int selectedAddress = row >= 0 && row < getRowCount() ? addressAt(row) : -1;
            rows = addresses;
            fireTableDataChanged();
            int restore = selectedAddress < 0 ? -1 : rowAt(selectedAddress);
            if (restore >= 0) table.setRowSelectionInterval(restore, restore);
        }
        void refreshData() {
            if (table.getModel() != this) { table.repaint(); return; }
            int selectedRow = table.getSelectedRow();
            int selectedAddress = selectedRow >= 0 && selectedRow < getRowCount() ? addressAt(selectedRow) : -1;
            Rectangle visible = table.getVisibleRect();
            fireTableDataChanged();
            int restore = selectedAddress < 0 ? -1 : rowAt(selectedAddress);
            if (restore >= 0) table.setRowSelectionInterval(restore, restore);
            table.scrollRectToVisible(visible);
        }
        int addressAt(int row) { return rows == null ? row : rows.get(row); }
        int rowAt(int at) {
            if (rows != null) return rows.indexOf(at);
            return at < getRowCount() ? at : -1;
        }
        @Override public int getRowCount() {
            if (rows != null) return rows.size();
            return session.memory == null ? 0 : session.memory.getSize();
        }
        @Override public int getColumnCount() { return 5; }
        @Override public String getColumnName(int col) {
            return new String[] {"Address", "Byte", "Label", "Comment", "Captured string"}[col];
        }
        @Override public Object getValueAt(int row, int col) {
            if (disposed || tasks.isBusy()) return "";
            int at = addressAt(row), selected = selectedBank();
            AnnotationIndex.Entry entry = annotationAt(at, selected);
            try {
                if (col == 0) return String.format("%04X", at);
                if (col == 1) {
                    if (viewCapture != null) return String.format("%02X", viewCapture.value(at, selected));
                    session.requirePaused();
                    return String.format("%02X", (Byte) session.memory.readBank(at, selected) & 255);
                }
                if (col == 2) return entry == null ? "" : entry.label;
                if (col == 3) return entry == null ? "" : entry.comment;
                return selected == stringBank ? discoveredStrings.getOrDefault(at, "") : "";
            } catch (RuntimeException ignored) { return ""; }
        }
    }

    private final class GraphPanel extends JPanel {
        FlowAnalysis flow;
        int bank, common;
        final Map<Integer, Rectangle> boxes = new LinkedHashMap<>();

        GraphPanel() {
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
                        int current = addresses.indexOf(inspectedAddress);
                        followNode(addresses.get(current < 0 ? 0 : Math.floorMod(current + delta, addresses.size())));
                    }
                });
            }
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "followFlow");
            getActionMap().put("followFlow", new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    if (boxes.containsKey(inspectedAddress)) followNode(inspectedAddress);
                }
            });
            addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            boxes.forEach(
                                    (at, box) -> {
                                        if (box.contains(e.getPoint())) {
                                            requestFocusInWindow();
                                            followNode(at);
                                        }
                                    });
                        }
                    });
        }

        private void followNode(int address) {
            guard(() -> {
                requireCurrent();
                navigate(address, bank, true, NavigationTarget.SOURCE_INSTRUCTION);
            });
        }

        boolean current() { return flow != null && flow.isCurrent(session.programIndex()); }

        void requireCurrent() {
            if (!current()) throw new IllegalStateException("Flow analysis is stale. Analyze again.");
        }

        @Override public String getToolTipText(MouseEvent event) {
            if (!current()) return "Flow analysis is stale. Analyze again.";
            for (Map.Entry<Integer, Rectangle> entry : boxes.entrySet())
                if (entry.getValue().contains(event.getPoint()))
                    return String.format("%04X · %s", entry.getKey(), flow.mnemonic(entry.getKey()));
            return "Arrow keys select an instruction; Enter follows it without changing the CPU";
        }

        void setFlow(FlowAnalysis flow) {
            this.flow = flow;
            bank = flow.bank();
            common = session.memory == null ? 0 : session.memory.getCommonBoundary();
            boxes.clear();
            flow.forEachInstruction(instruction -> {
                int at = instruction.cells().start;
                int i = boxes.size();
                boxes.put(at, new Rectangle(25 + (i % 3) * 240, 25 + (i / 3) * 100, 210, 60));
            });
            int i = boxes.size();
            setPreferredSize(new Dimension(760, Math.max(400, ((i + 2) / 3) * 100 + 50)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (flow == null) return;
            if (!current()) {
                graphics.setColor(getForeground());
                graphics.drawString("Flow analysis is stale. Analyze again.", 25, 35);
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            flow.forEachEdge((fromBank, from, toBank, to, kind) -> {
                Rectangle a = boxes.get(from), b = boxes.get(to);
                if (a == null || b == null) return;
                g.setColor(getForeground());
                int x = a.x + a.width / 2, y = a.y + a.height, tx = b.x + b.width / 2, ty = b.y;
                g.drawLine(x, y, tx, ty);
                double angle = Math.atan2(ty - y, tx - x);
                for (double delta : new double[] {-.5, .5})
                    g.drawLine(
                            tx,
                            ty,
                            tx - (int) (10 * Math.cos(angle + delta)),
                            ty - (int) (10 * Math.sin(angle + delta)));
                if (kind != ProgramIndex.EdgeKind.FALL_THROUGH) {
                    String label = kind.name().toLowerCase(Locale.ROOT);
                    int labelX = (x + tx) / 2, labelY = (y + ty) / 2;
                    g.setColor(getBackground());
                    g.fillRect(labelX - 4, labelY - 12, g.getFontMetrics().stringWidth(label) + 8, 16);
                    g.setColor(getForeground());
                    g.drawString(label, labelX, labelY);
                }
            });
            for (Map.Entry<Integer, Rectangle> entry : boxes.entrySet()) {
                Rectangle box = entry.getValue();
                boolean selected = (entry.getKey() >= common || bank == selectedBank()) && entry.getKey() == inspectedAddress;
                boolean pc = !running && !tasks.isBusy()
                        && (entry.getKey() >= common || bank == instructionBank()) && entry.getKey() == session.cpu.getInstructionLocation();
                Color selectionBackground = UIManager.getColor("List.selectionBackground");
                Color selectionForeground = UIManager.getColor("List.selectionForeground");
                g.setColor(selected && selectionBackground != null ? selectionBackground : getBackground());
                g.fillRect(box.x, box.y, box.width, box.height);
                g.setColor(selected && selectionForeground != null ? selectionForeground : getForeground());
                g.setStroke(new BasicStroke(selected ? 2 : 1));
                g.drawRect(box.x, box.y, box.width, box.height);
                g.drawString(String.format("%04X%s", entry.getKey(), pc ? " · PC" : ""), box.x + 8, box.y + 20);
                String text = flow.mnemonic(entry.getKey());
                g.drawString(
                        text.length() > 28 ? text.substring(0, 28) : text, box.x + 8, box.y + 42);
            }
            g.dispose();
        }
    }
}
