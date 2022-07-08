package net.emustudio.application.gui.editor;

import org.fife.rsta.ui.search.SearchListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public interface Editor extends SearchListener {

    void newFile();

    boolean openFile();

    boolean openFile(Path fileName);

    boolean saveFile();

    boolean saveFileAs();

    boolean isDirty();


    Optional<Boolean> findNext();

    Optional<Boolean> findPrevious();

    void clearMarkedOccurences();


    Component getView();

    void grabFocus();

    /**
     * Set caret position.
     * @param line line (if -1 does nothing)
     * @param column column (if -1 only sets the line)
     */
    void setPosition(int line, int column);


    Optional<File> getCurrentFile();
}
