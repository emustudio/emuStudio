/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTable;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Test;
import javax.swing.*;
import static org.easymock.EasyMock.*;
public class SettingsActionTest {
    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemoryThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        MemoryContextImpl ctx = createNiceMock(MemoryContextImpl.class);
        MemoryTable table = createNiceMock(MemoryTable.class);
        PluginSettings settings = createNiceMock(PluginSettings.class);
        replay(dialogs, parent, ctx, table, settings);
        new SettingsAction(dialogs, parent, null, ctx, table, settings, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        MemoryImpl memory = createNiceMock(MemoryImpl.class);
        MemoryTable table = createNiceMock(MemoryTable.class);
        PluginSettings settings = createNiceMock(PluginSettings.class);
        replay(dialogs, parent, memory, table, settings);
        new SettingsAction(dialogs, parent, memory, null, table, settings, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        MemoryImpl memory = createNiceMock(MemoryImpl.class);
        MemoryContextImpl ctx = createNiceMock(MemoryContextImpl.class);
        PluginSettings settings = createNiceMock(PluginSettings.class);
        replay(dialogs, parent, memory, ctx, settings);
        new SettingsAction(dialogs, parent, memory, ctx, null, settings, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullSettingsThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        MemoryImpl memory = createNiceMock(MemoryImpl.class);
        MemoryContextImpl ctx = createNiceMock(MemoryContextImpl.class);
        MemoryTable table = createNiceMock(MemoryTable.class);
        replay(dialogs, parent, memory, ctx, table);
        new SettingsAction(dialogs, parent, memory, ctx, table, null, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullDialogsThrows() {
        JDialog parent = createNiceMock(JDialog.class);
        MemoryImpl memory = createNiceMock(MemoryImpl.class);
        MemoryContextImpl ctx = createNiceMock(MemoryContextImpl.class);
        MemoryTable table = createNiceMock(MemoryTable.class);
        PluginSettings settings = createNiceMock(PluginSettings.class);
        replay(parent, memory, ctx, table, settings);
        new SettingsAction(null, parent, memory, ctx, table, settings, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullParentThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        MemoryImpl memory = createNiceMock(MemoryImpl.class);
        MemoryContextImpl ctx = createNiceMock(MemoryContextImpl.class);
        MemoryTable table = createNiceMock(MemoryTable.class);
        PluginSettings settings = createNiceMock(PluginSettings.class);
        replay(dialogs, memory, ctx, table, settings);
        new SettingsAction(dialogs, null, memory, ctx, table, settings, null);
    }
}
