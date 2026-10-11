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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static net.emustudio.application.Constants.FONT_CODE;

public class REditor implements Editor {
    private final static Logger LOGGER = LoggerFactory.getLogger(REditor.class);
    private final TextEditorPane textPane = new EditingTextPane();
    private final RTextScrollPane scrollPane = new RTextScrollPane(textPane);
    private final List<DocumentListener> changeListeners = new ArrayList<>();

    private final Dialogs dialogs;
    private final List<FileExtension> fileExtensions;
    private final Compiler compiler;
    private final EditorCommands commands;
    private REditor sharedWith;
    private Consumer<Path> openFileHandler = path -> {
    };
    private boolean isnew = true;
    private SearchContext lastSearchedContext;

    public REditor(Dialogs dialogs) {
        this(dialogs, null);
    }

    @SuppressWarnings("unused")
    public REditor(Dialogs dialogs, Compiler compiler) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.compiler = compiler;

        UnicodeWriter.setWriteUtf8BOM(false);

        textPane.setFont(FONT_CODE);

        textPane.setEncoding(StandardCharsets.UTF_8.name());
        textPane.setAutoIndentEnabled(true);
        textPane.setBracketMatchingEnabled(true);
        textPane.setAntiAliasingEnabled(true);
        textPane.clearParsers();
        FoldSupport.install(textPane, compiler);
        scrollPane.setFoldIndicatorEnabled(true);

        textPane.setCaretPosition(0);
        textPane.requestFocusInWindow();
        textPane.setMarkOccurrences(true);
        textPane.setClearWhitespaceLinesEnabled(false);
        scrollPane.setIconRowHeaderEnabled(true);

        // CTRL+{mouse wheel} zooms text
        MouseWheelListener[] listeners = scrollPane.getMouseWheelListeners();
        for (MouseWheelListener listener : listeners) {
            scrollPane.removeMouseWheelListener(listener);
        }

        scrollPane.addMouseWheelListener(e -> {
            int wheelRotation = e.getWheelRotation();
            if (wheelRotation != 0 && (e.getModifiersEx() & CTRL_DOWN_MASK) != 0) {
                setFontSize(getFontSize() + (wheelRotation > 0 ? -1 : 1));
                e.consume();
            } else {
                for (MouseWheelListener listener : listeners) {
                    listener.mouseWheelMoved(e);
                }
            }
        });

        bindFontZoom("zoomIn", 1,
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, CTRL_DOWN_MASK));
        bindFontZoom("zoomOut", -1,
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, CTRL_DOWN_MASK));

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearMarkedOccurences();
                }
            }
        });
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) {
                    int offset = textPane.viewToModel2D(e.getPoint());
                    if (offset >= 0) {
                        openIncludeAt(offset);
                        e.consume();
                    }
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
        commands = new EditorCommands(this, textPane, scrollPane, compiler);
    }

    TextEditorPane textPane() { return textPane; }
    Compiler compiler() { return compiler; }
    REditor sourceEditor() { return sharedWith == null ? this : sharedWith; }
    EditorCommands commands() { return commands; }
    void configure(EditorState state) { commands.configure(state); }

    REditor mirror() {
        REditor mirror = new REditor(dialogs, compiler);
        mirror.sharedWith = this;
        mirror.textPane.setDocument(textPane.getDocument());
        mirror.commands.selections().installFilter();
        mirror.configure(commands.state());
        mirror.setOpenFileHandler(openFileHandler);
        return mirror;
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
    public float getFontSize() {
        return textPane.getFont().getSize2D();
    }

    @Override
    public void setFontSize(float size) {
        float minimum = FONT_CODE.getSize2D() * MIN_FONT_ZOOM / 100.0f;
        float maximum = FONT_CODE.getSize2D() * MAX_FONT_ZOOM / 100.0f;
        textPane.setFont(textPane.getFont().deriveFont(Math.max(minimum, Math.min(size, maximum))));
    }

    @Override
    public void addFontSizeChangeListener(Runnable listener) {
        Objects.requireNonNull(listener);
        textPane.addPropertyChangeListener("font", e -> listener.run());
    }

    private void bindFontZoom(String name, int step, KeyStroke... keys) {
        textPane.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFontSize(getFontSize() + step);
            }
        });
        for (KeyStroke key : keys) {
            textPane.getInputMap().put(key, name);
        }
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
        if (sharedWith != null) return sharedWith.isDirty();
        return textPane.isDirty();
    }

    @Override
    public boolean saveFile() {
        if (sharedWith != null) return sharedWith.saveFile();
        Optional<File> fileSource = Optional.ofNullable(textPane.getFileFullPath()).map(File::new);
        if (isnew || fileSource.isEmpty() || fileSource.filter(File::canWrite).isEmpty()) {
            return saveFileAs();
        } else {
            try {
                textPane.save();
                commands.persistBookmarks();
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
        if (sharedWith != null) return sharedWith.getCurrentFile();
        return isnew ? Optional.empty() : Optional.ofNullable(textPane.getFileFullPath()).map(File::new);
    }

    @Override
    public boolean saveFileAs() {
        if (sharedWith != null) return sharedWith.saveFileAs();
        Optional<Path> savedPath = dialogs.chooseFile("Save file", "Save", getCurrentBaseDirectory(), true, saveFilters());
        if (savedPath.isPresent()) {
            try {
                textPane.saveAs(FileLocation.create(savedPath.get().toFile()));
                isnew = false;
                commands.persistBookmarks();
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
        return chooseFileToOpen().map(this::openFile).orElse(false);
    }

    Optional<Path> chooseFileToOpen() {
        return dialogs.chooseFile(
                "Open a file", "Open", getCurrentBaseDirectory(), false, openFilters()
        );
    }

    void addChangeListener(Runnable listener) {
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.run();
            }
        };
        changeListeners.add(documentListener);
        textPane.getDocument().addDocumentListener(documentListener);
    }

    void dispose() {
        changeListeners.forEach(textPane.getDocument()::removeDocumentListener);
        changeListeners.clear();
        commands.selections().clear();
        if (textPane.getDocument().getProperty("activeSelections") == commands.selections()) {
            textPane.getDocument().putProperty("activeSelections", null);
        }
        // Detach Swing's document, undo and syntax listeners from a shared split document.
        textPane.setDocument(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE));
    }

    void setOpenFileHandler(Consumer<Path> openFileHandler) {
        this.openFileHandler = Objects.requireNonNull(openFileHandler);
    }

    void openIncludeAt(int offset) {
        resolveIncludeAt(offset).ifPresent(path -> {
            if (Files.isRegularFile(path)) {
                textPane.setToolTipText(null);
                openFileHandler.accept(path);
            } else {
                textPane.setToolTipText("File not found: " + path);
            }
        });
    }

    private Optional<Path> resolveIncludeAt(int offset) {
        String filename = null;
        if (compiler != null) {
            net.emustudio.emulib.plugins.compiler.LexicalAnalyzer lexer = compiler.createLexer();
            char[] source = textPane.getText().toCharArray(); lexer.reset(source, 0, source.length);
            while (lexer.hasNext()) {
                net.emustudio.emulib.plugins.compiler.Token token = lexer.next();
                if (token.getReferencedFile() != null && offset >= token.getOffset()
                        && offset < token.getOffset() + token.getText().length()) {
                    filename = token.getReferencedFile(); break;
                }
            }
        } else {
            // Plain text can open any quoted relative file path, without language keywords.
            Matcher matcher = Pattern.compile("([\"'])([^\"']+)\\1").matcher(textPane.getText());
            while (matcher.find()) if (offset >= matcher.start(2) && offset < matcher.end(2)) { filename = matcher.group(2); break; }
        }
        if (filename == null) return Optional.empty();
        final String path = filename;
        try {
            return getCurrentFile().map(File::toPath).map(Path::getParent).map(parent -> parent.resolve(path).normalize());
        } catch (InvalidPathException e) { return Optional.empty(); }
    }

    @Override
    public boolean openFile(Path fileName) {
        try {
            textPane.load(FileLocation.create(fileName.toString()));
            textPane.discardAllEdits();
            isnew = false;
            commands.restoreBookmarks();
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
        commands.clearBookmarks();
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
