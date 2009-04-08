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
