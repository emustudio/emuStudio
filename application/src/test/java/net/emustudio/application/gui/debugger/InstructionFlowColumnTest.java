/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.TestCpu;
import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class InstructionFlowColumnTest {
    @Test public void foldedEndpointKeepsExactTargetAndSummaryShowsOnlyExternalEdges() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int at = 94; at < 100; at++) fixture.executed(at, at + 1);
            fixture.executed(93, 94);
            SwingUtilities.invokeAndWait(() -> {
                fixture.model.setFoldRepeats(true);
                fixture.select(100);
                int row = fixture.model.rowForAddress(99);
                assertTrue(fixture.model.isFolded(row));
                Rectangle cell = fixture.table.getCellRect(row, fixture.flowViewColumn(), true);
                Point endpoint = new Point(cell.x + 11, (int) cell.getCenterY());
                assertEquals(99, fixture.flow.targetAt(endpoint));
                assertFalse(fixture.flow.tooltipAt(endpoint).contains("not on this page"));
                AtomicInteger navigated = new AtomicInteger(-1);
                fixture.table.setFlowNavigation(navigated::set);
                fixture.click(endpoint, 1);
                assertEquals(99, navigated.get());

                fixture.table.setRowSelectionInterval(row, row);
                List<net.emustudio.application.emulation.debugger.FlowAnalysis.Edge> edges = fixture.model.observedEdges(row);
                assertEquals(1, edges.size());
                assertEquals(99, edges.get(0).from);
                assertEquals(100, edges.get(0).to);
                assertTrue(fixture.paintedPixels() > 0);
            });
        }
    }

    @Test public void selfLoopRemainsVisibleAtFoldBoundary() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int at = 94; at < 99; at++) fixture.executed(at, at + 1);
            fixture.executed(99, 99);
            SwingUtilities.invokeAndWait(() -> {
                fixture.model.setFoldRepeats(true);
                int row = fixture.model.rowForAddress(99);
                assertFalse(fixture.model.isFolded(row));
                assertTrue(fixture.model.isFolded(fixture.model.rowForAddress(98)));
                fixture.table.setRowSelectionInterval(row, row);
                assertTrue(fixture.model.observedEdges(row).stream().anyMatch(edge -> edge.from == 99 && edge.to == 99));
                assertTrue(fixture.paintedPixels() > 0);
            });
        }
    }

    @Test public void disclosureAndKeyboardToggleRunsWithoutSourceClickOrPcChange() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int at = 100; at < 110; at++) fixture.executed(at, at + 1);
            SwingUtilities.invokeAndWait(() -> {
                fixture.model.setFoldRepeats(true);
                int row = fixture.model.rowForAddress(101);
                assertTrue(fixture.model.isFolded(row));
                AtomicInteger sourceClicks = new AtomicInteger();
                fixture.table.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent event) { sourceClicks.incrementAndGet(); }
                });
                int addressColumn = fixture.table.convertColumnIndexToView(1);
                Rectangle cell = fixture.table.getCellRect(row, addressColumn, true);
                Point disclosure = new Point(cell.x + 7, (int) cell.getCenterY());
                fixture.click(disclosure, 1);
                assertFalse(fixture.model.isFolded(row));
                assertEquals(0, sourceClicks.get());
                fixture.table.setRowSelectionInterval(row, row);
                fixture.table.getActionMap().get("collapseRepeats").actionPerformed(
                        new java.awt.event.ActionEvent(fixture.table, 0, ""));
                assertTrue(fixture.model.isFolded(row));
                fixture.table.setRowSelectionInterval(row, row);
                fixture.table.getActionMap().get("expandRepeats").actionPerformed(
                        new java.awt.event.ActionEvent(fixture.table, 0, ""));
                assertFalse(fixture.model.isFolded(row));
                verify(fixture.cpu, never()).setInstructionLocation(anyInt());
            });
        }
    }

    @Test public void hiddenRunningValuesNeverDecodeDuringPaintingOrFoldGestures() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AtomicBoolean visible = new AtomicBoolean(true);
            AtomicInteger decodes = new AtomicInteger();
            when(fixture.cpu.getDecoder()).thenReturn(at -> {
                decodes.incrementAndGet();
                return DecodedInstruction.of(at, 1, "NOP", "00");
            });
            for (int at = 90; at < 110; at++) fixture.executed(at, at + 1);
            SwingUtilities.invokeAndWait(() -> {
                fixture.model.setPresentation(at -> "", visible::get, visible::get);
                fixture.model.setFoldRepeats(true);
                fixture.model.memoryChanged(0, 256);
                visible.set(false);
                fixture.table.setEnabled(false);
                when(TestCpu.context(fixture.cpu).getInstructionLocation()).thenReturn(101);
                int prepared = decodes.get();
                fixture.table.paint(new BufferedImage(fixture.table.getWidth(), fixture.table.getHeight(),
                        BufferedImage.TYPE_INT_RGB).getGraphics());
                fixture.model.toggleFoldAt(3);
                fixture.model.refreshFolding();
                fixture.model.setFoldRepeats(false);
                assertTrue(fixture.model.isFoldRepeats());
                assertEquals(prepared, decodes.get());
            });
        }
    }

    @Test public void selectionShowsBothDirectionsAndSelfLoopWithoutInspection() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.executed(99, 100);
            fixture.executed(100, 101);
            fixture.executed(100, 100);
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0, fixture.paintedPixels());
                fixture.select(100);
                assertTrue(fixture.paintedPixels() > 0);
                assertEquals(99, fixture.flow.targetAt(fixture.endpoint(99)));
                assertEquals(101, fixture.flow.targetAt(fixture.endpoint(101)));
                assertTrue(fixture.flow.tooltipAt(fixture.endpoint(99)).contains("0063 → 0064"));
                assertTrue(fixture.flow.tooltipAt(fixture.endpoint(101)).contains("0064 → 0065"));
                assertNull(fixture.session.flowAnalysis());
                fixture.table.clearSelection();
                assertEquals(0, fixture.paintedPixels());
            });
        }
    }

    @Test public void arrowGestureNavigatesOtherEndpointWithoutSelectingRowUnderPathOrOpeningSource() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.executed(99, 100);
            fixture.executed(100, 101);
            SwingUtilities.invokeAndWait(() -> {
                AtomicInteger target = new AtomicInteger(-1), sourceClicks = new AtomicInteger();
                fixture.table.setFlowNavigation(target::set);
                fixture.table.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent event) { sourceClicks.incrementAndGet(); }
                });
                fixture.select(100);
                fixture.click(fixture.endpoint(99), 2);
                assertEquals(99, target.get());
                assertEquals(100, fixture.selectedAddress());
                assertEquals(0, sourceClicks.get());
                fixture.click(fixture.endpoint(101), 1);
                assertEquals(101, target.get());
                assertEquals(100, fixture.selectedAddress());
                Point oldEndpoint = fixture.endpoint(101);
                fixture.table.setFlowNavigation(fixture::select);
                fixture.click(oldEndpoint, 1);
                fixture.click(oldEndpoint, 2);
                assertEquals(0, sourceClicks.get());
                verify(fixture.cpu, never()).setInstructionLocation(anyInt());
            });
        }
    }

    @Test public void offPageEndpointsKeepTooltipsAndClickableStubs() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.executed(2, 100);
            for (int address = 94; address < 100; address++) fixture.executed(address, address + 1);
            fixture.executed(100, 220);
            SwingUtilities.invokeAndWait(() -> {
                fixture.select(100);
                Rectangle column = fixture.table.getCellRect(0, fixture.flowViewColumn(), true);
                Point incoming = fixture.findStub(column, "0002");
                Point outgoing = fixture.findStub(column, "00DC");
                assertEquals(2, fixture.flow.targetAt(incoming));
                assertEquals(220, fixture.flow.targetAt(outgoing));
                assertTrue(fixture.flow.tooltipAt(incoming).contains("not on this page"));
                AtomicInteger target = new AtomicInteger(-1);
                fixture.table.setFlowNavigation(target::set);
                fixture.click(outgoing, 1);
                assertEquals(220, target.get());
            });
        }
    }

    @Test public void geometryFollowsColumnMovesResizesAndHorizontalViewport() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.executed(100, 101);
            SwingUtilities.invokeAndWait(() -> {
                fixture.select(100);
                int column = fixture.flowViewColumn();
                fixture.table.moveColumn(column, 0);
                fixture.table.getColumnModel().getColumn(0).setWidth(220);
                assertEquals(101, fixture.flow.targetAt(fixture.endpoint(101)));
                fixture.table.moveColumn(0, fixture.table.getColumnCount() - 1);
                JScrollPane scroll = new JScrollPane(fixture.table);
                scroll.setSize(240, 220);
                scroll.doLayout();
                fixture.table.setSize(fixture.table.getPreferredSize().width, 182);
                fixture.table.doLayout();
                scroll.getViewport().setViewPosition(new Point(450, 0));
                Point endpoint = fixture.endpoint(101);
                assertTrue(fixture.table.getVisibleRect().contains(endpoint));
                assertEquals(101, fixture.flow.targetAt(endpoint));
                scroll.getViewport().setViewPosition(new Point(0, 0));
                assertEquals(-1, fixture.flow.targetAt(endpoint));
            });
        }
    }

    @Test public void hiddenRunningValuesAndResetRemoveArrows() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.executed(100, 101);
            SwingUtilities.invokeAndWait(() -> {
                fixture.select(100);
                fixture.table.setEnabled(false);
                assertEquals(0, fixture.paintedPixels());
                assertEquals(-1, fixture.flow.targetAt(fixture.endpoint(101)));
                fixture.table.setEnabled(true);
                fixture.model.setPresentation(at -> "", () -> false, () -> false);
                assertEquals(0, fixture.paintedPixels());
                fixture.model.setPresentation(at -> "", () -> true, () -> true);
                assertTrue(fixture.paintedPixels() > 0);
                fixture.session.cpuReset();
                assertEquals(0, fixture.paintedPixels());
            });
        }
    }

    @Test public void denseOffPageStubsRemainClickableAtMinimumWidthAfterResize() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int source = 1; source <= 12; source++) fixture.executed(source, 100);
            for (int target = 200; target < 212; target++) fixture.executed(100, target);
            SwingUtilities.invokeAndWait(() -> {
                fixture.table.setSize(fixture.table.getWidth(), 100);
                fixture.model.setMaxRows(3);
                fixture.select(100);
                int column = fixture.flowViewColumn();
                fixture.table.setSize(fixture.table.getWidth(), 78);
                fixture.table.dispatchEvent(new java.awt.event.ComponentEvent(fixture.table,
                        java.awt.event.ComponentEvent.COMPONENT_RESIZED));
                assertEquals(3, fixture.model.getRowCount());
                fixture.table.getColumnModel().getColumn(column).setWidth(80);
                fixture.table.setSize(fixture.table.getColumnModel().getTotalColumnWidth(), 78);
                Rectangle cell = fixture.table.getCellRect(0, column, true);
                Map<Integer, Point> targets = new HashMap<>();
                for (int y : new int[] {8, fixture.table.getHeight() - 8}) {
                    for (int x = cell.x; x < cell.getMaxX(); x++) {
                        Point point = new Point(x, y);
                        String tooltip = fixture.flow.tooltipAt(point);
                        if (tooltip != null && tooltip.contains("not on this page"))
                            targets.putIfAbsent(fixture.flow.targetAt(point), point);
                    }
                }
                AtomicInteger navigated = new AtomicInteger(-1);
                fixture.table.setFlowNavigation(navigated::set);
                for (int address = 1; address <= 11; address++) {
                    assertTrue("Missing incoming source " + address, targets.containsKey(address));
                    fixture.click(targets.get(address), 1);
                    assertEquals(address, navigated.get());
                }
                for (int address = 200; address < 212; address++) {
                    assertTrue("Missing outgoing target " + address, targets.containsKey(address));
                    fixture.click(targets.get(address), 1);
                    assertEquals(address, navigated.get());
                }
            });
        }
    }

    @Test public void customCpuFlowTitleDoesNotStealApplicationColumnGeometry() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.executed(100, 101);
            SwingUtilities.invokeAndWait(() -> {
                fixture.model.setDebuggerColumns(List.of(DebuggerColumn.text("Flow", at -> "CPU value")));
                fixture.table.setSize(fixture.table.getColumnModel().getTotalColumnWidth(), 182);
                fixture.select(100);
                assertEquals(2, fixture.flowViewColumn());
                assertEquals("CPU value", fixture.model.getValueAt(fixture.table.getSelectedRow(), 1));
                assertEquals(101, fixture.flow.targetAt(fixture.endpoint(101)));
            });
        }
    }

    private static final class Fixture implements AutoCloseable {
        final CPU cpu = TestCpu.mockCpu();
        final DebuggerSession session;
        final DebugTableModel model = new DebugTableModel();
        DebugTableImpl table;
        InstructionFlowColumn flow;

        Fixture() throws Exception {
            when(cpu.getAddressSpaceSize()).thenReturn(256);
            when(TestCpu.context(cpu).getInstructionLocation()).thenReturn(100);
            when(cpu.getDecoder()).thenReturn(at -> DecodedInstruction.of(at, 1, "NOP", "00"));
            session = new DebuggerSession(cpu, TestCpu.context(cpu), null, null);
            SwingUtilities.invokeAndWait(() -> {
                model.setCPU(cpu, TestCpu.context(cpu));
                model.setDebuggerSession(session);
                table = new DebugTableImpl(model);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                table.setRowHeight(26);
                table.setSize(table.getPreferredSize().width, 182);
                table.doLayout();
                model.setMaxRows(7);
                flow = new InstructionFlowColumn(table, model);
            });
        }

        void executed(int address, int next) {
            session.afterStep(true, Optional.of(DecodedInstruction.of(address, 1, "NOP", "00")),
                    OptionalInt.of(next), Optional.empty());
        }

        int flowViewColumn() { return table.convertColumnIndexToView(model.flowColumnIndex()); }

        void select(int address) {
            model.showAddress(address);
            for (int row = 0; row < model.getRowCount(); row++)
                if (model.getLocationAt(row) == address) { table.setRowSelectionInterval(row, row); return; }
            throw new AssertionError("Missing address " + address);
        }

        int selectedAddress() { return model.getLocationAt(table.getSelectedRow()); }

        Point endpoint(int address) {
            for (int row = 0; row < model.getRowCount(); row++) {
                if (model.getLocationAt(row) == address) {
                    Rectangle cell = table.getCellRect(row, flowViewColumn(), true);
                    return new Point(cell.x + 11, (int) cell.getCenterY());
                }
            }
            throw new AssertionError("Endpoint not on page: " + address);
        }

        Point findStub(Rectangle column, String address) {
            for (int y : new int[] {8, table.getHeight() - 8}) {
                for (int x = column.x; x < column.getMaxX(); x++) {
                    Point point = new Point(x, y);
                    String tooltip = flow.tooltipAt(point);
                    if (tooltip != null && tooltip.contains("Go to " + address)) return point;
                }
            }
            throw new AssertionError("Missing off-page stub " + address);
        }

        void click(Point point, int count) {
            for (int type : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED})
                table.dispatchEvent(new MouseEvent(table, type, System.currentTimeMillis(), 0,
                        point.x, point.y, count, false, MouseEvent.BUTTON1));
        }

        int paintedPixels() {
            BufferedImage image = new BufferedImage(table.getWidth(), table.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try { flow.paint(graphics); } finally { graphics.dispose(); }
            int count = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                if ((image.getRGB(x, y) >>> 24) != 0) count++;
            return count;
        }

        @Override public void close() { session.close(); }
    }
}
