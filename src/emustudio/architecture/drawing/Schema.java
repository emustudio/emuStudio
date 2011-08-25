/*
 * Schema.java
 *
 * Created on 6.7.2008, 17:08:51
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package emustudio.architecture.drawing;

import java.awt.Point;
import java.util.ArrayList;

/**
 * This class represents abstract schema of virtual computer configuration.
 * It is created by the schema editor and used by ArchLoader.
 *
 * @author vbmacher
 */
public class Schema {
    private CompilerElement compilerElement;
    private CpuElement cpuElement;
    private MemoryElement memoryElement;
    private ArrayList<DeviceElement> deviceElements;
    private ArrayList<ConnectionLine> lines;

    /**
     * Name of the configuration
     */
    private String configName;

    /**
     * Creates schema by parameters.
     *
     * @param cpuElement
     * @param memoryElement
     * @param deviceElements
     * @param lines
     * @param configName
     * @param compilerElement
     */
    public Schema(CpuElement cpuElement, MemoryElement memoryElement, 
            ArrayList<DeviceElement> deviceElements, 
            ArrayList<ConnectionLine> lines, String configName, CompilerElement
            compilerElement) {
        this.cpuElement = cpuElement;
        this.memoryElement = memoryElement;
        this.deviceElements = new ArrayList<DeviceElement>();
        this.deviceElements.addAll(deviceElements);
        this.lines = new ArrayList<ConnectionLine>();
        this.lines.addAll(lines);
        this.configName = configName;
        this.compilerElement = compilerElement;
    }

    /**
     * Creates empty schema.
     */
    public Schema() {
        cpuElement = null;
        memoryElement = null;
        deviceElements = new ArrayList<DeviceElement>();
        lines = new ArrayList<ConnectionLine>();
        configName = "";
        compilerElement = null;
    }
    
    /**
     * Destroys the schema. It means - clear all arrays and
     * free memory holding by variables.
     */
    public void destroy() {
        cpuElement = null;
        compilerElement = null;
        memoryElement = null;
        deviceElements.clear();
        lines.clear();
        configName = "";
    }

    /**
     * Update all lines connections.
     * 
     * This method will replace an element e1 that a line is connected to in at
     * least one point to new element e2.
     * 
     * @param e1 old element
     * @param e2 new element
     */
    private void updateIncidentLines(Element e1, Element e2) {
        for (int i = lines.size()-1; i >= 0; i--)
            lines.get(i).replaceElement(e1, e2);
    }

    /**
     * Remove lines that is connected to specific element.
     *
     * @param e incident element
     */
    private void removeIncidentLines(Element e) {
        for (int i = lines.size()-1; i >= 0; i--)
            if (lines.get(i).containsElement(e))
                lines.remove(i);
    }

    /**
     * Get the virtual configuration name.
     *
     * @return virtual computer name
     */
    public String getConfigName() { return configName; }

    /**
     * Set the virtual configuration name
     *
     * @param cName new virtual computer name
     */
    public void setConfigName(String cName) { configName = cName; }

    /**
     * Set compiler element to this schema. If the origin compiler is not
     * null and it is connected to something, the connections are kept.
     *
     * @param compiler new compiler element.
     */
    public void setCompilerElement(CompilerElement compiler) {
        if ((compiler == null) && (this.compilerElement != null))
            removeIncidentLines(this.compilerElement);
        else if ((this.compilerElement != null) && (compiler != null))
            updateIncidentLines(this.compilerElement, compiler);
        compilerElement = compiler;
    }

    /**
     * Get compiler element.
     * @return Compiler element. Null if unset.
     */
    public CompilerElement getCompilerElement() {
        return compilerElement;
    }

    /**
     * Set cpu element to this schema. If the origin cpu is not
     * null and it is connected to something, the connections are kept.
     *
     * @param cpuElement new cpu element.
     */
    public void setCpuElement(CpuElement cpuElement) {
        if ((cpuElement == null) && (this.cpuElement != null))
            removeIncidentLines(this.cpuElement);
        else if ((this.cpuElement != null) && (cpuElement != null))
            updateIncidentLines(this.cpuElement, cpuElement);
        this.cpuElement = cpuElement;
    }
    
    /**
     * Get cpu element.
     * @return CPU element. Null if unset.
     */
    public CpuElement getCpuElement() {
        return cpuElement;
    }
    
    /**
     * Set memory element to this schema. If the origin memory is not
     * null and it is connected to something, the connections are kept.
     *
     * @param memoryElement new memory element.
     */
    public void setMemoryElement(MemoryElement memoryElement) {
        if ((memoryElement == null) && (this.memoryElement != null))
            removeIncidentLines(this.memoryElement);
        else if ((this.memoryElement != null) && (memoryElement != null))
            updateIncidentLines(this.memoryElement, memoryElement);
        this.memoryElement = memoryElement;
    }

    /**
     * Get memory element.
     * @return Memory element. Null if unset.
     */
    public MemoryElement getMemoryElement() {
        return memoryElement;
    }

    /**
     * Add device element. The method does nothing if the deviceElement
     * is null.
     *
     * @param deviceElement the device element.
     */
    public void addDeviceElement(DeviceElement deviceElement) {
        if (deviceElement == null)
            return;
        deviceElements.add(deviceElement);
    }

    /**
     * Method gets the list of all device elements.
     *
     * @return ArrayList object containing all devices
     */
    public ArrayList<DeviceElement>getDeviceElements() {
        return deviceElements;
    }

    /**
     * Removes specified device element. If the device is not included in the
     * schema, nothing is done.
     *
     * @param device the device element to remove
     */
    public void removeDeviceElement(DeviceElement device) {
        removeIncidentLines(device);
        deviceElements.remove(device);
    }

    /**
     * Removes an element from this schema.
     *
     * @param elem element to remove
     */
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
    /**
     * This method gets the list of all elements within this schema.
     * CPU, Memory, Compiler and devices are joined into a single ArrayList
     * object and returned.
     *
     * @return ArrayList object containing all elements within this schema
     */
    public ArrayList<Element>getAllElements() {
        ArrayList<Element> a = new ArrayList<Element>();
        if (cpuElement != null)
            a.add(cpuElement);
        if (memoryElement != null)
            a.add(memoryElement);
        if (compilerElement != null)
            a.add(compilerElement);
        a.addAll(deviceElements);
        return a;
    }

    /**
     * Gets all connection lines that exist within this schema.
     *
     * @return ArrayList object of all connection lines
     */
    public ArrayList<ConnectionLine>getConnectionLines() {
        return lines;
    }

    /**
     * Method adds new connection line to this schema. If it is null,
     * nothing is done.
     *
     * @param conLine connection line to add
     */
    public void addConnectionLine(ConnectionLine conLine) {
        if (conLine == null)
            return;
        lines.add(conLine);
    }

    /**
     * Removes specified connection line. If the index is out of the boundaries,
     * nothing is done.
     *
     * @param index index to an array of connection lines
     */
    public void removeConnectionLine(int index) {
        if ((index < 0) || (index >= lines.size()))
            return;
        lines.remove(index);
    }

    /**
     * Removes specified connection line. If the line is not included in the
     * schema, nothing is done.
     *
     * @param line the connection line to remove
     */
    public void removeConnectionLine(ConnectionLine line) {
        lines.remove(line);
    }

    /**
     * Gets an element that is located under the given point. It is used
     * in the drawing panel.
     * 
     * @param p Point that all elements locations are compared to
     * @return crossing element, or null if it was not found
     */
    public Element getCrossingElement(Point p) {
        ArrayList<Element> a = getAllElements();
        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
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
     * Get a connection line that crosses given point.
     *
     * @param p Point that the crossing is checked
     * @return connection line object if the point is crossing this line,
     * null otherwise
     */
    public ConnectionLine getCrossingLine(Point p) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine l = lines.get(i);
            if (l.getCrossPointAfter(p,5.0) != -1)
                return l;
        }
        return null;
    }

    /**
     * This method selects all elements and connection lines that
     * crosses or lies inside the selection area.
     *
     * If the area is empty (or null), nothing is done.
     *
     * @param x X coordinate of the selection start
     * @param y Y coordinate of the selection start
     * @param width width of the selection
     * @param height height of the selection
     */
    public void selectElements(int x, int y, int width, int height) {
        ArrayList<Element> a = getAllElements();

        Point p1 = new Point(x,y);
        Point p2 = new Point(x+width, y+height);

        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);

            if (elem.isAreaCrossing(p1, p2))
                elem.setSelected(true);
            else
                elem.setSelected(false);
        }

        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine l = lines.get(i);
            l.setSelected(l.isAreaCrossing(p1, p2));
        }
    }

    /**
     * This method moves all selected elements to a new location. The new
     * location is computed as: old + diff (the parameter).
     * 
     * BUGGY.
     *
     * @param diffX X difference between the new and old location
     * @param diffY Y difference between the new and old location
     */
    public void moveSelected(int diffX, int diffY) {
        ArrayList<Element> a = getAllElements();

        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
            if (elem.isSelected()) {
                elem.move(elem.getX() + diffX,
                        elem.getY() + diffY);
                System.out.println(elem.getPluginType() + ": " + (elem.getX() + diffX)
                        + ", " + (elem.getY() + diffY));
            }
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine l = lines.get(i);
            if (l.isSelected())
                l.pointMoveAll(diffX, diffY);
        }
    }

    /**
     * Deletes all selected elements.
     */
    public void deleteSelected() {
        ArrayList<Element> a = getAllElements();

        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
            if (elem.isSelected())
                removeElement(elem);
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine l = lines.get(i);
            if (l.isSelected())
                removeConnectionLine(l);
        }
    }

}
