/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

/**
 * Abstraction over a "connection" of computer components - i.e. plugins.
 * <p>
 * A connection can be one-directional or bi-directional. Connections are determined solely from abstract schema
 * of the emulated computer.
 */
@FunctionalInterface
public interface PluginConnections {

    /**
     * Determine if two plugins are connected.
     * <p>
     * More specifically, determines if <code>pluginA</code> "sees" <code>pluginB</code>. That means, if <code>pluginA</code>
     * can obtain and use contexts registered by <code>pluginB</code> in the context pool.
     * <p>
     * Connections can be one-directional. In case of bi-directional connection, the following must hold:
     * <p>
     * <code>
     * isConnected(pluginA, pluginB) == isConnected(pluginB, pluginA) == true
     * </code>
     *
     * @param pluginA first plugin ID
     * @param pluginB second plugin ID
     * @return true if pluginA is connected to pluginB.
     */
    boolean isConnected(long pluginA, long pluginB);
}
