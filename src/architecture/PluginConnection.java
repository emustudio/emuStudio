/*
 * PluginConnection.java
 *
 * Created on 9.7.2008, 10:11:55
 * hold to: KISS, YAGNI
 *
 */

package architecture;

/**
 *
 * @author vbmacher
 */
public class PluginConnection {
    private String junc0;
    private String junc1;
    public PluginConnection(String junc0, String junc1) {
        this.junc0 = junc0;
        this.junc1 = junc1;
    }
    public String getJunc0() { return junc0; }
    public String getJunc1() { return junc1; }
    public boolean contains(String type) {
        if (junc0.equals(type) || junc1.equals(type))
            return true;
        return false;
    }
}
