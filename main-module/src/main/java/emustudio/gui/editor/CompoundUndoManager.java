/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.editor;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;


/**
 * A simple UndoManager that groups the Edits in each 0.5 second.  If the time
 * difference between the current undo and the last one is less than 0.5 secs,
 * then the two edits are compound.
 *
 */
class CompoundUndoManager extends UndoManager {
    /**
     * Delay between consecutive edits in ms where edits are added together.
     * If the delay is greater than this, then separate undo operations are
     * done, otherwise they are combined.
     */
    static final int IDLE_DELAY_MS = 390;

    private long startMillis = 0;
    private CompoundEdit compoundEdit = null;

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean result = true;

        long now = System.currentTimeMillis();
        if (compoundEdit == null) {
            compoundEdit = new CompoundEdit();
            result = super.addEdit(compoundEdit);
        }
        if (!result) {
            return false;
        }
        compoundEdit.addEdit(anEdit);

        if ((startMillis > 0) && (now - startMillis > IDLE_DELAY_MS)) {
            compoundEdit.end();
            compoundEdit = null;
        }
        startMillis = now;
        return true;
    }

    @Override
    public synchronized boolean canRedo() {
        return super.canRedo();
    }

    @Override
    public synchronized boolean canUndo() {
        return super.canUndo();
    }

    @Override
    public synchronized void discardAllEdits() {
        compoundEdit = null;
        super.discardAllEdits();
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        commitCompound();
        super.redo();
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        commitCompound();
        super.undo();
    }

    synchronized void commitCompound() {
        if (compoundEdit != null) {
            compoundEdit.end();
            compoundEdit = null;
        }
    }
}
