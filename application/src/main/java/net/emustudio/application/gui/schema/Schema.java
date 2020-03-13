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

import static net.emustudio.application.gui.P.SELECTION_TOLERANCE;

/**
 * This class represents abstract schema of virtual computer configuration. It is created by the schema editor and used
 * by ArchLoader. It is a graphics model of the virtual computer.
 */
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

    public void setCompilerElement(CompilerElement compiler) {
        if ((compiler == null) && (this.compilerElement != null)) {
            removeIncidentLines(this.compilerElement);
        } else if (this.compilerElement != null) {
            updateIncidentLines(this.compilerElement, compiler);
        }
        compilerElement = compiler;
    }

    public void setCpuElement(CpuElement cpuElement) {
        if ((cpuElement == null) && (this.cpuElement != null)) {
            removeIncidentLines(this.cpuElement);
        } else if (this.cpuElement != null) {
            updateIncidentLines(this.cpuElement, cpuElement);
        }
        this.cpuElement = cpuElement;
    }

    public void setMemoryElement(MemoryElement memoryElement) {
        if ((memoryElement == null) && (this.memoryElement != null)) {
            removeIncidentLines(this.memoryElement);
        } else if (this.memoryElement != null) {
            updateIncidentLines(this.memoryElement, memoryElement);
        }
        this.memoryElement = memoryElement;
    }

    public void addDeviceElement(DeviceElement deviceElement) {
        if (deviceElement == null) {
            return;
        }
        deviceElements.add(deviceElement);
    }

    public void removeDeviceElement(DeviceElement device) {
        removeIncidentLines(device);
        deviceElements.remove(device);
    }

    public void removeElement(Element elem) {
        if (elem instanceof CompilerElement) {
            setCompilerElement(null);
        } else if (elem instanceof CpuElement) {
            setCpuElement(null);
        } else if (elem instanceof MemoryElement) {
            setMemoryElement(null);
        } else if (elem instanceof DeviceElement) {
            removeDeviceElement((DeviceElement) elem);
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

    public List<ConnectionLine> getConnectionLines() {
        return Collections.unmodifiableList(lines);
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

    public ConnectionLine getCrossingLine(Point p) {
        for (ConnectionLine line : lines) {
            if (line.getCrossPoint(p, SELECTION_TOLERANCE) != -1) {
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
    public void selectElements(int x, int y, int width, int height) {
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

    /**
     * This method moves all selected elements to a new location. The new location is computed as: old + diff (the
     * parameter).
     *
     * @param diffX X difference between the new and old location
     * @param diffY Y difference between the new and old location
     */
    public void moveSelection(int diffX, int diffY) {
        List<Element> allElements = getAllElements();

        // TODO: test only not selected element

        // test for movement of all elements and line points first
        for (Element elem : allElements) {
            int x = elem.getX() + diffX;
            int y = elem.getY() + diffY;
            if (elem.isSelected() && isElementNotMovable(x, y, elem)) {
                return;
            }
        }
        if (lines.stream().anyMatch(line -> (line.isSelected() && line.anyPointNotMovable(this, diffX, diffY)))) {
            return;
        }

        // actual movement
        for (Element elem : allElements) {
            if (elem.isSelected()) {
                elem.move(this, diffX, diffY);
            }
        }
        lines.stream().filter(ConnectionLine::isSelected).forEach(line -> line.moveAllPoints(this, diffX, diffY));
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

    public boolean isElementNotMovable(int newX, int newY, Element element) {
        int eW = element.getWidth() / 2;
        int eH = element.getHeight() / 2;

        Point elementStart = new Point(newX - eW, newY - eH);
        Point elementEnd = new Point(newX + eW, newY + eH);
        if (doesNotFitToMargins(elementStart.x, elementStart.y)) {
            return true;
        }
        if (getCrossingLine(elementStart, elementEnd) != null) {
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

    /**
     * Determines if a point can be moved to given location.
     *
     * @param newX new X location for the point
     * @param newY new Y location for the point
     * @return true if the point can be moved, false otherwise (out of margins, or some element is in the way).
     */
    public boolean isPointNotMovable(int newX, int newY) {
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

    public void save() throws CannotUpdateSettingException {
        List<PluginConnection> connections = lines.stream()
            .map(ConnectionLine::toPluginConnection)
            .collect(Collectors.toList());

        config.replaceConnections(connections);

        Optional.ofNullable(compilerElement).ifPresentOrElse(
            c -> config.setCompiler(c.pluginConfig), () -> config.setCompiler(null)
        );
        Optional.ofNullable(cpuElement).ifPresentOrElse(
            c -> config.setCPU(c.pluginConfig), () -> config.setCPU(null)
        );
        Optional.ofNullable(memoryElement).ifPresentOrElse(
            c -> config.setMemory(c.pluginConfig), () -> config.setMemory(null)
        );

        List<PluginConfig> devices = deviceElements.stream().map(d -> d.pluginConfig).collect(Collectors.toList());
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

    private ConnectionLine getCrossingLine(Point selectionStart, Point selectionEnd) {
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
            compilerElement = new CompilerElement(c);
            elements.put(c.getPluginId(), compilerElement);
        });
        config.getCPU().ifPresent(c -> {
            cpuElement = new CpuElement(c);
            elements.put(c.getPluginId(), cpuElement);
        });
        config.getMemory().ifPresent(c -> {
            memoryElement = new MemoryElement(c);
            elements.put(c.getPluginId(), memoryElement);
        });
        config.getDevices().forEach(c -> {
            DeviceElement device = new DeviceElement(c);
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
}
