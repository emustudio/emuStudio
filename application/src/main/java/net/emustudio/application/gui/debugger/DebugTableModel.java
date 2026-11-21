/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.FlowAnalysis;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Centered live listing; preceding addresses come from the shared execution observations.
 */
public class DebugTableModel extends AbstractTableModel {
    // Titles of columns created by DebuggerColumn.address/mnemo/opcode factories
    static final String TITLE_LABEL = "label";
    static final String TITLE_ADDRESS = "address";
    static final String TITLE_MNEMO = "instruction";
    static final String TITLE_OPCODE = "opcode";
    static final String TITLE_FLOW = "flow";

    private DebuggerColumn<?>[] columns = new DebuggerColumn[0];
    private IntFunction<String> labels = address -> "";
    private BooleanSupplier valuesVisible = () -> true;
    private BooleanSupplier editingAllowed = () -> true;
    private CPU cpu;
    private CPUContext cpuContext;
    private DebuggerSession session;
    private Integer inspectedLocation;
    private int maxRows = 21;
    private int pageIndex;
    private long rowOffset;
    private int cachedAnchor = -1;
    private long programRevision = -1;
    private InstructionRow[] rows;
    private boolean foldRepeats;
    private boolean foldsDirty = true;
    private long observationsRevision = -1;
    private int cachedPc = -1;
    private Supplier<Set<Integer>> foldBoundaries = Set::of;
    private final NavigableMap<Integer, InstructionRow> runsByStart = new TreeMap<>();
    private final Set<Integer> expandedInstructions = new HashSet<>();
    // No size limit; only changes to the memory view invalidate decoded presentation.
    private final Map<Integer, DecodedInstruction> instructions = new HashMap<>();
    private final Map<Integer, InvalidInstructionException> invalidInstructions = new HashMap<>();
    private final Decoder rowDecoder = this::decode;

    public void setCPU(CPU cpu, CPUContext cpuContext) {
        this.cpu = Objects.requireNonNull(cpu);
        this.cpuContext = Objects.requireNonNull(cpuContext);
        inspectedLocation = null;
        pageIndex = 0;
        rowOffset = 0;
        expandedInstructions.clear();
        clearDecodedInstructions();
        List<DebuggerColumn<?>> provided = cpu.getDebuggerColumns(rowDecoder);
        setDebuggerColumns(provided.isEmpty() ? defaultColumns(rowDecoder) : provided);
    }

    public void setDebuggerSession(DebuggerSession session) {
        this.session = Objects.requireNonNull(session);
        expandedInstructions.clear();
        programRevision = -1;
        clearDecodedInstructions();
    }

    public void setMaxRows(int rowCount) {
        int size = Math.max(1, rowCount);
        if (maxRows == size) return;
        if (cpu != null && (pageIndex != 0 || rowOffset != 0)) {
            materializePage();
            int center = getLocationAt(maxRows / 2);
            if (center >= 0) inspectedLocation = center;
            pageIndex = 0;
            rowOffset = 0;
            foldsDirty = true;
        }
        maxRows = size;
        rows = null;
        materializePage();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return cpu == null ? 0 : maxRows;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    public DebuggerColumn<?> getColumnAt(int index) {
        return columns[index];
    }

    @Override
    public String getColumnName(int column) {
        return columns[column].getTitle();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns[column].getClassType();
    }

    int flowColumnIndex() {
        return columns.length - 1;
    }

    List<FlowAnalysis.Edge> observedEdges(int row) {
        if (session == null || !valuesVisible.getAsBoolean()) return List.of();
        InstructionRow run = rowAt(row);
        if (run == null) return List.of();
        if (!run.folded()) return session.observedEdges(run.first());
        List<FlowAnalysis.Edge> edges = new ArrayList<>();
        for (int address : List.of(run.first(), run.last())) {
            for (FlowAnalysis.Edge edge : session.observedEdges(address)) {
                if (edge.from != edge.to && run.contains(edge.from) && run.contains(edge.to)) continue;
                edges.add(edge);
            }
        }
        return edges;
    }

    private int viewLocation() {
        return inspectedLocation == null ? cpuContext.getInstructionLocation() : inspectedLocation;
    }

    private void clearDecodedInstructions() {
        instructions.clear();
        invalidInstructions.clear();
        rows = null;
        foldsDirty = true;
    }

    private void refreshRevision() {
        long revision = session == null ? 0 : session.programRevision();
        if (revision != programRevision) {
            programRevision = revision;
            clearDecodedInstructions();
        }
    }

    private List<Integer> instructionsBefore(int address, int count) {
        if (count == 0) return List.of();
        int pc = cpuContext.getInstructionLocation();
        Deque<Integer> previous = new ArrayDeque<>(session == null ? List.of()
                : session.observedInstructionsBefore(Math.min(address, pc), count));
        // Extend observed history through the browsed, unexecuted instructions.
        // Only this displayed window is shortened; decoded instructions remain cached.
        for (int at = pc; at < address; at = nextAddress(at)) {
            previous.addLast(at);
            if (previous.size() > count) previous.removeFirst();
        }
        return new ArrayList<>(previous);
    }

    private void materializePage() {
        if (cpu == null || !valuesVisible.getAsBoolean()) return;
        refreshRevision();
        int anchor = viewLocation();
        int pc = cpuContext.getInstructionLocation();
        long observedRevision = session == null ? 0 : session.observedFlowRevision();
        if (cachedAnchor != anchor || cachedPc != pc || observationsRevision != observedRevision) {
            foldsDirty = true;
            rows = null;
        }
        if (rows != null) return;
        if (foldsDirty) rebuildFolds(anchor, pc);
        InstructionRow[] locations = new InstructionRow[maxRows];
        if (anchor >= 0 && anchor < cpu.getAddressSpaceSize()) {
            long first = (long) pageIndex * Math.max(1, maxRows - 1) + rowOffset - maxRows / 2;
            long end = first + maxRows;
            if (first < 0) {
                List<InstructionRow> previous = rowsBefore(anchor, (int) Math.min(Integer.MAX_VALUE, -first));
                for (int i = 0; i < previous.size(); i++) {
                    long row = (long) i - previous.size() - first;
                    if (row >= 0 && row < maxRows) locations[(int) row] = previous.get(i);
                }
            }
            int address = anchor;
            for (long slot = 0; slot < end && address < cpu.getAddressSpaceSize(); slot++) {
                InstructionRow run = listingRow(address);
                if (slot >= first) locations[(int) (slot - first)] = run;
                address = nextListingAddress(run.last());
            }
        }
        rows = locations;
        cachedAnchor = anchor;
        cachedPc = pc;
        observationsRevision = observedRevision;
    }

    private List<InstructionRow> rowsBefore(int anchor, int count) {
        List<Integer> addresses = instructionsBefore(anchor, foldRepeats ? Integer.MAX_VALUE : count);
        List<InstructionRow> previous = new ArrayList<>();
        for (int i = 0; i < addresses.size();) {
            InstructionRow run = listingRow(addresses.get(i));
            previous.add(run);
            i += run.members.size();
        }
        return previous.subList(Math.max(0, previous.size() - count), previous.size());
    }

    private InstructionRow listingRow(int address) {
        InstructionRow run = repeatRunContaining(address);
        if (run == null || expandedInstructions.contains(address)) return new InstructionRow(List.of(address));
        int index = Collections.binarySearch(run.members, address);
        return index == 0 ? run : new InstructionRow(run.members.subList(index, run.members.size()));
    }

    private InstructionRow repeatRunContaining(int address) {
        if (!foldRepeats) return null;
        Map.Entry<Integer, InstructionRow> entry = runsByStart.floorEntry(address);
        return entry != null && entry.getValue().contains(address) ? entry.getValue() : null;
    }

    private void rebuildFolds(int anchor, int pc) {
        runsByStart.clear();
        foldsDirty = false;
        if (!foldRepeats || session == null) return;
        NavigableSet<Integer> boundaries = new TreeSet<>(foldBoundaries.get());
        boundaries.add(anchor);
        boundaries.add(pc);
        AnnotationEditor.userAnnotations(session.annotationEngine.all()).forEach(entry -> {
            boundaries.add(entry.start);
            boundaries.add(entry.end);
        });
        for (List<Integer> candidate : session.observedInstructionRuns()) {
            List<Integer> run = new ArrayList<>();
            DecodedInstruction previous = null;
            boolean previousBoundary = false;
            for (int address : candidate) {
                DecodedInstruction instruction = null;
                if (address >= 0 && address < cpu.getAddressSpaceSize()) {
                    try { instruction = decode(address); } catch (InvalidInstructionException ignored) { }
                }
                Integer boundary = boundaries.ceiling(address);
                boolean protectedAddress = instruction == null || session.isBreakpointSet(address)
                        || !labels.apply(address).isBlank()
                        || (boundary != null && boundary < (long) address + instruction.length());
                if (protectedAddress || previousBoundary || !FlowAnalysis.identicalAdjacentInstructions(previous, instruction)) {
                    addRun(run);
                    run = new ArrayList<>();
                }
                run.add(address);
                previous = instruction;
                previousBoundary = protectedAddress;
            }
            addRun(run);
        }
    }

    private void addRun(List<Integer> members) {
        if (members.size() > 1) runsByStart.put(members.get(0), new InstructionRow(members));
    }

    public boolean isFoldRepeats() { return foldRepeats; }

    public void setFoldRepeats(boolean fold) {
        if (foldRepeats == fold || !valuesVisible.getAsBoolean() || !editingAllowed.getAsBoolean()) return;
        // Preserve the browsed page's center when the number of displayed rows changes.
        if (cpu != null && (pageIndex != 0 || rowOffset != 0)) {
            int center = getLocationAt(maxRows / 2);
            if (center >= 0) inspectedLocation = center;
        }
        foldRepeats = fold;
        expandedInstructions.clear();
        foldsDirty = true;
        showPage(0);
    }

    public void setFoldBoundaries(Supplier<Set<Integer>> boundaries) {
        foldBoundaries = Objects.requireNonNull(boundaries);
        refreshFolding();
    }

    /** Metadata and breakpoint updates can change fold boundaries without changing memory. */
    public void refreshFolding() {
        if (!foldRepeats || !valuesVisible.getAsBoolean() || !editingAllowed.getAsBoolean()) return;
        foldsDirty = true;
        rows = null;
        materializePage();
        fireTableDataChanged();
    }

    public boolean isFolded(int row) {
        if (!foldRepeats || !valuesVisible.getAsBoolean()) return false;
        InstructionRow run = rowAt(row);
        return run != null && run.folded();
    }

    boolean hasFoldControl(int row) {
        if (!foldRepeats || !valuesVisible.getAsBoolean()) return false;
        int address = getLocationAt(row);
        InstructionRow run = repeatRunContaining(address);
        return run != null && (isFolded(row) || expandedInstructions.contains(address));
    }

    public void toggleFoldAt(int row) {
        if (!valuesVisible.getAsBoolean() || !editingAllowed.getAsBoolean()) return;
        int address = getLocationAt(row);
        if (!hasFoldControl(row)) return;
        InstructionRow run = repeatRunContaining(address);
        if (expandedInstructions.contains(address)) expandedInstructions.removeAll(run.members);
        else expandedInstructions.addAll(run.members);
        long relativeRow = relativeRowOf(address);
        if (relativeRow != Long.MIN_VALUE)
            rowOffset = relativeRow - row + maxRows / 2 - (long) pageIndex * Math.max(1, maxRows - 1);
        rows = null;
        materializePage();
        fireTableDataChanged();
    }

    /** Keeps the clicked run in place when expanding instructions before the listing anchor. */
    private long relativeRowOf(int address) {
        int anchor = viewLocation();
        if (address < anchor) {
            List<InstructionRow> previous = rowsBefore(anchor, Integer.MAX_VALUE);
            for (int i = 0; i < previous.size(); i++)
                if (previous.get(i).contains(address)) return (long) i - previous.size();
        } else {
            long row = 0;
            for (int at = anchor; at < cpu.getAddressSpaceSize() && at <= address; row++) {
                InstructionRow run = listingRow(at);
                if (run.contains(address)) return row;
                at = nextListingAddress(run.last());
            }
        }
        return Long.MIN_VALUE;
    }

    boolean rowContains(int row, int address) {
        InstructionRow run = rowAt(row);
        return run != null && run.contains(address);
    }

    int rowForAddress(int address) {
        materializePage();
        if (rows != null) for (int row = 0; row < rows.length; row++)
            if (rows[row] != null && rows[row].contains(address)) return row;
        return -1;
    }

    String foldTooltip(int row) {
        if (!hasFoldControl(row)) return null;
        return isFolded(row) ? "Expand repeated instructions (Right arrow)" : "Collapse repeated instructions (Left arrow)";
    }

    private InstructionRow rowAt(int row) {
        materializePage();
        return rows == null || row < 0 || row >= rows.length ? null : rows[row];
    }

    private static final class InstructionRow {
        final List<Integer> members;
        InstructionRow(List<Integer> members) { this.members = List.copyOf(members); }
        int first() { return members.get(0); }
        int last() { return members.get(members.size() - 1); }
        boolean folded() { return members.size() > 1; }
        boolean contains(int address) { return Collections.binarySearch(members, address) >= 0; }
    }

    private DecodedInstruction decode(int address) throws InvalidInstructionException {
        InvalidInstructionException failure = invalidInstructions.get(address);
        if (failure != null) throw failure;
        DecodedInstruction instruction = instructions.get(address);
        if (instruction == null) {
            try {
                instruction = cpu.getDecoder().decode(address);
                instructions.put(address, instruction);
            } catch (InvalidInstructionException invalid) {
                invalidInstructions.put(address, invalid);
                throw invalid;
            }
        }
        return instruction;
    }

    private int nextAddress(int address) {
        int length = 1;
        try {
            length = decode(address).length();
        } catch (InvalidInstructionException invalid) { /* Invalid cells remain individually browsable. */ }
        return (int) Math.min(cpu.getAddressSpaceSize(), (long) address + length);
    }

    private int nextListingAddress(int address) {
        int pc = cpuContext.getInstructionLocation();
        if (address < pc) {
            int observed = session == null ? -1 : session.nextObservedInstruction(address);
            return observed >= 0 && observed < pc ? observed : pc;
        }
        return nextAddress(address);
    }

    public void previousPage() {
        seekBackwardPage(1);
    }

    public void nextPage() {
        seekForwardPage(1);
    }

    public void seekBackwardPage(int pages) {
        seekPages(pages, -1);
    }

    public void seekForwardPage(int pages) {
        seekPages(pages, 1);
    }

    private void seekPages(int pages, int direction) {
        if (cpu == null || !valuesVisible.getAsBoolean()) return;
        for (int page = 0; page < pages; page++) {
            pageIndex += direction;
            rows = null;
            materializePage();
            if (Arrays.stream(rows).allMatch(Objects::isNull)) {
                pageIndex -= direction;
                rows = null;
                break;
            }
        }
        fireTableDataChanged();
    }

    public void firstPage() {
        if (cpu == null || !valuesVisible.getAsBoolean()) return;
        refreshRevision();
        materializePage();
        List<InstructionRow> previous = rowsBefore(viewLocation(), Integer.MAX_VALUE);
        showPage(Math.floorDiv(maxRows / 2 - previous.size(), Math.max(1, maxRows - 1)));
    }

    public void currentPage() {
        inspectedLocation = null;
        showPage(0);
    }

    public void showAddress(int address) {
        if (cpu == null) return;
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        inspectedLocation = address;
        showPage(0);
    }

    private void showPage(int index) {
        pageIndex = index;
        rowOffset = 0;
        rows = null;
        materializePage();
        fireTableDataChanged();
    }

    public int getLocationAt(int row) {
        InstructionRow run = rowAt(row);
        return run == null ? -1 : run.first();
    }

    public boolean isRowAtCurrentInstruction(int row) {
        return cpu != null && valuesVisible.getAsBoolean()
                && getLocationAt(row) == cpuContext.getInstructionLocation();
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (cpu == null || !valuesVisible.getAsBoolean()) return null;
        int address = getLocationAt(row);
        if (address < 0) return column == 0 ? ""
                : columns[column].getClassType() == Boolean.class ? Boolean.FALSE : null;
        Object value = columns[column].getValue(address);
        if (isFolded(row) && columns[column].getClassType() == String.class) {
            InstructionRow run = rowAt(row);
            if (TITLE_ADDRESS.equals(getColumnName(column)))
                return value + "–" + columns[column].getValue(run.last());
            if (TITLE_MNEMO.equals(getColumnName(column))) return value + " × " + run.members.size();
        }
        return value;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return cpu != null && editingAllowed.getAsBoolean()
                && columns[column].isEditable() && getLocationAt(row) >= 0 && !isFolded(row);
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (!isCellEditable(row, column)) return;
        int address = getLocationAt(row);
        if (address < 0 || !columns[column].getClassType().isInstance(value)) return;
        try {
            columns[column].setValue(address, value);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void memoryChanged(int from, int to) {
        clearDecodedInstructions();
        materializePage();
        fireTableDataChanged();
    }

    public int guessPreviousInstructionLocation() {
        if (cpu == null) return 0;
        refreshRevision();
        int pc = cpuContext.getInstructionLocation();
        List<Integer> previous = instructionsBefore(pc, 1);
        return previous.isEmpty() ? pc : previous.get(0);
    }

    public void setDebuggerColumns(List<DebuggerColumn<?>> columns) {
        List<DebuggerColumn<?>> displayed = new ArrayList<>();
        displayed.add(DebuggerColumn.text(TITLE_LABEL, address -> labels.apply(address)));
        displayed.addAll(columns);
        displayed.add(DebuggerColumn.text(TITLE_FLOW, address -> ""));
        this.columns = displayed.toArray(new DebuggerColumn[0]);
        fireTableStructureChanged();
    }

    public void setPresentation(IntFunction<String> labels, BooleanSupplier valuesVisible,
                                BooleanSupplier editingAllowed) {
        this.labels = Objects.requireNonNull(labels);
        this.valuesVisible = Objects.requireNonNull(valuesVisible);
        this.editingAllowed = Objects.requireNonNull(editingAllowed);
        refreshFolding();
    }

    static List<DebuggerColumn<?>> defaultColumns(Decoder decoder) {
        return List.of(DebuggerColumn.address("%04X"), DebuggerColumn.mnemo(decoder), DebuggerColumn.opcode(decoder));
    }
}
