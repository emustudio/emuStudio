/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.virtualcomputer;

import net.emustudio.application.internal.Hashing;
import net.emustudio.application.internal.Reflection;
import net.emustudio.emulib.plugins.Context;
import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.compiler.CompilerContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ContextPoolImpl implements ContextPool {
    private final static Logger LOGGER = LoggerFactory.getLogger(ContextPoolImpl.class);

    private final Map<String, List<Context>> contexts = new HashMap<>();
    private final Map<Long, List<Context>> contextOwners = new HashMap<>();

    private final AtomicReference<PluginConnections> computer = new AtomicReference<>();
    private final long alwaysAllowedPluginId;

    public ContextPoolImpl(long alwaysAllowedPluginId) {
        this.alwaysAllowedPluginId = alwaysAllowedPluginId;
    }

    @Override
    public void register(long pluginID, Context context, Class<? extends Context> contextInterface) throws ContextAlreadyRegisteredException, InvalidContextException {
        verifyPluginContext(contextInterface);
        String contextHash = computeHash(contextInterface);

        // check if the contextInterface is implemented by the context
        if (!Reflection.doesImplement(context.getClass(), contextInterface)) {
            throw new InvalidContextException("Context does not implement context interface");
        }

        // check if the context is already registered
        List<Context> contextsByHash = contexts.get(contextHash);
        if (contextsByHash != null) {
            // Test if the context instance is already there
            if (contextsByHash.contains(context)) {
                throw new ContextAlreadyRegisteredException();
            }
        }

        // finally register the context
        List<Context> contextsByOwner = contextOwners.computeIfAbsent(pluginID, k -> new ArrayList<>());
        contextsByOwner.add(context);

        if (contextsByHash == null) {
            contextsByHash = new ArrayList<>();
            contexts.put(contextHash, contextsByHash);
        }
        contextsByHash.add(context);
    }

    @Override
    public boolean unregister(long pluginID, Class<? extends Context> contextInterface) throws InvalidContextException {
        verifyPluginContext(contextInterface);
        String contextHash = computeHash(contextInterface);

        List<Context> contextsByOwner = contextOwners.get(pluginID);
        if (contextsByOwner == null) {
            return false;
        }

        List<Context> contextsByHash = contexts.get(contextHash);

        if (contextsByHash == null) {
            return false;
        }

        Iterator<Context> contextIterator = contextsByHash.iterator();
        while (contextIterator.hasNext()) {
            Context context = contextIterator.next();
            if (contextsByOwner.contains(context)) {
                contextsByOwner.remove(context);
                contextIterator.remove();
            }
        }
        if (contextsByHash.isEmpty()) {
            contexts.remove(contextHash);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Context> T getContext(long pluginID, Class<T> contextInterface, int index)
        throws InvalidContextException, ContextNotFoundException {

        verifyPluginContext(contextInterface);
        // find the requested context
        List<Context> contextsByHash = contexts.get(computeHash(contextInterface));
        if ((contextsByHash == null) || contextsByHash.isEmpty()) {
            throw new ContextNotFoundException(
                "Context " + contextInterface + " is not found in registered contexts list."
            );
        }
        LOGGER.debug("Matching context " + contextInterface + " from " + contextsByHash.size() + " option(s)");

        // find context based on contextID
        int currentIndex = 0;
        for (Context context : contextsByHash) {
            if (pluginID == alwaysAllowedPluginId || hasPermission(pluginID, context)) {
                if ((index == -1) || (currentIndex == index)) {
                    LOGGER.debug("Found context with index " + currentIndex);
                    return (T) context;
                }
            }
            currentIndex++;
        }
        throw new ContextNotFoundException(
            "Plugin " + pluginID + " has either no permission to access context " + contextInterface +
                " or index is out of bounds"
        );
    }

    @Override
    public CPUContext getCPUContext(long pluginID) throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, CPUContext.class, -1);
    }

    @Override
    public <T extends CPUContext> T getCPUContext(long pluginID, Class<T> contextInterface)
        throws InvalidContextException, ContextNotFoundException {

        return getContext(pluginID, contextInterface, -1);
    }

    @Override
    public <T extends CPUContext> T getCPUContext(long pluginID, Class<T> contextInterface, int index)
        throws InvalidContextException, ContextNotFoundException {

        return getContext(pluginID, contextInterface, index);
    }

    @Override
    public CompilerContext getCompilerContext(long pluginID) throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, CompilerContext.class, -1);
    }

    @Override
    public <T extends CompilerContext> T getCompilerContext(long pluginID, Class<T> contextInterface)
        throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, contextInterface, -1);
    }

    @Override
    public <T extends CompilerContext> T getCompilerContext(long pluginID, Class<T> contextInterface, int index)
        throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, contextInterface, index);
    }

    @Override
    public <CellType, T extends MemoryContext<CellType>> T getMemoryContext(long pluginID, Class<T> contextInterface)
        throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, contextInterface, -1);
    }

    @Override
    public <CellType, T extends MemoryContext<CellType>> T getMemoryContext(long pluginID, Class<T> contextInterface,
                                                                            int index) throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, contextInterface, index);
    }

    @Override
    public <DataType, T extends DeviceContext<DataType>> T getDeviceContext(long pluginID, Class<T> contextInterface)
        throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, contextInterface, -1);
    }

    @Override
    public <DataType, T extends DeviceContext<DataType>> T getDeviceContext(long pluginID, Class<T> contextInterface,
                                                                            int index) throws InvalidContextException, ContextNotFoundException {
        return getContext(pluginID, contextInterface, index);
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

    /**
     * Compute emuStudio-specific hash of the context interface.
     * The name of the interface is not important, only method names and their signatures.
     *
     * @param contextInterface interface to compute hash of
     * @return hash representing the interface
     */
    private static <T extends Context> String computeHash(Class<T> contextInterface) throws InvalidContextException {
        List<Method> contextMethods = Arrays.asList(contextInterface.getMethods());
        contextMethods.sort(Comparator.comparing(Method::getName));

        StringBuilder hash = new StringBuilder();
        for (Method method : contextMethods.toArray(new Method[0])) {
            hash.append(method.getGenericReturnType().toString()).append(" ").append(method.getName()).append("(");
            for (Class<?> param : method.getParameterTypes()) {
                hash.append(param.getName()).append(",");
            }
            hash.append(");");
        }
        try {
            return Hashing.SHA1(hash.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidContextException("Could not compute hash for interface " + contextInterface, e);
        }
    }

    private void verifyPluginContext(Class<? extends Context> contextInterface) throws InvalidContextException {
        Objects.requireNonNull(contextInterface);

        if (!contextInterface.isInterface()) {
            throw new InvalidContextException("Given class is not an interface");
        }
        if (!contextInterface.isAnnotationPresent(PluginContext.class)) {
            throw new InvalidContextException("The interface is not annotated with 'PluginContext' annotation");
        }
    }
}
