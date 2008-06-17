/*
 * HighlightStyle.java
 *
 * Created on 7.2.2008, 9:53:55
 * hold to: KISS, YAGNI
 *
 */

package gui.syntaxHighlighting;

import java.awt.Color;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author vbmacher
 */
public class HighlightStyle extends SimpleAttributeSet {
    public HighlightStyle(boolean italic, boolean bold, Color color) {
        StyleConstants.setFontFamily(this, "Monospaced");
        StyleConstants.setFontSize(this, 12);
        StyleConstants.setBackground(this, Color.white);
        StyleConstants.setItalic(this, italic);
        StyleConstants.setForeground(this, color);
        StyleConstants.setBold(this, bold);
    }
}
