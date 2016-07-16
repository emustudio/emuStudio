/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
 */
package emustudio.main;

import emulib.runtime.ContextPool;
import emulib.runtime.PluginLoader;
import emulib.runtime.exceptions.InvalidPasswordException;
import emulib.runtime.exceptions.InvalidPluginException;
import emulib.runtime.exceptions.PluginInitializationException;
import emustudio.architecture.Computer;
import emustudio.architecture.ComputerConfig;
import emustudio.architecture.ComputerFactory;
import emustudio.architecture.Configuration;
import emustudio.architecture.ReadConfigurationException;
import emustudio.architecture.SettingsManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

class Emulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Emulator.class);
    private static final String ERROR_EMULIB_PROBLEMS = "Error: Could not initialize emuLib. Please see log file for details.";

    private final Computer computer;
    private final Configuration configuration;
    private final SettingsManagerImpl settingsManager;

    private Emulator(Computer computer, Configuration configuration, SettingsManagerImpl settingsManager) {
        this.computer = Objects.requireNonNull(computer);
        this.configuration = Objects.requireNonNull(configuration);
        this.settingsManager = Objects.requireNonNull(settingsManager);
    }

    public Computer getComputer() {
        return computer;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public SettingsManagerImpl getSettingsManager() {
        return settingsManager;
    }

    public static Optional<Emulator> load(String computerName, ContextPool contextPool, PluginLoader pluginLoader) {
        try {
            Computer computer = new ComputerFactory(pluginLoader).createComputer(computerName, contextPool);
            contextPool.setComputer(Main.emulibToken, computer);

            Configuration configuration = ComputerConfig.read(computerName);
            SettingsManagerImpl settingsManager = new SettingsManagerImpl(computer.getPluginInfos(), configuration);

            computer.initialize(settingsManager);

            return Optional.of(new Emulator(computer, configuration, settingsManager));
        } catch (InvalidPluginException e) {
            LOGGER.error("Could not load plugin.", e);
            Main.tryShowErrorMessage("Could not load plugin. Please see log file for details.");
        } catch (ReadConfigurationException e) {
            LOGGER.error("Could not read configuration.", e);
            Main.tryShowErrorMessage("Error: Could not read configuration. Please see log file for details.");
        } catch (PluginInitializationException e) {
            LOGGER.error("Could not initialize plugins.", e);
            Main.tryShowErrorMessage("Error: Could not initialize plugins. Please see log file for details.");
        } catch (InvalidPasswordException e) {
            LOGGER.error("Could not initialize emuLib.", e);
            Main.tryShowErrorMessage(ERROR_EMULIB_PROBLEMS);
        } catch (Throwable e) {
            LOGGER.error("Unexpected error", e);
            Main.tryShowErrorMessage(ERROR_EMULIB_PROBLEMS);
        }
        return Optional.empty();
    }
}
