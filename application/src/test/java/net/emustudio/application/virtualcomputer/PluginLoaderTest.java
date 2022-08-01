/*
 * Run-time library for emuStudio and plugins.
 *
 *     Copyright (C) 2006-2022  Peter Jakubčo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.virtualcomputer;

import net.emustudio.application.virtualcomputer.stubs.CPUImplStub;
import net.emustudio.application.virtualcomputer.stubs.CPUListenerStub;
import net.emustudio.application.virtualcomputer.stubs.UnannotatedCPUStub;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static net.emustudio.application.internal.Reflection.doesImplement;
import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginLoaderTest {
    private static final String BAD_PLUGIN_PATH = "plugin-invalid.jar";
    private static final String NOT_A_PLUGIN_PATH = "not-a-plugin.jar";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PluginLoader pluginLoader;

    @Before
    public void setUp() {
        pluginLoader = new PluginLoader();
    }

    private File toFile(String filename) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(filename).toURI());
    }

    private Collection<Class<Plugin>> loadBadPlugin(PluginLoader instance) throws Exception {
        return instance.loadPlugins(Collections.singletonList(toFile(BAD_PLUGIN_PATH)));
    }

    @Test(expected = InvalidPluginException.class)
    public void testLoadNotAPlugin() throws Exception {
        pluginLoader.loadPlugins(Collections.singletonList(toFile(NOT_A_PLUGIN_PATH)));
    }

    @Test
    public void testDoesImplement() {
        // test for nested interface
        assertFalse(doesImplement(CPUListenerStub.class, Plugin.class));
        // test for inherited interface
        assertTrue(doesImplement(CPUImplStub.class, Plugin.class));
    }

    @Test
    public void testCorrectTrustedPlugin() {
        assertTrue(PluginLoader.trustedPlugin(CPUImplStub.class));
    }

    @Test
    public void testTrustedPluginOnNotAPluginClassReturnsFalse() {
        assertFalse(PluginLoader.trustedPlugin(CPUListenerStub.class));
    }

    @Test
    public void testTrustedPluginOnInterfaceReturnsFalse() {
        assertFalse(PluginLoader.trustedPlugin(CPU.class));
    }

    @Test
    public void testTrustedPluginOnPluginClassWithoutAnnotation() {
        assertFalse(PluginLoader.trustedPlugin(UnannotatedCPUStub.class));
    }

    @Test(expected = NullPointerException.class)
    public void testLoadPluginNullFileNameThrows() throws Exception {
        pluginLoader.loadPlugins(null);
    }

    @Test(expected = Throwable.class)
    public void testInvalidPluginConstructorThrows() throws Exception {
        Collection<Class<Plugin>> result = loadBadPlugin(pluginLoader);

        for (Class<Plugin> pluginClass : result) {
            pluginClass.getConstructor(Long.class, ContextPool.class);
        }
    }

    private File createJar(String className, String... dependsOn) throws IOException, URISyntaxException {
        File file = temporaryFolder.newFile(className.replaceAll("/",".").concat(".jar"));
        JarCreator jarCreator = new JarCreator();

        file.getParentFile().mkdirs();
        file.createNewFile();
        jarCreator.createJar(file, toFile(className), Arrays.asList(dependsOn));

        return file;
    }

    @Test
    public void testDependenciesAreLoadedCorrectly() throws Exception {
        System.setProperty("sun.misc.URLClassPath.debugLookupCache", "true");

        File lastDep = createJar("dependencies/hidden/C.class", "");
        File secondDep = createJar("dependencies/hidden/BdependsOnC.class", lastDep.getAbsolutePath());
        File plugin = createJar("dependencies/APluginDependsOnB.class", secondDep.getAbsolutePath());

        // Since PluginLoader must share ClassLoader with current one, emuLib is preloaded automatically
        pluginLoader = new PluginLoader();
        Class<Plugin> cl = pluginLoader.loadPlugins(List.of(plugin)).iterator().next();

        Constructor<Plugin> constructor = cl.getDeclaredConstructor(long.class, ApplicationApi.class, PluginSettings.class);
        cl.getDeclaredMethod("hi").invoke(constructor.newInstance(
            0L, createNiceMock(ApplicationApi.class), createNiceMock(PluginSettings.class)
        ));
    }
}
