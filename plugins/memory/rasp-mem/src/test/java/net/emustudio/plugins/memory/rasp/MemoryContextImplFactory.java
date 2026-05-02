/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;

/**
 * Test helper to create MemoryContextImpl instances from other test packages.
 * Needed because MemoryContextImpl constructor has protected access.
 */
public class MemoryContextImplFactory {

    public static MemoryContextImpl create() {
        MemoryContextAnnotations annotations = createNiceMock(MemoryContextAnnotations.class);
        replay(annotations);
        return new MemoryContextImpl(annotations);
    }
}

