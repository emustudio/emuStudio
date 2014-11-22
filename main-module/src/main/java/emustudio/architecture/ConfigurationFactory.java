/*
 * Copyright (C) 2014, Peter Jakubčo
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class ConfigurationFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);

    private static String configurationBaseDirectory = System.getProperty("user.dir");

    public final static String CONFIGS_DIR = "config";
    public final static String CPUS_DIR = "cpu";
    public final static String COMPILERS_DIR = "compilers";
    public final static String MEMORIES_DIR = "mem";
    public final static String DEVICES_DIR = "devices";

    public static class SortedProperties extends Properties {

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
    public static void setConfigurationBaseDirectory(String baseDirectory) {
        configurationBaseDirectory = baseDirectory;
    }

    public static String getConfigurationBaseDirectory() {
        return configurationBaseDirectory;
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

    public static boolean delete(String configName) {
        File file = new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator
                + configName + ".conf");
        if (!file.exists()) {
            LOGGER.error("Could not delete configuration: " + file.getAbsolutePath() + ". The file does not exist.");
            return false;
        }
        try {
            return file.delete();
        } catch(Exception e) {
            LOGGER.error("Could not delete configuration: " + file.getAbsolutePath());
        }
        return false;
    }

    public static boolean rename(String newName, String oldName) {
        File oldConfig = new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator + oldName + ".conf");
        if (!oldConfig.exists()) {
            LOGGER.error("Could not rename configuration: " + oldConfig.getAbsolutePath() + ". The file does not exist.");
            return false;
        }
        try {
            return oldConfig.renameTo(new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator
                    + newName + ".conf"));
        } catch(Exception e) {
            LOGGER.error("Could not rename configuration: " + oldConfig.getAbsolutePath() + " to (thesamepath)/"
                    + newName);
        }
        return false;
    }

    public static Configuration read(String configName) throws ReadConfigurationException {
        Properties p = new SortedProperties();
        File configFile = new File(configurationBaseDirectory + File.separator + CONFIGS_DIR + File.separator + configName
                + ".conf");
        if (!configFile.exists() || !configFile.canRead()) {
            throw new ReadConfigurationException("Configuration file: " + configFile.getAbsolutePath()
                    + " does not exist.");
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
