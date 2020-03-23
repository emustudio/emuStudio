package net.emustudio.application.gui.editor;

import org.fife.rsta.ui.search.SearchListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Optional;

public interface Editor extends SearchListener {

    void newFile();

    boolean openFile();

    boolean openFile(String fileName);

    boolean saveFile();

    boolean saveFileAs();

    boolean isDirty();


    Optional<Boolean> findNext();

    Optional<Boolean> findPrevious();


    Component getView();

    JComponent getErrorStrip();

    void grabFocus();


    Optional<File> getCurrentFile();
}
