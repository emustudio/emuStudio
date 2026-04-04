/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import static org.easymock.EasyMock.*;
/**
 * Test helper to create MemoryContextImpl instances from tests
 * in subpackages that cannot access the protected constructor.
 */
public class TestMemoryContextFactory {
    public static MemoryContextImpl create(int size, int banks, int bankCommon) {
        MemoryContextAnnotations annotations = createNiceMock(MemoryContextAnnotations.class);
        replay(annotations);
        MemoryContextImpl context = new MemoryContextImpl(annotations);
        context.init(size, banks, bankCommon);
        return context;
    }
}
