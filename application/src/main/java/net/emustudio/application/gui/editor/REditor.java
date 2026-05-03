/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.Constants;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import org.fife.io.UnicodeWriter;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static net.emustudio.application.Constants.FONT_CODE;
import static net.emustudio.emulib.runtime.ui.Constants.FONT_DEFAULT_SIZE;

public class REditor implements Editor {
    private final static Logger LOGGER = LoggerFactory.getLogger(REditor.class);

    private final TextEditorPane textPane = new TextEditorPane(RTextArea.INSERT_MODE);
    private final RTextScrollPane scrollPane = new RTextScrollPane(textPane);

    private final Dialogs dialogs;
    private final List<FileExtension> fileExtensions;
    private boolean isnew = true;
    private SearchContext lastSearchedContext;

    public REditor(Dialogs dialogs) {
        this(dialogs, null);
    }

    @SuppressWarnings("unused")
    public REditor(Dialogs dialogs, Compiler compiler) {
        this.dialogs = Objects.requireNonNull(dialogs);

        UnicodeWriter.setWriteUtf8BOM(false);

        textPane.setFont(FONT_CODE);

        textPane.setCodeFoldingEnabled(false);
        textPane.setEncoding(StandardCharsets.UTF_8.name());
        textPane.setAutoIndentEnabled(true);
        textPane.setBracketMatchingEnabled(true);
        textPane.setAntiAliasingEnabled(true);
        textPane.clearParsers();

        textPane.setCaretPosition(0);
        textPane.requestFocusInWindow();
        textPane.setMarkOccurrences(true);
        textPane.setClearWhitespaceLinesEnabled(false);

        // CTRL+{mouse wheel} zooms text
        MouseWheelListener[] listeners = scrollPane.getMouseWheelListeners();
        for (MouseWheelListener listener : listeners) {
            scrollPane.removeMouseWheelListener(listener);
        }

        scrollPane.addMouseWheelListener(e -> {
            int wheelRotation = e.getWheelRotation();
            if (wheelRotation != 0 && (e.getModifiersEx() & CTRL_DOWN_MASK) != 0) {
                Font font = textPane.getFont();
                int currentSize = font.getSize();
                float newSize = (wheelRotation > 0) ? Math.max(currentSize - 1, FONT_DEFAULT_SIZE) : (currentSize + 1);
                textPane.setFont(font.deriveFont(newSize));
                e.consume();
            } else {
                for (MouseWheelListener listener : listeners) {
                    listener.mouseWheelMoved(e);
                }
            }
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearMarkedOccurences();
                }
            }
        });
        setupSyntaxTheme();

        if (compiler != null) {
            fileExtensions = List.copyOf(compiler.getSourceFileExtensions());
            ((RSyntaxDocument) textPane.getDocument()).setSyntaxStyle(new RTokenMaker(compiler));
        } else {
            fileExtensions = Collections.emptyList();
        }
        textPane.setDirty(false);
    }

    @Override
    public Component getView() {
        return scrollPane;
    }

    @Override
    public void clearMarkedOccurences() {
        SearchEngine.find(textPane, new SearchContext());
    }

    @Override
    public void grabFocus() {
        textPane.grabFocus();
    }

    @Override
    public void setPosition(SourceCodePosition position) {
        if (position.line >= 0) {
            try {
                int offset = textPane.getLineStartOffset(Math.max(0, position.line - 1));
                if (position.column >= 0) {
                    offset += position.column;
                }
                textPane.setCaretPosition(offset);
            } catch (BadLocationException ignored) {
            }
        }
    }

    @Override
    public boolean isDirty() {
        return textPane.isDirty();
    }

    @Override
    public boolean saveFile() {
        Optional<File> fileSource = Optional.ofNullable(textPane.getFileFullPath()).map(File::new);
        if (isnew || fileSource.isEmpty() || fileSource.filter(File::canWrite).isEmpty()) {
            return saveFileAs();
        } else {
            try {
                textPane.save();
                isnew = false;
                return true;
            } catch (IOException e) {
                LOGGER.error("Could not save file: {}", fileSource.get().getPath(), e);
                dialogs.showError("Cannot save current file. Please see log file for details.");
                return false;
            }
        }
    }

    @Override
    public Optional<File> getCurrentFile() {
        return isnew ? Optional.empty() : Optional.ofNullable(textPane.getFileFullPath()).map(File::new);
    }

    @Override
    public boolean saveFileAs() {
        Optional<Path> savedPath = dialogs.chooseFile("Save file", "Save", getCurrentBaseDirectory(), true, saveFilters());
        if (savedPath.isPresent()) {
            try {
                textPane.saveAs(FileLocation.create(savedPath.get().toFile()));
                isnew = false;
                return true;
            } catch (IOException e) {
                LOGGER.error("Could not save file: {}", savedPath.get(), e);
                dialogs.showError("Cannot save current file. Please see log file for details.");
            }
        }
        return false;
    }

    @Override
    public boolean openFile() {
        Optional<Path> openedFile = dialogs.chooseFile(
                "Open a file", "Open", getCurrentBaseDirectory(), false, openFilters()
        );
        return openedFile.map(this::openFile).orElse(false);
    }

    @Override
    public boolean openFile(Path fileName) {
        try {
            textPane.load(FileLocation.create(fileName.toString()));
            textPane.discardAllEdits();
            isnew = false;
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not open file.", e);
            dialogs.showError("Could not open file: " + fileName + ". Please see log file for details.");
            return false;
        }
    }

    @Override
    public void newFile() {
        textPane.setText("");
        textPane.discardAllEdits();
        textPane.setDirty(false);
        isnew = true;
    }

    @Override
    public void searchEvent(SearchEvent e) {
        SearchEvent.Type type = e.getType();
        SearchContext context = e.getSearchContext();
        SearchResult result;

        lastSearchedContext = context.clone();
        switch (type) {
            case MARK_ALL:
                SearchEngine.markAll(textPane, context);
                break;
            case FIND:
                result = SearchEngine.find(textPane, context);
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(textPane);
                }
                break;
            case REPLACE:
                result = SearchEngine.replace(textPane, context);
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(textPane);
                }
                break;
            case REPLACE_ALL:
                result = SearchEngine.replaceAll(textPane, context);
                dialogs.showInfo(result.getCount() + " occurrences replaced.", "Replace all");
                break;
            default:
                break;
        }
    }


    @Override
    public String getSelectedText() {
        return textPane.getSelectedText();
    }

    @Override
    public Optional<Boolean> findNext() {
        return Optional.ofNullable(lastSearchedContext).map(context -> {
            context.setSearchForward(true);
            context.setMarkAll(false);
            return SearchEngine.find(textPane, context).wasFound();
        });
    }

    @Override
    public Optional<Boolean> findPrevious() {
        return Optional.ofNullable(lastSearchedContext).map(context -> {
            context.setSearchForward(false);
            context.setMarkAll(false);
            return SearchEngine.find(textPane, context).wasFound();
        });
    }

    private Path getCurrentBaseDirectory() {
        return Optional.ofNullable(textPane.getFileFullPath())
                .filter(path -> !isnew)
                .map(Path::of)
                .map(Path::getParent)
                .orElseGet(() -> Path.of(System.getProperty("user.dir")));
    }

    private List<FileExtensionsFilter> saveFilters() {
        return fileExtensions.stream().map(FileExtensionsFilter::new).collect(Collectors.toList());
    }

    private List<FileExtensionsFilter> openFilters() {
        List<String> sourceExtensions = fileExtensions.stream()
                .map(FileExtension::getExtension)
                .collect(Collectors.toList());

        if (sourceExtensions.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(new FileExtensionsFilter("All source files", sourceExtensions));
    }

    private void setupSyntaxTheme() {
        SyntaxScheme scheme = textPane.getSyntaxScheme();
        scheme.getStyle(Token.COMMENT_MARKUP).foreground = Constants.TOKEN_COMMENT;
        scheme.getStyle(Token.RESERVED_WORD).foreground = Constants.TOKEN_RESERVED;
        scheme.getStyle(Token.IDENTIFIER).foreground = Constants.TOKEN_IDENTIFIER;
        scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.LITERAL_BACKQUOTE).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.LITERAL_CHAR).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.LITERAL_NUMBER_HEXADECIMAL).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = Constants.TOKEN_LITERAL;
        scheme.getStyle(Token.ANNOTATION).foreground = Constants.TOKEN_LABEL;
        scheme.getStyle(Token.RESERVED_WORD_2).foreground = Constants.TOKEN_REGISTER;
        scheme.getStyle(Token.PREPROCESSOR).foreground = Constants.TOKEN_PREPROCESSOR;
        scheme.getStyle(Token.SEPARATOR).foreground = Constants.TOKEN_SEPARATOR;
        scheme.getStyle(Token.OPERATOR).foreground = Constants.TOKEN_OPERATOR;
        scheme.getStyle(Token.ERROR_IDENTIFIER).foreground = Constants.TOKEN_ERROR;
        scheme.getStyle(Token.ERROR_CHAR).foreground = Constants.TOKEN_ERROR;
        scheme.getStyle(Token.ERROR_NUMBER_FORMAT).foreground = Constants.TOKEN_ERROR;
        scheme.getStyle(Token.ERROR_STRING_DOUBLE).foreground = Constants.TOKEN_ERROR;
    }
}
