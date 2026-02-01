/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.emulib.runtime.ui.ShortenedString;
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

    private List<ShortenedString<Path>> files = Collections.emptyList();
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
        for (ShortenedString<Path> file : files) {
            if (c != null) {
                file.deriveMaxStringLength(c, componentWidth);
            }
            addElement(file.getShortenedString());
        }
    }

    public Path getFilePath(int index) {
        return files.get(index).getValue();
    }

    private static List<ShortenedString<Path>> listPaths(Path directory) {
        if (directory == null) {
            return Collections.emptyList();
        }
        try(Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isReadable)
                    .filter(Loader::hasLoader)
                    .map(p -> new ShortenedString<>(p, pp -> pp.getFileName().toString()))
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
