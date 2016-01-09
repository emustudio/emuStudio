package sk.tuke.emustudio.rasp.memory.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.memory.AbstractMemory;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.util.Objects;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;
import sk.tuke.emustudio.rasp.memory.gui.MemoryWindow;

@PluginType(
        type = PLUGIN_TYPE.MEMORY,
        title = "RASP Memory",
        description = "RASP memory containing program as well as data",
        copyright = ""
)
public class RASPMemoryImpl extends AbstractMemory {

    private final RASPMemoryContextImpl context;
    private final ContextPool contextPool;
    private MemoryWindow gui;

    public RASPMemoryImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        this.context = new RASPMemoryContextImpl();

        try {
            contextPool.register(pluginID, context, RASPMemoryContext.class);
            contextPool.register(pluginID, context, MemoryContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException ex) {
            StaticDialogs.showErrorMessage("Could not register RASP Memory context",
                    RASPMemoryImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public int getSize() {
        return context.getSize();
    }

    @Override
    public void destroy() {
        context.destroy();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    @Override
    public void showSettings() {
        if (gui == null) {
            gui = new MemoryWindow(context);
        }
        gui.setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    @Override
    public String getVersion() {
        return "";
    }

}
