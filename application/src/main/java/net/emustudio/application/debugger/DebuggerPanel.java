/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
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
    private final JLabel inspectionContext = new JLabel();
    private final JLabel sourceContext = new JLabel("Compile sources to synchronize navigation");
    private final JLabel snapshotContext = new JLabel();
    private boolean sourceMappingsCurrent;
    private Consumer<SourceAddressMap.Symbol> symbolSelected = symbol -> {};
    private final DebugSession session;
    private final GUI gui;
    private final Consumer<SourceCodePosition> sourceNavigator;
    private final Runnable breakpointsChanged;
    private final AnalysisTasks tasks;
    private final Map<Component, Boolean> enabledControls = new IdentityHashMap<>();
    private boolean disposed;
    private boolean initializationPending;
    private JToggleButton recordingControl;
    private final JLabel annotationContext;
    private final Dialogs dialogs;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel breakpointPanel;
    private final JPanel watchPanel;
    private final JPanel navigationToolbar;
    private Runnable resetAnnotationEditor = () -> {};
    private final JLabel memoryContext;
    private final JLabel baselineContext;
    private final JLabel resultContext;
    private final JTextField annotationFilter;
    private final JCheckBox bookmarksOnly = new JCheckBox("Bookmarks only");
    private final Set<String> changedWatches = new HashSet<>();
    private List<String> displayedWatches = List.of();
    private List<DebugSession.Rule> displayedRules = List.of();
    private List<DebugCPU.Instruction> displayedHistory = List.of();
    private List<DebugCPU.Instruction> historySource = List.of();
    private final List<int[]> historyGroups = new ArrayList<>();
    private final HistoryModel historyModel = new HistoryModel();
    private final JTable history = new JTable(historyModel);
    private final JSpinner recentLimit = new JSpinner(new SpinnerNumberModel(1024, 1, 1_000_000, 1024));
    private final JTextField historyFrom = new JTextField(6), historyThrough = new JTextField(6),
            historyRegister = new JTextField(5), historyMemory = new JTextField(6), historyMemoryBank = new JTextField(3);
    private final JComboBox<String> historyAccess = new JComboBox<>(new String[] {"Any access", "Read", "Write"});
    private final JCheckBox foldHistory = new JCheckBox("Fold identical repeats");
    private final JLabel historyContext = new JLabel();
    private JButton fileRecordingControl, nextHistoryPage, firstHistoryPage, exportRecordingControl;
    private Path openedRecording;
    private InstructionRecording.Header openedHeader;
    private InstructionRecording.Reader.Cursor nextHistoryCursor;
    private long historyPage = 1;
    private boolean openedFinished;
    private String openedStatus = "";
    private List<Integer> ordinaryBreakpoints = List.of();
    private DebugSession.BreakpointHit heldInspection;
    private final int breakpointColumn;
    private final boolean addedBreakpointColumn;
    private final JTextField pattern;
    private int inspectedAddress;
    private int inspectionBank;
    private final JComboBox<String> width = new JComboBox<>(new String[] {"1", "2", "4", "8"});
    private final JComboBox<String> watchFormat = new JComboBox<>(new String[] {"Hex + decimal", "Binary", "ASCII"});
    private final JCheckBox bigEndian = new JCheckBox("Big endian");
    private final JCheckBox capturedStrings = new JCheckBox("String column");
    private final JCheckBox showUnchangedRegisters = new JCheckBox("Show unchanged registers");
    private final DefaultListModel<String> watchRows = new DefaultListModel<>();
    private final DefaultListModel<String> ruleRows = new DefaultListModel<>();
    private final DefaultListModel<AnnotationIndex.Entry> annotationRows = new DefaultListModel<>();
    private final DefaultListModel<FlowAnalysis.Edge> referenceRows = new DefaultListModel<>();
    private final JList<String> watches = new JList<>(watchRows),
            rules = new JList<>(ruleRows);
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
                                if (state != CPU.RunState.STATE_RUNNING) {
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
                                        guard(
                                                () ->
                                                        navigate(
                                                                session.cpu
                                                                        .getInstructionLocation(),
                                                                session.memory == null
                                                                        ? 0
                                                                        : session.memory
                                                                                .getSelectedBank(),
                                                                false));
                                    refresh();
                                } else {
                                    tasks.cancel();
                                    updateRecordingControls(false);
                                }
                            });
                }

                @Override
                public void internalStateChanged() {
                    SwingUtilities.invokeLater(() -> {
                        if (image == null && tasks.isBusy()) initializationPending = true;
                        refresh();
                    });
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
        annotationContext = gui.label("");
        memoryContext = gui.label("Live bytes — paused CPU");
        baselineContext = gui.label("Baseline: initial capture");
        resultContext = gui.label("All addresses");
        annotationFilter = gui.textField("", 14);
        lastInstructionAddress = session.cpu.getInstructionLocation();
        inspectedAddress = lastInstructionAddress;
        pattern = gui.textField("00 ??", 12);
        historicalState = gui.textAreaReadOnly(50, 10);
        for (JList<?> list : List.of(watches, rules, annotations, references)) gui.styleList(list);
        this.session = session;
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
        tabs.addTab("Instructions", instructionView);
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
        JPanel context = gui.panel("insets 2 6, fillx, hidemode 3", "[][grow][]", "");
        inspectionContext.getAccessibleContext().setAccessibleName("Inspection context");
        sourceContext.getAccessibleContext().setAccessibleName("Source synchronization status");
        snapshotContext.getAccessibleContext().setAccessibleName("Memory snapshot status");
        context.add(inspectionContext);
        context.add(sourceContext, "gapleft 12, growx");
        context.add(snapshotContext, "gapleft 12");
        add(context, BorderLayout.NORTH);
        JButton currentProgram = button(ICON_SNAPSHOT, "Take snapshot", () -> {
            if (index != null) session.saveAnnotations();
            runAnalysis(session::captureImage, captured -> {
                showImage(captured);
                snapshotContext.setText("Memory snapshot " + java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                snapshotContext.setToolTipText("Current memory and metadata captured. This does not capture a screenshot.");
            });
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
            if (!pinnedSelection) heldInspection = null;
            updateInspectionContext();
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
                                heldInspection = null;
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
        return pinnedSelection || session.keepsInspection(heldInspection);
    }

    public void setSourceNavigationStatus(String message, boolean current) {
        sourceContext.setText(message);
        sourceContext.setToolTipText(message);
        sourceContext.getAccessibleContext().setAccessibleDescription(message);
        sourceContext.setVisible(!"Source mapping current".equals(message));
        sourceMappingsCurrent = current;
        symbols.setEnabled(current && !running && !tasks.isBusy());
        updateSymbolCount();
    }

    private void updateInspectionContext() {
        inspectionContext.setText(running ? "Running — inspection paused"
                : String.format("Inspect %04X · Bank %d · %s", inspectedAddress, selectedBank(),
                        isInspectionPinned() ? "Pinned" : "Following"));
        inspectionContext.setToolTipText("Browsing changes the inspected address, never the CPU's PC or bank. Right-click the listing to pin inspection.");
        inspectionContext.getAccessibleContext().setAccessibleDescription(inspectionContext.getText());
        graph.repaint();
    }

    private void notifyAddressSelection(boolean visit) {
        updateInspectionContext();
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
        session.installImage(captured);
        image = captured.image;
        if (initial) baseline = image;
        annotationContext.setToolTipText("Annotations belong to memory image " + image.identity());
        index = captured.annotations;
        resetAnnotationEditor.run();
        refreshAnnotations(annotationFilter.getText());
        if (initial) model.showAll();
        else model.refreshData();
    }

    private void showFailure(Throwable failure) {
        if (failure.getCause() instanceof java.util.concurrent.CancellationException) return;
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
        }
        else {
            enabledControls.forEach(Component::setEnabled);
            enabledControls.clear();
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
        updateInspectionContext();
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
                if (session.keepsInspection(heldInspection)) {
                    if (selectedBank() != heldInspection.bank) resetMemoryView();
                    inspectionBank = heldInspection.bank;
                    lastMemoryAddress = heldInspection.address;
                }
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
        return byteMemory || session.cpu.isBreakpointSupported() || session.debug != null;
    }

    public void showBreakpointDialog(JFrame parent) {
        BreakpointDialog dialog = new BreakpointDialog(parent, dialogs, gui, inspectedAddress,
                selectedBank(), byteMemory ? session.memory.getBanksCount() : 1,
                session.cpu.getAddressSpaceSize(), byteMemory ? session.memory.getSize() : 0,
                session.cpu.isBreakpointSupported() || session.debug != null, session::breakpointState, this::applyBreakpointRequest);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    boolean applyBreakpointRequest(BreakpointDialog.Request request) {
        return applyBreakpointRequest(request, true);
    }

    private boolean applyBreakpointRequest(BreakpointDialog.Request request, boolean inspectNow) {
        try {
            if (tasks.isBusy()) throw new IllegalStateException("Wait for memory analysis to finish.");
            session.requirePaused();
            if (request.bank < 0 || request.bank >= (byteMemory ? session.memory.getBanksCount() : 1))
                throw new IllegalArgumentException("Bank outside memory");
            boolean inspect = request.operation == BreakpointDialog.Operation.INSPECT_ONLY
                    || request.operation == BreakpointDialog.Operation.SET && byteMemory;
            if (inspect && (!byteMemory || request.address < 0 || request.address >= session.memory.getSize()))
                throw new IllegalArgumentException("Address outside memory");
            if (request.operation != BreakpointDialog.Operation.INSPECT_ONLY) {
                if (request.address < 0 || request.address >= session.cpu.getAddressSpaceSize())
                    throw new IllegalArgumentException("Address outside CPU");
                boolean set = request.operation == BreakpointDialog.Operation.SET;
                session.setBreakpointState(request.address, !set ? BreakpointState.OFF
                        : request.keepInspection ? BreakpointState.KEEP_INSPECTION : BreakpointState.ON);
                breakpointsChanged.run();
                session.save();
                refresh();
            }
            if (inspect && inspectNow) {
                inspectLive(request.address, request.bank);
                if (request.operation == BreakpointDialog.Operation.SET)
                    heldInspection = session.inspectionAtBreakpoint(request.address, request.bank);
            }
            return true;
        } catch (Exception e) {
            showFailure(e);
            return false;
        }
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
        heldInspection = hit;
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
        if (remember) { rememberAddress(at, selected); heldInspection = null; lastMemoryAddress = at; }
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
        memoryContext.setText("Live bytes — paused CPU");
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
        JComboBox<String> findKind = new JComboBox<>(new String[] {"Bytes", "Integer", "Text"});
        JLabel widthLabel = label("Width (bytes)", width);
        findKind.addActionListener(e -> {
            boolean integer = findKind.getSelectedIndex() == 1;
            widthLabel.setVisible(integer);
            width.setVisible(integer);
            bigEndian.setVisible(integer);
            pattern.setToolTipText(integer ? "Integer, e.g. 42 or 0x2A"
                    : findKind.getSelectedIndex() == 2 ? "Exact UTF-8 text" : "Hex bytes, e.g. 3E ?? 32");
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
                        memoryContext.setText("Captured bytes — rescan, bank " + selected);
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
        resultTools.add(memoryContext);
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
                memoryContext.setText("Captured bytes — ASCII strings, bank " + selected);
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
                memoryContext.setText("Captured bytes — compared with baseline, bank " + selected);
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
                memoryContext.setText("Captured bytes — compared with " + file.getFileName());
                showDifferences(found.addresses, selected);
            });
        }));
        compareActions.add(button(ICON_COMPILER, "Show source", () -> showSource(inspectedAddress)));
        compareActions.add(capturedStrings);
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
                    memoryContext.setText("Captured bytes — search, bank " + selected);
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
        annotations.setCellRenderer(new DetailRenderer<>((entry, row) -> new ListDetail(
                String.format("%04X · Bank %d", entry.address, entry.bank),
                entry.label.isBlank() ? "Unnamed annotation" : entry.label,
                entry.kind + (entry.bookmark ? " · Bookmark" : "")
                        + (entry.comment.isBlank() ? "" : " · " + entry.comment), entry.toString())));
        annotations.getAccessibleContext().setAccessibleName("Annotations");
        panel.add(gui.scrollPane(annotations));
        JPanel tools = row();
        JTextField labelField = gui.textField("", 12), comment = gui.textField("", 20);
        JCheckBox bookmark = new JCheckBox("Bookmark");
        JComboBox<String> kind = new JComboBox<>(new String[] {"UNKNOWN", "CODE", "DATA"});
        int[] editingTarget = {selectedBank(), 0};
        annotationContext.getAccessibleContext().setAccessibleName("Annotation editing address");
        Runnable updateEditingContext = () -> annotationContext.setText(
                String.format("Edit %04X · Bank %d", editingTarget[1], editingTarget[0]));
        resetAnnotationEditor = () -> {
            annotations.clearSelection();
            editingTarget[0] = selectedBank();
            editingTarget[1] = inspectedAddress;
            AnnotationIndex.Entry entry = index == null ? null : index.get(editingTarget[0], editingTarget[1]);
            labelField.setText(entry == null ? "" : entry.label);
            comment.setText(entry == null ? "" : entry.comment);
            bookmark.setSelected(entry != null && entry.bookmark);
            kind.setSelectedItem(entry == null ? "UNKNOWN" : entry.kind);
            updateEditingContext.run();
        };
        annotations.addListSelectionListener(e -> {
            AnnotationIndex.Entry entry = annotations.getSelectedValue();
            if (!e.getValueIsAdjusting() && entry != null) {
                editingTarget[0] = entry.bank;
                editingTarget[1] = entry.address;
                updateEditingContext.run();
                labelField.setText(entry.label);
                comment.setText(entry.comment);
                bookmark.setSelected(entry.bookmark);
                kind.setSelectedItem(entry.kind);
                guard(() -> navigate(entry.address, entry.bank, true));
            }
        });
        JToolBar filterTools = line(tools, label("Filter", annotationFilter), annotationFilter, bookmarksOnly,
                button(ICON_JUMP, "Go to annotation", () -> {
                    AnnotationIndex.Entry entry = annotations.getSelectedValue();
                    if (entry != null) navigate(entry.address, entry.bank, true);
                }), button(ICON_DELETE, "Delete", () -> {
                    AnnotationIndex.Entry entry = annotations.getSelectedValue();
                    if (entry != null) {
                        index.remove(entry.bank, entry.address);
                        session.saveAnnotations();
                        refreshAnnotations(annotationFilter.getText());
                        model.refreshData();
                    }
                }));
        filterTools.addSeparator();
        filterTools.add(annotationContext);
        filterTools.add(button(ICON_RENAME, "Annotate inspected address", () -> resetAnnotationEditor.run()));
        annotationFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshAnnotations(annotationFilter.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshAnnotations(annotationFilter.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshAnnotations(annotationFilter.getText()); }
        });
        bookmarksOnly.addActionListener(e -> refreshAnnotations(annotationFilter.getText()));
        JPanel fields = gui.panel("insets 4, fillx", "[][120:180:240,fill][][grow,fill]", "");
        fields.add(label("Label", labelField));
        fields.add(labelField, "growx");
        fields.add(label("Comment", comment));
        fields.add(comment, "growx");
        tools.add(fields, "span, growx, wrap");
        JToolBar annotationActions = line(tools, label("Kind", kind), kind, bookmark, button(ICON_SAVE, "Save annotation", () -> {
            index.put(new AnnotationIndex.Entry(editingTarget[0], editingTarget[1],
                    labelField.getText(), comment.getText(), bookmark.isSelected(), (String) kind.getSelectedItem()));
            session.saveAnnotations();
            refreshAnnotations(annotationFilter.getText());
            model.refreshData();
        }), button(ICON_SAVE, "Export annotations", () -> {
            Path path = choose(true, false);
            if (path != null) index.save(path);
        }), button(ICON_LOAD, "Import", () -> {
            Path path = choose(false, false);
            if (path != null) {
                index.load(path);
                session.saveAnnotations();
                refreshAnnotations(annotationFilter.getText());
                model.refreshData();
            }
        }));
        annotationActions.add(button(ICON_ERASE_MEMORY, "Clear annotations", () -> {
            if (JOptionPane.showConfirmDialog(this, "Remove all annotations from this image?",
                    "Clear annotations", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            session.clearAnnotations();
            index = session.annotations();
            refreshAnnotations(annotationFilter.getText());
            model.refreshData();
        }));
        annotationActions.add(button(ICON_DISK_MOUNTED, "Export memory and metadata", () -> {
            Path path = choose(true, true);
            if (path != null) {
                MetadataExport.export(path, image, index, session.debug == null ? session.cpu.getTitle() : session.debug.processor(), selectedBank(), 0);
            }
        }));
        panel.add(scrollTools(tools), BorderLayout.NORTH);
        return panel;
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
        panel.setPreferredSize(new Dimension(280, 230));
        return panel;
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
                        rule.kind + " · " + target + " · " + (rule.bank < 0 ? "All banks" : "Bank " + rule.bank)
                                + " · hits " + rule.hits + " / " + rule.afterHits
                                + (rule.keepInspection ? " · Keep inspection" : "")
                                + (rule.condition.isBlank() ? "" : " · if " + rule.condition), value);
            }
            int ordinary = row - displayedRules.size();
            return ordinary >= 0 && ordinary < ordinaryBreakpoints.size()
                    ? new ListDetail("Enabled", String.format("EXEC · %04X", ordinaryBreakpoints.get(ordinary)),
                            session.breakpointState(ordinaryBreakpoints.get(ordinary)) == BreakpointState.KEEP_INSPECTION
                                    ? "Keep inspection while stepping" : "Inspect on hit", value)
                    : new ListDetail("", value, "", value);
        }));
        p.add(gui.scrollPane(rules));
        JPanel tools = row();
        JTextField name = gui.textField("Breakpoint", 10), at = gui.textField("0x0000", 7),
                condition = gui.textField("", 18), hits = gui.textField("1", 3),
                source = gui.textField("", 22);
        JComboBox<String> bankField = new JComboBox<>();
        bankField.addItem("All banks");
        for (int bank = 0; bank < (session.memory == null ? 1 : session.memory.getBanksCount()); bank++)
            bankField.addItem("Bank " + bank);
        JComboBox<String> kind = new JComboBox<>(new String[] {"EXEC", "READ", "WRITE", "SOURCE"});
        JCheckBox keepInspection = new JCheckBox("Keep inspection while stepping");
        kind.getAccessibleContext().setAccessibleName("Breakpoint kind");
        JLabel sourceLabel = label("Source file", source);
        source.setVisible(false);
        sourceLabel.setVisible(false);
        JLabel atLabel = label("Address", at);
        kind.addActionListener(e -> {
            boolean isSource = "SOURCE".equals(kind.getSelectedItem());
            source.setVisible(isSource);
            sourceLabel.setVisible(isSource);
            atLabel.setText(isSource ? "Line" : "Address");
            tools.revalidate();
        });
        JButton addRule = button(ICON_ADD, "Add breakpoint", () -> {
            if (session.debug == null) throw new IllegalStateException("Advanced breakpoints unavailable for this CPU");
            int ruleBank = bankField.getSelectedIndex() - 1, location = Integer.decode(at.getText());
            String type = (String) kind.getSelectedItem();
            if (session.memory != null && ruleBank >= session.memory.getBanksCount())
                throw new IllegalArgumentException("Bank outside memory; use -1 for all banks");
            if (!type.equals("SOURCE") && location >= session.cpu.getAddressSpaceSize())
                throw new IllegalArgumentException("Address outside CPU");
            if (!condition.getText().isBlank()) session.evaluate(condition.getText(), selectedBank());
            DebugSession.Rule rule = new DebugSession.Rule(name.getText(), type, location,
                    ruleBank, condition.getText(), Long.parseLong(hits.getText()));
            rule.keepInspection = keepInspection.isSelected();
            if (type.equals("SOURCE")) {
                if (source.getText().isBlank() || location < 1)
                    throw new IllegalArgumentException("Enter source file and line >= 1");
                rule.source = Path.of(source.getText()).toAbsolutePath().normalize().toString();
            }
            session.rules.add(rule);
            session.updateProbe();
            refresh();
            rules.setSelectedIndex(session.rules.size() - 1);
            session.save();
        });
        JPanel fields = gui.panel("insets 4, fillx, hidemode 3", "[][grow,fill][][grow,fill][]", "");
        fields.add(label("Name", name)); fields.add(name);
        fields.add(label("Kind", kind)); fields.add(kind); fields.add(addRule, "wrap");
        fields.add(atLabel); fields.add(at);
        fields.add(label("Bank", bankField)); fields.add(bankField);
        JToggleButton advanced = toggleButton(ICON_SETTINGS, "Advanced breakpoint options");
        fields.add(advanced, "wrap");
        fields.add(sourceLabel); fields.add(source, "span 3, growx, wrap");
        tools.add(fields, "span, growx, wrap");
        JPanel advancedFields = gui.panel("insets 4, fillx", "[][grow,fill][][70!,fill]", "");
        advancedFields.add(label("Condition", condition)); advancedFields.add(condition);
        advancedFields.add(label("Stop from hit", hits)); advancedFields.add(hits, "wrap");
        advancedFields.add(keepInspection, "span, wrap");
        advancedFields.setVisible(false);
        advanced.addActionListener(event -> {
            advancedFields.setVisible(advanced.isSelected());
            tools.revalidate();
        });
        tools.add(advancedFields, "span, growx, wrap");
        addRule.setEnabled(session.debug != null);

        JToolBar actions = line(tools);
        actions.add(
                button(ICON_BREAKPOINT, "Enable / disable",
                        () -> {
                            int selected = rules.getSelectedIndex();
                            if (selected >= 0 && selected < session.rules.size()) {
                                DebugSession.Rule rule = session.rules.get(selected);
                                rule.enabled = !rule.enabled;
                                session.updateProbe();
                                session.save();
                                refresh();
                            }
                        }));
        actions.add(
                button(ICON_REMOVE, "Remove",
                        () -> {
                            int selected = rules.getSelectedIndex();
                            if (selected >= 0 && selected < session.rules.size()) {
                                session.rules.remove(selected);
                                session.updateProbe();
                                session.save();
                                refresh();
                            } else if (selected >= session.rules.size()
                                    && selected
                                            < session.rules.size() + ordinaryBreakpoints.size()) {
                                session.setBreakpointState(
                                        ordinaryBreakpoints.get(selected - session.rules.size()), BreakpointState.OFF);
                                breakpointsChanged.run();
                                session.save();
                                refresh();
                            }
                        }));
        JButton addOrdinary = button(ICON_BREAKPOINTS, "Add ordinary breakpoint", () -> {
            int location = Integer.decode(at.getText());
            if (location < 0 || location >= session.cpu.getAddressSpaceSize())
                throw new IllegalArgumentException("Address outside CPU");
            session.setBreakpointState(location, BreakpointState.ON);
            breakpointsChanged.run();
            session.save();
            refresh();
            if (byteMemory) inspectLive(location, selectedBank());
        });
        addOrdinary.setEnabled(session.cpu.isBreakpointSupported());
        actions.add(addOrdinary);
        actions.add(button(ICON_SAVE, "Save settings", session::save));
        actions.add(
                button(ICON_RESET, "Reset hit counts",
                        () -> {
                            session.rules.forEach(r -> r.hits = 0);
                            refresh();
                        }));
        actions.add(
                button(ICON_JUMP, "Go to address",
                        () -> {
                            int selected = rules.getSelectedIndex();
                            if (selected >= 0 && selected < session.rules.size()) {
                                DebugSession.Rule rule = session.rules.get(selected);
                                if (!rule.kind.equals("SOURCE"))
                                    navigate(
                                            rule.address,
                                            rule.bank < 0 ? selectedBank() : rule.bank,
                                            true);
                            } else if (selected >= session.rules.size()
                                    && selected
                                            < session.rules.size() + ordinaryBreakpoints.size()) {
                                navigate(
                                        ordinaryBreakpoints.get(selected - session.rules.size()),
                                        selectedBank(),
                                        true);
                            }
                        }));
        if (session.debug == null) {
            name.setEnabled(false);
            kind.setEnabled(false);
            condition.setEnabled(false);
            hits.setEnabled(false);
            bankField.setEnabled(false);
            source.setEnabled(false);
            keepInspection.setEnabled(false);
            tools.add(gui.label("Advanced breakpoints unavailable; use ordinary execution breakpoints."), "span, wrap");
        }
        p.add(scrollTools(tools), BorderLayout.NORTH);
        return p;
    }

    private JPanel flowPanel() {
        JComboBox<Integer> nodes = new JComboBox<>();
        nodes.setPrototypeDisplayValue(65535);
        nodes.getAccessibleContext().setAccessibleName("Decoded instruction address");
        JPanel p = borderPanel(0);
        JPanel tools = row();
        JToolBar actions = line(tools);
        actions.add(
                button(ICON_CONNECTION, "Analyze from address",
                        () -> {
                            if (session.memory == null
                                    || selectedBank() != session.memory.getSelectedBank())
                                throw new IllegalStateException(
                                        "Flow analysis uses the CPU's currently selected bank");
                            FlowAnalysis flow =
                                    new FlowAnalysis(
                                            session.cpu.getDisassembler(),
                                            inspectedAddress,
                                            256,
                                            index,
                                            selectedBank());
                            referenceRows.clear();
                            flow.edges.stream()
                                    .filter(e -> !e.kind.equals("next"))
                                    .forEach(referenceRows::addElement);
                            graph.setFlow(flow);
                            nodes.removeAllItems();
                            flow.instructions.keySet().forEach(nodes::addItem);
                        }));
        actions.add(
                button(ICON_FIND, "Find incoming references",
                        () -> {
                            if (session.memory == null
                                    || selectedBank() != session.memory.getSelectedBank())
                                throw new IllegalStateException(
                                        "Reference analysis uses the CPU's currently selected"
                                                + " bank");
                            int target = inspectedAddress;
                            referenceRows.clear();
                            Disassembler dis = session.cpu.getDisassembler();
                            for (int at = 0; at < session.memory.getSize(); ) {
                                AnnotationIndex.Entry e =
                                        index == null ? null : index.get(selectedBank(), at);
                                try {
                                    if (e == null || !e.kind.equals("DATA")) {
                                        int source = at;
                                        dis.getFlow(at)
                                                .ifPresent(
                                                        f ->
                                                                f.targets.stream()
                                                                        .filter(to -> to == target)
                                                                        .forEach(
                                                                                to ->
                                                                                        referenceRows
                                                                                                .addElement(
                                                                                                        new FlowAnalysis
                                                                                                                .Edge(
                                                                                                                source,
                                                                                                                to,
                                                                                                                f.kind.name()
                                                                                                                        .toLowerCase(
                                                                                                                                Locale
                                                                                                                                        .ROOT)))));
                                    }
                                    int next = dis.getNextInstructionPosition(at);
                                    at = next > at ? next : at + 1;
                                } catch (RuntimeException ignored) {
                                    at++;
                                }
                            }
                        }));
        actions.add(
                button(ICON_JUMP, "Go to reference",
                        () -> {
                            FlowAnalysis.Edge e = references.getSelectedValue();
                            if (e != null) navigate(e.from, selectedBank(), true);
                        }));
        actions.add(label("Instruction", nodes));
        actions.add(nodes);
        actions.add(button(ICON_STEP, "Go to instruction", () -> {
            if (nodes.getSelectedItem() != null) navigate((Integer) nodes.getSelectedItem(), selectedBank(), true);
        }));
        JLabel flowHint = gui.label("Select a reference or instruction to inspect. Indirect targets may be unresolved.");
        flowHint.setToolTipText("Graph: arrow keys select instructions; Enter follows. PC marks the live instruction.");
        graph.getAccessibleContext().setAccessibleDescription(flowHint.getToolTipText());
        tools.add(flowHint, "span, wrap");
        references.getAccessibleContext().setAccessibleName("Static references");
        references.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "followReference");
        references.getActionMap().put("followReference", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                guard(() -> {
                    FlowAnalysis.Edge edge = references.getSelectedValue();
                    if (edge != null) navigate(edge.from, selectedBank(), true);
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

    private JPanel historyPanel() {
        JPanel p = borderPanel(0);
        history.getAccessibleContext().setAccessibleName("Recorded instructions");
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        gui.styleTable(history);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, history.getFont().getSize()));
        int[] widths = {120, 55, 65, 130, 180, 200, 220};
        for (int column = 0; column < widths.length; column++)
            history.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        JPanel tools = row();
        recordingControl = toggleButton(ICON_RECORD, "Record recent instructions");
        recordingControl.addActionListener(e -> guard(() -> {
            session.record(recordingControl.isSelected() ? ((Number) recentLimit.getValue()).intValue() : 0);
            refreshHistory();
            updateRecordingControls(true);
        }));
        recentLimit.addChangeListener(e -> guard(() -> {
            if (session.isRecording()) session.record(((Number) recentLimit.getValue()).intValue());
            refreshHistory();
        }));
        fileRecordingControl = button(ICON_SAVE, "Record to file", () -> {
            if (session.recordingPath() != null) {
                runAnalysis(() -> { session.stopRecordingFile(); return true; }, ignored -> refreshHistory());
            } else {
                Path file = choose(true, false);
                if (file != null) {
                    requireNewHistoryDestination(file);
                    runAnalysis(() -> { session.recordTo(file); return true; }, ignored -> refreshHistory());
                }
            }
        });
        line(tools, recordingControl, label("Recent limit", recentLimit), recentLimit,
                fileRecordingControl, button(ICON_REFRESH, "Refresh history", this::refreshHistory),
                button(ICON_SAVE, "Save recent recording", () -> saveHistory(false)),
                button(ICON_COPY, "Export displayed history as text", () -> saveHistory(true)));
        exportRecordingControl = button(ICON_COPY, "Export entire recording as text", () -> {
            if (openedRecording == null) return;
            Path file = choose(true, false);
            if (file == null) return;
            requireNewHistoryDestination(file);
            Path source = openedRecording;
            runAnalysis(() -> { InstructionRecording.exportText(file, source); return true; },
                    ignored -> historyContext.setText("Exported entire recording to " + file.getFileName()));
        });
        firstHistoryPage = button(ICON_UNDO, "First page", () -> {
            if (openedRecording != null) loadHistoryPage(openedRecording, null, 1);
        });
        nextHistoryPage = button(ICON_STEP, "Next page", () -> {
            if (openedRecording != null && !openedFinished)
                loadHistoryPage(openedRecording, nextHistoryCursor, historyPage + 1);
        });
        line(tools, exportRecordingControl, button(ICON_OPEN_FILE, "Open recording", () -> {
            Path file = choose(false, false);
            if (file != null) openRecording(file);
        }), firstHistoryPage, nextHistoryPage, button(ICON_MEMORY, "Live history", () -> {
            openedRecording = null;
            openedHeader = null;
            historyPage = 1;
            refreshHistory();
        }), showUnchangedRegisters, foldHistory);
        line(tools, label("PC from", historyFrom), historyFrom, label("PC through", historyThrough), historyThrough,
                label("Changed register", historyRegister), historyRegister);
        line(tools, label("Memory address", historyMemory), historyMemory, label("Memory bank", historyMemoryBank),
                historyMemoryBank, historyAccess, button(ICON_FIND, "Apply history filters", this::refreshHistory),
                button(ICON_UNDO, "Clear history filters", () -> { clearHistoryFilters(); refreshHistory(); }),
                button(ICON_STEP_BACK, "Previous recorded write", this::previousRecordedWrite));
        for (JTextField field : List.of(historyFrom, historyThrough, historyRegister, historyMemory, historyMemoryBank)) {
            field.setToolTipText(field == historyRegister ? "Register name; only instructions changing this register"
                    : "Optional address or bank: decimal or 0x-prefixed hexadecimal");
            field.addActionListener(e -> guard(this::refreshHistory));
        }
        historyAccess.getAccessibleContext().setAccessibleName("Memory access kind");
        historyAccess.addActionListener(e -> guard(this::refreshHistory));
        showUnchangedRegisters.addActionListener(e -> showHistoricalState(history.getSelectedRow()));
        foldHistory.addActionListener(e -> guard(this::refreshHistory));
        historyContext.getAccessibleContext().setAccessibleName("Recording status");
        historyContext.setToolTipText("Filters and previous-write navigation apply to the current page or retained recent history.");
        tools.add(historyContext, "span, growx, wrap");
        tools.add(gui.label("Read-only history. Moving PC does not restore machine state."), "span, growx, wrap");
        historicalState.setEditable(false);
        historicalState.setFont(new Font(Font.MONOSPACED, Font.PLAIN, historicalState.getFont().getSize()));
        historicalState.getAccessibleContext().setAccessibleName("Recorded instruction state");
        history.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showHistoricalState(history.getSelectedRow());
        });
        p.add(scrollTools(tools), BorderLayout.NORTH);
        JSplitPane split = gui.splitPaneLeftToRight(gui.scrollPane(history), gui.scrollPane(historicalState), .6);
        split.setResizeWeight(.6);
        p.add(split);
        updateRecordingControls(true);
        return p;
    }

    private void updateRecordingControls(boolean paused) {
        if (recordingControl == null) return;
        recordingControl.setSelected(session.isRecording());
        recordingControl.setEnabled(session.debug != null && paused);
        recentLimit.setEnabled(session.debug != null && paused);
        fileRecordingControl.setEnabled(session.debug != null && paused);
        boolean saving = session.recordingPath() != null;
        fileRecordingControl.setToolTipText(saving ? "Stop file recording" : "Record to file");
        fileRecordingControl.getAccessibleContext().setAccessibleName(fileRecordingControl.getToolTipText());
        firstHistoryPage.setEnabled(openedRecording != null && paused);
        exportRecordingControl.setEnabled(openedRecording != null && paused);
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
        requireNewHistoryDestination(file);
        InstructionRecording.Header header = text && openedHeader != null ? openedHeader : session.recordingHeader();
        List<DebugCPU.Instruction> events = text ? List.copyOf(displayedHistory) : session.history();
        runAnalysis(() -> {
            if (text) InstructionRecording.exportText(file, header, events);
            else InstructionRecording.write(file, header, events);
            return true;
        }, ignored -> historyContext.setText("Saved " + events.size() + " instructions to " + file.getFileName()));
    }

    // Kept package-visible for the same offline path exercised by the Open recording action.
    void openRecording(Path file) {
        loadHistoryPage(file, null, 1);
    }

    private void loadHistoryPage(Path file, InstructionRecording.Reader.Cursor cursor, long page) {
        runAnalysis(() -> {
            try (InstructionRecording.Reader reader = new InstructionRecording.Reader(file)) {
                if (cursor != null) reader.seek(cursor);
                List<DebugCPU.Instruction> events = reader.nextPage(1024);
                return new HistoryPage(file, reader.header(), events, reader.cursor(), page,
                        reader.finished(), reader.status());
            }
        }, loaded -> {
            openedRecording = loaded.file;
            openedHeader = loaded.header;
            historySource = loaded.events;
            nextHistoryCursor = loaded.cursor;
            historyPage = loaded.page;
            openedFinished = loaded.finished;
            openedStatus = loaded.status;
            refreshHistory();
        });
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

    private void previousRecordedWrite() {
        Integer address = historyNumber(historyMemory), bank = historyNumber(historyMemoryBank);
        if (address == null) throw new IllegalArgumentException("Enter a Memory address to find its previous recorded write");
        int selected = history.getSelectedRow();
        long beforeSequence = selected < 0 ? Long.MAX_VALUE : displayedHistory.get(historyGroups.get(selected)[0]).sequence;
        DebugCPU.Instruction found = null;
        for (int i = historySource.size() - 1; i >= 0; i--) {
            DebugCPU.Instruction event = historySource.get(i);
            if (event.sequence < beforeSequence && event.accesses.stream().anyMatch(access -> access.write
                    && access.address == address && (bank == null || access.bank == bank))) {
                found = event;
                break;
            }
        }
        if (found == null) {
            historyContext.setText("No earlier write in this page / retained history. Open an earlier page for older events.");
            return;
        }
        historyFrom.setText("");
        historyThrough.setText("");
        historyRegister.setText("");
        foldHistory.setSelected(false);
        historyAccess.setSelectedIndex(2);
        refreshHistory();
        for (int row = 0; row < historyGroups.size(); row++)
            if (displayedHistory.get(historyGroups.get(row)[0]) == found) {
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
        Integer from = historyNumber(historyFrom), through = historyNumber(historyThrough),
                memory = historyNumber(historyMemory), bank = historyNumber(historyMemoryBank);
        if (from != null && through != null && from > through)
            throw new IllegalArgumentException("PC from must not exceed PC through");
        String register = historyRegister.getText().strip().toUpperCase(Locale.ROOT);
        displayedHistory = new ArrayList<>();
        for (DebugCPU.Instruction event : historySource) {
            if ((from != null && event.address < from) || (through != null && event.address > through)) continue;
            if (!register.isEmpty() && (!event.before.containsKey(register)
                    || Objects.equals(event.before.get(register), event.after.get(register)))) continue;
            if ((memory != null || bank != null || historyAccess.getSelectedIndex() != 0)
                    && event.accesses.stream().noneMatch(access -> (memory == null || access.address == memory)
                    && (bank == null || access.bank == bank)
                    && (historyAccess.getSelectedIndex() == 0 || access.write == (historyAccess.getSelectedIndex() == 2)))) continue;
            displayedHistory.add(event);
        }
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
        String origin = openedRecording == null ? "Recent history" : openedHeader.processor + " · "
                + openedRecording.getFileName() + " · page " + historyPage + " · " + openedStatus;
        String recording = session.recordingPath() == null ? "" : " · Recording to " + session.recordingPath().getFileName();
        historyContext.setText(origin + " · " + displayedHistory.size() + " / " + historySource.size()
                + " instructions (filters apply to this page / retained history)" + recording
                + (session.recordingError().isBlank() ? "" : " · " + session.recordingError()));
        if (displayedHistory.isEmpty()) {
            historicalState.setText(historySource.isEmpty() ? "No recorded instructions. Record recent instructions or open a recording."
                    : "No instructions match the current page / retained history filters.");
            historicalState.setCaretPosition(0);
        } else if (history.getSelectedRow() < 0) {
            historicalState.setText("Select an instruction to inspect its captured registers and memory accesses.");
            historicalState.setCaretPosition(0);
        }
        updateRecordingControls(!running && !tasks.isBusy());
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
        private final String[] columns = {"Sequence", "Bank", "PC", "Bytes", "Instruction", "Changed registers", "Memory writes"};
        @Override public int getRowCount() { return historyGroups.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            int[] group = historyGroups.get(row);
            DebugCPU.Instruction event = displayedHistory.get(group[0]), last = displayedHistory.get(group[1]);
            switch (column) {
                case 0: return group[0] == group[1] ? Long.toString(event.sequence)
                        : event.sequence + "–" + last.sequence + " (" + (group[1] - group[0] + 1) + ")";
                case 1: return event.bank;
                case 2: return String.format("%04X", event.address);
                case 3: return event.opcodes;
                case 4: return event.mnemonic + (event.completed ? "" : " (failed)");
                case 5: {
                    List<String> values = new ArrayList<>();
                    for (String name : new TreeSet<>(event.before.keySet()))
                        if (!Objects.equals(event.before.get(name), event.after.get(name)))
                            values.add(name + "=" + String.format("%X→%X", event.before.get(name), event.after.getOrDefault(name, 0L)));
                    return String.join(" ", values);
                }
                default: {
                    List<String> values = new ArrayList<>();
                    for (DebugCPU.Access access : event.accesses)
                        if (access.write) values.add(String.format("%d:%04X=%02X", access.bank, access.address, access.value));
                    return String.join(" ", values);
                }
            }
        }
    }

    private void showHistoricalState(int selected) {
        if (selected < 0 || selected >= historyGroups.size()) return;
        int[] group = historyGroups.get(selected);
        DebugCPU.Instruction event = displayedHistory.get(group[0]);
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
        updateRows(annotationRows, entries);
        if (selected != null) {
            for (int i = 0; i < annotationRows.size(); i++) {
                AnnotationIndex.Entry entry = annotationRows.get(i);
                if (entry.bank == selected.bank && entry.address == selected.address) {
                    annotations.setSelectedIndex(i);
                    break;
                }
            }
        }
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

    private void refresh() {
        if (disposed || tasks.isBusy()) return;
        try { session.requirePaused(); } catch (IllegalStateException e) { return; }
        updateRecordingControls(true);
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
                String formatted = watchFormat.getSelectedIndex() == 1 ? Long.toBinaryString(value)
                        : watchFormat.getSelectedIndex() == 2 ? "'" + (value >= 32 && value <= 126 ? (char) value : '.') + "'"
                        : String.format("0x%X", value) + " / " + value;
                watchText.add(expression + " = " + formatted + (changedWatches.contains(expression) ? "  * changed" : ""));
            } catch (RuntimeException e) {
                watchText.add(expression + " — " + e.getMessage());
            }
        }
        updateRows(watchRows, watchText);
        if (selectedExpression != null) watches.setSelectedIndex(displayedWatches.indexOf(selectedExpression));
        int ruleSelection = rules.getSelectedIndex();
        DebugSession.Rule selectedRule = ruleSelection >= 0 && ruleSelection < displayedRules.size()
                ? displayedRules.get(ruleSelection) : null;
        Integer selectedOrdinary = ruleSelection >= displayedRules.size()
                && ruleSelection < displayedRules.size() + ordinaryBreakpoints.size()
                ? ordinaryBreakpoints.get(ruleSelection - displayedRules.size()) : null;
        displayedRules = List.copyOf(session.rules);
        List<String> ruleText = new ArrayList<>();
        for (DebugSession.Rule rule : displayedRules)
            ruleText.add((rule.enabled ? "Enabled " : "Disabled ") + rule.name + " [" + rule.kind + "] "
                    + (rule.kind.equals("SOURCE") ? rule.source + ":" + rule.address : String.format("%04X", rule.address))
                    + " bank=" + rule.bank + " hits=" + rule.hits
                    + (rule.keepInspection ? " keep inspection" : "")
                    + (rule.condition.isBlank() ? "" : " if " + rule.condition));
        ordinaryBreakpoints = new ArrayList<>(new TreeSet<>(session.cpu.getBreakpoints()));
        for (int ordinary : ordinaryBreakpoints)
            ruleText.add(String.format("Ordinary execution breakpoint: %04X%s", ordinary,
                    session.breakpointState(ordinary) == BreakpointState.KEEP_INSPECTION ? " keep inspection" : ""));
        updateRows(ruleRows, ruleText);
        if (selectedRule != null) rules.setSelectedIndex(displayedRules.indexOf(selectedRule));
        else if (selectedOrdinary != null) {
            int i = ordinaryBreakpoints.indexOf(selectedOrdinary);
            rules.setSelectedIndex(i < 0 ? -1 : displayedRules.size() + i);
        }
        try { refreshHistory(); } catch (IllegalArgumentException failure) {
            historyContext.setText(failure.getMessage());
        }
        navigationToolbar.setToolTipText(session.status.isBlank() ? null : session.status);
        model.refreshData();
        refreshAnnotations(annotationFilter.getText());
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
            return col < baseCount() ? instructionModel.getColumnName(baseColumn(col)) : col == baseCount() ? "Label" : "Comment";
        }
        @Override public Class<?> getColumnClass(int col) {
            if (col == breakpointColumn) return BreakpointState.class;
            return col < baseCount() ? instructionModel.getColumnClass(baseColumn(col)) : String.class;
        }
        @Override public Object getValueAt(int row, int col) {
            if (disposed || tasks.isBusy() || running) return null;
            if (col == breakpointColumn) {
                int address = instructionModel.getLocationAt(row);
                return address < 0 ? null : session.breakpointState(address);
            }
            if (col < baseCount()) return instructionModel.getValueAt(row, baseColumn(col));
            int at = instructionModel.getLocationAt(row);
            AnnotationIndex.Entry entry = annotationAt(at, instructionBank());
            return entry == null ? "" : col == baseCount() ? entry.label : entry.comment;
        }
        @Override public boolean isCellEditable(int row, int col) {
            if (disposed || tasks.isBusy() || running) return false;
            if (col == breakpointColumn) return instructionModel.getLocationAt(row) >= 0;
            return col < baseCount() && instructionModel.isCellEditable(row, baseColumn(col));
        }
        @Override public void setValueAt(Object value, int row, int col) {
            if (col == breakpointColumn && value instanceof BreakpointState) {
                int address = instructionModel.getLocationAt(row);
                BreakpointState state = (BreakpointState) value;
                // Finish the edit before breakpoint refreshes. Keep the listing visible so
                // consecutive clicks can cycle all three states; a hit opens live memory.
                SwingUtilities.invokeLater(() -> {
                    if (!disposed) applyBreakpointRequest(new BreakpointDialog.Request(
                            state == BreakpointState.OFF ? BreakpointDialog.Operation.UNSET : BreakpointDialog.Operation.SET,
                            address, instructionBank(), state == BreakpointState.KEEP_INSPECTION), false);
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
        int rowAt(int at) { return rows == null ? (at < getRowCount() ? at : -1) : rows.indexOf(at); }
        @Override public int getRowCount() { return rows == null ? (session.memory == null ? 0 : session.memory.getSize()) : rows.size(); }
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
            guard(() -> navigate(address, bank, true, NavigationTarget.SOURCE_INSTRUCTION));
        }

        @Override public String getToolTipText(MouseEvent event) {
            for (Map.Entry<Integer, Rectangle> entry : boxes.entrySet())
                if (entry.getValue().contains(event.getPoint()))
                    return String.format("%04X · %s", entry.getKey(), flow.listing.get(entry.getKey()));
            return "Arrow keys select an instruction; Enter follows it without changing the CPU";
        }

        void setFlow(FlowAnalysis flow) {
            this.flow = flow;
            bank = selectedBank();
            common = session.memory == null ? 0 : session.memory.getCommonBoundary();
            boxes.clear();
            int i = 0;
            for (int at : flow.instructions.keySet()) {
                boxes.put(at, new Rectangle(25 + (i % 3) * 240, 25 + (i / 3) * 100, 210, 60));
                i++;
            }
            setPreferredSize(new Dimension(760, Math.max(400, ((i + 2) / 3) * 100 + 50)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (flow == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (FlowAnalysis.Edge e : flow.edges) {
                Rectangle a = boxes.get(e.from), b = boxes.get(e.to);
                if (a == null || b == null) continue;
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
                if (!e.kind.equals("next")) {
                    int labelX = (x + tx) / 2, labelY = (y + ty) / 2;
                    g.setColor(getBackground());
                    g.fillRect(labelX - 4, labelY - 12, g.getFontMetrics().stringWidth(e.kind) + 8, 16);
                    g.setColor(getForeground());
                    g.drawString(e.kind, labelX, labelY);
                }
            }
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
                String text = flow.listing.get(entry.getKey());
                g.drawString(
                        text.length() > 28 ? text.substring(0, 28) : text, box.x + 8, box.y + 42);
            }
            g.dispose();
        }
    }
}
