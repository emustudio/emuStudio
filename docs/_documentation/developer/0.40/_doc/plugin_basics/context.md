---
layout: default
title: Plugin contexts
nav_order: 2
parent: Plugin basics
permalink: /plugin_basics/context
---

# Plugin contexts

Plugin contexts are answer to the question "how plugins communicate?". Plugin root classes are seen by emuStudio, but not by plugins. Instead, plugins can register their own custom objects (called "contexts") in the [ContextPool][contextPool]{:target="_blank"} object during plugin [instantiation][instantiation] (in the constructor). [ContextPool][contextPool]{:target="_blank"} is a shared container of all plugin contexts. Plugins can also obtain contexts of other plugins, during plugin [initialization][initialization]. Then, plugin communicate by calling regular Java methods on the plugin context objects, obtained from context pool.

[ContextPool][contextPool]{:target="_blank"} is not thread-safe, so all operations on it should be performed in the same thread which called method [Plugin.initialize()][pluginInitialize]{:target="_blank"}.
    
Each plugin context must implement a [Context][context]{:target="_blank"} interface (or it's derivative). Interfaces which extend [Context][context]{:target="_blank"} must be then annotated with [PluginContext][pluginContext]{:target="_blank"} annotation. 
There are prepared some standard context interfaces in the API ([CompilerContext][compilerContext]{:target="_blank"}, [MemoryContext][memoryContext]{:target="_blank"}, [CpuContext][cpuContext]{:target="_blank"} and [DeviceContext][deviceContext]{:target="_blank"}), and those can be used when implementing plugin context class - or they can be further extended to provide customized versions of contexts.

Plugin context objects are registered in [ContextPool][contextPool]{:target="_blank"} as key-value pair, where the key is an context interface which the context implements, and value is the context object. A plugin can register none, one or many contexts. Single context object can be registered multiple times, if the keys (context interfaces) are different.

Sample custom context might be created like this:

```java
@PluginContext
public interface SampleContext extends CpuContext {

   // custom methods...
}
```

And the context class might look as follows:

```java
public class SampleContextImpl implements SampleContext {

   // implementation ...
}
``` 

Registration of this context might look as follows:

```java
@PluginRoot(type = PLUGIN_TYPE.CPU, title = "Sample CPU emulator")
public class SamplePlugin implements CPU {

    public SamplePlugin(long pluginID, ApplicationApi emustudio, PluginSettings settings) {
        ContextPool contextPool = emustudio.getContextPool();

        SampleContext context = new SampleContextImpl();

        // We will register the same context two times, but by different context interfaces
        contextPool.register(pluginID, context, CpuContext.class); 
        contextPool.register(pluginID, context, SampleContext.class);
    }
}
```

If another plugin wants to obtain the context, it should do it in the [Plugin.initialize()][pluginInitialize]{:target="_blank"} method:

```java
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Sample device")
public class SampleDevice implements Device {

    ...

    @Override
    public void initialize() throws PluginInitializationException {

        // If obtaining the context is not vital, catch the exception
        SampleContext cpuContext = contextPool.getCPUContext(pluginID, SampleContext.class);

        ...
    }

    ...
}
```

If the requested context could not be found, or the plugins were not connected, an exception is thrown. If it is vital for the plugin to obtain the context, it should let it pass to the caller, otherwise it should be catched.


[instantiation]: {{ site.baseurl }}/plugin_basics/loading.html#plugin-instantiation
[initialization]: {{ site.baseurl }}/plugin_basics/loading.html#plugin-initialization
[contextPool]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/ContextPool.html
[pluginInitialize]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/Plugin.html#initialize()
[context]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/Context.html 
[pluginContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/annotations/PluginContext.html
[compilerContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/compiler/CompilerContext.html
[deviceContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/device/DeviceContext.html
[cpuContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/CPUContext.html
[memoryContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/memory/MemoryContext.html
