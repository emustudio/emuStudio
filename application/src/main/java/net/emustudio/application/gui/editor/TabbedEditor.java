/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.fife.rsta.ui.search.SearchEvent;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TabbedEditor implements Editor {
    private static final String UNTITLED = "Untitled";

    private final Dialogs dialogs;
    private final Compiler compiler;
    private final Consumer<List<Path>> openFilesChanged;
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<REditor> editors = new ArrayList<>();
    private final List<Runnable> fontSizeChangeListeners = new ArrayList<>();
    private EditorState state = new EditorState(null);
    private REditor focusedEditor, splitMirror;
    private SplitPaneView leftSplit, rightSplit;
    private int splitIndex = -1, secondaryIndex = -1;
    private InlineSearchBar searchBar;
    private final Deque<Location> back = new ArrayDeque<>(), forward = new ArrayDeque<>();
    private final Deque<Location> edits = new ArrayDeque<>();
    private Location location;
    private boolean navigating;
    private boolean synchronizingPosition;
    private Consumer<SourceCodePosition> sourcePositionChanged = position -> {};

    private static final class Location {
        final REditor editor;
        final int offset;

        Location(REditor editor, int offset) {
            this.editor = editor;
            this.offset = offset;
        }
    }

    private Runnable activeEditorChanged = () -> {
    };

    public TabbedEditor(Dialogs dialogs, Compiler compiler, Collection<Path> openFiles, Consumer<List<Path>> openFilesChanged) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.compiler = compiler;
        this.openFilesChanged = Objects.requireNonNull(openFilesChanged);

        Optional.ofNullable(openFiles).orElse(Collections.emptyList()).stream().filter(Files::isRegularFile).forEach(this::openFile);
        if (editors.isEmpty()) {
            addEditor(createEditor());
        }
        tabs.addChangeListener(e -> {
            if (splitIndex >= 0 && tabs.getSelectedIndex() == secondaryIndex) {
                tabs.setSelectedIndex(splitIndex);
                focusedEditor = editors.get(secondaryIndex);
                focusedEditor.grabFocus();
            } else focusedEditor = null;
            if (!editors.isEmpty()) {
                notifyActiveEditorChanged();
                notifyFontSizeChanged();
                remember(activeEditor(), activeEditor().textPane().getCaretPosition());
                notifySourcePosition(activeEditor());
            }
        });
        notifyOpenFilesChanged();
    }

    public void setActiveEditorChanged(Runnable activeEditorChanged) {
        this.activeEditorChanged = Objects.requireNonNull(activeEditorChanged);
    }

    private void notifyActiveEditorChanged() {
        updatePaneContext();
        activeEditorChanged.run();
    }

    private void updatePaneContext() {
        if (editors.isEmpty()) return;
        REditor active = activeEditor();
        if (leftSplit != null) leftSplit.update(active);
        if (rightSplit != null) rightSplit.update(active);
    }

    private final class SplitPaneView extends JPanel {
        final REditor editor;
        final String side;
        final JLabel heading = new JLabel();

        SplitPaneView(REditor editor, String side) {
            super(new BorderLayout());
            this.editor = editor;
            this.side = side;
            heading.setName("splitPaneTitle");
            heading.setOpaque(true);
            heading.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            add(heading, BorderLayout.NORTH);
            add(editor.getView());
        }

        void update(REditor active) {
            boolean selected = editor == active;
            heading.setText(side + " — " + title(editor) + (selected ? " — Active" : ""));
            heading.setToolTipText(editor.getCurrentFile().map(File::getAbsolutePath).orElse(UNTITLED));
            heading.setBackground(UIManager.getColor(selected ? "Table.selectionBackground" : "Panel.background"));
            heading.setForeground(UIManager.getColor(selected ? "Table.selectionForeground" : "Label.foreground"));
            heading.getAccessibleContext().setAccessibleName(heading.getText());
        }
    }

    public void setSourcePositionChanged(Consumer<SourceCodePosition> listener) {
        sourcePositionChanged = Objects.requireNonNull(listener);
    }

    public Optional<SourceCodePosition> currentSourcePosition() {
        if (editors.isEmpty()) return Optional.empty();
        REditor active = activeEditor();
        return active.getCurrentFile().flatMap(file -> {
            try {
                int offset = active.textPane().getCaretPosition();
                int line = active.textPane().getLineOfOffset(offset);
                return Optional.of(SourceCodePosition.of(line + 1,
                        offset - active.textPane().getLineStartOffset(line), file.toPath().toAbsolutePath().normalize().toString()));
            } catch (BadLocationException e) { return Optional.empty(); }
        });
    }

    /** Open buffers take precedence over disk, including unsaved edits and split mirrors. */
    public Optional<String> sourceText(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        for (REditor candidate : editors) {
            if (candidate.getCurrentFile().map(File::toPath).map(Path::toAbsolutePath)
                    .map(Path::normalize).filter(normalized::equals).isPresent())
                return Optional.of(candidate.textPane().getText());
        }
        try { return Optional.of(Files.readString(normalized)); }
        catch (IOException e) { return Optional.empty(); }
    }

    /** Follow a debugger selection without stealing focus, closing a split or recording an editor visit. */
    public boolean synchronizePosition(SourceCodePosition position) {
        boolean wasNavigating = navigating;
        synchronizingPosition = true;
        navigating = true;
        try {
            Path path = Path.of(position.fileName).toAbsolutePath().normalize();
            REditor target = null;
            for (REditor candidate : editors)
                if (candidate.getCurrentFile().map(File::toPath).map(Path::toAbsolutePath)
                        .map(Path::normalize).filter(path::equals).isPresent()) { target = candidate; break; }
            if (target == null) {
                target = createEditor();
                if (!target.openFile(path)) { target.dispose(); return false; }
                addEditor(target);
                tabs.setSelectedIndex(editors.indexOf(target));
                notifyOpenFilesChanged();
            } else if (target != activeEditor().sourceEditor()) {
                int index = editors.indexOf(target);
                if (index != secondaryIndex) tabs.setSelectedIndex(index);
                focusedEditor = target;
            } else target = activeEditor();
            target.setPosition(position);
            location = new Location(target.sourceEditor(), target.textPane().getCaretPosition());
            notifyActiveEditorChanged();
            return true;
        } finally {
            navigating = wasNavigating;
            synchronizingPosition = false;
        }
    }

    /** Explicit symbol/debugger visits participate in the existing #453 source history. */
    public boolean visitPosition(SourceCodePosition position) {
        Location previous = location;
        if (!synchronizePosition(position)) return false;
        if (previous != null && (previous.editor != location.editor || previous.offset != location.offset)) {
            back.push(previous);
            while (back.size() > 200) back.removeLast();
            forward.clear();
        }
        return true;
    }

    private void notifySourcePosition(REditor changed) {
        if (!synchronizingPosition && !editors.isEmpty() && changed == activeEditor())
            currentSourcePosition().ifPresent(sourcePositionChanged);
    }

    public boolean closeCurrent() {
        unsplit();
        return closeEditor(activeEditor());
    }

    private boolean closeEditor(REditor editor) {
        unsplit();
        editor.commands().persistBookmarks();
        if (!confirmSave(editor)) {
            return false;
        }

        int index = editors.indexOf(editor);
        editors.remove(index);
        tabs.removeTabAt(index);
        editor.dispose();
        if (editors.isEmpty()) {
            addEditor(createEditor());
        }
        notifyOpenFilesChanged();
        notifyActiveEditorChanged();
        notifyFontSizeChanged();
        grabFocus();
        return true;
    }

    public boolean confirmSaveAll() {
        unsplit();
        editors.forEach(editor -> editor.commands().persistBookmarks());
        for (int i = 0; i < editors.size(); i++) {
            REditor editor = editors.get(i);
            if (editor.isDirty()) {
                tabs.setSelectedIndex(i);
                if (!confirmSave(editor)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void newFile() {
        unsplit();
        REditor editor = createEditor();
        addEditor(editor);
        tabs.setSelectedIndex(editors.size() - 1);
        notifyActiveEditorChanged();
    }

    @Override
    public boolean openFile() {
        return activeEditor().chooseFileToOpen().map(this::openFile).orElse(false);
    }

    @Override
    public boolean openFile(Path fileName) {
        unsplit();
        Path normalized = fileName.toAbsolutePath().normalize();
        for (int i = 0; i < editors.size(); i++) {
            if (editors.get(i).getCurrentFile().map(File::toPath).map(Path::toAbsolutePath).map(Path::normalize).filter(normalized::equals).isPresent()) {
                tabs.setSelectedIndex(i);
                notifyActiveEditorChanged();
                return true;
            }
        }

        REditor editor = reusableUntitledEditor().orElseGet(this::createEditor);
        if (!editor.openFile(normalized)) {
            return false;
        }
        if (!editors.contains(editor)) {
            addEditor(editor);
        }
        tabs.setSelectedIndex(editors.indexOf(editor));
        updateTitle(editor);
        notifyOpenFilesChanged();
        notifyActiveEditorChanged();
        return true;
    }

    @Override
    public boolean saveFile() {
        return saved(activeEditor(), REditor::saveFile);
    }

    @Override
    public boolean saveFileAs() {
        return saved(activeEditor(), REditor::saveFileAs);
    }

    private boolean saved(REditor editor, Predicate<REditor> save) {
        boolean saved = save.test(editor);
        if (saved) {
            updateTitle(editor);
            notifyOpenFilesChanged();
        }
        return saved;
    }

    @Override
    public boolean isDirty() {
        return activeEditor().isDirty();
    }

    @Override
    public Optional<Boolean> findNext() {
        if (searchBar != null && !searchBar.find.getText().isEmpty()) return Optional.of(searchBar.next(true));
        return activeEditor().findNext();
    }

    @Override
    public Optional<Boolean> findPrevious() {
        if (searchBar != null && !searchBar.find.getText().isEmpty()) return Optional.of(searchBar.next(false));
        return activeEditor().findPrevious();
    }

    @Override
    public void clearMarkedOccurences() {
        activeEditor().clearMarkedOccurences();
    }

    @Override
    public Component getView() {
        return tabs;
    }

    @Override
    public void grabFocus() {
        activeEditor().grabFocus();
    }

    @Override
    public float getFontSize() {
        return activeEditor().getFontSize();
    }

    @Override
    public void setFontSize(float size) {
        activeEditor().setFontSize(size);
    }

    @Override
    public void addFontSizeChangeListener(Runnable listener) {
        fontSizeChangeListeners.add(Objects.requireNonNull(listener));
    }

    private void notifyFontSizeChanged() {
        if (!editors.isEmpty()) {
            fontSizeChangeListeners.forEach(Runnable::run);
        }
    }

    @Override
    public void setPosition(SourceCodePosition position) {
        activeEditor().setPosition(position);
    }

    @Override
    public Optional<File> getCurrentFile() {
        return activeEditor().getCurrentFile();
    }

    @Override
    public void searchEvent(SearchEvent event) {
        activeEditor().searchEvent(event);
    }

    @Override
    public String getSelectedText() {
        return activeEditor().getSelectedText();
    }

    private REditor createEditor() {
        REditor editor = new REditor(dialogs, compiler);
        editor.configure(state);
        observe(editor);
        editor.setOpenFileHandler(this::openFile);
        editor.addFontSizeChangeListener(() -> {
            if (editors.contains(editor) && editor == activeEditor()) {
                notifyFontSizeChanged();
            }
        });
        editor.addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            if (editors.contains(editor)) {
                updateTitle(editor);
                if (editor == activeEditor()) {
                    notifyActiveEditorChanged();
                }
            }
        }));
        return editor;
    }

    private void addEditor(REditor editor) {
        editors.add(editor);
        tabs.addTab(title(editor), editor.getView());
        tabs.setTabComponentAt(editors.size() - 1, new TabHeader(editor));
    }

    private final class TabHeader extends JPanel {
        private final JLabel label = new JLabel();
        private final JButton close = new JButton(new Icon() {
            public int getIconWidth() {
                return 10;
            }

            public int getIconHeight() {
                return 10;
            }

            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(component.getForeground());
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawLine(x + 1, y + 1, x + 8, y + 8);
                    g.drawLine(x + 8, y + 1, x + 1, y + 8);
                } finally {
                    g.dispose();
                }
            }
        });

        TabHeader(REditor editor) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            label.setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            close.setFocusable(false);
            close.setContentAreaFilled(false);
            close.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            close.setToolTipText("Close tab (Ctrl+W)");
            close.addActionListener(e -> closeEditor(editor));
            add(label);
            add(close);
            updateTitle(title(editor));
        }

        void updateTitle(String title) {
            label.setText(title);
            close.getAccessibleContext().setAccessibleName("Close " + title);
        }
    }

    REditor activeEditor() {
        return focusedEditor != null ? focusedEditor : editors.get(Math.max(0, tabs.getSelectedIndex()));
    }

    public void configure(AppSettings settings) {
        state = new EditorState(settings);
        editors.forEach(editor -> editor.configure(state));
    }

    List<REditor> editors() {
        return List.copyOf(editors);
    }

    void activate(REditor editor) {
        unsplit();
        int index = editors.indexOf(editor);
        if (index >= 0) {
            tabs.setSelectedIndex(index);
            focusedEditor = editor;
        }
    }

    public InlineSearchBar searchBar() {
        if (searchBar == null) {
            searchBar = new InlineSearchBar(this::activeEditor);
            editors.forEach(searchBar::observe);
            updatePaneContext();
        }
        return searchBar;
    }

    private void observe(REditor editor) {
        for (int i = 0; i <= 9; i++) {
            final String number = Integer.toString(i);
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
            editor.textPane().getInputMap().put(stroke, "workspaceBookmark" + number);
            editor.textPane().getActionMap().put("workspaceBookmark" + number, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    for (REditor candidate : editors) {
                        if (candidate.commands().bookmarks().containsValue(number)) {
                            activate(candidate);
                            candidate.commands().jumpNamed(number);
                            candidate.grabFocus();
                            break;
                        }
                    }
                }
            });
        }
        editor.textPane().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focusedEditor = editor;
                remember(editor, editor.textPane().getCaretPosition());
                notifyActiveEditorChanged();
                notifySourcePosition(editor);
            }
        });
        editor.textPane().addCaretListener(e -> {
            remember(editor, e.getDot());
            notifySourcePosition(editor);
        });
        editor.addChangeListener(() -> {
            if (!navigating) {
                edits.push(new Location(editor.sourceEditor(), editor.textPane().getCaretPosition()));
                while (edits.size() > 200) edits.removeLast();
            }
            notifySourcePosition(editor);
        });
        if (searchBar != null) searchBar.observe(editor);
    }

    private void remember(REditor editor, int offset) {
        if (navigating) return;
        editor = editor.sourceEditor();
        if (location != null && (location.editor != editor || Math.abs(location.offset - offset) > 1)) {
            back.push(location);
            while (back.size() > 200) back.removeLast();
            forward.clear();
        }
        location = new Location(editor, offset);
    }

    private void navigate(Deque<Location> from, Deque<Location> to) {
        if (from.isEmpty()) return;
        if (location != null) to.push(location);
        Location next = from.pop();
        if (next.editor == splitMirror) next = new Location(editors.get(splitIndex), next.offset);
        int index = editors.indexOf(next.editor);
        if (index < 0) {
            navigate(from, to);
            return;
        }
        navigating = true;
        try {
            unsplit();
            tabs.setSelectedIndex(index);
            focusedEditor = next.editor;
            next.editor.textPane().setCaretPosition(Math.min(next.offset, next.editor.textPane().getDocument().getLength()));
            location = next;
            next.editor.grabFocus();
            notifyActiveEditorChanged();
        } finally {
            navigating = false;
        }
    }

    public void extendEditingMenu(JMenu menu) {
        int standardItems = menu.getMenuComponentCount();
        Runnable refresh = () -> {
            while (menu.getMenuComponentCount() > standardItems) menu.remove(standardItems);
            menu.addSeparator();
            List<JMenu> groups = activeEditor().commands().menus();
            groups.forEach(menu::add);
            JMenu navigate = groups.get(2);
            navigate.addSeparator();
            addCommand(navigate, "Back", () -> navigate(back, forward));
            addCommand(navigate, "Forward", () -> navigate(forward, back));
            addCommand(navigate, "Previous edit", () -> navigate(edits, back));
            addCommand(navigate, "Bookmarks…", this::showBookmarks);
            menu.addSeparator();
            JMenu refactor = new JMenu("Refactor");
            addCommand(refactor, "Rename symbol…", () -> SourceRefactoring.rename(this));
            addCommand(refactor, "Extract selection…", () -> SourceRefactoring.extract(this));
            menu.add(refactor);
            JMenu split = new JMenu("Split view");
            addCommand(split, "Split with another file…", () -> split(false));
            addCommand(split, "Split current file", () -> split(true));
            addCommand(split, "Close split", this::unsplit);
            menu.add(split);
        };
        menu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                refresh.run();
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });
        refresh.run();
    }

    private void addCommand(JMenu menu, String name, Runnable command) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> {
            try {
                command.run();
            } catch (IllegalArgumentException | UncheckedIOException ex) {
                JOptionPane.showMessageDialog(tabs, ex.getMessage(), name, JOptionPane.ERROR_MESSAGE);
            }
        });
        menu.add(item);
    }

    private void showBookmarks() {
        List<Location> locations = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (REditor editor : editors) {
            editor.commands().bookmarks().forEach((line, name) -> {
                try {
                    locations.add(new Location(editor, editor.textPane().getLineStartOffset(line)));
                    labels.add(title(editor) + ":" + (line + 1) + (name.isEmpty() ? "" : " — " + name));
                } catch (BadLocationException ignored) {
                }
            });
        }
        if (labels.isEmpty()) return;
        JList<String> list = new JList<>(labels.toArray(new String[0]));
        list.setSelectedIndex(0);
        if (JOptionPane.showConfirmDialog(tabs, new JScrollPane(list), "Bookmarks", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            Deque<Location> selected = new ArrayDeque<>();
            selected.push(locations.get(list.getSelectedIndex()));
            navigate(selected, back);
        }
    }

    void split(boolean sameFile) {
        unsplit();
        int left = tabs.getSelectedIndex();
        REditor original = editors.get(left);
        REditor right;
        int other = -1;
        if (sameFile) {
            splitMirror = original.mirror();
            observe(splitMirror);
            right = splitMirror;
        } else {
            if (editors.size() < 2) return;
            List<REditor> options = new ArrayList<>(editors);
            options.remove(original);
            String[] labels = options.stream().map(editor -> title(editor) + " (tab " + (editors.indexOf(editor) + 1) + ")").toArray(String[]::new);
            String choice = (String) JOptionPane.showInputDialog(tabs, "File", "Split editor", JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
            if (choice == null) return;
            right = options.get(Arrays.asList(labels).indexOf(choice));
            other = editors.indexOf(right);
            tabs.setComponentAt(other, new JLabel("Shown in split editor"));
        }
        splitIndex = left;
        secondaryIndex = other;
        tabs.setComponentAt(left, new JPanel());
        leftSplit = new SplitPaneView(original, "Left");
        rightSplit = new SplitPaneView(right, "Right");
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit);
        original.getView().setVisible(true);
        right.getView().setVisible(true);
        split.setResizeWeight(0.5);
        tabs.setComponentAt(left, split);
        split.setDividerLocation(Math.max(1, tabs.getWidth() / 2));
        focusedEditor = original;
        notifyActiveEditorChanged();
    }

    void unsplit() {
        if (splitIndex < 0) return;
        REditor focused = activeEditor().sourceEditor();
        tabs.setComponentAt(splitIndex, editors.get(splitIndex).getView());
        if (secondaryIndex >= 0) tabs.setComponentAt(secondaryIndex, editors.get(secondaryIndex).getView());
        if (splitMirror != null) {
            splitMirror.commands().persistBookmarks();
            splitMirror.dispose();
            editors.get(splitIndex).commands().selections().installFilter();
            editors.get(splitIndex).commands().restoreBookmarks();
        }
        splitMirror = null;
        leftSplit = rightSplit = null;
        splitIndex = -1;
        secondaryIndex = -1;
        focusedEditor = null;
        tabs.setSelectedIndex(editors.indexOf(focused));
        notifyActiveEditorChanged();
    }


    private Optional<REditor> reusableUntitledEditor() {
        if (editors.size() == 1) {
            REditor editor = editors.get(0);
            if (!editor.isDirty() && editor.getCurrentFile().isEmpty()) {
                return Optional.of(editor);
            }
        }
        return Optional.empty();
    }

    private boolean confirmSave(REditor editor) {
        if (!editor.isDirty()) {
            return true;
        }
        Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
        if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
            return saved(editor, REditor::saveFile);
        }
        return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
    }

    private void updateTitle(REditor editor) {
        editor = editor.sourceEditor();
        int index = editors.indexOf(editor);
        if (index >= 0) {
            tabs.setTitleAt(index, title(editor));
            TabHeader header = (TabHeader) tabs.getTabComponentAt(index);
            if (header != null) {
                header.updateTitle(title(editor));
            }
        }
        updatePaneContext();
    }

    private String title(REditor editor) {
        String title = editor.getCurrentFile().map(File::getName).orElse(UNTITLED);
        return editor.isDirty() ? title + "*" : title;
    }

    private void notifyOpenFilesChanged() {
        openFilesChanged.accept(editors.stream().map(REditor::getCurrentFile).flatMap(Optional::stream).map(File::toPath).map(Path::toAbsolutePath).map(Path::normalize).collect(Collectors.toList()));
    }
}
