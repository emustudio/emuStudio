package net.emustudio.plugins.device.zxspectrum.display.io;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

@ThreadSafe
public class Keyboard implements KeyListener {
    private final List<DeviceContext<Byte>> devices = new ArrayList<>();

    public void connect(DeviceContext<Byte> device) {
        devices.add(device);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keycode = e.getKeyCode();
        if (!((keycode == KeyEvent.VK_SHIFT || keycode == KeyEvent.VK_CONTROL ||
            keycode == KeyEvent.VK_ALT || keycode == KeyEvent.VK_META))) {
            keycode = (e.getKeyChar() & 0xFF);
        }
        inputReceived((byte) keycode);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private void inputReceived(int input) {
        for (DeviceContext<Byte> device : devices) {
            device.writeData((byte) input);
        }
    }
}
