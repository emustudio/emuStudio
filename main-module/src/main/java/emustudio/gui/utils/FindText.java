/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FindText {
    public static final int CASE_SENSITIVE = 1;
    public static final int WHOLE_WORDS = 2;
    public static final int DIRECTION_TO_END = 0;
    public static final int DIRECTION_TO_START = 1;
    public static final int DIRECTION_ALL = 2;

    private Matcher matcher = null;
    private Pattern pattern = null;

    public String replacement = "";

    private int direction = 0;
    private byte params = 0;
    // start/end of last match
    private int startM = -1;
    private int endM = -1;

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

    public String getFindExpr() {
        if (matcher == null) {
            return null;
        }
        return matcher.pattern().toString();
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int dir) {
        direction = dir;
    }

    public boolean isCaseSensitive() {
        return ((params & CASE_SENSITIVE) != 0);
    }

    public boolean isWholeWords() {
        return ((params & WHOLE_WORDS) != 0);
    }

    public void setParams(byte par) {
        params = par;
    }

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
            replaceIfMatched(textPane, match);
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            while (matcher.find(endM)) {
                if (matcher.end() >= curPos) {
                    break;
                }
                startM = matcher.start() - 1;
                endM = matcher.end();
            }
            if (startM < 0) {
                startM = 0;
            }
            matcher.region(startM, curPos); // here is only one match
            match = matcher.find(startM);
            replaceIfMatched(textPane, match);
        } else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            if (!matcher.find(curPos)) {
                match = matcher.find(0);
            }
            replaceIfMatched(textPane, match);
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
                replaceNow(textPane);
            }
        } else if (direction == FindText.DIRECTION_TO_START) {
            matcher.region(0, curPos);
            match = false;
            while (matcher.find()) {
                replaceNow(textPane);
            }
        } else if (direction == FindText.DIRECTION_ALL) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = false;
            while (matcher.find()) {
                match = true;
                replaceNow(textPane);
            }
        }
        return match;
    }

    private void replaceIfMatched(JTextPane textPane, boolean match) {
        if (match) {
            replaceNow(textPane);
            textPane.setCaretPosition(matcher.start() + replacement.length());
        }
    }

    private void replaceNow(JTextPane textPane) {
        try {
            textPane.getDocument().remove(matcher.start(),
                    matcher.end() - matcher.start());
            textPane.getDocument().insertString(matcher.start(),
                    replacement, null);
        } catch (BadLocationException ignored) {
        }
    }
}
