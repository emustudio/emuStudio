package net.emustudio.plugins.memory.rasp.api;

import java.io.Serializable;

public interface RASPMemoryCell extends Serializable  {

    boolean isInstruction();

    int getAddress();

    int getValue();
}
