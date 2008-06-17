/*
 * ArchitectureHandler.java
 * 
 * Created on Friday, 28.1.2008 22:31
 * 
 * KEEP IT SIMPLE STUPID
 * sometimes just... YOU AREN'T GONNA NEED IT
 * 
 */

package architecture;

import java.util.Properties;
import java.util.Vector;
import plugins.memory.IMemory;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;

/**
 * Class holds actual computer configuration - plugins and settings.
 *
 * @author vbmacher
 */
public class ArchitectureHandler implements ISettingsHandler {
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
        initialize();
    }
    
    /**
     * Initialize all plugins. It is called from constructor
     */
    private void initialize() {
        compiler.initialize(this);
        memSize = Integer.valueOf(settings.getProperty("memorySize"));
        memory.initialize(memSize, this);
        cpu.initialize(memory, this);
        for (int i=0; i < devices.length; i++)
            devices[i].initialize(cpu.getContext(), memory, this);
    }
    
    /**
     * Gets actual compiler
     *
     * @return compiler interface object
     */
    public ICompiler getCompiler() { return compiler; }

    /**
     * Gets actual operating memory
     *
     * @return memory interface object
     */
    public IMemory getMemory() { return memory; }

    /**
     * Gets actual CPU
     *
     * @return CPU interface object
     */
    public ICPU getCPU() { return cpu; }

    /**
     * Gets list of available devices
     *
     * @return array of device interface objects
     */
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

    // work with settings
    
    /**
     * Method reads value of specified setting from Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param pluginID plugin name, this have a mean only for device plugin,
     *                 otherwise it is ignored.
     * @param settingName name of wanted setting
     * @return setting value if exists, or null if not
     */
    public String readSetting(pluginType plType, String pluginID, String settingName) {
        if (settingName == null || settingName.equals("")) return null;
        if (pluginID == null || pluginID.equals("")) return null;
        if (plType == null) return null;

        String prop = "";
                
        if (plType == pluginType.device) {
            // search for device
            for (int i = 0; i < 255; i++) {
                if (settings.containsKey("device"+i)
                        && settings.getProperty("device"+i).equals(pluginID)) {
                    prop = "device"+i+".";
                }
            }
        } else prop = plType.toString();
        
        if (prop.equals("")) return null;
        prop += settingName;
        
        return settings.getProperty(prop);
    }

    /**
     * Method writes a value of specified setting to Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param pluginID plugin name, this have a mean only for device plugin,
     *                 otherwise it is ignored.
     * @param settingName name of wanted setting
     * @return setting value if exists, or null if not
     */
    public void writeSetting(pluginType plType, String pluginID,
            String settingName, String val) {
        if (settingName == null || settingName.equals("")) return;
        if (pluginID == null || pluginID.equals("")) return;
        if (plType == null) return;

        String prop = "";
        if (plType == pluginType.device) {
            // search for device
            for (int i = 0; i < 255; i++) {
                if (settings.containsKey("device"+i)
                        && settings.getProperty("device"+i).equals(pluginID)) {
                    prop = "device"+i+".";
                }
            }
        } else prop = plType.toString();
        
        if (prop.equals("")) return;
        prop += settingName;
        
        settings.setProperty(prop, val);
        ArchitectureLoader.writeConfig(name, settings);
    }
 
}
