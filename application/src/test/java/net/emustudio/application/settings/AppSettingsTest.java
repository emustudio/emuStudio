/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppSettingsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testContradictingNoGui() throws IOException {
        FileConfig config = FileConfig.of(temporaryFolder.newFile("emustudio.toml"));
        config.set(AppSettings.KEY_NOGUI, true);
        config.save();

        AppSettings appSettings = new AppSettings(config, false);

        Optional<Boolean> noguiOpt = appSettings.getBoolean(AppSettings.KEY_NOGUI);
        assertTrue(noguiOpt.isPresent());
        assertFalse(noguiOpt.get());
    }

    @Test
    public void removedEditorBookmarksAreDeletedFromSettingsFile() throws IOException {
        Path file = temporaryFolder.newFile("emuStudio.toml").toPath();
        Files.writeString(file, "editorBookmarks = \"x=1\"\neditorTabSize = 8\n");

        AppSettings.fromFile(file, false);

        try (FileConfig reloaded = FileConfig.of(file)) {
            reloaded.load();
            assertFalse(reloaded.contains("editorBookmarks"));
            assertEquals(Integer.valueOf(8), reloaded.get("editorTabSize"));
        }
    }

}
