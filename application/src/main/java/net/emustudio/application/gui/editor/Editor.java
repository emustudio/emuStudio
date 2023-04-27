/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
