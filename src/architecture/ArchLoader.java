/*
 * ArchLoader.java
 *
 * Created on Utorok, 2007, august 7, 11:11
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
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
 */

package architecture;

import architecture.drawing.ConnectionLine;
import architecture.drawing.CpuElement;
import architecture.drawing.DeviceElement;
import architecture.drawing.Element;
import architecture.drawing.MemoryElement;
import architecture.drawing.Schema;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import plugins.compiler.ICompiler;
import plugins.memory.IMemory;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import runtime.StaticDialogs;

/**
 * Class loader for plugins and their resources
 * @author vbmacher
 */
public class ArchLoader extends ClassLoader {
    public final static String configsDir = "config";
    public final static String cpusDir = "cpu";
    public final static String compilersDir = "compilers";
    public final static String memoriesDir = "mem";
    public final static String devicesDir = "devices";
    
    private Hashtable resources;
    
    /** Creates a new instance of ArchLoader */
    public ArchLoader() {     
        resources = new Hashtable();
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
    
    public static boolean deleteConfig(String configName) {
        File f = new File(System.getProperty("user.dir") + 
                File.separator + configsDir + File.separator + configName
                + ".conf");
        if (!f.exists()) return false;
        return f.delete();
    }
    
    public static Schema loadSchema(String configName) {
        try{
            Properties p = readConfig(configName,true);
            
            String compilerName = p.getProperty("compiler");
            int memorySize = Integer.parseInt(p.getProperty("memory.size","0"));
            
            int x = Integer.parseInt(p.getProperty("cpu.point.x","0"));
            int y = Integer.parseInt(p.getProperty("cpu.point.y","0"));
            CpuElement cpu = new CpuElement(x,y,p.getProperty("cpu",
                    "cpu"));
            x = Integer.parseInt(p.getProperty("memory.point.x","0"));
            y = Integer.parseInt(p.getProperty("memory.point.y","0"));
            MemoryElement memory = new MemoryElement(x,y,
                    p.getProperty("memory","memory"));
            
            ArrayList<DeviceElement> devices = new ArrayList<DeviceElement>();
            ArrayList<ConnectionLine> lines = new ArrayList<ConnectionLine>();

            for (int i = 0; p.containsKey("device"+i); i++) {
                x = Integer.parseInt(p.getProperty("device"+i+".point.x",
                        "0"));
                y = Integer.parseInt(p.getProperty("device"+i+".point.y",
                        "0"));
                devices.add(new DeviceElement(x,y,p.getProperty("device"+i,
                        "device"+i)));
            }
            for (int i = 0; p.containsKey("connection"+i+".junc0"); i++) {
                String j0 = p.getProperty("connection"+i
                        +".junc0", "");
                String j1 = p.getProperty("connection"+i
                        +".junc1", "");
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
            return new Schema(cpu,memory,devices,lines,configName,compilerName,
                    memorySize);
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error reading configuration: " + e.toString());
            return null;
        }
    }
    
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
                p.put("memory.size", String.valueOf(s.getMemorySize()));
                p.put("memory.point.x", String.valueOf((int)(mem.getX()
                        + mem.getWidth()/2)));
                p.put("memory.point.y", String.valueOf((int)(mem.getY()
                        + mem.getHeight()/2)));
            }
            ArrayList<DeviceElement> devs = s.getDeviceElements();
            Hashtable devsHash = new Hashtable();
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
     * Method reads configuration into properties
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
            p.load(new FileInputStream(f));
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
     * Method save configuration to file with name configName. 
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
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error writing configuration: " + e.toString());
        }
    }

    /**
     * Method loads architecture configuration from current settings
     * @return a handler of loaded architecture
     */
    public ArchHandler load(String name) {
        try {
            Properties settings = readConfig(name,true);
            if (settings == null) return null;
            
            String comName = settings.getProperty("compiler");
            ICompiler com = (ICompiler)loadPlugin(compilersDir, comName,
                    ICompiler.class.getName());
            String cpuName = settings.getProperty("cpu");
            ICPU cpu = (ICPU)loadPlugin(cpusDir, cpuName, ICPU.class.getName());
            String memName = settings.getProperty("memory");
            IMemory mem = (IMemory)loadPlugin(memoriesDir, memName,
                    IMemory.class.getName());

            ArrayList<IDevice> devs = new ArrayList<IDevice>();
            for (int i = 0; settings.containsKey("device"+i); i++) {
                IDevice dev = (IDevice)loadPlugin(devicesDir,
                        settings.getProperty("device"+i),
                        IDevice.class.getName());
                devs.add(dev);
            }
            
            ArrayList<PluginConnection> lines = new ArrayList<PluginConnection>();
            for (int i = 0; settings.containsKey("connection"+i+".junc0"); i++) {
                String j0 = settings.getProperty("connection"+i
                        +".junc0", "");
                String j1 = settings.getProperty("connection"+i
                        +".junc1", "");
                if (j0.equals("") || j1.equals("")) continue;
                lines.add(new PluginConnection(j0,j1));
            }

            return new ArchHandler(name, com, cpu, mem, 
                    devs.toArray(new IDevice[0]), 
                    lines.toArray(new PluginConnection[0]), settings,
                    loadSchema(name));
        }
        catch (Exception e) {
            String h = e.getLocalizedMessage();
            if (h.equals("")) h = "Unknown error";
            StaticDialogs.showErrorMessage("Error reading plugins: " + h);
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
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error reading plugin: " + filename);
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
