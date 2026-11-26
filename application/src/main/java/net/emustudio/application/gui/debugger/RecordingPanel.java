/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.InstructionRecording;
import net.emustudio.application.emulation.debugger.InstructionRecording.Page;
import net.emustudio.application.emulation.debugger.InstructionRecording.Recording;
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
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;

/**
 * Browses the current instruction recording and completed captures.
 */
final class RecordingPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingPanel.class);
    private static final Pattern MEMORY_ACCESS = Pattern.compile("(?i)(read|write)\\s+@(?:0x)?([0-9a-f]+)");
    private static final String RECORDING_ICONS = "/net/emustudio/application/gui/icons/emulation/";

    private final EmuStudioGui gui;
    private final DebuggerSession session;
    private List<InstructionRecording.Event> displayed = List.of();
    private final List<Step> steps = new ArrayList<>();
    private final HistoryModel model = new HistoryModel();
    private final JTable history = new JTable(model);
    private final JComboBox<Link> navigation = new JComboBox<>();
    private final JToggleButton fold;
    private final JComboBox<Recording> origin = new JComboBox<>();
    private List<Recording> catalog = List.of();
    private Recording pageSource;
    private Page page;
    private long pageStart = InstructionRecording.LATEST_PAGE;
    private boolean finalFilePage;
    private long pendingWrite = -1;
    private SwingWorker<Page, Void> pageRead;
    private SwingWorker<Long, Void> writeSearch;
    private SwingWorker<Void, Void> fileAction;
    private boolean updatingOrigin, disposed, foldedPage;
    private final JToggleButton showUnchangedValues;
    private final JTextPane state = new JTextPane();
    private final JButton create, open, save, start, stop, edit, delete, earlierWrite, previous, latest, next;
    private final JLabel pageLabel = new JLabel();
    private final JToggleButton pause;
    private Popup writeFeedback;
    private final javax.swing.Timer writeFeedbackTimer = new javax.swing.Timer(2500, event -> hideWriteFeedback());
    private final javax.swing.Timer liveRefresh = new javax.swing.Timer(250, event -> {
        if (isShowing()) refresh();
    });

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
        capture.setLayout(new net.miginfocom.swing.MigLayout("insets 0, gapx 2, fillx", "", ""));
        origin.getAccessibleContext().setAccessibleName("Recording");
        origin.addActionListener(event -> {
            if (!disposed && !updatingOrigin) {
                history.clearSelection();
                refresh();
            }
        });
        create = gui.toolbarButton(event -> run(this::newRecording), RECORDING_ICONS + "new-recording.png", "New recording");
        open = gui.toolbarButton(event -> run(this::openRecording), RECORDING_ICONS + "open-recording.png", "Open recording");
        save = gui.toolbarButton(event -> run(this::saveRecording), RECORDING_ICONS + "save-recording.png", "Save recording");
        start = gui.toolbarButton(event -> run(this::startRecording),
                "/net/emustudio/application/gui/icons/record.png", "Start recording");
        start.setName("recordInstructions");
        start.getAccessibleContext().setAccessibleName(start.getToolTipText());
        pause = gui.toolbarToggleButton(event -> run(() -> {
            session.setRecordingPaused(!session.recording.isPaused());
            refresh();
        }), "/net/emustudio/application/gui/icons/pause.png", "Pause recording");
        stop = gui.toolbarButton(event -> run(this::stopRecording), "/net/emustudio/application/gui/icons/stop.png", "Stop recording");
        fold = gui.toolbarToggleButton(event -> {}, "/net/emustudio/application/gui/icons/emulation/fold.png", "Fold identical repeats");
        fold.setSelected(true);
        edit = gui.toolbarButton(event -> editRecording(), RECORDING_ICONS + "edit-recording.png", "Edit recording");
        delete = gui.toolbarButton(event -> deleteRecording(), RECORDING_ICONS + "delete-recording.png", "Delete recording");
        capture.add(create);
        capture.add(open);
        capture.add(save);
        capture.add(new JToolBar.Separator(), "growy");
        capture.add(start);
        capture.add(pause);
        capture.add(stop);
        capture.add(new JToolBar.Separator(), "growy");
        capture.add(fold);
        capture.add(new JToolBar.Separator(), "growy");
        capture.add(origin, "growx, pushx, wmin 80");
        capture.add(edit);
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
        earlierWrite = gui.toolbarButton(event -> run(this::followLink), "/net/emustudio/application/gui/icons/step.png", "Find previous write");
        writeFeedbackTimer.setRepeats(false);
        earlierWrite.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !earlierWrite.isShowing()) {
                hideWriteFeedback();
            }
        });
        navigation.addActionListener(event -> {
            hideWriteFeedback();
            updateNavigation();
        });
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
        JPanel recording = gui.section("Recording", "", "", "");
        recording.setLayout(new BorderLayout(0, 6));
        recording.setBorder(BorderFactory.createCompoundBorder(recording.getBorder(),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JScrollPane historyScroll = new JScrollPane(history);
        recording.add(historyScroll, BorderLayout.CENTER);
        previous = gui.toolbarButton(event -> showPage(page.previous), "/net/emustudio/application/gui/icons/page-back.png", "Previous recording page");
        next = gui.toolbarButton(event -> showPage(page.next), "/net/emustudio/application/gui/icons/run.png", "Next recording page");
        latest = gui.toolbarButton(event -> showPage(InstructionRecording.LATEST_PAGE), "/net/emustudio/application/gui/icons/page-last.png", "Latest recording page");
        JPanel paging = gui.toolbarPanel();
        gui.toolbarRow(paging, previous, next, latest, pageLabel);
        recording.add(paging, BorderLayout.SOUTH);
        JSplitPane split = gui.splitPane(JSplitPane.VERTICAL_SPLIT, recording, detail, .6);
        split.setOneTouchExpandable(false);
        split.setDividerLocation(.6);
        add(split);
        refresh();
    }

    void close() {
        disposed = true;
        liveRefresh.stop();
        hideWriteFeedback();
        if (fileAction != null) fileAction.cancel(true);
        if (pageRead != null) pageRead.cancel(true);
        if (writeSearch != null) writeSearch.cancel(true);
        updateControls();
    }

    @Override public void addNotify() {
        super.addNotify();
        if (!disposed) liveRefresh.start();
    }

    @Override public void removeNotify() {
        liveRefresh.stop();
        super.removeNotify();
    }

    private void run(Operation operation) {
        if (disposed) return;
        try { operation.run(); } catch (Exception failure) { logFailure(LOGGER, failure); }
    }

    private void newRecording() {
        RecordingDialog dialog = new RecordingDialog(
                (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this), gui, session, this::refresh);
        dialog.setVisible(true);
    }

    private void startRecording() {
        Recording selected = (Recording) origin.getSelectedItem();
        try {
            if (selected != null && selected.isPending()) session.startPreparedRecording(selected);
            else session.record(InstructionRecording.DEFAULT_LIMIT);
            refresh();
        } catch (IOException failure) {
            showFileError("Start recording", failure);
        }
    }

    private void openRecording() {
        Path path = RecordingDialog.chooseFile(this, "Open recording", false);
        if (path != null) runFileAction("Open recording", () -> session.recording.open(path));
    }

    private void saveRecording() {
        Recording selected = (Recording) origin.getSelectedItem();
        if (selected == null) return;
        Path path = RecordingDialog.chooseFile(this, "Save recording", true);
        if (path != null) runFileAction("Save recording", () -> session.recording.save(selected, path, false));
    }

    private void runFileAction(String title, Operation operation) {
        fileAction = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                operation.run();
                return null;
            }

            @Override protected void done() {
                fileAction = null;
                if (disposed || isCancelled()) return;
                try {
                    get();
                    refresh();
                } catch (ExecutionException failure) {
                    showFileError(title, failure.getCause());
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                } finally {
                    updateControls();
                }
            }
        };
        updateControls();
        fileAction.execute();
    }

    private void showFileError(String title, Throwable failure) {
        logFailure(LOGGER, failure);
        JOptionPane.showMessageDialog(this, failure.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void stopRecording() {
        session.stopRecording();
        refresh();
    }

    void addRecording(List<InstructionRecording.Event> captured) {
        session.recording.addRecording(captured);
        refresh();
    }

    private void refreshRecordings() {
        List<Recording> recordings = session.recording.recordings();
        if (catalog.equals(recordings)) return;
        Recording newest = recordings.isEmpty() ? null : recordings.get(recordings.size() - 1);
        Object selected = origin.getSelectedItem();
        boolean added = newest != null && !catalog.contains(newest);
        catalog = recordings;
        updatingOrigin = true;
        try {
            origin.removeAllItems();
            recordings.forEach(origin::addItem);
            origin.setSelectedItem(!added && recordings.contains(selected) ? selected : newest);
        } finally {
            updatingOrigin = false;
        }
    }

    private void editRecording() {
        Recording selected = (Recording) origin.getSelectedItem();
        if (selected == null || disposed) return;
        String name = (String) JOptionPane.showInputDialog(this, "Recording name:", "Edit recording",
                JOptionPane.PLAIN_MESSAGE, null, null, selected.name());
        if (name != null && !name.isBlank()) {
            session.recording.rename(selected, name);
            origin.repaint();
        }
    }

    private void deleteRecording() {
        Recording selected = (Recording) origin.getSelectedItem();
        if (selected == null || disposed) return;
        session.recording.delete(selected);
        refresh();
    }

    void updateControls() {
        boolean recording = session.recording.isRecording();
        boolean available = !disposed && session.debug != null;
        start.setEnabled(available);
        create.setEnabled(available);
        open.setEnabled(!disposed && fileAction == null);
        pause.setEnabled(available && recording);
        stop.setEnabled(available && recording);
        boolean paused = recording && session.recording.isPaused();
        pause.setSelected(paused);
        EmuStudioGui.toolbarIcon(pause, paused ? "/net/emustudio/application/gui/icons/run.png" : "/net/emustudio/application/gui/icons/pause.png");
        String action = paused ? "Continue recording" : "Pause recording";
        pause.setToolTipText(action);
        pause.getAccessibleContext().setAccessibleName(action);
        origin.setEnabled(!disposed && origin.getItemCount() > 0);
        history.setEnabled(!disposed);
        fold.setEnabled(!disposed);
        Recording chosen = (Recording) origin.getSelectedItem();
        start.setToolTipText("Start recording (" + describeStart(chosen) + ")");
        edit.setEnabled(!disposed && chosen != null);
        save.setEnabled(!disposed && fileAction == null && chosen != null && !chosen.isPending()
                && (!chosen.isFile() || chosen.isFileFinished()));
        delete.setEnabled(!disposed && chosen != null && session.recording.canDelete(chosen));
        boolean canPage = !disposed && page != null && pageRead == null;
        previous.setEnabled(canPage && page.previous >= 0);
        next.setEnabled(canPage && page.next >= 0);
        latest.setEnabled(canPage && pageStart != InstructionRecording.LATEST_PAGE);
        showUnchangedValues.setEnabled(!disposed);
        navigation.setEnabled(!disposed && navigation.getItemCount() > 0);
        updateNavigation();
    }

    private static String describeStart(Recording chosen) {
        if (chosen == null || !chosen.isPending()) return "last " + InstructionRecording.DEFAULT_LIMIT + " instructions";

        InstructionRecording.AddressRange range = chosen.range();
        String addresses = range == null ? ""
                : String.format(Locale.ROOT, "between 0x%04X and 0x%04X", range.from, range.through);
        InstructionRecording.Options options = chosen.options();
        if (options == null) return addresses.isEmpty() ? "until stopped" : addresses;

        String order = options.mode == InstructionRecording.Mode.FIRST ? "keep first " : "last ";
        String count = order + options.limit + (options.limit == 1 ? " instruction" : " instructions");
        return addresses.isEmpty() ? count : count + " " + addresses;
    }

    private void updateNavigation() {
        Link selected = (Link) navigation.getSelectedItem();
        earlierWrite.setEnabled(!disposed && selected != null && writeSearch == null);
    }

    private void followLink() {
        hideWriteFeedback();
        Link selected = (Link) navigation.getSelectedItem();
        if (selected == null) throw new IllegalArgumentException("Select a recorded navigation link first");
        OptionalLong target = InstructionRecording.resolveEvent(displayed, selected.sequence, selected.link);
        if (target.isPresent()) {
            showWrite(page.start, target.getAsLong());
        } else if (page.previous >= 0) {
            findWriteOnEarlierPage(selected);
        } else {
            showMissingWrite();
        }
    }

    private void findWriteOnEarlierPage(Link link) {
        Recording source = pageSource;
        Page original = page;
        writeSearch = new SwingWorker<>() {
            private OptionalLong sequence = OptionalLong.empty();

            @Override protected Long doInBackground() throws IOException {
                long start = original.previous;
                while (start >= 0 && !isCancelled()) {
                    Page earlier = session.recording.page(source, start);
                    sequence = InstructionRecording.resolveEvent(earlier.events, link.sequence, link.link);
                    if (sequence.isPresent()) return earlier.start;
                    start = earlier.previous;
                }
                return -1L;
            }

            @Override protected void done() {
                writeSearch = null;
                if (!disposed && !isCancelled() && pageSource == source && navigation.getSelectedItem() == link) {
                    RecordingPanel.this.run(() -> {
                        long start = get();
                        if (start >= 0) showWrite(start, sequence.getAsLong());
                        else showMissingWrite();
                    });
                }
                updateNavigation();
            }
        };
        updateNavigation();
        writeSearch.execute();
    }

    private void showMissingWrite() {
        if (!earlierWrite.isShowing()) return;
        JToolTip tooltip = earlierWrite.createToolTip();
        tooltip.setTipText("No previous write found in this recording");
        Point location = earlierWrite.getLocationOnScreen();
        Dimension size = tooltip.getPreferredSize();
        writeFeedback = PopupFactory.getSharedInstance().getPopup(earlierWrite, tooltip,
                location.x + earlierWrite.getWidth() - size.width, location.y - size.height);
        writeFeedback.show();
        writeFeedbackTimer.restart();
    }

    private void hideWriteFeedback() {
        writeFeedbackTimer.stop();
        if (writeFeedback != null) {
            writeFeedback.hide();
            writeFeedback = null;
        }
    }

    private void showWrite(long start, long sequence) {
        fold.setSelected(false);
        if (page.start == start) {
            showPageContents(page);
            selectSequence(sequence, true);
            return;
        }
        pendingWrite = sequence;
        showPage(start);
    }

    private void showPage(long start) {
        cancelPageRead();
        pageStart = start;
        refresh();
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
        refreshRecordings();
        Recording source = (Recording) origin.getSelectedItem();
        if (source != pageSource) {
            cancelPageRead();
            pageSource = source;
            pageStart = InstructionRecording.LATEST_PAGE;
            pendingWrite = -1;
            showPageContents(null);
        }
        if (source == null) return;
        if (source.isFile()) {
            readFilePage(source);
        } else {
            run(() -> showPageContents(session.recording.page(source, pageStart)));
        }
    }

    private void cancelPageRead() {
        if (pageRead != null) pageRead.cancel(true);
        pageRead = null;
        finalFilePage = false;
    }

    private void readFilePage(Recording source) {
        showPageContents(page);
        if (pageRead != null || finalFilePage) return;
        long requested = pageStart;
        if (page != null && page.start == requested && page.next >= 0) return;
        boolean finished = source.isFileFinished();
        pageRead = new SwingWorker<>() {
            @Override protected Page doInBackground() throws IOException {
                return session.recording.page(source, requested);
            }

            @Override protected void done() {
                if (pageRead != this || isCancelled() || disposed) return;
                pageRead = null;
                RecordingPanel.this.run(() -> {
                    showPageContents(get());
                    finalFilePage = finished;
                });
                updateControls();
            }
        };
        updateControls();
        pageRead.execute();
    }

    private void showPageContents(Page upcoming) {
        List<InstructionRecording.Event> events = upcoming == null ? List.of() : upcoming.events;
        boolean rebuild = !sameEvents(events, displayed) || foldedPage != fold.isSelected();
        page = upcoming;
        String range = events.isEmpty() ? "" : "#" + events.get(0).sequence + " – #" + events.get(events.size() - 1).sequence;
        String status = pageSource == null ? "" : pageSource.error();
        if (pageSource != null && pageSource.isPending()) status = "Ready to start";
        if (status.isEmpty() && pageSource != null && pageSource.isComplete()) status = "Complete";
        pageLabel.setText(status.isEmpty() ? range : range.isEmpty() ? status : range + " · " + status);
        if (!rebuild) {
            if (pendingWrite >= 0 && selectSequence(pendingWrite, true)) pendingWrite = -1;
            updateControls();
            return;
        }
        int selected = history.getSelectedRow();
        long selectedSequence = selected >= 0 && selected < steps.size() ? steps.get(selected).first().sequence : -1;
        displayed = events;
        foldedPage = fold.isSelected();
        steps.clear();
        for (int i = 0; i < displayed.size(); i++) {
            int last = i;
            while (foldedPage && last + 1 < displayed.size()
                    && sameStep(displayed.get(last), displayed.get(last + 1))) last++;
            steps.add(new Step(i, last));
            i = last;
        }
        model.fireTableDataChanged();
        if (pendingWrite >= 0 && selectSequence(pendingWrite, true)) pendingWrite = -1;
        else if (selectedSequence >= 0) selectSequence(selectedSequence, false);
        if (history.getSelectedRow() < 0) showState(-1);
        updateControls();
    }

    private boolean selectSequence(long sequence, boolean scroll) {
        selectRow(step -> sequence >= step.first().sequence && sequence <= step.last().sequence, scroll);
        int selected = history.getSelectedRow();
        return selected >= 0 && sequence >= steps.get(selected).first().sequence && sequence <= steps.get(selected).last().sequence;
    }

    private static boolean sameEvents(List<InstructionRecording.Event> first, List<InstructionRecording.Event> second) {
        int last = first.size() - 1;
        return first.size() == second.size()
                && (last < 0 || first.get(0).sequence == second.get(0).sequence && first.get(last).sequence == second.get(last).sequence);
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
        StringBuilder text = new StringBuilder(step.location() + " · " + step.summary() + "\n");
        if (folded) text.append(step.size())
                .append(" matching steps; unfold to inspect each event.\n");
        String section = null;
        List<int[]> headings = new ArrayList<>();
        FontMetrics metrics = state.getFontMetrics(state.getFont());
        int labelWidth = 0;
        for (ViewRow row : step.rows()) {
            if (folded && navigational(row)) continue;
            if (!row.changed && !navigational(row) && !showUnchangedValues.isSelected()) continue;
            section = appendSection(text, section, row.group, headings);
            labelWidth = Math.max(labelWidth, metrics.stringWidth(row.label));
            text.append(row.label).append('\t').append(row.before);
            if (row.changed || !row.before.equals(row.after)) text.append(" → ").append(row.after);
            if (folded && row.changed && row.before.equals(row.after)) text.append(" (changed within run)");
            text.append("\n");
        }
        // Keep every row's links tied to its own original ordinal, including within folded groups.
        for (InstructionRecording.Event instruction : step.events()) {
            for (ViewRow row : instruction.view.rows) {
                if (folded && navigational(row)) {
                    section = appendSection(text, section, row.group, headings);
                    String label = "#" + instruction.sequence + " · " + row.label;
                    labelWidth = Math.max(labelWidth, metrics.stringWidth(label));
                    text.append(label).append("\t= ");
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
        SimpleAttributeSet columns = new SimpleAttributeSet();
        StyleConstants.setTabSet(columns, new TabSet(new TabStop[]{
                new TabStop(labelWidth + metrics.stringWidth("    "))
        }));
        state.getStyledDocument().setParagraphAttributes(0, text.length(), columns, false);
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
