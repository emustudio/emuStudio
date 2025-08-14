/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
