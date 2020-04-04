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
