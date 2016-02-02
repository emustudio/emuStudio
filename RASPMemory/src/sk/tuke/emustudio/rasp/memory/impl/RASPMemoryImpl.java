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

/**
 * Class representing memory plugin for RASP.
 */
public class RASPMemoryImpl extends AbstractMemory {

    private final RASPMemoryContextImpl context;
    private final ContextPool contextPool;
    private MemoryWindow gui;

    /**
     * Constructor.
     *
     * @param pluginID ID of the plugin
     * @param contextPool the contextPool to register this plugin to
     */
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

    /**
     * Get number of items in the memory.
     *
     * @return the number of items in the memory.
     */
    @Override
    public int getSize() {
        return context.getSize();
    }

    /**
     * Clears the memory and distroys the GUI windows.
     */
    @Override
    public void destroy() {
        context.destroy();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    /**
     * Shows memory window.
     */
    @Override
    public void showSettings() {
        if (gui == null) {
            gui = new MemoryWindow(context);
        }
        gui.setVisible(true);
    }

    /**
     * This plugin has GUI window implemented, so true is returned.
     *
     * @return always true
     */
    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    /**
     * This method is not yet needed.
     *
     * @return empty string
     */
    @Override
    public String getVersion() {
        return "";
    } 

    @Override
    public int getProgramStart() {
        return context.getProgramStart();
    } 
    
}
