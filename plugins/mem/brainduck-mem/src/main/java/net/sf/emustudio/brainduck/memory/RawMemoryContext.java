package net.sf.emustudio.brainduck.memory;

import emulib.annotations.ContextType;
import emulib.plugins.memory.MemoryContext;

@ContextType(id = "BrainDuck Raw Memory")
public interface RawMemoryContext extends MemoryContext<Short> {

    /**
     * Returns raw memory represented by Java array.
     * <p>
     * Memory notifications must be handled manually if this array changes.
     *
     * @return raw memory
     */
    short[] getRawMemory();
}
