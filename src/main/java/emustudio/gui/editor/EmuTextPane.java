/*
 * EmuTextPane.java
 *
 * Created on Štvrtok, 2007, august 9, 15:05
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2012, Peter Jakubčo
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

import emulib.plugins.compiler.ICompiler;
import emulib.plugins.compiler.ILexer;
import emulib.plugins.compiler.IToken;
import emulib.plugins.compiler.SourceFileExtension;
import emustudio.gui.utils.EmuFileFilter;
import emustudio.interfaces.ITokenColor;
import emustudio.main.Main;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is extended JTextPane class. Support some awesome features like
 * line numbering or syntax highlighting and other.
 * TODO: add ability to set breakpoints
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class EmuTextPane extends JTextPane {
    private final static Logger logger = LoggerFactory.getLogger(EmuTextPane.class);

    /**
     * Width of a column in the editor where line numbers are shown
     */
    public static final short NUMBERS_WIDTH = 40;
    
    /**
     * Height of the line
     */
    public static final short NUMBERS_HEIGHT = 4;

    private ILexer syntaxLexer = null;
    private DocumentReader reader;
    private HighLightedDocument document;
    private Map<Integer, HighlightStyle> styles; // token styles
    private HighlightThread highlight;
    private boolean fileSaved; // if is document saved
    private File fileSource;   // opened file
    private CompoundUndoManager undo;
    private ActionListener undoListener;
    private ActionEvent aevt;
    private ReadWriteLock undoTimerLock;
    private final Object undoLock = new Object();
    private Timer undoTimer;

    private SourceFileExtension[] fileExtensions;

    private class UndoUpdater extends TimerTask {

        @Override
        public void run() {
            synchronized (undoLock) {
                try {
                    undoTimerLock.writeLock().lock();
                    this.cancel();
                    undoTimer = null;
                    if (undoListener != null) {
                        undoListener.actionPerformed(aevt);
                    }
                } catch (Exception e) {
                    logger.error("Error during performing undoable action.", e);
                } finally {
                    undoTimerLock.writeLock().unlock();
                }
            }
        }
    }

    /**
     * Creates a new instance of EmuTextPane
     *
     * @param compiler The chosen compiler.
     */
    public EmuTextPane(ICompiler compiler) {
        initStyles();

        fileSaved = true;
        fileSource = null;

        document = new HighLightedDocument();
        reader = new DocumentReader(document);
        document.setDocumentReader(reader);
        this.setStyledDocument(document);

        if (compiler != null) {
            this.fileExtensions = compiler.getSourceSuffixList();
            this.syntaxLexer = compiler.getLexer(reader);
        }

        undoTimerLock = new ReentrantReadWriteLock();
        aevt = new ActionEvent(this, 0, "");
        undo = new CompoundUndoManager();

        document.addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                // Do nothing
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                try {
                    undoTimerLock.writeLock().lock();
                    if (undoTimer == null) {
                        undoTimer = new Timer();
                        undoTimer.schedule(new UndoUpdater(), CompoundUndoManager.IDLE_DELAY_MS);
                    }
                } finally {
                    undoTimerLock.writeLock().unlock();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                try {
                    undoTimerLock.writeLock().lock();
                    if (undoTimer == null) {
                        undoTimer = new Timer();
                        undoTimer.schedule(new UndoUpdater(), CompoundUndoManager.IDLE_DELAY_MS);
                    }
                } finally {
                    undoTimerLock.writeLock().unlock();
                }
            }
        });
        document.addUndoableEditListener(new UndoableEditListener() {

            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                try {
                    document.readLock();
                    UndoableEdit ed = e.getEdit();
                    DefaultDocumentEvent event = (DefaultDocumentEvent) e.getEdit();
                    if (event.getType().equals(DocumentEvent.EventType.CHANGE)) {
                        return;
                    }
                    if (ed.isSignificant()) {
                        undo.addEdit(ed);
                    }
                } finally {
                    document.readUnlock();
                }
            }
        });
        if (syntaxLexer != null) {
            highlight = new HighlightThread(syntaxLexer, reader, document, styles);
        }
    }

    private void initStyles() {
        styles = new HashMap<Integer, HighlightStyle>();
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
        this.setFont(new java.awt.Font("monospaced", 0, 12));
        this.setMargin(new Insets(NUMBERS_HEIGHT, NUMBERS_WIDTH, 0, 0));
        this.setBackground(Color.WHITE);
    }

    /*** UNDO/REDO IMPLEMENTATION ***/
    
    /**
     * Set up undo/redo listener.
     * 
     * @param l The undo listener
     */
    public void setUndoStateChangedAction(ActionListener l) {
        undoListener = l;
    }

    /**
     * Determine if the Redo operation can be realized.
     * 
     * @return true if Redo can be realized, false otherwise.
     */
    public synchronized boolean canRedo() {
        synchronized (undoLock) {
            return undo.canRedo();
        }
    }

    /**
     * Determine if the Undo operation can be realized.
     *
     * @return true if Undo can be realized, false otherwise.
     */
    public synchronized boolean canUndo() {
        synchronized (undoLock) {
            return undo.canUndo();
        }
    }

    /**
     * Perform Undo operation.
     */
    public void undo() {
        synchronized (undoLock) {
            try {
                undo.undo();
            } catch (CannotUndoException e) {
            }
        }
    }

    /**
     * Perform Redo operation.
     */
    public void redo() {
        synchronized (undoLock) {
            try {
                undo.redo();
            } catch (CannotRedoException e) {
            }
        }
    }

    /*** SYNTAX HIGHLIGHTING IMPLEMENTATION ***/
    
    /**
     * Get document reader for this editor.
     * 
     * @return document reader
     */
    public Reader getDocumentReader() {
        return reader;
    }

    /*** LINE NUMBERS PAINT IMPLEMENTATION ***/
    
    /**
     * Implements view lines numbers
     * 
     * @param g Graphics object
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int start, end, startline, endline;
        try {
            document.readLock();

            start = document.getStartPosition().getOffset();
            end = document.getEndPosition().getOffset();
            // translate offsets to lines
            startline = document.getDefaultRootElement().getElementIndex(start);
            endline = document.getDefaultRootElement().getElementIndex(end) + 1;
        } finally {
            document.readUnlock();
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

    /**
     * The method opens the file based on file name given as String.
     *
     * @param fileName the name of the file to open
     * @return true if the file was opened, false otherwise.
     */
    public boolean openFile(String fileName) {
        return openFile(new File(fileName));
    }

    /**
     * Opens a file into text editor.
     * WARNING: This method Doesn't check whether file is saved.
     * 
     * @param file The file to open
     * @return true if file was successfuly opened, false otherwise.
     */
    public boolean openFile(File file) {
        fileSource = file;
        if (fileSource.canRead() == false) {
            String msg = new StringBuilder().append("File: ").append(fileSource.getPath()).append(" cannot be read.")
                    .toString();
            logger.error(msg);
            Main.tryShowErrorMessage(msg);
            return false;
        }
        try {
            if (highlight != null) {
                highlight.shouldStop();
                highlight = null;
            }
            FileReader vstup = new FileReader(fileSource);
            setText("");
            getEditorKit().read(vstup, document, 0);
            this.setCaretPosition(0);
            vstup.close();
            fileSaved = true;

            if (syntaxLexer != null) {
                highlight = new HighlightThread(syntaxLexer, reader, document, styles);
                highlight.colorAll();
            }
        } catch (java.io.FileNotFoundException e) {
            logger.error("Could not open file. File not found.", e);
            Main.tryShowErrorMessage("File not found: " + fileSource.getPath());
            return false;
        } catch (Exception e) {
            logger.error("Could not open file.", e);
            Main.tryShowErrorMessage("Error opening the file: " + fileSource.getPath());
            return false;
        }
        return true;
    }

    /**
     * Display open-file dialog and opens a file.
     *
     * @return true if a file was opened; false otherwise
     */
    public boolean openFileDialog() {
        JFileChooser f = new JFileChooser();
        EmuFileFilter f1 = new EmuFileFilter();
        EmuFileFilter f2 = new EmuFileFilter();

        if (this.confirmSave() == true) {
            return false;
        }

        if ((fileExtensions != null) && (fileExtensions.length > 0)) {
            String descr = "Source files (";
            for (int i = 0; i < fileExtensions.length; i++) {
                f1.addExtension(fileExtensions[i].getExtension());
                descr += "*." + fileExtensions[i].getExtension() + ",";
            }
            descr = descr.substring(0, descr.length()-1);
            descr += ")";
            f1.setDescription(descr);
        }
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");

        f.setDialogTitle("Open a file");
        f.setAcceptAllFileFilterUsed(false);
        if (f1.getExtensionsCount() > 0) {
            f.addChoosableFileFilter(f1);
        } else {
            f1 = f2;
        }
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

    /**
     * Method asks the user if he wants to save a file and performs it when
     * he confirms.
     *
     * @return If the user cancels the confirmation, return true. Otherwise
     * return false.
     */
    public boolean confirmSave() {
        if (fileSaved == false) {
            int r = JOptionPane.showConfirmDialog(null, "File is not saved yet. Do you want to save the file ?");
            if (r == JOptionPane.YES_OPTION) {
                return (!saveFile(true));
            } else if (r == JOptionPane.CANCEL_OPTION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Save the file.
     *
     * @param showDialogIfFileIsInvalid the flag indicating if the save
     * dialog should be shown to select new file if the fileSource is null
     * or invalid.
     * @return true if file was saved, false otherwise
     */
    public boolean saveFile(boolean showDialogIfFileIsInvalid) {
        if ((fileSource == null) || (fileSource.exists() && (fileSource.canWrite() == false))) {
            if (showDialogIfFileIsInvalid)
                return saveFileDialog();
            else {
                logger.error("Could not save file, the file is not writable: " + fileSource.getPath());
                Main.tryShowErrorMessage("Error: Cannot save the file!\n The selected file is not writable.");
                return false;
            }
        }
        try {
            FileWriter vystup = new FileWriter(fileSource);
            write(vystup);
            vystup.close();
            fileSaved = true;
            return true;
        } catch (Exception e) {
            logger.error("Could not save file: " + fileSource.getPath(), e);
            Main.tryShowErrorMessage(new StringBuilder().append("Error: Cannot save the file: ")
                    .append(fileSource.getPath()).append("\n").append(e.getLocalizedMessage()).toString());
            return false;
        }
    }

    /**
     * Shows file chooser dialog to select the file and save the file.
     *
     * @return true if the file was saved, false otherwise.
     */
    public boolean saveFileDialog() {
        JFileChooser f = new JFileChooser();
        EmuFileFilter[] filters;
        int tmpLen = (fileExtensions != null) ? fileExtensions.length : 0;
        
        f.setDialogTitle("Save the file");
        f.setAcceptAllFileFilterUsed(false);

        filters = new EmuFileFilter[tmpLen + 1];
        if ((fileExtensions != null) && (fileExtensions.length > 0)) {
            for (int i = 0; i < fileExtensions.length; i++) {
                filters[i] = new EmuFileFilter();
                filters[i].addExtension(fileExtensions[i].getExtension());
                filters[i].setDescription(fileExtensions[i].getFormattedDescription());
                f.addChoosableFileFilter(filters[i]);
            }
        }
        filters[filters.length-1] = new EmuFileFilter();
        filters[filters.length-1].addExtension("*");
        filters[filters.length-1].setDescription("All files (*.*)");
        f.addChoosableFileFilter(filters[filters.length-1]);
        f.setFileFilter(filters[0]);
        f.setApproveButtonText("Save");
        f.setSelectedFile(fileSource);
        if (fileSource == null) {
            f.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        int returnVal = f.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        fileSource = f.getSelectedFile();
        String fn = fileSource.getAbsolutePath();
        EmuFileFilter fil = (EmuFileFilter) f.getFileFilter();
        if ((EmuFileFilter.getExtension(fileSource) == null)
                && (fil.getFirstExtension() != null)) {
            if (!fil.getFirstExtension().equals("*")) {
                fn += "." + fil.getFirstExtension();
            }
        }
        fileSource = new java.io.File(fn);
        return saveFile(false);
    }

    /**
     * Determine if the file was saved - i.e. if it is unmodified from last
     * save.
     *
     * @return true if the file is saved, false otherwise
     */
    public boolean isFileSaved() {
        if (fileSaved  && (fileSource != null)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return full path of the source file name, if it exists.
     * @return
     */
    public String getFileName() {
        if (fileSource == null) {
            return null;
        } else {
            return fileSource.getAbsolutePath();
        }
    }

    /**
     * Create new file environment.
     *
     * Confirms to save the file when it is not already saved, or is modified.
     * Clears the environment and the text area.
     *
     * @return true if the new file was created, false otherwise
     */
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
