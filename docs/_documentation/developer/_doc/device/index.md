---
layout: default
title: Writing a device
nav_order: 6
permalink: /device/
---

# Writing a device

In emuStudio, plugin root class must either implement [Device][device]{:target="_blank"} interface, or can extend more bloat-free [AbstractDevice][abstractDevice]{:target="_blank"} class.
 
A device in emuStudio has three components:

- a device "engine" (device functionality)
- optional main GUI window
- optional settings GUI window 

Communication with devices is realized through device contexts. A device, as any other plugin, can have none, one or more contexts, which should be registered in plugin root class constructor. Device context should implement a [DeviceContext][deviceContext]{:target="_blank"} interface.

Sample implementation of a device root class might look as follows:

```java
@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "Sample device"
)
public class DeviceImpl extends AbstractDevice {
    private final DeviceContext deviceEngine = new DeviceEngine();

    private MainWindow mainWindow;
    private SettingsWindow settingsWindow;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        try {
            applicationApi.getContextPool().register(pluginID, deviceEngine, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            applicationApi.getDialogs().showError(
                "Could not register Sample Device.", getTitle()
            );
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (mainWindow == null) {
            mainWindow = new MainWindow(parent);
        }
        mainWindow.setVisible(true);
    }

    @Override
    public void showSettings(JFrame parent) {
        if (settingsWindow == null) {
            settingsWindow = new SettingsWindow(parent); 
        } 
        settingsWindow.setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
```


[device]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/device/Device.html
[deviceContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/device/DeviceContext.html
[abstractDevice]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/device/AbstractDevice.html
