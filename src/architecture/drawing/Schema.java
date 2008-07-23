/*
 * Schema.java
 *
 * Created on 6.7.2008, 17:08:51
 * hold to: KISS, YAGNI
 *
 */

package architecture.drawing;

import java.util.ArrayList;

/**
 *
 * @author vbmacher
 */
public class Schema {
    private CpuElement cpuElement;
    private MemoryElement memoryElement;
    private ArrayList<DeviceElement> deviceElements;
    private ArrayList<ConnectionLine> lines;
    
    private String compilerName;
    private int memorySize;
    private String configName;

    public Schema(CpuElement cpuElement, MemoryElement memoryElement, 
            ArrayList<DeviceElement> deviceElements, 
            ArrayList<ConnectionLine> lines, String configName, String compilerName,
            int memorySize) {
        this.cpuElement = cpuElement;
        this.memoryElement = memoryElement;
        this.deviceElements = new ArrayList<DeviceElement>();
        this.deviceElements.addAll(deviceElements);
        this.lines = new ArrayList<ConnectionLine>();
        this.lines.addAll(lines);
        this.configName = configName;
        this.compilerName = compilerName;
        this.memorySize = memorySize;
    }
    
    public Schema() {
        cpuElement = null;
        memoryElement = null;
        deviceElements = new ArrayList<DeviceElement>();
        lines = new ArrayList<ConnectionLine>();
        configName = "";
        compilerName = "";
        memorySize = 0;
    }
    
    private void removeIncidentLines(Element el) {
        for (int i = lines.size()-1; i >= 0; i--)
            if (lines.get(i).containsElement(el))
                lines.remove(i);
    }
    
    public String getConfigName() { return configName; }
    public String getCompilerName() { return compilerName; }
    public int getMemorySize() { return memorySize; }
    public void setConfigName(String cName) { configName = cName; }
    public void setCompilerName(String cName) { compilerName = cName; }
    public void setMemorySize(int mSize) { memorySize = mSize; }

    public CpuElement getCpuElement() {
        return cpuElement;
    }
    
    public MemoryElement getMemoryElement() {
        return memoryElement;
    }
    
    public ArrayList<DeviceElement>getDeviceElements() {
        return deviceElements;
    }
    
    public ArrayList<Element>getAllElements() {
        ArrayList<Element> a = new ArrayList<Element>();
        if (cpuElement != null) a.add(cpuElement);
        if (memoryElement != null) a.add(memoryElement);
        a.addAll(deviceElements);
        return a;
    }
    
    public ArrayList<ConnectionLine>getConnectionLines() {
        return lines;
    }
    
    public void setCpuElement(CpuElement cpuElement) {
        if (cpuElement == null && this.cpuElement != null)
            removeIncidentLines(this.cpuElement);
        this.cpuElement = cpuElement;
    }

    public void setMemoryElement(MemoryElement memoryElement) {
        if (memoryElement == null && this.memoryElement != null)
            removeIncidentLines(this.memoryElement);
        this.memoryElement = memoryElement;
    }

    public void addDeviceElement(DeviceElement deviceElement) {
        deviceElements.add(deviceElement);
    }
    
    public void addConnectionLine(ConnectionLine conLine) {
        lines.add(conLine);
    }
    
    public void removeDeviceElement(int index) {
        if (index < 0) return;
        try {
            removeIncidentLines(deviceElements.get(index));
            deviceElements.remove(index);
        } catch(Exception e) {}
    }
    
    public void removeConnectionLine(int index) {
        if (index < 0) return;
        lines.remove(index);
    }
}
