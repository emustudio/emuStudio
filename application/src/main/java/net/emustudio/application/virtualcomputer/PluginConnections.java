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

/**
 * Abstraction over a "connection" of computer components - i.e. plugins.
 *
 * A connection can be one-directional or bi-directional. Connections are determined solely from abstract schema
 * of the emulated computer.
 */
@FunctionalInterface
public interface PluginConnections {

    /**
     * Determine if two plugins are connected.
     *
     * More specifically, determines if <code>pluginA</code> "sees" <code>pluginB</code>. That means, if <code>pluginA</code>
     * can obtain and use contexts registered by <code>pluginB</code> in the context pool.
     *
     * Connections can be one-directional. In case of bi-directional connection, the following must hold:
     *
     * <code>
     *     isConnected(pluginA, pluginB) == isConnected(pluginB, pluginA) == true
     * </code>
     *
     * @param pluginA first plugin ID
     * @param pluginB second plugin ID
     * @return true if pluginA is connected to pluginB.
     */
    boolean isConnected(long pluginA, long pluginB);
}
