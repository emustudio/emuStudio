/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.model;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;
import java.nio.file.Path;
import java.util.Optional;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
public class FileImagesModelTest {
    private Dialogs dialogs;
    @Before
    public void setUp() {
        dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
    }
    private PluginSettings createEmptySettings() {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(settings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        replay(settings);
        return settings;
    }
    private PluginSettings createSettingsWithOneImage() {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("imageName0")).andReturn(Optional.of("/path/to/image0.bin")).anyTimes();
        expect(settings.getInt("imageAddress0")).andReturn(Optional.of(0x1000)).anyTimes();
        expect(settings.getInt("imageBank0")).andReturn(Optional.of(1)).anyTimes();
        expect(settings.getString("imageName1")).andReturn(Optional.empty()).anyTimes();
        expect(settings.getInt("imageAddress1")).andReturn(Optional.empty()).anyTimes();
        expect(settings.getInt("imageBank1")).andReturn(Optional.empty()).anyTimes();
        replay(settings);
        return settings;
    }
    @Test
    public void testEmptyModel() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        assertEquals(0, model.getRowCount());
    }
    @Test
    public void testColumnCount() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        assertEquals(3, model.getColumnCount());
    }
    @Test
    public void testColumnNames() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        assertEquals("File name", model.getColumnName(0));
        assertEquals("Address", model.getColumnName(1));
        assertEquals("Bank", model.getColumnName(2));
        assertEquals("", model.getColumnName(3));
    }
    @Test
    public void testColumnClass() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals(Integer.class, model.getColumnClass(2));
    }
    @Test
    public void testLoadedFromSettings() {
        FileImagesModel model = new FileImagesModel(createSettingsWithOneImage(), dialogs);
        assertEquals(1, model.getRowCount());
    }
    @Test
    public void testGetValueAtFileName() {
        FileImagesModel model = new FileImagesModel(createSettingsWithOneImage(), dialogs);
        assertEquals("image0.bin", model.getValueAt(0, 0));
    }
    @Test
    public void testGetValueAtAddress() {
        FileImagesModel model = new FileImagesModel(createSettingsWithOneImage(), dialogs);
        assertEquals("0x1000", model.getValueAt(0, 1));
    }
    @Test
    public void testGetValueAtBank() {
        FileImagesModel model = new FileImagesModel(createSettingsWithOneImage(), dialogs);
        assertEquals(1, model.getValueAt(0, 2));
    }
    @Test
    public void testGetValueAtInvalidColumn() {
        FileImagesModel model = new FileImagesModel(createSettingsWithOneImage(), dialogs);
        assertNull(model.getValueAt(0, 5));
    }
    @Test
    public void testAddImage() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/test.bin"), 0x100, 0);
        assertEquals(1, model.getRowCount());
        assertEquals("test.bin", model.getValueAt(0, 0));
        assertEquals("0x0100", model.getValueAt(0, 1));
        assertEquals(0, model.getValueAt(0, 2));
    }
    @Test
    public void testRemoveImageAt() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/test1.bin"), 0x100, 0);
        model.addImage(Path.of("/tmp/test2.bin"), 0x200, 1);
        model.removeImageAt(0);
        assertEquals(1, model.getRowCount());
        assertEquals("test2.bin", model.getValueAt(0, 0));
    }
    @Test
    public void testGetFileNameAtRow() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/myfile.bin"), 0x100, 0);
        assertEquals("/tmp/myfile.bin", model.getFileNameAtRow(0));
    }
    @Test
    public void testGetImageAddressAtRow() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/myfile.bin"), 0x500, 0);
        assertEquals(0x500, model.getImageAddressAtRow(0));
    }
    @Test
    public void testGetImageBankAtRow() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/myfile.bin"), 0x100, 2);
        assertEquals(2, model.getImageBankAtRow(0));
    }
    @Test
    public void testGetImageFullNames() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/a.bin"), 0, 0);
        model.addImage(Path.of("/tmp/b.bin"), 0, 0);
        assertEquals(2, model.getImageFullNames().size());
        assertEquals("/tmp/a.bin", model.getImageFullNames().get(0));
    }
    @Test
    public void testGetImageAddresses() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/a.bin"), 0x100, 0);
        assertEquals(1, model.getImageAddresses().size());
        assertEquals(Integer.valueOf(0x100), model.getImageAddresses().get(0));
    }
    @Test
    public void testGetImageBanks() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/a.bin"), 0, 3);
        assertEquals(Integer.valueOf(3), model.getImageBanks().get(0));
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testGetImageFullNamesIsUnmodifiable() {
        FileImagesModel model = new FileImagesModel(createEmptySettings(), dialogs);
        model.addImage(Path.of("/tmp/a.bin"), 0, 0);
        model.getImageFullNames().add("fail");
    }
    @Test
    public void testImageBankDefaultsToZeroWhenNotInSettings() {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("imageName0")).andReturn(Optional.of("/img.bin")).anyTimes();
        expect(settings.getInt("imageAddress0")).andReturn(Optional.of(0)).anyTimes();
        expect(settings.getInt("imageBank0")).andReturn(Optional.empty()).anyTimes();
        expect(settings.getString("imageName1")).andReturn(Optional.empty()).anyTimes();
        expect(settings.getInt("imageAddress1")).andReturn(Optional.empty()).anyTimes();
        expect(settings.getInt("imageBank1")).andReturn(Optional.empty()).anyTimes();
        replay(settings);
        FileImagesModel model = new FileImagesModel(settings, dialogs);
        assertEquals(1, model.getRowCount());
        assertEquals(0, model.getImageBankAtRow(0));
    }
}
