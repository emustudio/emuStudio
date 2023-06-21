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
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.plugins.device.audiotape_player.loaders.Loader;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotThreadSafe
public class TapesListModel extends DefaultListModel<String> {
    private final static Logger LOGGER = LoggerFactory.getLogger(TapesListModel.class);

    private List<PathString> files = Collections.emptyList();
    private Path directory;
    private final AtomicReference<Component> componentRef = new AtomicReference<>();
    private int componentWidth;

    public void refresh() {
        reset(directory);
    }

    public void reset(Path directory) {
        this.directory = directory;
        clear();
        this.files = listPaths(directory);
        Component c = componentRef.get();
        for (PathString file : files) {
            if (c != null) {
                file.deriveMaxStringLength(c, componentWidth);
            }
            addElement(file.getPathShortened());
        }
    }

    public Path getFilePath(int index) {
        return files.get(index).getPath();
    }

    private static List<PathString> listPaths(Path directory) {
        if (directory == null) {
            return Collections.emptyList();
        }
        try(Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isReadable)
                    .filter(Loader::hasLoader)
                    .map(PathString::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Could not load tape files from directory: " + directory, e);
        }
        return Collections.emptyList();
    }

    public void resize(Component component, int width) {
        componentRef.set(component);
        componentWidth = width;
        refresh();
    }
}
