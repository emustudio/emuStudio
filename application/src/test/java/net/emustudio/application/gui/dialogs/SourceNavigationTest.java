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
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SourceNavigationTest extends AbstractSwingTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test(timeout = 10000)
    public void replacingMemoryLayoutRejectsOldSourceNavigationInBothDirections() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            net.emustudio.application.debugger.index.ProgramIndex previous = f.session.programIndex();
            when(f.memory.getAddressSpaceSize()).thenReturn(64);
            assertNotSame(previous, f.session.programIndex());

            f.sourceLine(3);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            JLabel status = findComponent(f.debugger, JLabel.class,
                    label -> "Source synchronization status".equals(label.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(status::getText).contains("Memory image changed"));
            runOnEdt(() -> f.table.setRowSelectionInterval(0, 0));
            assertEquals(3, f.line());
            assertTrue(onEdt(status::getText).contains("Memory image changed"));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void mutationBeforeDeferredCompilerCaptureCannotPublishOldSourceAsCurrent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false, () -> f.session.runMutation(() -> {}));

            AtomicInteger mappings = new AtomicInteger();
            f.session.programIndex().snapshot().forEachMapping(mapping -> mappings.incrementAndGet());
            assertEquals(0, mappings.get());
            JLabel status = findComponent(f.debugger, JLabel.class,
                    label -> "Source synchronization status".equals(label.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(status::getText).contains("Source mappings unavailable"));
        }
    }

    @Test(timeout = 10000)
    public void editsWhileRunningStillExposeStaleSourcesWithoutMovingInspection() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(2);
            triggerButton(onEdt(() -> (JCheckBoxMenuItem) f.table.getComponentPopupMenu().getComponent(0)));
            f.running(true);
            runOnEdt(() -> f.pane.insert("; edited while running\n", 0));
            JLabel status = findComponent(f.debugger, JLabel.class,
                    label -> "Source synchronization status".equals(label.getAccessibleContext().getAccessibleName()));
            assertTrue(onEdt(status::getText).contains("Source changed"));
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            f.running(false);
            assertTrue(onEdt(status::getText).contains("Source changed"));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void pinAndStaleSourceStatesExplainWhyFollowingStops() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(2);
            JLabel source = findComponent(f.debugger, JLabel.class,
                    label -> "Source synchronization status".equals(label.getAccessibleContext().getAccessibleName()));
            assertFalse(onEdt(f.debugger::isInspectionPinned));
            triggerButton(onEdt(() -> (JCheckBoxMenuItem) f.table.getComponentPopupMenu().getComponent(0)));
            f.sourceLine(3);
            assertTrue(onEdt(f.debugger::isInspectionPinned));
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> f.pane.insert("; edited\n", 0));
            assertTrue(onEdt(source::getText).contains("Source changed"));
            assertTrue(onEdt(source::isVisible));
            JList<?> symbols = findComponent(f.debugger, JList.class,
                    list -> "Compiled symbols".equals(list.getAccessibleContext().getAccessibleName()));
            assertFalse(onEdt(symbols::isEnabled));
            runOnEdt(() -> { f.pane.setText(f.text); assertTrue(f.editor.saveFile()); });
            f.compile(false);
            assertTrue(onEdt(symbols::isEnabled));
            f.sourceLine(2);
            assertFalse(onEdt(source::isVisible));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void symbolFilteringPreservesTheQueryAndUsesFullMappingsForSourceFollowing() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            JTextField filter = findComponent(f.debugger, JTextField.class,
                    field -> "Filter symbols".equals(field.getAccessibleContext().getAccessibleName()));
            JList<?> symbols = findComponent(f.debugger, JList.class,
                    list -> "Compiled symbols".equals(list.getAccessibleContext().getAccessibleName()));
            setText(filter, "third");
            assertEquals(1, onEdt(() -> symbols.getModel().getSize()).intValue());
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            assertEquals("third", onEdt(filter::getText));
            JTabbedPane tabs = findComponent(f.debugger, JTabbedPane.class, pane -> true);
            runOnEdt(() -> { tabs.setSelectedIndex(tabs.indexOfTab("Symbols")); symbols.setSelectedIndex(0); });
            assertEquals(3, f.line());
            assertEquals("Symbols", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
        }
    }

    @Test(timeout = 10000)
    public void sourceMemorySymbolsAndEditorHistoryStaySynchronized() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            f.sourceLine(3);
            assertEquals(4, onEdt(f.table::getSelectedRow).intValue());
            f.sourceHistory("Back");
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(2, f.line());
            f.sourceHistory("Forward");
            assertEquals(4, onEdt(f.table::getSelectedRow).intValue());

            assertEquals(3, f.line());

            JTabbedPane tabs = findComponent(f.debugger, JTabbedPane.class, p -> true);
            runOnEdt(() -> tabs.setSelectedIndex(tabs.indexOfTab("Symbols")));
            JList<?> symbols = findComponent(f.debugger, JList.class,
                    list -> "Compiled symbols".equals(list.getAccessibleContext().getAccessibleName()));
            assertEquals(3, onEdt(() -> symbols.getModel().getSize()).intValue());
            runOnEdt(() -> symbols.setSelectedIndex(0));
            assertEquals(1, f.line());
            assertEquals("Symbols", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            runOnEdt(f.debugger::showMemory);
            assertEquals(0, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> f.table.setRowSelectionInterval(4, 4));
            assertEquals(3, f.line());
            assertTrue(onEdt(() -> symbols.getSelectedValue().toString().startsWith("third")));
            verify(f.cpu, never()).setInstructionLocation(anyInt());
            assertEquals(0, f.bank.get());
        }
    }

    @Test(timeout = 10000)
    public void sourceBookmarksFollowAddressesAndPinnedInspectionStopsAutomaticFollowing() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(3);
            f.sourceAction("Toggle bookmark");
            f.sourceLine(1);
            f.sourceAction("Next bookmark");
            assertEquals(3, f.line());
            assertEquals(4, onEdt(f.table::getSelectedRow).intValue());

            JCheckBoxMenuItem pin = onEdt(() -> (JCheckBoxMenuItem) f.table.getComponentPopupMenu().getComponent(0));
            triggerButton(pin);
            f.sourceLine(1);
            assertEquals(4, onEdt(f.table::getSelectedRow).intValue());
            assertTrue(onEdt(f.debugger::isInspectionPinned));
            triggerButton(pin);
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    @Test(timeout = 10000)
    public void instructionSelectionSurvivesViewSwitchAndRowCountRefresh() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
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
    public void genericInstructionPinKeepsListingAnchorDuringStepping() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showInstructions);
            f.sourceLine(3);
            clearInvocations(f.original);
            triggerButton(onEdt(() -> (JCheckBoxMenuItem) f.table.getComponentPopupMenu().getComponent(0)));
            when(f.cpu.getInstructionLocation()).thenReturn(2);
            f.running(true);
            f.running(false);
            verify(f.original, never()).currentPage();
            verify(f.original, never()).showAddress(anyInt());
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    @Test(timeout = 10000)
    public void symbolsRemainVisibleForForeignBankAndDataSelections() throws Exception {
        try (Fixture f = new Fixture("first: nop\nsecond: db 0\nthird: nop\n")) {
            f.bank.set(1);
            f.compile(false);
            JTabbedPane tabs = findComponent(f.debugger, JTabbedPane.class, p -> true);
            JList<?> symbols = findComponent(f.debugger, JList.class,
                    list -> "Compiled symbols".equals(list.getAccessibleContext().getAccessibleName()));
            runOnEdt(() -> tabs.setSelectedIndex(tabs.indexOfTab("Symbols")));
            clearInvocations(f.original);
            runOnEdt(() -> symbols.setSelectedIndex(2));
            assertEquals("Symbols", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            verify(f.original, never()).showAddress(4); // Must not decode the CPU's different bank.
            runOnEdt(() -> symbols.setSelectedIndex(1));
            assertEquals("Symbols", onEdt(() -> tabs.getTitleAt(tabs.getSelectedIndex())));
            verify(f.original, never()).showAddress(2); // Data never becomes speculative instructions.
            assertEquals(2, f.line());
            runOnEdt(f.debugger::showMemory);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(0, f.bank.get());
        }
    }

    @Test(timeout = 10000)
    public void dirtyAndSavedEditsFailedCompilationAndRunningCpuNeverUseOldMappings() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> f.pane.insert("; edited\n", 0));
            f.sourceLine(4);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            runOnEdt(() -> assertTrue(f.editor.saveFile()));
            f.sourceLine(2);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
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
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            f.running(false);
        }
    }

    @Test(timeout = 10000)
    public void savedExternalEditsCannotHideBehindAnUnchangedOpenBuffer() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(2);
            Files.writeString(f.path, "; externally saved\n" + f.text);
            f.sourceLine(3);
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            assertEquals(f.text, onEdt(f.pane::getText));
        }
    }

    @Test(timeout = 10000)
    public void changesDuringCompilationNeverBindNewSourceToOldAddresses() throws Exception {
        try (Fixture f = new Fixture()) {
            f.compile(false);
            runOnEdt(f.debugger::showMemory);
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
            assertEquals(2, onEdt(f.table::getSelectedRow).intValue());
            JList<?> symbols = findComponent(f.debugger, JList.class,
                    list -> "Compiled symbols".equals(list.getAccessibleContext().getAccessibleName()));
            assertEquals(0, onEdt(() -> symbols.getModel().getSize()).intValue());
        }
    }

    @Test(timeout = 10000)
    public void compileBankIsCapturedBeforeCpuResetAndCloseDetachesSourceListener() throws Exception {
        try (Fixture f = new Fixture()) {
            f.bank.set(1);
            f.compile(false); // Fixture resets selected CPU bank before queued UI work.
            runOnEdt(f.debugger::showMemory);
            f.sourceLine(3);
            assertEquals(4, onEdt(f.table::getSelectedRow).intValue());
            // Following compiled bank 1 must leave the live CPU bank at 0.
            assertEquals(0, f.bank.get());
            runOnEdt(f.navigation::close);
            verify(f.compiler).removeCompilerListener(f.compilerEvents);
            f.sourceLine(2);
            assertEquals(4, onEdt(f.table::getSelectedRow).intValue());
        }
    }

    private final class Fixture implements AutoCloseable {
        final String text;
        final Path path;
        final CPU cpu = mock(CPU.class);
        final Compiler compiler = mock(Compiler.class);
        final MemoryContext<Byte> memory;
        final Annotations annotations = new Annotations();
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
            when(memory.annotations()).thenReturn(annotations);
            when(memory.getSize()).thenReturn(32);
            when(memory.getAddressSpaceSize()).thenReturn(32);
            when(memory.getBanksCount()).thenReturn(2);
            when(memory.getCommonBoundary()).thenReturn(16);
            when(memory.getSelectedBank()).thenAnswer(c -> bank.get());
            when(memory.getCellTypeClass()).thenReturn(Byte.class);
            when(memory.readBank(anyInt(), anyInt())).thenReturn((byte) 0);
            when(cpu.getAddressSpaceSize()).thenReturn(32);
            when(cpu.getTitle()).thenReturn("Source navigation CPU");
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
            session = new DebugSession(cpu, memory, null);
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
            navigation = onEdt(() -> new SourceNavigation(compiler, session, editor, debugger));
            ArgumentCaptor<CompilerListener> captor = ArgumentCaptor.forClass(CompilerListener.class);
            verify(compiler).addCompilerListener(captor.capture());
            compilerEvents = captor.getValue();
            showInFrame(debugger);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (!onEdt(findButtonByTooltip(debugger, "Find")::isEnabled) && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(onEdt(findButtonByTooltip(debugger, "Find")::isEnabled));
        }

        void compile(boolean fail) {
            compile(fail, () -> {});
        }

        void compile(boolean fail, Runnable afterFinish) {
            runOnEdt(() -> {
                compilerEvents.onStart();
                annotations.clear();
                for (int i = 0; i < 3; i++) annotations.add(i * 2,
                        new SourceCodeAnnotation(7, SourceCodePosition.of(i + 1,
                                text.lines().toArray(String[]::new)[i].indexOf(' ') + 1, path.toString())));
                if (fail) compilerEvents.onMessage(new CompilerMessage(CompilerMessage.MessageType.TYPE_ERROR, "bad source"));
                compilerEvents.onFinish();
                afterFinish.run();
                bank.set(0);
            });
            runOnEdt(() -> {});
        }

        void sourceLine(int line) {
            runOnEdt(() -> editor.setPosition(SourceCodePosition.of(line, 0, path.toString())));
        }

        int line() { return onEdt(() -> editor.currentSourcePosition().orElseThrow().line); }

        void sourceAction(String name) {
            runOnEdt(() -> pane.getActionMap().get(name).actionPerformed(new java.awt.event.ActionEvent(pane, 0, name)));
        }

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
            ArgumentCaptor<CPU.CPUListener> captor = ArgumentCaptor.forClass(CPU.CPUListener.class);
            verify(cpu, atLeastOnce()).addCPUListener(captor.capture());
            runOnEdt(() -> captor.getAllValues().forEach(listener -> listener.runStateChanged(
                    value ? CPU.RunState.STATE_RUNNING : CPU.RunState.STATE_STOPPED_BREAK)));
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
