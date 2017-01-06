/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package emustudio.drawing;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class represents abstract schema of virtual computer configuration. It is created by the schema editor and used
 * by ArchLoader. It is a graphics model of the virtual computer.
 *
 */
public class Schema {
    final static int MIN_LEFT_MARGIN = 5;
    final static int MIN_TOP_MARGIN = 5;

    private CompilerElement compilerElement;
    private CpuElement cpuElement;
    private MemoryElement memoryElement;
    private List<DeviceElement> deviceElements;
    private List<ConnectionLine> lines;
    private final Properties settings;

    private boolean useGrid;
    private int gridGap;
    private String configName;

    public Schema(String configName, Properties settings) throws NumberFormatException {
        this.configName = configName;
        this.settings = settings;
        loadFromSettings();
    }

    public Schema() {
        cpuElement = null;
        memoryElement = null;
        deviceElements = new ArrayList<>();
        lines = new ArrayList<>();
        configName = "";
        compilerElement = null;
        this.useGrid = true;
        this.gridGap = DrawingPanel.DEFAULT_GRID_GAP;
        this.settings = new Properties();
    }

    private void loadFromSettings() throws NumberFormatException, NullPointerException {
        this.deviceElements = new ArrayList<>();
        this.lines = new ArrayList<>();

        // grid
        useGrid = Boolean.parseBoolean(settings.getProperty("useGrid", "false"));
        gridGap = Integer.parseInt(settings.getProperty("gridGap",
                DrawingPanel.DEFAULT_GRID_GAP.toString()));

        compilerElement = new CompilerElement(settings.getProperty("compiler"), selectSettings("compiler"), this);
        // if cpu is null here, it does not matter. Maybe user just did not
        // finish the schema..
        cpuElement = new CpuElement(settings.getProperty("cpu"), selectSettings("cpu"), this);
        memoryElement = new MemoryElement(settings.getProperty("memory"), selectSettings("memory"), this);
        // load devices
        for (int i = 0; settings.containsKey("device" + i); i++) {
            deviceElements.add(new DeviceElement(settings.getProperty("device" + i), selectSettings("device" + i), this));
        }

        // load line connections
        for (int i = 0; settings.containsKey("connection" + i + ".junc0"); i++) {
            String j0 = settings.getProperty("connection" + i + ".junc0", "");
            String j1 = settings.getProperty("connection" + i + ".junc1", "");
            boolean bidi = Boolean.parseBoolean(settings.getProperty("connection" + i + ".bidirectional", "true"));
            if (j0.equals("") || j1.equals("")) {
                continue;
            }

            Element e1 = getElementFromEndpoint(j0);
            Element e2 = getElementFromEndpoint(j1);
            int x, y;
            if ((e1 != null) && (e2 != null)) {
                ConnectionLine lin = new ConnectionLine(e1, e2, null, this);
                lin.setBidirectional(bidi);
                for (int j = 0; settings.containsKey("connection" + i + ".point" + j + ".x"); j++) {
                    x = Integer.parseInt(settings.getProperty("connection" + i + ".point" + j + ".x", "0"));
                    y = Integer.parseInt(settings.getProperty("connection" + i + ".point" + j + ".y", "0"));
                    lin.addPoint(new Point(x, y));
                }
                lines.add(lin);
            }
        }
    }

    private Element getElementFromEndpoint(String endpoint) {
        if (endpoint.equals("cpu")) {
            return cpuElement;
        } else if (endpoint.equals("memory")) {
            return memoryElement;
        } else if (endpoint.equals("compiler")) {
            return compilerElement;
        } else if (endpoint.startsWith("device")) {
            int index = Integer.parseInt(endpoint.substring(6));
            return deviceElements.get(index);
        }
        return null;
    }

    void destroy() {
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

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String cName) {
        configName = cName;
    }

    public void setCompilerElement(CompilerElement compiler) {
        if ((compiler == null) && (this.compilerElement != null)) {
            removeIncidentLines(this.compilerElement);
        } else if (this.compilerElement != null) {
            updateIncidentLines(this.compilerElement, compiler);
        }
        compilerElement = compiler;
    }

    public CompilerElement getCompilerElement() {
        return compilerElement;
    }

    public void setCpuElement(CpuElement cpuElement) {
        if ((cpuElement == null) && (this.cpuElement != null)) {
            removeIncidentLines(this.cpuElement);
        } else if (this.cpuElement != null) {
            updateIncidentLines(this.cpuElement, cpuElement);
        }
        this.cpuElement = cpuElement;
    }

    public CpuElement getCpuElement() {
        return cpuElement;
    }

    public void setMemoryElement(MemoryElement memoryElement) {
        if ((memoryElement == null) && (this.memoryElement != null)) {
            removeIncidentLines(this.memoryElement);
        } else if (this.memoryElement != null) {
            updateIncidentLines(this.memoryElement, memoryElement);
        }
        this.memoryElement = memoryElement;
    }

    public MemoryElement getMemoryElement() {
        return memoryElement;
    }

    public void addDeviceElement(DeviceElement deviceElement) {
        if (deviceElement == null) {
            return;
        }
        deviceElements.add(deviceElement);
    }

    public List<DeviceElement> getDeviceElements() {
        return deviceElements;
    }

    void removeDeviceElement(DeviceElement device) {
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

    List<Element> getAllElements() {
        List<Element> a = new ArrayList<>();
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

    public List<ConnectionLine> getConnectionLines() {
        return lines;
    }

    void addConnectionLine(ConnectionLine conLine) {
        if (conLine == null) {
            return;
        }
        lines.add(conLine);
    }

    void removeConnectionLine(int index) {
        if ((index < 0) || (index >= lines.size())) {
            return;
        }
        lines.remove(index);
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
     *
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

    public boolean isGridUsed() {
        return useGrid;
    }

    public int getGridGap() {
        return gridGap;
    }

    public void setUsingGrid(boolean useGrid) {
        this.useGrid = useGrid;
    }

    public void setGridGap(int gridGap) {
        this.gridGap = gridGap;
    }

    public ConnectionLine getCrossingLine(Point p) {
        for (ConnectionLine line : lines) {
            if (line.getCrossPoint(p, ConnectionLine.TOLERANCE) != -1) {
                return line;
            }
        }
        return null;
    }

    private ConnectionLine getCrossingLine(Point selectionStart, Point selectionEnd) {
        for (ConnectionLine line : lines) {
            if (line.isAreaCrossingPoint(selectionStart, selectionEnd)) {
                return line;
            }
        }
        return null;
    }

    /**
     * This method selects all elements and connection lines that crosses or lies inside the selection area.
     *
     * If the area is empty (or null), nothing is done.
     *
     * @param x X coordinate of the selection start
     * @param y Y coordinate of the selection start
     * @param width width of the selection
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
        lines.stream().forEach(line -> line.setSelected(line.isAreaCrossing(p1, p2)));
    }

    void selectAll() {
        getAllElements().stream().forEach(elem -> elem.setSelected(true));
        lines.stream().forEach(line -> line.setSelected(true));
    }

    void deselectAll() {
        getAllElements().stream().forEach(elem -> elem.setSelected(false));
        lines.stream().forEach(line -> line.setSelected(false));
    }

    /**
     * This method moves all selected elements to a new location. The new location is computed as: old + diff (the
     * parameter).
     *
     * @param diffX X difference between the new and old location
     * @param diffY Y difference between the new and old location
     * @return true if the selection was moved; false otherwise (either due margin violations or other elements conflict)
     */
    public boolean moveSelection(int diffX, int diffY) {
        List<Element> allElements = getAllElements();

        // TODO: test only not selected element

        // test for movement of all elements and line points first
        for (Element elem : allElements) {
            int x = elem.getX() + diffX;
            int y = elem.getY() + diffY;
            if (elem.isSelected() && !canMoveElement(x, y, elem)) {
                return false;
            }
        }
        if (!lines.stream().noneMatch(line -> (line.isSelected() && !line.canMoveAllPoints(diffX, diffY)))) {
            return false;
        }

        // actual movement
        for (Element elem : allElements) {
            if (elem.isSelected()) {
                elem.move(diffX, diffY);
            }
        }
        lines.stream().filter(ConnectionLine::isSelected).forEach(line -> line.moveAllPoints(diffX, diffY));
        return true;
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
     * Determine if point fits to margins.
     *
     * @param x new X location
     * @param y new Y location
     * @return true if the line point can be moved, false otherwise
     */
    private static boolean fitToMargins(int x, int y) {
        return (x >= MIN_LEFT_MARGIN) && (y >= MIN_TOP_MARGIN);
    }

    boolean canMoveElement(int newX, int newY, Element element) {
        int eW = element.getWidth()/2;
        int eH = element.getHeight()/2;

        Point elementStart = new Point(newX - eW, newY - eH);
        Point elementEnd = new Point(newX + eW, newY + eH);
        if (!fitToMargins(elementStart.x, elementStart.y)) {
            return false;
        }
        if (getCrossingLine(elementStart, elementEnd) != null) {
            return false;
        }

        // Test all points of the line
        for (Element elem : getAllElements()) {
            if (elem == element) {
                continue;
            }
            if (elem.isSelected()) {
                continue;
            }
            int elemW = elem.getWidth()/2;
            int elemH = elem.getHeight()/2;

            // test left line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX()-elemW, elem.getY()-elemH),
                    new Point(elem.getX()-elemW, elem.getY()+elemH), elementStart, elementEnd)) {
                return false;
            }

            // test right line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX()+elemW, elem.getY()-elemH),
                    new Point(elem.getX()+elemW, elem.getY()+elemH), elementStart, elementEnd)) {
                return false;
            }

            // test top line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX()-elemW, elem.getY()-elemH),
                    new Point(elem.getX()+elemW, elem.getY()-elemH), elementStart, elementEnd)) {
                return false;
            }

            // test bottom line
            if (ConnectionLine.isAreaCrossing(new Point(elem.getX()-elemW, elem.getY()+elemH),
                    new Point(elem.getX()+elemW, elem.getY()+elemH), elementStart, elementEnd)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if a point can be moved to given location.
     *
     * @param newX new X location for the point
     * @param newY new Y location for the point
     * @return true if the point can be moved, false otherwise (out of margins, or some element is in the way).
     */
    boolean canMovePoint(int newX, int newY) {
        if (!fitToMargins(newX, newY)) {
            return false;
        }
        // Test all points of the line
        for (Element elem : getAllElements()) {
            if (elem.isSelected()) {
                continue;
            }
            Point elementStart = new Point(elem.getX() - elem.getWidth()/2, elem.getY()- elem.getHeight()/2);
            Point elementEnd = new Point(elem.getX() + elem.getWidth()/2, elem.getY() + elem.getHeight()/2);
            if (ConnectionLine.isAreaCrossingPoint(elementStart, elementEnd, new Point(newX, newY))) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method selects all settings that belong to specific plug-in. The plug-in name is specified in format as in
     * configuration file (eg. CPU plug-in name is 'cpu', compiler plug-in is called 'compiler', memory plug-in is
     * 'memory', devices are 'device0', 'device1', etc.).
     *
     * @param key name of the plug-in
     * @return Properties selection of settings for that plug-ing
     */
    private Properties selectSettings(String key) {
        Enumeration e = settings.keys();
        Properties selProps = new Properties();
        while (e.hasMoreElements()) {
            String akey = (String) e.nextElement();
            if (!akey.equals(key) && akey.startsWith(key)) {
                selProps.put(akey.substring(key.length() + 1), settings.getProperty(akey));
            }
        }
        return selProps;
    }

    /**
     * Get all settings of the virtual computer.
     *
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
            compilerElement.saveProperties(settings, "compiler");
        }
        // cpu
        if (cpuElement != null) {
            cpuElement.saveProperties(settings, "cpu");
        }
        // memory
        if (memoryElement != null) {
            memoryElement.saveProperties(settings, "memory");
        }
        // devices
        Map<DeviceElement, String> devsHash = new HashMap<>();
        int devicesCount = deviceElements.size();
        for (int i = 0; i < devicesCount; i++) {
            DeviceElement dev = deviceElements.get(i);
            devsHash.put(dev, "device" + i);
            dev.saveProperties(settings, "device" + i);
        }
        int linesCount = lines.size();
        for (int i = 0; i < linesCount; i++) {
            ConnectionLine line = lines.get(i);

            settings.put("connection" + i + ".bidirectional", String.valueOf(line.isBidirectional()));

            Element e = line.getJunc0();
            if (e instanceof CompilerElement) {
                settings.put("connection" + i + ".junc0", "compiler");
            } else if (e instanceof CpuElement) {
                settings.put("connection" + i + ".junc0", "cpu");
            } else if (e instanceof MemoryElement) {
                settings.put("connection" + i + ".junc0", "memory");
            } else if (e instanceof DeviceElement) {
                settings.put("connection" + i + ".junc0", devsHash.get(e));
            }

            e = line.getJunc1();
            if (e instanceof CompilerElement) {
                settings.put("connection" + i + ".junc1", "compiler");
            } else if (e instanceof CpuElement) {
                settings.put("connection" + i + ".junc1", "cpu");
            } else if (e instanceof MemoryElement) {
                settings.put("connection" + i + ".junc1", "memory");
            } else if (e instanceof DeviceElement) {
                settings.put("connection" + i + ".junc1", devsHash.get(e));
            }
            List<Point> points = line.getPoints();
            int pointsCount = points.size();
            for (int j = 0; j < pointsCount; j++) {
                Point po = points.get(j);
                settings.put("connection" + i + ".point" + j + ".x", String.valueOf((int) po.getX()));
                settings.put("connection" + i + ".point" + j + ".y", String.valueOf((int) po.getY()));
            }
        }
    }
}
