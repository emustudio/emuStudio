package net.emustudio.plugins.memory.rasp.api;

import java.io.Serializable;

/**
 * A Label is a named pointer to an address in memory.
 */
public interface RASPLabel extends Serializable {

    /**
     * Get address to which this label points to
     *
     * @return memory address
     */
    int getAddress();

    /**
     * Get name of this label
     *
     * @return name of this label
     */
    String getLabel();
}
