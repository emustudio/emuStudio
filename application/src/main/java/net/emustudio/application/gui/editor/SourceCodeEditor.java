/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.editor;

import net.emustudio.application.Constants;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceFileExtension;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is extended JTextPane class. Support some awesome features like
 * line numbering or syntax highlighting and other.
 * TODO: add ability to set breakpoints
 */
public class SourceCodeEditor extends JTextPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceCodeEditor.class);

    private static final Map<Integer, HighlightStyle> tokenStyles = new HashMap<>();

    private static final short NUMBERS_WIDTH = 40;
    private static final short NUMBERS_HEIGHT = 4;

    private final Dialogs dialogs;

    private final HighLightedDocument document = new HighLightedDocument();
    private final DocumentReader reader = new DocumentReader(document);

    private final CompoundUndoManager undo = new CompoundUndoManager();
    private UndoActionListener undoListener;
    private Timer undoTimer = new Timer("UndoTimer");
    private final Object undoLock = new Object();

    private Timer syntaxStartTimer = new Timer("SyntaxStartTimer");
    private final Object syntaxLock = new Object();
    private final ViewFactory htmlFactory = new Workaround6606443ViewFactory();
    private LexicalAnalyzer syntaxLexer;
    private HighlightThread highlight;

    private final List<SourceFileExtension> sourceFileExtensions;

    private boolean fileSaved = true;
    private File fileSource;


    static {
        tokenStyles.put(Token.COMMENT, new HighlightStyle(true, false, Constants.TOKEN_COMMENT));
        tokenStyles.put(Token.ERROR, new HighlightStyle(false, false, Constants.TOKEN_ERROR));
        tokenStyles.put(Token.IDENTIFIER, new HighlightStyle(false, false, Constants.TOKEN_IDENTIFIER));
        tokenStyles.put(Token.LABEL, new HighlightStyle(false, false, Constants.TOKEN_LABEL));
        tokenStyles.put(Token.LITERAL, new HighlightStyle(false, false, Constants.TOKEN_LITERAL));
        tokenStyles.put(Token.OPERATOR, new HighlightStyle(false, true, Constants.TOKEN_OPERATOR));
        tokenStyles.put(Token.PREPROCESSOR, new HighlightStyle(false, true, Constants.TOKEN_PREPROCESSOR));
        tokenStyles.put(Token.REGISTER, new HighlightStyle(false, false, Constants.TOKEN_REGISTER));
        tokenStyles.put(Token.RESERVED, new HighlightStyle(false, true, Constants.TOKEN_RESERVED));
        tokenStyles.put(Token.SEPARATOR, new HighlightStyle(false, false, Constants.TOKEN_SEPARATOR));
    }


    public SourceCodeEditor(Compiler compiler, Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
        document.setDocumentReader(reader);

        if (compiler != null) {
            sourceFileExtensions = Objects.requireNonNull(compiler.getSourceFileExtensions());
            this.syntaxLexer = compiler.getLexer(reader);
        } else {
            sourceFileExtensions = Collections.emptyList();
        }

        setFont(Constants.MONOSPACED_PLAIN_12);
        setMargin(new Insets(NUMBERS_HEIGHT, NUMBERS_WIDTH, 0, 0));
        setBackground(Color.WHITE);
        setEditorKit(new StyledEditorKit() {

            @Override
            public ViewFactory getViewFactory() {
                return htmlFactory;
            }
        });
        setStyledDocument(document);

        document.addUndoableEditListener(new UndoableEditListener() {

            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                UndoableEdit ed = e.getEdit();

                Optional<DefaultDocumentEvent> event = Optional.empty();
                if (ed instanceof DefaultDocumentEvent) {
                    event = Optional.of((DefaultDocumentEvent) ed);
                } else {
                    // it is a private AbstractDocument.DefaultDocumentEventUndoableWrapper ... :/
                    try {
                        Field[] fields = ed.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            if (field.getType() == DefaultDocumentEvent.class) {
                                field.setAccessible(true);
                                event = Optional.of((DefaultDocumentEvent) field.get(ed));
                                break;
                            }
                        }
                    } catch (IllegalAccessException ex) {
                        LOGGER.error("Could not access DefaultDocumentEvent", ex);
                    }
                }
                if (event.filter(evt -> evt.getType().equals(DocumentEvent.EventType.CHANGE)).isPresent()) {
                    return;
                }
                if (event.isPresent() && ed.isSignificant()) {
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
                highlight = new HighlightThread(syntaxLexer, reader, document, tokenStyles);
            }
        }
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
            } catch (CannotUndoException ignored) {
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
            } catch (CannotRedoException ignored) {
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
                        highlight = new HighlightThread(syntaxLexer, reader, document, tokenStyles);
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

    public void openFile(String fileName) {
        openFile(new File(fileName));
    }

    private boolean openFile(File file) {
        fileSource = file;
        if (!fileSource.canRead()) {
            dialogs.showError("File: " + fileSource.getPath() + " cannot be read (unknown reason).", "Open file");
            return false;
        }
        try {
            if (highlight != null) {
                highlight.stopMe();
                highlight = null;
            }
            try (FileReader fileReader = new FileReader(fileSource)) {
                setText("");
                getEditorKit().read(fileReader, document, 0);
                this.setCaretPosition(0);
            }
            fileSaved = true;

            synchronized (syntaxLock) {
                if (syntaxLexer != null) {
                    highlight = new HighlightThread(syntaxLexer, reader, document, tokenStyles);
                    highlight.colorAll();
                }
            }
        } catch (java.io.FileNotFoundException e) {
            dialogs.showError("File not found: " + fileSource.getPath(), "Open file");
            return false;
        } catch (IOException | BadLocationException e) {
            LOGGER.error("Could not open file.", e);
            dialogs.showError("Error opening the file: " + fileSource.getPath() + ". Please see log file for details.");
            return false;
        }
        return true;
    }

    public boolean openFileDialog() {
        if (this.confirmSaveAndSaved()) {
            List<FileExtensionsFilter> filters = sourceFileExtensions.stream()
                .map(FileExtensionsFilter::new).collect(Collectors.toList());

            List<String> sourceExtensions = sourceFileExtensions.stream()
                .map(SourceFileExtension::getExtension).collect(Collectors.toList());
            filters.add(new FileExtensionsFilter("All source files", sourceExtensions));

            File currentDirectory = Objects.requireNonNullElse(fileSource, new File(System.getProperty("user.dir")));
            Optional<Path> openedFile = dialogs.chooseFile("Open a file", "Open", currentDirectory.toPath(), filters);
            if (openedFile.isPresent()) {
                fileSource = openedFile.get().toFile();
                return openFile(fileSource);
            }
        }
        return false;
    }

    public boolean confirmSaveAndSaved() {
        if (!fileSaved) {
            Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
            if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                return (saveFile(true));
            } else return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
        }
        return true;
    }

    public boolean saveFile(boolean showDialogIfFileIsInvalid) {
        if ((fileSource == null) || (fileSource.exists() && (!fileSource.canWrite()))) {
            if (showDialogIfFileIsInvalid) {
                return saveFileDialog();
            } else {
                dialogs.showError("Cannot save current file (either the file is not selected or it is not writable).");
                return false;
            }
        }
        try {
            try (FileWriter fileWriter = new FileWriter(fileSource)) {
                write(fileWriter);
            }
            fileSaved = true;
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not save file: " + fileSource.getPath(), e);
            dialogs.showError("Cannot save current file. Please see log file for details.");
            return false;
        }
    }

    public boolean saveFileDialog() {
        List<FileExtensionsFilter> filters = sourceFileExtensions.stream()
            .map(FileExtensionsFilter::new).collect(Collectors.toList());

        List<String> sourceExtensions = sourceFileExtensions.stream()
            .map(SourceFileExtension::getExtension).collect(Collectors.toList());
        filters.add(new FileExtensionsFilter("All source files", sourceExtensions));

        File currentDirectory = Objects.requireNonNullElse(fileSource, new File(System.getProperty("user.dir")));
        Optional<Path> savedPath = dialogs.chooseFile("Save file", "Save", currentDirectory.toPath(), filters);
        if (savedPath.isPresent()) {
            // TODO: check if suffix is present
            fileSource = savedPath.get().toFile();
            return saveFile(false);
        }
        return false;
    }

    public Optional<File> getCurrentFile() {
        return Optional.ofNullable(fileSource);
    }

    public void newFile() {
        if (confirmSaveAndSaved()) {
            fileSource = null;
            fileSaved = true;
            this.setText("");
        }
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
                    LOGGER.error("Could not set custom FlowStrategy in editor pane", e);
                }
            }
            return view;
        }
    }

    public interface UndoActionListener {
        void undoStateChanged(boolean canUndo, String presentationName);

        void redoStateChanged(boolean canRedo, String presentationName);
    }
}
