/*
 * ArchitectureLoader.java
 *
 * Created on Utorok, 2007, august 7, 11:11
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This class deals with emulator configuration - loads classes, maps devices, etc.
 *
 * A configuration file is Java properties file. It means it is like Windows INI file without [] classes.
 * Example of using them: http://www.rgagnon.com/javadetails/java-0024.html
 * Standard emulator configuration (e.g. Altair8800.props) file contains following:
 * # and ! are comments
 *
 * # emulator version
 * emu8Version = 2
 * ! CPU name
 * cpu = "Intel8080"
 * compiler = "compilerIntel8080"
 * memory = "nonbanked"
 * memorySize = 16384
 * device0 = "harddisk"
 * device1 = "keyboard"
 * device2 = "screen"
 * ...
 *
 */

package emu8;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import javax.swing.*;

import plugins.memory.IMemory;
import plugins.compiler.*;
import plugins.cpu.ICPU;
import plugins.device.IDevice;

/**
 * Class loader for plugins and their resources
 * @author vbmacher
 */
public class ArchitectureLoader extends ClassLoader {
    public final static String configsDir = "config";
    public final static String cpusDir = "cpu";
    public final static String compilersDir = "compilers";
    public final static String memoriesDir = "mem";
    public final static String devicesDir = "devices";
    
    private Hashtable resources;
    
    /** Creates a new instance of ArchitectureLoader */
    public ArchitectureLoader() {     
        resources = new Hashtable();
    }
    
    /**
     * Method determines all names from a dir that contains postfix.
     * 
     * @param dirname
     * @param postfix
     * @return String array of names
     */
    public String[] getAllNames(String dirname, final String postfix) {
        String[] allNames = null;
        File dir = new File(System.getProperty("user.dir") + File.separator + dirname);
        if (dir.exists() && dir.isDirectory())
            allNames = dir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(postfix);
                }
            });
        return allNames;
    }
    
    /**
     * Method reads configuration into properties
     * 
     * @param configName
     * @return properties object (settings for actual architecture congiguration)
     */
    public Properties readConfig(String configName) {
        try{
            Properties p = new Properties();
            p.load(new FileInputStream(System.getProperty("user.dir") + 
                    File.separator + configsDir + File.separator + configName
                    + ".props"));
            if (!p.getProperty("emu8Version").equals("2")) {
                Main.showErrorMessage("Error reading configuration: " +
                        "unsupported file version");
                return null;
            }
            return p;
        }
        catch (Exception e) {
            Main.showErrorMessage("Error reading configuration: " + e.toString());
            return null;
        }
    }
    
    /** 
     * Method save configuration to file with name configName. 
     *
     * @param configName name of configuration
     * @param settings data are taken from
     */
    public void writeConfig(String configName, Properties settings) {
        try {
            settings.put("emu8Version", "2");
            FileOutputStream out = new FileOutputStream(
                    System.getProperty("user.dir") + File.separator + configsDir
                    + File.separator + configName + ".props");
            settings.store(out, configName + " configuration file");
        }
        catch (Exception e) {
            Main.showErrorMessage("Error writing configuration: " + e.toString());
        }
    }

    /**
     * Method loads architecture configuration from current settings
     * @param settings Properties object
     * @return a handler of loaded architecture
     */
    public ArchitectureHandler load(String name, Properties settings) {
        try {
            String comName = settings.getProperty("compiler");
            ICompiler com = (ICompiler)loadPlugin(compilersDir, comName,
                    ICompiler.class.getName());
            String cpuName = settings.getProperty("cpu");
            ICPU cpu = (ICPU)loadPlugin(cpusDir, cpuName, ICPU.class.getName());
            String memName = settings.getProperty("memory");
            IMemory mem = (IMemory)loadPlugin(memoriesDir, memName,
                    IMemory.class.getName());

            // max. 256 devices
            Vector devs = new Vector();
            for (int i = 0; i < 256; i++)
                if (settings.containsKey("device"+i)) {
                    IDevice dev = (IDevice)loadPlugin(devicesDir,
                            settings.getProperty("device"+i),
                            IDevice.class.getName());
                    devs.add(dev);
                }
            IDevice[] devices = (IDevice[])devs.toArray(new IDevice[0]);
            return new ArchitectureHandler(name, com, cpu, mem, devices, settings);
        }
        catch (Exception e) {
            Main.showErrorMessage("Error reading plugins: " + e.toString());
            return null;
        }
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
    private Object loadPlugin(String dirname, String filename, String interfaceName) {
        ArrayList classes = new ArrayList();
        Hashtable sizes = new Hashtable();
        Vector undone = new Vector();
        
        if (!filename.toLowerCase().endsWith(".jar")) filename += ".jar";
        try {
            // load all classes in jar
            JarFile zf = new JarFile(System.getProperty("user.dir") + File.separator
                    + dirname + File.separator + filename);
            Enumeration e = zf.entries();
            while (e.hasMoreElements()) {
                  JarEntry ze=(JarEntry)e.nextElement();
                  sizes.put(ze.getName(),new Integer((int)ze.getSize()));
            }
            FileInputStream fis = new FileInputStream(zf.getName());
            BufferedInputStream bis = new BufferedInputStream(fis);
            JarInputStream zis = new JarInputStream(bis);
            JarEntry ze = null;

            while ((ze=zis.getNextJarEntry())!=null) {
                if (ze.isDirectory()) continue;
                if (!ze.getName().toLowerCase().endsWith(".class")) {
                    //for windows: "jar:file:/D:/JavaApplicat%20ion12/dist/JavaApplication12.jar!/resources/Find%2024.gif";
                    //for linux:   "jar:file:/home/vbmacher/dev/school%20projects/shit.jar!/resources/Find%2024.gif";
                    String fN = zf.getName().replaceAll("\\\\","/");
                    if (!fN.startsWith("/")) fN = "/" + fN;
                    String URLstr = URLEncoder.encode("jar:file:" + fN
                            + "!/" + ze.getName().replaceAll("\\\\","/"),"UTF-8");
                    URLstr = URLstr.replaceAll("%3A",":").replaceAll("%2F","/").replaceAll("%21","!").replaceAll("\\+","%20");
                    resources.put("/" + ze.getName(), new URL(URLstr));
                    continue;
                }
                // load class data
                int size=(int)ze.getSize();
                if (size == -1)
                    size = ((Integer)sizes.get(ze.getName())).intValue();
                
                byte[] b=new byte[(int)size];
                int rb=0;
                int chunk=0;
                while (((int)size - rb) > 0) {
                    chunk = zis.read(b,rb,(int)size - rb);
                    if (chunk==-1) break;
                    rb+=chunk;
                }
                try {
                    // try load class data
                    Class cl = defineLoadedClass(ze.getName(),b,size,true);
                    classes.add(cl);
                } catch (Exception nf) {
                    undone.addElement(ze.getName());
                }
            }
            // try to load all undone classes
            if (undone.size() > 0) {
                boolean res = loadUndoneClasses(undone,classes,sizes,zf.getName());
                while ((res == true) && (undone.size() > 0))
                    res = loadUndoneClasses(undone,classes,sizes,zf.getName());
                if (undone.size() > 0) {
                    // if a jar file contains some error
                    throw new Exception();
                }
            }
            // find a first class that implements wanted interface
            for (int i = 0; i < classes.size(); i++) {
                Class c = (Class)classes.get(i);
                Class[] intf = c.getInterfaces();
                for (int j = 0; j < intf.length; j++) {
                    if (intf[j].getName().equals(interfaceName))
                        return c.newInstance();
                }
            }
            zf.close();
        }
        catch (NullPointerException e) {}
        catch (Exception e) {
            Main.showErrorMessage("Error reading plugin: " + filename);
        }
        return null;
    }

    protected URL findResource(String name) {
        if (!name.startsWith("/")) name = "/" + name;
        if (resources.containsKey(name)) {
            URL url = (URL)resources.get(name);
            return url;
        }
        else return null;
    }

    public InputStream getResourceAsStream(String name) {
        if (!name.startsWith("/")) name = "/" + name;
        if (resources.containsKey(name)) {
            URL url = (URL)resources.get(name);
            try { return url != null ? url.openStream() : null; }
            catch (Exception e) {}
        }
        return null;
    }
    
    /**
     * This method tries to load all classes that couldnt be loaded
     * before. For example we cant load a class that is extended from
     * yet not loaded class. So this method tries to resolve this
     * 
     * @param undone vector of not loaded classes
     * @param classes where to put loaded classes
     * @param sizes how much size has each class in bytes
     * @param filename name of the plugin (jar file)
     * @return true if at least 1 class was loaded successfully
     */
    private boolean loadUndoneClasses(Vector undone, ArrayList classes,
            Hashtable sizes, String filename) {
        JarEntry ze = null;
        boolean result = false;
        try {
            FileInputStream fis = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            JarInputStream zis = new JarInputStream(bis);
            while ((ze=zis.getNextJarEntry())!=null) {
                if (ze.isDirectory()) continue;
                if (!ze.getName().toLowerCase().endsWith(".class")) continue;
                if (!undone.contains(ze.getName())) continue;
                // load class data
                int size=(int)ze.getSize();
                if (size == -1)
                    size = ((Integer)sizes.get(ze.getName())).intValue();
                byte[] b=new byte[(int)size];
                int rb=0,chunk=0;
                while (((int)size - rb) > 0) {
                    chunk = zis.read(b,rb,(int)size - rb);
                    if (chunk==-1) break;
                    rb+=chunk;
                }
                try {
                    // try load class data
                    Class cl = defineLoadedClass(ze.getName(),b,size,true);
                    classes.add(cl);
                    undone.removeElement(ze.getName());
                    result = true;
                } catch (ClassNotFoundException nf) {}
            }
        } catch(Exception e) {}
        return result;
    }
    
    public synchronized Class defineLoadedClass(String classname,
            byte[] classbytes, int length, boolean resolve) 
            throws ClassNotFoundException {
        if (classname.toLowerCase().endsWith(".class"))
            classname = classname.substring(0,classname.length() - 6);
        classname = classname.replace('/', '.');
        classname = classname.replace(File.separatorChar, '.');
        try {
            Class c = findLoadedClass(classname);
            if (c == null) {
                try { c = findSystemClass(classname); }
                catch (Exception e) {}
            }
            if (c == null) 
                c = defineClass(null, classbytes, 0, length);
            if (resolve && (c != null)) resolveClass(c);
            return c;
        }
        catch (Error err) { throw new ClassNotFoundException(err.getMessage());}
        catch (Exception ex) { throw new ClassNotFoundException(ex.toString());}
    }
    
}
