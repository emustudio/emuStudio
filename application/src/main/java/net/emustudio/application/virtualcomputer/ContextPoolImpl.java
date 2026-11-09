/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import net.emustudio.emulib.plugin.Context;
import net.emustudio.emulib.application.ContextNotFoundException;
import net.emustudio.emulib.application.ContextPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ContextPoolImpl implements ContextPool {
    private final static Logger LOGGER = LoggerFactory.getLogger(ContextPoolImpl.class);

    private final Map<Class<? extends Context>, List<Context>> contexts = new HashMap<>();
    private final Map<Long, List<Context>> contextOwners = new HashMap<>();

    private final AtomicReference<PluginConnections> computer = new AtomicReference<>();
    private final long alwaysAllowedPluginId;

    public ContextPoolImpl(long alwaysAllowedPluginId) {
        this.alwaysAllowedPluginId = alwaysAllowedPluginId;
    }

    @Override
    public void register(long pluginID, Context context, Class<? extends Context> contextInterface) {
        verifyContextInterface(contextInterface);

        // check if the contextInterface is implemented by the context
        if (!contextInterface.isInstance(context)) {
            throw new IllegalArgumentException("Context does not implement context interface");
        }

        // check if the context is already registered
        List<Context> contextsByInterface = contexts.get(contextInterface);
        if (contextsByInterface != null) {
            // Test if the context instance is already there
            if (contextsByInterface.contains(context)) {
                throw new IllegalStateException("Context is already registered");
            }
        }

        // finally register the context
        List<Context> contextsByOwner = contextOwners.computeIfAbsent(pluginID, k -> new ArrayList<>());
        contextsByOwner.add(context);

        if (contextsByInterface == null) {
            contextsByInterface = new ArrayList<>();
            contexts.put(contextInterface, contextsByInterface);
        }
        contextsByInterface.add(context);
    }

    @Override
    public boolean unregister(long pluginID, Class<? extends Context> contextInterface) {
        verifyContextInterface(contextInterface);

        List<Context> contextsByOwner = contextOwners.get(pluginID);
        if (contextsByOwner == null) {
            return false;
        }

        List<Context> contextsByInterface = contexts.get(contextInterface);

        if (contextsByInterface == null) {
            return false;
        }

        boolean removed = false;
        Iterator<Context> contextIterator = contextsByInterface.iterator();
        while (contextIterator.hasNext()) {
            Context context = contextIterator.next();
            if (contextsByOwner.contains(context)) {
                contextsByOwner.remove(context);
                contextIterator.remove();
                removed = true;
            }
        }
        if (contextsByInterface.isEmpty()) {
            contexts.remove(contextInterface);
        }
        return removed;
    }

    @Override
    public <T extends Context> T get(long pluginID, Class<T> contextInterface, int index)
            throws ContextNotFoundException {

        verifyContextInterface(contextInterface);
        // find the requested context
        List<Context> contextsByInterface = contexts.get(contextInterface);
        if ((contextsByInterface == null) || contextsByInterface.isEmpty()) {
            throw new ContextNotFoundException(
                    "Context " + contextInterface + " is not found in registered contexts list."
            );
        }
        LOGGER.debug("Matching context {} from {} option(s)", contextInterface, contextsByInterface.size());

        // find context based on contextID
        int currentIndex = 0;
        for (Context context : contextsByInterface) {
            if (pluginID == alwaysAllowedPluginId || hasPermission(pluginID, context)) {
                if ((index == -1) || (currentIndex == index)) {
                    LOGGER.debug("Found context with index {}", currentIndex);
                    return contextInterface.cast(context);
                }
            }
            currentIndex++;
        }
        throw new ContextNotFoundException(
                "Plugin " + pluginID + " has either no permission to access context " + contextInterface +
                        " or index is out of bounds"
        );
    }

    /**
     * Set a computer, represented as plugin connections, loaded by emuStudio.
     * <p>
     * This method should be called only by the emuStudio.
     *
     * @param computer virtual computer, loaded by emuStudio
     * @throws NullPointerException if computer is null
     */
    public void setComputer(PluginConnections computer) {
        this.computer.set(Objects.requireNonNull(computer));
    }

    private boolean hasPermission(long pluginID, Context context) {
        PluginConnections tmpComputer = Objects.requireNonNull(computer.get(), "Computer is not set");

        Optional<Long> contextOwner = findContextOwner(context);
        return contextOwner.filter(co -> tmpComputer.isConnected(pluginID, co)).isPresent();
    }

    private Optional<Long> findContextOwner(Context context) {
        Long contextOwner = null;
        for (Map.Entry<Long, List<Context>> owner : contextOwners.entrySet()) {
            List<Context> contextsByOwner = owner.getValue();
            if (contextsByOwner.contains(context)) {
                contextOwner = owner.getKey();
                break;
            }
        }
        return Optional.ofNullable(contextOwner);
    }

    private void verifyContextInterface(Class<? extends Context> contextInterface) {
        Objects.requireNonNull(contextInterface);

        if (!contextInterface.isInterface()) {
            throw new IllegalArgumentException("Given class is not an interface");
        }
    }
}
