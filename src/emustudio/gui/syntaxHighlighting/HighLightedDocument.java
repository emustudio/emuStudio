/**
 * HighLightedDocument.java
 *
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package emustudio.gui.syntaxHighlighting;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

/**
 * Just like a DefaultStyledDocument but intercepts inserts and
 * removes to color them.
 */
@SuppressWarnings("serial")
public class HighLightedDocument extends DefaultStyledDocument {

    private HighlightThread high;
    private DocumentReader documentReader;

    /**
     * Set the document reader object for this syntax highlighter.
     * The document reader should be initialized by the source code
     * already.
     *
     * @param documentReader the DoucmentReader object
     */
    public void setDocumentReader(DocumentReader documentReader) {
        this.documentReader = documentReader;
    }

    /**
     * Assign a Thread object that will execute the syntax highlighting
     * process.
     *
     * @param high the syntax highlighting thread
     */
    public void setThread(HighlightThread high) {
        this.high = high;
    }

    /**
     * Performs the update of the document reader and re-color needed parts,
     * when a text is inserted into the document.
     *
     * It should be called whenever a text is inserted somewhere into the
     * source code.
     *
     * If the position lies outside of the document, it throws
     * BadLocationException exception.
     *
     * @param offs the begin position of the inserted text
     * @param str the string that is inserted
     * @param a attributes of the text
     * @throws BadLocationException
     */
    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        //      synchronized (HighlightThread.doclock){
        super.insertString(offs, str, a);
        if (high != null) {
            high.color(offs, str.length());
        }
        if (documentReader != null) {
            documentReader.update(offs, str.length());
        }
        //   }
    }

    /**
     * Performs the update of the document reader and re-color needed parts,
     * when a text is removed from the document.
     *
     * It should be called whenever a text is removed from somewhere in the
     * source code.
     *
     * If the position lies outside of the document, it throws
     * BadLocationException exception.
     *
     * @param offs offset of removed text
     * @param len length of the removed text
     * @throws BadLocationException
     */
    @Override
    public void remove(int offs, int len) throws BadLocationException {
        //    synchronized (HighlightThread.doclock){
        super.remove(offs, len);
        if (high != null) {
            high.color(offs, -len);
        }
        if (documentReader != null) {
            documentReader.update(offs, -len);
        }
        //    }
    }
}
