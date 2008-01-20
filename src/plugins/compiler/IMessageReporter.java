/*
 * IMessageReporter.java
 *
 * Created on Pondelok, 2007, september 17, 11:46
 *
 * KEEP IT SIMPLE STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package plugins.compiler;

/**
 *
 * @author vbmacher
 */
public interface IMessageReporter {
    public void reportMessage(String message);
    public void reportMessage(String location, String message);
}
