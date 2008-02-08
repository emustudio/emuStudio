/*
 * IPlugin.java
 *
 * Created on Sobota, 2008, january 19, 11:56
 *
 * KEEP IT SIMPLE STUPID
 * sometimes just: YOU AREN'T GONNA NEED IT
 *
 * Basic, root interface for all plugins
 */

package plugins;

import java.util.*;
import javax.swing.*;


/**
 * Root interface for all plugins, defines
 * description, version, name and copyright information
 * @author vbmacher
 */
public interface IPlugin {
    public String getDescription();
    public String getVersion();
    public String getName();
    public String getCopyright();
    public void destroy();   
}
