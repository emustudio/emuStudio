/*
 * EmuTextPane.java
 *
 * Created on Štvrtok, 2007, august 9, 15:05
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This class is extended JTextPane class. Support some awesome features like
 * line numbering or syntax highlighting and other.
 * TODO: add ability to set breakpoints
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import emustudio.gui.syntaxHighlighting.HighLightedDocument;
import emustudio.gui.syntaxHighlighting.HighlightStyle;
import emustudio.gui.syntaxHighlighting.HighlightThread;
import emustudio.gui.syntaxHighlighting.DocumentReader;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import plugins.compiler.ILexer;
import plugins.compiler.IToken;
import emustudio.interfaces.ITokenColor;
import runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class EmuTextPane extends JTextPane {

    public static final short NUMBERS_WIDTH = 40;
    public static final short NUMBERS_HEIGHT = 4;

    public static final Object docLock = new Object();

    private ILexer syntaxLexer = null;
    private DocumentReader reader;
    private HighLightedDocument document;
    private Hashtable<Integer, HighlightStyle> styles; // token styles
    private HighlightThread highlight;
    private boolean fileSaved; // if is document saved
    private File fileSource;   // opened file
    private CompoundUndoManager undo;
    private ActionListener undoListener;
    private ActionEvent aevt;
    private boolean acceptUndo = true;
    private Timer undoTimer;

    private class UndoUpdater extends TimerTask {

        @Override
        public void run() {
            try {
                if (undoListener != null) {
                    undoListener.actionPerformed(aevt);
                }
                if (undoTimer != null) {
                    undoTimer.cancel();
                }
                undoTimer = null;
            } catch (Exception e) {
            }
        }
    }

    /** Creates a new instance of EmuTextPane */
    public EmuTextPane() {
        styles = new Hashtable<Integer, HighlightStyle>();
        initStyles();
        this.setFont(new java.awt.Font("monospaced", 0, 12));
        this.setMargin(new Insets(NUMBERS_HEIGHT, NUMBERS_WIDTH, 0, 0));
        this.setBackground(Color.WHITE);

        fileSaved = true;
        fileSource = null;
        undo = new CompoundUndoManager();
        aevt = new ActionEvent(this, 0, "");
        undoTimer = null;

        document = new HighLightedDocument();
        reader = new DocumentReader(document);
        document.setDocumentReader(reader);
        this.setStyledDocument(document);
        document.addDocumentListener(new DocumentListener() {

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (acceptUndo == false) {
                    return;
                }
                if (undoTimer != null) {
                    undoTimer.cancel();
                }
                undoTimer = new Timer();
                undoTimer.schedule(new UndoUpdater(), CompoundUndoManager.IDLE_DELAY_MS);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (acceptUndo == false) {
                    return;
                }
                if (undoTimer != null) {
                    undoTimer.cancel();
                }
                undoTimer = new Timer();
                undoTimer.schedule(new UndoUpdater(), CompoundUndoManager.IDLE_DELAY_MS);
            }
        });
        document.addUndoableEditListener(new UndoableEditListener() {

            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!acceptUndo) {
                    return;
                }
                UndoableEdit ed = e.getEdit();
                DefaultDocumentEvent event = (DefaultDocumentEvent) e.getEdit();
                if (event.getType().equals(DocumentEvent.EventType.CHANGE)) {
                    return;
                }
                if (ed.isSignificant()) {
                    undo.addEdit(ed);
                }
            }
        });
    }

    public void setLexer(ILexer sLexer) {
        this.syntaxLexer = sLexer;
        if (highlight != null) {
            try {
                highlight.shouldStop();
                highlight = null;
            } catch (Exception e) {
            }
        }
        highlight = new HighlightThread(syntaxLexer, reader, document, styles);
    }

    private void initStyles() {
        styles.clear();
        styles.put(IToken.COMMENT, new HighlightStyle(true, false, ITokenColor.COMMENT));
        styles.put(IToken.ERROR, new HighlightStyle(false, false, ITokenColor.ERROR));
        styles.put(IToken.IDENTIFIER, new HighlightStyle(false, false, ITokenColor.IDENTIFIER));
        styles.put(IToken.LABEL, new HighlightStyle(false, false, ITokenColor.LABEL));
        styles.put(IToken.LITERAL, new HighlightStyle(false, false, ITokenColor.LITERAL));
        styles.put(IToken.OPERATOR, new HighlightStyle(false, true, ITokenColor.OPERATOR));
        styles.put(IToken.PREPROCESSOR, new HighlightStyle(false, true, ITokenColor.PREPROCESSOR));
        styles.put(IToken.REGISTER, new HighlightStyle(false, false, ITokenColor.REGISTER));
        styles.put(IToken.RESERVED, new HighlightStyle(false, true, ITokenColor.RESERVED));
        styles.put(IToken.SEPARATOR, new HighlightStyle(false, false, ITokenColor.SEPARATOR));
    }

    /*** UNDO/REDO IMPLEMENTATION ***/
    public void setUndoStateChangedAction(ActionListener l) {
        undoListener = l;
    }

    public boolean canRedo() {
        return undo.canRedo();
    }

    public boolean canUndo() {
        return undo.canUndo();
    }

    public void undo() {
        if (undoTimer != null) {
            undoTimer.cancel();
        }
        acceptUndo = false;
        try {
            undo.undo();
        } catch (CannotUndoException e) {
        }
        undoTimer = new Timer();
        undoTimer.schedule(new UndoUpdater(), CompoundUndoManager.IDLE_DELAY_MS);
        acceptUndo = true;
    }

    public void redo() {
        if (undoTimer != null) {
            undoTimer.cancel();
        }
        try {
            undo.redo();
        } catch (CannotRedoException e) {
        }
        undoTimer = new Timer();
        undoTimer.schedule(new UndoUpdater(), CompoundUndoManager.IDLE_DELAY_MS);
    }

    /*** SYNTAX HIGHLIGHTING IMPLEMENTATION ***/
    public Reader getDocumentReader() {
        return reader;
    }

    /*** LINE NUMBERS PAINT IMPLEMENTATION ***/
    // implements view lines numbers
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int start, end, startline, endline;

        synchronized(docLock) {
            start = document.getStartPosition().getOffset();
            end = document.getEndPosition().getOffset();
        }
        // translate offsets to lines
        synchronized(docLock) {
            startline = document.getDefaultRootElement().getElementIndex(start);
            endline = document.getDefaultRootElement().getElementIndex(end) + 1;
        }
        int fontHeight = g.getFontMetrics(getFont()).getHeight(); // font height

        g.setColor(Color.RED);
        for (int line = startline, y = 0; line <= endline; line++, y += fontHeight) {
            g.drawString(Integer.toString(line), 0, y);
        }

        // paint blue thin lines
        g.setColor(Color.PINK);
        g.drawLine(NUMBERS_WIDTH - 5, g.getClipBounds().y, NUMBERS_WIDTH - 5,
                g.getClipBounds().y + g.getClipBounds().height);
    }

    /*** OPENING/SAVING FILE ***/
    public boolean openFile(String fileName) {
        return openFile(new File(fileName));
    }

    /***
     * Opens a file into text editor.
     * WARNING: Don't check whether file is saved.
     * 
     * @param file
     * @return true if file was successfuly opened.
     */
    public boolean openFile(File file) {
        fileSource = file;
        if (fileSource.canRead() == true) {
            try {
                FileReader vstup = new FileReader(fileSource.getAbsolutePath());
                setText("");
                getEditorKit().read(vstup, document, 0);
                this.setCaretPosition(0);
                vstup.close();
                fileSaved = true;
            } catch (java.io.FileNotFoundException ex) {
                StaticDialogs.showErrorMessage("File not found: "
                        + fileSource.getPath());
                return false;
            } catch (Exception e) {
                StaticDialogs.showErrorMessage("Error opening file: "
                        + fileSource.getPath());
                return false;
            }
        } else {
            StaticDialogs.showErrorMessage("File " + fileSource.getPath()
                    + " can't be read.");
            return false;
        }
        return true;
    }

    // returns true if a file was opened
    public boolean openFileDialog() {
        JFileChooser f = new JFileChooser();
        EmuFileFilter f1 = new EmuFileFilter();
        EmuFileFilter f2 = new EmuFileFilter();

        if (this.confirmSave() == true) {
            return false;
        }

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
        if (fileSource == null) {
            f.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileSource = f.getSelectedFile();
            return openFile(fileSource);
        }
        return false;
    }

    // return if cancel was pressed
    public boolean confirmSave() {
        if (fileSaved == false) {
            int r = JOptionPane.showConfirmDialog(null,
                    "File is not saved yet. Do you want to save the file ?");
            if (r == JOptionPane.YES_OPTION) {
                this.saveFile();
            } else if (r == JOptionPane.CANCEL_OPTION) {
                return true;
            }
        }
        return false;
    }

    // return true if file was saved
    public boolean saveFile() {
        if (fileSource == null || fileSource.canWrite() == false) {
            return saveFileDialog();
        } else {
            String fn = fileSource.getAbsolutePath();
            try {
                FileWriter vystup = new FileWriter(fn);
                write(vystup);
                vystup.close();
                fileSaved = true;
            } catch (Exception e) {
                StaticDialogs.showErrorMessage("Can't save file: " + fileSource.getPath());
                return false;
            }
            return true;
        }
    }

    // return true if file was saved
    public boolean saveFileDialog() {
        JFileChooser f = new JFileChooser();
        EmuFileFilter f1 = new EmuFileFilter();
        EmuFileFilter f2 = new EmuFileFilter();
        EmuFileFilter f3 = new EmuFileFilter();

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
        if (fileSource == null) {
            f.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        int returnVal = f.showSaveDialog(this);
        f.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileSource = f.getSelectedFile();
            if (fileSource.canWrite() == true || fileSource.exists() == false) {
                String fn = fileSource.getAbsolutePath();
                try {
                    EmuFileFilter fil = (EmuFileFilter) f.getFileFilter();
                    if (fil.getExtension(fileSource) == null && fil.getFirstExtension() != null) {
                        if (!fil.getFirstExtension().equals("*")) {
                            fn += "." + fil.getFirstExtension();
                        }
                    }
                    fileSource = new java.io.File(fn);
                    FileWriter vystup = new FileWriter(fn);
                    write(vystup);
                    vystup.close();
                    fileSaved = true;
                } catch (Exception e) {
                    StaticDialogs.showErrorMessage("Can't save file: " + fileSource.getPath());
                    return false;
                }
            } else {
                StaticDialogs.showErrorMessage("Bad file name");
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isFileSaved() {
        if (this.fileSaved == true && this.fileSource != null) {
            return true;
        } else {
            return false;
        }
    }

    public String getFileName() {
        if (this.fileSource == null) {
            return null;
        } else {
            return this.fileSource.getAbsolutePath();
        }
    }

    // return true if new file was created
    public boolean newFile() {
        if (confirmSave() == true) {
            return false;
        }
        fileSource = null;
        fileSaved = true;
        this.setText("");
        return true;
    }
}
