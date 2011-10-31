/*
 * FindText.java
 *
 * Created on 15.2.2008, 11:40:01
 * hold to: KISS, YAGNI
 * 
 * Class for generally matching/replacing a text with pattern
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;

/**
 * Singleton for finding text.
 * 
 * @author vbmacher
 */
public class FindText {

    /**
     * Flag for case-sentisiveness for text searching.
     */
    public static final int CASE_SENSITIVE = 1;

    /**
     * Flag for whole words only in text searching.
     */
    public static final int WHOLE_WORDS = 2;

    /**
     * Flag for text searching only to end of the document.
     */
    public static final int DIRECTION_TO_END = 0;

    /**
     * Flag for text searching only from start to actual position in the
     * document.
     */
    public static final int DIRECTION_TO_START = 1;

    /**
     * Flag for text searching in whole document.
     */
    public static final int DIRECTION_ALL = 2;
    
    private static FindText instance = null;
    private Matcher matcher = null;
    private Pattern pattern = null;

    /**
     * Text replacement for "find and replace" action
     */
    public String replacement = "";

    private int direction = 0;
    private byte params = 0;
    // start/end of last match
    private int startM = -1;
    private int endM = -1;

    /**
     * Private constructor
     */
    private FindText() {
    }

    /**
     * Get a singleton instance of this class.
     * 
     * @return Singleton instance
     */
    public static FindText getInstance() {
        if (instance == null) {
            instance = new FindText();
        }
        return instance;
    }

    /**
     * Parameter setting.
     * 
     * @param findExpr new find pattern
     * @throws PatternSyntaxException if the pattern syntax is wrong (regular
     * expression).
     */
    public void createPattern(String findExpr) throws PatternSyntaxException {
        int flags = 0;
        String str = findExpr;
        if ((params & CASE_SENSITIVE) == 0) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        if ((params & WHOLE_WORDS) != 0) {
            str = "\\b(" + str + ")\\b";
        }
        this.pattern = Pattern.compile(str, flags);
        matcher = null;
    }

    /**
     * Get find expression (text to find).
     * 
     * @return find expression
     */
    public String getFindExpr() {
        if (matcher == null) {
            return null;
        }
        return matcher.pattern().toString();
    }

    /**
     * Get direction of searching in a document.
     * @return direction flag
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Set direction of searching in a document.
     * @param dir the direction flag
     */
    public void setDirection(int dir) {
        direction = dir;
    }

    /**
     * Determine if text searching is case sensitive.
     * 
     * @return true if yes, false if not.
     */
    public boolean isCaseSensitive() {
        return ((params & CASE_SENSITIVE) != 0);
    }

    /**
     * Determine if text searching searches for whole words only.
     * 
     * @return true if yes, false if not.
     */
    public boolean isWholeWords() {
        return ((params & WHOLE_WORDS) != 0);
    }

    /**
     * Set search parameters.
     * 
     * @param par searching flags (OR-ed together)
     */
    public void setParams(byte par) {
        params = par;
    }

    /**
     * Searching or replaces the text on specified position in document.
     * 
     * @param text The document where to perform the search
     * @param curPos the starting position
     * @param endPos the ending position
     * @return false if text was not found. Otherwise returns true.
     * @throws NullPointerException if the find pattern or text is null
     */
    public boolean findNext(String text, int curPos, int endPos) throws NullPointerException {
        if ((matcher == null) && (pattern == null)) {
            throw new NullPointerException("matcher can't be null, use dialog");
        } else if (matcher == null) {
            matcher = pattern.matcher("");
        }

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
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            endM = 0;
            match = false;
            while (matcher.find(endM)) {
                if (matcher.end() >= curPos) {
                    break;
                }
                match = true;
                startM = matcher.start();
                endM = matcher.end();
            }
        } else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            if (!matcher.find(curPos)) {
                match = matcher.find(0);
            }
            if (match) {
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        return match;
    }

    /**
     * If text searching matches a string, this method would get the starting
     * position of the string within the document.
     * 
     * @return starting position of matched string in the document.
     */
    public int getMatchStart() {
        return startM;
    }

    /**
     * If text searching matches a string, this method would get the ending
     * position of the string within the document.
     * 
     * @return ending position of matched string in the document.
     */
    public int getMatchEnd() {
        return endM;
    }

    /**
     * Method replaces next found occurence by find replacement string.
     * 
     * @param textPane the document where to perform search
     * @return false if replacement is null or if text was not found. Otherwise
     *  it returns true.
     * @throws NullPointerException if the find pattern is null
     */
    public boolean replaceNext(JTextPane textPane)
            throws NullPointerException {
        if ((matcher == null) && (pattern == null)) {
            throw new NullPointerException("matcher can't be null, use dialog");
        } else if (matcher == null) {
            matcher = pattern.matcher("");
        }
        if (replacement == null) {
            return false;
        }

        boolean match = false;
        int curPos = textPane.getCaretPosition();
        int endPos = textPane.getDocument().getEndPosition().getOffset() - 1;
        String txt = textPane.getText();

        matcher.reset(txt);
        matcher.useTransparentBounds(false);
        if (direction == FindText.DIRECTION_TO_END) {
            matcher.region(curPos, endPos);
            match = matcher.find();
            if (match) {
                try {
                    textPane.getDocument().remove(matcher.start(),
                            matcher.end() - matcher.start());
                    textPane.getDocument().insertString(matcher.start(),
                            replacement, null);
                    textPane.setCaretPosition(matcher.start() + replacement.length());
                } catch (BadLocationException e) {
                }
            }
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            match = false;
            while (matcher.find(endM)) {
                if (matcher.end() >= curPos) {
                    break;
                }
                startM = matcher.start() - 1;
                endM = matcher.end();
                match = true;
            }
            if (startM < 0) {
                startM = 0;
            }
            matcher.region(startM, curPos); // here is only one match
            match = matcher.find(startM);
            if (match) {
                try {
                    textPane.getDocument().remove(matcher.start(),
                            matcher.end() - matcher.start());
                    textPane.getDocument().insertString(matcher.start(),
                            replacement, null);
                    textPane.setCaretPosition(matcher.start() + replacement.length());
                } catch (BadLocationException e) {
                }
            }
        } else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            if (!matcher.find(curPos)) {
                match = matcher.find(0);
            }
            if (match) {
                try {
                    textPane.getDocument().remove(matcher.start(),
                            matcher.end() - matcher.start());
                    textPane.getDocument().insertString(matcher.start(),
                            replacement, null);
                    textPane.setCaretPosition(matcher.start() + replacement.length());
                } catch (BadLocationException e) {
                }
            }
        }
        return match;
    }

    /**
     * Replaces all find pattern occurences with find replacement.
     * 
     * @param textPane the document where to perform search
     * @return false if replacement is null or if text was not found. Otherwise
     *  it returns true.
     * @throws NullPointerException if the find pattern is null
     */
    public boolean replaceAll(JTextPane textPane)
            throws NullPointerException {
        if ((matcher == null) && (pattern == null)) {
            throw new NullPointerException("matcher can't be null, use dialog");
        } else if (matcher == null) {
            matcher = pattern.matcher("");
        }
        if (replacement == null) {
            return false;
        }

        boolean match = false;
        int curPos = textPane.getCaretPosition();
        int endPos = textPane.getDocument().getEndPosition().getOffset() - 1;
        String txt = textPane.getText();

        matcher.reset(txt);
        matcher.useTransparentBounds(false);
        if (direction == FindText.DIRECTION_TO_END) {
            matcher.region(curPos, endPos);
            while (matcher.find()) {
                match = true;
                try {
                    textPane.getDocument().remove(matcher.start(),
                            matcher.end() - matcher.start());
                    textPane.getDocument().insertString(matcher.start(),
                            replacement, null);
                } catch (BadLocationException e) {
                }
            }
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            match = false;
            while (matcher.find()) {
                try {
                    textPane.getDocument().remove(matcher.start(),
                            matcher.end() - matcher.start());
                    textPane.getDocument().insertString(matcher.start(),
                            replacement, null);
                } catch (BadLocationException e) {
                }
            }
        } else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = false;
            while (matcher.find()) {
                match = true;
                try {
                    textPane.getDocument().remove(matcher.start(),
                            matcher.end() - matcher.start());
                    textPane.getDocument().insertString(matcher.start(),
                            replacement, null);
                } catch (BadLocationException e) {
                }
            }
        }
        return match;
    }
}
