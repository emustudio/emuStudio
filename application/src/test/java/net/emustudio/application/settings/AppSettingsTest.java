/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppSettingsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testContradictingNoGui() throws IOException {
        FileConfig config = FileConfig.of(temporaryFolder.newFile("emustudio.toml"));
        config.set(AppSettings.KEY_NOGUI, true);
        config.set(AppSettings.KEY_AUTO, true);
        config.save();

        AppSettings appSettings = new AppSettings(config, false, false);

        Optional<Boolean> noguiOpt = appSettings.getBoolean(AppSettings.KEY_NOGUI);
        Optional<Boolean> autoOpt = appSettings.getBoolean(AppSettings.KEY_AUTO);
        assertTrue(noguiOpt.isPresent());
        assertTrue(autoOpt.isPresent());
        assertFalse(noguiOpt.get());
        assertFalse(autoOpt.get());
    }
}
