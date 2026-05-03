/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.runtime.helpers.Unchecked;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import static java.util.stream.Collectors.toList;
import static net.emustudio.application.internal.Reflection.doesImplement;

/**
 * This class provides methods for dynamic loading of emuStudio plugins (which in turn are JAR files.)
 */
@NotThreadSafe
public class PluginLoader {
    private final static Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);

    /**
     * Check if provided class meets plugin requirements.
     *
     * @param pluginClass the main class of the plugin
     * @return true if the class meets plugin requirements; false otherwise
     */
    static boolean trustedPlugin(Class<?> pluginClass) {
        Objects.requireNonNull(pluginClass);

        return !pluginClass.isInterface() &&
                pluginClass.isAnnotationPresent(PluginRoot.class) &&
                doesImplement(pluginClass, Plugin.class);
    }

    /**
     * Loads emuStudio plugins.
     * <p>
     * This method is called by emuStudio.
     * <p>
     * The plugins are loaded into separate class loader.
     *
     * @param pluginFiles plugin files.
     * @return List of plugins main classes
     * @throws IOException if other error happens
     */
    public List<Class<Plugin>> loadPlugins(List<File> pluginFiles) throws IOException {
        Objects.requireNonNull(pluginFiles);

        final Map<String, URL> urlsToLoad = new LinkedHashMap<>();
        for (File pluginFile : pluginFiles) {
            addUrl(urlsToLoad, pluginFile.toURI().toURL());
            for (URL dependency : findDependencies(pluginFile)) {
                addUrl(urlsToLoad, dependency);
            }
        }

        LOGGER.debug("Loading {} plugins", urlsToLoad.size());
        //noinspection resource
        URLClassLoader pluginsClassLoader = new URLClassLoader(urlsToLoad.values().toArray(new URL[0]));

        try {
            return pluginFiles.stream()
                    .map(this::findClassesInJAR)
                    .map(l -> findMainClass(pluginsClassLoader, l))
                    .collect(toList());
        } catch (Exception e) {
            // Those can be "sneaky" thrown
            //noinspection ConstantValue
            if ((e instanceof InvalidPluginException) || (e instanceof IOException)) {
                throw e;
            }
            throw new IOException(e);
        }
    }

    private void addUrl(Map<String, URL> urlsToLoad, URL url) {
        urlsToLoad.putIfAbsent(url.toExternalForm(), url);
    }

    private List<URL> findDependencies(File pluginFile) throws IOException {
        List<URL> dependencies = new ArrayList<>();

        try (JarFile file = new JarFile(pluginFile)) {
            String classPath = file.getManifest().getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (classPath != null) {
                StringTokenizer tokenizer = new StringTokenizer(classPath);
                while (tokenizer.hasMoreTokens()) {
                    dependencies.add(new File(tokenizer.nextToken()).toURI().toURL());
                }
            }
        }
        return dependencies;
    }

    private List<String> findClassesInJAR(File file) {
        List<String> classes = new ArrayList<>();

        try (JarInputStream jis = new JarInputStream(new FileInputStream(file))) {
            JarEntry jarEntry;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String jarEntryName = jarEntry.getName();
                if (!jarEntryName.toLowerCase().endsWith(".class")) {
                    continue;
                }
                String className = getValidClassName(jarEntryName);
                classes.add(className);
            }
        } catch (IOException e) {
            Unchecked.sneakyThrow(e);
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    private Class<Plugin> findMainClass(ClassLoader classLoader, List<String> classes) {
        for (String className : classes) {
            try {
                Class<?> definedClass = classLoader.loadClass(className);

                if (definedClass != null && trustedPlugin(definedClass)) {
                    return (Class<Plugin>) definedClass;
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                Unchecked.sneakyThrow(new InvalidPluginException("Could not find loaded class: " + className, e));
            }
        }
        Unchecked.sneakyThrow(new InvalidPluginException("Could not find plugin main class"));
        return null; // never goes here
    }

    /**
     * Transform a relative file name into valid Java class name.
     * <p>
     * For example, if the class file name is "somepackage/nextpackage/SomeClass.class", the method
     * will transform it to the format "somepackage.nextpackage.SomeClass".
     * <p>
     * It doesnt't work for absolute file names.
     * <p>
     * It doesn't hurt if the class name is already in valid Java format.
     *
     * @param classFileName File name defining class
     * @return valid Java class name
     */
    private String getValidClassName(String classFileName) {
        if (classFileName.toLowerCase().endsWith(".class")) {
            classFileName = classFileName.substring(0, classFileName.length() - 6);
        }
        classFileName = classFileName.replace("\\\\", "/").replace('/', '.');
        return classFileName.replace(File.separatorChar, '.');
    }
}
