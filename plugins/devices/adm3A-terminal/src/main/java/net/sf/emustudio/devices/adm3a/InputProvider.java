package net.sf.emustudio.devices.adm3a;

import emulib.plugins.device.DeviceContext;

public interface InputProvider {
    public void addDeviceObserver(DeviceContext<Short> observer);

    public void removeDeviceObserver(DeviceContext<Short> observer);

    public void destroy();

}