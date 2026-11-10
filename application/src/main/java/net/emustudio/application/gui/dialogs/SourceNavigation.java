/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.gui.debugger.DebuggerPanel;
import net.emustudio.application.emulation.debugger.SourceAddressMap;
import net.emustudio.application.emulation.debugger.SourceInputs;
import net.emustudio.application.gui.editor.TabbedEditor;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;

import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;

/** Coordinates view selections; execution state and source editing histories remain their owners'. */
final class SourceNavigation implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceNavigation.class);

    private final Compiler compiler;
    private final DebuggerSession session;
    private final TabbedEditor editor;
    private final DebuggerPanel debugger;
    private volatile SourceAddressMap mappings = SourceAddressMap.empty();
    private volatile boolean closed;
    private boolean synchronizing;
    private volatile long compilation;
    private volatile boolean compiling;
    private boolean failed, inputCaptureFailed;
    private SourceInputs inputs = SourceInputs.empty();
    private volatile Path activeSource;
    private final CompilerListener listener = new CompilerListener() {
        @Override public void onStart() {
            final long current;
            synchronized (SourceNavigation.this) {
                current = ++compilation;
                failed = false;
                inputCaptureFailed = false;
                compiling = true;
                inputs = SourceInputs.empty();
            }
            SwingUtilities.invokeLater(() -> {
                if (!closed && current == compilation) {
                    debugger.setSourceMappingsCurrent(false);
                }
            });
            try {
                Path root = SwingUtilities.isEventDispatchThread()
                        ? editor.getCurrentFile().map(java.io.File::toPath).orElse(null) : activeSource;
                SourceInputs captured = root == null ? SourceInputs.empty() : SourceInputs.capture(compiler, root);
                synchronized (SourceNavigation.this) { if (current == compilation) inputs = captured; }
            } catch (Exception e) {
                synchronized (SourceNavigation.this) { if (current == compilation) inputCaptureFailed = true; }
                LOGGER.debug("Source inputs unavailable", e);
            }
        }

        @Override public void onMessage(CompilerMessage message) {
            if (message.getType() == CompilerMessage.Type.ERROR)
                synchronized (SourceNavigation.this) { failed = true; }
        }

        @Override public void onFinish() {
            final long current;
            final SourceInputs sourceInputs;
            synchronized (SourceNavigation.this) {
                compiling = false;
                if (closed) return;
                current = compilation;
                if (failed || inputCaptureFailed) {
                    publishCurrent(current, false);
                    return;
                }
                sourceInputs = inputs;
            }
            // Publish after CompileAction's reset/mutation finishes.
            final DebuggerSession.SourceCaptureGuard guard;
            try {
                guard = session.captureSourceGuard();
            } catch (RuntimeException e) {
                publishCurrent(current, false);
                return;
            }
            guard.whenReady().thenRun(() -> SwingUtilities.invokeLater(() -> {
                synchronized (SourceNavigation.this) {
                    if (closed || failed || current != compilation) return;
                }
                final SourceAddressMap captured;
                try {
                    SourceAddressMap[] result = new SourceAddressMap[1];
                    session.applySourceCapture(guard, () -> {
                        try {
                            result[0] = SourceAddressMap.capture(compiler, session.memory, sourceInputs,
                                    session.memory == null ? null : session, session.annotationEngine);
                        } catch (java.io.IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        if (session.memory != null) session.setSourceMappings(result[0]);
                    });
                    captured = result[0];
                } catch (Exception e) {
                    LOGGER.debug("Source navigation unavailable", e);
                    publishCurrent(current, false);
                    return;
                }
                synchronized (SourceNavigation.this) {
                    if (closed || failed || current != compilation) return;
                    mappings = captured;
                }
                debugger.setSymbols(captured::symbols);
                debugger.setSourceMappingsCurrent(!captured.sourceFiles().isEmpty());
                editor.currentSourcePosition().ifPresent(SourceNavigation.this::sourceSelected);
            }));
        }
    };

    SourceNavigation(Compiler compiler, DebuggerSession session, TabbedEditor editor, DebuggerPanel debugger) {
        this.compiler = compiler;
        this.session = session;
        this.editor = editor;
        this.debugger = debugger;
        activeSource = editor.getCurrentFile().map(java.io.File::toPath).orElse(null);
        editor.setSourcePositionChanged(this::sourceSelected);
        debugger.setAddressSelectionListener(this::addressSelected);
        debugger.setInspectionReadyListener(() -> editor.currentSourcePosition().ifPresent(this::sourceSelected));
        debugger.setSymbols(java.util.List::of);
        debugger.setSourceMappingsCurrent(false);
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
        if (snapshot.sourceFiles().isEmpty() || (session.memory != null && snapshot.session() != session))
            return stale();
        if (!snapshot.inputsUnchanged()) return stale();
        for (Path file : snapshot.sourceFiles())
            if (editor.openSourceText(file).filter(text -> !snapshot.matches(file, text)).isPresent()) return stale();
        boolean mappedFile = snapshot.sourceFiles().contains(Path.of(position.fileName).toAbsolutePath().normalize());
        debugger.setSourceMappingsCurrent(true);
        if (!mappedFile) return Optional.empty();
        return mappings == snapshot ? Optional.of(snapshot) : Optional.empty();
    }

    private Optional<SourceAddressMap> stale() {
        debugger.setSourceMappingsCurrent(false);
        return Optional.empty();
    }

    private void publishCurrent(long generation, boolean current) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && compilation == generation) debugger.setSourceMappingsCurrent(current);
        });
    }

    private void sourceSelected(SourceCodePosition position) {
        activeSource = Path.of(position.fileName);
        if (synchronizing || closed) return;
        Optional<SourceAddressMap> snapshot = current(position);
        if (snapshot.isEmpty() || !paused()) return;
        Optional<SourceAddressMap.Location> address = snapshot.get().addressOf(position);
        address.ifPresent(location -> {
            synchronizing = true;
            try {
                debugger.synchronizeSourceAddress(location);
            } finally { synchronizing = false; }
        });
    }

    private void addressSelected(int address, boolean visit) {
        if (synchronizing || !paused()) return;
        SourceAddressMap snapshot = mappings;
        Optional<SourceAddressMap.Location> target = snapshot.sourceAt(address);
        if (target.isEmpty()) {
            editor.currentSourcePosition().flatMap(this::current).ifPresent(ignored ->
                    debugger.setSourceMappingsCurrent(true));
            return;
        }
        target.filter(location -> current(location.position).filter(snapshot::equals).isPresent()).ifPresent(location -> {
            synchronizing = true;
            try {
                if (location.instruction)
                    debugger.alignInstructionAddress(location.address);
                if (visit) editor.visitPosition(location.position);
                else editor.synchronizePosition(location.position);
            } finally { synchronizing = false; }
        });
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        if (compiler != null) compiler.removeCompilerListener(listener);
        editor.setSourcePositionChanged(position -> {});
        debugger.setAddressSelectionListener((address, visit) -> {});
        debugger.setInspectionReadyListener(() -> {});
    }
}
