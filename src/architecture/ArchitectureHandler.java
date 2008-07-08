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
import plugins.memory.IMemory;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.cpu.ICPUContext;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;

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
    private PluginConnection[] connections;
    private String name;
    private Properties settings;
    
    public class PluginConnection {
        private String junc0;
        private String junc1;
        public PluginConnection(String junc0, String junc1) {
            this.junc0 = junc0;
            this.junc1 = junc1;
        }
        public String getJunc0() { return junc0; }
        public String getJunc1() { return junc1; }
        public boolean contains(String type) {
            if (junc0.equals(type) || junc1.equals(type))
                return true;
            return false;
        }
    }

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
            IMemory memory, IDevice[] devices, 
            PluginConnection[] connections, Properties settings) 
            throws IllegalArgumentException, Error {
        
        if (compiler == null) 
            throw new IllegalArgumentException("Compiler can't be null");
        if (cpu == null)
            throw new IllegalArgumentException("CPU can't be null");
        if (memory == null)
            throw new IllegalArgumentException("Memory can't be null");
        if (name == null) name = "";
        this.compiler = compiler;
        this.cpu = cpu;
        this.memory = memory;
        this.devices = devices;
        this.connections = connections;
        this.settings = settings;
        
        if (initialize() == false) 
            throw new Error("Initialization of plugins failed");
    }
    
    private boolean isConnected(String type1, String type2) {
        for (int i = 0; i < connections.length; i++)
            if (connections[i].contains(type1)
                    && connections[i].contains(type2))
                return true;
        return false;
    }
    /**
     * Initialize all plugins. It is called from constructor.
     * Also provides necessary connections
     */
    private boolean initialize() {
        boolean r = false;
        compiler.initialize(this);
        int memSize = Integer.parseInt(settings
                .getProperty("memory.size"));
        memory.initialize(memSize, this);
        
        if (isConnected("cpu","memory"))        
            r = cpu.initialize(memory.getContext(), this);
        else
            r = cpu.initialize(null, this);
        
        for (int i=0; i < devices.length; i++) {
            ICPUContext ccon = isConnected("cpu","device"+i) 
                    ? cpu.getContext() : null;
            IMemoryContext mcon = isConnected("memory","device"+i)
                    ? memory.getContext() : null;
            r &= devices[i].initialize(ccon, mcon, this);
        }
        // finally device-device connections
        for (int i = 0; i < connections.length; i++) {
            String j0 = connections[i].getJunc0();
            String j1 = connections[i].getJunc1();
            if (j0.startsWith("device") && j1.startsWith("device")) {
                // TODO: jednosmerna komunikacia??
                int i1 = Integer.parseInt(j0.substring(6));
                int i2 = Integer.parseInt(j1.substring(6));
                // 1. smer
                IDeviceContext female = devices[i1].getFreeFemale();
                IDeviceContext male = devices[i2].getFreeMale();
                if (female != null && male != null)
                    r &= devices[i1].attachDevice(female, male);
                // 2. smer
                female = devices[i2].getFreeFemale();
                male = devices[i1].getFreeMale();
                if (female != null && male != null)
                    r &= devices[i2].attachDevice(female, male);
            }
        }
        return r;
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
    
    /**
     * Method reads value of specified setting from Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param pluginID plugin name, this have a mean only for device plugin,
     *                 otherwise it is ignored.
     * @param settingName name of wanted setting
     * @return setting value if exists, or null if not
     */
    public String readSetting(pluginType plType, String pluginID, 
            String settingName) {
        if (settingName == null || settingName.equals("")) return null;
        if (pluginID == null || pluginID.equals("")) return null;
        if (plType == null) return null;

        String prop = "";
                
        if (plType == pluginType.device) {
            // search for device
            for (int i = 0; i < devices.length; i++)
                if (settings.getProperty("device"+i,"").equals(pluginID)) {
                    prop = "device"+i+".";
                    break;
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
            for (int i = 0; i < devices.length; i++)
                if (settings.getProperty("device"+i,"").equals(pluginID)) {
                    prop = "device"+i+".";
                    break;
                }
        } else prop = plType.toString();
        
        if (prop.equals("")) return;
        prop += settingName;
        
        settings.setProperty(prop, val);
        ArchitectureLoader.writeConfig(name, settings);
    }
 
}
