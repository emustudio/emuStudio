/*
 * ICompiler.java
 *
 * Created on Nedeľa, 2007, august 12, 18:48
 *
 * KEEP IT SIMPLE STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */
package plugins.compiler;

import plugins.IPlugin;
/**
 *
 * @author vbmacher
 */
public interface ICompiler extends IPlugin {
    public String getDescription();
    public String getVersion();
    public String getName();
    public String getCopyright();

    public ILexer getLexer(java.io.Reader in, IMessageReporter reporter);
    public boolean compile(String filename);
}
