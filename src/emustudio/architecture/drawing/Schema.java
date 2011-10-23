/*
 * Schema.java
 *
 * Created on 6.7.2008, 17:08:51
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2011 Peter Jakubčo <pjakubco@gmail.com>
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * This class represents abstract schema of virtual computer configuration.
 * It is created by the schema editor and used by ArchLoader. It is a graphics
 * model of the virtual computer.
 *
 * @author vbmacher
 */
public class Schema {

    /**
     * Minimum left margin for all elements and line points within the schema
     */
    public final static int MIN_LEFT_MARGIN = 5;
    /**
     * Minimum top margin for all elements and line points within the schema
     */
    public final static int MIN_TOP_MARGIN = 5;
    private CompilerElement compilerElement;
    private CpuElement cpuElement;
    private MemoryElement memoryElement;
    private ArrayList<DeviceElement> deviceElements;
    private ArrayList<ConnectionLine> lines;
    private Properties settings;
    /**
     * Whether to use and draw grid
     */
    private boolean useGrid;
    /**
     * Gap between vertical and horizontal grid lines
     */
    private int gridGap;
    /**
     * Name of the configuration
     */
    private String configName;

    /**
     * Creates new instance of Schema by parameters.
     *
     * @param configName name of the configuration file
     * @param settings configuration file (all settings for all plug-ins)
     */
    public Schema(String configName, Properties settings) throws NumberFormatException {
        this.configName = configName;
        this.settings = settings;
        load();
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
        this.useGrid = true;
        this.gridGap = DrawingPanel.DEFAULT_GRID_GAP_INT;
        this.settings = new Properties();
    }

    /**
     * Method loads schema from configuration file (settings).
     */
    private void load() throws NumberFormatException {
        this.deviceElements = new ArrayList<DeviceElement>();
        this.lines = new ArrayList<ConnectionLine>();

        // grid
        useGrid = Boolean.parseBoolean(settings.getProperty("useGrid", "false"));
        gridGap = Integer.parseInt(settings.getProperty("gridGap",
                DrawingPanel.DEFAULT_GRID_GAP));

        try {
            compilerElement = new CompilerElement(settings);
        } catch (Exception e) {}
        try {
            // if cpu is null here, it does not matter. Maybe user just did not
            // finish the schema..
            cpuElement = new CpuElement(settings);
        } catch (Exception e) {}
        try {
            memoryElement = new MemoryElement(settings);
        } catch (Exception e) {}
        // load devices
        for (int i = 0; settings.containsKey("device" + i); i++) {
            try {
                deviceElements.add(new DeviceElement(settings, "device" + i));
            } catch (Exception e) {}
        }

        // load line connections
        for (int i = 0; settings.containsKey("connection" + i + ".junc0"); i++) {
            String j0 = settings.getProperty("connection" + i + ".junc0", "");
            String j1 = settings.getProperty("connection" + i + ".junc1", "");
            boolean bidi = Boolean.parseBoolean(settings.getProperty("connection"
                    + i + ".bidirectional", "true"));
            if (j0.equals("") || j1.equals("")) {
                continue;
            }

            Element e1 = null, e2 = null;
            if (j0.equals("cpu")) {
                e1 = cpuElement;
            } else if (j0.equals("memory")) {
                e1 = memoryElement;
            } else if (j0.equals("compiler")) {
                e1 = compilerElement;
            } else if (j0.startsWith("device")) {
                int index = Integer.parseInt(j0.substring(6));
                e1 = deviceElements.get(index);
            }
            if (j1.equals("cpu")) {
                e2 = cpuElement;
            } else if (j1.equals("memory")) {
                e2 = memoryElement;
            } else if (j1.equals("compiler")) {
                e2 = compilerElement;
            } else if (j1.startsWith("device")) {
                int index = Integer.parseInt(j1.substring(6));
                e2 = deviceElements.get(index);
            }
            int x,y;
            if ((e1 != null) && (e2 != null)) {
                ConnectionLine lin = new ConnectionLine(e1, e2, null);
                lin.setBidirectional(bidi);
                for (int j = 0; settings.containsKey("connection" + i + ".point" + j + ".x"); j++) {
                    x = Integer.parseInt(settings.getProperty("connection"
                            + i + ".point" + j + ".x", "0"));
                    y = Integer.parseInt(settings.getProperty("connection"
                            + i + ".point" + j + ".y", "0"));
                    lin.addPoint(new Point(x, y));
                }
                lines.add(lin);
            }
        }
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
        for (int i = lines.size() - 1; i >= 0; i--) {
            lines.get(i).replaceElement(e1, e2);
        }
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
     * Get the virtual configuration name.
     *
     * @return virtual computer name
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * Set the virtual configuration name
     *
     * @param cName new virtual computer name
     */
    public void setConfigName(String cName) {
        configName = cName;
    }

    /**
     * Set compiler element to this schema. If the origin compiler is not
     * null and it is connected to something, the connections are kept.
     *
     * @param compiler new compiler element.
     */
    public void setCompilerElement(CompilerElement compiler) {
        if ((compiler == null) && (this.compilerElement != null)) {
            removeIncidentLines(this.compilerElement);
        } else if ((this.compilerElement != null) && (compiler != null)) {
            updateIncidentLines(this.compilerElement, compiler);
        }
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
        if ((cpuElement == null) && (this.cpuElement != null)) {
            removeIncidentLines(this.cpuElement);
        } else if ((this.cpuElement != null) && (cpuElement != null)) {
            updateIncidentLines(this.cpuElement, cpuElement);
        }
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
        if ((memoryElement == null) && (this.memoryElement != null)) {
            removeIncidentLines(this.memoryElement);
        } else if ((this.memoryElement != null) && (memoryElement != null)) {
            updateIncidentLines(this.memoryElement, memoryElement);
        }
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
        if (deviceElement == null) {
            return;
        }
        deviceElements.add(deviceElement);
    }

    /**
     * Method gets the list of all device elements.
     *
     * @return ArrayList object containing all devices
     */
    public ArrayList<DeviceElement> getDeviceElements() {
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
    public ArrayList<Element> getAllElements() {
        ArrayList<Element> a = new ArrayList<Element>();
        if (cpuElement != null) {
            a.add(cpuElement);
        }
        if (memoryElement != null) {
            a.add(memoryElement);
        }
        if (compilerElement != null) {
            a.add(compilerElement);
        }
        a.addAll(deviceElements);
        return a;
    }

    /**
     * Gets all connection lines that exist within this schema.
     *
     * @return ArrayList object of all connection lines
     */
    public ArrayList<ConnectionLine> getConnectionLines() {
        return lines;
    }

    /**
     * Method adds new connection line to this schema. If it is null,
     * nothing is done.
     *
     * @param conLine connection line to add
     */
    public void addConnectionLine(ConnectionLine conLine) {
        if (conLine == null) {
            return;
        }
        lines.add(conLine);
    }

    /**
     * Removes specified connection line. If the index is out of the boundaries,
     * nothing is done.
     *
     * @param index index to an array of connection lines
     */
    public void removeConnectionLine(int index) {
        if ((index < 0) || (index >= lines.size())) {
            return;
        }
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
     * Detects an element that wants to be resized. It searches for an element
     * where the given point is pointing to its border. It is used
     * in the drawing panel.
     * 
     * @param p Point that all elements locations are compared to
     * @return resize element, or null if it was not found
     */
    public Element getResizeElement(Point p) {
        ArrayList<Element> a = getAllElements();
        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
            if (elem.isBottomCrossing(p) || (elem.isLeftCrossing(p))
                    || elem.isRightCrossing(p) || elem.isTopCrossing(p)) {
                return elem;
            }
        }
        return null;
    }

    /**
     * Return whether use grid in the schema
     * 
     * @return true if the schema uses grid, false otherwise
     */
    public boolean getUseGrid() {
        return useGrid;
    }

    /**
     * Return grid cells gap (i.e. cell size)
     * 
     * @return grid cells gap
     */
    public int getGridGap() {
        return gridGap;
    }

    /**
     * Set whether use grid in the schema
     * 
     * @param useGrid true if the schema uses grid, false otherwise
     */
    public void setUseGrid(boolean useGrid) {
        this.useGrid = useGrid;
    }

    /**
     * Set grid cells gap (i.e. cell size)
     * 
     * @param gridGap grid cells gap
     */
    public void setGridGap(int gridGap) {
        this.gridGap = gridGap;
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
            if (l.getCrossPointAfter(p, 5.0) != -1) {
                return l;
            }
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

        Point p1 = new Point(x, y);
        Point p2 = new Point(x + width, y + height);

        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);

            if (elem.isAreaCrossing(p1, p2)) {
                elem.setSelected(true);
            } else {
                elem.setSelected(false);
            }
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
     * @param diffX X difference between the new and old location
     * @param diffY Y difference between the new and old location
     */
    public void moveSelected(int diffX, int diffY) {
        ArrayList<Element> a = getAllElements();

        // first determine if all elements and lines can be moved
        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
            int x = elem.getX() + diffX;
            int y = elem.getY() + diffY;
            if (elem.isSelected()) {
                if (!canMove(x, y)) {
                    return;
                }
            }
        }
        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
            int x = elem.getX() + diffX;
            int y = elem.getY() + diffY;
            if (elem.isSelected()) {
                elem.move(x, y);
            }
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine l = lines.get(i);
            if (l.isSelected()) {
                l.pointMoveAll(diffX, diffY);
            }
        }
    }

    /**
     * Deletes all selected elements.
     */
    public void deleteSelected() {
        ArrayList<Element> a = getAllElements();

        for (int i = a.size() - 1; i >= 0; i--) {
            Element elem = a.get(i);
            if (elem.isSelected()) {
                removeElement(elem);
            }
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            ConnectionLine l = lines.get(i);
            if (l.isSelected()) {
                removeConnectionLine(l);
            }
        }
    }

    /**
     * Determine if given location for the point is valid.
     * 
     * @param x new X location
     * @param y new Y location
     * @return true if the line point can be moved, false otherwise
     */
    public static boolean canMove(int x, int y) {
        return (x >= MIN_LEFT_MARGIN)
                && (y >= MIN_TOP_MARGIN);
    }

    /**
     * This method selects all settings that belong to specific plug-in.
     * The plug-in name is specified in format as in configuration file (eg.
     * CPU plug-in name is 'cpu', compiler plug-in is called 'compiler',
     * memory plug-in is 'memory', devices are 'device0', 'device1', etc.).
     * 
     * @param key name of the plug-in
     * @return Properties selection of settings for that plug-ing
     */
    public Properties selectSettings(String key) {
        Enumeration e = settings.keys();
        Properties selProps = new Properties();
        while (e.hasMoreElements()) {
            String akey = (String)e.nextElement();
            if (akey.startsWith(key))
                selProps.put(akey, settings.getProperty(akey));
        }
        return selProps;
    }
    
    /**
     * Method incorporates settings in parameter into this virtual computer
     * settings.
     * 
     * @param props settings to incorporate
     */
    public void addSettings(Properties props) {
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String akey = (String)e.nextElement();
            settings.put(akey, props.getProperty(akey));
        }
    }

    /**
     * Get all settings of the virtual computer.
     * @return all plug-ins' settings
     */
    public Properties getSettings() {
        return settings;
    }
    
    /**
     * This method saves this schema into configuration (settings).
     */
    public void save() {
        settings.clear(); // needed when removing elements
        settings.put("useGrid", String.valueOf(useGrid));
        settings.put("gridGap", String.valueOf(gridGap));
        // compiler
        if (compilerElement != null) {
            compilerElement.saveSettings(settings);
        }
        // cpu
        if (cpuElement != null) {
            cpuElement.saveSettings(settings);
        }
        // memory
        if (memoryElement != null) {
            memoryElement.saveSettings(settings);
        }
        // devices
        HashMap<DeviceElement, Object> devsHash = new HashMap<DeviceElement, Object>();
        for (int i = 0; i < deviceElements.size(); i++) {
            DeviceElement dev = deviceElements.get(i);
            devsHash.put(dev, "device" + i);
            dev.saveSettings(settings,"device" + i);
        }
        for (int i = 0; i < lines.size(); i++) {
            ConnectionLine line = lines.get(i);

            settings.put("connection" + i + ".bidirectional",
                    String.valueOf(line.isBidirectional()));

            Element e = line.getJunc0();
            if (e instanceof CompilerElement) {
                settings.put("connection" + i + ".junc0", "compiler");
            } else if (e instanceof CpuElement) {
                settings.put("connection" + i + ".junc0", "cpu");
            } else if (e instanceof MemoryElement) {
                settings.put("connection" + i + ".junc0", "memory");
            } else if (e instanceof DeviceElement) {
                settings.put("connection" + i + ".junc0", devsHash.get((DeviceElement) e));
            }

            e = line.getJunc1();
            if (e instanceof CompilerElement) {
                settings.put("connection" + i + ".junc1", "compiler");
            } else if (e instanceof CpuElement) {
                settings.put("connection" + i + ".junc1", "cpu");
            } else if (e instanceof MemoryElement) {
                settings.put("connection" + i + ".junc1", "memory");
            } else if (e instanceof DeviceElement) {
                settings.put("connection" + i + ".junc1", devsHash.get((DeviceElement) e));
            }
            ArrayList<Point> points = line.getPoints();
            for (int j = 0; j < points.size(); j++) {
                Point po = points.get(j);
                settings.put("connection" + i + ".point" + j + ".x", String.valueOf((int) po.getX()));
                settings.put("connection" + i + ".point" + j + ".y", String.valueOf((int) po.getY()));
            }
        }
    }
}
