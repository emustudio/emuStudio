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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;
import static net.emustudio.emulib.ui.StandardIcons.*;

/**
 * Browses the current instruction recording and completed captures.
 */
final class RecordingPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingPanel.class);
    private static final Pattern MEMORY_ACCESS = Pattern.compile("(?i)(read|write)\\s+@(?:0x)?([0-9a-f]+)");

    private final EmuStudioGui gui;
    private final DebuggerSession session;
    private List<InstructionRecording.Event> displayed = List.of();
    private final List<Step> steps = new ArrayList<>();
    private final HistoryModel model = new HistoryModel();
    private final JTable history = new JTable(model);
    private final JComboBox<Link> navigation = new JComboBox<>();
    private final JToggleButton fold;
    private final JComboBox<Recording> origin = new JComboBox<>();
    private Recording activeRecording;
    private int recordingsCreated;
    private boolean updatingOrigin, disposed;
    private final JToggleButton showUnchangedValues;
    private final JTextPane state = new JTextPane();
    private final JButton start, stop, rename, delete, earlierWrite;
    private final JToggleButton pause;

    RecordingPanel(EmuStudioGui gui, DebuggerSession session) {
        super(new BorderLayout(0, 0));
        this.gui = Objects.requireNonNull(gui);
        this.session = Objects.requireNonNull(session);
        history.getAccessibleContext().setAccessibleName("Recorded instructions");
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        gui.styleTable(history);
        history.setFont(EmuStudioGui.monospaced(history).deriveFont(13f));
        int[] widths = {200, 200, 200, 200};
        for (int column = 0; column < widths.length; column++)
            history.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        JPanel tools = gui.toolbarPanel();
        JToolBar capture = gui.toolBar();
        capture.setLayout(new net.miginfocom.swing.MigLayout("insets 0, gapx 2, fillx", "[][][][][][grow,fill][][]", ""));
        origin.getAccessibleContext().setAccessibleName("Recording");
        origin.addActionListener(event -> {
            if (!disposed && !updatingOrigin) {
                history.clearSelection();
                refresh();
            }
        });
        start = gui.toolbarButton(event -> run(this::startRecording), ICON_RECORD, "Start recording");
        start.setName("recordInstructions");
        pause = gui.toolbarToggleButton(event -> run(() -> {
            session.update(() -> session.setRecordingPaused(!session.recording.isPaused())).join();
            refresh();
        }), ICON_PAUSE, "Pause recording");
        stop = gui.toolbarButton(event -> run(this::stopRecording), ICON_STOP, "Stop recording");
        fold = gui.toolbarToggleButton(event -> {}, "/net/emustudio/application/gui/icons/emulation/fold.png", "Fold identical repeats");
        fold.setSelected(true);
        rename = gui.toolbarButton(event -> renameRecording(), ICON_RENAME, "Rename recording");
        delete = gui.toolbarButton(event -> deleteRecording(), ICON_DELETE, "Delete recording");
        capture.add(start);
        capture.add(pause);
        capture.add(stop);
        capture.add(new JSeparator(SwingConstants.VERTICAL), "h 22!");
        capture.add(fold);
        capture.add(origin, "growx, wmin 80");
        capture.add(rename);
        capture.add(delete);
        tools.add(capture, "growx");
        fold.addActionListener(event -> {
            if (!disposed) refresh();
        });
        showUnchangedValues = gui.toolbarToggleButton(event -> showState(history.getSelectedRow()),
                "/net/emustudio/application/gui/icons/emulation/show.png", "Show unchanged values");
        state.setEditable(false);
        state.setFont(EmuStudioGui.monospaced(state).deriveFont(13f));
        state.setBackground(getBackground());
        state.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        state.getAccessibleContext().setAccessibleName("Recorded instruction state");
        history.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showState(history.getSelectedRow());
        });
        add(gui.toolbarScrollPane(tools), BorderLayout.NORTH);
        JPanel detail = gui.section("Recorded state", "", "", "");
        detail.setLayout(new BorderLayout(4, 4));
        JPanel detailTools = gui.toolbarPanel();
        earlierWrite = gui.toolbarButton(event -> run(this::followLink), ICON_STEP, "Find earlier write");
        navigation.addActionListener(event -> updateNavigation());
        JToolBar memoryActions = gui.toolbarRow(detailTools, showUnchangedValues);
        memoryActions.add(new JSeparator(SwingConstants.VERTICAL), "h 22!");
        memoryActions.add(gui.label("Memory:", navigation));
        navigation.getAccessibleContext().setAccessibleName("Memory navigation");
        memoryActions.add(navigation, "w 280, wmin 80");
        memoryActions.add(earlierWrite);
        detail.add(gui.toolbarScrollPane(detailTools), BorderLayout.NORTH);
        JScrollPane stateScroll = gui.scrollPane(state);
        stateScroll.getViewport().setBackground(getBackground());
        detail.add(stateScroll);
        JPanel recording = gui.section("Recording", "insets 8, fill", "[grow,fill]", "[grow,fill]");
        JScrollPane historyScroll = new JScrollPane(history);
        recording.add(historyScroll, "grow");
        JSplitPane split = gui.splitPane(JSplitPane.VERTICAL_SPLIT, recording, detail, .6);
        split.setOneTouchExpandable(false);
        split.setDividerLocation(.6);
        add(split);
        refresh();
    }

    void close() {
        disposed = true;
        updateControls();
    }

    private void run(Operation operation) {
        if (disposed) return;
        try { operation.run(); } catch (Exception failure) { logFailure(LOGGER, failure); }
    }

    private void startRecording() {
        RecordingDialog dialog = new RecordingDialog(
                (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this), gui, session, previous -> {
                    if (activeRecording != null) activeRecording.events = previous;
                    activeRecording = null;
                    refresh();
                });
        dialog.setVisible(true);
    }

    private void stopRecording() {
        retainActiveRecording();
        session.update(() -> session.record(0)).join();
        if (activeRecording != null) activeRecording.events = session.recording.events();
        activeRecording = null;
        refresh();
    }

    void addRecording(List<InstructionRecording.Event> captured) {
        addRecording(new Recording(++recordingsCreated, List.copyOf(captured)));
        refresh();
    }

    private void addRecording(Recording recording) {
        updatingOrigin = true;
        try {
            origin.addItem(recording);
            origin.setSelectedItem(recording);
            history.clearSelection();
        } finally {
            updatingOrigin = false;
        }
    }

    private void retainActiveRecording() {
        if (session.recording.isRecording() && activeRecording == null) {
            activeRecording = new Recording(++recordingsCreated, List.of());
            addRecording(activeRecording);
        }
        if (activeRecording != null) activeRecording.events = session.recording.events();
    }

    private void renameRecording() {
        Recording selected = (Recording) origin.getSelectedItem();
        if (selected == null || disposed) return;
        String name = (String) JOptionPane.showInputDialog(this, "Recording name:", "Rename recording",
                JOptionPane.PLAIN_MESSAGE, null, null, selected.name);
        if (name != null && !name.isBlank()) {
            selected.name = name.strip();
            origin.repaint();
        }
    }

    private void deleteRecording() {
        Recording selected = (Recording) origin.getSelectedItem();
        if (selected == null || selected == activeRecording || disposed) return;
        origin.removeItem(selected);
        refresh();
    }

    void updateControls() {
        boolean recording = session.recording.isRecording();
        boolean available = !disposed && session.debug != null;
        start.setEnabled(available);
        pause.setEnabled(available && recording);
        stop.setEnabled(available && recording);
        boolean paused = recording && session.recording.isPaused();
        pause.setSelected(paused);
        EmuStudioGui.toolbarIcon(pause, paused ? ICON_RUN : ICON_PAUSE);
        String action = paused ? "Continue recording" : "Pause recording";
        pause.setToolTipText(action);
        pause.getAccessibleContext().setAccessibleName(action);
        origin.setEnabled(!disposed && origin.getItemCount() > 0);
        history.setEnabled(!disposed);
        fold.setEnabled(!disposed);
        rename.setEnabled(!disposed && origin.getSelectedItem() != null);
        delete.setEnabled(!disposed && origin.getSelectedItem() != null && origin.getSelectedItem() != activeRecording);
        showUnchangedValues.setEnabled(!disposed);
        navigation.setEnabled(!disposed && navigation.getItemCount() > 0);
        updateNavigation();
    }

    private void updateNavigation() {
        Link selected = (Link) navigation.getSelectedItem();
        earlierWrite.setEnabled(!disposed && selected != null);
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
        retainActiveRecording();
        int selected = history.getSelectedRow();
        InstructionRecording.Event selectedEvent = selected >= 0 && selected < steps.size()
                ? steps.get(selected).first() : null;
        Recording source = (Recording) origin.getSelectedItem();
        displayed = source == null ? List.of() : source.events;
        if (!session.recording.isRecording()) activeRecording = null;
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
        if (history.getSelectedRow() < 0) showState(-1);
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

    private static final class Recording {
        final int number;
        String name = "Unnamed";
        List<InstructionRecording.Event> events;

        Recording(int number, List<InstructionRecording.Event> events) {
            this.number = number;
            this.events = events;
        }

        @Override public String toString() { return number + " | " + name; }
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
        private final String[] columns = {"Sequence", "Address", "Step", "Changes"};

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
        earlierWrite.setEnabled(false);
        if (selected < 0 || selected >= steps.size()) {
            setState("");
            return;
        }
        Step step = steps.get(selected);
        boolean folded = step.folded();
        StringBuilder text = new StringBuilder("Sequence " + step.sequences() + " · " + step.location() + " · "
                + step.summary() + "\n");
        if (folded) text.append(step.size())
                .append(" matching steps; unfold to inspect each event.\n");
        String section = null;
        List<int[]> headings = new ArrayList<>();
        for (ViewRow row : step.rows()) {
            if (folded && navigational(row)) continue;
            if (!row.changed && !navigational(row) && !showUnchangedValues.isSelected()) continue;
            section = appendSection(text, section, row.group, headings);
            text.append(row.label).append("    ").append(row.before);
            if (row.changed || !row.before.equals(row.after)) text.append(" → ").append(row.after);
            if (folded && row.changed && row.before.equals(row.after)) text.append(" (changed within run)");
            text.append("\n");
        }
        // Keep every row's links tied to its own original ordinal, including within folded groups.
        for (InstructionRecording.Event instruction : step.events()) {
            for (ViewRow row : instruction.view.rows) {
                if (folded && navigational(row)) {
                    section = appendSection(text, section, row.group, headings);
                    text.append("#").append(instruction.sequence).append(" · ")
                            .append(row.label).append(" = ");
                    if (!row.before.isEmpty() && !row.before.equals(row.after)) text.append(row.before).append(" → ");
                    text.append(row.after).append("\n");
                }
                for (NavLink link : row.links)
                    navigation.addItem(new Link(instruction.sequence, link, (folded ? "#" + instruction.sequence + " · " : "")
                            + navigationLabel(row.label)));
            }
        }
        navigation.setEnabled(navigation.getItemCount() > 0);
        setState(text.toString());
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        for (int[] heading : headings)
            state.getStyledDocument().setCharacterAttributes(heading[0], heading[1], bold, false);
        updateNavigation();
    }

    private static String navigationLabel(String label) {
        Matcher access = MEMORY_ACCESS.matcher(label);
        if (!access.matches()) return label;
        return access.group(1).substring(0, 1).toUpperCase(Locale.ROOT)
                + access.group(1).substring(1).toLowerCase(Locale.ROOT) + " 0x" + access.group(2);
    }

    private static String appendSection(StringBuilder text, String current, String section, List<int[]> headings) {
        if (!section.equals(current)) {
            text.append("\n");
            headings.add(new int[]{text.length(), section.length()});
            text.append(section).append("\n");
        }
        return section;
    }
}
