---
layout: default
title: Getting started
nav_order: 1
has_children: true
permalink: /getting_started/
---

# Getting started

emuStudio is a Java Swing application which implements editor of virtual computer configuration, source code editor, and emulation "controller" (sometimes known as "debugger"). Emulation controller is used for controlling the emulation, and also supports the interaction in the application GUI. Under the hood, it operates with an instance of so-called "virtual computer". The virtual computer - or computer emulator - is loaded from the computer configuration, selected by the user in the beginning.

Virtual computer is composed of plugin instances, which can be interconnected, as the computer configuration defines.
Each plugin is single, almost self-contained, JAR file. It means that almost all dependencies the plugin uses should be
present in the JAR file, except the following, which are bundled with emuStudio and will always be available on the class-path:

- [emuLib][emulib]{:target="_blank"}
- [java cup runtime][java-cup]{:target="_blank"}
- [SLF4J logging][slf4j]{:target="_blank"}
- [args4j][args4j]{:target="_blank"} for command-line parsing

The application, besides, provides also:

- plugin configuration management - implementation of [PluginSettings][pluginSettings]{:target="_blank"}
- and runtime API for the communication between plugins and the application - implementation of [ApplicationApi][applicationApi]{:target="_blank"}

Plugins get those objects in the constructor. Details are provided in further chapters, but here can be revealed just this: there are four types of plugins. Virtual computer can contain a compiler (which produces code loadable in the emulated computer), one CPU emulator, one operating memory and none, one or more virtual devices. The core concept of a virtual computer is inspired by the [von Neumann model][vonNeumann]{:target="_blank"}.

Each plugin implements API from emuLib, following some predefined rules. Then a plugin is be compiled to a JAR file
and put into a proper subdirectory in emuStudio installation.

[emulib]: https://search.maven.org/artifact/net.emustudio/emulib/11.5.0/jar
[java-cup]: https://mvnrepository.com/artifact/com.github.vbmacher/java-cup-runtime/11b-20160615
[slf4j]: https://mvnrepository.com/artifact/org.slf4j/slf4j-api/1.7.30
[args4j]: https://mvnrepository.com/artifact/args4j/args4j/2.33
[pluginSettings]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/PluginSettings.html
[applicationApi]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/ApplicationApi.html
[vonNeumann]: https://en.wikipedia.org/wiki/Von_Neumann_architecture
