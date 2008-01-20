/*
 * emuConfiguration.java
 *
 * Created on Utorok, 2007, august 7, 11:11
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 * Needs reorganization
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
 * device1 = "harddisk"
 * device2 = "keyboard"
 * device3 = "screen"
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
 *
 * @author vbmacher
 */
public class emuConfiguration extends ClassLoader {
    public final String configsDir = "config";
    public final String cpusDir = "cpu";
    public final String compilersDir = "compilers";
    public final String memoriesDir = "mem";
    public final String devicesDir = "devices";
    
    private Hashtable resources;
    
    // actual configuration
    public String nowName;
    public String nowCPU;
    public String nowCompiler;
    public String nowMemory;
    public int nowMemorySize;
    public ArrayList nowDevices;
    
    // actual loaded classes
    public ICompiler cCompiler;
    public ICPU cCPU;
    public ArrayList cDevices;
    public IMemory cMemory;
    
    
    /** Creates a new instance of emuConfiguration */
    public emuConfiguration() {     
        nowDevices = new ArrayList();
        nowName = "";
        nowCPU = "";
        nowCompiler = "";
        nowMemory = "";
        nowMemorySize = 0;
        cDevices = new ArrayList();
        resources = new Hashtable();
    }
    
    private class filFilter implements java.io.FilenameFilter {
        String pf;
        public filFilter(String p) { pf = p; }
        public boolean accept(File dir, String name) {
            if (name.toLowerCase().endsWith(pf.toLowerCase())) return true;
            return false;
        }
    }
    
    public String[] getAllNames(String dirname, String postfix) {
        String[] allNames = null;
        File dir = new File(System.getProperty("user.dir") + File.separator + dirname);
        if (dir.exists() && dir.isDirectory())
            allNames = dir.list(new filFilter(postfix));
        return allNames;
    }
    
    // reads configuration into public variables prefixed with "now"
    public boolean readConfig(String configName) {
        try{
            Properties p = new Properties();
            p.load(new FileInputStream(System.getProperty("user.dir") + 
                    File.separator + configsDir + File.separator + configName
                    + ".props"));
            if (!p.getProperty("emu8Version").equals("2")) {
                Main.showErrorMessage("Error reading configuration: " +
                        "unsupported file version");
                return false;
            }
            nowName = configName;
            nowCPU = p.getProperty("cpu");
            nowCompiler = p.getProperty("compiler");
            nowMemory = p.getProperty("memory");
            nowMemorySize = Integer.valueOf(p.getProperty("memorySize"));
            
            // max. 256 devices at all
            nowDevices.clear();
            for (int i = 1; i <= 256; i++)
                if (p.containsKey("device"+i))
                    nowDevices.add(p.getProperty("device"+i));
        }
        catch (Exception e) {
            Main.showErrorMessage("Error reading configuration: " + e.toString());
            return false;
        }
        return true;
    }
    
    // save configuration to file with name configName. Data take from public
    // vars prefixed with "now"
    public boolean writeConfig(String configName) {
        try {
            Properties p = new Properties();
            p.put("emu8Version", "2");
            p.put("cpu",nowCPU);
            p.put("compiler",nowCompiler);
            p.put("memory",nowMemory);
            p.put("memorySize", String.valueOf(nowMemorySize));
            for (int i=0; i < nowDevices.size(); i++)
                p.put("device"+String.valueOf(i+1),(String)nowDevices.get(i));
            FileOutputStream out = new FileOutputStream(
                    System.getProperty("user.dir") + File.separator + configsDir
                    + File.separator + configName + ".props");
            p.store(out, nowName + " configuration file");
        }
        catch (Exception e) {
            Main.showErrorMessage("Error writing configuration: " + e.toString());
            return false;
        }
        return true;
    }

    // loads configuration with parameters defined in variables starting with "now"
    public void loadConfig() {
        try {
            cCompiler = (ICompiler)loadPlugin(compilersDir, nowCompiler,
                    ICompiler.class.getName());
            cCPU = (ICPU)loadPlugin(cpusDir, nowCPU, ICPU.class.getName());
            cMemory = (IMemory)loadPlugin(memoriesDir, nowMemory,
                    IMemory.class.getName());
            cMemory.init(nowMemorySize);
            cCPU.init(cMemory);
            for (int i=0; i < nowDevices.size(); i++)
                cDevices.add(loadPlugin(devicesDir,(String)nowDevices.get(i),
                        IDevice.class.getName()));
            for (int i=0;i < cDevices.size(); i++)
                ((IDevice)cDevices.get(i)).init(cCPU, cMemory);
        }
        catch (Exception e) {
            Main.showErrorMessage("Error reading plugins: " + e.toString());
        }
    }
    
    // return new instance of a plugin from jar file
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
                    //"jar:file:/D:/JavaApplicat%20ion12/dist/JavaApplication12.jar!/resources/Find%2024.gif";
                    String URLstr = URLEncoder.encode("jar:file:/" + zf.getName().replaceAll("\\\\","/")
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
           e.printStackTrace();
        }
        return null;
    }

    @Override
    protected URL findResource(String name) {
        if (!name.startsWith("/")) name = "/" + name;
        if (resources.containsKey(name)) {
            URL url = (URL)resources.get(name);
            return url;
        }
        else return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (!name.startsWith("/")) name = "/" + name;
        if (resources.containsKey(name)) {
            URL url = (URL)resources.get(name);
            try { return url != null ? url.openStream() : null; }
            catch (Exception e) {}
        }
        return null;
    }
    
    // this method tries to load all classes that couldnt be loaded
    // before. For example we cant load a class that is extended from
    // yet not loaded class. So this method tries to resolve this
    // return true if at least 1 class was loaded successfully
    // params: classes is where to put loaded classes
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
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
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
