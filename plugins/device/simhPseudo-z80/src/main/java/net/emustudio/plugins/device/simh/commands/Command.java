package net.emustudio.plugins.device.simh.commands;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.HashMap;
import java.util.Map;

public interface Command {

    /**
     * Called on SIMH interface reset
     */
    void reset();

    /**
     * Read byte
     *
     * @param control control
     * @return data
     */
    byte read(Control control);

    /**
     * Write data byte
     *
     * @param data    byte
     * @param control control
     */
    void write(byte data, Control control);

    /**
     * On command start
     *
     * @param control control
     */
    void start(Control control);


    interface Control {

        /**
         * Clears last command
         */
        void clearCommand();

        ByteMemoryContext getMemory();

        ExtendedContext getCpu();

        DeviceContext<Byte> getDevice();
    }


}
