/*
 * CompoundUndoManager.java
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012, Peter Jakubčo
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

/**
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License 
 *       at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 */

import javax.swing.undo.*;


/**
 * A simple UndoManager that groups the Edits in each 0.5 second.  If the time 
 * difference between the current undo and the last one is less than 0.5 secs,
 * then the two edits are compound.
 * 
 * @author Ayman Al-Sairafi
 */
@SuppressWarnings("serial")
public class CompoundUndoManager extends UndoManager {
    /**
     * Delay between consequtive edits in ms where edits are added together.
     * If the delay is greater than this, then separate undo operations are 
     * done, otherwise they are combined.
     */
    public static final int IDLE_DELAY_MS = 390;

    private long startMillis = 0;
    private CompoundEdit compoundEdit = null;
    
    /**
     * See corresponding Javadoc for UndoManager.
     * @param anEdit
     * @return
     */
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
        return result;
    }

    /**
     * Determine if the Redo operation can be realized.
     * 
     * @return true if Redo can be realized, false otherwise.
     */
    @Override
    public synchronized boolean canRedo() {
        return super.canRedo();
    }

    /**
     * Determine if the Undo operation can be realized.
     * 
     * @return true if Undo can be realized, false otherwise.
     */
    @Override
    public synchronized boolean canUndo() {
        return super.canUndo();
    }

    /**
     * See corresponding Javadoc for UndoManager.
     */
    @Override
    public synchronized void discardAllEdits() {
        compoundEdit = null;
        super.discardAllEdits();
    }


    /**
     * Perform Redo operation. For more information, see corresponding Javadoc
     * for UndoManager.
     * 
     * @throws CannotRedoException
     */
    @Override
    public synchronized void redo() throws CannotRedoException {
        commitCompound();
        super.redo();
    }

    /**
     * Perform Redo operation. For more information, see corresponding Javadoc
     * for UndoManager.
     * 
     * @throws CannotUndoException
     */
    @Override
    public synchronized void undo() throws CannotUndoException {
        commitCompound();
        super.undo();
    }

    public synchronized void commitCompound() {
        if (compoundEdit != null) {
            compoundEdit.end();
            compoundEdit = null;
        }
    }
}
