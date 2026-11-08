/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.application.debugger.DebuggerPanel;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.debugtable.DebugTableImpl;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.editor.TabbedEditor;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.*;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SourceNavigationTest extends AbstractSwingTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test(timeout = 10000)
    public void compilerLabelsAppearInFirstColumnAndDisappearWhenSourceChanges() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            JTabbedPane tabs = findComponent(f.debugger, JTabbedPane.class, pane -> true);
            assertEquals(-1, onEdt(() -> tabs.indexOfTab("Symbols")).intValue());
            assertEquals("label", onEdt(() -> f.table.getColumnName(0)));
            assertEquals("first", onEdt(() -> f.table.getValueAt(0, 0)));
            assertEquals("second", onEdt(() -> f.table.getValueAt(1, 0)));
            assertEquals("third", onEdt(() -> f.table.getValueAt(2, 0)));
            assertEquals("", onEdt(() -> f.table.getValueAt(3, 0)));
            assertFalse(onEdt(() -> f.table.isCellEditable(1, 0)));
            assertEquals(onEdt(f.table::getBackground), onEdt(() ->
                    f.table.prepareRenderer(f.table.getCellRenderer(1, 0), 1, 0).getBackground()));
            runOnEdt(() -> f.table.setRowSelectionInterval(1, 1));
            assertEquals(onEdt(f.table::getSelectionBackground), onEdt(() ->
                    f.table.prepareRenderer(f.table.getCellRenderer(1, 0), 1, 0).getBackground()));
            assertEquals(onEdt(f.table::getBackground), onEdt(() ->
                    f.table.prepareRenderer(f.table.getCellRenderer(3, 0), 3, 0).getBackground()));
            runOnEdt(() -> { f.pane.insert("; edited\n", 0); f.table.clearSelection(); });
            assertEquals("", onEdt(() -> f.table.getValueAt(1, 0)));
            assertEquals(onEdt(f.table::getBackground), onEdt(() ->
                    f.table.prepareRenderer(f.table.getCellRenderer(1, 0), 1, 0).getBackground()));
        }
    }

    @Test(timeout = 10000)
    public void replacingMemoryLayoutRejectsOldSourceNavigationInBothDirections() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            net.emustudio.application.debugger.index.ProgramIndex previous = f.session.programIndex();
            when(f.cpu.getAddressSpaceSize()).thenReturn(64);
            assertNotSame(previous, f.session.programIndex());

            f.sourceLine(3);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> f.table.setRowSelectionInterval(0, 0));
            assertEquals(3, f.line());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void sourceNavigationRejectsCapturedMappingAfterMemoryMutation() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            assertTrue(f.session.sourceAt(2).isPresent());

            f.session.runMutation(() -> {});

            assertTrue(f.annotations.at(2).stream().anyMatch(entry -> entry.source != null));
            assertTrue(f.session.sourceAt(2).isEmpty());
        }
    }

    @Test(timeout = 10000)
    public void failedCompilationPreservesCapturedSourceMapping() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            assertTrue(f.session.sourceAt(2).isPresent());

            f.session.runCompilation(() -> f.compile(true));

            assertTrue(f.session.sourceAt(2).isPresent());
        }
    }

    @Test(timeout = 10000)
    public void mutationBeforeDeferredCompilerCaptureCannotPublishOldSourceAsCurrent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false, () -> f.session.runMutation(() -> {}));

            runOnEdt(() -> { f.debugger.showInstructions(); f.table.setRowSelectionInterval(0, 0); });
            f.sourceLine(2);
            assertEquals(0, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    @Test(timeout = 10000)
    public void editsWhileRunningStillExposeStaleSourcesWithoutMovingInspection() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            f.running(true);
            runOnEdt(() -> f.pane.insert("; edited while running\n", 0));
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            f.running(false);
            f.sourceLine(3);
            assertEquals(0, onEdt(() -> f.original.getLocationAt(f.table.getSelectedRow())).intValue());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void staleSourcesStopAutomaticFollowing() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            runOnEdt(() -> f.pane.insert("; edited\n", 0));
            f.sourceLine(3);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> { f.pane.setText(f.text); assertTrue(f.editor.saveFile()); });
            f.compile(false);
            f.sourceLine(2);
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void sourceMemorySymbolsAndEditorHistoryStaySynchronized() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            f.sourceLine(3);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            f.sourceHistory("Back");
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(2, f.line());
            f.sourceHistory("Forward");
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());

            assertEquals(3, f.line());

            runOnEdt(() -> f.table.setRowSelectionInterval(0, 0));
            assertEquals(1, f.line());
            runOnEdt(() -> f.table.setRowSelectionInterval(2, 2));
            assertEquals(3, f.line());
            verify(f.cpu, never()).setInstructionLocation(anyInt());
            assertEquals(0, f.bank.get());
        }
    }

    @Test(timeout = 10000)
    public void sourceLinesFollowAddresses() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(3);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());

            f.sourceLine(1);
            assertEquals(0, onEdt(f.table::getSelectedRow).intValue());
            f.sourceLine(2);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    @Test(timeout = 10000)
    public void instructionSelectionSurvivesViewSwitchAndRowCountRefresh() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(3);
            runOnEdt(f.debugger::showInstructions);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            ArgumentCaptor<javax.swing.event.TableModelListener> listener = ArgumentCaptor.forClass(javax.swing.event.TableModelListener.class);
            verify(f.original, atLeastOnce()).addTableModelListener(listener.capture());
            runOnEdt(() -> listener.getValue().tableChanged(new javax.swing.event.TableModelEvent(f.original)));
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(3, f.line());
        }
    }

    @Test(timeout = 10000)
    public void genericInstructionsFollowPcDuringStepping() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(3);
            clearInvocations(f.original);
            when(f.cpu.getInstructionLocation()).thenReturn(2);
            f.running(true);
            f.running(false);
            verify(f.original).currentPage();
            assertEquals(2, onEdt(() -> f.original.getLocationAt(f.table.getSelectedRow())).intValue());
        }
    }

    @Test(timeout = 10000)
    public void workerCompilationPublishesSourcesOnlyAfterHostCommandCompletes() throws Exception {
        try (Fixture f = new Fixture()) {
            CountDownLatch callbacksFinished = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<DebugSession.SourceCaptureGuard> captured = new AtomicReference<>();
            CompletableFuture<CPU.RunState> compilation = f.session.execution().compile(() ->
                    f.publishCompilation(false, () -> {
                        captured.set(f.session.captureSourceGuard());
                        callbacksFinished.countDown();
                        try { release.await(); }
                        catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(interrupted);
                        }
                    }));
            try {
                assertTrue(callbacksFinished.await(2, TimeUnit.SECONDS));
                runOnEdt(() -> assertFalse(captured.get().whenReady().isDone()));
                assertFalse(compilation.isDone());
            } finally { release.countDown(); }

            assertEquals(CPU.RunState.STATE_STOPPED_BREAK, compilation.get(2, TimeUnit.SECONDS));
            runOnEdt(() -> {});
            assertTrue(captured.get().whenReady().isDone());
            f.sourceLine(2);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            assertEquals("second", onEdt(() -> f.table.getValueAt(1, 0)));
        }
    }

    @Test(timeout = 10000)
    public void failedCompilationPreservesMappingsWhileDirtyEditsAndRunningCpuBlockNavigation() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> f.pane.insert("; edited\n", 0));
            f.sourceLine(4);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> assertTrue(f.editor.saveFile()));
            f.sourceLine(2);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> f.pane.setText(f.text));
            runOnEdt(() -> assertTrue(f.editor.saveFile()));
            f.sourceLine(2);
            f.compile(true);
            f.sourceLine(3);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            f.compile(false);
            f.sourceLine(2);
            f.running(true);
            f.sourceLine(3);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            f.running(false);
        }
    }

    @Test(timeout = 10000)
    public void savedExternalEditsCannotHideBehindAnUnchangedOpenBuffer() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            Files.writeString(f.path, "; externally saved\n" + f.text);
            f.sourceLine(3);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(f.text, onEdt(f.pane::getText));
        }
    }

    @Test(timeout = 10000)
    public void changesDuringCompilationNeverBindNewSourceToOldAddresses() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(2);
            runOnEdt(() -> f.compilerEvents.onStart());
            String changed = "third: nop\nfirst: nop\nsecond: nop\n";
            Files.writeString(f.path, changed);
            runOnEdt(() -> {
                f.pane.setText(changed);
                f.compilerEvents.onFinish(); // Old assembly succeeded, but its inputs have changed.
            });
            runOnEdt(() -> {});
            f.sourceLine(3);
            assertEquals(1, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    @Test(timeout = 10000)
    public void closingSourceNavigationDetachesCompilerListener() throws Exception {
        try (Fixture f = new Fixture()) {
            f.bank.set(1);
            f.compile(false); // Fixture resets selected CPU bank before queued UI work.
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(3);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(0, f.bank.get());
            runOnEdt(f.navigation::close);
            verify(f.compiler).removeCompilerListener(f.compilerEvents);
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    private final class Fixture implements AutoCloseable {
        final String text;
        final Path path;
        final CPU cpu = mock(CPU.class);
        final Compiler compiler = mock(Compiler.class);
        final MemoryContext<Byte> memory;
        final MemoryAnnotations annotations = new MemoryAnnotations();
        final AtomicInteger bank = new AtomicInteger();
        final DebugSession session;
        final TabbedEditor editor;
        final DebuggerPanel debugger;
        final JTable table;
        final TextEditorPane pane;
        final SourceNavigation navigation;
        final CompilerListener compilerEvents;
        final DebugTableModel original;

        Fixture() throws Exception { this("first: nop\nsecond: nop\nthird: nop\n"); }
        @SuppressWarnings({"unchecked", "rawtypes"})
        Fixture(String program) throws Exception {
            text = program;
            path = files.newFile("program.asm").toPath();
            Files.writeString(path, text);
            memory = mock(MemoryContext.class);
            when(memory.getSize()).thenReturn(32);
            when(memory.getCellTypeClass()).thenReturn(Byte.class);
            when(memory.read(anyInt())).thenReturn((byte) 0);
            when(cpu.getAddressSpaceSize()).thenReturn(32);
            when(cpu.getTitle()).thenReturn("Source navigation CPU");
            when(cpu.execute()).thenAnswer(call -> {
                try { new java.util.concurrent.CountDownLatch(1).await(); }
                catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                return CPU.RunState.STATE_RUNNING;
            });
            Disassembler disassembler = mock(Disassembler.class);
            when(disassembler.disassemble(anyInt())).thenAnswer(c -> new DisassembledInstruction(c.getArgument(0), "nop", "00"));
            when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(c -> (Integer) c.getArgument(0) + 2);
            when(cpu.getDisassembler()).thenReturn(disassembler);
            LexicalAnalyzer lexer = mock(LexicalAnalyzer.class);
            when(compiler.getSourcePositionColumnUnit()).thenReturn(SourceCodePosition.ColumnUnit.CODE_POINT);
            when(compiler.createLexer()).thenReturn(lexer);
            java.util.List<Token> tokens = new java.util.ArrayList<>();
            for (String name : List.of("first:", "second:", "third:")) {
                int start = text.indexOf(name), end = text.indexOf('\n', start);
                tokens.add(definition(name, start));
                int opcode = text.indexOf("nop", start);
                if (opcode >= 0 && opcode < end) tokens.add(new SourceToken(Token.RESERVED, opcode, "nop", null, null, null));
            }
            when(lexer.analyze(anyString())).thenReturn(tokens);
            session = new DebugSession(cpu, memory, null, annotations);
            editor = onEdt(() -> new TabbedEditor(mock(Dialogs.class), compiler, List.of(path), paths -> {}));
            pane = findComponent((java.awt.Container) editor.getView(), TextEditorPane.class, p -> true);
            original = mock(DebugTableModel.class);
            DebuggerColumn column = mock(DebuggerColumn.class);
            when(column.getClassType()).thenReturn(String.class);
            when(column.getDefaultWidth()).thenReturn(-1);
            when(original.getColumnCount()).thenReturn(1);
            when(original.getColumnName(0)).thenReturn("Instruction");
            when(original.getColumnAt(0)).thenReturn(column);
            when(original.getColumnClass(0)).thenReturn((Class) String.class);
            when(original.getRowCount()).thenReturn(4);
            when(original.getLocationAt(anyInt())).thenAnswer(c -> (Integer) c.getArgument(0) * 2);
            debugger = onEdt(() -> {
                EmuStudioGui gui = new EmuStudioGui();
                DebugTableImpl listing = new DebugTableImpl(original);
                JScrollPane scroll = gui.scrollPane(listing);
                JPanel instructions = new JPanel(new BorderLayout());
                instructions.add(scroll);
                return new DebuggerPanel(session, listing, scroll, instructions, p -> {}, () -> {}, gui, mock(Dialogs.class));
            });
            table = findComponent(debugger, JTable.class, t -> true);
            runOnEdt(() -> table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && debugger.isInstructionView() && table.getSelectedRow() >= 0)
                    debugger.followAddress(original.getLocationAt(table.convertRowIndexToModel(table.getSelectedRow())));
            }));
            navigation = onEdt(() -> new SourceNavigation(compiler, session, editor, debugger));
            ArgumentCaptor<CompilerListener> captor = ArgumentCaptor.forClass(CompilerListener.class);
            verify(compiler).addCompilerListener(captor.capture());
            compilerEvents = captor.getValue();
            showInFrame(debugger);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (!onEdt(findButtonByTooltip(debugger, "Analyze from PC")::isEnabled) && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(onEdt(findButtonByTooltip(debugger, "Analyze from PC")::isEnabled));
        }

        void compile(boolean fail) {
            compile(fail, () -> {});
        }

        void compile(boolean fail, Runnable afterFinish) {
            runOnEdt(() -> publishCompilation(fail, afterFinish));
            runOnEdt(() -> {});
        }

        void publishCompilation(boolean fail, Runnable afterFinish) {
            compilerEvents.onStart();
            if (fail) {
                compilerEvents.onMessage(new CompilerMessage(CompilerMessage.Type.ERROR, "bad source", null));
            } else {
                java.util.List<MemoryAnnotations.Entry> mappings = new java.util.ArrayList<>();
                for (int i = 0; i < 3; i++) mappings.add(new MemoryAnnotations.Entry(7, i, i * 2, i * 2 + 1,
                        "source", "", SourceCodePosition.of(i + 1,
                        text.lines().toArray(String[]::new)[i].indexOf(' ') + 1, path.toString())));
                for (int i = 0; i < 3; i++) mappings.add(new MemoryAnnotations.Entry(7, i + 3,
                        i * 2, i * 2 + 1, "label", List.of("first", "second", "third").get(i), null));
                annotations.replaceOwner(7, mappings);
            }
            compilerEvents.onFinish();
            afterFinish.run();
            bank.set(0);
        }


        void sourceLine(int line) {
            runOnEdt(() -> editor.setPosition(SourceCodePosition.of(line, 0, path.toString())));
        }

        int line() { return onEdt(() -> editor.currentSourcePosition().orElseThrow().line); }

        void sourceHistory(String name) {
            runOnEdt(() -> {
                JMenu menu = new JMenu("Edit");
                editor.extendEditingMenu(menu);
                JMenu navigate = (JMenu) java.util.Arrays.stream(menu.getMenuComponents())
                        .filter(c -> c instanceof JMenu && "Navigate".equals(((JMenu) c).getText())).findFirst().orElseThrow();
                for (java.awt.Component component : navigate.getMenuComponents())
                    if (component instanceof JMenuItem && name.equals(((JMenuItem) component).getText()))
                        ((JMenuItem) component).doClick(0);
            });
        }

        void running(boolean value) {
            if (value) session.execution().start().join();
            else session.execution().pause().join();
            runOnEdt(() -> {});
        }

        @Override public void close() {
            runOnEdt(() -> { navigation.close(); debugger.close(); });
            session.close();
        }

        private Token definition(String value, int offset) {
            return new SourceToken(Token.SYMBOL_DEFINITION, offset, value,
                    value.substring(0, value.length() - 1), "", "");
        }
    }
}
