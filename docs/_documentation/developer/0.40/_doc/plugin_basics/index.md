---
layout: default
title: Plugin basics
nav_order: 2
has_children: true
permalink: /plugin_basics/
---

# Plugin basics

Each plug-in is a separate Java module (source code can be in any JVM language however!), compiled into single JAR file.
The JAR file is then placed in the proper directory (e.g. `compiler/`, `cpu/`, `memory/`, and `device/`) in emuStudio
installation.

In the source code, plugins are located in `plugins/` subdirectory, then branched further by plugin type.

## Naming conventions

Plug-in names are derived from JAR file names. A name should be picked by a convention, different by plugin type.
General idea is that from the JAR file name it should be clear what the plugin is about.
 
A plugin JAR file name should be in the form of: 

```
[specific abbreviation]-[plugin type].jar
```

where `[specific abbreviation]` means some custom abbreviation of the real world "device" the plugin emulates or is part of,
optionally preceded with the manufacturer (e.g. `intel-8080`, `lsi-adm-3A`, etc.).
Then `[plugin type]` follows, but in a form as it is shown in the following table:

{:.table-responsive}
{:.table .table-stripped}
|---
| Plugin type | Naming convention | Examples
|-|-|-
| Compiler | `[language]-compiler` (for compiler of higher language), or `as-[cpu type]` (for assembler) | `as-8080`, `as-z80`, `brainc-compiler`, `ram-compiler`
|---
| CPU | `[cpu model]-cpu`, or `[computer type]-cpu` | `8080-cpu`, `z80-cpu`, `ram-cpu`, `brainduck-cpu`
|---
| Memory | `[some feature]-mem`, or `[computer type]-mem` | `standard-mem`, `ram-mem`, `brainduck-mem`
|---
| Device | `[device model]-[device type]` | `88-disk`, `adm3a-terminal`, `simh-pseudo`
|===


Plug-in names can contain digits, small and capital letters (regex: `[a-zA-Z0-9]+`). Capital letters shall be used only
just for word separation (e.g. `zilogZ80`).

## Plugin structure

A plugin must contain a public class which is considered as _plugin root_. The plugin root is automatically found, then
instantiated by emuStudio, then assigned into virtual computer and used.

A class which is to be plugin root, must:

- implement some plugin interface (i.e. [CPU][cpu]{:target="_blank"}, [Device][device]{:target="_blank"}, [Memory][memory]{:target="_blank"} or [Compiler][compiler]{:target="_blank"})
- annotate the class with [PluginRoot][pluginRoot]{:target="_blank"} annotation  
- implement a public constructor with three arguments of types (`long`, [ApplicationApi][applicationApi]{:target="_blank"}, [PluginSettings][pluginSettings]{:target="_blank"})

A sample plugin root class might look like this: 

```java
@PluginRoot(type = PLUGIN_TYPE.CPU, title = "Sample CPU emulator")
public class SamplePlugin implements CPU {

    public SamplePlugin(long pluginId, ApplicationApi emustudio, PluginSettings settings) {
        ...
    }

    ...
}
```

If there are more classes which implements some plugin interface, just one of them has to be annotated with `PluginRoot`.
If there are more classes like this, the plugin might not work correctly.  

The constructor parameters have the following meaning:

- `pluginId` is a unique plugin identification, assigned by emuStudio. Some operations require it as input argument.
- `emustudio` is a runtime API implementation, provided by emuStudio application, to be used by plugins.
- `settings` are plugin's settings. A plugin can use it for reading/writing its custom or emuStudio settings.
  Updated settings are saved immediately in the configuration file, in the same thread.

## Third-party dependencies

Each plugin can depend on third-party libraries (or other plugins). In this case, the dependencies should be either
bundled with the plugin, or the location should be present in `Class-Path` attribute in the plugin's `Manifest` file.

Some libraries are pre-loaded by emuStudio and those shouldn't be included in plugin JAR file:

- [emuLib][emulib]{:target="_blank"}
- [java cup runtime][java-cup]{:target="_blank"}
- [SLF4J logging][slf4j]{:target="_blank"}
- [args4j][args4j]{:target="_blank"} for command-line parsing

Plugins which want to use the dependencies above, should specify them as "provided" in the project.

## Incorporating a plugin in emuStudio

New plugin is another Gradle submodule, which should be "registered" in `settings.gradle` file.

If a plugin is part of a new computer, the new configuration should be created (in [TOML][toml]{:target="_blank"} format) and put in `application/src/main/files/config` directory.

Plugin can have static example files, or shell scripts. Plugin must copy them into build directory, e.g. `plugins/compiler/as-8080/build/libs/examples` or `plugins/compiler/as-8080/build/libs/scripts`. Then, in `application/build.gradle` are sections marked with `// Examples` or `// Scripts` comments:

```groovy
...
      // Examples
      ["as-8080", "as-z80", "as-ssem", "brainc-brainduck", "ramc-ram", "raspc-rasp"].collect { compiler ->
        from(examples(":plugins:compiler:$compiler")) {
          into "examples/$compiler"
        }
      }

      // Scripts
      ["as-8080", "as-z80", "as-ssem", "brainc-brainduck", "ramc-ram", "raspc-rasp"].collect { compiler ->
        from(scripts(":plugins:compiler:$compiler")) {
          into "bin"
        }
      }
      ["88-disk"].collect { device ->
        from(scripts(":plugins:device:$device")) {
          into "bin"
        }
      }
...
```

It is necessary to put your plugin name in the particular collection.

[emulib]: https://search.maven.org/artifact/net.emustudio/emulib/11.5.0/jar
[java-cup]: https://mvnrepository.com/artifact/com.github.vbmacher/java-cup-runtime/11b-20160615
[slf4j]: https://mvnrepository.com/artifact/org.slf4j/slf4j-api/1.7.30
[args4j]: https://mvnrepository.com/artifact/args4j/args4j/2.33
[pluginSettings]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/PluginSettings.html
[applicationApi]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/ApplicationApi.html
[cpu]: {{ site.baseurl}}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/CPU.html
[device]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/device/Device.html
[memory]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/memory/Memory.html
[compiler]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/compiler/Compiler.html
[pluginRoot]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/annotations/PluginRoot.html  
[toml]: https://github.com/toml-lang/toml/blob/master/versions/en/toml-v0.5.0.md
