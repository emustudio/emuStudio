/*
 * emuTextPane.java
 *
 * Created on Štvrtok, 2007, august 9, 15:05
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This class is extended JTextPane class. Support some awesome features like
 * line numbering or syntax highlighting and other.
 * TODO: add ability to set breakpoints
 */

package emu8.gui;

import emu8.*;
import plugins.compiler.*;

import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.swing.event.*;
import java.awt.*;
import javax.swing.text.*;
import java.util.*;
import javax.swing.undo.*;

/**
 *
 * @author vbmacher
 */
public class emuTextPane extends JTextPane {
    public static final short NUMBERS_WIDTH = 35;
    public static final short NUMBERS_HEIGHT = 4;

    private ILexer syntaxLexer = null;
    private DocumentReader reader;
    private HighlightedDocument document;
    private Hashtable styles; // token styles
    private HighLightThread highlight;

    private boolean fileSaved; // if is document saved
    private File fileSource;   // opened file
    private UndoManager undo;
    private ActionListener undoStateListener;

    /** Creates a new instance of emuTextPane */
    public emuTextPane() {
        styles = new Hashtable();
        initStyles();
        document = new HighlightedDocument();
        reader = new DocumentReader(document);
        this.setStyledDocument(document);
        this.setFont(new java.awt.Font("monospaced", 0, 12));
        this.setMargin(new Insets(NUMBERS_HEIGHT,NUMBERS_WIDTH,0,0));
        this.setBackground(Color.WHITE);

        fileSaved = true;
        fileSource = null;
        undo = new UndoManager();
        document.addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                undo.addEdit(e.getEdit());
                if (undoStateListener != null)
                    undoStateListener.actionPerformed(new ActionEvent(this,0,""));
            }
        });
        undoStateListener = null;
    }

    public void setLexer(ILexer sLexer) {
        this.syntaxLexer = sLexer;
    }

    private class HighlightStyle extends SimpleAttributeSet {
        public HighlightStyle(boolean italic, boolean bold, Color color) {
            StyleConstants.setFontFamily(this, "Monospaced");
            StyleConstants.setFontSize(this, 12);
            StyleConstants.setBackground(this, Color.white);
            StyleConstants.setItalic(this, italic);
            StyleConstants.setForeground(this, color);
            StyleConstants.setBold(this, bold);
        }
    }
    private void initStyles() {
        styles.clear();
        styles.put(IToken.COMMENT, new HighlightStyle(true,false,ITokenColor.COMMENT));
        styles.put(IToken.ERROR, new HighlightStyle(false,false,ITokenColor.ERROR));
        styles.put(IToken.IDENTIFIER, new HighlightStyle(false,false,ITokenColor.IDENTIFIER));
        styles.put(IToken.LABEL, new HighlightStyle(false,false,ITokenColor.LABEL));
        styles.put(IToken.LITERAL, new HighlightStyle(false,false,ITokenColor.LITERAL));
        styles.put(IToken.OPERATOR, new HighlightStyle(false,true,ITokenColor.OPERATOR));
        styles.put(IToken.PREPROCESSOR, new HighlightStyle(false,true,ITokenColor.PREPROCESSOR));
        styles.put(IToken.REGISTER, new HighlightStyle(false,false,ITokenColor.REGISTER));
        styles.put(IToken.RESERVED, new HighlightStyle(false,true,ITokenColor.RESERVED));
        styles.put(IToken.SEPARATOR, new HighlightStyle(false,false,ITokenColor.SEPARATOR));
    }


    /*** UNDO/REDO IMPLEMENTATION ***/
    public void setUndoStateChangedAction(ActionListener l) {
        undoStateListener = l;
    }
    public boolean canRedo() { return undo.canRedo(); }
    public boolean canUndo() { return undo.canUndo(); }
    public void undo() { undo.undo(); }
    public void redo() { undo.redo(); }
    
    /*** SYNTAX HIGHLIGHTING IMPLEMENTATION ***/
     
    public Reader getDocumentReader() { return reader; }
    
    private class HighlightedDocument extends DefaultStyledDocument { 
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            super.insertString(offs, str, a);
            fileSaved = false;
            reHighlight();
        }
        public void remove(int offs, int len) throws BadLocationException {
            super.remove(offs, len);
            fileSaved = false;
            reHighlight();
        }
    }
    
    private void reHighlight() {
        if (highlight != null && highlight.isAlive())
            highlight.stopRun();
        highlight = new HighLightThread();
    }
    
    private class HighLightThread extends Thread {
        private boolean running;
        public HighLightThread() { running = true; start(); }
        public void run() {
            if (syntaxLexer == null ||
                    (syntaxLexer instanceof ILexer) == false) return;
            reader.reset(); // from the beginning
            // recolor all tokens
            try {
                // first reset lexer
                syntaxLexer.reset(reader,0,0,0);
                IToken t = syntaxLexer.getSymbol();
                while (running && t.getType() != IToken.TEOF) {
                    reColor(t.getCharBegin(),t.getCharEnd()-t.getCharBegin(),t.getType());
                    t = syntaxLexer.getSymbol();
                    Thread.yield();
                }
            } catch (IOException e) {}
            catch (Error y) { /* no match */ }
        }
        public void stopRun() { running = false; }
        private void reColor(int position, int length, int tokenType) {
            if ((position + length) <= document.getLength()){
                SimpleAttributeSet style = (SimpleAttributeSet)styles.get(tokenType);
                if (style == null)
                    style = (SimpleAttributeSet)styles.get(IToken.ERROR);
                document.setCharacterAttributes(position,length,style,true);
            }                                                                   
        }
    }
   
    /*** LINE NUMBERS PAINT IMPLEMENTATION ***/
    // implements view lines numbers
    public void paint(Graphics g) {
        super.paint(g);
        int start = document.getStartPosition().getOffset();
        int end = document.getEndPosition().getOffset();

        // translate offsets to lines
        int startline = document.getDefaultRootElement().getElementIndex(start) ;
        int endline = document.getDefaultRootElement().getElementIndex(end)+1;

        int fontHeight = g.getFontMetrics(getFont()).getHeight(); // font height

        g.setColor(Color.RED);
        for (int line = startline, y = 0; line <= endline; line++, y += fontHeight)
            g.drawString(Integer.toString(line), 0, y);
    }

    /*** OPENING/SAVING FILE ***/
    
    // returns true if a file was opened
    public boolean openFileDialog() {
        JFileChooser f = new JFileChooser();
        emuFileFilter f1 = new emuFileFilter();
        emuFileFilter f2 = new emuFileFilter();

        if (this.confirmSave() == true)
            return false;
        
        f1.addExtension("asm");
        f1.addExtension("txt");
        f1.setDescription("Assembler source (*.asm, *.txt)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");
        
        f.setDialogTitle("Open a file");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Open");
        f.setSelectedFile(fileSource);

        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            fileSource = f.getSelectedFile();
            if (fileSource.canRead() == true) {
                try {
                    FileReader vstup = new FileReader(fileSource.getAbsolutePath());
                    setText("");
                    ILexer l = this.syntaxLexer;
                    this.syntaxLexer = null;
                    getEditorKit().read(vstup, document,0);
                    this.syntaxLexer = l;
                    reHighlight();
                    this.setCaretPosition(0);
                    vstup.close(); fileSaved = true;
                }  catch (java.io.FileNotFoundException ex) {
                    Main.showErrorMessage("File not found: " 
                            + fileSource.getPath());
                    return false;
                }
                catch (Exception e) {
                    Main.showErrorMessage("Error opening file: "
                            + fileSource.getPath());
                    return false;
                }
            } else {
                Main.showErrorMessage("File " + fileSource.getPath()
                    + " can't be read.");
                return false;
            }
            return true;
        }
        return false;
    }
    
    // return if cancel was pressed
    public boolean confirmSave() {
        if (fileSaved == false) {
            int r = JOptionPane.showConfirmDialog(null,
                    "File is not saved yet. Do you want to save the file ?");
            if (r == JOptionPane.YES_OPTION)
                this.saveFile();
            else if (r == JOptionPane.CANCEL_OPTION)
                return true;
        }
        return false;
    }

    // return true if file was saved
    public boolean saveFile() {
        if (fileSource == null || fileSource.canWrite() == false) return saveFileDialog();
        else {
            String fn = fileSource.getAbsolutePath();
            try {
                FileWriter vystup = new FileWriter(fn);
                write(vystup);
                vystup.close();
                fileSaved = true;
            } 
            catch (Exception e) {
                Main.showErrorMessage("Can't save file: " + fileSource.getPath());
                return false;
            }
            return true;
        }
    }
    
    // return true if file was saved
    public boolean saveFileDialog() {
        JFileChooser f = new JFileChooser();
        emuFileFilter f1 = new emuFileFilter();
        emuFileFilter f2 = new emuFileFilter();
        emuFileFilter f3 = new emuFileFilter();
        
        f1.addExtension("asm");
        f1.setDescription("Assembler source (*.asm)");
        f2.addExtension("txt");
        f2.setDescription("Assembler source (*.txt)");
        f3.addExtension("*");
        f3.setDescription("All files (*.*)");
        
        f.setDialogTitle("Save a file");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.addChoosableFileFilter(f3);
        f.setFileFilter(f1);
        f.setApproveButtonText("Save");
        f.setSelectedFile(fileSource);

        int returnVal = f.showSaveDialog(this);
        f.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileSource = f.getSelectedFile();
            if (fileSource.canWrite() == true || fileSource.exists() == false ) {
                String fn = fileSource.getAbsolutePath();
                try {
                    emuFileFilter fil = (emuFileFilter)f.getFileFilter();
                    if (fil.getExtension(fileSource) == null && fil.getFirstExtension()!=null) {
                        if (!fil.getFirstExtension().equals("*"))
                            fn +="."+fil.getFirstExtension();
                    }
                    fileSource = new java.io.File(fn);
                    FileWriter vystup = new FileWriter(fn);
                    write(vystup);
                    vystup.close();
                    fileSaved = true;
                } 
                catch (Exception e) {
                    Main.showErrorMessage("Can't save file: " + fileSource.getPath());
                    return false;
                }
            } else {
                Main.showErrorMessage("Bad file name"); return false;
            }
            return true;
        }
        return false;
    }
    
    public boolean isFileSaved() { 
        if (this.fileSaved == true && this.fileSource != null)
            return true;
        else return false;
    }
    
    public String getFileName() {
        if (this.fileSource == null) return null;
        else return this.fileSource.getAbsolutePath();
    }
    
    // return true if new file was created
    public boolean newFile() {
        if (confirmSave() == true) return false;
        fileSource = null;
        fileSaved = true;
        this.setText("");
        return true;
    }
    
    /*** DOCUMENT READER CLASS IMPLEMENTATION ***/
    class DocumentReader extends Reader {
        private long position = 0;
        /* pouziva sa na oznacenie miesta v dokumente, v ktorom je mozne bezpecne
         * resetovat lex. analyzator  */
        private long mark = -1;
        private AbstractDocument document;
      
        public DocumentReader(AbstractDocument document){
            this.document = document;
        }

        public void close() {}
        public void mark(int readAheadLimit) { mark = position; }
        public boolean markSupported() { return true; }

        public int read(){
            if (position < document.getLength()){
                try {
                    char c = document.getText((int)position, 1).charAt(0);
                    position++;
                    return c;
                } catch (BadLocationException x){
                    return -1;
                }
            } else return -1;
        }

        public int read(char[] cbuf, int off, int len){
            if (position < document.getLength()){
                int length = len;
                if (position + length >= document.getLength())
                    length = document.getLength() - (int)position;
                if (off + length >= cbuf.length)
                    length = cbuf.length - off;
                try {
                    String s = document.getText((int)position, length);
                    position += length;
                    for (int i=0; i<length; i++)
                        cbuf[off+i] = s.charAt(i);
                    return length;
                } catch (BadLocationException x){
                    return -1;
                }
            } else return -1;
        }

        public boolean ready() { return true; }

        public void reset(){
            if (mark == -1) position = 0;
            else position = mark;
            mark = -1;
        }

        /* Skip characters of input */
        public long skip(long n){
            if (position + n <= document.getLength()){
                position += n;
                return n;
            } else {
                long oldPos = position;
                position = document.getLength();
                return (document.getLength() - oldPos);
            }
        }

        /* Seek to the given position in the document.  */
        public void seek(long n){
            if (n <= document.getLength())
                position = n;
            else position = document.getLength();
        }
    }
}
