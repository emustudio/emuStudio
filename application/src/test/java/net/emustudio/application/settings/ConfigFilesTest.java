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

package net.emustudio.application.settings;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static net.emustudio.application.settings.ConfigFiles.loadConfigurations;
import static org.junit.Assert.assertTrue;

public class ConfigFilesTest {

    @Test
    public void testInvalidConfigsAreIgnored() throws IOException, URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        Path configDir = Path.of(Objects.requireNonNull(classLoader.getResource(".")).toURI());
        assertTrue(loadConfigurations(configDir).isEmpty());
    }
}
