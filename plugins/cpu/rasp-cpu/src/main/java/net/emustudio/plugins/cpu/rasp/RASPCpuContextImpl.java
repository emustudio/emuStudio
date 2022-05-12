package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.cpu.rasp.api.RASPCpuContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import java.util.Optional;

public class RASPCpuContextImpl implements RASPCpuContext {
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;

    public void init(long pluginID, ContextPool contextPool) throws PluginInitializationException {
        inputTape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 0);
        inputTape.setLeftBounded(true);
        inputTape.setEditable(true);
        inputTape.setHighlightHeadPosition(true);
        inputTape.setClearAtReset(false);
        inputTape.setTitle("Input tape");
        inputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);

        outputTape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 1);
        outputTape.setLeftBounded(true);
        outputTape.setEditable(false);
        outputTape.setHighlightHeadPosition(true);
        outputTape.setClearAtReset(true);
        outputTape.setTitle("Output tape");
        outputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);
    }


    @Override
    public AbstractTapeContext getInputTape() {
        return inputTape;
    }

    @Override
    public AbstractTapeContext getOutputTape() {
        return outputTape;
    }

    public void destroy() {
        Optional.ofNullable(inputTape).ifPresent(AbstractTapeContext::clear);
        Optional.ofNullable(outputTape).ifPresent(AbstractTapeContext::clear);
    }
}
