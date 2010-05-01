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
package gui.syntaxHighlighting;

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
	
	public void setDocumentReader(DocumentReader documentReader) {
		this.documentReader = documentReader;
	}
	
	public void setThread(HighlightThread high) {
		this.high = high;
	}
	
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
  //      synchronized (HighlightThread.doclock){
            super.insertString(offs, str, a);
            if (high != null) 
            	high.color(offs, str.length());
            if (documentReader != null) 
            	documentReader.update(offs, str.length());
     //   }
    }

    public void remove(int offs, int len) throws BadLocationException {
    //    synchronized (HighlightThread.doclock){
            super.remove(offs, len);
            if (high != null)
            	high.color(offs, -len);
            if (documentReader != null)
            	documentReader.update(offs, -len);
    //    }
    }
}
