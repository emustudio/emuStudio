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
    private boolean failed;
    private Map<Path, String> inputs = Map.of();
    private volatile Path activeSource;
    private final CompilerListener listener = new CompilerListener() {
        @Override public void onStart() {
            final long current;
            synchronized (SourceNavigation.this) {
                current = ++compilation;
                failed = false;
                inputs = Map.of();
                mappings = SourceAddressMap.empty();
            }
            SwingUtilities.invokeLater(() -> {
                if (!closed && current == compilation) debugger.setSymbols(java.util.List.of(), SourceNavigation.this::symbolSelected);
            });
            try {
                Path root = SwingUtilities.isEventDispatchThread()
                        ? editor.getCurrentFile().map(java.io.File::toPath).orElse(null) : activeSource;
                Map<Path, String> captured = root == null ? Map.of() : SourceAddressMap.captureInputs(compiler, root);
                synchronized (SourceNavigation.this) { if (current == compilation) inputs = captured; }
            } catch (Exception e) {
                synchronized (SourceNavigation.this) { if (current == compilation) failed = true; }
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
                if (closed || failed) return;
                current = compilation;
                sourceInputs = inputs;
            }
            // Assemblers have installed annotations here; capture bank before CompileAction resets CPU.
            final SourceAddressMap captured;
            try { captured = SourceAddressMap.capture(compiler, session.memory, session.cpu.getDisassembler(), sourceInputs); }
            catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(SourceNavigation.class).debug("Source navigation unavailable", e);
                return;
            }
            synchronized (SourceNavigation.this) {
                if (closed || failed || current != compilation) return;
                mappings = captured;
            }
            SwingUtilities.invokeLater(() -> {
                if (closed || mappings != captured) return;
                debugger.setSymbols(captured.symbols(), SourceNavigation.this::symbolSelected);
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
        if (compiler != null) compiler.addCompilerListener(listener);
    }

    private boolean paused() {
        if (closed) return false;
        try { session.requirePaused(); return true; }
        catch (IllegalStateException e) { return false; }
    }

    private Optional<SourceAddressMap> current(SourceCodePosition position) {
        SourceAddressMap snapshot = mappings;
        if (!snapshot.sourceFiles().contains(Path.of(position.fileName).toAbsolutePath().normalize())
                || !snapshot.inputsUnchanged()) return Optional.empty();
        for (Path file : snapshot.sourceFiles())
            if (editor.sourceText(file).filter(text -> snapshot.matches(file, text)).isEmpty()) return Optional.empty();
        return mappings == snapshot ? Optional.of(snapshot) : Optional.empty();
    }

    private void sourceSelected(SourceCodePosition position) {
        activeSource = Path.of(position.fileName);
        if (synchronizing || !paused()) return;
        Optional<SourceAddressMap> snapshot = current(position);
        if (snapshot.isEmpty()) return;
        debugger.selectSymbol(position, -1, -1);
        snapshot.get().addressOf(position).ifPresent(location -> {
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
        snapshot.sourceAt(address, bank).filter(location -> current(location.position).filter(snapshot::equals).isPresent()).ifPresent(location -> {
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
            snapshot.get().addressOf(symbol.position)
                    .ifPresent(debugger::synchronizeSourceAddress);
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
