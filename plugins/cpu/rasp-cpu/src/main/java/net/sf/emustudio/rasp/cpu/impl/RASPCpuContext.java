/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.emustudio.rasp.cpu.impl;

import emulib.runtime.exceptions.PluginInitializationException;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.exceptions.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.InvalidContextException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;

/**
 * Context of the RASP CPU emulator.
 *
 * @author miso
 */
public class RASPCpuContext implements CPUContext {

    private final RASPEmulatorImpl cpuEmulator;
    private final AbstractTapeContext[] tapes; //[0] -  input tape, [1] - output tape
    private final ContextPool contextPool;

    /**
     * Constructor.
     *
     * @param cpu the RASP CPU emulator instance
     * @param contextPool the context pool to register to
     */
    public RASPCpuContext(RASPEmulatorImpl cpu, ContextPool contextPool) {
        this.cpuEmulator = Objects.requireNonNull(cpu);
        this.contextPool = Objects.requireNonNull(contextPool);
        this.tapes = new AbstractTapeContext[2];
    }

    /**
     * Initialization of the CPU context.
     *
     * @param pluginID the ID of the plugin
     * @throws emulib.plugins.PluginInitializationException
     */
    public void init(long pluginID) throws PluginInitializationException {
        try {
            // ========== initialization of the input tape
            tapes[0] = (AbstractTapeContext) contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 0);
            if (tapes == null) {
                throw new PluginInitializationException(cpuEmulator, "Could not get the context of the input tape.");
            }
            //tape will be bounded by left (can't move beyond zeroth position)
            tapes[0].setBounded(true);
            //user will be able to change the values on the output tape
            tapes[0].setEditable(true);
            //the position will be highlighted by blue in the GUI
            tapes[0].setPosVisible(true);
            //we don't the input tape to be cleared when emulator is reset, so we do not have to provide inputs again after reset
            tapes[0].setClearAtReset(false);
            tapes[0].setTitle("Input tape");
            //============

            //============initialization of the output tape
            tapes[1] = (AbstractTapeContext) contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 1);
            if (tapes[1] == null) {
                throw new PluginInitializationException(cpuEmulator, "Could not get the context of the output tape.");
            }
            tapes[1].setBounded(true);
            //we don't want the user to be able to change the values at the output tape
            tapes[1].setEditable(false);
            tapes[1].setPosVisible(true);
            //outputs should be cleared at reset
            tapes[1].setClearAtReset(true);
            tapes[1].setTitle("Output tape");

        } catch (InvalidContextException | ContextNotFoundException ex) {
            Logger.getLogger(RASPCpuContext.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get the input tape.
     *
     * @return the input tape
     */
    public AbstractTapeContext getInputTape() {
        return tapes[0];
    }

    /**
     * Get the output tape.
     *
     * @return the output tape
     */
    public AbstractTapeContext getOutputTape() {
        return tapes[1];
    }

    /**
     * Destroy the tapes.
     */
    public void destroy() {
        for (AbstractTapeContext tape : tapes) {
            tape = null;
        }
    }

    /**
     * Checks if both input and output tapes are contained, i.e. if both are
     * non-null
     *
     * @return true if both are non-null, false otherwise
     */
    public boolean allTapesAreNonNull() {
        for (AbstractTapeContext tape : tapes) {
            if (tape == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRawInterruptSupported() {
        return false;
    }

    @Override
    public void signalRawInterrupt(DeviceContext dc, byte[] bytes) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void signalInterrupt(DeviceContext dc, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearInterrupt(DeviceContext dc, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getCPUFrequency() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
