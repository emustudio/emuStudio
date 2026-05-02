/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.model;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.PluginSettingsMock;
import org.junit.Test;

import java.nio.file.Path;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class FileImagesModelTest {
    @Test
    public void emptyModelExposesTableMetadata() {
        FileImagesModel m = model();
        assertEquals(0, m.getRowCount());
        assertEquals(3, m.getColumnCount());
        assertEquals("File name", m.getColumnName(0));
        assertEquals("Address", m.getColumnName(1));
        assertEquals("Bank", m.getColumnName(2));
        assertEquals("", m.getColumnName(3));
        assertEquals(String.class, m.getColumnClass(0));
        assertEquals(String.class, m.getColumnClass(1));
        assertEquals(Integer.class, m.getColumnClass(2));
    }

    @Test
    public void settingsPopulateRowsAndDefaultBank() {
        FileImagesModel loaded = model("imageName0", "/path/to/image0.bin", "imageAddress0", 0x1000, "imageBank0", 1);
        assertEquals(1, loaded.getRowCount());
        assertEquals("image0.bin", loaded.getValueAt(0, 0));
        assertEquals("0x1000", loaded.getValueAt(0, 1));
        assertEquals(1, loaded.getValueAt(0, 2));
        assertNull(loaded.getValueAt(0, 5));
        FileImagesModel defaulted = model("imageName0", "/img.bin", "imageAddress0", 0);
        assertEquals(0, defaulted.getImageBankAtRow(0));
    }

    @Test
    public void addingAndRemovingImagesUpdatesViews() {
        FileImagesModel m = model();
        m.addImage(Path.of("/tmp/test1.bin"), 0x100, 0);
        m.addImage(Path.of("/tmp/test2.bin"), 0x200, 1);
        assertEquals("/tmp/test1.bin", m.getFileNameAtRow(0));
        assertEquals(0x100, m.getImageAddressAtRow(0));
        assertEquals(0, m.getImageBankAtRow(0));
        assertEquals("/tmp/test1.bin", m.getImageFullNames().get(0));
        assertEquals(Integer.valueOf(0x100), m.getImageAddresses().get(0));
        assertEquals(Integer.valueOf(0), m.getImageBanks().get(0));
        m.removeImageAt(0);
        assertEquals(1, m.getRowCount());
        assertEquals("test2.bin", m.getValueAt(0, 0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void imageListsAreUnmodifiable() {
        FileImagesModel m = model();
        m.addImage(Path.of("/tmp/a.bin"), 0, 0);
        m.getImageFullNames().add("fail");
    }

    static FileImagesModel model(Object... settings) {
        return new FileImagesModel(settings(settings), dialogs());
    }

    static PluginSettings settings(Object... keyValues) {
        PluginSettingsMock settings = new PluginSettingsMock();
        for (int i = 0; i < keyValues.length; i += 2) {
            settings.set((String) keyValues[i], keyValues[i + 1]);
        }
        return settings.mock();
    }

    private static Dialogs dialogs() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        return dialogs;
    }
}
