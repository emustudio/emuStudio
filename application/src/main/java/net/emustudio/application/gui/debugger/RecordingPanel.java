/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.InstructionRecording;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Operation;
import net.emustudio.application.gui.dialogs.RecordingDialog;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.ViewRow;
import net.emustudio.emulib.plugin.cpu.CPU;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;
import static net.emustudio.emulib.ui.StandardIcons.*;

/**
 * Browses the current instruction recording and completed captures.
 */
final class RecordingPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingPanel.class);

    private final EmuStudioGui gui;
    private final DebuggerSession session;
    private List<InstructionRecording.Event> displayed = List.of();
    private final List<Step> steps = new ArrayList<>();
    private final HistoryModel model = new HistoryModel();
    private final JTable history = new JTable(model);
    private final JComboBox<Link> navigation = new JComboBox<>();
    private final JCheckBox fold = new JCheckBox("Fold identical repeats", true);
    private final JComboBox<String> origin = new JComboBox<>(new String[]{"Current recording"});
    // Completed captures are kept until the panel is disposed.
    private final List<List<InstructionRecording.Event>> recordings = new ArrayList<>();
    private int recordingsCreated;
    private boolean updatingOrigin, disposed;
    private final JPanel body = new JPanel(new CardLayout());
    private final JLabel emptyTitle, emptyHint;
    private final JButton emptyRecord;
    private final JCheckBox showUnchangedValues = new JCheckBox("Show unchanged values");
    private final JTextArea state;
    private final JToggleButton recordingControl;

    RecordingPanel(EmuStudioGui gui, DebuggerSession session) {
        super(new BorderLayout(0, 0));
        this.gui = Objects.requireNonNull(gui);
        this.session = Objects.requireNonNull(session);
        emptyTitle = gui.labelBold("");
        emptyHint = gui.label("");
        state = gui.textAreaReadOnly(50, 10);
        history.getAccessibleContext().setAccessibleName("Recorded instructions");
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        gui.styleTable(history);
        history.setFont(EmuStudioGui.monospaced(history));
        int[] widths = {170, 120, 250, 300};
        for (int column = 0; column < widths.length; column++)
            history.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        JPanel tools = gui.panel("insets 4, fillx, hidemode 3", "[grow,fill]", "");
        JPanel capture = gui.panel("insets 0, fillx, hidemode 3", "[][grow,fill][]", "");
        origin.getAccessibleContext().setAccessibleName("Recording");
        origin.addActionListener(event -> {
            if (!disposed && !updatingOrigin) {
                history.clearSelection();
                refresh();
            }
        });
        capture.add(gui.label("Recording", origin));
        capture.add(origin);
        recordingControl = gui.toolbarToggleButton(event -> {}, ICON_RECORD, "Record instructions");
        recordingControl.setName("recordInstructions");
        recordingControl.addActionListener(event -> {
            run(this::toggleRecording);
            updateControls();
        });
        capture.add(recordingControl);
        tools.add(capture, "growx, wrap");
        tools.add(fold, "alignx right, wrap");
        fold.addActionListener(event -> {
            if (!disposed) refresh();
        });
        showUnchangedValues.addActionListener(event -> showState(history.getSelectedRow()));
        state.setFont(EmuStudioGui.monospaced(state));
        state.getAccessibleContext().setAccessibleName("Recorded instruction state");
        history.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showState(history.getSelectedRow());
        });
        add(gui.toolbarScrollPane(tools), BorderLayout.NORTH);
        JPanel detail = gui.section("Recorded state", "", "", "");
        detail.setLayout(new BorderLayout(4, 4));
        JPanel detailTools = gui.panel("insets 4, fillx", "[grow,fill][]", "");
        detailTools.add(showUnchangedValues, "cell 1 0");
        detail.add(detailTools, BorderLayout.NORTH);
        detail.add(gui.scrollPane(state));
        JButton followLink = gui.toolbarTextButton(event -> run(this::followLink), ICON_FIND, "Follow link");
        followLink.setToolTipText("Follow the selected recorded navigation link");
        JPanel memoryActions = gui.panel("insets 4, fillx", "[][grow,fill][]", "");
        memoryActions.add(gui.label("Navigation", navigation));
        memoryActions.add(navigation);
        memoryActions.add(followLink);
        detail.add(memoryActions, BorderLayout.SOUTH);
        JSplitPane split = gui.splitPane(JSplitPane.VERTICAL_SPLIT, gui.scrollPane(history), detail, .6);
        split.setOneTouchExpandable(false);
        body.add(split, "results");
        JPanel empty = gui.panel("insets 24, fillx, aligny center, hidemode 3", "[grow,center]", "");
        emptyTitle.getAccessibleContext().setAccessibleName("History empty state");
        empty.add(emptyTitle, "wrap");
        empty.add(emptyHint, "wrap");
        emptyRecord = gui.toolbarTextButton(event -> run(this::toggleRecording), ICON_RECORD, "Record instructions");
        empty.add(emptyRecord, "wrap");
        body.add(empty, "empty");
        add(body);
        updateControls();
    }

    void close() {
        disposed = true;
        updateControls();
    }

    private void run(Operation operation) {
        if (disposed) return;
        try { operation.run(); } catch (Exception failure) { logFailure(LOGGER, failure); }
    }

    private void toggleRecording() {
        if (session.recording.isRecording()) {
            String scope = recordingScope();
            try {
                session.update(() -> session.record(0)).join();
                List<InstructionRecording.Event> captured = session.recording.events();
                if (!captured.isEmpty()) addRecording(captured, scope);
            } finally {
                refresh();
            }
        } else {
            RecordingDialog dialog = new RecordingDialog(
                    (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this), gui, session,
                    () -> {
                        origin.setSelectedIndex(0);
                        refresh();
                    });
            dialog.setVisible(true);
        }
        updateControls();
    }

    private String recordingScope() {
        InstructionRecording.Mode mode = session.recording.mode();
        if (mode == InstructionRecording.Mode.ADDRESS_RANGE)
            return String.format("From 0x%04X to 0x%04X", session.recording.from(), session.recording.through());
        if (mode == InstructionRecording.Mode.UNLIMITED) return "";
        return (mode == InstructionRecording.Mode.FIRST ? "First " : "Last ")
                + session.recording.limit() + " instructions";
    }

    void addRecording(List<InstructionRecording.Event> captured, String scope) {
        if (captured.isEmpty()) return;
        updatingOrigin = true;
        try {
            recordings.add(List.copyOf(captured));
            origin.addItem("Recording " + (++recordingsCreated) + (scope.isEmpty() ? "" : " · " + scope));
            origin.setSelectedIndex(recordings.size());
        } finally {
            updatingOrigin = false;
        }
        refresh();
    }

    void updateControls() {
        boolean recording = session.recording.isRecording();
        String action = recording ? "Stop recording" : "Record instructions";
        recordingControl.setSelected(recording);
        EmuStudioGui.toolbarIcon(recordingControl, recording ? ICON_STOP : ICON_RECORD);
        recordingControl.setToolTipText(action);
        recordingControl.getAccessibleContext().setAccessibleName(action);
        boolean available = !disposed && session.debug != null;
        recordingControl.setEnabled(available);
        emptyRecord.setEnabled(available);
        boolean paused = !disposed && !session.isRunning();
        origin.setEnabled(paused);
        history.setEnabled(paused);
    }

    private void followLink() {
        Link selected = (Link) navigation.getSelectedItem();
        if (selected == null) throw new IllegalArgumentException("Select a recorded navigation link first");
        OptionalLong target = InstructionRecording.resolveEvent(displayed, selected.sequence, selected.link);
        if (target.isPresent()) {
            fold.setSelected(false);
            refresh();
            selectRow(step -> step.first().sequence == target.getAsLong(), true);
        } else LOGGER.info("No matching event retained in this recording.");
    }

    private void selectRow(Predicate<Step> matches, boolean scroll) {
        for (int row = 0; row < steps.size(); row++)
            if (matches.test(steps.get(row))) {
                history.setRowSelectionInterval(row, row);
                if (scroll) history.scrollRectToVisible(history.getCellRect(row, 0, true));
                return;
            }
    }

    void refresh() {
        if (disposed) return;
        int selected = history.getSelectedRow();
        InstructionRecording.Event selectedEvent = selected >= 0 && selected < steps.size()
                ? steps.get(selected).first() : null;
        int source = origin.getSelectedIndex();
        displayed = source <= 0 ? session.recording.events() : recordings.get(source - 1);
        steps.clear();
        for (int i = 0; i < displayed.size(); i++) {
            int last = i;
            while (fold.isSelected() && last + 1 < displayed.size()
                    && sameStep(displayed.get(last), displayed.get(last + 1))) last++;
            steps.add(new Step(i, last));
            i = last;
        }
        model.fireTableDataChanged();
        if (selectedEvent != null) selectRow(step -> step.events().contains(selectedEvent), false);
        if (displayed.isEmpty()) {
            navigation.removeAllItems();
            emptyTitle.setText(session.debug == null ? "Recording unavailable for this CPU"
                    : session.recording.isRecording() ? "Ready to capture instructions" : "No instructions recorded");
            emptyHint.setText(session.recording.isRecording()
                    ? "Step or run your program, then pause to inspect its instructions."
                    : "Start recording, then step or run your program.");
            emptyRecord.setVisible(session.debug != null && !session.recording.isRecording());
            setState("");
        } else if (history.getSelectedRow() < 0) {
            setState("Select a step to inspect its captured details.");
        }
        ((CardLayout) body.getLayout()).show(body, displayed.isEmpty() ? "empty" : "results");
        updateControls();
    }

    private void setState(String text) {
        state.setText(text);
        state.setCaretPosition(0);
    }

    private static boolean sameStep(InstructionRecording.Event first, InstructionRecording.Event second) {
        return second.sequence == first.sequence + 1 && first.completed && second.completed
                && !first.view.foldKey.isBlank() && first.view.foldKey.equals(second.view.foldKey);
    }

    private static boolean navigational(ViewRow row) {
        return !row.tags.isEmpty() || !row.links.isEmpty();
    }

    /** Run of displayed events shown as one history row; identical repeats fold into one step. */
    private final class Step {
        final int from, to;
        private List<ViewRow> rows;

        Step(int from, int to) {
            this.from = from;
            this.to = to;
        }

        InstructionRecording.Event first() { return displayed.get(from); }
        InstructionRecording.Event last() { return displayed.get(to); }
        int size() { return to - from + 1; }
        boolean folded() { return from != to; }
        List<InstructionRecording.Event> events() { return displayed.subList(from, to + 1); }
        String sequences() { return first().sequence + (folded() ? "–" + last().sequence : ""); }
        String summary() { return first().view.summary + (first().completed ? "" : " (failed)"); }

        String location() {
            InstructionRecording.Event first = first(), last = last();
            if (first.view.programLocation.isEmpty()) return "";
            int start = first.view.programLocation.getAsInt();
            return last.view.programLocation.isPresent() && start != last.view.programLocation.getAsInt()
                    ? String.format("%04X → %04X", start, last.view.programLocation.getAsInt())
                    : String.format("%04X", start);
        }

        /** Rows of all events; state rows merge first-before with last-after, navigation rows are kept. */
        List<ViewRow> rows() {
            if (rows == null) rows = folded() ? combinedRows(events()) : first().view.rows;
            return rows;
        }
    }

    /** Navigation combo item bound to the event whose row declared the link. */
    private static final class Link {
        final long sequence;
        final NavLink link;
        final String label;

        Link(long sequence, NavLink link, String label) {
            this.sequence = sequence;
            this.link = link;
            this.label = label;
        }

        @Override public String toString() { return label; }
    }

    private static List<ViewRow> combinedRows(List<InstructionRecording.Event> events) {
        List<ViewRow> rows = new ArrayList<>();
        Map<List<String>, Integer> stateRows = new LinkedHashMap<>();
        for (InstructionRecording.Event event : events) {
            for (ViewRow row : event.view.rows) {
                if (navigational(row)) {
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

    private final class HistoryModel extends AbstractTableModel {
        private final String[] columns = {"Sequence", "Location", "Step", "Changes"};

        @Override
        public int getRowCount() {
            return steps.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Step step = steps.get(row);
            switch (column) {
                case 0:
                    return step.sequences() + (step.folded() ? " (" + step.size() + ")" : "");
                case 1:
                    return step.location();
                case 2:
                    return step.summary() + (step.folded() ? " × " + step.size() : "");
                default: {
                    List<String> values = new ArrayList<>();
                    for (ViewRow value : step.rows())
                        if (value.changed) values.add(value.label + "=" + value.before + "→" + value.after);
                    return String.join(" ", values);
                }
            }
        }
    }

    private void showState(int selected) {
        navigation.removeAllItems();
        navigation.setEnabled(false);
        if (selected < 0 || selected >= steps.size()) return;
        Step step = steps.get(selected);
        boolean folded = step.folded();
        StringBuilder text = new StringBuilder("Sequence " + step.sequences() + " · " + step.location() + " · "
                + step.summary() + "\n");
        if (folded) text.append(step.size())
                .append(" matching steps; unfold to inspect each event.\n");
        String section = null;
        for (ViewRow row : step.rows()) {
            if (folded && navigational(row)) continue;
            if (!row.changed && !navigational(row) && !showUnchangedValues.isSelected()) continue;
            section = appendSection(text, section, row.group);
            text.append(row.label).append("    ").append(row.before);
            if (row.changed || !row.before.equals(row.after)) text.append(" → ").append(row.after);
            if (folded && row.changed && row.before.equals(row.after)) text.append(" (changed within run)");
            text.append("\n");
        }
        // Keep every row's links tied to its own original ordinal, including within folded groups.
        for (InstructionRecording.Event instruction : step.events()) {
            for (ViewRow row : instruction.view.rows) {
                if (folded && navigational(row)) {
                    section = appendSection(text, section, row.group);
                    text.append("#").append(instruction.sequence).append(" · ")
                            .append(row.label).append(" = ");
                    if (!row.before.isEmpty() && !row.before.equals(row.after)) text.append(row.before).append(" → ");
                    text.append(row.after).append("\n");
                }
                for (NavLink link : row.links)
                    navigation.addItem(new Link(instruction.sequence, link, (folded ? "#" + instruction.sequence + " · " : "")
                            + row.label + " · " + link.label));
            }
        }
        navigation.setEnabled(navigation.getItemCount() > 0);
        setState(text.toString());
    }

    private static String appendSection(StringBuilder text, String current, String section) {
        if (!section.equals(current)) text.append("\n").append(section).append("\n");
        return section;
    }
}
