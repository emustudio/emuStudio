---
layout: default
title: Loading and initialization
nav_order: 1
parent: Plugin basics
permalink: /plugin_basics/loading
---

# Loading and initialization

Instantiating and initializing plugins is done by emuStudio application in one thread. All plugins share one class-loader, which creates a potential risk of naming conflicts. Therefore, each classes and resources should be put in packages with unique name.  

## Plugin instantiation

Plugin JAR files are unzipped and loaded into memory as one bunch mixed together. The class loader will recognize all found classes and resources. Dependencies explicitly specified in manifest files are recognized and loaded as well. In case of circular dependencies, plugins loading will fail.

This process happens just once in the beginning, so adding another plugin at run-time is not possible. The result of this phase is that all plugin classes are loaded in memory and all plugin roots are instantiated.

### What should plugin do in the constructor

Plugin constructor has three arguments - plugin ID, emuStudio API and plugin settings. Here, plugin can for example read its settings, or instantiate some final objects used later. But the most important operation here is to register so-called "plugin contexts" (if a plugin has some) into [ContextPool][contextPool]{:target="_blank"}, obtainable from emuStudio API. Note that plugin contexts of connected plugins must NOT be obtained here - in constructor.  

Another chapter talks about plugin contexts in more detail.

## Plugin initialization

After plugins are instantiated, they are being "initialized". In fact, it means just that emuStudio will call [Plugin.initialize()][pluginInitialize]{:target="_blank"} method on each plugin. The plugin initializations are ordered by plugin type:

1. Compiler
2. CPU
3. Memory
4. Devices in the order as they are defined in the virtual computer configuration

### What should plugin do here

The most important operation what a plugin should do in the [Plugin.initialize()][pluginInitialize]{:target="_blank"} method is to obtain "plugin contexts" of another connected plugins. Plugin contexts can be obtained from already mentioned [ContextPool][contextPool]{:target="_blank"} class, obtainable from emuStudio API.


[contextPool]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/ContextPool.html
[pluginInitialize]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/Plugin.html#initialize()
