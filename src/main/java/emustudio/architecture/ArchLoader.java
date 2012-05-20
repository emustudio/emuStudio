/**
 * ArchLoader.java
 *
 * Created on Utorok, 2007, august 7, 11:11
 * KISS, YAGNI, DRY
 * 
 * Copyright (C) 2007-2012 Peter Jakubčo
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
 *
 */

package emustudio.architecture;

import emulib.plugins.IPlugin;
import emulib.plugins.compiler.ICompiler;
import emulib.plugins.cpu.ICPU;
import emulib.plugins.device.IDevice;
import emulib.plugins.memory.IMemory;
import emulib.runtime.PluginLoader;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.drawing.Schema;
import emustudio.main.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Class loader for plugins and their resources (singleton).
 *
 * This class deals with emulator configuration - loads classes, maps devices,
 * etc.
 *
 * A configuration file is Java properties file. It means it is like Windows INI
 * file without [] classes. Settings of specific devices can be accessed via
 * dot (.) notation. Settings can have various hierarchy. Loader separates type
 * of plugin (first string before dot) and the rest passes to the plugin.
 * Syntax of the definition of a plugin in the configuration is as follows:
 *
 * plugin_type = "name_of_plugin"
 * [plugin_type.param1 = "param"]
 * [plugin_type.param1.parama = "param2"]
 *   ...
 *
 * Example of using them: http://www.rgagnon.com/javadetails/java-0024.html
 * Standard emulator configuration (e.g. Altair8800.conf) file contains
 * following: (# and ! are comments)
 *
 *
 * # emulator version
 * emu8Version = 4
 * ! CPU name
 * cpu = Intel8080                            ! required
 * cpu.frequency = 2000
 * cpu.testperiode = 50
 * cpu.point.x = 340                          ! required
 * cpu.point.y = 266                          ! required
 * compiler = assemblerIntel8080              ! required
 * compiler.output = file
 * memory = nonbanked                         ! required
 * memory.size = 16384                        ! required
 * memory.point.x = 400                       ! required
 * memory.point.y = 266                       ! required
 * device0 = harddisk                         ! required
 * device0.image =/home/vbmacher/image.dsk
 * device0.point.x = 340                      ! required
 * device0.point.y = 500                      ! required
 * device1 = keyboard
 * device1.point.x = 400                      ! required
 * device1.point.y = 500                      ! required
 * device2 = "screen"
 * device2.point.x = 600                      ! required
 * device2.point.y = 500                      ! required
 * device2.antialiasing = "false"
 * ...
 * connection0.junc0 = cpu                    ! required
 * connection0.junc1 = memory                 ! required
 * connection0.point0.x = 300
 * connection0.point0.y = 400
 * ...
 * 
 * @author vbmacher
 */
public class ArchLoader {
    /**
     * Directory name where the virtual computer configurations are stored.
     */
    public final static String CONFIGS_DIR = "config";

    /**
     * Directory name where CPUs are stored.
     */
    public final static String CPUS_DIR = "cpu";

    /**
     * Directory name where compilers are stored.
     */
    public final static String COMPILERS_DIR = "compilers";

    /**
     * Directory name where memories are stored.
     */
    public final static String MEMORIES_DIR = "mem";

    /**
     * Directory name where devices are stored.
     */
    public final static String DEVICES_DIR = "devices";

    private static long nextPluginID = 0;
    
    private static ArchLoader instance;

    class PluginInfo {
        public String pluginSettingsName;
        public String pluginName;
        public Class<?> pluginInterface;
        public long pluginId;
        public IPlugin plugin;
        public String dirName;
        public Class<IPlugin> mainClass;

        public PluginInfo(String pluginSettingsName, String dirName,
                String pluginName, Class<?> pluginInterface, long pluginId) {
            this.dirName = dirName;
            this.pluginId = pluginId;
            this.pluginInterface = pluginInterface;
            this.pluginName = pluginName;
            this.pluginSettingsName = pluginSettingsName;
        }
    }

    /**
     * This forbids of creating the instance of this class. This class is
     * a singleton.
     */
    private ArchLoader() {
    }

    /**
     * Get instance of this class.
     * 
     * @return always the same instance (singleton)
     */
    public static ArchLoader getInstance() {
        if (instance == null) {
            instance = new ArchLoader();
        }
        return instance;
    }
    
    /**
     * Method returns all file names from a dir that ends with specified
     * postfix.
     * 
     * @param dirname directory to get files from
     * @param postfix
     * @return String array of names
     */
    public static String[] getAllNames(String dirname, 
            final String postfix) {
        String[] allNames = null;
        File dir = new File(System.getProperty("user.dir") + File.separator + dirname);
        if (dir.exists() && dir.isDirectory()) {
            allNames = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(postfix);
                }
            });
            for (int i = 0; i < allNames.length; i++)
                allNames[i] = allNames[i].substring(0, 
                        allNames[i].lastIndexOf(postfix));
        }
        return allNames;
    }
    
    /**
     * Method deletes virtual configuration file from filesystem.
     * 
     * @param configName Name of the configuration
     * @return true if the operation was successful, false otherwise
     */
    public static boolean deleteConfig(String configName) {
        File file = new File(System.getProperty("user.dir") +
                File.separator + CONFIGS_DIR + File.separator + configName
                + ".conf");
        if (!file.exists())
            return false;
        try {
            return file.delete();
        } catch(Exception e) {
        }
        return false;
    }

    /**
     * Renames configuration to a new name.
     *
     * @param newName new name
     * @param oldName old, origin name
     * @return true if the operation was successful.
     */
    public static boolean renameConfig(String newName, String oldName) {
        File f = new File(System.getProperty("user.dir") +
                File.separator + CONFIGS_DIR + File.separator + oldName
                + ".conf");
        if (!f.exists())
            return false;
        try {
            return f.renameTo(new File(newName));
        } catch(Exception e) {
        }
        return false;
    }
    
    /**
     * Method loads schema from configuration file. It is used
     * by ArchitectureEditor.
     * 
     * @param configName Name of the virtual configuration
     * @return Schema of the configuration, or null if some error
     *         raises.
     */
    public Schema loadSchema(String configName) {
        try{
            Properties p = readConfig(configName,true);

            if (p == null)
                return null;
            
            return new Schema(configName,p);
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error reading configuration: "
                    + e.toString());
            return null;
        }
    }
    
    /**
     * Method saves abstract schema of some configuration into configuration
     * file. 
     * @param schema Schema to save
     */
    public void saveSchema(Schema schema) {
        try {
            schema.save();
            writeConfig(schema.getConfigName(),schema.getSettings());
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error writing configuration: " 
                    + e.toString());
        }
    }
    
    
    /**
     * Method reads configuration into Properties object.
     * 
     * @param configName
     * @param schema_too whether read schema settings too
     * @return properties object (settings for actual architecture
     * congiguration)
     */
    private static Properties readConfig(String configName, boolean schema_too) {
        try{
            Properties p = new Properties();
            File f = new File(System.getProperty("user.dir") + 
                    File.separator + CONFIGS_DIR + File.separator + configName
                    + ".conf");
            if (!f.exists()) return null;
            FileInputStream fin = new FileInputStream(f);
            p.load(fin);
            fin.close();
            if (!p.getProperty("emu8Version").equals("3")
                    && !p.getProperty("emu8Version").equals("4")) {
                StaticDialogs.showErrorMessage("Error reading configuration: " +
                        "unsupported file version");
                return null;
            }
            if (!schema_too) {
                p.remove("compiler");
                p.remove("compiler.point.x");
                p.remove("compiler.point.y");
                p.remove("compiler.width");
                p.remove("compiler.height");
                p.remove("cpu");
                p.remove("cpu.point.x");
                p.remove("cpu.point.y");
                p.remove("cpu.width");
                p.remove("cpu.height");
                p.remove("memory");
                p.remove("memory.point.x");
                p.remove("memory.point.y");
                p.remove("memory.width");
                p.remove("memory.height");
                for (int i = 0; p.containsKey("device"+i); i++) {
                    p.remove("device"+i);
                    p.remove("device"+i+",point.x");
                    p.remove("device"+i+",point.y");
                    p.remove("device"+i+".width");
                    p.remove("device"+i+".height");
                }
                for (int i = 0; p.containsKey("connection"+i+".junc0"); i++) {
                    p.remove("connection"+i+".junc0");
                    p.remove("connection"+i+".junc1");
                    for (int j = 0; p.containsKey("connection"+i+".point"+j+".x"); j++) {
                        p.remove("connection"+i+".point"+j+".x");
                        p.remove("connection"+i+".point"+j+".y");
                    }
                }
            }
            return p;
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error reading configuration: " + e.toString());
            return null;
        }
    }
    
    /** 
     * Method save configuration to a file with name configName.
     *
     * @param configName name of configuration
     * @param settings data are taken from
     */
    public static void writeConfig(String configName, Properties settings) {
        try {
            String dir = System.getProperty("user.dir") + File.separator
                    + CONFIGS_DIR;
            File dirFile = new File(dir);
            if (!dirFile.exists() || (dirFile.exists() && !dirFile.isDirectory())) {
                if (!dirFile.mkdir())
                    throw new Exception("could not create config directory");
            }

            File f = new File(dir + File.separator + configName + ".conf");
            f.createNewFile();
            FileOutputStream out = new FileOutputStream(f);
            settings.put("emu8Version", "4");
            settings.store(out, configName + " configuration file");
            out.close();
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error writing configuration: " + e.toString());
        }
    }
    
    /**
     * Method loads virtual configuration from current settings and
     * creates virtual architecture.
     * 
     * @param name  Name of the configuration
     * @param auto  If the emuStudio is runned in automatization mode
     * @param nogui If "--nogui" parameter was passed to emuStudio
     * 
     * @return instance of virtual architecture
     */
    public ArchHandler loadComputer(String name, boolean auto, boolean nogui)
            throws PluginLoadingException {
        
        Properties settings = readConfig(name, true);
        if (settings == null) {
            return null;
        }
        
        Map<String, PluginInfo> pluginsToLoad = new HashMap<String, PluginInfo>();
        
        String tmp = settings.getProperty("compiler");
        if (tmp != null) {
            pluginsToLoad.put("compiler", new PluginInfo("compiler", COMPILERS_DIR,
                    tmp, ICompiler.class, createPluginID()));
        }
        tmp = settings.getProperty("cpu");
        if (tmp != null) {
            pluginsToLoad.put("cpu", new PluginInfo("cpu", CPUS_DIR, tmp,
                    ICPU.class, createPluginID()));
        }
        tmp = settings.getProperty("memory");
        if (tmp != null) {
            pluginsToLoad.put("memory", new PluginInfo("memory", MEMORIES_DIR,
                    tmp, IMemory.class, createPluginID()));
        }
        for (int i = 0; settings.containsKey("device" + i); i++) {
            tmp = settings.getProperty("device" + i);
            if (tmp != null) {
                pluginsToLoad.put("device" + i, new PluginInfo("device" + i,
                        DEVICES_DIR, tmp, IDevice.class, createPluginID()));
            }
        }
        
        PluginLoader pluginLoader = PluginLoader.getInstance();
        for (PluginInfo plugin : pluginsToLoad.values()) {
            Class<IPlugin> mainClass = loadPlugin(plugin.dirName, plugin.pluginName);
            plugin.mainClass = mainClass;
        }
        if (pluginLoader.canResolveClasses()) {
            // Resolve all plug-in classes
            pluginLoader.resolveLoadedClasses();
        } else {
            if (pluginLoader.loadUndoneClasses()) {
                pluginLoader.resolveLoadedClasses();
            } else {
                throw new PluginLoadingException("Cannot load all classes of plug-ins.", "[unknown]", null);
            }
        }
        System.out.println("All plugins are loaded and resolved.");
        
        ICompiler compiler = null;
        ICPU cpu = null;
        IMemory mem = null;
        List<IDevice> devList = new ArrayList<IDevice>();
        for (PluginInfo plugin : pluginsToLoad.values()) {
            try {
                plugin.plugin = (IPlugin)newPlugin(plugin.mainClass, plugin.pluginInterface, plugin.pluginId);
                if (plugin.plugin instanceof ICompiler) {
                    compiler = (ICompiler)plugin.plugin;
                } else if (plugin.plugin instanceof ICPU) {
                    cpu = (ICPU)plugin.plugin;
                } else if (plugin.plugin instanceof IMemory) {
                    mem = (IMemory)plugin.plugin;
                } else if (plugin.plugin instanceof IDevice) {
                    devList.add((IDevice)plugin.plugin);
                }
            } catch (ClassNotFoundException e) {
                throw new PluginLoadingException("Plugin '" + plugin.pluginName
                        + "' cannot be loaded.", plugin.pluginName, 
                        (IPlugin) plugin.plugin);
            }
        }

        // load connections
        Map<Long, ArrayList<Long>> connections = new HashMap<Long,
                ArrayList<Long>>();
        for (int i = 0; settings.containsKey("connection" + i + ".junc0"); i++) {
            // get i-th connection from settings
            String j0 = settings.getProperty("connection" + i + ".junc0", "");
            String j1 = settings.getProperty("connection" + i + ".junc1", "");
            boolean bidi = Boolean.parseBoolean(settings.getProperty("bidirectional", "true"));

            if (j0.equals("") || j1.equals("")) {
                continue;
            }

            // map the connection elements to plug-ins: p1 and p2
            // note the connection: p1 -> p2  (p1 wants to use p2)
            IPlugin p1 = null, p2 = null;
            long pID1, pID2;

            PluginInfo pluginInfo = pluginsToLoad.get(j0);
            if (pluginInfo == null) {
                System.out.println("Invalid connection, j0=" + j0);
                continue; // invalid connection
            }
            p1 = (IPlugin)pluginInfo.plugin;
            pID1 = pluginInfo.pluginId;

            pluginInfo = pluginsToLoad.get(j1);
            if (pluginInfo == null) {
                System.out.println("Invalid connection, j1=" + j1);
                continue; // invalid connection
            }
            p2 = (IPlugin)pluginInfo.plugin;
            pID2 = pluginInfo.pluginId;

            // the first direction
            if (connections.containsKey(pID1)) {
                connections.get(pID1).add(pID2);
            } else {
                ArrayList<Long> ar = new ArrayList<Long>();
                ar.add(pID2);
                connections.put(pID1, ar);
            }
            if (bidi) {
                // if bidirectional, then also the other connection
                if (connections.containsKey(pID2)) {
                    connections.get(pID2).add(pID1);
                } else {
                    ArrayList<Long> ar = new ArrayList<Long>();
                    ar.add(pID1);
                    connections.put(pID2, ar);
                }
            }
        }

        // this creates reversed array..
        Collections.reverse(devList);
        IDevice[] devices = (IDevice[]) devList.toArray(new IDevice[0]);
        Computer arch = new Computer(cpu, mem, compiler, devices,
                pluginsToLoad.values(), connections);
        emulib.runtime.Context.getInstance().assignComputer(Main.getPassword(),
                arch);
        return new ArchHandler(arch, settings, loadSchema(name), 
                pluginsToLoad.values(), auto, nogui);
    }
    
    /**
     * Method compute an ID for a plugin identification for one runtime
     * session.
     *
     * For the security reasons, the ID should be made from
     * <code>System.nanoTime()</code> and from <code>Math.random()</code>.
     *
     * For the safety of correctness, the ID is made only as increased value
     * of some variable.
     * 
     * @return hash for an identification of the plugin
     */
    private long createPluginID() {
    	return nextPluginID++;
    }


    /**
     * Method return new instance of a plugin from jar file that implements
     * given interface.
     *
     * Each main class implementing the interface must have two parameters within
     * the constructor - Long pluginID and ISettingsHandler settings.
     * 
     * @param dirname type of a plugin (compiler, cpu, memory, devices)
     * @param filename name of the plugin
     * @return Main class of the plugin. It must be resolved before first use.
     */
    private Class<IPlugin> loadPlugin(String dirname, String filename) {
        return emulib.runtime.PluginLoader.getInstance().loadPlugin(
                System.getProperty("user.dir") + File.separator + dirname
                + File.separator + filename);
    }
    
    /**
     * Creates new instance of class from given list that implements specified
     * interface.
     * 
     * The class implementing the interface is found automatically. The method
     * takes into account only the first matching class.
     * 
     * @param mainClass Main class of the plug-in. It must be already resolved.
     * @param pluginInterface The interface that main class MUST implement
     * @param pluginID The plug-in identification number
     * @return Instance object of loaded plugin
     * @throws ClassNotFoundException 
     *     When the main class is null, or the class does not contain proper
     *     constructor.
     */
    private IPlugin newPlugin(Class<IPlugin> mainClass, Class<?> pluginInterface, long pluginID)
            throws ClassNotFoundException {
        if (mainClass == null) {
            throw new ClassNotFoundException("Plug-in main class does not exist");
        }
        
        if (!PluginLoader.getInstance().doesImplement(mainClass, pluginInterface)) {
            throw new ClassNotFoundException("Plug-in main class does not implement specified interface");
        }

        // First parameter of constructor is plug-in ID
        Class<?>[] conParameters = {Long.class};

        try {
            Constructor<?> con = mainClass.getDeclaredConstructor(conParameters);
            if (con != null) {
                return (IPlugin) con.newInstance(pluginID);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassNotFoundException("Plug-in main class does not have proper constructor");
        }
    }
}
