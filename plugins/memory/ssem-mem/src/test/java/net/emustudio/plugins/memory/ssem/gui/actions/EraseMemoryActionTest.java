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
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import javax.swing.table.AbstractTableModel;

import static org.easymock.EasyMock.*;

public class EraseMemoryActionTest {

    @Test
    public void testEraseMemoryAction() {
        AbstractTableModel tableModel = createMock(AbstractTableModel.class);
        tableModel.fireTableDataChanged();
        expectLastCall().once();
        replay(tableModel);

        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        memory.clear();
        expectLastCall().once();
        replay(memory);

        EraseMemoryAction action = new EraseMemoryAction(tableModel, memory);
        action.actionPerformed(null);
    }
}