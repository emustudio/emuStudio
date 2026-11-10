/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.FlowAnalysis;
import net.emustudio.application.emulation.debugger.InstructionRecording;
import net.emustudio.application.emulation.debugger.SourceAddressMap;
import net.emustudio.application.emulation.debugger.UserAnnotations;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Operation;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Selection;
import net.emustudio.application.gui.dialogs.BreakpointDialog;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;
import static net.emustudio.emulib.ui.StandardIcons.ICON_SNAPSHOT;

/**
 * Unified debugger workspace over CPU-visible addresses; browsing never changes PC.
 * <p>
 * Every emulator, memory and annotation event only schedules one coalesced {@link #sync()} on the EDT,
 * which reads the current state and brings all views up to date.
 */
public final class DebuggerPanel extends JPanel implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggerPanel.class);

    @FunctionalInterface public interface AddressSelectionListener {
        void selected(int address, boolean visit);
    }

    private AddressSelectionListener addressSelectionListener = (address, visit) -> {};
    private Runnable inspectionReadyListener = () -> {};
    private Supplier<List<SourceAddressMap.Symbol>> compiledSymbols = List::of;
    private boolean sourceMappingsCurrent;
    private final DebuggerSession session;
    private final EmuStudioGui gui;
    private final Consumer<SourceCodePosition> sourceNavigator;
    private final Runnable breakpointsChanged;
    private final DebugTableImpl table;
    private final DebugTableModel instructionModel;
    private final JComponent instructionView;
    private final Selection<MemoryAnnotations.Entry> annotationSelection =
            new Selection<>((first, second) -> first.entryId == second.entryId);
    private final FlowGraphPanel graph;
    private final WatchesPanel watchesPanel;
    private final RecordingPanel recordingPanel;
    private final AnnotationEditor annotationEditor;
    private AnnotationSearchPanel search;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<JComponent, BooleanSupplier> enablers = new LinkedHashMap<>();
    private JToggleButton followPc;
    private boolean disposed, running, pcMoved, readyPending = true;
    private int inspectedAddress, lastInstructionAddress;
    private CPU.RunState appliedState;
    private int appliedPc;
    private long appliedAnnotationRevision = -1;

    private final AtomicBoolean syncQueued = new AtomicBoolean();
    private final Runnable annotationListener = this::changed;
    private final Consumer<CPU.RunState> executionListener = state -> changed();
    private final MemoryContext.MemoryListener memoryListener = new MemoryContext.MemoryListener() {
        @Override public void memoryContentChanged(int from, int to) { changed(); }
        @Override public void memorySizeChanged() { changed(); }
    };
    private final CPUContext.CPUListener stateListener = new CPUContext.CPUListener() {
        @Override public void runStateChanged(CPU.RunState state) { changed(); }
        @Override public void internalStateChanged() { changed(); }
    };

    public DebuggerPanel(
            DebuggerSession session,
            DebugTableImpl table,
            JComponent instructionView,
            Consumer<SourceCodePosition> sourceNavigator,
            Runnable breakpointsChanged,
            EmuStudioGui gui) {
        this.session = Objects.requireNonNull(session);
        this.gui = Objects.requireNonNull(gui);
        this.table = Objects.requireNonNull(table);
        this.instructionView = Objects.requireNonNull(instructionView);
        this.sourceNavigator = sourceNavigator;
        this.breakpointsChanged = Objects.requireNonNull(breakpointsChanged);
        instructionModel = table.instructionModel();
        lastInstructionAddress = session.execution().cpuContext().getInstructionLocation();
        inspectedAddress = lastInstructionAddress;
        instructionModel.setDebuggerSession(session);
        graph = new FlowGraphPanel(session, annotationSelection,
                () -> sourceMappingsCurrent ? compiledSymbols.get() : List.of(),
                (address, navigate) -> guard(() -> {
                    if (navigate) navigate(address);
                    else inspectAddress(address, false, () -> selectListingInstruction(address));
                }), this::editAnnotation);
        graph.addPropertyChangeListener("selectedNode", event -> updateActions());
        watchesPanel = new WatchesPanel(gui, session, this::live, this::submitUpdate);
        recordingPanel = new RecordingPanel(gui, session);
        annotationEditor = new AnnotationEditor(gui, this, session);
        instructionModel.setPresentation(this::sourceLabelsAt, () -> !disposed && !running,
                () -> !disposed && !running && !session.execution().isBusy());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setInstructionRowPresentation(row -> {
            if (disposed || running) return false;
            int address = instructionModel.getLocationAt(row);
            return address >= 0 && session.isBreakpointSet(address);
        }, row -> disposed || running ? null : annotationTooltip(instructionModel.getLocationAt(row)),
                this::selectInstructionAddress);
        table.setRowBackground(row -> disposed || running ? null : annotationBackground(instructionModel.getLocationAt(row)));
        enabledWhen(table, () -> !running);
        setLayout(new BorderLayout(6, 6));
        tabs.setName("debuggerViews");
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Debugger", instructionView);
        tabs.addTab("Inspection", flowPanel());
        tabs.addTab("Recording", EmuStudioGui.loadIcon(ICON_SNAPSHOT, DebuggerPanel.class), recordingPanel);
        add(tabs, BorderLayout.CENTER);
        tabs.addChangeListener(event -> activateView());
        activateView();
        session.execution().cpuContext().addCPUListener(stateListener);
        session.addExecutionListener(executionListener);
        session.annotationEngine.addListener(annotationListener);
        if (session.memory != null) session.memory.addMemoryListener(memoryListener);
        setMinimumSize(new Dimension(300, 160));
        installSelectionMenu();
        changed();
    }

    private void installSelectionMenu() {
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
        selectionMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                boolean enabled = isInstructionView() && table.getSelectedRow() >= 0 && !running;
                inspect.setEnabled(enabled);
                goToSource.setEnabled(enabled);
                int address = enabled ? selectedRowAddress() : -1;
                toggleBreakpoint.setEnabled(address >= 0 && isBreakpointDialogSupported());
                toggleBreakpoint.setText(address >= 0 && session.isBreakpointSet(address)
                        ? "Unset breakpoint" : "Set breakpoint");
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {}
            public void popupMenuCanceled(PopupMenuEvent event) {}
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
    }

    public JComponent watchesPanel() {
        return watchesPanel;
    }

    public void setAddressSelectionListener(AddressSelectionListener listener) {
        addressSelectionListener = Objects.requireNonNull(listener);
    }

    public void setInspectionReadyListener(Runnable listener) {
        inspectionReadyListener = Objects.requireNonNull(listener);
    }

    public void setSourceMappingsCurrent(boolean current) {
        if (sourceMappingsCurrent == current) return;
        sourceMappingsCurrent = current;
        repaintLabels();
    }

    public void setSymbols(Supplier<List<SourceAddressMap.Symbol>> values) {
        compiledSymbols = Objects.requireNonNull(values);
        repaintLabels();
    }

    private void repaintLabels() {
        graph.labelsChanged();
        table.repaint();
    }

    /** Automatic source following keeps the active analysis tab. */
    public void synchronizeSourceAddress(SourceAddressMap.Location location) {
        if (!location.instruction) return;
        alignInstructionAddress(location.address);
        inspectAddress(location.address, true, () -> {
            graph.reveal(location.address);
            selectListingInstruction(location.address);
        });
    }

    public void alignInstructionAddress(int address) {
        if (lastInstructionAddress == address) return;
        lastInstructionAddress = address;
        instructionModel.showAddress(address);
    }

    public void stepCompleted() {
        if (!disposed) changed();
    }

    void addRecording(List<InstructionRecording.Event> captured, String scope) {
        recordingPanel.addRecording(captured, scope);
    }

    @Override
    public void close() {
        if (disposed) return;
        disposed = true;
        recordingPanel.close();
        if (search != null) search.close();
        session.execution().cpuContext().removeCPUListener(stateListener);
        session.removeExecutionListener(executionListener);
        session.annotationEngine.removeListener(annotationListener);
        if (session.memory != null) session.memory.removeMemoryListener(memoryListener);
    }

    /** Called from any thread; schedules one {@link #sync()} for any number of changes. */
    private void changed() {
        if (syncQueued.compareAndSet(false, true)) SwingUtilities.invokeLater(() -> {
            syncQueued.set(false);
            sync();
        });
    }

    /** Brings all views up to date with the emulator state; runs on the EDT. */
    private void sync() {
        if (disposed) return;
        annotations();
        watchesPanel.refresh();
        long annotationRevision = session.annotationEngine.revision();
        if (annotationRevision != appliedAnnotationRevision) {
            appliedAnnotationRevision = annotationRevision;
            if (search != null) search.refresh();
            repaintLabels();
        }
        if (session.execution().isStepping()) return;
        CPU.RunState state = session.execution().getRunState();
        int pc = session.execution().cpuContext().getInstructionLocation();
        pcMoved |= state != appliedState || pc != appliedPc;
        appliedState = state;
        appliedPc = pc;
        if (running != (state == CPU.RunState.STATE_RUNNING)) {
            running = !running;
            updateActions();
        }
        if (running || session.execution().isBusy() || !isPaused()) return;
        if (!inspectBreakpointHit() && pcMoved) followPc(pc);
        pcMoved = false;
        recordingPanel.updateControls();
        if (tabs.getSelectedComponent() == recordingPanel) {
            try { recordingPanel.refresh(); } catch (IllegalArgumentException failure) { logFailure(LOGGER, failure); }
        }
        table.repaint();
        if (followPc.isSelected() && graph.hasFlow()) {
            if (!graph.current() || graph.selectedNode() != pc) {
                guard(() -> analyzeFlow(graph.start(), true, pc));
            } else if (graph.foldControl().isSelected() && graph.layoutPc() != pc) graph.relayout();
        }
        if (readyPending) ready();
    }

    private void ready() {
        readyPending = false;
        inspectionReadyListener.run();
    }

    private void followPc(int pc) {
        lastInstructionAddress = pc;
        instructionModel.currentPage();
        if (pc >= 0) guard(() -> inspectAddress(pc, false, () -> {
            if (table.getSelectedRow() >= 0) selectInstructionAddress();
        }));
    }

    private boolean inspectBreakpointHit() {
        DebuggerSession.BreakpointHit hit = session.takeBreakpointHit();
        if (hit == null) return false;
        instructionModel.currentPage();
        lastInstructionAddress = session.execution().cpuContext().getInstructionLocation();
        showInstructions();
        guard(() -> navigate(hit.address));
        return true;
    }

    private boolean isPaused() {
        try {
            session.requirePaused();
            return true;
        } catch (IllegalStateException notPaused) {
            return false;
        }
    }

    /** Runs a user action that needs a paused CPU; failures are reported. */
    private void guard(Operation operation) {
        if (disposed || session.execution().isBusy()) return;
        try {
            session.requirePaused();
            operation.run();
        } catch (Exception e) {
            logFailure(LOGGER, e);
        }
    }

    /** Runs a user action that does not require a paused CPU; failures are reported. */
    private void live(Operation operation) {
        if (disposed) return;
        try { operation.run(); } catch (Exception failure) { logFailure(LOGGER, failure); }
    }

    /** Applies a settings change; the session applies it immediately, even while the CPU runs. */
    private void submitUpdate(Operation operation) {
        live(() -> session.update(() -> {
            try { operation.run(); }
            catch (Exception failure) { throw new CompletionException(failure); }
        }).join());
        if (disposed) return;
        watchesPanel.refresh();
        recordingPanel.refresh();
    }

    private <C extends JComponent> C enabledWhen(C component, BooleanSupplier enabled) {
        enablers.put(component, enabled);
        return component;
    }

    private void updateActions() {
        enablers.forEach((component, enabled) -> component.setEnabled(enabled.getAsBoolean()));
        if (search != null) search.updateState();
        recordingPanel.updateControls();
        annotationEditor.updateState();
        graph.repaint();
    }

    private JButton inspectionButton(String icon, String tooltip, Operation operation) {
        return gui.toolbarButton(event -> guard(operation),
                "/net/emustudio/application/gui/icons/inspection/" + icon + ".png", tooltip);
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
        int address = isInstructionView() ? selectedRowAddress() : -1;
        if (address < 0) throw new IllegalStateException("Select an instruction first");
        return address;
    }

    /** Address of the selected listing row, or -1 when no row (or an empty row) is selected. */
    private int selectedRowAddress() {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : instructionModel.getLocationAt(table.convertRowIndexToModel(row));
    }

    private void activateView() {
        int previous = inspectedAddress;
        if (search != null) search.hideResults();
        if (tabs.getSelectedComponent() == recordingPanel) recordingPanel.refresh();
        if (isInstructionView()) {
            inspectedAddress = lastInstructionAddress;
            if (table.getSelectedRow() >= 0) selectInstructionAddress();
            table.getAccessibleContext().setAccessibleName("Instruction listing");
        }
        updateActions();
        revalidate();
        repaint();
        if (inspectedAddress != previous) notifyAddressSelection(false);
    }

    public boolean isBreakpointDialogSupported() {
        return session.debug != null;
    }

    public void showBreakpointDialog(JFrame parent) {
        if (!isBreakpointDialogSupported()) return;
        BreakpointDialog dialog = new BreakpointDialog(parent, gui, inspectedAddress,
                session.cpu.getAddressSpaceSize(), session::isBreakpointSet,
                this::applyBreakpointRequest);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    boolean applyBreakpointRequest(BreakpointDialog.Request request) {
        try {
            if (request.address < 0 || request.address >= session.cpu.getAddressSpaceSize())
                throw new IllegalArgumentException("Address outside CPU");
            session.setBreakpoint(request.address, request.operation == BreakpointDialog.Operation.SET).join();
            breakpointsChanged.run();
            session.config.save();
            sync();
            return true;
        } catch (Exception e) {
            logFailure(LOGGER, e);
            return false;
        }
    }

    /** Follows a listing selection made outside the panel. */
    public void followAddress(int at) {
        if (at != inspectedAddress) guard(() -> navigate(at));
    }

    public void navigate(int at) {
        inspectAddress(at, true, () -> {
            graph.reveal(at);
            selectListingInstruction(at);
            showInstructions();
        });
    }

    private void selectListingInstruction(int at) {
        lastInstructionAddress = at;
        inspectedAddress = at;
        instructionModel.showAddress(at);
        selectInstructionAddress();
    }

    private void selectInstructionAddress() {
        if (running || disposed) return;
        for (int row = 0; row < instructionModel.getRowCount(); row++) {
            if (instructionModel.getLocationAt(row) == lastInstructionAddress) {
                table.setRowSelectionInterval(row, row);
                return;
            }
        }
    }

    /** Moves inspection to the address, lets the views follow, then notifies listeners (visit = user navigation). */
    private void inspectAddress(int at, boolean visit, Runnable updateViews) {
        session.requirePaused();
        if (at < 0 || at >= session.cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside memory");
        inspectedAddress = at;
        updateViews.run();
        notifyAddressSelection(visit);
    }

    private void notifyAddressSelection(boolean visit) {
        graph.repaint();
        if (!disposed && !running) addressSelectionListener.selected(inspectedAddress, visit);
    }

    private void showSource(int at) {
        session.sourceAt(at)
                .ifPresentOrElse(sourceNavigator, () -> {
                    throw new IllegalStateException("No current source mapping at this address");
                });
    }

    private JPanel flowPanel() {
        JPanel panel = gui.panel("", "", "");
        panel.setLayout(new BorderLayout());
        JPanel tools = gui.toolbarPanel();
        JToolBar actions = gui.toolbarRow(tools);
        String followTooltip = "Show instructions from PC";
        followPc = enabledWhen(gui.toolbarToggleButton(event -> {},
                "/net/emustudio/application/gui/icons/inspection/follow-pc.png", followTooltip), () -> !running);
        followPc.addActionListener(event -> {
            updateActions();
            if (followPc.isSelected()) guard(() -> {
                int pc = session.execution().cpuContext().getInstructionLocation();
                analyzeFlow(pc, true, pc);
            });
        });
        actions.add(followPc);
        actions.add(enabledWhen(inspectionButton("refresh-flow", "Refresh instruction flow", this::refreshFlowFromSelection),
                () -> !running && !followPc.isSelected()));
        actions.addSeparator();
        actions.add(enabledWhen(inspectionButton("go-to-instruction", "Go to instruction", graph::followSelectedNode),
                () -> !running && graph.hasSelectedNode()));
        actions.add(Box.createHorizontalGlue(), "pushx, growx");
        actions.add(enabledWhen(graph.foldControl(), () -> !running));
        actions.add(graph.zoomControl());
        if (session.cpu.getAddressSpaceSize() > 0) {
            JToolBar flowAnnotations = gui.toolbarRow(tools);
            search = new AnnotationSearchPanel(gui, session, annotationEditor, annotationSelection, flowAnnotations,
                    () -> inspectedAddress, entry -> inspectAddress(entry.start, true, () -> graph.reveal(entry.start)));
        }
        panel.add(gui.toolbarScrollPane(tools), BorderLayout.NORTH);
        panel.add(graph.scrollPane(gui), BorderLayout.CENTER);
        return panel;
    }

    private void refreshFlowFromSelection() {
        int address = table.getSelectedRow() >= 0 ? selectedRowAddress()
                : graph.selectedNode() >= 0 ? graph.selectedNode() : lastInstructionAddress;
        analyzeFlow(address);
    }

    private void analyzeFlow(int address) {
        followPc.setSelected(false);
        updateActions();
        analyzeFlow(address, false, address);
    }

    private void analyzeFlow(int address, boolean fromPc, int selection) {
        FlowAnalysis flow = session.analyzeFlow(address);
        graph.setFlow(flow, address, selection);
        updateActions();
        if (fromPc) graph.selectNode(selection);
        ready();
    }

    /** User annotations, or null when the CPU has no address space; recreated after a memory size change. */
    private UserAnnotations annotations() {
        return session.cpu.getAddressSpaceSize() > 0 ? session.config.annotations() : null;
    }

    private List<MemoryAnnotations.Entry> annotationsCovering(int address) {
        if (address < 0 || session.cpu.getAddressSpaceSize() < 1 || address >= session.addressSpaceSize())
            return List.of();
        return AnnotationEditor.userAnnotations(session.annotationEngine.at(address));
    }

    private String annotationTooltip(int address) {
        String tooltip = annotationsCovering(address).stream()
                .map(entry -> entry.text.trim())
                .map(text -> text.isEmpty() ? "Annotation" : text)
                .collect(Collectors.joining(" | "));
        return tooltip.isEmpty() ? null : tooltip;
    }

    private Color annotationBackground(int address) {
        List<MemoryAnnotations.Entry> entries = annotationsCovering(address);
        if (entries.isEmpty()) return null;
        Color background = graph.getBackground();
        if (background == null) background = table.getBackground();
        for (MemoryAnnotations.Entry entry : entries) {
            Color lane = FlowGraphPanel.laneColor(entry);
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
                .collect(Collectors.joining(", "));
    }

    private void editAnnotation(UserAnnotations.Entry entry) {
        live(() -> annotationEditor.showDialog(entry.address,
                entry.id < 0 ? "New annotation" : "Edit annotation", entry));
    }

}
