/*
 * ArchLoader.java
 *
 * Created on Utorok, 2007, august 7, 11:11
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import emustudio.architecture.drawing.ConnectionLine;
import emustudio.architecture.drawing.CpuElement;
import emustudio.architecture.drawing.DeviceElement;
import emustudio.architecture.drawing.Element;
import emustudio.architecture.drawing.MemoryElement;
import emustudio.architecture.drawing.Schema;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import plugins.IPlugin;
import plugins.compiler.ICompiler;
import plugins.memory.IMemory;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import runtime.StaticDialogs;

/**
 * Class loader for plugins and their resources.
 *
 * This class deals with emulator configuration - loads classes, maps devices,
 * etc
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
 * emu8Version = 3
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
    public final static String configsDir = "config";
    public final static String cpusDir = "cpu";
    public final static String compilersDir = "compilers";
    public final static String memoriesDir = "mem";
    public final static String devicesDir = "devices";

    private static long nextPluginID = 0;
    
    /** 
     * This forbids of creating the instance of this class.
     */
    private ArchLoader() {
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
        File f = new File(System.getProperty("user.dir") + 
                File.separator + configsDir + File.separator + configName
                + ".conf");
        if (!f.exists())
            return false;
        try {
            return f.delete();
        } catch(Exception e) {
            e.printStackTrace();
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
                File.separator + configsDir + File.separator + oldName
                + ".conf");
        if (!f.exists())
            return false;
        try {
            return f.renameTo(new File(newName));
        } catch(Exception e) {
            e.printStackTrace();
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
    public static Schema loadSchema(String configName) {
        try{
            Properties p = readConfig(configName,true);
            
            String compilerName = p.getProperty("compiler");
            
            int x = Integer.parseInt(p.getProperty("cpu.point.x","0"));
            int y = Integer.parseInt(p.getProperty("cpu.point.y","0"));
            CpuElement cpu = new CpuElement(x,y,p.getProperty("cpu","cpu"));
            x = Integer.parseInt(p.getProperty("memory.point.x","0"));
            y = Integer.parseInt(p.getProperty("memory.point.y","0"));
            MemoryElement memory = new MemoryElement(x,y,p.getProperty("memory","memory"));
            
            ArrayList<DeviceElement> devices = new ArrayList<DeviceElement>();
            ArrayList<ConnectionLine> lines = new ArrayList<ConnectionLine>();

            for (int i = 0; p.containsKey("device"+i); i++) {
                x = Integer.parseInt(p.getProperty("device"+i+".point.x","0"));
                y = Integer.parseInt(p.getProperty("device"+i+".point.y","0"));
                devices.add(new DeviceElement(x,y,p.getProperty("device"+i,"device"+i)));
            }
            for (int i = 0; p.containsKey("connection"+i+".junc0"); i++) {
                String j0 = p.getProperty("connection"+i+".junc0", "");
                String j1 = p.getProperty("connection"+i+".junc1", "");
                if (j0.equals("") || j1.equals("")) continue;
                
                Element e1=null,e2=null;
                if (j0.equals("cpu")) e1 = cpu;
                else if (j0.equals("memory")) e1 = memory;
                else if (j0.startsWith("device")) {
                    int index = Integer.parseInt(j0.substring(6));
                    e1 = devices.get(index);
                }
                if (j1.equals("cpu")) e2 = cpu;
                else if (j1.equals("memory")) e2 = memory;
                else if (j1.startsWith("device")) {
                    int index = Integer.parseInt(j1.substring(6));
                    e2 = devices.get(index);
                }
                ConnectionLine lin = new ConnectionLine(e1,e2,null);
                for (int j = 0; p.containsKey("connection"+i+".point"+j+".x"); j++) {
                    x = Integer.parseInt(p.getProperty("connection"
                            + i + ".point" + j + ".x","0"));
                    y = Integer.parseInt(p.getProperty("connection"
                            + i + ".point" + j + ".y","0"));
                    lin.addPoint(new Point(x,y));
                }
                lines.add(lin);
            }
            return new Schema(cpu,memory,devices,lines,configName,compilerName);
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error reading configuration: " + e.toString());
            return null;
        }
    }
    
    /**
     * Method saves abstract schema of some configuration into configuration
     * file. 
     * @param s Schema to save
     */
    public static void saveSchema(Schema s) {
        try {
            Properties p = readConfig(s.getConfigName(),false);
            if (p == null) p = new Properties();
            p.put("compiler", s.getCompilerName());
            // cpu
            CpuElement cpu = s.getCpuElement();
            if (cpu != null) {
                p.put("cpu", cpu.getDetails());
                p.put("cpu.point.x", String.valueOf((int)(cpu.getX()
                        + cpu.getWidth()/2)));
                p.put("cpu.point.y", String.valueOf((int)(cpu.getY()
                        + cpu.getHeight()/2)));
            }
            MemoryElement mem = s.getMemoryElement();
            if (mem != null) {
                p.put("memory", mem.getDetails());
                p.put("memory.point.x", String.valueOf((int)(mem.getX()
                        + mem.getWidth()/2)));
                p.put("memory.point.y", String.valueOf((int)(mem.getY()
                        + mem.getHeight()/2)));
            }
            ArrayList<DeviceElement> devs = s.getDeviceElements();
            Hashtable<DeviceElement, Object> devsHash = new Hashtable<DeviceElement, Object>();
            for (int i = 0; i < devs.size(); i++) {
                DeviceElement dev = devs.get(i);
                devsHash.put(dev, "device" + i);
                p.put("device"+i, dev.getDetails());
                p.put("device"+i+".point.x", String.valueOf((int)(dev.getX()
                        + dev.getWidth()/2)));
                p.put("device"+i+".point.y", String.valueOf((int)(dev.getY()
                        + dev.getHeight()/2)));
            }
            ArrayList<ConnectionLine> lines = s.getConnectionLines();
            for (int i = 0; i < lines.size(); i++) {
                ConnectionLine line = lines.get(i);
                Element e = line.getJunc0();
                if (e instanceof CpuElement) 
                    p.put("connection"+i+".junc0", "cpu");
                else if (e instanceof MemoryElement) 
                    p.put("connection"+i+".junc0", "memory");
                else if (e instanceof DeviceElement)
                    p.put("connection"+i+".junc0", devsHash.get((DeviceElement)e));
               
                e = line.getJunc1();
                if (e instanceof CpuElement) 
                    p.put("connection"+i+".junc1", "cpu");
                else if (e instanceof MemoryElement) 
                    p.put("connection"+i+".junc1", "memory");
                else if (e instanceof DeviceElement)
                    p.put("connection"+i+".junc1", devsHash.get((DeviceElement)e));
                ArrayList<Point> points = line.getPoints();
                for (int j = 0; j < points.size(); j++) {
                    Point po = points.get(j);
                    p.put("connection"+i+".point"+j+".x", String.valueOf((int)po.getX()));
                    p.put("connection"+i+".point"+j+".y", String.valueOf((int)po.getY()));
                }
            }
            writeConfig(s.getConfigName(),p);
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
                    File.separator + configsDir + File.separator + configName
                    + ".conf");
            if (!f.exists()) return null;
            FileInputStream fin = new FileInputStream(f);
            p.load(fin);
            fin.close();
            if (!p.getProperty("emu8Version").equals("3")) {
                StaticDialogs.showErrorMessage("Error reading configuration: " +
                        "unsupported file version");
                return null;
            }
            if (!schema_too) {
                p.remove("cpu");
                p.remove("cpu.point.x");
                p.remove("cpu.point.y");
                p.remove("memory");
                p.remove("memory.point.x");
                p.remove("memory.point.y");
                for (int i = 0; p.containsKey("device"+i); i++) {
                    p.remove("device"+i);
                    p.remove("device"+i+",point.x");
                    p.remove("device"+i+",point.y");
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
            File f = new File(System.getProperty("user.dir") + File.separator
                    + configsDir + File.separator + configName + ".conf");
            f.createNewFile();
            FileOutputStream out = new FileOutputStream(f);
            settings.put("emu8Version", "3");
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
     * @return instance of virtual architecture
     */
    public static ArchHandler load(String name, boolean verbose) {
        try {
            Properties settings = readConfig(name,true);
            if (settings == null) return null;
            
            Hashtable<IPlugin, Long> pluginsReverse = new Hashtable<IPlugin, Long>();
            Hashtable<Long, IPlugin> plugins = new Hashtable<Long, IPlugin>();
            Hashtable<Long,String> pluginNames = new Hashtable<Long, String>();

            // load compiler
            String comName = settings.getProperty("compiler");
            long id;
            ICompiler com = null;

            if (comName != null) {
                // the compiler is defined in the config file
                com = (ICompiler)loadPlugin(compilersDir,
                        comName,ICompiler.class);
                if (com == null)
                    throw new IllegalArgumentException("Compiler '" + comName
                            + "' cannot be loaded.");
                id = createPluginID();
                pluginsReverse.put(com, id);
                plugins.put(id, com);
                pluginNames.put(id, "compiler");
            }
            // load cpu
            String cpuName = settings.getProperty("cpu");
            if (cpuName == null)
                throw new IllegalArgumentException("CPU is not defined.");
            ICPU cpu = (ICPU)loadPlugin(cpusDir, cpuName, ICPU.class);
            if (cpu == null)
                throw new IllegalArgumentException("CPU '" + cpuName
                        + "' cannot be loaded.");
            id = createPluginID();
            pluginsReverse.put(cpu, id);
            plugins.put(id, cpu);
            pluginNames.put(id, "cpu");
            
            // load memory
            IMemory mem = null;
            String memName = settings.getProperty("memory");
            if (memName != null) {
                mem = (IMemory)loadPlugin(memoriesDir, memName,IMemory.class);
                if (mem == null)
                    throw new IllegalArgumentException("Memory '" + memName
                            + "' cannot be loaded.");
                id = createPluginID();
                pluginsReverse.put(mem, id);
                plugins.put(id, mem);
                pluginNames.put(id, "memory");
            }

            // load devices
            Hashtable<IDevice,String> devNames = new Hashtable<IDevice,String>();
            for (int i = 0; settings.containsKey("device"+i); i++) {
            	String devName = settings.getProperty("device"+i);
                IDevice dev = (IDevice)loadPlugin(devicesDir, devName,IDevice.class);
                if (dev != null) {
                    id = createPluginID();
                    pluginsReverse.put(dev, id);
                    plugins.put(id, dev);
                    pluginNames.put(id,"device" + i);
                    devNames.put(dev,"device" + i);
                } else
                    throw new IllegalArgumentException("Device '" + devName
                            + "' cannot be loaded.");
            }

            // load connections
            Hashtable<Long,ArrayList<Long>> connections =
                    new Hashtable<Long,ArrayList<Long>>();
            IDevice[] tmpDevices = devNames.keySet().toArray(new IDevice[0]);
            for (int i = 0; settings.containsKey("connection"+i+".junc0"); i++) {
            	// get i-th connection from settings
                String j0 = settings.getProperty("connection"+i+".junc0", "");
                String j1 = settings.getProperty("connection"+i+".junc1", "");
                
                if (j0.equals("") || j1.equals(""))
                    continue;

                // map the connection elements to plug-ins: p1 and p2
                IPlugin p1 = null,p2 = null;

                if (j0.equals("cpu"))
                    p1 = cpu;
                else if (j0.equals("memory"))
                    p1 = mem;
                else if (j0.startsWith("device")) {
                    int index = Integer.parseInt(j0.substring(6));
                    p1 = tmpDevices[index];
                }
                if (j1.equals("cpu"))
                    p2 = cpu;
                else if (j1.equals("memory"))
                    p2 = mem;
                else if (j1.startsWith("device")) {
                    int index = Integer.parseInt(j1.substring(6));
                    p2 = tmpDevices[index];
                }

                // note the connection: p1 -> p2  (p1 wants to use p2)
                long pID1 = pluginsReverse.get(p1);
                long pID2 = pluginsReverse.get(p2);

                if (connections.containsKey(pID1))
                    connections.get(pID1).add(pID2);
                else {
                    ArrayList<Long> ar = new ArrayList<Long>();
                    ar.add(pID2);
                    connections.put(pID1, ar);
                }
            }
            Computer arch = new Computer(cpu, mem, com, tmpDevices,
                    plugins, pluginsReverse, connections);
            return new ArchHandler(name, arch, settings, loadSchema(name),
                    pluginNames, verbose);
        }
        catch (IllegalArgumentException e) {
            StaticDialogs.showMessage(e.getMessage(), "Error reading plugins");
        } finally {
            return null;
        }
    }
    
    /**
     * Method compute a hash for a plugin identification for one runtime
     * session. The hash is made from <code>System.nanoTime()</code> and
     * from <code>Math.random()</code>.
     * 
     * @return hash for an identification of the plugin
     */
    private static long createPluginID() {
    	return nextPluginID++;
    }
    
    /**
     * Method return new instance of a plugin from jar file
     * 
     * @param dirname type of a plugin (compiler, cpu, memory, devices)
     * @param filename name of the plugin
     * @param interfaceName name of a interface that some class in the plugin
     *        has to implement
     * @return instance object of loaded plugin
     */
    private static Object loadPlugin(String dirname, String filename, Class<?> interfaceName) {
        try {
            ArrayList<Class<?>> classes = runtime.Loader.getInstance().loadJAR(
                System.getProperty("user.dir") + File.separator + dirname
                + File.separator + filename);
            if (classes == null)
                throw new Exception();
        
            // find a first class that implements wanted interface
            Class<?>[] conParameters = {};
            for (int i = 0; i < classes.size(); i++) {
                Class<?> c = (Class<?>)classes.get(i);
                Class<?>[] intf = c.getInterfaces();
                for (int j = 0; j < intf.length; j++) {
                    if (intf[j].equals(interfaceName)) {
                        Constructor<?> con = c.getDeclaredConstructor(conParameters);
                        return con.newInstance();
                    }
                }
            }
        } catch (Exception e) {}
        finally {
            return null;
        }
    }
    
}
