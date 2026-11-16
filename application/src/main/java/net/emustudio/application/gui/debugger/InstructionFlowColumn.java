/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.FlowAnalysis;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-row geometry and hit testing for the listing's observed-flow column.
 */
final class InstructionFlowColumn {
    private static final BasicStroke LINE = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke HIT = new BasicStroke(10f);
    private final DebugTableImpl table;
    private final DebugTableModel model;

    InstructionFlowColumn(DebugTableImpl table, DebugTableModel model) {
        this.table = table;
        this.model = model;
    }

    void paint(Graphics2D graphics) {
        List<Arrow> arrows = layout();
        if (arrows.isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.clip(columnBounds());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(LINE);
            Color color = UIManager.getColor("Component.linkColor");
            g.setColor(color == null ? new Color(0x2675bf) : color);
            for (Arrow arrow : arrows) arrow.paint(g);
            // The selected row may use a filled blue background; keep its arrow segments legible.
            int selected = table.getSelectedRow();
            g.clip(table.getCellRect(selected, flowViewColumn(), true));
            g.setColor(model.isRowAtCurrentInstruction(table.convertRowIndexToModel(selected))
                    ? Color.BLACK : table.getSelectionForeground());
            for (Arrow arrow : arrows) arrow.paint(g);
        } finally {
            g.dispose();
        }
    }

    int targetAt(Point point) {
        Arrow arrow = arrowAt(point);
        return arrow == null ? -1 : arrow.target;
    }

    String tooltipAt(Point point) {
        Arrow arrow = arrowAt(point);
        return arrow == null ? null : String.format("%04X → %04X — Go to %04X%s", arrow.edge.from,
                arrow.edge.to, arrow.target, arrow.offPage ? " (not on this page)" : "");
    }

    private Arrow arrowAt(Point point) {
        Rectangle bounds = columnBounds();
        if (bounds == null || !bounds.contains(point)) return null;
        List<Arrow> arrows = layout();
        Arrow closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow arrow = arrows.get(i);
            if (HIT.createStrokedShape(arrow.path).contains(point) || arrow.head.contains(point)) {
                double candidate = arrow.distanceTo(point);
                if (candidate < distance) {
                    closest = arrow;
                    distance = candidate;
                }
            }
        }
        return closest;
    }

    private int flowViewColumn() {
        return model.flowColumnIndex() < 0 ? -1 : table.convertColumnIndexToView(model.flowColumnIndex());
    }

    private Rectangle columnBounds() {
        int column = flowViewColumn();
        if (column < 0 || table.getRowCount() == 0) return null;
        Rectangle bounds = table.getCellRect(0, column, true);
        bounds.height = table.getHeight();
        return bounds.intersection(table.getVisibleRect());
    }

    private Map<Integer, Integer> rowLocations() {
        Map<Integer, Integer> rows = new HashMap<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            int address = model.getLocationAt(table.convertRowIndexToModel(row));
            if (address >= 0) rows.put(address, row);
        }
        return rows;
    }

    private List<Arrow> layout() {
        int selected = table.getSelectedRow();
        Rectangle visible = columnBounds();
        if (!table.isEnabled() || selected < 0 || selected >= table.getRowCount()
                || visible == null || visible.isEmpty()) return List.of();
        List<FlowAnalysis.Edge> edges = model.observedEdges(table.convertRowIndexToModel(selected));
        if (edges.isEmpty()) return List.of();
        int selectedAddress = model.getLocationAt(table.convertRowIndexToModel(selected));
        Rectangle cell = table.getCellRect(selected, flowViewColumn(), true);
        // Geometry uses the full column, not its horizontally clipped part.
        double startX = cell.x + 10;
        double laneStart = startX + 16;
        double laneWidth = Math.max(1, cell.width - 40);
        Map<Integer, Integer> rows = rowLocations();
        List<Arrow> arrows = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            FlowAnalysis.Edge edge = edges.get(i);
            boolean outgoing = edge.from == selectedAddress;
            int other = outgoing ? edge.to : edge.from;
            double lane = laneStart + laneWidth * (i + 1) / (edges.size() + 1);
            double selectedY = cell.getCenterY() + (outgoing ? 4 : -4);
            Integer otherRow = rows.get(other);
            Rectangle otherCell = otherRow == null ? null : table.getCellRect(otherRow, flowViewColumn(), true);
            boolean onPage = otherCell != null && visible.intersects(otherCell);
            Point2D.Double selectedPoint = new Point2D.Double(startX, selectedY);
            Point2D.Double otherPoint;
            boolean above = other < selectedAddress;
            if (onPage) {
                double y = otherCell.getCenterY() + (outgoing ? -4 : 4);
                otherPoint = new Point2D.Double(startX, y);
            } else {
                double y = above ? visible.y + 8 : visible.getMaxY() - 8;
                otherPoint = new Point2D.Double(lane, y);
            }
            Point2D.Double from = outgoing ? selectedPoint : otherPoint;
            Point2D.Double to = outgoing ? otherPoint : selectedPoint;
            Path2D.Double path;
            if (onPage) path = roundedPath(from, new Point2D.Double(lane, from.y),
                    new Point2D.Double(lane, to.y), to);
            else {
                Point2D.Double laneSelected = new Point2D.Double(lane, selectedY);
                path = roundedPath(from, laneSelected, to);
            }
            // Off-page outgoing arrows point toward the page edge; in-page arrows point toward rows.
            int verticalDirection = !onPage && outgoing ? (above ? -1 : 1) : 0;
            arrows.add(new Arrow(edge, other, path, from, to, verticalDirection, !onPage));
        }
        return arrows;
    }

    private static Path2D.Double roundedPath(Point2D.Double... points) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0].x, points[0].y);
        for (int i = 1; i < points.length - 1; i++) {
            Point2D.Double before = points[i - 1], at = points[i], after = points[i + 1];
            double incoming = before.distance(at), outgoing = at.distance(after);
            double radius = Math.min(5, Math.min(incoming, outgoing) / 2);
            if (radius == 0) {
                path.lineTo(at.x, at.y);
                continue;
            }
            path.lineTo(at.x + (before.x - at.x) * radius / incoming,
                    at.y + (before.y - at.y) * radius / incoming);
            path.quadTo(at.x, at.y, at.x + (after.x - at.x) * radius / outgoing,
                    at.y + (after.y - at.y) * radius / outgoing);
        }
        Point2D.Double end = points[points.length - 1];
        path.lineTo(end.x, end.y);
        return path;
    }

    private static final class Arrow {
        final FlowAnalysis.Edge edge;
        final int target;
        final Path2D.Double path;
        final Shape head;
        final Shape source;
        final boolean offPage;

        Arrow(FlowAnalysis.Edge edge, int target, Path2D.Double path, Point2D.Double from, Point2D.Double to,
              int verticalDirection, boolean offPage) {
            this.edge = edge;
            this.target = target;
            this.path = path;
            this.offPage = offPage;
            source = new Ellipse2D.Double(from.x - 2.3, from.y - 2.3, 4.6, 4.6);
            Path2D.Double triangle = new Path2D.Double();
            triangle.moveTo(to.x, to.y);
            if (verticalDirection == 0) {
                triangle.lineTo(to.x + 7, to.y - 4);
                triangle.lineTo(to.x + 7, to.y + 4);
            } else {
                triangle.lineTo(to.x - 4, to.y - verticalDirection * 7);
                triangle.lineTo(to.x + 4, to.y - verticalDirection * 7);
            }
            triangle.closePath();
            head = triangle;
        }

        void paint(Graphics2D g) {
            g.draw(path);
            g.fill(source);
            g.fill(head);
        }

        double distanceTo(Point point) {
            PathIterator segments = path.getPathIterator(null, 0.5);
            double[] coordinates = new double[6];
            double x = 0, y = 0, distance = Double.POSITIVE_INFINITY;
            while (!segments.isDone()) {
                int type = segments.currentSegment(coordinates);
                if (type == PathIterator.SEG_LINETO)
                    distance = Math.min(distance, Line2D.ptSegDist(x, y, coordinates[0], coordinates[1], point.x, point.y));
                x = coordinates[0];
                y = coordinates[1];
                segments.next();
            }
            return distance;
        }
    }
}
