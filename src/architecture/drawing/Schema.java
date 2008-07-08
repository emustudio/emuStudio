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

    public Schema(CpuElement cpuElement, MemoryElement memoryElement, 
            ArrayList<DeviceElement> deviceElements, 
            ArrayList<ConnectionLine> lines) {
        this.cpuElement = cpuElement;
        this.memoryElement = memoryElement;
        this.deviceElements = new ArrayList<DeviceElement>();
        this.deviceElements.addAll(deviceElements);
        this.lines = new ArrayList<ConnectionLine>();
        this.lines.addAll(lines);
    }
    
    public Schema() {
        cpuElement = null;
        memoryElement = null;
        deviceElements = new ArrayList<DeviceElement>();
        lines = new ArrayList<ConnectionLine>();
    }
    
    private void removeIncidentLines(Element el) {
        for (int i = lines.size()-1; i >= 0; i--)
            if (lines.get(i).containsElement(el))
                lines.remove(i);
    }
    
    public CpuElement getCpuElement() {
        return cpuElement;
    }
    
    public MemoryElement getMemoryElement() {
        return memoryElement;
    }
    
    public ArrayList<DeviceElement>getDeviceElements() {
        return deviceElements;
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
