/*
 * ITokenColor.java
 *
 * Created on Streda, 2007, september 5, 10:43
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package plugins.compiler;

import java.awt.Color;

/**
 *
 * @author vbmacher
 */
public class ITokenColor {
    public final static Color COMMENT = new Color(0,128,0); // green 
    public final static Color RESERVED = Color.BLACK;
    public final static Color IDENTIFIER = Color.BLACK;
    public final static Color LITERAL = new Color(0,0,128); // blue
    public final static Color LABEL = new Color(0,128,128); // weird blue
    public final static Color REGISTER = new Color(128,0,0);// brown
    
    public final static Color PREPROCESSOR = new Color(80,80,80); // weird gray
    public final static Color SEPARATOR = Color.BLACK;
    public final static Color OPERATOR = new Color(0,0,128);
    public final static Color ERROR = Color.RED;
}
