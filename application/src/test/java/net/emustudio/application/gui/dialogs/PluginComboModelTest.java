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
package net.emustudio.application.gui.dialogs;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class PluginComboModelTest {

    @Test
    public void testEmptyModel() {
        PluginComboModel comboModel = new PluginComboModel(Collections.emptyList());

        assertNull(comboModel.getSelectedItem());
        assertTrue(comboModel.getSelectedFileName().isEmpty());
        assertEquals(0, comboModel.getSize());
    }

    @Test
    public void testFileNamesArePreserved() {
        PluginComboModel comboModel = new PluginComboModel(List.of("test.jar", "nojar"));

        assertNull(comboModel.getSelectedItem());
        assertTrue(comboModel.getSelectedFileName().isEmpty());
        assertEquals(2, comboModel.getSize());

        comboModel.setSelectedItem("test");
        assertEquals("test", comboModel.getSelectedItem());
        assertEquals(Optional.of("test.jar"), comboModel.getSelectedFileName());

        comboModel.setSelectedItem("nojar");
        assertEquals("nojar", comboModel.getSelectedItem());
        assertEquals(Optional.of("nojar"), comboModel.getSelectedFileName());
    }
}
