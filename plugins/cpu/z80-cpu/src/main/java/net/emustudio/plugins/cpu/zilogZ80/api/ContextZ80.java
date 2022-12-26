package net.emustudio.plugins.cpu.zilogZ80.api;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

@SuppressWarnings("unused")
public interface ContextZ80 extends Context8080 {

    /**
     * Signals a non-maskable interrupt.
     * <p>
     * On the interrupt execution, CPU ignores the next instruction and instead performs a restart
     * at address 0066h. Routines should exit with RETN instruction.
     */
    void signalNonMaskableInterrupt();
}
