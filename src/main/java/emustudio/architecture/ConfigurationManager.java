/*
 * ConfigurationManager
 * 
 * KISS, YAGNI, DRY
 * 
 * Copyright (C) 2012, Peter Jakubčo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.architecture;

import emustudio.architecture.drawing.Schema;
import java.util.Properties;

/**
 * This interface provides methods for managing configuration of a virtual computer.
 */
public interface ConfigurationManager {
    public Properties readConfiguration(String configName, boolean schema_too) throws ReadConfigurationException;
    public void writeConfiguration(String configName, Properties settings) throws WriteConfigurationException;
    public boolean renameConfiguration(String newName, String oldName);
    public boolean deleteConfiguration(String configName);
    
    public Schema loadSchema(String configName) throws ReadConfigurationException;
    public void saveSchema(Schema schema) throws WriteConfigurationException;
    
}
