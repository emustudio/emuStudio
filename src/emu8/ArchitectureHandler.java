/*
 * ArchitectureHandler.java
 * 
 * Created on Friday, 28.1.2008 22:31
 * 
 * KEEP IT SIMPLE STUPID
 * sometimes just... YOU AREN'T GONNA NEED IT
 * 
 */

package emu8;

import java.util.Properties;
import java.util.Vector;
import plugins.memory.IMemory;
import plugins.compiler.*;
import plugins.cpu.ICPU;
import plugins.device.IDevice;

/**
 * Class holds actual computer configuration - plugins and settings
 * @author vbmacher
 */
public class ArchitectureHandler {
    private ICompiler compiler;
    private ICPU cpu;
    private IMemory memory;
    private IDevice[] devices;
    
    private int memSize;

    private Properties settings;
    private String name;
    private String[] devNames;
    /**
     * Constructor of new computer configuration and init all plugins.
     *
     * @param compiler Compiler plugin object
     * @param cpu CPU plugin object
     * @param memory Memory plugin object
     * @param devices Array of devices
     * @throws IllegalArgumentException if compiler, CPU or memory is null.
     */
    public ArchitectureHandler(String name, ICompiler compiler, ICPU cpu,
            IMemory memory, IDevice[] devices, Properties settings) 
            throws IllegalArgumentException {
        if (compiler == null) 
            throw new IllegalArgumentException("Compiler can't be null");
        if (cpu == null)
            throw new IllegalArgumentException("CPU can't be null");
        if (memory == null)
            throw new IllegalArgumentException("Memory can't be null");
        if (name == null) name = "";
        this.settings = settings;
        this.compiler = compiler;
        this.cpu = cpu;
        this.memory = memory;
        this.devices = devices;
        
        // assign names
        this.name = name;
        Vector devs = new Vector();
        // max. 256 devices
        for (int i = 0; i < 256; i++)
            if (settings.containsKey("device"+i))
                devs.add(settings.getProperty("device"+i));
        devNames = (String[])devs.toArray(new String[0]);
        
        // initialization
        memSize = Integer.valueOf(settings.getProperty("memorySize"));
        memory.init(memSize);
        cpu.init(memory);
        for (int i=0; i < devices.length; i++)
            devices[i].init(cpu, memory);
    }
    
    public ICompiler getCompiler() { return compiler; }
    public IMemory getMemory() { return memory; }
    public ICPU getCPU() { return cpu; }
    public IDevice[] getDevices() { return devices; }

    public String getArchName() { return name; }
    public String getCompilerName() {
        return settings.getProperty("compiler");
    }
    public String getCPUName() {
        return settings.getProperty("cpu");
    }
    public String getMemoryName() {
        return settings.getProperty("memory");
    }
    public String[] getDeviceNames() { return devNames; }
}
