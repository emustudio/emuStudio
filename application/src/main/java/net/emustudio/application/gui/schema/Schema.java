/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.schema;

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.PluginConfig;
import net.emustudio.application.configuration.PluginConnection;
import net.emustudio.application.gui.P;
import net.emustudio.application.gui.schema.elements.*;
import net.emustudio.emulib.runtime.CannotUpdateSettingException;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Schema {
    public final static int MIN_LEFT_MARGIN = 5;
    public final static int MIN_TOP_MARGIN = 5;
    private final static int DEFAULT_GRID_GAP = 20;

    private CompilerElement compilerElement;
    private CpuElement cpuElement;
    private MemoryElement memoryElement;
    private final List<DeviceElement> deviceElements = new ArrayList<>();
    private final List<ConnectionLine> lines = new ArrayList<>();
    private final ComputerConfig config;
    private final ApplicationConfig applicationConfig;

    public Schema(ComputerConfig config, ApplicationConfig applicationConfig) throws NumberFormatException {
        this.config = Objects.requireNonNull(config);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);

        load();
    }

    public ComputerConfig getComputerConfig() {
        return config;
    }

    public boolean useSchemaGrid() {
        return applicationConfig.useSchemaGrid().orElse(true);
    }

    public void setUseSchemaGrid(boolean useSchemaGrid) {
        applicationConfig.setUseSchemaGrid(useSchemaGrid);
    }

    public int getSchemaGridGap() {
        return applicationConfig.getSchemaGridGap().orElse(DEFAULT_GRID_GAP);
    }

    public void setSchemaGridGap(int gridGap) {
        applicationConfig.setSchemaGridGap(gridGap);
    }

    public void setCompilerElement(Point clickPoint, String pluginFile) {
        CompilerElement element = createCompilerElement(clickPoint, pluginFile);
        if (this.compilerElement != null) {
            updateIncidentLines(this.compilerElement, element);
        }
        compilerElement = element;
    }

    public void setCpuElement(Point clickPoint, String pluginFile) {
        CpuElement element = createCpuElement(clickPoint, pluginFile);
        if (this.cpuElement != null) {
            updateIncidentLines(this.cpuElement, element);
        }
        this.cpuElement = element;
    }

    public void setMemoryElement(Point clickPoint, String pluginFile) {
        MemoryElement element = createMemoryElement(clickPoint, pluginFile);
        if (this.memoryElement != null) {
            updateIncidentLines(this.memoryElement, element);
        }
        this.memoryElement = element;
    }

    public void addDeviceElement(Point clickPoint, String pluginFile) {
        DeviceElement element = createDeviceElement(clickPoint, pluginFile);
        deviceElements.add(element);
    }

    public void removeElement(Element element) {
        removeIncidentLines(element);
        if (element instanceof CompilerElement) {
            cpuElement = null;
        } else if (element instanceof CpuElement) {
            cpuElement = null;
        } else if (element instanceof MemoryElement) {
            memoryElement = null;
        } else if (element instanceof DeviceElement) {
            deviceElements.remove(element);
        }
    }

    public List<Element> getAllElements() {
        List<Element> elements = new ArrayList<>();
        if (cpuElement != null) {
            elements.add(cpuElement);
        }
        if (memoryElement != null) {
            elements.add(memoryElement);
        }
        if (compilerElement != null) {
            elements.add(compilerElement);
        }
        elements.addAll(deviceElements);
        return elements;
    }

    public boolean isConnected(Element e1, Element e2) {
        return lines.stream().anyMatch(l -> l.containsElement(e1) && l.containsElement(e2));
    }

    public List<ConnectionLine> getConnectionLines() {
        return Collections.unmodifiableList(lines);
    }

    public void addConnectionLine(Element e1, Element e2, List<P> points, boolean bidirectional) {
        List<P> linePoints = points.stream().map(this::searchGridPoint).collect(Collectors.toList());
        lines.add(new ConnectionLine(e1, e2, linePoints, bidirectional));
    }

    public void removeConnectionLine(ConnectionLine line) {
        lines.remove(line);
    }

    public Element getCrossingElement(Point p) {
        if (p == null) {
            return null;
        }
        for (Element elem : getAllElements()) {
            int eX = elem.getX() - elem.getWidth() / 2;
            int eY = elem.getY() - elem.getHeight() / 2;

            if ((eX <= p.getX()) && (eX + elem.getWidth() >= p.x)
                && (eY <= p.getY()) && (eY + elem.getHeight() >= p.y)) {
                return elem;
            }
        }
        return null;
    }

    /**
     * Get an element where at least one its border is crossing a point.
     * <p>
     * It is used in the drawing panel.
     *
     * @param borderPoint Point that is checked for pointing at some element's border
     * @return an element if the given point points to its border; null otherwise
     */
    public Element getElementByBorderPoint(Point borderPoint) {
        List<Element> allElements = getAllElements();
        for (Element element : allElements) {
            if (element.crossesBottomBorder(borderPoint) || (element.crossesLeftBorder(borderPoint))
                || element.crossesRightBorder(borderPoint) || element.crossesTopBorder(borderPoint)) {
                return element;
            }
        }
        return null;
    }

    public ConnectionLine findCrossingLine(Point p) {
        for (ConnectionLine line : lines) {
            if (line.findCrossingPoint(p) != -1) {
                return line;
            }
        }
        return null;
    }

    /**
     * This method selects all elements and connection lines that crosses or lies inside the selection area.
     * <p>
     * If the area is empty (or null), nothing is done.
     *
     * @param x      X coordinate of the selection start
     * @param y      Y coordinate of the selection start
     * @param width  width of the selection
     * @param height height of the selection
     */
    public void select(int x, int y, int width, int height) {
        Point p1 = new Point(x, y);
        Point p2 = new Point(x + width, y + height);

        for (Element elem : getAllElements()) {
            if (elem.crossesArea(p1, p2)) {
                elem.setSelected(true);
            } else {
                elem.setSelected(false);
            }
        }
        lines.forEach(line -> line.setSelected(line.isAreaCrossing(p1, p2)));
    }

    public void selectAll() {
        getAllElements().forEach(elem -> elem.setSelected(true));
        lines.forEach(line -> line.setSelected(true));
    }

    public void deselectAll() {
        getAllElements().forEach(elem -> elem.setSelected(false));
        lines.forEach(line -> line.setSelected(false));
    }

    public void deleteSelected() {
        List<Element> allElements = getAllElements();

        for (int i = allElements.size() - 1; i >= 0; i--) {
            Element elem = allElements.get(i);
            if (elem.isSelected()) {
                removeElement(elem);
            }
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine line = lines.get(i);
            if (line.isSelected()) {
                removeConnectionLine(line);
            }
        }
    }

    /**
     * This method moves all selected elements to a new location. The new location is computed as: old + diff (the
     * parameter).
     *
     * @param diffX X difference between the new and old location
     * @param diffY Y difference between the new and old location
     */
    public void moveSelection(int diffX, int diffY) {
        List<Element> allElements = getAllElements();

        // test for movement of all elements and line points first
        for (Element elem : allElements) {
            P movedPoint = searchGridPoint(elem.getX() + diffX, elem.getY() + diffY);
            if (elem.isSelected() && isElementNotMovable(elem, movedPoint)) {
                return;
            }
        }
        if (lines.stream().anyMatch(line -> (line.isSelected() && !allPointMovable(line.getPoints(), diffX, diffY)))) {
            return;
        }

        // actual movement
        allElements.stream()
            .filter(Element::isSelected)
            .forEach(element -> {
                P movedPoint = searchGridPoint(element.getX() + diffX, element.getY() + diffY);
                element.move(movedPoint);
            });

        lines.stream()
            .filter(ConnectionLine::isSelected)
            .forEach(line -> line.moveAllPoints(diffX, diffY, this::searchGridPoint));
    }

    public void moveElement(Element element, Point newLocation) {
        P movedPoint = searchGridPoint(newLocation);
        if (!isElementNotMovable(element, newLocation.x, newLocation.y)) {
            element.move(movedPoint);
        }
    }

    public void moveLinePoint(ConnectionLine line, P p, Point newLocation) {
        if (!isPointNotMovable(newLocation.x, newLocation.y)) {
            P movedPoint = searchGridPoint(newLocation);
            line.movePoint(p, movedPoint);
        }
    }

    public void addLinePoint(ConnectionLine line, int beforePoint, Point point) {
        P addedPoint = searchGridPoint(point);
        line.addPoint(beforePoint, addedPoint);
    }

    private boolean isElementNotMovable(Element element, P newLocation) {
        return isElementNotMovable(element, newLocation.ix(), newLocation.iy());
    }

    private boolean isElementNotMovable(Element element, int newX, int newY) {
        int eW = element.getWidth() / 2;
        int eH = element.getHeight() / 2;

        Point elementStart = new Point(newX - eW, newY - eH);
        Point elementEnd = new Point(newX + eW, newY + eH);
        if (doesNotFitToMargins(elementStart.x, elementStart.y)) {
            return true;
        }
        if (findCrossingLine(elementStart, elementEnd) != null) {
            return true;
        }

        // Test all points of the line
        for (Element elem : getAllElements()) {
            if (elem == element) {
                continue;
            }
            if (elem.isSelected()) {
                continue;
            }
            int elemW = elem.getWidth() / 2;
            int elemH = elem.getHeight() / 2;

            // test left line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX() - elemW, elem.getY() - elemH),
                new Point(elem.getX() - elemW, elem.getY() + elemH), elementStart, elementEnd)) {
                return true;
            }

            // test right line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX() + elemW, elem.getY() - elemH),
                new Point(elem.getX() + elemW, elem.getY() + elemH), elementStart, elementEnd)) {
                return true;
            }

            // test top line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX() - elemW, elem.getY() - elemH),
                new Point(elem.getX() + elemW, elem.getY() - elemH), elementStart, elementEnd)) {
                return true;
            }

            // test bottom line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX() - elemW, elem.getY() + elemH),
                new Point(elem.getX() + elemW, elem.getY() + elemH), elementStart, elementEnd)) {
                return true;
            }
        }
        return false;
    }


    private boolean isPointNotMovable(P newLocation) {
        return isPointNotMovable(newLocation.ix(), newLocation.iy());
    }

    private boolean isPointNotMovable(int newX, int newY) {
        if (doesNotFitToMargins(newX, newY)) {
            return true;
        }
        // Test all points of the line
        for (Element elem : getAllElements()) {
            if (elem.isSelected()) {
                continue;
            }
            Point elementStart = new Point(elem.getX() - elem.getWidth() / 2, elem.getY() - elem.getHeight() / 2);
            Point elementEnd = new Point(elem.getX() + elem.getWidth() / 2, elem.getY() + elem.getHeight() / 2);
            if (ConnectionLine.isAreaCrossingPoint(elementStart, elementEnd, new Point(newX, newY))) {
                return true;
            }
        }
        return false;
    }

    private boolean allPointMovable(List<P> points, int diffX, int diffY) {
        for (P point : points) {
            P movedPoint = searchGridPoint(point.diff(point.ix() + diffX, point.iy() + diffY));
            if (isPointNotMovable(movedPoint)) {
                return false;
            }
        }
        return true;
    }

    public void save() throws CannotUpdateSettingException {
        List<PluginConnection> connections = lines.stream()
            .map(ConnectionLine::toPluginConnection)
            .collect(Collectors.toList());

        config.setConnections(connections);

        Optional.ofNullable(compilerElement).ifPresentOrElse(
            c -> config.setCompiler(c.save()), () -> config.setCompiler(null)
        );
        Optional.ofNullable(cpuElement).ifPresentOrElse(
            c -> config.setCPU(c.save()), () -> config.setCPU(null)
        );
        Optional.ofNullable(memoryElement).ifPresentOrElse(
            c -> config.setMemory(c.save()), () -> config.setMemory(null)
        );

        List<PluginConfig> devices = deviceElements.stream().map(Element::save).collect(Collectors.toList());
        config.setDevices(devices);

        config.save();
    }

    /**
     * Determine if point fits to margins.
     *
     * @param x new X location
     * @param y new Y location
     * @return true if the line point can be moved, false otherwise
     */
    private static boolean doesNotFitToMargins(int x, int y) {
        return (x < MIN_LEFT_MARGIN) || (y < MIN_TOP_MARGIN);
    }

    /**
     * Remove lines that is connected to specific element.
     *
     * @param e incident element
     */
    private void removeIncidentLines(Element e) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).containsElement(e)) {
                lines.remove(i);
            }
        }
    }

    /**
     * Update all lines connections.
     * <p>
     * This method will replace an element e1 that a line is connected to in at least one point to new element e2.
     *
     * @param e1 old element
     * @param e2 new element
     */
    private void updateIncidentLines(Element e1, Element e2) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            lines.get(i).replaceElement(e1, e2);
        }
    }

    private ConnectionLine findCrossingLine(Point selectionStart, Point selectionEnd) {
        for (ConnectionLine line : lines) {
            if (line.isAreaCrossingPoint(selectionStart, selectionEnd)) {
                return line;
            }
        }
        return null;
    }


    private void load() throws NumberFormatException, NullPointerException {
        compilerElement = null;
        cpuElement = null;
        memoryElement = null;
        deviceElements.clear();
        lines.clear();

        Map<String, Element> elements = new HashMap<>();

        config.getCompiler().ifPresent(c -> {
            compilerElement = new CompilerElement(c, this::searchGridPoint);
            compilerElement.move(searchGridPoint(compilerElement.getSchemaPoint()));
            elements.put(c.getPluginId(), compilerElement);
        });
        config.getCPU().ifPresent(c -> {
            cpuElement = new CpuElement(c, this::searchGridPoint);
            elements.put(c.getPluginId(), cpuElement);
        });
        config.getMemory().ifPresent(c -> {
            memoryElement = new MemoryElement(c, this::searchGridPoint);
            elements.put(c.getPluginId(), memoryElement);
        });
        config.getDevices().forEach(c -> {
            DeviceElement device = new DeviceElement(c, this::searchGridPoint);
            elements.put(c.getPluginId(), device);
            deviceElements.add(device);
        });

        config.getConnections().forEach(c -> {
            Element from = elements.get(c.getFromPluginId());
            Element to = elements.get(c.getToPluginId());

            List<P> points = c.getSchemaPoints().stream().map(P::of).collect(Collectors.toList());
            ConnectionLine line = new ConnectionLine(from, to, points, c.isBidirectional());
            lines.add(line);
        });
    }

    private P searchGridPoint(Point old) {
        return searchGridPoint(P.of(old));
    }

    private P searchGridPoint(int oldX, int oldY) {
        return searchGridPoint(P.of(oldX, oldY));
    }

    private P searchGridPoint(P old) {
        boolean useGrid = useSchemaGrid();
        int gridGap = getSchemaGridGap();
        if (!useGrid || gridGap <= 0) {
            return old;
        }
        int dX = (int) Math.round(old.x / (double) gridGap);
        int dY = (int) Math.round(old.y / (double) gridGap);
        return P.of(dX * gridGap, dY * gridGap);
    }

    private CompilerElement createCompilerElement(Point clickPoint, String pluginFile) {
        String pluginName = pluginFile.substring(0, pluginFile.length() - ".jar".length());
        return new CompilerElement(searchGridPoint(P.of(clickPoint)), pluginName, pluginFile);
    }

    private CpuElement createCpuElement(Point clickPoint, String pluginFile) {
        String pluginName = pluginFile.substring(0, pluginFile.length() - ".jar".length());
        return new CpuElement(searchGridPoint(P.of(clickPoint)), pluginName, pluginFile);
    }

    private MemoryElement createMemoryElement(Point clickPoint, String pluginFile) {
        String pluginName = pluginFile.substring(0, pluginFile.length() - ".jar".length());
        return new MemoryElement(searchGridPoint(P.of(clickPoint)), pluginName, pluginFile);
    }

    private DeviceElement createDeviceElement(Point clickPoint, String pluginFile) {
        String pluginName = pluginFile.substring(0, pluginFile.length() - ".jar".length());
        return new DeviceElement(searchGridPoint(P.of(clickPoint)), pluginName, pluginFile);
    }
}
