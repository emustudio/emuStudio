/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

package net.emustudio.application.configuration;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationConfigTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testContradictingNoGui() throws IOException {
        FileConfig config = FileConfig.of(temporaryFolder.newFile("emustudio.toml"));
        config.set(ApplicationConfig.KEY_NOGUI, true);
        config.set(ApplicationConfig.KEY_AUTO, true);
        config.save();

        ApplicationConfig applicationConfig = new ApplicationConfig(config, false, false);

        Optional<Boolean> noguiOpt = applicationConfig.getBoolean(ApplicationConfig.KEY_NOGUI);
        Optional<Boolean> autoOpt = applicationConfig.getBoolean(ApplicationConfig.KEY_AUTO);
        assertTrue(noguiOpt.isPresent());
        assertTrue(autoOpt.isPresent());
        assertFalse(noguiOpt.get());
        assertFalse(autoOpt.get());
    }
}
