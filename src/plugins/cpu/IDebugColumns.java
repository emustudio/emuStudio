/*
 * IDebugColumns.java
 *
 * Created on Piatok, 2007, október 26, 10:40
 *
 * KEEP IT SIMPLE STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This interface define informations about one column for debug window
 */

package plugins.cpu;

/**
 *
 * @author vbmacher
 */
public interface IDebugColumns {
    public Class getType();
    public String getName();
    public boolean isEditable();
}
