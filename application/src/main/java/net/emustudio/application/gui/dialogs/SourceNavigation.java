/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.application.debugger.DebuggerPanel;
import net.emustudio.application.debugger.SourceAddressMap;
import net.emustudio.application.gui.editor.TabbedEditor;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;

import javax.swing.*;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** Coordinates view selections; execution state and source editing histories remain their owners'. */
final class SourceNavigation implements AutoCloseable {
    private final Compiler compiler;
    private final DebugSession session;
    private final TabbedEditor editor;
    private final DebuggerPanel debugger;
    private volatile SourceAddressMap mappings = SourceAddressMap.empty();
    private volatile boolean closed;
    private boolean synchronizing;
    private volatile long compilation;
    private volatile boolean compiling;
    private volatile String mappingStatus = "";
    private boolean failed, inputCaptureFailed;
    private Map<Path, String> inputs = Map.of();
    private volatile Path activeSource;
    private final CompilerListener listener = new CompilerListener() {
        @Override public void onStart() {
            final long current;
            synchronized (SourceNavigation.this) {
                current = ++compilation;
                failed = false;
                inputCaptureFailed = false;
                compiling = true;
                inputs = Map.of();
                mappings = SourceAddressMap.empty();
            }
            SwingUtilities.invokeLater(() -> {
                if (!closed && current == compilation) {
                    debugger.setSymbols(java.util.List.of(), SourceNavigation.this::symbolSelected);
                    showStatus("Compiling sources…", false);
                }
            });
            try {
                Path root = SwingUtilities.isEventDispatchThread()
                        ? editor.getCurrentFile().map(java.io.File::toPath).orElse(null) : activeSource;
                Map<Path, String> captured = root == null ? Map.of() : SourceAddressMap.captureInputs(compiler, root);
                synchronized (SourceNavigation.this) { if (current == compilation) inputs = captured; }
            } catch (Exception e) {
                synchronized (SourceNavigation.this) { if (current == compilation) inputCaptureFailed = true; }
                org.slf4j.LoggerFactory.getLogger(SourceNavigation.class).debug("Source inputs unavailable", e);
            }
        }

        @Override public void onMessage(CompilerMessage message) {
            if (message.getMessageType() == CompilerMessage.MessageType.TYPE_ERROR)
                synchronized (SourceNavigation.this) { failed = true; }
        }

        @Override public void onFinish() {
            final long current;
            final Map<Path, String> sourceInputs;
            synchronized (SourceNavigation.this) {
                compiling = false;
                if (closed) return;
                current = compilation;
                if (failed) {
                    mappingStatus = "Compilation failed — fix the source and compile again";
                    publishStatus(current, mappingStatus, false);
                    return;
                }
                if (inputCaptureFailed) {
                    mappingStatus = "Source inputs unavailable — save files and compile the program";
                    publishStatus(current, mappingStatus, false);
                    return;
                }
                sourceInputs = inputs;
            }
            // Capture the compiler bank now; publish after CompileAction's reset/mutation finishes.
            final int compiledBank;
            final DebugSession.SourceCaptureGuard guard;
            try {
                compiledBank = session.memory == null ? 0 : session.memory.getSelectedBank();
                guard = session.captureSourceGuard();
            } catch (RuntimeException e) {
                mappingStatus = "Source mappings unavailable: " + e.getMessage();
                publishStatus(current, mappingStatus, false);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                synchronized (SourceNavigation.this) {
                    if (closed || failed || current != compilation) return;
                }
                final SourceAddressMap captured;
                try {
                    SourceAddressMap[] result = new SourceAddressMap[1];
                    session.applySourceCapture(guard, () -> {
                        try {
                            result[0] = SourceAddressMap.capture(compiler, session.memory, session.cpu.getDisassembler(),
                                    sourceInputs, session.memory == null ? null : session.programIndex(), compiledBank);
                        } catch (java.io.IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        if (session.memory != null) session.setSourceMappings(result[0]);
                    });
                    captured = result[0];
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(SourceNavigation.class).debug("Source navigation unavailable", e);
                    mappingStatus = "Source mappings unavailable: " + e.getMessage();
                    publishStatus(current, mappingStatus, false);
                    return;
                }
                synchronized (SourceNavigation.this) {
                    if (closed || failed || current != compilation) return;
                    mappings = captured;
                    mappingStatus = captured.diagnostics().isEmpty() ? "Source mapping current"
                            : "Source mapping incomplete: " + captured.diagnostics().get(0);
                }
                debugger.setSymbols(captured.symbols(), SourceNavigation.this::symbolSelected);
                showStatus(mappingStatus, !captured.sourceFiles().isEmpty());
                editor.currentSourcePosition().ifPresent(SourceNavigation.this::sourceSelected);
            });
        }
    };

    SourceNavigation(Compiler compiler, DebugSession session, TabbedEditor editor, DebuggerPanel debugger) {
        this.compiler = compiler;
        this.session = session;
        this.editor = editor;
        this.debugger = debugger;
        activeSource = editor.getCurrentFile().map(java.io.File::toPath).orElse(null);
        editor.setSourcePositionChanged(this::sourceSelected);
        debugger.setAddressSelectionListener(this::addressSelected);
        debugger.setInspectionReadyListener(() -> editor.currentSourcePosition().ifPresent(this::sourceSelected));
        debugger.setSymbols(java.util.List.of(), this::symbolSelected);
        if (compiler == null) mappingStatus = "Source synchronization unavailable — no compiler";
        showStatus(mappingStatus, false);
        if (compiler != null) compiler.addCompilerListener(listener);
    }

    private boolean paused() {
        if (closed) return false;
        try { session.requirePaused(); return true; }
        catch (IllegalStateException e) { return false; }
    }

    private Optional<SourceAddressMap> current(SourceCodePosition position) {
        SourceAddressMap snapshot = mappings;
        if (compiling) return Optional.empty();
        if (snapshot.sourceFiles().isEmpty()) {
            showStatus(mappingStatus, false);
            return Optional.empty();
        }
        if (session.memory != null && snapshot.programIndex() != session.programIndex()) {
            showStatus("Memory image changed — compile the program to synchronize", false);
            return Optional.empty();
        }
        if (!snapshot.inputsUnchanged()) return stale();
        for (Path file : snapshot.sourceFiles())
            if (editor.sourceText(file).filter(text -> snapshot.matches(file, text)).isEmpty()) return stale();
        if (!snapshot.sourceFiles().contains(Path.of(position.fileName).toAbsolutePath().normalize())) {
            showStatus("This file is not part of the compiled program", true);
            return Optional.empty();
        }
        showStatus("Source mapping current", true);
        return mappings == snapshot ? Optional.of(snapshot) : Optional.empty();
    }

    private Optional<SourceAddressMap> stale() {
        showStatus("Source changed — compile the program to synchronize", false);
        return Optional.empty();
    }

    private void showStatus(String message, boolean current) {
        debugger.setSourceNavigationStatus(message, current);
    }

    private void publishStatus(long generation, String message, boolean current) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && compilation == generation) showStatus(message, current);
        });
    }

    private void sourceSelected(SourceCodePosition position) {
        activeSource = Path.of(position.fileName);
        if (synchronizing || closed) return;
        Optional<SourceAddressMap> snapshot = current(position);
        if (snapshot.isEmpty() || !paused()) return;
        debugger.selectSymbol(position, -1, -1);
        Optional<SourceAddressMap.Location> address = snapshot.get().addressOf(position);
        if (address.isEmpty()) showStatus("Source line has no current memory address", true);
        address.ifPresent(location -> {
            synchronizing = true;
            try {
                debugger.synchronizeSourceAddress(location);
                debugger.selectSymbol(location.position, location.address, location.bank);
            } finally { synchronizing = false; }
        });
    }

    private void addressSelected(int address, int bank, boolean visit) {
        if (synchronizing || !paused()) return;
        SourceAddressMap snapshot = mappings;
        Optional<SourceAddressMap.Location> target = snapshot.sourceAt(address, bank);
        if (target.isEmpty()) {
            editor.currentSourcePosition().flatMap(this::current).ifPresent(ignored ->
                    showStatus("Inspected address has no current source mapping", true));
            return;
        }
        target.filter(location -> current(location.position).filter(snapshot::equals).isPresent()).ifPresent(location -> {
            synchronizing = true;
            try {
                if (location.instruction && location.bank == session.memory.getBank(location.address))
                    debugger.alignInstructionAddress(location.address);
                debugger.selectSymbol(location.position, location.address, location.bank);
                if (visit) editor.visitPosition(location.position);
                else editor.synchronizePosition(location.position);
            } finally { synchronizing = false; }
        });
    }

    private void symbolSelected(SourceAddressMap.Symbol symbol) {
        if (synchronizing || !paused()) return;
        Optional<SourceAddressMap> snapshot = current(symbol.position);
        if (snapshot.isEmpty() || snapshot.get().symbols().stream().noneMatch(value -> value.name.equals(symbol.name)
                && value.position.fileName.equals(symbol.position.fileName) && value.position.line == symbol.position.line
                && value.position.column == symbol.position.column)) return;
        synchronizing = true;
        try {
            editor.visitPosition(symbol.position);
            Optional<SourceAddressMap.Location> address = snapshot.get().addressOf(symbol.position);
            if (address.isPresent()) debugger.synchronizeSourceAddress(address.get());
            else showStatus(symbol.sourceOnly
                    ? "Source-only declaration — no memory address"
                    : "Memory mapping changed — compile the program to synchronize", true);
        } finally { synchronizing = false; }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (compiler != null) compiler.removeCompilerListener(listener);
        editor.setSourcePositionChanged(position -> {});
        debugger.setAddressSelectionListener((address, bank, visit) -> {});
        debugger.setInspectionReadyListener(() -> {});
    }
}
