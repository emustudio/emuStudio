/*
 * ArchHandler.java
 * 
 * Created on Friday, 28.1.2008 22:31
 * 
 * KEEP IT SIMPLE STUPID
 * sometimes just... YOU AREN'T GONNA NEED IT
 * 
 */

package architecture;

import architecture.drawing.Schema;

import java.util.Hashtable;
import java.util.Properties;
import plugins.memory.IMemory;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import plugins.IPlugin;
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
public class ArchHandler implements ISettingsHandler {
    private Architecture           arch;
    private Properties             settings;
    private Schema                 schema;
    private Hashtable<Long,String> deviceNames;
    
    /**
     * Constructor of new computer configuration and init all plugins.
     * 
     * @param name         Name of the architecture
     * @param arch         Virtual architecture object - plugin handler
     * @param settings     Architecture settings (Properties)
     * @param schema       Abstract schema of the architecture
     * @param deviceNames  Names of all devices (can be redundant)
     *  
     * @throws Error if initialization of the architecture failed.
     */
    public ArchHandler(String name, Architecture arch, Properties settings,
            Schema schema, Hashtable<Long,String> deviceNames) throws Error {
    	if (name == null) name = "";
    	this.arch = arch;
        this.settings = settings;
        this.schema = schema;
        this.deviceNames = deviceNames;

        if (initialize() == false) 
            throw new Error("Initialization of plugins failed");
    }

    /**
     * Initialize all plugins exept compiler. The method is called by
     * constructor. Also provides necessary connections.
     * 
     * @return true If the initialization succeeded, false otherwise
     */
    private boolean initialize() {
        boolean success = false;

        int memSize = Integer.parseInt(settings.getProperty("memory.size"));
        success = arch.getMemory().initialize(memSize, this);
        
        if (arch.isConnected(arch.getCPU(), arch.getMemory()))
            success &= arch.getCPU().initialize(arch.getMemory().getContext(), this);
        else
            success &= arch.getCPU().initialize(null, this);
        
        // CPU-device and Memory-device connections
        for (int i=0; i < arch.getDeviceCount(); i++) {
            ICPUContext ccon = arch.isConnected(arch.getCPU(),arch.getDevice(i)) 
                    ? arch.getCPU().getContext() : null;
            IMemoryContext mcon = arch.isConnected(arch.getMemory(),arch.getDevice(i))
                    ? arch.getMemory().getContext() : null;
            success &= arch.getDevice(i).initialize(ccon, mcon, this);
        }
        
        // finally device-device connections
        for (int i = 0; i < arch.getDeviceCount(); i++) {
        	IDevice female = arch.getDevice(i);
        	IDeviceContext[] males = arch.getMales(female);
        	if (males != null) {
        		for (int j = 0; j < males.length; j++)
        			female.attachDevice(males[j]);
        	}
        }

        // first reset of all plugins
        arch.resetPlugins();
        return success;
    }
    
    /**
     * Method destroys current architecture
     */
    public void destroy() {
        try {
            for (int i = 0; i < arch.getAllDevices().length; i++)
                arch.getAllDevices()[i].destroy();
            arch.getCPU().destroy();
            arch.getMemory().destroy();
        } catch (Exception e) {}    	
    }
    
    /***
     * Get schema of this virtual architecture
     * 
     * @return Abstract schema
     */
    public Schema getSchema() { return schema; }
    
    /**
     * Gets actual compiler
     *
     * @return compiler interface object
     */
    public ICompiler getCompiler() { return arch.getCompiler(); }

    /**
     * Gets actual operating memory
     *
     * @return memory interface object
     */
    public IMemory getMemory() { return arch.getMemory(); }

    /**
     * Gets actual CPU
     *
     * @return CPU interface object
     */
    public ICPU getCPU() { return arch.getCPU(); }

    /**
     * Gets list of available devices
     *
     * @return array of device interface objects
     */
    public IDevice[] getDevices() { return arch.getAllDevices(); }

    public String getArchName() { 
        return (schema == null) ? "unknown" : schema.getConfigName(); 
    }
    
    /**
     * Method reads value of specified setting from Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param hash         plugin hash, identification of a plugin
     * @param settingName  name of wanted setting
     * @return setting value if exists, or null if not
     */
    public synchronized String readSetting(long hash, String settingName) {
        IPlugin plug = arch.getPlugin(hash);
        if (plug == null) return null;
        
        String prop = "";
                
        if (plug instanceof IDevice) {
            // search for device
        	String deviceName = deviceNames.get(hash);
            for (int i = 0; i < arch.getDeviceCount(); i++)
                if (settings.getProperty("device"+i,"").equals(deviceName)) {
                    prop = "device"+i;
                    break;
                }
        } else if (plug instanceof ICPU)    prop = "cpu";
        else if (plug instanceof IMemory)   prop = "memory";
        else if (plug instanceof ICompiler) prop = "compiler";
        
        if (prop.equals("")) return null;
        if (settingName != null && !settingName.equals("")) 
            prop += "." + settingName;
        
        return settings.getProperty(prop,null);
    }

    /**
     * Get device name (file name without extension)
     * 
     * @param index  Index of the device
     * @return device file name without extension, or null
     *         if device is unknown
     */
    public String getDeviceName(int index) { 
    	return settings.getProperty("device"+index, null);
    }
    
    /**
     * Get compiler file name, without file extension.
     * 
     * @return compiler name or null
     */     
    public String getCompilerName() {
    	return settings.getProperty("compiler",null);
    }

    /**
     * Get CPU file name, without file extension.
     * 
     * @return CPU name or null
     */     
    public String getCPUName() {
    	return settings.getProperty("cpu",null);
    }

    /**
     * Get memory file name, without file extension.
     * 
     * @return memory name or null
     */     
    public String getMemoryName() {
    	return settings.getProperty("memory",null);
    }
    
    /**
     * Method writes a value of specified setting to Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param hash         plugin hash, identification of a plugin
     * @param settingName name of wanted setting
     */
    public synchronized void writeSetting(long hash, String settingName, String val) {
        if (settingName == null || settingName.equals("")) return;

        IPlugin plug = arch.getPlugin(hash);
        if (plug == null) return;

        String prop = "";
        if (plug instanceof IDevice) {
            // search for device
        	String deviceName = deviceNames.get(hash);
            for (int i = 0; i < arch.getDeviceCount(); i++)
                if (settings.getProperty("device"+i,"").equals(deviceName)) {
                    prop = "device"+i;
                    break;
                }
        } else if (plug instanceof ICPU) prop = "cpu";
        else if (plug instanceof IMemory)   prop = "memory";
        else if (plug instanceof ICompiler) prop = "compiler";
        
        if (prop.equals("")) return;
        prop += "." + settingName;
        
        settings.setProperty(prop, val);
        ArchLoader.writeConfig(schema.getConfigName(), settings);
    }
    
    /**
     * Method removes value of specified setting from Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param hash         plugin hash, identification of a plugin
     * @param settingName name of wanted setting
     */
    public synchronized void removeSetting(long hash, String settingName) {
        if (settingName == null || settingName.equals("")) return;

        IPlugin plug = arch.getPlugin(hash);
        if (plug == null) return;

        String prop = "";
                
        if (plug instanceof IDevice) {
            // search for device
        	String deviceName = deviceNames.get(hash);
            for (int i = 0; i < arch.getDeviceCount(); i++)
                if (settings.getProperty("device"+i,"").equals(deviceName)) {
                    prop = "device"+i;
                    break;
                }
        } else if (plug instanceof ICPU) prop = "cpu";
        else if (plug instanceof IMemory)   prop = "memory";
        else if (plug instanceof ICompiler) prop = "compiler";

        if (prop.equals("")) return;
        prop += "." + settingName;
        
        settings.remove(prop);
        ArchLoader.writeConfig(schema.getConfigName(), settings);        
    }

 
}
