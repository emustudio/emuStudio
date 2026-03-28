/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PluginConnectionTest {

    @Test
    public void createAndReadBackConnectionMetadata() {
        PluginConnection connection = PluginConnection.create(
                "from",
                "to",
                true,
                Arrays.asList(SchemaPoint.of(10, 20), SchemaPoint.of(30, 40))
        );

        assertEquals("from", connection.getFromPluginId());
        assertEquals("to", connection.getToPluginId());
        assertTrue(connection.isBidirectional());
        assertEquals(Arrays.asList(SchemaPoint.of(10, 20), SchemaPoint.of(30, 40)), connection.getSchemaPoints());
        assertEquals("from", connection.getConfig().get("from"));
    }
}
