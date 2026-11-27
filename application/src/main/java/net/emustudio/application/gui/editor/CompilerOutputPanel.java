/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import static net.emustudio.emulib.ui.Constants.FONT_MONOSPACED;

public final class CompilerOutputPanel extends JPanel {
    private static final String OUTPUT_PREFIX = "Compile was successful.\n\tOutput: ";
    private static final String ENTRY_PREFIX = "\n\tProgram starts at ";
    private static final Set<String> LOAD_CONFIRMATIONS = Set.of(
            "Compiled file was loaded into memory.",
            "Compiled file was loaded into program memory.",
            "Compiled file was loaded into operating memory."
    );

    private final JLabel status;
    private final JLabel details;
    private final JTextArea log;
    private final JButton goToSource;
    private final NavigableMap<Integer, Diagnostic> diagnostics = new TreeMap<>();
    private boolean compiling;
    private String outputPath;
    private String entryAddress;
    private boolean loaded;
    private boolean memoryUnavailable;
    private int firstIssue;
    private int firstError;

    public CompilerOutputPanel(EmuStudioGui gui, Consumer<SourceCodePosition> navigateToSource) {
        super(new MigLayout("insets 0, fill, hidemode 3", "[grow]", "[][grow]"));

        JLabel title = gui.label("Compiler output");
        status = gui.labelBold("");
        details = gui.label("");
        details.setFont(FONT_MONOSPACED);
        goToSource = gui.button("Go to source");

        log = gui.textAreaReadOnly(3, 20);
        log.setFont(FONT_MONOSPACED);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        title.setLabelFor(log);
        ((DefaultCaret) log.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        Action openDiagnostic = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                Diagnostic diagnostic = diagnosticAt(log.getCaretPosition());
                if (diagnostic != null) navigateToSource.accept(diagnostic.position);
            }
        };
        goToSource.addActionListener(openDiagnostic);
        log.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openDiagnostic");
        log.getActionMap().put("openDiagnostic", openDiagnostic);
        log.addCaretListener(event -> updateGoToSource());
        log.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2) {
                    Diagnostic diagnostic = diagnosticAt(log.viewToModel2D(event.getPoint()));
                    if (diagnostic != null) navigateToSource.accept(diagnostic.position);
                }
            }
        });

        JPanel header = new JPanel(new MigLayout("insets 4 8, hidemode 3", "[][grow,fill][right]", ""));
        header.add(title);
        header.add(status, "gapleft 6");
        header.add(goToSource);
        header.add(details, "cell 0 1, span, growx, wmin 0");
        add(header, "growx, wrap");
        add(gui.scrollPane(log), "grow");
        clear();
    }

    public void clear() {
        if (compiling) return;
        diagnostics.clear();
        log.setText("");
        log.setToolTipText(null);
        log.getAccessibleContext().setAccessibleDescription(null);
        goToSource.setVisible(false);
        goToSource.setEnabled(false);
        status.setText("No compilation yet");
        details.setText("");
        details.setVisible(false);
        details.setToolTipText(null);
        outputPath = null;
        entryAddress = null;
        loaded = false;
        memoryUnavailable = false;
        firstIssue = -1;
        firstError = -1;
    }

    public void begin(Path source) {
        clear();
        compiling = true;
        status.setText("Compiling…");
        details.setText(source.getFileName().toString());
        details.setToolTipText(source.toString());
        details.setVisible(true);
    }

    public void append(CompilerMessage message) {
        int start = log.getDocument().getLength();
        if (message.getType() == CompilerMessage.Type.INFO) {
            readBuildDetails(message.getMessage());
        } else {
            if (firstIssue == -1) firstIssue = start;
            if (message.getType() == CompilerMessage.Type.WARNING) {
                memoryUnavailable |= message.getMessage().equals("Memory is not available.")
                        || message.getMessage().equals("Memory is not available");
            } else if (firstError == -1) firstError = start;
        }

        SourceCodePosition position = message.getPosition().orElse(null);
        String text = message.getMessage();
        if (position != null) {
            String location = basename(position.fileName);
            if (position.line > 0) {
                location += ":" + position.line;
                if (position.column >= 0) location += ":" + (position.column + 1L);
            }
            text = location + "  " + text;
        }
        if (message.getType() != CompilerMessage.Type.INFO || position != null) text = message.getType() + "  " + text;
        log.append(text + "\n");
        if (position != null) {
            diagnostics.put(start, new Diagnostic(log.getDocument().getLength(), position));
            if (!goToSource.isVisible()) {
                String hint = "Select a diagnostic and press Enter, or double-click it to open the source";
                log.setToolTipText(hint);
                log.getAccessibleContext().setAccessibleDescription(hint);
                goToSource.setVisible(true);
            }
            updateGoToSource();
        }
    }

    public void finish(boolean failed) {
        compiling = false;
        if (failed) {
            if (loaded) status.setText("Compiled and loaded · setup failed");
            else if (outputPath != null) status.setText("Compiled · load unconfirmed");
            else status.setText("Compilation failed");
        } else {
            if (loaded) status.setText("Compiled and loaded");
            else if (memoryUnavailable) status.setText("Compiled · memory unavailable");
            else status.setText("Compiled");
        }
        if (outputPath != null) {
            String filename = basename(outputPath);
            details.setText(entryAddress == null ? filename : filename + "   ·   Entry point " + entryAddress);
            details.setToolTipText(outputPath);
        }
        int offset = firstError >= 0 ? firstError : firstIssue;
        if (offset >= 0 && !log.hasFocus()) log.setCaretPosition(offset);
    }

    // The compiler API carries artifact and memory-load results in the bundled compilers' messages.
    private void readBuildDetails(String message) {
        loaded |= LOAD_CONFIRMATIONS.contains(message);
        if (message.startsWith(OUTPUT_PREFIX)) {
            int entry = message.lastIndexOf(ENTRY_PREFIX);
            outputPath = entry < 0 ? message.substring(OUTPUT_PREFIX.length())
                    : message.substring(OUTPUT_PREFIX.length(), entry);
            entryAddress = entry < 0 ? null : message.substring(entry + ENTRY_PREFIX.length());
        }
    }

    private Diagnostic diagnosticAt(int offset) {
        Map.Entry<Integer, Diagnostic> entry = diagnostics.floorEntry(offset);
        return entry != null && offset < entry.getValue().end ? entry.getValue() : null;
    }

    private void updateGoToSource() {
        Diagnostic diagnostic = diagnosticAt(log.getCaretPosition());
        goToSource.setEnabled(diagnostic != null);
        goToSource.setToolTipText(diagnostic == null ? "Select a diagnostic with a source location"
                : "Open " + diagnostic.position.fileName + " (Enter)");
    }

    private static String basename(String path) {
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
    }

    private static final class Diagnostic {
        private final int end;
        private final SourceCodePosition position;

        private Diagnostic(int end, SourceCodePosition position) {
            this.end = end;
            this.position = position;
        }
    }
}
