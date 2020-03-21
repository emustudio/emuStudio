package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceFileExtension;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.Document;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class REditor implements Editor {
    private final static Logger LOGGER = LoggerFactory.getLogger(REditor.class);

    private final TextEditorPane textPane = new TextEditorPane(RTextArea.INSERT_MODE, true);
    private final Dialogs dialogs;
    private final List<SourceFileExtension> sourceFileExtensions = Collections.emptyList();

    public REditor(Dialogs dialogs) {
        this(dialogs, null);
    }

    @SuppressWarnings("unused")
    public REditor(Dialogs dialogs, LexicalAnalyzer lexicalAnalyzer) {
        this.dialogs = Objects.requireNonNull(dialogs);

        textPane.setCodeFoldingEnabled(true);
        textPane.setAnimateBracketMatching(true);
        textPane.setAutoIndentEnabled(true);
        textPane.setBracketMatchingEnabled(true);
        textPane.setAntiAliasingEnabled(true);
        textPane.clearParsers();
        Optional.ofNullable(lexicalAnalyzer).ifPresent(lex -> {
            RTokenMakerWrapper unusedButUseful = new RTokenMakerWrapper(lex);

            AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
            atmf.putMapping("text/emustudio", RTokenMakerWrapper.class.getName());
            textPane.setSyntaxEditingStyle("text/emustudio");
        });
    }

    @Override
    public Component getView() {
        return textPane;
    }

    @Override
    public void grabFocus() {
        textPane.grabFocus();
    }

    @Override
    public boolean saveFile(boolean showDialogIfFileIsInvalid) {
        Optional<File> fileSource = Optional.ofNullable(textPane.getFileFullPath()).map(File::new);
        if (fileSource.isEmpty() || fileSource.filter(f -> f.exists() && f.canWrite()).isEmpty()) {
            if (showDialogIfFileIsInvalid) {
                return saveFile();
            } else {
                dialogs.showError("Cannot save current file (either no file is selected or the file is not writable).");
                return false;
            }
        } else {
            try {
                textPane.save();
                return true;
            } catch (IOException e) {
                LOGGER.error("Could not save file: " + fileSource.get().getPath(), e);
                dialogs.showError("Cannot save current file. Please see log file for details.");
                return false;
            }
        }
    }

    @Override
    public Optional<File> getCurrentFile() {
        return Optional.ofNullable(textPane.getFileFullPath()).map(File::new);
    }

    @Override
    public boolean saveFile() {
        java.util.List<FileExtensionsFilter> filters = sourceFileExtensions.stream()
            .map(FileExtensionsFilter::new).collect(Collectors.toList());

        List<String> sourceExtensions = sourceFileExtensions.stream()
            .map(SourceFileExtension::getExtension).collect(Collectors.toList());
        filters.add(new FileExtensionsFilter("All source files", sourceExtensions));

        File currentDirectory = Optional
            .ofNullable(textPane.getFileFullPath())
            .map(File::new)
            .orElse(new File(System.getProperty("user.dir")));

        Optional<Path> savedPath = dialogs.chooseFile("Save file", "Save", currentDirectory.toPath(), filters);
        if (savedPath.isPresent()) {
            try {
                textPane.saveAs(FileLocation.create(savedPath.get().toFile()));
                return saveFile(false);
            } catch (IOException e) {
                LOGGER.error("Could not save file: " + savedPath.get().toString(), e);
                dialogs.showError("Cannot save current file. Please see log file for details.");
            }
        }
        return false;
    }

    @Override
    public boolean saveFileWithConfirmation() {
        if (textPane.isDirty()) {
            Dialogs.DialogAnswer answer = dialogs.ask("File is not saved yet. Do you want to save it?");
            if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                return (saveFile(true));
            } else return answer != Dialogs.DialogAnswer.ANSWER_CANCEL;
        }
        return true;
    }

    @Override
    public void openFile() {
        if (this.saveFileWithConfirmation()) {
            List<FileExtensionsFilter> filters = sourceFileExtensions.stream()
                .map(FileExtensionsFilter::new).collect(Collectors.toList());

            List<String> sourceExtensions = sourceFileExtensions.stream()
                .map(SourceFileExtension::getExtension).collect(Collectors.toList());
            if (sourceExtensions.size() > 1) {
                filters.add(new FileExtensionsFilter("All source files", sourceExtensions));
            }

            File currentDirectory = Optional
                .ofNullable(textPane.getFileFullPath())
                .map(File::new)
                .orElse(new File(System.getProperty("user.dir")));

            Optional<Path> openedFile = dialogs.chooseFile("Open a file", "Open", currentDirectory.toPath(), filters);
            openedFile.ifPresent(path -> openFile(path.toString()));
        }
    }

    @Override
    public void openFile(String fileName) {
        try {
            textPane.load(FileLocation.create(fileName));
        } catch (IOException e) {
            LOGGER.error("Could not open file.", e);
            dialogs.showError("Could not open file: " + fileName + ". Please see log file for details.");
        }
    }

    @Override
    public void newFile() {
        textPane.setText("");
    }

    @Override
    public String getText() {
        return textPane.getText();
    }

    @Override
    public int getCaretPosition() {
        return textPane.getCaretPosition();
    }

    @Override
    public void setCaretPosition(int position) {
        textPane.setCaretPosition(position);
    }

    @Override
    public Document getDocument() {
        return textPane.getDocument();
    }

    @Override
    public void select(int start, int end) {
        textPane.select(start, end);
    }
}
