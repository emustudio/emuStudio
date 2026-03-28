/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class BrowseButtonTest extends AbstractSwingTest {

    @Test
    public void directoryButtonUsesMostRecentlySelectedDirectoryAsBasePath() {
        Dialogs dialogs = mock(Dialogs.class);
        Path initialDirectory = Path.of(System.getProperty("user.dir"));
        Path selectedDirectory = Path.of("/tmp/selected-directory");
        AtomicReference<Path> approvedPath = new AtomicReference<>();

        when(dialogs.chooseDirectory("Select directory", "Open", initialDirectory)).thenReturn(Optional.of(selectedDirectory));
        when(dialogs.chooseDirectory("Select directory", "Open", selectedDirectory)).thenReturn(Optional.empty());

        BrowseButton button = onEdt(
                () -> new BrowseButton(dialogs, "Select directory", "Open", approvedPath::set)
        );

        showInFrame(button);
        triggerButton(button);
        triggerButton(button);

        assertEquals(selectedDirectory, approvedPath.get());
        verify(dialogs).chooseDirectory("Select directory", "Open", initialDirectory);
        verify(dialogs).chooseDirectory("Select directory", "Open", selectedDirectory);
    }

    @Test
    public void fileButtonUsesMostRecentlySelectedFileAsBasePath() {
        Dialogs dialogs = mock(Dialogs.class);
        Path initialDirectory = Path.of(System.getProperty("user.dir"));
        Path selectedFile = Path.of("/tmp/settings.toml");
        AtomicReference<Path> approvedPath = new AtomicReference<>();
        FileExtensionsFilter filter = new FileExtensionsFilter("Configuration", "toml");

        when(dialogs.chooseFile("Select file", "Open", initialDirectory, true, filter)).thenReturn(Optional.of(selectedFile));
        when(dialogs.chooseFile("Select file", "Open", selectedFile, true, filter)).thenReturn(Optional.empty());

        BrowseButton button = onEdt(
                () -> new BrowseButton(dialogs, "Select file", "Open", true, approvedPath::set, filter)
        );

        showInFrame(button);
        triggerButton(button);
        triggerButton(button);

        assertEquals(selectedFile, approvedPath.get());
        verify(dialogs).chooseFile("Select file", "Open", initialDirectory, true, filter);
        verify(dialogs).chooseFile("Select file", "Open", selectedFile, true, filter);
    }
}
