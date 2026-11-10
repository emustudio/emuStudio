/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.FlowAnalysis;
import net.emustudio.application.emulation.debugger.FlowGraph;
import net.emustudio.application.emulation.debugger.SourceAddressMap;
import net.emustudio.application.emulation.debugger.UserAnnotations;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.plugin.cpu.CPU;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.emustudio.application.gui.debugger.DebuggerWidgets.Selection;
import static net.emustudio.application.gui.debugger.DebuggerWidgets.addressRange;

/**
 * Instruction flow graph with annotation lanes; selecting nodes inspects addresses without changing PC.
 */
final class FlowGraphPanel extends JPanel {

    private static final int[] ZOOM_LEVELS = {50, 75, 100, 125, 150, 200, 250, 300};
    private static final String STALE = "Flow analysis is stale. Analyze again.";

    private final DebuggerSession session;
    private final Supplier<List<SourceAddressMap.Symbol>> symbols;
    private final BiConsumer<Integer, Boolean> inspect;
    private final Consumer<UserAnnotations.Entry> edit;
    private final Selection<MemoryAnnotations.Entry> selection;
    private FlowAnalysis flow;
    private int layoutPc = -1;
    private int start;
    private int selectedNode = -1;
    private final Map<Integer, Node> nodes = new LinkedHashMap<>();
    private final Map<Integer, Node> byMember = new HashMap<>();
    private List<FlowAnalysis.Edge> selectedEdges = List.of();
    private final List<AnnotationLane> lanes = new ArrayList<>();
    private int selectedLane = -1;
    private int zoomIndex = 2;
    private final JComboBox<String> zoomControl;
    private final JCheckBox foldControl = new JCheckBox("Fold identical repeats", true);
    private Point dragStart, dragView;
    private Point selectionStart;
    private Rectangle selectionArea, candidateLane;
    private boolean draggedSelection;

    /**
     * One graph box: a single instruction or a folded run of identical ones.
     */
    private static final class Node {
        final int address, end;
        final List<Integer> members;
        final Rectangle box;
        String label;

        Node(int address, List<Integer> members, int end, Rectangle box, String label) {
            this.address = address;
            this.members = members;
            this.end = end;
            this.box = box;
            this.label = label;
        }
    }

    private static final class AnnotationLane {
        final MemoryAnnotations.Entry annotation;
        final Rectangle bounds;
        final Rectangle header;

        AnnotationLane(MemoryAnnotations.Entry annotation, Rectangle bounds) {
            this.annotation = annotation;
            this.bounds = bounds;
            this.header = new Rectangle(bounds.x + 3, bounds.y + 3, 100, 24);
        }
    }

    FlowGraphPanel(DebuggerSession session, Selection<MemoryAnnotations.Entry> selection,
                   Supplier<List<SourceAddressMap.Symbol>> symbols, BiConsumer<Integer, Boolean> inspect,
                   Consumer<UserAnnotations.Entry> edit) {
        super(new BorderLayout());
        this.session = Objects.requireNonNull(session);
        this.symbols = Objects.requireNonNull(symbols);
        this.inspect = Objects.requireNonNull(inspect);
        this.edit = Objects.requireNonNull(edit);
        this.selection = Objects.requireNonNull(selection);
        selection.listen(annotation -> {
            if (annotation != null) showAnnotation(annotation);
            else {
                selectedLane = -1;
                repaint();
            }
        });
        foldControl.getAccessibleContext().setAccessibleName("Fold identical analysis repeats");
        foldControl.setToolTipText("Group identical consecutive instructions; keep entry points, labels and PC separate");
        foldControl.addActionListener(event -> {
            if (paused()) relayout();
        });
        setFocusable(true);
        setToolTipText("");
        getAccessibleContext().setAccessibleName("Instruction flow graph");
        getAccessibleContext().setAccessibleDescription("Select an instruction with the mouse or arrow keys; press Enter or double-click to navigate. Indirect targets may be unresolved.");
        zoomControl = new JComboBox<>(Arrays.stream(ZOOM_LEVELS).mapToObj(level -> level + "%").toArray(String[]::new));
        zoomControl.setSelectedIndex(zoomIndex);
        zoomControl.setName("Graph zoom");
        zoomControl.addActionListener(event -> setZoom(zoomControl.getSelectedIndex(), null));
        zoomControl.getAccessibleContext().setAccessibleName("Graph zoom");
        zoomControl.setToolTipText("Graph zoom (Ctrl+mouse wheel)");
        bindKeys();
        MouseAdapter mouse = new GraphMouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void bindKeys() {
        for (int code : new int[]{KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN}) {
            int delta = code == KeyEvent.VK_LEFT || code == KeyEvent.VK_UP ? -1 : 1;
            EmuStudioGui.bindKey(this, KeyStroke.getKeyStroke(code, 0), "flow" + code, () -> {
                List<Integer> addresses = new ArrayList<>(nodes.keySet());
                if (addresses.isEmpty()) return;
                int current = addresses.indexOf(selectedNode);
                selectInstruction(addresses.get(current < 0 ? 0 : Math.floorMod(current + delta, addresses.size())), true);
            });
        }
        EmuStudioGui.bindKey(this, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "followFlow", () -> {
            if (nodes.containsKey(selectedNode)) followNode(selectedNode);
        });
        for (int direction : new int[]{-1, 1}) {
            EmuStudioGui.bindKey(this, KeyStroke.getKeyStroke(direction < 0 ? KeyEvent.VK_UP : KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), direction < 0 ? "previousAnnotationLane" : "nextAnnotationLane", () -> {
                if (!lanes.isEmpty())
                    selectLane(selectedLane < 0 ? 0 : Math.floorMod(selectedLane + direction, lanes.size()));
            });
        }
    }

    /**
     * Wraps the graph in a scroll pane where Ctrl+wheel zooms around the pointer.
     */
    JScrollPane scrollPane(EmuStudioGui gui) {
        JScrollPane scroll = gui.scrollPane(this);
        scroll.getVerticalScrollBar().setUnitIncrement(30);
        scroll.getHorizontalScrollBar().setUnitIncrement(30);
        MouseWheelListener[] wheelListeners = scroll.getMouseWheelListeners();
        for (MouseWheelListener listener : wheelListeners) scroll.removeMouseWheelListener(listener);
        scroll.addMouseWheelListener(event -> {
            if (event.isControlDown() && event.getWheelRotation() != 0) {
                Point anchor = SwingUtilities.convertPoint(scroll, event.getPoint(), scroll.getViewport());
                setZoom(zoomIndex - event.getWheelRotation(), anchor);
                event.consume();
            } else {
                for (MouseWheelListener listener : wheelListeners) listener.mouseWheelMoved(event);
            }
        });
        return scroll;
    }

    JComboBox<String> zoomControl() {
        return zoomControl;
    }

    JCheckBox foldControl() {
        return foldControl;
    }

    boolean hasFlow() {
        return flow != null;
    }

    boolean current() {
        return flow != null && flow.isCurrent(session);
    }

    private boolean paused() {
        return session.execution().getRunState() != CPU.RunState.STATE_RUNNING;
    }

    private List<MemoryAnnotations.Entry> annotations() {
        return AnnotationEditor.userAnnotations(session.annotationEngine.all());
    }

    int start() {
        return start;
    }

    int selectedNode() {
        return selectedNode;
    }

    boolean hasSelectedNode() {
        return nodes.containsKey(selectedNode);
    }

    int layoutPc() {
        return layoutPc;
    }

    /**
     * Derived from the annotated range only, so an annotation keeps its color across sessions.
     */
    static Color laneColor(MemoryAnnotations.Entry annotation) {
        int hue = Math.floorMod((annotation.start * 31 + annotation.end) * 0x9E3779B9, 360);
        return Color.getHSBColor(hue / 360f, .55f, .78f);
    }

    void labelsChanged() {
        Map<Integer, String> labels = labels();
        nodes.values().forEach(node -> node.label = labels.getOrDefault(node.address, ""));
        repaint();
    }

    /**
     * Selects the node containing the address if it is part of the current graph.
     */
    void reveal(int address) {
        if (byMember.containsKey(address) && selectedNode != address) selectNode(address);
    }

    void followSelectedNode() {
        followNode(selectedNode);
    }

    /**
     * Rebuilds annotation lanes when there is no flow, keeping the selected annotation visible.
     */
    void resetLanes() {
        lanes.clear();
        selectedLane = -1;
        if (selection.get() != null) showAnnotation(selection.get());
        updateGraphSize();
        repaint();
    }

    private Point modelPoint(Point point) {
        double scale = scale();
        return new Point((int) (point.x / scale), (int) (point.y / scale));
    }

    private Rectangle viewRect(Rectangle rect) {
        double scale = scale();
        return new Rectangle((int) (rect.x * scale), (int) (rect.y * scale), (int) Math.ceil(rect.width * scale), (int) Math.ceil(rect.height * scale));
    }

    private double scale() {
        return ZOOM_LEVELS[zoomIndex] / 100.0;
    }

    private JViewport viewport() {
        return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
    }

    private static void setViewPosition(JViewport viewport, int x, int y) {
        Dimension view = viewport.getViewSize(), extent = viewport.getExtentSize();
        viewport.setViewPosition(new Point(Math.max(0, Math.min(x, Math.max(0, view.width - extent.width))), Math.max(0, Math.min(y, Math.max(0, view.height - extent.height)))));
    }

    private void setZoom(int index, Point anchor) {
        index = Math.max(0, Math.min(index, ZOOM_LEVELS.length - 1));
        if (index == zoomIndex) return;
        JViewport viewport = viewport();
        if (viewport != null && anchor == null) anchor = new Point(viewport.getWidth() / 2, viewport.getHeight() / 2);
        Point previous = viewport == null ? null : viewport.getViewPosition();
        double ratio = (double) ZOOM_LEVELS[index] / ZOOM_LEVELS[zoomIndex];
        zoomIndex = index;
        if (zoomControl.getSelectedIndex() != index) zoomControl.setSelectedIndex(index);
        updateGraphSize();
        if (viewport != null) {
            viewport.setViewSize(getPreferredSize());
            setViewPosition(viewport, (int) Math.round((previous.x + anchor.x) * ratio - anchor.x), (int) Math.round((previous.y + anchor.y) * ratio - anchor.y));
        }
        repaint();
    }

    private void selectLane(int lane) {
        showLane(lane);
        selection.set(lanes.get(lane).annotation);
    }

    private void showLane(int lane) {
        selectedLane = lane;
        AnnotationLane selected = lanes.get(lane);
        getAccessibleContext().setAccessibleDescription("Selected annotation: " + laneTitle(selected.annotation) + ". Alt Up and Alt Down select annotation lanes.");
        scrollRectToVisible(viewRect(selected.header));
        repaint();
    }

    private void editLane(int lane) {
        MemoryAnnotations.Entry annotation = lanes.get(lane).annotation;
        edit.accept(AnnotationEditor.editable(annotation));
    }

    private void showAnnotation(MemoryAnnotations.Entry annotation) {
        for (int i = 0; i < lanes.size(); i++) {
            if (lanes.get(i).annotation.entryId == annotation.entryId) {
                showLane(i);
                return;
            }
        }
        int top = nodes.values().stream().mapToInt(node -> node.box.y + node.box.height).max().orElse(10) + 25;
        for (AnnotationLane lane : lanes) top = Math.max(top, lane.bounds.y + lane.bounds.height + 12);
        lanes.add(new AnnotationLane(annotation, new Rectangle(5, top, 220, 45)));
        updateGraphSize();
        showLane(lanes.size() - 1);
    }

    private static String laneTitle(MemoryAnnotations.Entry annotation) {
        return annotation.text.isBlank() ? String.format("%04X", annotation.start) : annotation.text;
    }

    void selectNode(int address) {
        selectNode(address, true);
    }

    private void selectInstruction(int address, boolean scroll) {
        if (!current()) return;
        selectNode(address, scroll);
        inspect.accept(address, false);
    }

    private void selectNode(int address, boolean scroll) {
        if (!nodes.containsKey(address) && byMember.containsKey(address) && current()) setFlow(flow, address);
        selectedNode = address;
        Node selected = nodes.get(address);
        if (selected != null)
            getAccessibleContext().setAccessibleDescription(nodeDescription(selected) + ". Turn off Fold identical repeats to inspect individual instructions.");
        firePropertyChange("selectedNode", null, selectedNode);
        updateSelectedEdges();
        updateGraphSize();
        repaint();
        if (scroll && selected != null) scrollRectToVisible(viewRect(selected.box));
    }

    private void followNode(int address) {
        if (current()) inspect.accept(address, true);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (flow != null && !current()) return STALE;
        Point point = modelPoint(event.getPoint());
        Node node = nodeAt(point);
        if (node != null) return nodeDescription(node);
        for (AnnotationLane lane : lanes)
            if (lane.header.contains(point))
                return laneTitle(lane.annotation) + " · " + addressRange(lane.annotation.start, lane.annotation.end);
        return null;
    }

    private Node nodeAt(Point point) {
        for (Node node : nodes.values())
            if (node.box.contains(point)) return node;
        return null;
    }

    void relayout() {
        if (current()) setFlow(flow, selectedNode);
    }

    private static String nodeAddress(Node node) {
        return node.members.size() > 1 ? addressRange(node.address, node.end) : String.format("%04X", node.address);
    }

    private String nodeMnemonic(Node node) {
        int count = node.members.size();
        return flow.mnemonic(node.address) + (count > 1 ? " × " + count : "");
    }

    private String nodeDescription(Node node) {
        String unknown = flow.unknown().get(node.address);
        return (node.label.isEmpty() ? "" : node.label + " · ") + nodeAddress(node) + " · " + nodeMnemonic(node) + (flow.isData(node.address) ? " · Data region" : "") + (unknown == null ? "" : " · " + unknown);
    }

    /**
     * Shows flow analysed from the start address and selects the given address.
     */
    void setFlow(FlowAnalysis flow, int start, int selectedAddress) {
        this.start = start;
        setFlow(flow, selectedAddress);
    }

    private void setFlow(FlowAnalysis flow, int selectedAddress) {
        this.flow = flow;
        nodes.clear();
        byMember.clear();
        lanes.clear();
        selectedLane = -1;
        selectedNode = selectedAddress;
        Map<Integer, FlowGraph.Instruction> instructions = new HashMap<>();
        flow.forEachInstruction(instruction -> instructions.put(instruction.cells().start, instruction));
        Map<Integer, List<Integer>> successors = new HashMap<>();
        flow.forEachEdge((from, to) -> successors.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to));
        Deque<Integer> pending = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            int at = pending.removeFirst();
            if (!visited.add(at)) continue;
            if (!instructions.containsKey(at)) {
                if (!flow.unknown().containsKey(at)) visited.remove(at);
                continue;
            }
            pending.addAll(successors.getOrDefault(at, List.of()));
        }
        layoutPc = session.execution().cpuContext().getInstructionLocation();
        Set<Integer> boundaries = new HashSet<>(List.of(start, layoutPc, selectedAddress));
        Map<Integer, String> labels = labels();
        boundaries.addAll(labels.keySet());
        List<MemoryAnnotations.Entry> annotations = annotations();
        for (MemoryAnnotations.Entry annotation : annotations) {
            instructions.forEach((at, instruction) -> {
                if ((at <= annotation.start && annotation.start < instruction.cells().end) || (at < annotation.end && annotation.end <= instruction.cells().end))
                    boundaries.add(at);
            });
        }
        List<List<Integer>> runs = foldControl.isSelected() ? flow.instructionRuns(visited, boundaries) : new TreeSet<>(visited).stream().map(List::of).collect(Collectors.toList());
        for (List<Integer> run : runs) {
            int at = run.get(0);
            FlowGraph.Instruction last = instructions.get(run.get(run.size() - 1));
            Node node = new Node(at, run, last == null ? at + 1 : last.cells().end,
                    new Rectangle(30, 25 + nodes.size() * 90, 210, 60), labels.getOrDefault(at, ""));
            nodes.put(at, node);
            run.forEach(member -> byMember.put(member, node));
        }
        List<MemoryAnnotations.Entry> visible = annotations.stream().filter(annotation -> nodes.values().stream().anyMatch(node -> covers(annotation, node))).collect(Collectors.toList());
        int nodeX = 30 + visible.size() * 110;
        nodes.values().forEach(node -> node.box.x = nodeX);
        for (int i = 0; i < visible.size(); i++) {
            MemoryAnnotations.Entry annotation = visible.get(i);
            List<Rectangle> covered = new ArrayList<>();
            for (Node node : nodes.values())
                if (covers(annotation, node)) covered.add(node.box);
            int top = covered.get(0).y - 8, bottom = covered.get(covered.size() - 1).y + 68;
            int x = 5 + i * 110;
            lanes.add(new AnnotationLane(annotation, new Rectangle(x, top, nodeX + 220 + i * 4 - x, bottom - top)));
        }
        if (selectedNode >= 0 && !nodes.containsKey(selectedNode) && !nodes.isEmpty())
            selectedNode = nodes.keySet().iterator().next();
        if (selection.get() != null) showAnnotation(selection.get());
        firePropertyChange("selectedNode", null, selectedNode);
        updateSelectedEdges();
        updateGraphSize();
        revalidate();
        repaint();
    }

    private Map<Integer, String> labels() {
        Map<Integer, Set<String>> annotations = new HashMap<>(), symbols = new HashMap<>();
        for (MemoryAnnotations.Entry entry : annotations()) {
            String label = entry.text.trim();
            if (!label.isEmpty()) annotations.computeIfAbsent(entry.start, ignored -> new LinkedHashSet<>()).add(label);
        }
        for (SourceAddressMap.Symbol symbol : this.symbols.get())
            symbol.location.ifPresent(location -> symbols.computeIfAbsent(location.address, ignored -> new LinkedHashSet<>()).add(symbol.name));
        Map<Integer, String> labels = new HashMap<>();
        Set<Integer> keys = new HashSet<>(annotations.keySet());
        keys.addAll(symbols.keySet());
        for (int key : keys) {
            String annotation = String.join(", ", annotations.getOrDefault(key, Set.of()));
            String source = String.join(", ", symbols.getOrDefault(key, Set.of()));
            labels.put(key, annotation.isEmpty() ? source : source.isEmpty() || source.equals(annotation) ? annotation : annotation + ", " + source);
        }
        return labels;
    }

    private static boolean covers(MemoryAnnotations.Entry annotation, Node node) {
        return Math.max(node.address, annotation.start) < Math.min(node.end, annotation.end);
    }

    private void updateSelectedEdges() {
        List<FlowAnalysis.Edge> edges = new ArrayList<>();
        if (flow != null) flow.forEachEdge((from, to) -> {
            Node source = byMember.get(from), target = byMember.get(to);
            if (source == null || target == null) return;
            if (source == target && from != to) return;
            if (source.address == selectedNode || target.address == selectedNode)
                edges.add(new FlowAnalysis.Edge(source.address, target.address));
        });
        selectedEdges = edges;
    }

    private void updateGraphSize() {
        double scale = scale();
        int bottom = lanes.stream().mapToInt(lane -> lane.bounds.y + lane.bounds.height + 20).max().orElse(0);
        setPreferredSize(new Dimension((int) Math.ceil(Math.max(460, 290 + lanes.size() * 114 + selectedEdges.size() * 20) * scale), (int) Math.ceil(Math.max(Math.max(400, nodes.size() * 90 + 50), bottom) * scale)));
        revalidate();
    }

    private final class GraphMouse extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && current()) {
                selectionStart = e.getPoint();
                draggedSelection = false;
                return;
            }
            if (!SwingUtilities.isRightMouseButton(e)) return;
            JViewport viewport = viewport();
            if (viewport != null) {
                dragStart = SwingUtilities.convertPoint(FlowGraphPanel.this, e.getPoint(), viewport);
                dragView = viewport.getViewPosition();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragStart = dragView = null;
            if (!SwingUtilities.isLeftMouseButton(e) || selectionStart == null) return;
            selectionStart = null;
            if (!draggedSelection) return;
            List<Node> selected = new ArrayList<>();
            for (Node node : nodes.values())
                if (selectionArea != null && selectionArea.intersects(node.box)) selected.add(node);
            selectionArea = null;
            if (!selected.isEmpty() && current()) {
                int first = selected.stream().mapToInt(node -> node.address).min().getAsInt();
                int end = selected.stream().mapToInt(node -> node.end).max().getAsInt();
                edit.accept(new UserAnnotations.Entry(-1, first, end, "", "CODE"));
            }
            candidateLane = null;
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectionStart != null && (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                if (!draggedSelection && selectionStart.distance(e.getPoint()) < 4) return;
                draggedSelection = true;
                Point a = modelPoint(selectionStart), b = modelPoint(e.getPoint());
                selectionArea = new Rectangle(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(1, Math.abs(a.x - b.x)), Math.max(1, Math.abs(a.y - b.y)));
                candidateLane = null;
                for (Node node : nodes.values())
                    if (selectionArea.intersects(node.box))
                        candidateLane = candidateLane == null ? new Rectangle(node.box) : candidateLane.union(node.box);
                if (candidateLane != null) candidateLane.grow(8, 8);
                repaint();
                return;
            }
            JViewport viewport = viewport();
            if (viewport == null || dragStart == null) return;
            Point point = SwingUtilities.convertPoint(FlowGraphPanel.this, e.getPoint(), viewport);
            setViewPosition(viewport, dragView.x + dragStart.x - point.x, dragView.y + dragStart.y - point.y);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            if (draggedSelection) {
                draggedSelection = false;
                return;
            }
            Point point = modelPoint(e.getPoint());
            Node node = nodeAt(point);
            if (node != null) {
                requestFocusInWindow();
                selectInstruction(node.address, false);
                if (e.getClickCount() == 2) followNode(node.address);
                return;
            }
            for (int i = 0; i < lanes.size(); i++) {
                Rectangle bounds = lanes.get(i).bounds;
                Rectangle inner = new Rectangle(bounds);
                inner.grow(-5, -5);
                if (lanes.get(i).header.contains(point) || (bounds.contains(point) && !inner.contains(point))) {
                    requestFocusInWindow();
                    selectLane(i);
                    if (e.getClickCount() == 2) editLane(i);
                    return;
                }
            }
            if (e.getClickCount() == 2) {
                for (int i = lanes.size() - 1; i >= 0; i--) {
                    if (lanes.get(i).bounds.contains(point)) {
                        selectLane(i);
                        editLane(i);
                        return;
                    }
                }
            }
            selectedLane = -1;
            selection.set(null);
            selectNode(-1);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (flow == null && lanes.isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = scale();
        g.scale(scale, scale);
        Rectangle clip = g.getClipBounds();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (candidateLane != null)
            paintSelection(g, new RoundRectangle2D.Float(candidateLane.x, candidateLane.y, candidateLane.width, candidateLane.height, 14, 14), 40);
        paintLanes(g);
        paintEdges(g, clip);
        paintNodes(g, clip);
        if (selectionArea != null) paintSelection(g, selectionArea, 35);
        g.dispose();
    }

    private static void paintSelection(Graphics2D g, Shape shape, int alpha) {
        g.setColor(new Color(55, 120, 200, alpha));
        g.fill(shape);
        g.setColor(new Color(55, 120, 200));
        g.draw(shape);
    }

    private void paintLanes(Graphics2D g) {
        for (AnnotationLane lane : lanes) {
            Rectangle bounds = lane.bounds;
            Color color = laneColor(lane.annotation);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);
        }
        for (int i = 0; i < lanes.size(); i++) {
            AnnotationLane lane = lanes.get(i);
            g.setColor(Color.getHSBColor((i * .19f) % 1f, .65f, .65f));
            g.setStroke(new BasicStroke(i == selectedLane ? 3f : 1.5f));
            g.drawRoundRect(lane.bounds.x, lane.bounds.y, lane.bounds.width, lane.bounds.height, 14, 14);
            g.fillRoundRect(lane.header.x, lane.header.y, lane.header.width, lane.header.height, 8, 8);
            g.setColor(Color.WHITE);
            g.drawString(truncate(laneTitle(lane.annotation), 12), lane.header.x + 5, lane.header.y + 16);
        }
    }

    private void paintEdges(Graphics2D g, Rectangle clip) {
        List<FlowAnalysis.Edge> edges = selectedEdges;
        for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
            FlowAnalysis.Edge edge = edges.get(edgeIndex);
            int from = edge.from, to = edge.to;
            Node source = nodes.get(from), target = nodes.get(to);
            if (source == null || target == null) continue;
            Rectangle a = source.box, b = target.box;
            Rectangle bounds = a.union(b);
            bounds.grow(getPreferredSize().width, 20);
            if (clip != null && !clip.intersects(bounds)) continue;
            g.setColor(getForeground());
            boolean fallThrough = flow.nextAddress(from) == to && b.y == a.y + 90;
            int lane = a.x + a.width + 35 + edgeIndex * 20;
            int x = fallThrough ? a.x + a.width / 2 : a.x + a.width;
            int offset = Math.floorMod(edgeIndex, 9) * 5 - 20;
            int y = fallThrough ? a.y + a.height : a.y + (from == to ? a.height / 3 : a.height / 2 + offset);
            int tx = fallThrough ? b.x + b.width / 2 : b.x + b.width;
            int ty = fallThrough ? b.y : b.y + (from == to ? 2 * b.height / 3 : b.height / 2 + offset);
            if (fallThrough) g.drawLine(x, y, tx, ty);
            else {
                g.drawLine(x, y, lane, y);
                g.drawLine(lane, y, lane, ty);
                g.drawLine(lane, ty, tx, ty);
            }
            double angle = fallThrough ? Math.atan2(ty - y, tx - x) : Math.PI;
            for (double delta : new double[]{-.5, .5})
                g.drawLine(tx, ty, tx - (int) (10 * Math.cos(angle + delta)), ty - (int) (10 * Math.sin(angle + delta)));
        }
    }

    private void paintNodes(Graphics2D g, Rectangle clip) {
        Font nodeFont = EmuStudioGui.monospaced(g.getFont().deriveFont(Font.PLAIN));
        Font labelFont = nodeFont.deriveFont(Font.BOLD);
        Color selectionBackground = UIManager.getColor("List.selectionBackground");
        Color selectionForeground = UIManager.getColor("List.selectionForeground");
        Color surface = UIManager.getColor("Table.background");
        Color foreground = UIManager.getColor("Table.foreground");
        for (Node node : nodes.values()) {
            int address = node.address;
            Rectangle box = node.box;
            if (clip != null && !clip.intersects(box)) continue;
            boolean selected = address == selectedNode;
            boolean pc = paused() && address == session.execution().cpuContext().getInstructionLocation();
            g.setColor(selected && selectionBackground != null ? selectionBackground : surface != null ? surface : getBackground());
            g.fillRoundRect(box.x, box.y, box.width, box.height, 10, 10);
            g.setColor(selected && selectionForeground != null ? selectionForeground : foreground != null ? foreground : getForeground());
            float width = selected ? 2f : 1f;
            boolean marked = flow.isData(address) || flow.unknown().containsKey(address);
            g.setStroke(marked ? new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 4f}, 0f) : new BasicStroke(width));
            g.drawRoundRect(box.x, box.y, box.width, box.height, 10, 10);
            int addressY = node.label.isEmpty() ? 20 : 34;
            if (!node.label.isEmpty()) {
                g.setFont(labelFont);
                g.drawString(truncate(node.label, 28), box.x + 8, box.y + 16);
            }
            g.setFont(nodeFont);
            g.drawString(nodeAddress(node) + (pc ? " · PC" : "") + (flow.isData(address) ? " · DATA" : ""), box.x + 8, box.y + addressY);
            g.drawString(truncate(nodeMnemonic(node), 28), box.x + 8, box.y + addressY + 20);
        }
    }

    private static String truncate(String text, int length) {
        return text.length() > length ? text.substring(0, length) : text;
    }
}
