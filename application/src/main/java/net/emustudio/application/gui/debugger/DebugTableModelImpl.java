/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/** Centered live listing; preceding addresses come from the shared execution observations. */
public class DebugTableModelImpl extends DebugTableModel {
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
    private int cachedAnchor = -1;
    private long programRevision = -1;
    private int[] rowLocations;
    // No size limit; only changes to the memory view invalidate decoded presentation.
    private final Map<Integer, DecodedInstruction> instructions = new HashMap<>();
    private final Map<Integer, InvalidInstructionException> invalidInstructions = new HashMap<>();
    private final Decoder rowDecoder = this::decode;

    public void setCPU(CPU cpu, CPUContext cpuContext) {
        this.cpu = Objects.requireNonNull(cpu);
        this.cpuContext = Objects.requireNonNull(cpuContext);
        inspectedLocation = null;
        pageIndex = 0;
        clearDecodedInstructions();
        List<DebuggerColumn<?>> provided = cpu.getDebuggerColumns(rowDecoder);
        setDebuggerColumns(provided.isEmpty() ? defaultColumns(rowDecoder) : provided);
    }

    public void setDebuggerSession(DebuggerSession session) {
        this.session = Objects.requireNonNull(session);
        programRevision = -1;
        clearDecodedInstructions();
    }

    @Override public void setMaxRows(int rows) {
        int size = Math.max(1, rows);
        if (maxRows == size) return;
        if (cpu != null && pageIndex != 0) {
            materializePage();
            int center = rowLocations[maxRows / 2];
            if (center >= 0) inspectedLocation = center;
            pageIndex = 0;
        }
        maxRows = size;
        rowLocations = null;
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return cpu == null ? 0 : maxRows; }
    @Override public int getColumnCount() { return columns.length; }
    @Override public DebuggerColumn<?> getColumnAt(int index) { return columns[index]; }
    @Override public String getColumnName(int column) { return columns[column].getTitle(); }
    @Override public Class<?> getColumnClass(int column) { return columns[column].getClassType(); }

    private int viewLocation() {
        return inspectedLocation == null ? cpuContext.getInstructionLocation() : inspectedLocation;
    }

    private void clearDecodedInstructions() {
        instructions.clear();
        invalidInstructions.clear();
        rowLocations = null;
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
        if (cpu == null) return;
        refreshRevision();
        int anchor = viewLocation();
        if (rowLocations != null && cachedAnchor == anchor) return;
        int[] locations = new int[maxRows];
        Arrays.fill(locations, -1);
        if (anchor >= 0 && anchor < cpu.getAddressSpaceSize()) {
            long first = (long) pageIndex * Math.max(1, maxRows - 1) - maxRows / 2;
            long end = first + maxRows;
            if (first < 0) {
                List<Integer> previous = instructionsBefore(anchor, (int) Math.min(Integer.MAX_VALUE, -first));
                for (int i = 0; i < previous.size(); i++) {
                    long row = (long) i - previous.size() - first;
                    if (row >= 0 && row < maxRows) locations[(int) row] = previous.get(i);
                }
            }
            int address = anchor;
            for (long slot = 0; slot < end && address < cpu.getAddressSpaceSize(); slot++) {
                if (slot >= first) locations[(int) (slot - first)] = address;
                address = nextListingAddress(address);
            }
        }
        rowLocations = locations;
        cachedAnchor = anchor;
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
        try { length = decode(address).length(); }
        catch (InvalidInstructionException invalid) { /* Invalid cells remain individually browsable. */ }
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

    @Override public void previousPage() { seekBackwardPage(1); }
    @Override public void nextPage() { seekForwardPage(1); }

    @Override public void seekBackwardPage(int pages) {
        seekPages(pages, -1);
    }

    @Override public void seekForwardPage(int pages) {
        seekPages(pages, 1);
    }

    private void seekPages(int pages, int direction) {
        if (cpu == null) return;
        for (int page = 0; page < pages; page++) {
            pageIndex += direction;
            rowLocations = null;
            materializePage();
            if (Arrays.stream(rowLocations).allMatch(address -> address < 0)) {
                pageIndex -= direction;
                rowLocations = null;
                break;
            }
        }
        fireTableDataChanged();
    }

    @Override public void firstPage() {
        if (cpu == null) return;
        refreshRevision();
        List<Integer> previous = instructionsBefore(viewLocation(), Integer.MAX_VALUE);
        pageIndex = Math.floorDiv(maxRows / 2 - previous.size(), Math.max(1, maxRows - 1));
        rowLocations = null;
        fireTableDataChanged();
    }

    @Override public void lastPage() {
        if (cpu == null) return;
        refreshRevision();
        int anchor = viewLocation();
        int last = 0;
        for (int next = nextListingAddress(anchor); next < cpu.getAddressSpaceSize(); next = nextListingAddress(anchor)) {
            anchor = next;
            last++;
        }
        pageIndex = (int) (((long) last + maxRows / 2) / Math.max(1, maxRows - 1));
        rowLocations = null;
        fireTableDataChanged();
    }

    @Override public void currentPage() {
        inspectedLocation = null;
        pageIndex = 0;
        rowLocations = null;
        fireTableDataChanged();
    }

    @Override public void showAddress(int address) {
        if (cpu == null) return;
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        inspectedLocation = address;
        pageIndex = 0;
        rowLocations = null;
        fireTableDataChanged();
    }

    @Override public int getLocationAt(int row) {
        materializePage();
        return rowLocations == null || row < 0 || row >= rowLocations.length ? -1 : rowLocations[row];
    }

    @Override public boolean isRowAtCurrentInstruction(int row) {
        return cpu != null && getLocationAt(row) == cpuContext.getInstructionLocation();
    }

    @Override public Object getValueAt(int row, int column) {
        if (cpu == null || !valuesVisible.getAsBoolean()) return null;
        int address = getLocationAt(row);
        if (address < 0) return column == 0 ? ""
                : columns[column].getClassType() == Boolean.class ? Boolean.FALSE : null;
        return columns[column].getValue(address);
    }

    @Override public boolean isCellEditable(int row, int column) {
        return cpu != null && editingAllowed.getAsBoolean()
                && columns[column].isEditable() && getLocationAt(row) >= 0;
    }

    @Override public void setValueAt(Object value, int row, int column) {
        if (!isCellEditable(row, column)) return;
        int address = getLocationAt(row);
        if (address < 0 || !columns[column].getClassType().isInstance(value)) return;
        try { columns[column].setValue(address, value); }
        catch (IllegalArgumentException ignored) { }
    }

    @Override public void memoryChanged(int from, int to) {
        clearDecodedInstructions();
        fireTableDataChanged();
    }

    @Override public int guessPreviousInstructionLocation() {
        if (cpu == null) return 0;
        refreshRevision();
        int pc = cpuContext.getInstructionLocation();
        List<Integer> previous = instructionsBefore(pc, 1);
        return previous.isEmpty() ? pc : previous.get(0);
    }

    @Override public void setDebuggerColumns(List<DebuggerColumn<?>> columns) {
        List<DebuggerColumn<?>> displayed = new ArrayList<>();
        displayed.add(DebuggerColumn.text(TITLE_LABEL, address -> labels.apply(address)));
        displayed.addAll(columns);
        this.columns = displayed.toArray(new DebuggerColumn[0]);
        fireTableStructureChanged();
    }

    @Override public void setPresentation(IntFunction<String> labels, BooleanSupplier valuesVisible,
                                          BooleanSupplier editingAllowed) {
        this.labels = Objects.requireNonNull(labels);
        this.valuesVisible = Objects.requireNonNull(valuesVisible);
        this.editingAllowed = Objects.requireNonNull(editingAllowed);
    }

    static List<DebuggerColumn<?>> defaultColumns(Decoder decoder) {
        return List.of(DebuggerColumn.address("%04X"), DebuggerColumn.mnemo(decoder), DebuggerColumn.opcode(decoder));
    }
}
