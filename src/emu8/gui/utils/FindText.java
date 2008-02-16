/*
 * FindText.java
 *
 * Created on 15.2.2008, 11:40:01
 * hold to: KISS, YAGNI
 * 
 * Class for generally matching/replacing a text with pattern
 *
 */

package emu8.gui.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author vbmacher
 */
public class FindText {
    public static final int CASE_SENSITIVE = 1;
    public static final int WHOLE_WORDS = 2;
    
    public static final int DIRECTION_TO_END = 0;
    public static final int DIRECTION_TO_START = 1;
    public static final int DIRECTION_ALL = 2;

    public static FindText instance;
    
    public FindText() {
        FindText.instance = this;
    }
    
    public static FindText getThis() { return instance; }

    private Matcher matcher = null;
    private Pattern pattern = null;
    
    public String replacement = "";
    private StringBuffer sb = new StringBuffer();

    private int direction = 0;
    private byte params = 0;

    // start/end of last match
    private int startM = -1;
    private int endM = -1;
    
    /* parameter setting/getting */
    public void createPattern(String findExpr) throws PatternSyntaxException {
        int flags = 0;
        String str = findExpr;
        if ((params & CASE_SENSITIVE) == 0)
            flags |= Pattern.CASE_INSENSITIVE;
        
        if ((params & WHOLE_WORDS) != 0) str = "\\b(" + str + ")\\b";
        this.pattern = Pattern.compile(str, flags);
        matcher = null;
    }
    
    public String getFindExpr() {
        if (matcher == null) return null;
        return matcher.pattern().toString();
    }
    
    public int getDirection() { return direction; }
    public void setDirection(int dir) { direction = dir; }

    public boolean isCaseSensitive() { 
        return ((params & CASE_SENSITIVE) != 0); 
    }
    public boolean isWholeWords() { 
        return ((params & WHOLE_WORDS) != 0) ;
    }
    public void setParams(byte par) { params = par; }
    
    /* searching/replacing */
    
    public boolean findNext(String text, int curPos, int endPos) throws NullPointerException {
        if ((matcher == null) && (pattern == null))
            throw new NullPointerException("matcher can't be null, use dialog");
        else if (matcher == null)
            matcher = pattern.matcher("");
        
        boolean match = false;
        String txt = text.replaceAll("\n\r", "\n").replaceAll("\r\n", "\n");
        
        matcher.reset(txt);
        matcher.useTransparentBounds(false);
        if (direction == FindText.DIRECTION_TO_END) {
            matcher.region(curPos, endPos);
            match = matcher.find();
            if (match) {
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            endM = 0;
            match = false;
            while (matcher.find(endM)) {
                if (matcher.end() >= curPos) break;
                match = true;
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            if (!matcher.find(curPos)) match = matcher.find(0);
            if (match) {
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        return match;
    }
    
    public int getMatchStart() { return startM; }
    public int getMatchEnd() { return endM; }
    
    public boolean replaceNext(String text, int curPos, int endPos)
            throws NullPointerException {
        if ((matcher == null) && (pattern == null))
            throw new NullPointerException("matcher can't be null, use dialog");
        else if (matcher == null)
            matcher = pattern.matcher("");

        boolean match = false;
        String txt = text.replaceAll("\n\r", "\n").replaceAll("\r\n", "\n");

        matcher.reset(txt);
        matcher.useTransparentBounds(false);
        if (direction == FindText.DIRECTION_TO_END) {
            matcher.region(curPos, endPos);
            match = matcher.find();
            if (match) {
                matcher.appendReplacement(sb, replacement);
                matcher.appendTail(sb);
            }
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            match = false;
            while (matcher.find(endM)) {
                if (matcher.end() >= curPos) break;
                startM = matcher.start()-1;
                endM = matcher.end();
                match = true;
            }
            if (startM < 0) startM = 0;
            matcher.region(startM, curPos); // here is only one match
            match = matcher.find(startM);
            if (match) {
                matcher.appendReplacement(sb, replacement);
                matcher.appendTail(sb);
            }
        }
        else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            if (!matcher.find(curPos)) match = matcher.find(0);
            if (match) matcher.appendReplacement(sb, replacement);
            matcher.appendTail(sb);
        }
        return match;
    }
    
    public boolean replaceAll(String text, int curPos, int endPos)
            throws NullPointerException {
        if ((matcher == null) && (pattern == null))
            throw new NullPointerException("matcher can't be null, use dialog");
        else if (matcher == null)
            matcher = pattern.matcher("");

        boolean match = false;
        String txt = text.replaceAll("\n\r", "\n").replaceAll("\r\n", "\n");
        
        matcher.reset(txt);
        matcher.useTransparentBounds(false);
        if (direction == FindText.DIRECTION_TO_END) {
            matcher.region(curPos, endPos);
            while (matcher.find()) {
                match = true;
                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            match = false;
            while (matcher.find())
                matcher.appendReplacement(sb, replacement);
            matcher.appendTail(sb);
        }
        else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            sb.append(matcher.replaceAll(replacement));
        }
        return match;        
    }
    
    public String getReplacedString() { return sb.toString(); }
}
