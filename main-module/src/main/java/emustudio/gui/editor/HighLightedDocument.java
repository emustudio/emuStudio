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
package emustudio.gui.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.text.DefaultStyledDocument;

/**
 * Just like a DefaultStyledDocument but intercepts inserts and removes to color them.
 */
class HighLightedDocument extends DefaultStyledDocument {

    private HighlightThread high;
    private DocumentReader documentReader;

    void setDocumentReader(DocumentReader documentReader) {
        this.documentReader = documentReader;
    }

    synchronized void setThread(HighlightThread high) {
        this.high = high;
    }

    /**
     * Performs the update of the document reader and re-color needed parts,
     * when a text is inserted into the document.
     * <p>
     * It should be called whenever a text is inserted somewhere into the
     * source code.
     *
     * @param evt the change event
     */
    @Override
    public void fireInsertUpdate(DocumentEvent evt) {
        super.fireInsertUpdate(evt);
        if (documentReader != null) {
            documentReader.update(evt.getOffset(), evt.getLength());
        }
        if (high != null) {
            high.color(evt.getOffset(), evt.getLength());
        }
    }

    /**
     * Performs the update of the document reader and re-color needed parts,
     * when a text is removed from the document.
     * <p>
     * It should be called whenever a text is removed from somewhere in the
     * source code.
     *
     * @param evt the change event
     */
    @Override
    public void fireRemoveUpdate(DocumentEvent evt) {
        super.fireRemoveUpdate(evt);
        int offs = evt.getOffset();
        int len = -evt.getLength();

        if (documentReader != null) {
            documentReader.update(offs, len);
        }
        if (high != null) {
            high.color(offs, len);
        }
    }
}
