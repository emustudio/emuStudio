package net.emustudio.application.gui.editor;


import javax.swing.text.Document;
import java.awt.*;
import java.io.File;
import java.util.Optional;

public interface Editor {

    void newFile();

    boolean openFile();

    boolean openFile(String fileName);

    boolean saveFile();

    boolean saveFileAs();

    boolean isDirty();

    String getText();


    int getCaretPosition();

    void setCaretPosition(int position);

    void select(int start, int end);


    Component getView();

    void grabFocus();


    Optional<File> getCurrentFile();


    Document getDocument();
}
