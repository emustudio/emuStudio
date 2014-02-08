/*
 * ArchitectureLoader.java
 *
 * Created on Utorok, 2007, august 7, 11:11
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2013, Peter Jakubčo
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

import emulib.plugins.Plugin;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidPasswordException;
import emulib.runtime.InvalidPluginException;
import emulib.runtime.PluginLoader;
import emustudio.architecture.drawing.Schema;
import emustudio.main.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public class ArchitectureLoader implements ConfigurationManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(ArchitectureLoader.class);
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

    private static String configurationBaseDirectory = System.getProperty("user.dir");
    private static long nextPluginID = 0;
    private final PluginLoader pluginLoader = new PluginLoader();

    private static ArchitectureLoader instance;

    public class SortedProperties extends Properties {

        @Override
        public synchronized Enumeration keys() {
            Enumeration keysEnum = super.keys();
            List keyList = new ArrayList();
            while (keysEnum.hasMoreElements()) {
                keyList.add(keysEnum.nextElement());
            }
            Collections.sort(keyList);
            return Collections.enumeration(keyList);
        }
    }

    public class PluginInfo {
        public String pluginSettingsName;
        public String pluginName;
        public Class<?> pluginInterface;
        public long pluginId;
        public Plugin plugin;
        public String dirName;
        public Class<Plugin> mainClass;

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
    private ArchitectureLoader() {
    }

    /**
     * Get instance of this class.
     *
     * @return always the same instance (singleton)
     */
    public static ArchitectureLoader getInstance() {
        if (instance == null) {
            instance = new ArchitectureLoader();
        }
        return instance;
    }

    /**
     * Get all file names from a directory ending with specified postfix.
     *
     * @param dirname directory to get files from
     * @param postfix file name postfix, e.g. ".png"
     * @return String array of names
     */
    public static String[] getAllFileNames(String dirname, final String postfix) {
        String[] allNames = null;
        File dir = new File(configurationBaseDirectory + File.separator + dirname);
        if (dir.exists() && dir.isDirectory()) {
            allNames = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(postfix);
                }
            });
            for (int i = 0; i < allNames.length; i++) {
                allNames[i] = allNames[i].substring(0, allNames[i].lastIndexOf(postfix));
            }
        }
        return (allNames == null) ? new String[0] : allNames;
    }

    /**
     * Set base directory for locating configuration files.
     *
     * @param baseDirectory Absolute path of the base directory for the configurations
     */
    public static void setConfigurationBaseDirectory(String baseDirectory) {
        configurationBaseDirectory = baseDirectory;
    }

    /**
     * Method deletes virtual configuration file from filesystem.
     *
     * @param configName Name of the configuration
     * @return true if the operation was successful, false otherwise
     */
    @Override
    public boolean deleteConfiguration(String configName) {
        File file = new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator
                + configName + ".conf");
        if (!file.exists()) {
            LOGGER.error("Could not delete configuration: " + file.getAbsolutePath() + ". The file does not exist.");
            return false;
        }
        try {
            return file.delete();
        } catch(Exception e) {
            LOGGER.error(new StringBuilder().append("Could not delete configuration: ")
                    .append(file.getAbsolutePath()).toString());
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
    @Override
    public boolean renameConfiguration(String newName, String oldName) {
        File oldConfig = new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator + oldName + ".conf");
        if (!oldConfig.exists()) {
            LOGGER.error("Could not rename configuration: " + oldConfig.getAbsolutePath() + ". The file does not exist.");
            return false;
        }
        try {
            return oldConfig.renameTo(new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator
                    + newName + ".conf"));
        } catch(Exception e) {
            LOGGER.error(new StringBuilder().append("Could not rename configuration: ")
                    .append(oldConfig.getAbsolutePath()).append(" to (thesamepath)/").append(newName).toString());
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
     * @throws emustudio.architecture.ReadConfigurationException
     */
    @Override
    public Schema loadSchema(String configName) throws ReadConfigurationException {
        try {
            return new Schema(configName, readConfiguration(configName, true));
        } catch (ReadConfigurationException e) {
            throw e;
        } catch (NullPointerException | NumberFormatException e) {
            throw new ReadConfigurationException("Could not load schema", e);
        }
    }

    /**
     * Method saves abstract schema of some configuration into configuration file.
     *
     * @param schema Schema to save
     * @throws emustudio.architecture.WriteConfigurationException
     */
    @Override
    public void saveSchema(Schema schema) throws WriteConfigurationException {
        schema.save();
        writeConfiguration(schema.getConfigName(), schema.getSettings());
    }

    /**
     * Method reads configuration into Properties object.
     *
     * @param configName
     * @param schema_too whether read schema settings too
     * @return properties object (settings for actual architecture
     * congiguration)
     * @throws ReadConfigurationException if there is some error with configuration reading
     */
    @Override
    public Properties readConfiguration(String configName, boolean schema_too) throws ReadConfigurationException {
        Properties p = new SortedProperties();
        File configFile = new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator + configName
                + ".conf");
        if (!configFile.exists() || !configFile.canRead()) {
            throw new ReadConfigurationException(new StringBuilder().append("Configuration file: ")
                    .append(configFile.getAbsolutePath()).append(" does not exist.").toString());
        }
        try {
            try (FileInputStream fin = new FileInputStream(configFile)) {
                p.load(fin);
            }
        } catch (IOException e) {
            throw new ReadConfigurationException(new StringBuilder().append("Could not read configuration file: ")
                    .append(configFile.getAbsolutePath()).toString(), e);
        }
        if (!p.getProperty("emu8Version").equals("3")
                && !p.getProperty("emu8Version").equals("4")) {
            throw new ReadConfigurationException(new StringBuilder().append("Could not read configuration: ")
                    .append(configName).append(". Unsupported file version.").toString());
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
            for (int i = 0; p.containsKey("device" + i); i++) {
                p.remove("device" + i);
                p.remove("device" + i + ",point.x");
                p.remove("device" + i + ",point.y");
                p.remove("device" + i + ".width");
                p.remove("device" + i + ".height");
            }
            for (int i = 0; p.containsKey("connection" + i + ".junc0"); i++) {
                p.remove("connection" + i + ".junc0");
                p.remove("connection" + i + ".junc1");
                for (int j = 0; p.containsKey("connection" + i + ".point" + j + ".x"); j++) {
                    p.remove("connection" + i + ".point" + j + ".x");
                    p.remove("connection" + i + ".point" + j + ".y");
                }
            }
        }
        return p;
    }

    /**

     * Method save configuration to a file with name configName.
     *
     * @param configName name of configuration
     * @param settings data are taken from
     * @throws WriteConfigurationException if there is some error with configuration writing
     */
    @Override
    public void writeConfiguration(String configName, Properties settings) throws WriteConfigurationException {
        if ((configName == null) || configName.isEmpty()) {
            throw new WriteConfigurationException("Configuration name is not set");
        }
        String dir = configurationBaseDirectory + File.separator + CONFIGS_DIR;
        File dirFile = new File(dir);
        if (!dirFile.exists() || (dirFile.exists() && !dirFile.isDirectory())) {
            if (!dirFile.mkdir()) {
                throw new WriteConfigurationException("Could not create config directory");
            }
        }
        try {
            File configFile = new File(dir + File.separator + configName + ".conf");
            configFile.createNewFile();
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                settings.put("emu8Version", "4");
                String noGUI = null;
                if (settings.containsKey("nogui")) {
                    noGUI = (String)settings.remove("nogui");
                }
                String auto = null;
                if (settings.containsKey("auto")) {
                    auto = (String)settings.remove("auto");
                }

                settings.store(out, configName + " configuration file");

                if (noGUI != null) {
                    settings.put("nogui", noGUI);
                }
                if (auto != null) {
                    settings.put("auto", auto);
                }
            }
        } catch (IOException e) {
            throw new WriteConfigurationException("Could not save configuration.", e);
        }
    }

    private Map<String, PluginInfo> preparePluginsToLoad(Properties settings) {
        Map<String, PluginInfo> pluginsToLoad = new HashMap<>();

        String tmp = settings.getProperty("compiler");
        if (tmp != null) {
            long id = createPluginID();
            LOGGER.debug("Assigned compiler pluginID=" + id);
            pluginsToLoad.put("compiler", new PluginInfo("compiler", COMPILERS_DIR, tmp, Compiler.class, id));
        }
        tmp = settings.getProperty("cpu");
        if (tmp != null) {
            long id = createPluginID();
            LOGGER.debug("Assigned CPU pluginID=" + id);
            pluginsToLoad.put("cpu", new PluginInfo("cpu", CPUS_DIR, tmp, CPU.class, id));
        }
        tmp = settings.getProperty("memory");
        if (tmp != null) {
            long id = createPluginID();
            LOGGER.debug("Assigned memory pluginID=" + id);
            pluginsToLoad.put("memory", new PluginInfo("memory", MEMORIES_DIR, tmp, Memory.class, id));
        }
        for (int i = 0; settings.containsKey("device" + i); i++) {
            tmp = settings.getProperty("device" + i);
            if (tmp != null) {
                long id = createPluginID();
                LOGGER.debug("Assigned device[" + i + "] pluginID=" + id);
                pluginsToLoad.put("device" + i, new PluginInfo("device" + i, DEVICES_DIR, tmp, Device.class, id));
            }
        }
        return pluginsToLoad;
    }

    private void loadPlugins(Map<String, PluginInfo> pluginsToLoad) throws InvalidPasswordException,
            InvalidPluginException, PluginLoadingException, IOException {

        for (PluginInfo plugin : pluginsToLoad.values()) {
            Class<Plugin> mainClass = loadPlugin(plugin.dirName, plugin.pluginName);
            plugin.mainClass = mainClass;
        }
//        if (pluginLoader.canResolveClasses(Main.password)) {
//            // Resolve all plug-in classes
//            pluginLoader.resolveLoadedClasses(Main.password);
//        } else {
//            if (pluginLoader.loadUndoneClasses(Main.password)) {
//                pluginLoader.resolveLoadedClasses(Main.password);
//            } else {
//                throw new PluginLoadingException("Cannot load all classes of plug-ins:"
//                        + Arrays.toString(pluginLoader.getUnloadedClassesList(Main.password)), "[unknown]", null);
//            }
//        }
        LOGGER.info("All plugins are loaded and resolved.");
    }

    private Map<Long, List<Long>> preparePluginConnections(Properties settings, Map<String, PluginInfo> pluginsToLoad) {
        Map<Long, List<Long>> connections = new HashMap<>();
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
            long pID1, pID2;

            PluginInfo pluginInfo = pluginsToLoad.get(j0);
            if (pluginInfo == null) {
                LOGGER.error(new StringBuilder().append("Invalid connection, j0=").append(j0).toString());
                continue; // invalid connection
            }
            pID1 = pluginInfo.pluginId;

            pluginInfo = pluginsToLoad.get(j1);
            if (pluginInfo == null) {
                LOGGER.error(new StringBuilder().append("Invalid connection, j1=").append(j1).toString());
                continue; // invalid connection
            }
            pID2 = pluginInfo.pluginId;

            // the first direction
            if (connections.containsKey(pID1)) {
                connections.get(pID1).add(pID2);
            } else {
                List<Long> ar = new ArrayList<>();
                ar.add(pID2);
                connections.put(pID1, ar);
            }
            if (bidi) {
                // if bidirectional, then also the other connection
                if (connections.containsKey(pID2)) {
                    connections.get(pID2).add(pID1);
                } else {
                    List<Long> ar = new ArrayList<>();
                    ar.add(pID1);
                    connections.put(pID2, ar);
                }
            }
        }
        return connections;
    }

    public ArchitectureManager createArchitecture(String configName)
            throws PluginLoadingException, ReadConfigurationException, PluginInitializationException,
            InvalidPasswordException, InvalidPluginException, IOException {

        Properties settings = readConfiguration(configName, true);
        Map<String, PluginInfo> pluginsToLoad = preparePluginsToLoad(settings);
        loadPlugins(pluginsToLoad);

        Compiler compiler = null;
        CPU cpu = null;
        Memory mem = null;
        List<Device> devList = new ArrayList<>();
        for (PluginInfo plugin : pluginsToLoad.values()) {
            try {
                plugin.plugin = (Plugin)newPlugin(plugin.mainClass, plugin.pluginInterface, plugin.pluginId);
                if (plugin.plugin instanceof Compiler) {
                    compiler = (Compiler)plugin.plugin;
                } else if (plugin.plugin instanceof CPU) {
                    cpu = (CPU)plugin.plugin;
                } else if (plugin.plugin instanceof Memory) {
                    mem = (Memory)plugin.plugin;
                } else if (plugin.plugin instanceof Device) {
                    devList.add((Device)plugin.plugin);
                }
            } catch (ClassNotFoundException e) {
                throw new PluginLoadingException("Plugin " + plugin.pluginName + " cannot be loaded.", plugin.pluginName,
                        plugin.plugin);
            }
        }

        Map<Long, List<Long>> connections = preparePluginConnections(settings, pluginsToLoad);
        Collections.reverse(devList);
        Device[] devices = (Device[]) devList.toArray(new Device[0]);
        Computer computer = new Computer(cpu, mem, compiler, devices, pluginsToLoad.values(), connections);
        ContextPool.getInstance().setComputer(Main.password, computer);
        return new ArchitectureManager(computer, settings, loadSchema(configName), this);
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
     * @param pluginName name of the plugin
     * @return Main class of the plugin. It must be resolved before first use.
     */
    private Class<Plugin> loadPlugin(String dirname, String pluginName) throws InvalidPasswordException, InvalidPluginException, IOException {
        return pluginLoader.loadPlugin(configurationBaseDirectory + File.separator
                + dirname + File.separator + pluginName + ".jar", Main.password);
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
    private Plugin newPlugin(Class<Plugin> mainClass, Class<?> pluginInterface, long pluginID)
            throws ClassNotFoundException {
        if (mainClass == null) {
            throw new ClassNotFoundException("Plug-in main class does not exist");
        }

        if (!PluginLoader.doesImplement(mainClass, pluginInterface)) {
            throw new ClassNotFoundException("Plug-in main class does not implement specified interface");
        }

        // First parameter of constructor is plug-in ID
        Class<?>[] conParameters = {Long.class};

        try {
            Constructor<?> con = mainClass.getDeclaredConstructor(conParameters);
            if (con != null) {
                return (Plugin) con.newInstance(pluginID);
            } else {
                throw new Exception("Constructor of the plug-in is null.");
            }
        } catch (Exception e) {
            throw new ClassNotFoundException("Plug-in main class does not have proper constructor", e);
        }
    }
}
