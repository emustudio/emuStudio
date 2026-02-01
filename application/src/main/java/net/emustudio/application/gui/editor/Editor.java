/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.fife.rsta.ui.search.SearchListener;

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
     *
     * @param position position in the source code
     */
    void setPosition(SourceCodePosition position);


    Optional<File> getCurrentFile();
}
