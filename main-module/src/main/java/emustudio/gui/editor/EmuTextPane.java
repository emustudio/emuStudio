/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.compiler.Token;
import emulib.runtime.UniversalFileFilter;
import emustudio.Constants;
import emustudio.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class is extended JTextPane class. Support some awesome features like
 * line numbering or syntax highlighting and other.
 * TODO: add ability to set breakpoints
 *
 */
public class EmuTextPane extends JTextPane {
    private final static Logger logger = LoggerFactory.getLogger(EmuTextPane.class);

    private static final short NUMBERS_WIDTH = 40;
    private static final short NUMBERS_HEIGHT = 4;

    private LexicalAnalyzer syntaxLexer = null;
    private DocumentReader reader;
    private HighLightedDocument document;
    private Map<Integer, HighlightStyle> styles; // token styles
    private HighlightThread highlight;
    private boolean fileSaved; // if is document saved
    private File fileSource;   // opened file
    private CompoundUndoManager undo;
    private UndoActionListener undoListener;
    private Timer undoTimer = new Timer("UndoTimer");
    private final Object undoLock = new Object();
    private Timer syntaxStartTimer = new Timer("SyntaxStartTimer");
    private final Object syntaxLock = new Object();
    private final ViewFactory htmlFactory = new Workaround6606443ViewFactory();

    private SourceFileExtension[] fileExtensions;

    public interface UndoActionListener {
        void undoStateChanged(boolean canUndo, String presentationName);
        void redoStateChanged(boolean canRedo, String presentationName);
    }

    private static class MyFlowStrategy extends FlowView.FlowStrategy {

        @Override
        public void layout(FlowView fv) {
            fv.layoutChanged(fv.getAxis());
            super.layout(fv);
        }
    }

    private static class Workaround6606443ViewFactory implements ViewFactory {

        @Override
        public View create(Element element) {
            String name = element.getName();
            View view;
            switch (name) {
                case AbstractDocument.ContentElementName:
                    view = new LabelView(element);
                    break;
                case AbstractDocument.ParagraphElementName:
                    view = new ParagraphView(element);
                    break;
                case AbstractDocument.SectionElementName:
                    view = new BoxView(element, View.Y_AXIS);
                    break;
                case StyleConstants.ComponentElementName:
                    view = new ComponentView(element);
                    break;
                case StyleConstants.IconElementName:
                    view = new IconView(element);
                    break;
                default:
                    throw new AssertionError("Unknown Element type: "
                            + element.getClass().getName() + " : "
                            + name);
            }
            if (view instanceof ParagraphView) {
                try {
                    Field strategy = FlowView.class.getDeclaredField("strategy");
                    strategy.setAccessible(true);
                    strategy.set(view, new MyFlowStrategy());
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    logger.error("Could not set custom FlowStrategy in editor pane", e);
                }
            }
            return view;
        }
    }

    public EmuTextPane(Optional<Compiler> compiler) {
        initStyles();

        fileSaved = true;
        fileSource = null;

        setEditorKit(new StyledEditorKit() {

            @Override
            public ViewFactory getViewFactory() {
                return htmlFactory;
            }
        });
        document = new HighLightedDocument();
        reader = new DocumentReader(document);
        document.setDocumentReader(reader);
        this.setStyledDocument(document);

        if (compiler.isPresent()) {
            this.fileExtensions = compiler.get().getSourceSuffixList();
            this.syntaxLexer = compiler.get().getLexer(reader);
        }

        undo = new CompoundUndoManager();

        document.addUndoableEditListener(new UndoableEditListener() {

            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                UndoableEdit ed = e.getEdit();
                DefaultDocumentEvent event = (DefaultDocumentEvent) e.getEdit();
                if (event.getType().equals(DocumentEvent.EventType.CHANGE)) {
                    return;
                }
                if (ed.isSignificant()) {
                    synchronized (undoLock) {
                        undo.addEdit(ed);
                    }
                    undoTimer.cancel();
                    undoTimer = new Timer("UndoTimer");
                    undoTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            this.cancel();
                            boolean canUndo, canRedo;
                            String undoPresName, redoPresName;
                            synchronized (undoLock) {
                                undo.commitCompound();
                                canUndo = canUndo();
                                canRedo = canRedo();
                                undoPresName = undo.getUndoPresentationName();
                                redoPresName = undo.getRedoPresentationName();
                            }
                            if (undoListener != null) {
                                undoListener.undoStateChanged(canUndo, undoPresName);
                                undoListener.redoStateChanged(canRedo, redoPresName);
                            }
                        }
                    }, CompoundUndoManager.IDLE_DELAY_MS);
                }
            }
        });
        synchronized (syntaxLock) {
            if (syntaxLexer != null) {
                highlight = new HighlightThread(syntaxLexer, reader, document, styles);
            }
        }
    }

    private void initStyles() {
        styles = new HashMap<>();
        styles.put(Token.COMMENT, new HighlightStyle(true, false, Constants.TOKEN_COMMENT));
        styles.put(Token.ERROR, new HighlightStyle(false, false, Constants.TOKEN_ERROR));
        styles.put(Token.IDENTIFIER, new HighlightStyle(false, false, Constants.TOKEN_IDENTIFIER));
        styles.put(Token.LABEL, new HighlightStyle(false, false, Constants.TOKEN_LABEL));
        styles.put(Token.LITERAL, new HighlightStyle(false, false, Constants.TOKEN_LITERAL));
        styles.put(Token.OPERATOR, new HighlightStyle(false, true, Constants.TOKEN_OPERATOR));
        styles.put(Token.PREPROCESSOR, new HighlightStyle(false, true, Constants.TOKEN_PREPROCESSOR));
        styles.put(Token.REGISTER, new HighlightStyle(false, false, Constants.TOKEN_REGISTER));
        styles.put(Token.RESERVED, new HighlightStyle(false, true, Constants.TOKEN_RESERVED));
        styles.put(Token.SEPARATOR, new HighlightStyle(false, false, Constants.TOKEN_SEPARATOR));
        this.setFont(Constants.MONOSPACED_PLAIN_12);
        this.setMargin(new Insets(NUMBERS_HEIGHT, NUMBERS_WIDTH, 0, 0));
        this.setBackground(Color.WHITE);
    }

    public void setUndoActionListener(UndoActionListener l) {
        undoListener = l;
    }

    private boolean canRedo() {
        synchronized (undoLock) {
            return undo.canRedo();
        }
    }

    private boolean canUndo() {
        synchronized (undoLock) {
            return undo.canUndo();
        }
    }

    public void undo() {
        boolean canUndo, canRedo;
        String undoPresName, redoPresName;
        synchronized (undoLock) {
            if (highlight != null) {
                highlight.stopMe();
                highlight = null;
            }
            try {
                undo.undo();
            } catch (CannotUndoException e) {
            }
            canUndo = canUndo();
            canRedo = canRedo();
            undoPresName = undo.getUndoPresentationName();
            redoPresName = undo.getRedoPresentationName();
            syntaxStartTimer.cancel();
            syntaxStartTimer = new Timer("SyntaxStartTimer");
            scheduleHighlighter();
        }
        if (undoListener != null) {
            undoListener.undoStateChanged(canUndo, undoPresName);
            undoListener.redoStateChanged(canRedo, redoPresName);
        }
    }

    public void redo() {
        boolean canUndo, canRedo;
        String undoPresName, redoPresName;
        synchronized (undoLock) {
            if (highlight != null) {
                highlight.stopMe();
                highlight = null;
            }
            try {
                undo.redo();
            } catch (CannotRedoException e) {
            }
            canUndo = canUndo();
            canRedo = canRedo();
            undoPresName = undo.getUndoPresentationName();
            redoPresName = undo.getRedoPresentationName();
            syntaxStartTimer.cancel();
            syntaxStartTimer = new Timer("SyntaxStartTimer");
            scheduleHighlighter();
        }
        if (undoListener != null) {
            undoListener.undoStateChanged(canUndo, undoPresName);
            undoListener.redoStateChanged(canRedo, redoPresName);
        }
    }

    private void scheduleHighlighter() {
        syntaxStartTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (syntaxLock) {
                    if (syntaxLexer != null) {
                        highlight = new HighlightThread(syntaxLexer, reader, document, styles);
                        highlight.colorAll();
                    }
                }
            }
        }, CompoundUndoManager.IDLE_DELAY_MS);
    }

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
        // paint thin lines
        g.setColor(Color.PINK);
        g.drawLine(NUMBERS_WIDTH - 5, g.getClipBounds().y, NUMBERS_WIDTH - 5,
            g.getClipBounds().y + g.getClipBounds().height);
    }

    public boolean openFile(String fileName) {
        return openFile(new File(fileName));
    }

    private boolean openFile(File file) {
        fileSource = file;
        if (!fileSource.canRead()) {
            String msg = "File: " + fileSource.getPath() + " cannot be read.";
            logger.error(msg);
            Main.tryShowErrorMessage(msg);
            return false;
        }
        try {
            if (highlight != null) {
                highlight.stopMe();
                highlight = null;
            }
            try (FileReader vstup = new FileReader(fileSource)) {
                setText("");
                getEditorKit().read(vstup, document, 0);
                this.setCaretPosition(0);
            }
            fileSaved = true;

            synchronized (syntaxLock) {
                if (syntaxLexer != null) {
                    highlight = new HighlightThread(syntaxLexer, reader, document,
                            styles);
                    highlight.colorAll();
                }
            }
        } catch (java.io.FileNotFoundException e) {
            logger.error("Could not open file. File not found.", e);
            Main.tryShowErrorMessage("File not found: " + fileSource.getPath());
            return false;
        } catch (IOException | BadLocationException e) {
            logger.error("Could not open file.", e);
            Main.tryShowErrorMessage("Error opening the file: " + fileSource.getPath());
            return false;
        }
        return true;
    }

    public boolean openFileDialog() {
        JFileChooser f = new JFileChooser();
        UniversalFileFilter f1 = new UniversalFileFilter();
        UniversalFileFilter f2 = new UniversalFileFilter();

        if (this.confirmSave()) {
            return false;
        }

        if ((fileExtensions != null) && (fileExtensions.length > 0)) {
            String descr = "Source files (";
            for (SourceFileExtension fileExtension : fileExtensions) {
                f1.addExtension(fileExtension.getExtension());
                descr += "*." + fileExtension.getExtension() + ",";
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

    public boolean confirmSave() {
        if (!fileSaved) {
            int r = JOptionPane.showConfirmDialog(null, "File is not saved yet. Do you want to save the file ?");
            if (r == JOptionPane.YES_OPTION) {
                return (!saveFile(true));
            } else if (r == JOptionPane.CANCEL_OPTION) {
                return true;
            }
        }
        return false;
    }

    public boolean saveFile(boolean showDialogIfFileIsInvalid) {
        if ((fileSource == null) || (fileSource.exists() && (!fileSource.canWrite()))) {
            if (showDialogIfFileIsInvalid) {
                return saveFileDialog();
            } else {
                logger.error("Could not save file, the file is not writable: " + fileSource);
                Main.tryShowErrorMessage("Error: Cannot save the file!\n The selected file is not writable.");
                return false;
            }
        }
        try {
            try (FileWriter vystup = new FileWriter(fileSource)) {
                write(vystup);
            }
            fileSaved = true;
            return true;
        } catch (IOException e) {
            logger.error("Could not save file: " + fileSource.getPath(), e);
            Main.tryShowErrorMessage("Error: Cannot save the file: " +
                fileSource.getPath() + "\n" + e.getLocalizedMessage());
            return false;
        }
    }

    public boolean saveFileDialog() {
        JFileChooser f = new JFileChooser();

        UniversalFileFilter[] filters;
        int tmpLen = (fileExtensions != null) ? fileExtensions.length : 0;

        f.setDialogTitle("Save file");
        f.setAcceptAllFileFilterUsed(false);

        filters = new UniversalFileFilter[tmpLen + 1];
        if ((fileExtensions != null) && (fileExtensions.length > 0)) {
            for (int i = 0; i < fileExtensions.length; i++) {
                filters[i] = new UniversalFileFilter();
                filters[i].addExtension(fileExtensions[i].getExtension());
                filters[i].setDescription(fileExtensions[i].getFormattedDescription());
                f.addChoosableFileFilter(filters[i]);
            }
        }
        filters[filters.length-1] = new UniversalFileFilter();
        filters[filters.length-1].addExtension("*");
        filters[filters.length-1].setDescription("All files (*.*)");
        f.addChoosableFileFilter(filters[filters.length-1]);
        f.setFileFilter(filters[0]);
        f.setApproveButtonText("Save");
        f.setSelectedFile(fileSource);
        if (fileSource != null) {
            f.setCurrentDirectory(fileSource.getParentFile());
        } else {
            f.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }
        f.setSelectedFile(null);

        int returnVal = f.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File selectedFile = f.getSelectedFile();
        UniversalFileFilter selectedFileFilter = (UniversalFileFilter)f.getFileFilter();

        String suffix = selectedFileFilter.getFirstExtension();
        if (!suffix.equals("*") &&
                selectedFile.getName().toLowerCase().endsWith("." + suffix.toLowerCase())) {
            fileSource = selectedFile;
        } else {
            fileSource = new File(selectedFile.getAbsolutePath() + "." + suffix.toLowerCase());
        }
        return saveFile(false);
    }

    public boolean isFileSaved() {
        return fileSaved  && (fileSource != null);
    }

    public String getFileName() {
        if (fileSource == null) {
            return null;
        } else {
            return fileSource.getAbsolutePath();
        }
    }

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
