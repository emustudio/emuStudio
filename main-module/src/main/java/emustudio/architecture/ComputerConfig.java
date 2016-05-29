/*
 * Copyright (C) 2014-2016, Peter Jakubčo
 *
 * KISS, YAGNI, DRY
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ComputerConfig {
    private final static String CONFIGS_DIR = "config";
    private final static String CPUS_DIR = "cpu";
    private final static String COMPILERS_DIR = "compilers";
    private final static String MEMORIES_DIR = "mem";
    private final static String DEVICES_DIR = "devices";

    public static final String COMPILER = "compiler";
    public static final String CPU = "cpu";
    public static final String MEMORY = "memory";
    public static final String DEVICE = "device";

    private final static Logger LOGGER = LoggerFactory.getLogger(ComputerConfig.class);
    private final static Map<Class<? extends Plugin>, String> PLUGIN_SUBDIRS = new HashMap<>();

    private static String configBaseDir = System.getProperty("user.dir");

    static {
        PLUGIN_SUBDIRS.put(Compiler.class, COMPILERS_DIR);
        PLUGIN_SUBDIRS.put(CPU.class, CPUS_DIR);
        PLUGIN_SUBDIRS.put(Memory.class, MEMORIES_DIR);
        PLUGIN_SUBDIRS.put(Device.class, DEVICES_DIR);
    }

    private final static class SortedProperties extends Properties {

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

    /**
     * Set base directory for locating configuration files.
     *
     * @param baseDirectory Absolute path of the base directory for the configurations
     */
    static void setConfigBaseDir(String baseDirectory) {
        configBaseDir = baseDirectory;
    }

    public static Path getPluginDir(Class<? extends Plugin> pluginClass) {
        return Paths.get(configBaseDir, PLUGIN_SUBDIRS.get(pluginClass));
    }

    static Path getConfigDir() {
        return Paths.get(configBaseDir, CONFIGS_DIR);
    }

    /**
     * Get all file names from a directory ending with specified postfix.
     *
     * @param dirname directory to get files from
     * @param postfix file name postfix, e.g. ".png"
     * @return String array of names
     */
    static String[] getAllFiles(Path dirname, final String postfix) {
        String[] allNames = null;
        File dir = dirname.toFile();
        if (dir.exists() && dir.isDirectory()) {
            allNames = dir.list((dir1, name) -> name.endsWith(postfix));
            for (int i = 0; i < allNames.length; i++) {
                allNames[i] = allNames[i].substring(0, allNames[i].lastIndexOf(postfix));
            }
        }
        return (allNames == null) ? new String[0] : allNames;
    }

    public static String[] getAllPluginFiles(Class<? extends Plugin> pluginClass, String postfix) {
        return getAllFiles(getPluginDir(pluginClass), postfix);
    }

    public static String[] getAllConfigFiles() {
        return getAllFiles(getConfigDir(), ".conf");
    }

    public static boolean delete(String configName) {
        File file = getConfigDir().resolve(configName + ".conf").toFile();
        if (!file.exists()) {
            LOGGER.error("Could not delete configuration: " + file.getAbsolutePath() + ". The file does not exist.");
            return false;
        }
        try {
            return file.delete();
        } catch(Exception e) {
            LOGGER.error("Could not delete configuration: " + file.getAbsolutePath(), e);
        }
        return false;
    }

    public static boolean rename(String newName, String oldName) {
        File oldConfig = getConfigDir().resolve(oldName + ".conf").toFile();
        if (!oldConfig.exists()) {
            LOGGER.error("Could not rename configuration: " + oldConfig.getAbsolutePath() + ". The file does not exist.");
            return false;
        }
        try {
            return oldConfig.renameTo(getConfigDir().resolve(newName + ".conf").toFile());
        } catch(Exception e) {
            LOGGER.error(
                "Could not rename configuration: {}  to (thesamepath)/{}", oldConfig.getAbsolutePath(), newName, e
            );
        }
        return false;
    }

    public static Configuration read(String configName) throws ReadConfigurationException {
        Properties p = new SortedProperties();
        File configFile = getConfigDir().resolve(configName + ".conf").toFile();
        if (!configFile.exists() || !configFile.canRead()) {
            throw new ReadConfigurationException(
                "Configuration file: " + configFile.getAbsolutePath() + " does not exist."
            );
        }
        try {
            try (FileInputStream fin = new FileInputStream(configFile)) {
                p.load(fin);
            }
        } catch (IOException e) {
            throw new ReadConfigurationException("Could not read configuration file: " + configFile.getAbsolutePath(), e);
        }
        if (!p.getProperty("emu8Version").equals("3")
                && !p.getProperty("emu8Version").equals("4")) {
            throw new ReadConfigurationException("Could not read configuration: " + configName + ". Unsupported file version.");
        }
        return new ConfigurationImpl(configName, p);
    }


}
