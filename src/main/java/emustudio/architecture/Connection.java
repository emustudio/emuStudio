/*
 * Connection.java
 *
 * KISS, YAGNI, DRY
 * 
 * Copyright (C) 2012 Peter Jakubčo <pjakubco@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package emustudio.architecture;

import emulib.plugins.IPlugin;

/**
 * The class represents virtual connection of two plug-ins.
 * 
 * @author vbmacher
 */
public class Connection {
    private boolean onewayOnly;
    private IPlugin pluginOne;
    private IPlugin pluginTwo;
    
    public Connection(boolean onewayOnly, IPlugin pluginOne, IPlugin pluginTwo) {
        this.onewayOnly = onewayOnly;
        this.pluginOne = pluginOne;
        this.pluginTwo = pluginTwo;
    }
    
    public boolean isOneWayOnly() {
        return onewayOnly;
    }
    
    public IPlugin getPluginOne() {
        return pluginOne;
    }
    
    public IPlugin getPluginTwo() {
        return pluginTwo;
    }
}
